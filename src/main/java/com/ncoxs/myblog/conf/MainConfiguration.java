package com.ncoxs.myblog.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MainConfiguration {

    private AtomicInteger asyncThreadId = new AtomicInteger();

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
}
