package com.cooperativa.met.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP para la API pública de Checkout/Transacciones de Wompi
 * (docs.wompi.co/docs/colombia/) — el mismo producto que ya usa
 * {@code GeneratePseLinkUseCase} para construir el link de checkout
 * hosteado, ahora también usado para crear transacciones PSE nativas
 * directamente vía API (sin pasar por el checkout hosteado).
 * Reutiliza met.wompi.api-url ya configurada.
 *
 * Sin header Authorization por defecto: cada llamada elige su llave (pública para
 * los endpoints de catálogo, privada para crear y consultar transacciones), ver
 * {@code WompiPseAdapter}.
 */
@Configuration
public class WompiCheckoutConfig {

    @Bean
    public RestClient wompiCheckoutRestClient(
            @Value("${met.wompi.api-url}") String baseUrl,
            RestClient.Builder restClientBuilder) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(15));
        factory.setConnectTimeout(Duration.ofSeconds(5));

        return restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
