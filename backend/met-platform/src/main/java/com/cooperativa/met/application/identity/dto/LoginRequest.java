package com.cooperativa.met.application.identity.dto;

import com.cooperativa.met.domain.identity.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull DocumentType documentType,
        @NotBlank String documentNumber,
        @NotBlank String deviceId,
        String pin,
        String biometricPayload,
        String otpCode
) {
}
