package com.cooperativa.met.application.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetCredentialsRequest(
        @NotBlank(message = "El motivo del reseteo es requerido")
        String reason
) {}
