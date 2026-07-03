package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.FeeScheduleRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageFeesUseCase {

    private final FeeScheduleRepositoryPort feeRepository;
    private final AdminAuditLogPort auditLog;

    public List<FeeSchedule> getAll() {
        return feeRepository.findAll();
    }

    public List<FeeSchedule> getVigentes() {
        return feeRepository.findVigentes();
    }

    /**
     * Crea una nueva versión de tarifa.
     * Cierra automáticamente la vigencia de la tarifa anterior del mismo tipo.
     */
    @Transactional
    public FeeSchedule create(UUID actorAdminId, FeeScheduleRequest request, String ip) {
        // Cerrar vigencia de la versión anterior (versionado)
        feeRepository.cerrarVigencia(request.tipoTarifa());

        FeeSchedule newFee = FeeSchedule.builder()
                .tipoTarifa(request.tipoTarifa())
                .descripcion(request.descripcion())
                .valor(request.valor())
                .esPorcentaje(request.esPorcentaje())
                .vigentDesde(request.vigentDesde())
                .vigentaHasta(null) // abierta hasta que se cree la siguiente versión
                .creadoPor(actorAdminId)
                .createdAt(Instant.now())
                .build();

        FeeSchedule saved = feeRepository.save(newFee);

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("FEE_SCHEDULE_CREATED")
                .entidadAfectada("FEE_SCHEDULE")
                .idEntidad(saved.getId().toString())
                .valoresNuevos(String.format(
                        "{\"tipoTarifa\":\"%s\",\"valor\":%s,\"vigentDesde\":\"%s\"}",
                        request.tipoTarifa(), request.valor(), request.vigentDesde()))
                .motivo("Nueva versión de tarifa creada")
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return saved;
    }

    /**
     * Actualiza una tarifa existente: cierra la actual y crea una nueva versión.
     * Estrategia de versionado: se elimina la antigua y se crea la nueva con los
     * datos actualizados, manteniendo el mismo tipoTarifa.
     */
    @Transactional
    public FeeSchedule update(UUID actorAdminId, UUID feeId, FeeScheduleRequest request, String ip) {
        FeeSchedule existing = feeRepository.findById(feeId)
                .orElseThrow(() -> new IllegalArgumentException("Tarifa no encontrada: " + feeId));

        // Eliminar la versión anterior
        feeRepository.deleteById(feeId);

        // Crear nueva versión con los datos actualizados
        FeeSchedule updated = FeeSchedule.builder()
                .tipoTarifa(existing.getTipoTarifa())
                .descripcion(request.descripcion() != null ? request.descripcion() : existing.getDescripcion())
                .valor(request.valor())
                .esPorcentaje(request.esPorcentaje())
                .vigentDesde(request.vigentDesde())
                .vigentaHasta(null)
                .creadoPor(actorAdminId)
                .createdAt(Instant.now())
                .build();

        FeeSchedule saved = feeRepository.save(updated);

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("FEE_SCHEDULE_UPDATED")
                .entidadAfectada("FEE_SCHEDULE")
                .idEntidad(feeId.toString())
                .valoresAnteriores(String.format(
                        "{\"valor\":%s,\"esPorcentaje\":%b}", existing.getValor(), existing.isEsPorcentaje()))
                .valoresNuevos(String.format(
                        "{\"valor\":%s,\"esPorcentaje\":%b}", request.valor(), request.esPorcentaje()))
                .motivo("Tarifa actualizada")
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return saved;
    }

    /**
     * Elimina una tarifa permanentemente del sistema.
     */
    @Transactional
    public void delete(UUID actorAdminId, UUID feeId, String ip) {
        FeeSchedule existing = feeRepository.findById(feeId)
                .orElseThrow(() -> new IllegalArgumentException("Tarifa no encontrada: " + feeId));

        feeRepository.deleteById(feeId);

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("FEE_SCHEDULE_DELETED")
                .entidadAfectada("FEE_SCHEDULE")
                .idEntidad(feeId.toString())
                .valoresAnteriores(String.format(
                        "{\"tipoTarifa\":\"%s\",\"valor\":%s}", existing.getTipoTarifa(), existing.getValor()))
                .motivo("Tarifa eliminada por administrador")
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());
    }
}
