package com.cooperativa.met.infrastructure.persistence.bank.repository;

import com.cooperativa.met.infrastructure.persistence.bank.entity.ExternalPayoutJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExternalPayoutJpaRepository extends JpaRepository<ExternalPayoutJpaEntity, UUID> {
    Optional<ExternalPayoutJpaEntity> findByCoreTransactionId(UUID coreTransactionId);
    Optional<ExternalPayoutJpaEntity> findByRailReference(String railReference);
}
