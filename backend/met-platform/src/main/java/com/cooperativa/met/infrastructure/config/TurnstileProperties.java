package com.cooperativa.met.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "turnstile")
public class TurnstileProperties {
    private boolean enabled = false;
    private String secretKey = "";
}
