package com.cooperativa.met.domain.compliance.port;

import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceAlertRepositoryPort {

    ComplianceAlert save(ComplianceAlert alert);

    Optional<ComplianceAlert> findById(UUID id);

    List<ComplianceAlert> findByStatus(AlertStatus status, int page, int pageSize);

    List<ComplianceAlert> findByUserId(UUID userId);

    long countByStatus(AlertStatus status);
}
