package com.cooperativa.met.application.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmergencyLockRequest(
        @NotBlank(message = "El ID del objetivo es requerido")
        String targetId,

        /**
         * Alcance del bloqueo: USER, CARD, ACCESS_TOKEN
         */
        @NotBlank(message = "El alcance del bloqueo es requerido")
        String scope,

        @NotBlank(message = "El motivo es obligatorio para bloqueos de emergencia")
        String reason
) {}
