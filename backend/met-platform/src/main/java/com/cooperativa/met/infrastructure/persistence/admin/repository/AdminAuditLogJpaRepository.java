package com.cooperativa.met.infrastructure.persistence.admin.repository;

import com.cooperativa.met.infrastructure.persistence.admin.entity.AdminAuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminAuditLogJpaRepository extends JpaRepository<AdminAuditLogJpaEntity, UUID> {
    Page<AdminAuditLogJpaEntity> findByActorAdminId(UUID actorAdminId, Pageable pageable);
    Page<AdminAuditLogJpaEntity> findByEntidadAfectadaAndIdEntidad(String entidadAfectada, String idEntidad, Pageable pageable);
}
