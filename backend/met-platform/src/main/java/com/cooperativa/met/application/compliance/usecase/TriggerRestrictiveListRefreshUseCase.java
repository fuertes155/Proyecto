package com.cooperativa.met.application.compliance.usecase;

import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Un admin fuerza el refresco manual de las listas restrictivas (fuera del cron diario). */
@Service
@RequiredArgsConstructor
public class TriggerRestrictiveListRefreshUseCase {

    private final RefreshRestrictiveListsUseCase refreshUseCase;
    private final AdminAuditLogPort auditLog;

    public Map<ComplianceListType, RefreshRestrictiveListsUseCase.RefreshResult> execute(UUID actorAdminId, String ip) {
        Map<ComplianceListType, RefreshRestrictiveListsUseCase.RefreshResult> results = refreshUseCase.execute();

        String summary = results.entrySet().stream()
                .map(e -> String.format("\"%s\":{\"success\":%s,\"entries\":%d}",
                        e.getKey(), e.getValue().success(), e.getValue().entriesLoaded()))
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        auditLog.log(AdminAuditEntry.builder()
                .actorAdminId(actorAdminId)
                .accion("RESTRICTIVE_LIST_REFRESH_TRIGGERED")
                .entidadAfectada("RESTRICTIVE_LIST")
                .valoresNuevos("{" + summary + "}")
                .motivo("Refresco manual desde el panel admin")
                .ipOrigen(ip)
                .timestamp(Instant.now())
                .build());

        return results;
    }
}
