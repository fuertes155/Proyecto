package com.cooperativa.met.application.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ReverseTransactionRequest(
        @NotBlank(message = "El ID de transacción es requerido")
        String transactionId,

        @NotBlank(message = "El motivo es requerido para revertir transacciones")
        String reason,

        /**
         * Código de confirmación de doble factor para reversión.
         * El admin debe confirmar esta acción con un código secundario.
         */
        @NotBlank(message = "El código de confirmación es requerido")
        String confirmationCode
) {}
