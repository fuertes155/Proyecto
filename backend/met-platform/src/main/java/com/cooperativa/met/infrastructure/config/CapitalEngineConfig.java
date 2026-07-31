package com.cooperativa.met.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CapitalEngineProperties.class)
public class CapitalEngineConfig {
}
