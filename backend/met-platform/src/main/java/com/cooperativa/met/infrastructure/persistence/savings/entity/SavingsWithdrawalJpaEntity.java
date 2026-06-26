package com.cooperativa.met.infrastructure.persistence.savings.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "savings_withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsWithdrawalJpaEntity {
    @Id
    private UUID id;
    private UUID accountId;
    private UUID userId;
    private BigDecimal amount;
    private String withdrawalType;
    private Instant createdAt;
}
