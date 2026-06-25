package com.cooperativa.met.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.port.TokenPort;
import com.cooperativa.met.infrastructure.config.MetSecurityProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

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
        UUID jti = UUID.randomUUID();
        return buildToken(userId, null, now, expiry, "refresh", jti);
    }

    @Override
    public UUID validateAccessToken(String token) {
        TokenPort.RefreshTokenClaims claims = validateTokenAndType(token, "access");
        return claims.userId();
    }

    @Override
    public String generateAdminAccessToken(UUID adminId, String username, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(securityProperties.getJwt().getExpirationMs());
        return Jwts.builder()
                .subject(adminId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("type", "access")
                .claim("role", role)
                .claim("username", username)
                .signWith(secretKey())
                .compact();
    }

    @Override
    public String extractRole(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public UUID validateRefreshToken(String token) {
        TokenPort.RefreshTokenClaims claims = validateTokenAndType(token, "refresh");
        return claims.userId();
    }

    @Override
    public TokenPort.RefreshTokenClaims validateRefreshTokenClaims(String token) {
        Claims claims = parseClaims(token);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new BusinessRuleException("INVALID_TOKEN", "Token de refresco inválido");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UUID jti = UUID.fromString(claims.get("jti", String.class));
        return new TokenPort.RefreshTokenClaims(userId, jti);
    }

    private TokenPort.RefreshTokenClaims validateTokenAndType(String token, String expectedType) {
        Claims claims = parseClaims(token);
        String type = claims.get("type", String.class);
        if (!expectedType.equals(type)) {
            throw new BusinessRuleException("INVALID_TOKEN", "Token inválido");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UUID jti = null;
        Object jtiRaw = claims.get("jti");
        if (jtiRaw != null) {
            jti = UUID.fromString(jtiRaw.toString());
        }
        return new TokenPort.RefreshTokenClaims(userId, jti);
    }

    private String buildToken(UUID userId, String email, Instant issuedAt, Instant expiry, String type, UUID jti) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiry))
                .claim("type", type);

        if (jti != null) {
            builder.claim("jti", jti.toString());
        }

        if (email != null) {
            builder.claim("email", email);
        }
        return builder.signWith(secretKey()).compact();
    }

    private String buildToken(UUID userId, String email, Instant issuedAt, Instant expiry, String type) {
        return buildToken(userId, email, issuedAt, expiry, type, null);
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
