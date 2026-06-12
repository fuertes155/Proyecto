package com.cooperativa.fintech.application.identity.dto;

import com.cooperativa.fintech.domain.identity.model.DocumentType;
import com.cooperativa.fintech.domain.identity.model.KycStatus;
import com.cooperativa.fintech.domain.identity.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        DocumentType documentType,
        String documentNumber,
        String email,
        String phone,
        String firstName,
        String lastName,
        UserStatus status,
        KycStatus kycStatus,
        Instant createdAt
) {
}
