package com.ncoxs.myblog.conf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ReflectionUtils;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MainConfiguration {

    private AtomicInteger asyncThreadId = new AtomicInteger();

    private AtomicInteger timeTaskThreadId = new AtomicInteger();


    @Bean
    public AsyncTaskExecutor asyncTaskExecutor() {
        int cpu = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor async = new ThreadPoolTaskExecutor();
        async.setCorePoolSize(cpu + 1);
        async.setMaxPoolSize(cpu * 2 + 1);
        async.setQueueCapacity(100);
        async.setKeepAliveSeconds(10 * 60);
        async.setThreadFactory(r -> new Thread(r, "async-task-pool-" + asyncThreadId.getAndIncrement()));
        async.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        async.afterPropertiesSet();

        return async;
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        int cpu = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(cpu + 1);
        scheduler.setAwaitTerminationSeconds(10 * 60);
        scheduler.setThreadFactory(r -> new Thread(r, "time-task-pool-" + timeTaskThreadId.getAndIncrement()));
        scheduler.setErrorHandler(ReflectionUtils::rethrowRuntimeException);
        scheduler.afterPropertiesSet();

        return scheduler;
    }

    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        // 不序列化 null 和 ""
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        return objectMapper;
    }
}
