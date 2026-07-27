package com.cooperativa.met.infrastructure.scheduler;

import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogRetentionJob {

    private final AuditLogService auditLogService;
    private final AdminAuditLogPort adminAuditLogPort;
    
    // Configuramos a 90 días por defecto
    private static final int RETENTION_DAYS = 90;

    /**
     * Se ejecuta todos los días a las 3:00 AM para purgar registros de auditoría antiguos.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeOldAuditLogs() {
        log.info("Iniciando purga programada de logs de auditoría (más de {} días)...", RETENTION_DAYS);
        
        // 1. Purgar tabla audit_log
        int deletedUserLogs = auditLogService.purgeOldLogs(RETENTION_DAYS);
        
        // 2. Purgar tabla admin_audit_log
        Instant threshold = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        adminAuditLogPort.purgeOldLogs(threshold);
        
        log.info("Purga finalizada. Se eliminaron {} registros de audit_log y registros antiguos de admin_audit_log.", deletedUserLogs);
    }
}
