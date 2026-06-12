package com.cooperativa.fintech.infrastructure.persistence.lending.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "personal_loan_amortization")
@Getter
@Setter
public class PersonalLoanAmortizationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "payment_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "remaining_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal remainingBalance;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
}
