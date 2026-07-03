package com.cooperativa.met.infrastructure.persistence.identity.entity;

import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 10)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(nullable = false)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "biometric_hash")
    private String biometricHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 30)
    private KycStatus kycStatus;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(name = "push_notifications_enabled", nullable = false)
    private boolean pushNotificationsEnabled = true;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
