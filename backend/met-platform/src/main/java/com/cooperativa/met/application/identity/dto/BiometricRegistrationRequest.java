package com.cooperativa.met.application.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BiometricRegistrationRequest(
        @NotBlank String documentImageBase64,
        @NotBlank String selfieImageBase64
) {
}
