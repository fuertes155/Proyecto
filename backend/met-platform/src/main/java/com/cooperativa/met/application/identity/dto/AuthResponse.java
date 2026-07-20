package com.cooperativa.met.application.identity.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresInMs
) {
    public static AuthResponse of(UUID userId, String accessToken, String refreshToken, Long expiresInMs) {
        return new AuthResponse(userId, accessToken, refreshToken, "Bearer", expiresInMs);
    }
}
