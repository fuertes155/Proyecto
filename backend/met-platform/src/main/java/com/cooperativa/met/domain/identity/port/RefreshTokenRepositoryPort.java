package com.cooperativa.met.domain.identity.port;

import com.cooperativa.met.domain.identity.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByJti(UUID jti);

    void revoke(UUID jti);

    /** Revoca todos los refresh tokens activos de un usuario (bloqueo de emergencia) */
    void revokeAllByUserId(UUID userId);
}
