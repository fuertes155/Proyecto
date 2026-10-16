package com.cooperativa.met.application.account.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Decide a qué URL redirige Wompi al usuario cuando termina un pago (checkout
 * hosteado y PSE nativo).
 *
 * <p>Si {@code met.wompi.redirect-url} está configurada (p. ej. la página
 * {@code /api/v1/payments/wompi/return} del propio backend), se usa siempre: el
 * esquema {@code metapp://} no está registrado en la app Android, así que un
 * navegador externo no sabe abrirlo. Si no, se mantiene el comportamiento
 * anterior: la URL que pida el cliente (solo dominios permitidos) o el deep link.</p>
 */
@Slf4j
@Component
public class PaymentReturnUrlResolver {

    static final String DEFAULT_DEEP_LINK = "metapp://deposit/success";

    private final String configuredRedirectUrl;

    public PaymentReturnUrlResolver(@Value("${met.wompi.redirect-url:}") String configuredRedirectUrl) {
        this.configuredRedirectUrl = configuredRedirectUrl;
    }

    public String resolve(UUID userId, String requestedReturnUrl) {
        if (configuredRedirectUrl != null && !configuredRedirectUrl.isBlank()) {
            return configuredRedirectUrl.trim();
        }
        if (requestedReturnUrl != null && !requestedReturnUrl.isBlank()) {
            if (requestedReturnUrl.startsWith("https://met-platform.com") || requestedReturnUrl.startsWith("metapp://")) {
                return requestedReturnUrl;
            }
            log.warn("Blocked unvalidated redirect attempt to: {}", requestedReturnUrl);
        }
        return DEFAULT_DEEP_LINK + "?userId=" + userId;
    }
}
