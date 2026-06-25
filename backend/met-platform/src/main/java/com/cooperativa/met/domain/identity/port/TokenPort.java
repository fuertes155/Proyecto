package com.cooperativa.met.domain.identity.port;

import java.util.UUID;

public interface TokenPort {

    String generateAccessToken(UUID userId, String email);

    String generateRefreshToken(UUID userId);

    UUID validateAccessToken(String token);

    UUID validateRefreshToken(String token);

    RefreshTokenClaims validateRefreshTokenClaims(String token);

    /** Genera un JWT de acceso para administradores con claim 'role' */
    String generateAdminAccessToken(UUID adminId, String username, String role);

    /** Extrae el claim 'role' de un token (null si no existe) */
    String extractRole(String token);

    record RefreshTokenClaims(UUID userId, UUID jti) {
    }
}
