package com.cooperativa.met.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "met.cors")
public class MetCorsProperties {

    private List<String> allowedOrigins = List.of();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @PostConstruct
    void validate() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException(
                "met.cors.allowed-origins no puede estar vacío. " +
                "Configura la variable de entorno CORS_ORIGINS con los dominios permitidos."
            );
        }
        if (allowedOrigins.contains("*")) {
            log.warn("⚠️  CORS configurado con wildcard '*'. " +
                     "Solo permitido en entorno local/dev. NO usar en producción.");
        }
    }
}
