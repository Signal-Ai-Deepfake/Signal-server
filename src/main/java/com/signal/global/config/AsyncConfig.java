package com.signal.global.config;

import com.signal.global.i18n.LanguageContext;
import com.signal.global.i18n.ResponseLanguage;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-task-");
        // 요청의 Accept-Language를 비동기 작업(예: 딥페이크 탐지의 AI 호출)으로 넘긴다.
        executor.setTaskDecorator(runnable -> {
            ResponseLanguage language = LanguageContext.current();
            return () -> {
                LanguageContext.set(language);
                try {
                    runnable.run();
                } finally {
                    LanguageContext.clear();
                }
            };
        });
        executor.initialize();
        return executor;
    }
}
