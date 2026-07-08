package com.cooperativa.met.application.identity.dto;

import com.cooperativa.met.domain.identity.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
        @NotNull(message = "El tipo de documento es obligatorio")
        DocumentType documentType,
        
        @NotBlank(message = "El número de documento es obligatorio")
        String documentNumber,
        
        @NotBlank(message = "El código OTP es obligatorio")
        @Size(min = 6, max = 6, message = "El código OTP debe tener 6 dígitos")
        @Pattern(regexp = "^\\d+$", message = "El código OTP debe contener solo números")
        String otpCode
) {
}
