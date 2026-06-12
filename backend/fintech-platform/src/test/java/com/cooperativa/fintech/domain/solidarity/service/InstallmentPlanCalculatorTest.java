package com.cooperativa.fintech.domain.solidarity.service;

import com.cooperativa.fintech.domain.solidarity.model.LoanInstallment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallmentPlanCalculatorTest {

    @Test
    void shouldGenerateCorrectNumberOfInstallments() {
        UUID loanId = UUID.randomUUID();
        List<LoanInstallment> plan = InstallmentPlanCalculator.generate(
                loanId,
                new BigDecimal("120000.00"),
                new BigDecimal("0.0050"),
                6,
                LocalDate.of(2026, 6, 1)
        );
        assertEquals(6, plan.size());
        assertEquals(1, plan.getFirst().getInstallmentNumber());
        assertEquals(6, plan.getLast().getInstallmentNumber());
    }
}
