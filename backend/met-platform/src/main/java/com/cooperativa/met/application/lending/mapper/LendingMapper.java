package com.cooperativa.met.application.lending.mapper;

import com.cooperativa.met.application.lending.dto.AmortizationInstallmentResponse;
import com.cooperativa.met.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.met.application.lending.dto.LoanEligibilityResponse;
import com.cooperativa.met.application.lending.dto.LoanSimulationResponse;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.LoanEligibilityDecision;
import com.cooperativa.met.domain.lending.model.LoanSimulationResult;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LendingMapper {

    public LoanSimulationResponse toResponse(LoanSimulationResult result) {
        return new LoanSimulationResponse(
                result.getAmount(),
                result.getTermMonths(),
                result.getAnnualInterestRate(),
                result.getMonthlyInterestRate(),
                result.getMonthlyPayment(),
                result.getTotalInterest(),
                result.getTotalPayment(),
                result.getSchedule().stream().map(this::toResponse).toList()
        );
    }

    public LoanApplicationResponse toResponse(PersonalLoanApplication application, List<AmortizationInstallment> schedule) {
        return new LoanApplicationResponse(
                application.getId(),
                application.getAmount(),
                application.getTermMonths(),
                application.getAnnualInterestRate(),
                application.getMonthlyPayment(),
                application.getTotalInterest(),
                application.getTotalPayment(),
                application.getPurpose(),
                application.getStatus(),
                application.getRejectionReason(),
                application.getRiskTier() != null ? application.getRiskTier().name() : null,
                application.getSubmittedAt(),
                schedule.stream().map(this::toResponse).toList()
        );
    }

    public LoanApplicationResponse toSummary(PersonalLoanApplication application) {
        return new LoanApplicationResponse(
                application.getId(),
                application.getAmount(),
                application.getTermMonths(),
                application.getAnnualInterestRate(),
                application.getMonthlyPayment(),
                application.getTotalInterest(),
                application.getTotalPayment(),
                application.getPurpose(),
                application.getStatus(),
                application.getRejectionReason(),
                application.getRiskTier() != null ? application.getRiskTier().name() : null,
                application.getSubmittedAt(),
                List.of()
        );
    }

    public LoanEligibilityResponse toResponse(LoanEligibilityDecision decision) {
        return new LoanEligibilityResponse(
                decision.isApproved(),
                decision.getTier().name(),
                decision.getScore(),
                decision.getMaxAmount(),
                decision.getMaxTermMonths(),
                decision.getAnnualInterestRate(),
                decision.getReason()
        );
    }

    public AmortizationInstallmentResponse toResponse(AmortizationInstallment installment) {
        return new AmortizationInstallmentResponse(
                installment.getInstallmentNumber(),
                installment.getPaymentAmount(),
                installment.getPrincipalAmount(),
                installment.getInterestAmount(),
                installment.getRemainingBalance(),
                installment.getDueDate()
        );
    }
}
