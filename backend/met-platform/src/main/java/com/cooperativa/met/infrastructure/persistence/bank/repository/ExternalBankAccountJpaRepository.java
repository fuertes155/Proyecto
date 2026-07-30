package com.cooperativa.met.infrastructure.persistence.bank.repository;

import com.cooperativa.met.infrastructure.persistence.bank.entity.ExternalBankAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExternalBankAccountJpaRepository extends JpaRepository<ExternalBankAccountJpaEntity, UUID> {
    List<ExternalBankAccountJpaEntity> findByUserIdAndActiveTrue(UUID userId);
}
