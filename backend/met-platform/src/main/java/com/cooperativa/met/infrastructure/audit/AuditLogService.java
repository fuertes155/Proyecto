package com.cooperativa.met.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Servicio de auditoría para registrar operaciones sensibles de forma asíncrona.
 *
 * <p>Persiste en la tabla {@code audit_log} sin bloquear el flujo principal.
 * Si falla, solo emite un log de advertencia para no interrumpir la operación del usuario.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final JdbcTemplate jdbc;

    // ─── Constantes de acción ───────────────────────────────────────────────
    public static final String TRANSFER_EXECUTED    = "TRANSFER_EXECUTED";
    public static final String TRANSFER_FAILED      = "TRANSFER_FAILED";
    public static final String PIN_CHANGED          = "PIN_CHANGED";
    public static final String PIN_RECOVERY_REQUEST = "PIN_RECOVERY_REQUEST";
    public static final String LOGIN_FAILED         = "LOGIN_FAILED";
    public static final String LOGIN_SUCCESS        = "LOGIN_SUCCESS";
    public static final String EMAIL_VERIFIED       = "EMAIL_VERIFIED";
    public static final String LOAN_SUBMITTED       = "LOAN_SUBMITTED";
    public static final String ADMIN_LOGIN          = "ADMIN_LOGIN";
    public static final String PROFILE_UPDATED      = "PROFILE_UPDATED";

    /**
     * Registra un evento de auditoría de forma asíncrona (no bloquea el hilo principal).
     *
     * @param userId     ID del usuario (puede ser null para acciones anónimas)
     * @param action     Constante de acción (ver constantes de esta clase)
     * @param entityType Tipo de entidad afectada (ej. "TRANSFER", "USER")
     * @param entityId   ID de la entidad afectada
     * @param details    JSON o texto libre con contexto adicional
     * @param result     "SUCCESS" o "FAILURE"
     */
    @Async
    public void log(UUID userId, String action, String entityType, String entityId,
                    String details, String result) {
        try {
            String ip = resolveClientIp();
            jdbc.update(
                    """
                    INSERT INTO audit_log (user_id, action, entity_type, entity_id, ip_address, details, result)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    userId, action, entityType, entityId, ip, details, result
            );
        } catch (Exception ex) {
            // La auditoría no debe interrumpir la operación principal
            log.warn("No se pudo registrar evento de auditoría: action={} userId={} error={}",
                    action, userId, ex.getMessage());
        }
    }

    /** Atajo para registrar un evento exitoso */
    @Async
    public void logSuccess(UUID userId, String action, String entityType, String entityId, String details) {
        log(userId, action, entityType, entityId, details, "SUCCESS");
    }

    /** Atajo para registrar un evento fallido */
    @Async
    public void logFailure(UUID userId, String action, String entityType, String entityId, String details) {
        log(userId, action, entityType, entityId, details, "FAILURE");
    }

    /**
     * Purga los registros de auditoría que tengan más de 'days' días de antigüedad.
     * @param days límite de días (ej. 90)
     * @return cantidad de registros eliminados
     */
    public int purgeOldLogs(int days) {
        try {
            return jdbc.update(
                    "DELETE FROM audit_log WHERE created_at < NOW() - CAST(? || ' days' AS INTERVAL)",
                    days
            );
        } catch (Exception ex) {
            log.error("Error al purgar audit_log antiguo: {}", ex.getMessage());
            return 0;
        }
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "N/A";
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
            String xri = req.getHeader("X-Real-IP");
            if (xri != null && !xri.isBlank()) return xri.trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "N/A";
        }
    }
}
