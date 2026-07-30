package com.cooperativa.met.infrastructure.persistence.bank.adapter;

import com.cooperativa.met.domain.bank.model.ExternalPayout;
import com.cooperativa.met.domain.bank.port.ExternalPayoutRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.bank.entity.ExternalPayoutJpaEntity;
import com.cooperativa.met.infrastructure.persistence.bank.repository.ExternalPayoutJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExternalPayoutRepositoryAdapter implements ExternalPayoutRepositoryPort {

    private final ExternalPayoutJpaRepository repository;

    @Override
    public Optional<ExternalPayout> findById(UUID id) {
        return repository.findById(id).map(ExternalPayoutJpaEntity::toDomain);
    }

    @Override
    public Optional<ExternalPayout> findByCoreTransactionId(UUID coreTransactionId) {
        return repository.findByCoreTransactionId(coreTransactionId).map(ExternalPayoutJpaEntity::toDomain);
    }

    @Override
    public Optional<ExternalPayout> findByRailReference(String railReference) {
        return repository.findByRailReference(railReference).map(ExternalPayoutJpaEntity::toDomain);
    }

    @Override
    public ExternalPayout save(ExternalPayout payout) {
        return repository.save(ExternalPayoutJpaEntity.fromDomain(payout)).toDomain();
    }
}
