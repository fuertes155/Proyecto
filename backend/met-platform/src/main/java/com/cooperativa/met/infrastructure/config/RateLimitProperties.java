package com.cooperativa.met.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de rate limiting para los endpoints de autenticación.
 * Configurable desde application.yml bajo el prefijo met.rate-limit.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "met.rate-limit")
public class RateLimitProperties {

    /**
     * Número máximo de solicitudes permitidas dentro de la ventana de tiempo.
     * Valor predeterminado: 10 intentos.
     */
    private int maxRequests = 10;

    /**
     * Duración de la ventana de tiempo en segundos.
     * Valor predeterminado: 60 segundos.
     */
    private long windowSeconds = 60;

    /**
     * Si true, el rate limit está habilitado.
     */
    private boolean enabled = true;
}
