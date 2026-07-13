package com.krino.backend.configuration;

import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    @Bean("mailExecutor")
    ThreadPoolTaskExecutor mailExecutor() {
        log.debug("Creating Async Mail Executor");
        var executor = new ThreadPoolTaskExecutor();
        // those are intentionally set to 2, since this backend won't be deployed on a strong VPS
        // and doesn't send a lot of emails
        // use 4 for more concurrent emails sending
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * Safety net for uncaught exceptions thrown by void {@code @Async} methods, which would
     * otherwise vanish on the worker thread. Delivery failures are already handled explicitly in
     * {@code EmailEventListener}; this only fires if something unexpected escapes that handling.
     *
     * <p>{@code getAsyncExecutor()} is intentionally left at its default (null) so this does not
     * become the global default executor: {@code @Async} usages keep resolving by qualifier.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async method failed: {}.{}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    ex);
        };
    }
}
