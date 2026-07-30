package com.cooperativa.met.domain.bank.port;

import com.cooperativa.met.domain.bank.model.ExternalPayout;

import java.util.Optional;
import java.util.UUID;

public interface ExternalPayoutRepositoryPort {
    Optional<ExternalPayout> findById(UUID id);
    Optional<ExternalPayout> findByCoreTransactionId(UUID coreTransactionId);
    Optional<ExternalPayout> findByRailReference(String railReference);
    ExternalPayout save(ExternalPayout payout);
}
