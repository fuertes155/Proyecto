package com.cooperativa.met.infrastructure.security;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements TokenPort {

    private final MetSecurityProperties securityProperties;

    @Override
    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(securityProperties.getJwt().getExpirationMs());
        return buildToken(userId, email, now, expiry, "access");
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(securityProperties.getJwt().getRefreshExpirationMs());
        return buildToken(userId, null, now, expiry, "refresh");
    }

    @Override
    public UUID validateAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new BusinessRuleException("INVALID_TOKEN", "Token de acceso inválido");
        }
        return UUID.fromString(claims.getSubject());
    }

    @Override
    public UUID validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessRuleException("INVALID_TOKEN", "Token de refresco inválido");
        }
        return UUID.fromString(claims.getSubject());
    }

    private String buildToken(UUID userId, String email, Instant issuedAt, Instant expiry, String type) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiry))
                .claim("type", type);
        if (email != null) {
            builder.claim("email", email);
        }
        return builder.signWith(secretKey()).compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new BusinessRuleException("INVALID_TOKEN", "Token inválido o expirado");
        }
    }

    private SecretKey secretKey() {
        byte[] keyBytes = securityProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
