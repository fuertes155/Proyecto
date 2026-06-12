package com.cooperativa.fintech.domain.identity.port;

import java.util.UUID;

public interface TokenPort {

    String generateAccessToken(UUID userId, String email);

    String generateRefreshToken(UUID userId);

    UUID validateAccessToken(String token);

    UUID validateRefreshToken(String token);
}
