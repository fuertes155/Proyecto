package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.MaintenanceWindowRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.MaintenanceWindow;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.MaintenanceRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageMaintenanceUseCaseTest {

    private static final String IP = "181.50.10.2";

    @Mock
    private MaintenanceRepositoryPort maintenanceRepository;
    @Mock
    private AdminAuditLogPort auditLog;

    @InjectMocks
    private ManageMaintenanceUseCase manageMaintenanceUseCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID windowId = UUID.randomUUID();

    @Test
    void shouldScheduleWindowInactiveUntilAnAdminActivatesIt() {
        Instant inicio = Instant.now().plus(1, ChronoUnit.DAYS);
        when(maintenanceRepository.save(any())).thenAnswer(inv -> ((MaintenanceWindow) inv.getArgument(0)).toBuilder().id(windowId).build());

        MaintenanceWindow saved = manageMaintenanceUseCase.schedule(adminId, new MaintenanceWindowRequest(
                "Migración de BD", inicio, inicio.plus(2, ChronoUnit.HOURS), List.of("/v1/health")), IP);

        assertFalse(saved.isActivo());
        assertEquals(List.of("/v1/health"), saved.getEndpointsActivos());
        AdminAuditEntry entry = auditEntry();
        assertEquals("MAINTENANCE_SCHEDULED", entry.getAccion());
        assertEquals(windowId.toString(), entry.getIdEntidad());
    }

    @Test
    void shouldActivateAndDeactivateWithAudit() {
        when(maintenanceRepository.findById(windowId)).thenReturn(Optional.of(window(false)));
        when(maintenanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(manageMaintenanceUseCase.activate(adminId, windowId, IP).isActivo());
        assertFalse(manageMaintenanceUseCase.deactivate(adminId, windowId, IP).isActivo());

        ArgumentCaptor<AdminAuditEntry> entries = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog, org.mockito.Mockito.times(2)).log(entries.capture());
        assertEquals(List.of("MAINTENANCE_ACTIVATED", "MAINTENANCE_DEACTIVATED"),
                entries.getAllValues().stream().map(AdminAuditEntry::getAccion).toList());
    }

    @Test
    void shouldFailForUnknownWindow() {
        when(maintenanceRepository.findById(windowId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> manageMaintenanceUseCase.activate(adminId, windowId, IP));

        verify(maintenanceRepository, never()).save(any());
        verifyNoInteractions(auditLog);
    }

    private MaintenanceWindow window(boolean activo) {
        Instant now = Instant.now();
        return MaintenanceWindow.builder().id(windowId).descripcion("Migración").inicio(now).fin(now.plusSeconds(7200)).activo(activo).build();
    }

    private AdminAuditEntry auditEntry() {
        ArgumentCaptor<AdminAuditEntry> entry = ArgumentCaptor.forClass(AdminAuditEntry.class);
        verify(auditLog).log(entry.capture());
        return entry.getValue();
    }
}
