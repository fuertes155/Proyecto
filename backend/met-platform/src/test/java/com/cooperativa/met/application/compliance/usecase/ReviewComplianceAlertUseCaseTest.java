package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.ComplianceAlertResponse;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.compliance.model.AlertSeverity;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.AlertType;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;
import com.cooperativa.met.domain.compliance.port.ComplianceAlertRepositoryPort;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewComplianceAlertUseCaseTest {

    @Mock
    private ComplianceAlertRepositoryPort alertRepository;
    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private AdminAuditLogPort auditLog;

    @InjectMocks
    private ReviewComplianceAlertUseCase reviewComplianceAlertUseCase;

    private final UUID officerId = UUID.randomUUID();
    private final UUID alertId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldRecordWhoReviewedTheAlertAndWhy() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(openAlert()));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId).firstName("Ana").lastName("Pérez").documentNumber("52123456").build()));

        ComplianceAlertResponse response = reviewComplianceAlertUseCase.execute(officerId, alertId,
                AlertStatus.REPORTED, "Reportado a la UIAF", "181.50.10.2");

        assertEquals("REPORTED", response.status());
        assertEquals(officerId, response.reviewedByAdminId());
        assertNotNull(response.reviewedAt());
        assertEquals("Reportado a la UIAF", response.resolutionNotes());
        assertEquals("Ana Pérez", response.userFullName());
        assertEquals("52123456", response.userDocumentNumber());

        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        assertEquals("COMPLIANCE_ALERT_REVIEWED", entry.getValue().getAccion());
        assertEquals("{\"status\":\"OPEN\"}", entry.getValue().getValoresAnteriores());
        assertEquals("{\"status\":\"REPORTED\"}", entry.getValue().getValoresNuevos());
    }

    @Test
    void shouldStillAnswerWhenTheUserNoLongerExists() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(openAlert()));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ComplianceAlertResponse response = reviewComplianceAlertUseCase.execute(officerId, alertId,
                AlertStatus.DISMISSED, "Falso positivo", "181.50.10.2");

        assertEquals("Usuario eliminado", response.userFullName());
        assertEquals("-", response.userDocumentNumber());
    }

    @Test
    void shouldFailForUnknownAlert() {
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewComplianceAlertUseCase.execute(officerId, alertId,
                AlertStatus.DISMISSED, "x", "181.50.10.2"));

        verifyNoInteractions(auditLog);
    }

    private ComplianceAlert openAlert() {
        return ComplianceAlert.builder()
                .id(alertId)
                .userId(userId)
                .transactionId(UUID.randomUUID())
                .alertType(AlertType.STRUCTURING_PATTERN)
                .severity(AlertSeverity.HIGH)
                .description("3 operaciones en 24h justo debajo del umbral")
                .status(AlertStatus.OPEN)
                .createdAt(Instant.now())
                .build();
    }
}
