package com.cooperativa.met.infrastructure.persistence.investment.adapter;

import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.model.LedgerEntryType;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.investment.entity.LedgerEntryJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.LedgerEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LedgerEntryRepositoryAdapter implements LedgerRepositoryPort {

    private final LedgerEntryJpaRepository repository;

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        return toModel(repository.save(toEntity(entry)));
    }

    @Override
    public List<LedgerEntry> saveAll(List<LedgerEntry> entries) {
        List<LedgerEntryJpaEntity> entities = entries.stream().map(this::toEntity).toList();
        return repository.saveAll(entities).stream().map(this::toModel).toList();
    }

    @Override
    public List<LedgerEntry> findByAccountId(UUID accountId) {
        return repository.findByAccountId(accountId).stream().map(this::toModel).toList();
    }

    @Override
    public List<LedgerEntry> findByTransactionReference(UUID transactionReference) {
        return repository.findByTransactionReference(transactionReference).stream().map(this::toModel).toList();
    }

    private LedgerEntryJpaEntity toEntity(LedgerEntry model) {
        return LedgerEntryJpaEntity.builder()
                .id(model.getId())
                .accountId(model.getAccountId())
                .transactionReference(model.getTransactionReference())
                .entryType(model.getEntryType().name())
                .amount(model.getAmount())
                .concept(model.getConcept())
                .createdAt(model.getCreatedAt())
                .build();
    }

    private LedgerEntry toModel(LedgerEntryJpaEntity entity) {
        return LedgerEntry.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .transactionReference(entity.getTransactionReference())
                .entryType(LedgerEntryType.valueOf(entity.getEntryType()))
                .amount(entity.getAmount())
                .concept(entity.getConcept())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
