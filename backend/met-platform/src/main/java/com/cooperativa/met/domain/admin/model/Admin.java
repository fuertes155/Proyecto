package com.cooperativa.met.domain.admin.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Admin {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String email;
    private final AdminRole role;
    private final AdminStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Admin withStatus(AdminStatus newStatus) {
        return this.toBuilder().status(newStatus).updatedAt(Instant.now()).build();
    }

    public Admin withPasswordHash(String newHash) {
        return this.toBuilder().passwordHash(newHash).updatedAt(Instant.now()).build();
    }

    public boolean isActive() {
        return AdminStatus.ACTIVE.equals(this.status);
    }

    public boolean isSuperAdmin() {
        return AdminRole.SUPER_ADMIN.equals(this.role);
    }
}
