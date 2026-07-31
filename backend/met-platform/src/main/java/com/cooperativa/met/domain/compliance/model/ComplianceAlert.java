package com.cooperativa.met.domain.compliance.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Alerta de operación inusual (control SARLAFT). A diferencia de las reglas
 * de fraude ({@code FraudDetectionService}), esto NO bloquea la operación —
 * la deja pasar y queda registrada para que un oficial de cumplimiento la
 * revise y decida si amerita un Reporte de Operación Sospechosa (ROS).
 */
@Getter
@Builder(toBuilder = true)
public class ComplianceAlert {

    private final UUID id;
    private final UUID userId;
    private final UUID transactionId;
    private final AlertType alertType;
    private final AlertSeverity severity;
    private final String description;
    private final AlertStatus status;
    private final Instant createdAt;
    private final UUID reviewedByAdminId;
    private final Instant reviewedAt;
    private final String resolutionNotes;

    public ComplianceAlert review(UUID adminId, AlertStatus newStatus, String notes) {
        return this.toBuilder()
                .status(newStatus)
                .reviewedByAdminId(adminId)
                .reviewedAt(Instant.now())
                .resolutionNotes(notes)
                .build();
    }
}
