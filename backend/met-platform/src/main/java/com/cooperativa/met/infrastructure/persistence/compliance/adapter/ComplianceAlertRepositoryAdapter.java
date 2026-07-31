package com.cooperativa.met.infrastructure.persistence.compliance.adapter;

import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;
import com.cooperativa.met.domain.compliance.port.ComplianceAlertRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.compliance.entity.ComplianceAlertJpaEntity;
import com.cooperativa.met.infrastructure.persistence.compliance.repository.ComplianceAlertJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ComplianceAlertRepositoryAdapter implements ComplianceAlertRepositoryPort {

    private final ComplianceAlertJpaRepository repository;

    @Override
    public ComplianceAlert save(ComplianceAlert alert) {
        return toModel(repository.save(toEntity(alert)));
    }

    @Override
    public Optional<ComplianceAlert> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public List<ComplianceAlert> findByStatus(AlertStatus status, int page, int pageSize) {
        return repository.findByStatusPaged(status, page, pageSize).stream().map(this::toModel).toList();
    }

    @Override
    public List<ComplianceAlert> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toModel).toList();
    }

    @Override
    public long countByStatus(AlertStatus status) {
        return repository.countByStatus(status);
    }

    private ComplianceAlertJpaEntity toEntity(ComplianceAlert model) {
        ComplianceAlertJpaEntity entity = new ComplianceAlertJpaEntity();
        entity.setId(model.getId() != null ? model.getId() : UUID.randomUUID());
        entity.setUserId(model.getUserId());
        entity.setTransactionId(model.getTransactionId());
        entity.setAlertType(model.getAlertType());
        entity.setSeverity(model.getSeverity());
        entity.setDescription(model.getDescription());
        entity.setStatus(model.getStatus());
        entity.setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : Instant.now());
        entity.setReviewedByAdminId(model.getReviewedByAdminId());
        entity.setReviewedAt(model.getReviewedAt());
        entity.setResolutionNotes(model.getResolutionNotes());
        return entity;
    }

    private ComplianceAlert toModel(ComplianceAlertJpaEntity entity) {
        return ComplianceAlert.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .transactionId(entity.getTransactionId())
                .alertType(entity.getAlertType())
                .severity(entity.getSeverity())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .reviewedByAdminId(entity.getReviewedByAdminId())
                .reviewedAt(entity.getReviewedAt())
                .resolutionNotes(entity.getResolutionNotes())
                .build();
    }
}
