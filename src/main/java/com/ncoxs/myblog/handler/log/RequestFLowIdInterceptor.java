package com.ncoxs.myblog.handler.log;

import com.ncoxs.myblog.constant.RequestAttributeConst;
import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.util.general.ResourceUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

@Component
@PropertySource("classpath:app-props.properties")
public class RequestFLowIdInterceptor implements HandlerInterceptor {
    
    @Value("${request.flow-id.write-mode}")
    private String requestFlowIdWriteMode;
    
    @Value("${request.flow-id.resource}")
    private String requestFlowIdResource;


    private static final String REQUEST_FLOW_WRITE_MODE_SECONDS = "seconds";
    private static final String REQUEST_FLOW_WRITE_MODE_MINUTES = "minutes";
    private static final String REQUEST_FLOW_WRITE_MODE_PER_REQUEST = "per_request";


    private AtomicLong requestFlowId;
    private DataOutputStream requestFlowIdOut;
    private boolean writePerRequest;


    public RequestFLowIdInterceptor() {
        switch (requestFlowIdWriteMode) {
            case REQUEST_FLOW_WRITE_MODE_PER_REQUEST:
                writePerRequest = true;
                break;
        }

        String requestFlowIdStr = ResourceUtil.loadString(requestFlowIdResource);
        if (StringUtils.hasText(requestFlowIdStr)) {
            requestFlowId.set(Long.parseLong(requestFlowIdStr));
        } else {
            requestFlowId.set(1);
        }

        try {
            requestFlowIdOut = new DataOutputStream(new FileOutputStream(Paths.get(ResourceUtil.classpath(),
                    requestFlowIdResource).toString()));
        } catch (FileNotFoundException e) {
            throw new ImpossibleError(e);
        }
    }
    

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long flowId = requestFlowId.getAndIncrement();
        request.setAttribute(RequestAttributeConst.REQUEST_FLOW_ID, flowId);
        if (writePerRequest) {
            requestFlowIdOut.writeLong(flowId);
        }

        return true;
    }
}
