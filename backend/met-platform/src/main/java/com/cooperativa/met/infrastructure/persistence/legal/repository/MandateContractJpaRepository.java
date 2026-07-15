package com.cooperativa.met.infrastructure.persistence.legal.repository;

import com.cooperativa.met.infrastructure.persistence.legal.entity.MandateContractJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MandateContractJpaRepository extends JpaRepository<MandateContractJpaEntity, UUID> {
    Optional<MandateContractJpaEntity> findByUserId(UUID userId);
}
