package com.cooperativa.met.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(DatacreditoProperties.class)
public class CreditBureauConfig {

    @Bean
    public WebClient datacreditoWebClient(DatacreditoProperties properties, WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
