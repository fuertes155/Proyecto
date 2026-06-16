package com.cooperativa.met.infrastructure.persistence.solidarity.entity;

import com.cooperativa.met.domain.solidarity.model.GroupStatus;
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
import java.util.UUID;

@Entity
@Table(name = "solidarity_groups")
@Getter
@Setter
public class SolidarityGroupJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "invite_code", nullable = false, length = 8)
    private String inviteCode;

    @Column(name = "min_contribution", nullable = false, precision = 18, scale = 2)
    private BigDecimal minContribution;

    @Column(name = "max_loan_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxLoanPercentage;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "pool_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal poolBalance;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
