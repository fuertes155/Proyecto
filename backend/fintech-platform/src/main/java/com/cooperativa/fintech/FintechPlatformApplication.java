package com.cooperativa.fintech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FintechPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintechPlatformApplication.class, args);
    }
}
