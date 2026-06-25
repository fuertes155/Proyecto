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
}
