package com.cooperativa.met.infrastructure.persistence.savings.entity;

import com.cooperativa.met.domain.savings.model.ContributionFrequency;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "scheduled_savings_accounts")
@Getter
@Setter
public class ScheduledSavingsAccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "target_amount", precision = 18, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "contribution_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal contributionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionFrequency frequency;

    @Column(name = "debit_day_of_week")
    private Integer debitDayOfWeek;

    @Column(name = "debit_day_of_month")
    private Integer debitDayOfMonth;

    @Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduledSavingsStatus status;

    @Column(name = "next_contribution_date", nullable = false)
    private LocalDate nextContributionDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
