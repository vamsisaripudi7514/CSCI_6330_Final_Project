package com.vamsi.saripudi.piiscannerredactor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig {

    @Value("${thread.count}")
    private int threadCount;

    @Bean(name= "fileExecutor")
    public ExecutorService fileExecutor() {
        return Executors.newFixedThreadPool(threadCount);
    }
}
