package com.cooperativa.met.infrastructure.persistence.investment.repository;

import com.cooperativa.met.infrastructure.persistence.investment.entity.LedgerEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {
    List<LedgerEntryJpaEntity> findByAccountId(UUID accountId);
    List<LedgerEntryJpaEntity> findByTransactionReference(UUID transactionReference);
}
