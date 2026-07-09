package br.com.mpgsistemas.revisionalweb.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Habilita processamento assíncrono. O executor dedicado limita a extração
 * OCR+IA (CPU/IO pesados) para não esgotar as threads do Tomcat.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "extracaoExecutor")
    public ThreadPoolTaskExecutor extracaoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("extracao-");
        executor.initialize();
        return executor;
    }
}
