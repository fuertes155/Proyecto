package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.OperationLimitRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.model.OperationLimit;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.admin.port.OperationLimitRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageOperationLimitsUseCase {

    private final OperationLimitRepositoryPort limitRepository;
    private final AdminAuditLogPort auditLog;

    public List<OperationLimit> getAll() {
        return limitRepository.findAll();
    }

    @Transactional
    public OperationLimit update(UUID actorAdminId, OperationLimitRequest request, String ip) {
        OperationLimit existing = limitRepository.findByTipo(request.tipoOperacion())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Límite no encontrado para: " + request.tipoOperacion()));

        long prevDiario = existing.getMontoDiarioMax();
        long prevPorTx = existing.getMontoPorTransaccionMax();

        OperationLimit updated = limitRepository.save(
                existing.withLimits(request.montoDiarioMax(), request.montoPorTransaccionMax()));

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("OPERATION_LIMIT_UPDATED")
                .entidadAfectada("OPERATION_LIMIT")
                .idEntidad(request.tipoOperacion())
                .valoresAnteriores(String.format(
                        "{\"montoDiarioMax\":%d,\"montoPorTransaccionMax\":%d}", prevDiario, prevPorTx))
                .valoresNuevos(String.format(
                        "{\"montoDiarioMax\":%d,\"montoPorTransaccionMax\":%d}",
                        request.montoDiarioMax(), request.montoPorTransaccionMax()))
                .motivo("Actualización de límites de operación")
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return updated;
    }
}
