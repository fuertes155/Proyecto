package com.cooperativa.met.domain.admin.port;

import com.cooperativa.met.domain.admin.model.AdminAuditEntry;

import java.util.List;
import java.util.UUID;

/**
 * Puerto para registrar y consultar el log de auditoría administrativa.
 * Toda acción del panel admin DEBE llamar a log().
 */
public interface AdminAuditLogPort {

    void log(AdminAuditEntry entry);

    /**
     * @param page     página (0-indexed)
     * @param pageSize número de registros por página
     */
    List<AdminAuditEntry> findAll(int page, int pageSize);

    List<AdminAuditEntry> findByAdminId(UUID adminId, int page, int pageSize);

    List<AdminAuditEntry> findByEntidad(String entidadAfectada, String idEntidad, int page, int pageSize);

    long countAll();
    
    void purgeOldLogs(java.time.Instant threshold);
}
