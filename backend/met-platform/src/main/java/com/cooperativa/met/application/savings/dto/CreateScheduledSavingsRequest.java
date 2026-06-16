package com.cooperativa.met.application.savings.dto;

import com.cooperativa.met.domain.savings.model.ContributionFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateScheduledSavingsRequest(
        @NotBlank @Size(max = 100) String name,
        @DecimalMin(value = "1000.00", message = "El aporte mínimo es $1.000") BigDecimal contributionAmount,
        @DecimalMin(value = "10000.00", message = "La meta mínima es $10.000") BigDecimal targetAmount,
        @NotNull ContributionFrequency frequency,
        @Min(1) @Max(7) Integer debitDayOfWeek,
        @Min(1) @Max(28) Integer debitDayOfMonth
) {
}
