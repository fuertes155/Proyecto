package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.ComplianceAlertResponse;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;
import com.cooperativa.met.domain.compliance.port.ComplianceAlertRepositoryPort;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Un oficial de cumplimiento marca una alerta como revisada, descartada o reportable. */
@Service
@RequiredArgsConstructor
public class ReviewComplianceAlertUseCase {

    private final ComplianceAlertRepositoryPort alertRepository;
    private final UserRepositoryPort userRepository;
    private final AdminAuditLogPort auditLog;

    @Transactional
    public ComplianceAlertResponse execute(UUID actorAdminId, UUID alertId, AlertStatus newStatus, String notes, String ip) {
        ComplianceAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada: " + alertId));

        AlertStatus previousStatus = alert.getStatus();
        ComplianceAlert reviewed = alertRepository.save(alert.review(actorAdminId, newStatus, notes));

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("COMPLIANCE_ALERT_REVIEWED")
                .entidadAfectada("COMPLIANCE_ALERT")
                .idEntidad(alertId.toString())
                .valoresAnteriores(String.format("{\"status\":\"%s\"}", previousStatus))
                .valoresNuevos(String.format("{\"status\":\"%s\"}", newStatus))
                .motivo(notes)
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        User user = userRepository.findById(reviewed.getUserId()).orElse(null);
        String fullName = user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "Usuario eliminado";
        String documentNumber = user != null ? user.getDocumentNumber() : "-";
        return ComplianceAlertResponse.from(reviewed, fullName, documentNumber);
    }
}
