package com.cooperativa.met.domain.identity.port;

import java.util.UUID;

public interface TokenPort {

    String generateAccessToken(UUID userId, String email);

    String generateRefreshToken(UUID userId);

    UUID validateAccessToken(String token);

    UUID validateRefreshToken(String token);

    RefreshTokenClaims validateRefreshTokenClaims(String token);

    record RefreshTokenClaims(UUID userId, UUID jti) {
    }
}
