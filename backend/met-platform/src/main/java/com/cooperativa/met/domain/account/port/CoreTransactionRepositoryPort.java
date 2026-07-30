package com.cooperativa.met.domain.account.port;

import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoreTransactionRepositoryPort {
    CoreTransaction save(CoreTransaction transaction);
    Optional<CoreTransaction> findById(UUID id);
    List<CoreTransaction> findByAccountId(UUID accountId);
    java.math.BigDecimal sumOutgoingTransfersByAccountIdAndDateRange(UUID accountId, java.time.Instant start, java.time.Instant end);
    java.math.BigDecimal sumOutgoingByAccountIdAndTypeAndDateRange(UUID accountId, TransactionType type, java.time.Instant start, java.time.Instant end);
}
