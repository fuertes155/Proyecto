package com.cooperativa.fintech.infrastructure.persistence.savings.mapper;

import com.cooperativa.fintech.domain.savings.model.ScheduledContribution;
import com.cooperativa.fintech.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.fintech.infrastructure.persistence.savings.entity.ScheduledContributionJpaEntity;
import com.cooperativa.fintech.infrastructure.persistence.savings.entity.ScheduledSavingsAccountJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ScheduledSavingsPersistenceMapper {

    public ScheduledSavingsAccount toDomain(ScheduledSavingsAccountJpaEntity entity) {
        return ScheduledSavingsAccount.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .targetAmount(entity.getTargetAmount())
                .contributionAmount(entity.getContributionAmount())
                .frequency(entity.getFrequency())
                .debitDayOfWeek(entity.getDebitDayOfWeek())
                .debitDayOfMonth(entity.getDebitDayOfMonth())
                .currentBalance(entity.getCurrentBalance())
                .status(entity.getStatus())
                .nextContributionDate(entity.getNextContributionDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ScheduledSavingsAccountJpaEntity toEntity(ScheduledSavingsAccount account) {
        ScheduledSavingsAccountJpaEntity entity = new ScheduledSavingsAccountJpaEntity();
        entity.setId(account.getId());
        entity.setUserId(account.getUserId());
        entity.setName(account.getName());
        entity.setTargetAmount(account.getTargetAmount());
        entity.setContributionAmount(account.getContributionAmount());
        entity.setFrequency(account.getFrequency());
        entity.setDebitDayOfWeek(account.getDebitDayOfWeek());
        entity.setDebitDayOfMonth(account.getDebitDayOfMonth());
        entity.setCurrentBalance(account.getCurrentBalance());
        entity.setStatus(account.getStatus());
        entity.setNextContributionDate(account.getNextContributionDate());
        entity.setCreatedAt(account.getCreatedAt());
        entity.setUpdatedAt(account.getUpdatedAt());
        return entity;
    }

    public ScheduledContribution toDomain(ScheduledContributionJpaEntity entity) {
        return ScheduledContribution.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .amount(entity.getAmount())
                .scheduledDate(entity.getScheduledDate())
                .executedAt(entity.getExecutedAt())
                .status(entity.getStatus())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ScheduledContributionJpaEntity toEntity(ScheduledContribution contribution) {
        ScheduledContributionJpaEntity entity = new ScheduledContributionJpaEntity();
        entity.setId(contribution.getId());
        entity.setAccountId(contribution.getAccountId());
        entity.setAmount(contribution.getAmount());
        entity.setScheduledDate(contribution.getScheduledDate());
        entity.setExecutedAt(contribution.getExecutedAt());
        entity.setStatus(contribution.getStatus());
        entity.setFailureReason(contribution.getFailureReason());
        entity.setCreatedAt(contribution.getCreatedAt());
        return entity;
    }
}
