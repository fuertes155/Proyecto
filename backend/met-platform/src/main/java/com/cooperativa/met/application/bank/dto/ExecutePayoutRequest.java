package com.cooperativa.met.application.bank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ExecutePayoutRequest(
        @NotNull(message = "La cuenta bancaria destino es obligatoria")
        UUID externalBankAccountId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal amount,

        String concept,

        @NotBlank(message = "El PIN es obligatorio")
        String pin,

        String otp,

        @NotBlank(message = "La llave de idempotencia es obligatoria")
        String idempotencyKey
) {
}
