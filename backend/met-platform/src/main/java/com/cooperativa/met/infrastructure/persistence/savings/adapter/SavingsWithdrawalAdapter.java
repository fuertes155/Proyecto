package com.cooperativa.met.infrastructure.persistence.savings.adapter;

import com.cooperativa.met.domain.savings.model.SavingsWithdrawal;
import com.cooperativa.met.domain.savings.model.WithdrawalType;
import com.cooperativa.met.domain.savings.port.SavingsWithdrawalPort;
import com.cooperativa.met.infrastructure.persistence.savings.entity.SavingsWithdrawalJpaEntity;
import com.cooperativa.met.infrastructure.persistence.savings.repository.SavingsWithdrawalJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SavingsWithdrawalAdapter implements SavingsWithdrawalPort {

    private final SavingsWithdrawalJpaRepository repository;

    @Override
    public SavingsWithdrawal save(SavingsWithdrawal withdrawal) {
        SavingsWithdrawalJpaEntity entity = SavingsWithdrawalJpaEntity.builder()
                .id(withdrawal.getId())
                .accountId(withdrawal.getAccountId())
                .userId(withdrawal.getUserId())
                .amount(withdrawal.getAmount())
                .withdrawalType(withdrawal.getType().name())
                .createdAt(withdrawal.getCreatedAt())
                .build();
        
        SavingsWithdrawalJpaEntity saved = repository.save(entity);
        
        return SavingsWithdrawal.builder()
                .id(saved.getId())
                .accountId(saved.getAccountId())
                .userId(saved.getUserId())
                .amount(saved.getAmount())
                .type(WithdrawalType.valueOf(saved.getWithdrawalType()))
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
