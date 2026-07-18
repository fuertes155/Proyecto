package com.cooperativa.met.domain.account.port;

import com.cooperativa.met.domain.account.model.CoreTransaction;

import java.util.List;
import java.util.UUID;

public interface CoreTransactionRepositoryPort {
    CoreTransaction save(CoreTransaction transaction);
    List<CoreTransaction> findByAccountId(UUID accountId);
    java.math.BigDecimal sumOutgoingTransfersByAccountIdAndDateRange(UUID accountId, java.time.Instant start, java.time.Instant end);
}
