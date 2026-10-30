package com.cooperativa.met.infrastructure.config;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "met.security")
public class MetSecurityProperties {

    private Jwt jwt = new Jwt();
    private Encryption encryption = new Encryption();

    private static final int MIN_JWT_SECRET_BYTES = 32; // "256-bit mínimo"
    // AesKey: requirement existente en el default anterior: 32 chars -> 256-bit.
    private static final Pattern SAFE_AES_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-!@#$%^&*()+=]{16,64}$");

    @PostConstruct
    void validate() {
        if (jwt == null || jwt.getSecret() == null || jwt.getSecret().isBlank()) {
            throw new IllegalStateException("Falta la configuración obligatoria met.security.jwt.secret (no tiene valor por defecto en application.yml).");
        }
        byte[] jwtSecretBytes = jwt.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (jwtSecretBytes.length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("met.security.jwt.secret no es válido: debe tener al menos 256 bits (32 bytes en UTF-8).");
        }

        if (encryption == null || encryption.getAesKey() == null || encryption.getAesKey().isBlank()) {
            throw new IllegalStateException("Falta la configuración obligatoria met.security.encryption.aesKey (no tiene valor por defecto en application.yml).");
        }

        // Como el AES adapter usa el string tal cual para derivar una SecretKey, validamos el formato y tamaño razonable.
        if (!SAFE_AES_KEY_PATTERN.matcher(encryption.getAesKey()).matches()) {
            throw new IllegalStateException("met.security.encryption.aesKey no es válida: tiene caracteres o una longitud no soportados.");
        }
        // AES-256 => 32 bytes como referencia; por seguridad validamos longitud mínima 32 chars (antes era default 32 chars)
        if (encryption.getAesKey().length() < 32) {
            throw new IllegalStateException("met.security.encryption.aesKey no es válida: debe tener al menos 32 caracteres para AES-256.");
        }
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
        private long refreshExpirationMs;
    }

    @Getter
    @Setter
    public static class Encryption {
        private String aesKey;
        private String rsaPrivateKey;
        private String rsaPublicKey;
        private String hmacSecret;
    }
}
