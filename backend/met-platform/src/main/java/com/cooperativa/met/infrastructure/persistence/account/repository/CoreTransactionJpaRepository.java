package com.cooperativa.met.infrastructure.persistence.account.repository;

import com.cooperativa.met.infrastructure.persistence.account.entity.CoreTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CoreTransactionJpaRepository extends JpaRepository<CoreTransactionJpaEntity, UUID> {
    
    @Query("SELECT t FROM CoreTransactionJpaEntity t WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId ORDER BY t.createdAt DESC")
    List<CoreTransactionJpaEntity> findByAccountId(UUID accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CoreTransactionJpaEntity t " +
           "WHERE t.sourceAccountId = :accountId AND t.createdAt >= :start AND t.createdAt <= :end")
    java.math.BigDecimal sumOutgoingTransfers(UUID accountId, java.time.Instant start, java.time.Instant end);
}
