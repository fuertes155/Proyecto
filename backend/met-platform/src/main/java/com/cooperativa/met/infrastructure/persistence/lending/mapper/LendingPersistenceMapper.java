package com.cooperativa.met.infrastructure.persistence.lending.mapper;

import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.infrastructure.persistence.lending.entity.PersonalLoanAmortizationJpaEntity;
import com.cooperativa.met.infrastructure.persistence.lending.entity.PersonalLoanApplicationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class LendingPersistenceMapper {

    public PersonalLoanApplication toDomain(PersonalLoanApplicationJpaEntity e) {
        return PersonalLoanApplication.builder()
                .id(e.getId()).userId(e.getUserId()).amount(e.getAmount())
                .termMonths(e.getTermMonths()).annualInterestRate(e.getAnnualInterestRate())
                .monthlyPayment(e.getMonthlyPayment()).totalInterest(e.getTotalInterest())
                .totalPayment(e.getTotalPayment()).purpose(e.getPurpose()).status(e.getStatus())
                .rejectionReason(e.getRejectionReason())
                .creditScore(e.getCreditScore()).creditBureauRef(e.getCreditBureauRef())
                .riskTier(e.getRiskTier())
                .submittedAt(e.getSubmittedAt())
                .reviewedAt(e.getReviewedAt()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    public PersonalLoanApplicationJpaEntity toEntity(PersonalLoanApplication a) {
        PersonalLoanApplicationJpaEntity e = new PersonalLoanApplicationJpaEntity();
        e.setId(a.getId()); e.setUserId(a.getUserId()); e.setAmount(a.getAmount());
        e.setTermMonths(a.getTermMonths()); e.setAnnualInterestRate(a.getAnnualInterestRate());
        e.setMonthlyPayment(a.getMonthlyPayment()); e.setTotalInterest(a.getTotalInterest());
        e.setTotalPayment(a.getTotalPayment()); e.setPurpose(a.getPurpose()); e.setStatus(a.getStatus());
        e.setRejectionReason(a.getRejectionReason()); 
        e.setCreditScore(a.getCreditScore()); e.setCreditBureauRef(a.getCreditBureauRef());
        e.setRiskTier(a.getRiskTier());
        e.setSubmittedAt(a.getSubmittedAt());
        e.setReviewedAt(a.getReviewedAt()); e.setCreatedAt(a.getCreatedAt()); e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    public AmortizationInstallment toDomain(PersonalLoanAmortizationJpaEntity e) {
        return AmortizationInstallment.builder()
                .id(e.getId()).applicationId(e.getApplicationId())
                .installmentNumber(e.getInstallmentNumber()).paymentAmount(e.getPaymentAmount())
                .principalAmount(e.getPrincipalAmount()).interestAmount(e.getInterestAmount())
                .remainingBalance(e.getRemainingBalance()).dueDate(e.getDueDate())
                .build();
    }

    public PersonalLoanAmortizationJpaEntity toEntity(AmortizationInstallment i) {
        PersonalLoanAmortizationJpaEntity e = new PersonalLoanAmortizationJpaEntity();
        e.setId(i.getId() != null ? i.getId() : java.util.UUID.randomUUID());
        e.setApplicationId(i.getApplicationId());
        e.setInstallmentNumber(i.getInstallmentNumber());
        e.setPaymentAmount(i.getPaymentAmount());
        e.setPrincipalAmount(i.getPrincipalAmount());
        e.setInterestAmount(i.getInterestAmount());
        e.setRemainingBalance(i.getRemainingBalance());
        e.setDueDate(i.getDueDate());
        return e;
    }
}
