package com.cooperativa.met.domain.investment.port;

import com.cooperativa.met.domain.investment.model.LedgerEntry;

import java.util.List;
import java.util.UUID;

public interface LedgerRepositoryPort {
    LedgerEntry save(LedgerEntry entry);
    List<LedgerEntry> saveAll(List<LedgerEntry> entries);
    List<LedgerEntry> findByAccountId(UUID accountId);
    List<LedgerEntry> findByTransactionReference(UUID transactionReference);
}
