package com.cooperativa.met.domain.lending.service;

import com.cooperativa.met.domain.lending.model.LoanEligibilityDecision;
import com.cooperativa.met.domain.lending.model.RiskTier;

import java.math.BigDecimal;

/**
 * Servicio de dominio único y reutilizable que traduce un score de central de riesgo
 * en una decisión de elegibilidad de préstamo (monto máximo, plazo máximo, tasa).
 *
 * Todo módulo que necesite aplicar la política de riesgo crediticio (originación,
 * revisión manual de admin, futuros productos de crédito) debe pasar por aquí en
 * vez de reimplementar las bandas de {@link RiskTier}.
 */
public final class CreditScoringEngine {

    private CreditScoringEngine() {
    }

    public static LoanEligibilityDecision evaluate(int creditScore, BigDecimal savingsBalance) {
        RiskTier tier = RiskTier.fromScore(creditScore);

        if (!tier.isApproved()) {
            return LoanEligibilityDecision.builder()
                    .tier(tier)
                    .score(creditScore)
                    .maxAmount(BigDecimal.ZERO)
                    .maxTermMonths(0)
                    .annualInterestRate(BigDecimal.ZERO)
                    .reason("Score insuficiente (" + creditScore + "). Mínimo requerido: "
                            + RiskTier.RIESGO_ALTO.getMinScore() + ".")
                    .build();
        }

        BigDecimal maxBySavings = savingsBalance.multiply(tier.getSavingsMultiplier());
        BigDecimal maxAmount = maxBySavings.min(tier.getAbsoluteCapCop());

        return LoanEligibilityDecision.builder()
                .tier(tier)
                .score(creditScore)
                .maxAmount(maxAmount)
                .maxTermMonths(tier.getMaxTermMonths())
                .annualInterestRate(tier.getAnnualInterestRate())
                .build();
    }
}
