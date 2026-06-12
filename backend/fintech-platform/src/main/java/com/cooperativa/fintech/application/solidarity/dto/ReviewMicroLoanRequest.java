package com.cooperativa.fintech.application.solidarity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewMicroLoanRequest(
        @NotNull boolean approved,
        @Size(max = 255) String rejectionReason
) {
}
