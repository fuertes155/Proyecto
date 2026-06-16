package com.cooperativa.met.domain.lending.service;

import com.cooperativa.met.domain.lending.model.LoanSimulationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrenchAmortizationCalculatorTest {

    @Test
    void shouldCalculateFixedMonthlyPayment() {
        LoanSimulationResult result = FrenchAmortizationCalculator.simulate(
                new BigDecimal("1000000"),
                12,
                new BigDecimal("0.2400"),
                LocalDate.of(2026, 6, 1)
        );

        assertEquals(12, result.getSchedule().size());
        assertTrue(result.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, result.getSchedule().getLast().getRemainingBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldConvertEffectiveAnnualToMonthly() {
        BigDecimal monthly = FrenchAmortizationCalculator.effectiveAnnualToMonthly(new BigDecimal("0.2400"));
        assertTrue(monthly.compareTo(new BigDecimal("0.018")) > 0);
        assertTrue(monthly.compareTo(new BigDecimal("0.019")) < 0);
    }
}
