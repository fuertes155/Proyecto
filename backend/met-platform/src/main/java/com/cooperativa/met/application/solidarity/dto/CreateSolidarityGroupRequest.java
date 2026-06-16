package com.cooperativa.met.application.solidarity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateSolidarityGroupRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @DecimalMin(value = "5000.00", message = "Aporte mínimo: $5.000") BigDecimal minContribution,
        @Min(3) @Max(50) Integer maxMembers
) {
}
