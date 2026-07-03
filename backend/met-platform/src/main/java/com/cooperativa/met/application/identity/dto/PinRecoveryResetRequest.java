package com.cooperativa.met.application.identity.dto;

import com.cooperativa.met.domain.identity.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PinRecoveryResetRequest {
    @NotNull(message = "El tipo de documento es obligatorio")
    private DocumentType documentType;

    @NotBlank(message = "El número de documento es obligatorio")
    private String documentNumber;

    @NotBlank(message = "El código OTP es obligatorio")
    private String otpCode;

    @NotBlank(message = "El nuevo PIN es obligatorio")
    private String newPin;
}
