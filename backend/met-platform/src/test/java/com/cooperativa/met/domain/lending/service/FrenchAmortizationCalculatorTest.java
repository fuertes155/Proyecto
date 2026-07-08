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
        
        // El pago mensual exacto para $1'000.000 a 12 meses al 2% mensual (24% nominal anual)
        // PMT = 1000000 * (0.02 * (1.02)^12) / ((1.02)^12 - 1) = 94559.60
        assertEquals(new BigDecimal("94559.60"), result.getMonthlyPayment());
        
        // Verificamos que la última cuota deja el saldo en exactamente CERO
        assertEquals(BigDecimal.ZERO, result.getSchedule().getLast().getRemainingBalance());
    }

    @Test
    void shouldConvertEffectiveAnnualToMonthly() {
        BigDecimal monthly = FrenchAmortizationCalculator.effectiveAnnualToMonthly(new BigDecimal("0.2400"));
        // Ahora usamos TNA (24% / 12 = 2%)
        assertEquals(new BigDecimal("0.02000000"), monthly);
    }
}
