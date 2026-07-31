package com.cooperativa.met.infrastructure.persistence.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "biometric_registrations")
@Getter
@Setter
public class BiometricRegistrationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "document_image", columnDefinition = "TEXT")
    private String documentImage;

    @Column(name = "document_back_image", columnDefinition = "TEXT")
    private String documentBackImage;

    @Column(name = "selfie_image", columnDefinition = "TEXT")
    private String selfieImage;

    @Column(name = "signature_image", columnDefinition = "TEXT")
    private String signatureImage;

    @Column(name = "liveness_score", precision = 5, scale = 4)
    private BigDecimal livenessScore;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
