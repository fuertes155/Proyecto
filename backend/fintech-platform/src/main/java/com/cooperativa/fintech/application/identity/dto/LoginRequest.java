package com.cooperativa.fintech.application.identity.dto;

import com.cooperativa.fintech.domain.identity.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull DocumentType documentType,
        @NotBlank String documentNumber,
        String pin,
        String biometricPayload
) {
}
