package com.cooperativa.met.infrastructure.persistence.legal.repository;

import com.cooperativa.met.infrastructure.persistence.legal.entity.MandateContractJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MandateContractJpaRepository extends JpaRepository<MandateContractJpaEntity, UUID> {
    Optional<MandateContractJpaEntity> findByUserId(UUID userId);
}
