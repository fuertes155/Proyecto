package com.cooperativa.met.application.bank.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfirmBankAccountVerificationRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        Integer amount
) {
}
