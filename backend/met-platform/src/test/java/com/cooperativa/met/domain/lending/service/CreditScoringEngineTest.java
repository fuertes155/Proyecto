package com.cooperativa.met.domain.lending.service;

import com.cooperativa.met.domain.lending.model.LoanEligibilityDecision;
import com.cooperativa.met.domain.lending.model.RiskTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditScoringEngineTest {

    @Test
    void evaluate_rejectsScoreBelow600() {
        LoanEligibilityDecision decision = CreditScoringEngine.evaluate(599, new BigDecimal("10000000"));

        assertFalse(decision.isApproved());
        assertEquals(RiskTier.RECHAZADO, decision.getTier());
        assertEquals(BigDecimal.ZERO, decision.getMaxAmount());
        assertEquals(0, decision.getMaxTermMonths());
    }

    @Test
    void evaluate_riesgoAlto_appliesOneTimesSavingsMultiplier() {
        LoanEligibilityDecision decision = CreditScoringEngine.evaluate(650, new BigDecimal("2000000"));

        assertTrue(decision.isApproved());
        assertEquals(RiskTier.RIESGO_ALTO, decision.getTier());
        assertEquals(new BigDecimal("2000000"), decision.getMaxAmount());
        assertEquals(12, decision.getMaxTermMonths());
        assertEquals(new BigDecimal("0.26"), decision.getAnnualInterestRate());
    }

    @Test
    void evaluate_riesgoAlto_capsAtAbsoluteLimitRegardlessOfSavings() {
        // Saldo muy alto no debe superar el tope absoluto de la banda ($5,000,000 para RIESGO_ALTO)
        LoanEligibilityDecision decision = CreditScoringEngine.evaluate(650, new BigDecimal("50000000"));

        assertEquals(new BigDecimal("5000000"), decision.getMaxAmount());
    }

    @Test
    void evaluate_riesgoMedio_appliesTwoTimesSavingsMultiplier() {
        LoanEligibilityDecision decision = CreditScoringEngine.evaluate(750, new BigDecimal("5000000"));

        assertEquals(RiskTier.RIESGO_MEDIO, decision.getTier());
        assertEquals(new BigDecimal("10000000"), decision.getMaxAmount());
        assertEquals(24, decision.getMaxTermMonths());
        assertEquals(new BigDecimal("0.22"), decision.getAnnualInterestRate());
    }

    @Test
    void evaluate_riesgoBajo_appliesThreeTimesSavingsMultiplier() {
        LoanEligibilityDecision decision = CreditScoringEngine.evaluate(850, new BigDecimal("5000000"));

        assertEquals(RiskTier.RIESGO_BAJO, decision.getTier());
        assertEquals(new BigDecimal("15000000"), decision.getMaxAmount());
        assertEquals(36, decision.getMaxTermMonths());
        assertEquals(new BigDecimal("0.19"), decision.getAnnualInterestRate());
    }

    @Test
    void evaluate_prime_appliesFiveTimesSavingsMultiplier() {
        LoanEligibilityDecision decision = CreditScoringEngine.evaluate(920, new BigDecimal("5000000"));

        assertEquals(RiskTier.PRIME, decision.getTier());
        assertEquals(new BigDecimal("25000000"), decision.getMaxAmount());
        assertEquals(48, decision.getMaxTermMonths());
        assertEquals(new BigDecimal("0.15"), decision.getAnnualInterestRate());
    }

    @Test
    void evaluate_prime_capsAtSixtyMillionAbsoluteLimit() {
        LoanEligibilityDecision decision = CreditScoringEngine.evaluate(950, new BigDecimal("100000000"));

        assertEquals(new BigDecimal("60000000"), decision.getMaxAmount());
    }

    @Test
    void fromScore_handlesTierBoundariesExactly() {
        assertEquals(RiskTier.RECHAZADO, RiskTier.fromScore(0));
        assertEquals(RiskTier.RECHAZADO, RiskTier.fromScore(599));
        assertEquals(RiskTier.RIESGO_ALTO, RiskTier.fromScore(600));
        assertEquals(RiskTier.RIESGO_ALTO, RiskTier.fromScore(699));
        assertEquals(RiskTier.RIESGO_MEDIO, RiskTier.fromScore(700));
        assertEquals(RiskTier.RIESGO_BAJO, RiskTier.fromScore(800));
        assertEquals(RiskTier.PRIME, RiskTier.fromScore(900));
        // Defensivo: un score por encima de la escala documentada (150-950) no debe romper el motor
        assertEquals(RiskTier.PRIME, RiskTier.fromScore(999));
    }
}
