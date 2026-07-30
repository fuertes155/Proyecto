package com.cooperativa.met.infrastructure.persistence.account.repository;

import com.cooperativa.met.infrastructure.persistence.account.entity.CoreTransactionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CoreTransactionJpaRepository extends JpaRepository<CoreTransactionJpaEntity, UUID> {
    
    // Optimización: Agregamos una versión paginada. Esto evita problemas de memoria si la cuenta
    // tiene miles de transacciones y permite cargas diferidas en el frontend.
    @Query("SELECT t FROM CoreTransactionJpaEntity t " +
           "WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId " +
           "ORDER BY t.createdAt DESC")
    Page<CoreTransactionJpaEntity> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    // Mantenemos la original para compatibilidad con código existente (Backward Compatibility)
    @Query("SELECT t FROM CoreTransactionJpaEntity t " +
           "WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId " +
           "ORDER BY t.createdAt DESC")
    List<CoreTransactionJpaEntity> findByAccountId(@Param("accountId") UUID accountId);

    // Optimización: Uso de imports explícitos, operador BETWEEN y @Param para seguridad en los bindings
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CoreTransactionJpaEntity t " +
           "WHERE t.sourceAccountId = :accountId AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumOutgoingTransfers(
        @Param("accountId") UUID accountId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CoreTransactionJpaEntity t " +
           "WHERE t.sourceAccountId = :accountId AND t.type = :type AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumOutgoingByAccountIdAndType(
        @Param("accountId") UUID accountId,
        @Param("type") com.cooperativa.met.domain.account.model.TransactionType type,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
}
