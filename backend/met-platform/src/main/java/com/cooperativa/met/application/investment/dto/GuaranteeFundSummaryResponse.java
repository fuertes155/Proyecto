package com.cooperativa.met.application.investment.dto;

import java.math.BigDecimal;
import java.util.List;

public record GuaranteeFundSummaryResponse(
        BigDecimal balance,
        BigDecimal coverageRatio,
        int activationDaysLate,
        List<GuaranteeFundMovementResponse> movements
) {
}
