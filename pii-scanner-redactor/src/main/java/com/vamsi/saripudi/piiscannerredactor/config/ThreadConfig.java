package com.vamsi.saripudi.piiscannerredactor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ThreadConfig {

    @Bean(name= "fileExecutor")
    public Executor fileExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
