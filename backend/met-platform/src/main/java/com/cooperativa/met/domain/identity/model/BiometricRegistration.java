package com.cooperativa.met.domain.identity.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class BiometricRegistration {

    private final UUID id;
    private final UUID userId;
    private final String documentImage;
    private final String documentBackImage;
    private final String selfieImage;
    private final String signatureImage;
    private final BigDecimal livenessScore;
    private final boolean verified;
    private final Instant verifiedAt;
    private final Instant createdAt;

    public BiometricRegistration markVerified(BigDecimal score) {
        return this.toBuilder()
                .verified(true)
                .livenessScore(score)
                .verifiedAt(Instant.now())
                .build();
    }
}
