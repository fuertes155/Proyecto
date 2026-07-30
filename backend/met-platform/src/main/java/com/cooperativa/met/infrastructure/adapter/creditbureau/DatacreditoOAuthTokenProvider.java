package com.cooperativa.met.infrastructure.adapter.creditbureau;

import com.cooperativa.met.infrastructure.config.DatacreditoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Maneja la autenticación OAuth2 (grant_type=client_credentials, RFC 6749) contra
 * DataCrédito Experian: obtiene el access token con client-id/client-secret y lo
 * cachea hasta que expira, para que los adaptadores de {@link com.cooperativa.met.domain.lending.port.CreditBureauPort}
 * no tengan que preocuparse por la autenticación en cada llamada.
 *
 * IMPORTANTE: el mecanismo exacto (OAuth2 vs. mTLS vs. otro) y el endpoint real
 * de token dependen del contrato con DataCrédito, que aún no está firmado. Este
 * es el estándar más común para APIs B2B de este tipo — cuando llegue la
 * documentación técnica real, ajustar solo esta clase si el mecanismo difiere.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatacreditoOAuthTokenProvider {

    private static final Duration EXPIRY_SAFETY_MARGIN = Duration.ofSeconds(30);
    private static final int DEFAULT_EXPIRES_IN_SECONDS = 3600;

    private final DatacreditoProperties properties;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    @SuppressWarnings("unchecked")
    private String fetchNewToken() {
        if (properties.getTokenUrl() == null || properties.getTokenUrl().isBlank()) {
            throw new IllegalStateException(
                    "met.credit-bureau.datacredito.token-url no está configurado (DATACREDITO_TOKEN_URL)");
        }

        log.info("Solicitando nuevo token OAuth2 a DataCrédito Experian");

        Map<String, Object> response = RestClient.create()
                .post()
                .uri(properties.getTokenUrl())
                .headers(headers -> headers.setBasicAuth(properties.getClientId(), properties.getClientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("Respuesta inválida del servidor OAuth2 de DataCrédito");
        }

        cachedToken = (String) response.get("access_token");
        int expiresInSeconds = response.get("expires_in") != null
                ? ((Number) response.get("expires_in")).intValue()
                : DEFAULT_EXPIRES_IN_SECONDS;
        tokenExpiresAt = Instant.now().plusSeconds(expiresInSeconds).minus(EXPIRY_SAFETY_MARGIN);

        return cachedToken;
    }
}
