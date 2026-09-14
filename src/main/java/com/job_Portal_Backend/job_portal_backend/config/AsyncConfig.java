package com.job_Portal_Backend.job_portal_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Backs @Async methods (e.g. EmailServiceImpl.sendOtpEmail) with a small, bounded, named
 * executor. Without an explicit bean named "taskExecutor", Spring can't pick a default here —
 * the WebSocket/STOMP message broker already registers several of its own TaskExecutor beans
 * (clientInboundChannelExecutor, etc.), so @Async falls back to an unmanaged one-thread-per-call
 * executor and logs a warning on every invocation.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.initialize();
        return executor;
    }
}
