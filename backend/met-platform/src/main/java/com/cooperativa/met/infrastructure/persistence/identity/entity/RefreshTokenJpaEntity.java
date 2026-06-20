package com.cooperativa.met.infrastructure.persistence.identity.entity;

import java.time.Instant;
import java.util.UUID;

import com.cooperativa.met.domain.identity.model.RefreshToken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    @Column(name = "jti", nullable = false, updatable = false)
    private UUID jti;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Version
    @Column(name = "version")
    private Long version;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(UUID jti, UUID userId, Instant issuedAt, Instant expiresAt, boolean revoked) {
        this.jti = jti;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    public UUID getJti() {
        return jti;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public RefreshToken toDomain() {
        return new RefreshToken(jti, userId, issuedAt, expiresAt, revoked);
    }

    public static RefreshTokenJpaEntity fromDomain(RefreshToken token) {
        return new RefreshTokenJpaEntity(token.jti(), token.userId(), token.issuedAt(), token.expiresAt(), token.revoked());
    }
}
