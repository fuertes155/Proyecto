package com.cooperativa.met.infrastructure.persistence.admin.repository;

import com.cooperativa.met.infrastructure.persistence.admin.entity.MaintenanceWindowJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MaintenanceWindowJpaRepository extends JpaRepository<MaintenanceWindowJpaEntity, UUID> {
    Optional<MaintenanceWindowJpaEntity> findByActivoTrue();
}
