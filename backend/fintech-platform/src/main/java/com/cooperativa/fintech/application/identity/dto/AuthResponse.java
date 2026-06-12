package com.cooperativa.fintech.application.identity.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs
) {
    public static AuthResponse of(UUID userId, String accessToken, String refreshToken, long expiresInMs) {
        return new AuthResponse(userId, accessToken, refreshToken, "Bearer", expiresInMs);
    }
}
