package com.cooperativa.met.application.compliance.dto;

import com.cooperativa.met.domain.compliance.model.ComplianceAlert;

import java.time.Instant;
import java.util.UUID;

public record ComplianceAlertResponse(
        UUID id,
        UUID userId,
        String userFullName,
        String userDocumentNumber,
        UUID transactionId,
        String alertType,
        String severity,
        String description,
        String status,
        Instant createdAt,
        UUID reviewedByAdminId,
        Instant reviewedAt,
        String resolutionNotes
) {
    public static ComplianceAlertResponse from(ComplianceAlert alert, String userFullName, String userDocumentNumber) {
        return new ComplianceAlertResponse(
                alert.getId(),
                alert.getUserId(),
                userFullName,
                userDocumentNumber,
                alert.getTransactionId(),
                alert.getAlertType().name(),
                alert.getSeverity().name(),
                alert.getDescription(),
                alert.getStatus().name(),
                alert.getCreatedAt(),
                alert.getReviewedByAdminId(),
                alert.getReviewedAt(),
                alert.getResolutionNotes()
        );
    }
}
