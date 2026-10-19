package com.cooperativa.met.infrastructure.persistence.bank.repository;

import com.cooperativa.met.infrastructure.persistence.bank.entity.ExternalBankAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExternalBankAccountJpaRepository extends JpaRepository<ExternalBankAccountJpaEntity, UUID> {
    List<ExternalBankAccountJpaEntity> findByUserIdAndActiveTrue(UUID userId);
}
