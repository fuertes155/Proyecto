package com.cooperativa.met.application.admin.usecase;

import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import com.cooperativa.met.domain.admin.port.AdminAuditLogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAuditLogUseCase {

    private final AdminAuditLogPort auditLog;

    public List<AdminAuditEntry> getAll(int page, int pageSize) {
        return auditLog.findAll(page, pageSize);
    }

    public List<AdminAuditEntry> getByAdmin(String adminId, int page, int pageSize) {
        try {
            return auditLog.findByAdminId(java.util.UUID.fromString(adminId), page, pageSize);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    public List<AdminAuditEntry> getByEntidad(String entidad, String idEntidad, int page, int pageSize) {
        return auditLog.findByEntidad(entidad, idEntidad, page, pageSize);
    }

    public long count() {
        return auditLog.countAll();
    }
}
