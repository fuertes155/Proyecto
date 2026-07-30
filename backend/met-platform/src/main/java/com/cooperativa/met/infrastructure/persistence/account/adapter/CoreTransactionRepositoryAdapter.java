package com.cooperativa.met.infrastructure.persistence.account.adapter;

import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.account.entity.CoreTransactionJpaEntity;
import com.cooperativa.met.infrastructure.persistence.account.repository.CoreTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CoreTransactionRepositoryAdapter implements CoreTransactionRepositoryPort {

    private final CoreTransactionJpaRepository repository;

    @Override
    public CoreTransaction save(CoreTransaction transaction) {
        CoreTransactionJpaEntity entity = CoreTransactionJpaEntity.fromDomain(transaction);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<CoreTransaction> findById(UUID id) {
        return repository.findById(id).map(CoreTransactionJpaEntity::toDomain);
    }

    @Override
    public List<CoreTransaction> findByAccountId(UUID accountId) {
        return repository.findByAccountId(accountId).stream()
                .map(CoreTransactionJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public java.math.BigDecimal sumOutgoingTransfersByAccountIdAndDateRange(UUID accountId, java.time.Instant start, java.time.Instant end) {
        return repository.sumOutgoingTransfers(accountId, start, end);
    }

    @Override
    public java.math.BigDecimal sumOutgoingByAccountIdAndTypeAndDateRange(UUID accountId, TransactionType type, java.time.Instant start, java.time.Instant end) {
        return repository.sumOutgoingByAccountIdAndType(accountId, type, start, end);
    }
}
