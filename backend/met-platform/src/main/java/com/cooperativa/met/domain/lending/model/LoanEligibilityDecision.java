package com.cooperativa.met.domain.lending.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Resultado del motor de scoring: la banda de riesgo aplicable y, si el usuario es
 * elegible, los límites (monto máximo, plazo máximo, tasa) dentro de los que puede
 * solicitar un préstamo.
 */
@Getter
@Builder
public class LoanEligibilityDecision {

    private final RiskTier tier;
    private final int score;
    private final BigDecimal maxAmount;
    private final int maxTermMonths;
    private final BigDecimal annualInterestRate;
    private final String reason;

    public boolean isApproved() {
        return tier.isApproved();
    }
}
