package com.cooperativa.met.domain.identity.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        UUID jti,
        UUID userId,
        Instant issuedAt,
        Instant expiresAt,
        boolean revoked
) {
}
