package com.cooperativa.met.application.compliance.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewAlertRequest(
        @NotNull String status,
        String notes
) {
}
