package com.cooperativa.met.domain.solidarity.service;

import com.cooperativa.met.domain.solidarity.model.LoanInstallment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InstallmentPlanCalculator {

    private InstallmentPlanCalculator() {
    }

    public static List<LoanInstallment> generate(
            UUID loanId,
            BigDecimal principal,
            BigDecimal monthlyRate,
            int termMonths,
            LocalDate startDate) {

        BigDecimal monthlyPrincipal = principal.divide(
                BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        List<LoanInstallment> installments = new ArrayList<>();

        for (int i = 1; i <= termMonths; i++) {
            BigDecimal remaining = principal.subtract(monthlyPrincipal.multiply(BigDecimal.valueOf(i - 1)));
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = i == termMonths
                    ? remaining
                    : monthlyPrincipal;
            BigDecimal total = principalPart.add(interest);

            installments.add(LoanInstallment.builder()
                    .id(UUID.randomUUID())
                    .loanId(loanId)
                    .installmentNumber(i)
                    .principalAmount(principalPart)
                    .interestAmount(interest)
                    .totalAmount(total)
                    .dueDate(startDate.plusMonths(i))
                    .status(com.cooperativa.met.domain.solidarity.model.InstallmentStatus.PENDING)
                    .build());
        }
        return installments;
    }
}
