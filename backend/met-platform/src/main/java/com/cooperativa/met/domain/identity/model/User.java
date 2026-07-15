package com.cooperativa.met.domain.identity.model;

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
    private final int failedLoginAttempts;
    private final UserStatus status;
    private final KycStatus kycStatus;
    private final boolean termsAccepted;
    private final Instant termsAcceptedAt;
    private final boolean emailNotificationsEnabled;
    private final boolean pushNotificationsEnabled;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String lastKnownIp;
    private final String paymentCardToken;

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
    
    public User withProfile(String newEmail, String newPhone) {
        return this.toBuilder().email(newEmail).phone(newPhone).updatedAt(Instant.now()).build();
    }
    
    public User withNotifications(boolean email, boolean push) {
        return this.toBuilder().emailNotificationsEnabled(email).pushNotificationsEnabled(push).updatedAt(Instant.now()).build();
    }

    public User withFailedLoginAttempts(int attempts) {
        return this.toBuilder().failedLoginAttempts(attempts).updatedAt(Instant.now()).build();
    }
    
    public User withLastKnownIp(String ip) {
        return this.toBuilder().lastKnownIp(ip).updatedAt(Instant.now()).build();
    }

    public User withPaymentCardToken(String token) {
        return this.toBuilder().paymentCardToken(token).updatedAt(Instant.now()).build();
    }
}
