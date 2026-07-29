package com.cooperativa.met.application.identity.dto;

import com.cooperativa.met.domain.identity.model.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotNull DocumentType documentType,
        @NotBlank @Size(max = 20) String documentNumber,
        @NotBlank @Email String email,
        @Pattern(regexp = "^3\\d{9}$", message = "Teléfono colombiano inválido") String phone,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        // Viaja cifrado con RSA (E2EE, ver AesEncryptionAdapter#decryptRsa en RegisterUserUseCase);
        // no puede validarse aquí como 4 dígitos en texto plano.
        @NotBlank String pin,
        @jakarta.validation.constraints.AssertTrue(message = "Debe aceptar los términos y condiciones") Boolean termsAccepted
) {
}
