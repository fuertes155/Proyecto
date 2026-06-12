package com.cooperativa.fintech.application.savings.dto;

import com.cooperativa.fintech.domain.savings.model.ScheduledSavingsStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateScheduledSavingsRequest(
        @DecimalMin(value = "1000.00", message = "El aporte mínimo es $1.000") BigDecimal contributionAmount,
        ScheduledSavingsStatus status
) {
}
