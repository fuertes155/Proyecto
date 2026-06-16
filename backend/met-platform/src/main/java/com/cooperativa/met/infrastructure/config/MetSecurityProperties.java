package com.cooperativa.met.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "met.security")
public class MetSecurityProperties {

    private Jwt jwt = new Jwt();
    private Encryption encryption = new Encryption();

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
    }
}
