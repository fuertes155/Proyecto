package com.cooperativa.fintech.domain.identity.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class User {

    private final UUID id;
    private final DocumentType documentType;
    private final String documentNumber;
    private final String email;
    private final String phone;
    private final String firstName;
    private final String lastName;
    private final String pinHash;
    private final String biometricHash;
    private final UserStatus status;
    private final KycStatus kycStatus;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User withPinHash(String newPinHash) {
        return this.toBuilder().pinHash(newPinHash).updatedAt(Instant.now()).build();
    }

    public User withBiometricHash(String newBiometricHash) {
        return this.toBuilder().biometricHash(newBiometricHash).updatedAt(Instant.now()).build();
    }

    public User withStatus(UserStatus newStatus) {
        return this.toBuilder().status(newStatus).updatedAt(Instant.now()).build();
    }

    public User withKycStatus(KycStatus newKycStatus) {
        return this.toBuilder().kycStatus(newKycStatus).updatedAt(Instant.now()).build();
    }
}
