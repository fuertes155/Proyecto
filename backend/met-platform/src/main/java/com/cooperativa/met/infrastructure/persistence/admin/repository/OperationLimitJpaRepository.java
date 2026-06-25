package com.cooperativa.met.infrastructure.persistence.admin.repository;

import com.cooperativa.met.infrastructure.persistence.admin.entity.OperationLimitJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperationLimitJpaRepository extends JpaRepository<OperationLimitJpaEntity, UUID> {
    Optional<OperationLimitJpaEntity> findByTipoOperacion(String tipoOperacion);
}
