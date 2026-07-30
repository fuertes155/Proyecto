package com.cooperativa.met.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP para la API de "Pagos a Terceros" (Payouts) de Wompi.
 *
 * IMPORTANTE: la URL base ({@code met.wompi.payouts.api-url}) y los
 * nombres de los headers de autenticación son provisionales — Wompi
 * documenta este producto como una API separada de la de
 * Checkout/Transacciones ya integrada para depósitos, con su propia
 * "API Key y Principal User ID". Deben confirmarse contra la
 * documentación/dashboard reales que Wompi entregue al activar el
 * producto antes de salir a producción (ver WompiPayoutAdapter).
 */
@Configuration
public class WompiPayoutConfig {

    @Bean
    public RestClient wompiPayoutRestClient(
            @Value("${met.wompi.payouts.api-url}") String baseUrl,
            @Value("${met.wompi.payouts.api-key}") String apiKey,
            @Value("${met.wompi.payouts.principal-user-id}") String principalUserId,
            @Value("${met.wompi.payouts.auth-header:Authorization}") String authHeader,
            @Value("${met.wompi.payouts.principal-header:X-Principal-User-Id}") String principalHeader,
            RestClient.Builder restClientBuilder) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(15));
        factory.setConnectTimeout(Duration.ofSeconds(5));

        String authHeaderValue = "Authorization".equals(authHeader) ? "Bearer " + apiKey : apiKey;

        return restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(authHeader, authHeaderValue)
                .defaultHeader(principalHeader, principalUserId)
                .build();
    }
}
