package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.MaintenanceWindowRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.MaintenanceWindow;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.MaintenanceRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageMaintenanceUseCase {

    private final MaintenanceRepositoryPort maintenanceRepository;
    private final AdminAuditLogPort auditLog;

    public List<MaintenanceWindow> getAll() {
        return maintenanceRepository.findAll();
    }

    public Optional<MaintenanceWindow> getActiva() {
        return maintenanceRepository.findActiva();
    }

    @Transactional
    public MaintenanceWindow schedule(UUID actorAdminId, MaintenanceWindowRequest request, String ip) {
        MaintenanceWindow window = MaintenanceWindow.builder()
                .descripcion(request.descripcion())
                .inicio(request.inicio())
                .fin(request.fin())
                .activo(false)
                .endpointsActivos(request.endpointsActivos())
                .creadoPor(actorAdminId)
                .createdAt(Instant.now())
                .build();

        MaintenanceWindow saved = maintenanceRepository.save(window);

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("MAINTENANCE_SCHEDULED")
                .entidadAfectada("MAINTENANCE_WINDOW")
                .idEntidad(saved.getId().toString())
                .valoresNuevos(String.format("{\"inicio\":\"%s\",\"fin\":\"%s\"}", request.inicio(), request.fin()))
                .motivo(request.descripcion())
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return saved;
    }

    @Transactional
    public MaintenanceWindow activate(UUID actorAdminId, UUID windowId, String ip) {
        MaintenanceWindow window = maintenanceRepository.findById(windowId)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana de mantenimiento no encontrada"));
        MaintenanceWindow activated = maintenanceRepository.save(window.activate());

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("MAINTENANCE_ACTIVATED")
                .entidadAfectada("MAINTENANCE_WINDOW")
                .idEntidad(windowId.toString())
                .valoresAnteriores("{\"activo\":false}")
                .valoresNuevos("{\"activo\":true}")
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return activated;
    }

    @Transactional
    public MaintenanceWindow deactivate(UUID actorAdminId, UUID windowId, String ip) {
        MaintenanceWindow window = maintenanceRepository.findById(windowId)
                .orElseThrow(() -> new ResourceNotFoundException("Ventana de mantenimiento no encontrada"));
        MaintenanceWindow deactivated = maintenanceRepository.save(window.deactivate());

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("MAINTENANCE_DEACTIVATED")
                .entidadAfectada("MAINTENANCE_WINDOW")
                .idEntidad(windowId.toString())
                .valoresAnteriores("{\"activo\":true}")
                .valoresNuevos("{\"activo\":false}")
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return deactivated;
    }
}
