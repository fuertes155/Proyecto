package com.cooperativa.fintech.infrastructure.persistence.identity.repository;

import com.cooperativa.fintech.infrastructure.persistence.identity.entity.ComplianceCheckJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplianceCheckJpaRepository extends JpaRepository<ComplianceCheckJpaEntity, UUID> {
}
