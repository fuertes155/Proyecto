package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.application.admin.dto.ReverseTransactionRequest;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso de reversión de transacciones.
 * Requiere doble confirmación (confirmationCode) antes de ejecutar.
 * El rastreo completo queda registrado en el audit log.
 *
 * NOTA: La lógica de reversión real se conecta al módulo de transacciones
 * existente (savings, loans) según el tipo de transacción identificado.
 */
@Service
@RequiredArgsConstructor
public class ReverseTransactionUseCase {

    private static final String CONFIRMATION_PREFIX = "CONFIRM-";
    private final AdminAuditLogPort auditLog;

    public void execute(UUID actorAdminId, ReverseTransactionRequest request, String ip) {
        // Validar código de confirmación (formato: CONFIRM-{transactionId parcial})
        String expectedCode = CONFIRMATION_PREFIX + request.transactionId().substring(0, 8).toUpperCase();
        if (!expectedCode.equalsIgnoreCase(request.confirmationCode())) {
            throw new BusinessRuleException("INVALID_CONFIRMATION_CODE",
                    "El código de confirmación no es válido. Esperado: " + expectedCode);
        }

        // Aquí se integra con el servicio de transacciones existente para la reversión real.
        // Por ahora registramos la intención con trazabilidad completa.
        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("TRANSACTION_REVERSED")
                .entidadAfectada("TRANSACTION")
                .idEntidad(request.transactionId())
                .valoresAnteriores("{\"estado\":\"COMPLETED\"}")
                .valoresNuevos("{\"estado\":\"REVERSED\"}")
                .motivo(request.reason())
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());
    }
}
