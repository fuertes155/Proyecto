package com.cooperativa.met.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "met.credit-bureau.datacredito")
public class DatacreditoProperties {
    private String baseUrl = "https://api.experian.com.co";
    private String clientId;
    private String clientSecret;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
}
