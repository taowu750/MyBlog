package com.ncoxs.myblog.handler.log;

import com.ncoxs.myblog.constant.RequestAttributeKey;
import com.ncoxs.myblog.util.general.ResourceUtil;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 此拦截器为每个 request 定义一个流水号，方便后续在日志中标识哪些日志属于一个请求。
 * 流水号保存在 request 的 {@link RequestAttributeKey#REQUEST_FLOW_ID} 属性中。
 *
 * 流水号分配类似于数据库的自增主键，它会按照配置文件中的保存策略进行保存。
 */
@Component
@PropertySource("classpath:app-props.properties")
public class RequestFLowIdInterceptor implements HandlerInterceptor, InitializingBean, DisposableBean {
    
    @Value("${request.flow-id.write-mode}")
    private String requestFlowIdWriteMode;
    
    @Value("${request.flow-id.resource}")
    private String requestFlowIdResource;

    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    public void setThreadPoolTaskScheduler(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
    }


    // 日志保存策略，每秒保存一次
    private static final String REQUEST_FLOW_WRITE_MODE_SECONDS = "seconds";
    // 日志保存策略，每分钟保存一次
    private static final String REQUEST_FLOW_WRITE_MODE_MINUTES = "minutes";
    // 日志保存策略，每次请求开始时保存
    private static final String REQUEST_FLOW_WRITE_MODE_PER_REQUEST = "per_request";


    private AtomicLong requestFlowId;
    private final Object requestFlowIdLock = new Object();
    private RandomAccessFile requestFlowIdAccess;
    private boolean writePerRequest;
    private ScheduledFuture<?> timeTask;


    // 这里要用 afterPropertiesSet 而不用构造器，是因为 @Value 注入的属性要等到构造完成后才会被注入
    @Override
    public void afterPropertiesSet() throws Exception {
        requestFlowId = new AtomicLong();
        Path path = Paths.get(ResourceUtil.classpath(), requestFlowIdResource);
        // 目录不存在则创建
        if (!Files.isDirectory(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        requestFlowIdAccess = new RandomAccessFile(path.toString(), "rw");

        if (requestFlowIdAccess.length() > 0) {
            requestFlowId.set(requestFlowIdAccess.readLong());
        } else {
            requestFlowId.set(1);
        }

        switch (requestFlowIdWriteMode) {
            case REQUEST_FLOW_WRITE_MODE_PER_REQUEST:
                writePerRequest = true;
                break;

            case REQUEST_FLOW_WRITE_MODE_SECONDS:
                timeTask = threadPoolTaskScheduler.schedule(() -> {
                    try {
                        saveRequestFlowId(requestFlowId.get());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, new CronTrigger("*/1 * * * * *"));
                break;

            case REQUEST_FLOW_WRITE_MODE_MINUTES:
                timeTask = threadPoolTaskScheduler.schedule(() -> {
                    try {
                        saveRequestFlowId(requestFlowId.get());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, new CronTrigger("* */1 * * * *"));
                break;
        }
    }

    @Override
    public void destroy() throws IOException {
        if (timeTask != null) {
            timeTask.cancel(true);
            saveRequestFlowId(requestFlowId.get());
            requestFlowIdAccess.close();
        }
    }
    

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long flowId = requestFlowId.getAndIncrement();
        request.setAttribute(RequestAttributeKey.REQUEST_FLOW_ID, flowId);
        if (writePerRequest) {
            saveRequestFlowId(flowId);
        }

        return true;
    }


    private void saveRequestFlowId(long flowId) throws IOException {
        synchronized (requestFlowIdLock) {
            // 覆写原来的流水号
            requestFlowIdAccess.seek(0);
            requestFlowIdAccess.writeLong(flowId);
        }
    }
}
