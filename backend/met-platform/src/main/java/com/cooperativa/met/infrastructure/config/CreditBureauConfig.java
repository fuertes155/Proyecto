package com.cooperativa.met.infrastructure.config;

import com.cooperativa.met.infrastructure.adapter.creditbureau.DatacreditoOAuthTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(DatacreditoProperties.class)
public class CreditBureauConfig {

    @Bean
    public RestClient datacreditoRestClient(DatacreditoProperties properties, RestClient.Builder restClientBuilder,
            DatacreditoOAuthTokenProvider tokenProvider) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenProvider.getAccessToken());
                    return execution.execute(request, body);
                })
                .build();
    }
}
