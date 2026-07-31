package com.cooperativa.met.infrastructure.persistence.compliance.repository;

import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.infrastructure.persistence.compliance.entity.ComplianceAlertJpaEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceAlertJpaRepository extends JpaRepository<ComplianceAlertJpaEntity, UUID> {

    List<ComplianceAlertJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByStatus(AlertStatus status);

    default List<ComplianceAlertJpaEntity> findByStatusPaged(AlertStatus status, int page, int pageSize) {
        return findByStatus(status, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    List<ComplianceAlertJpaEntity> findByStatus(AlertStatus status, org.springframework.data.domain.Pageable pageable);
}
