package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.application.compliance.dto.ComplianceAlertResponse;
import com.cooperativa.met.application.compliance.dto.RestrictiveListMatchResponse;
import com.cooperativa.met.domain.compliance.model.AlertSeverity;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.AlertType;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;
import com.cooperativa.met.domain.compliance.port.ComplianceAlertRepositoryPort;
import com.cooperativa.met.domain.identity.model.ComplianceCheckRecord;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Las dos bandejas del oficial de cumplimiento: alertas SARLAFT y coincidencias en listas restrictivas. */
@ExtendWith(MockitoExtension.class)
class ListComplianceAlertsUseCaseTest {

    @Mock
    private ComplianceAlertRepositoryPort alertRepository;
    @Mock
    private ComplianceCheckPort complianceCheckPort;
    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private ListComplianceAlertsUseCase listComplianceAlertsUseCase;
    @InjectMocks
    private ListRestrictiveListMatchesUseCase listRestrictiveListMatchesUseCase;

    private final UUID knownUserId = UUID.randomUUID();
    private final UUID deletedUserId = UUID.randomUUID();

    @Test
    void shouldShowWhoEachAlertIsAboutEvenIfTheUserWasDeleted() {
        when(alertRepository.findByStatus(AlertStatus.OPEN, 0, 20)).thenReturn(List.of(alert(knownUserId), alert(deletedUserId)));
        givenUsers();

        List<ComplianceAlertResponse> alerts = listComplianceAlertsUseCase.execute(AlertStatus.OPEN, 0, 20);

        assertEquals("Ana Pérez", alerts.get(0).userFullName());
        assertEquals("52123456", alerts.get(0).userDocumentNumber());
        assertEquals("Usuario eliminado", alerts.get(1).userFullName());
        assertEquals("-", alerts.get(1).userDocumentNumber());
    }

    @Test
    void shouldListRestrictiveListMatchesWithTheMatchedPerson() {
        when(complianceCheckPort.findByResult(ComplianceResult.MATCH, 0, 20)).thenReturn(List.of(
                match(knownUserId, ComplianceListType.OFAC), match(deletedUserId, ComplianceListType.ONU)));
        givenUsers();

        List<RestrictiveListMatchResponse> matches = listRestrictiveListMatchesUseCase.execute(0, 20);

        assertEquals("OFAC", matches.get(0).listType());
        assertEquals("Ana Pérez", matches.get(0).userFullName());
        assertEquals("Usuario eliminado", matches.get(1).userFullName());
    }

    private void givenUsers() {
        when(userRepository.findById(knownUserId)).thenReturn(Optional.of(User.builder()
                .id(knownUserId).firstName("Ana").lastName("Pérez").documentNumber("52123456").build()));
        when(userRepository.findById(deletedUserId)).thenReturn(Optional.empty());
    }

    private ComplianceAlert alert(UUID userId) {
        return ComplianceAlert.builder().id(UUID.randomUUID()).userId(userId).alertType(AlertType.UNUSUAL_AMOUNT)
                .severity(AlertSeverity.HIGH).status(AlertStatus.OPEN).createdAt(Instant.now()).build();
    }

    private ComplianceCheckRecord match(UUID userId, ComplianceListType listType) {
        return ComplianceCheckRecord.builder().id(UUID.randomUUID()).userId(userId).listType(listType)
                .result(ComplianceResult.MATCH).checkedAt(Instant.now()).build();
    }
}
