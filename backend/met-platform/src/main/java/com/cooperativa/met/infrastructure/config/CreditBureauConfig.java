package com.cooperativa.met.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(DatacreditoProperties.class)
public class CreditBureauConfig {

    @Bean
    public RestClient datacreditoRestClient(DatacreditoProperties properties, RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        
        return restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }
}
