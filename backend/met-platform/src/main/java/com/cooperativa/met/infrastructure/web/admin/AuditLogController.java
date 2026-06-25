package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.usecase.GetAuditLogUseCase;
import com.cooperativa.met.domain.admin.model.AdminAuditEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final GetAuditLogUseCase getAuditLogUseCase;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AdminAuditEntry> entries = getAuditLogUseCase.getAll(page, size);
        long total = getAuditLogUseCase.count();
        return ResponseEntity.ok(Map.of(
                "data", entries,
                "total", total,
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<List<AdminAuditEntry>> getByAdmin(
            @PathVariable String adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(getAuditLogUseCase.getByAdmin(adminId, page, size));
    }

    @GetMapping("/entidad/{entidad}/{id}")
    public ResponseEntity<List<AdminAuditEntry>> getByEntidad(
            @PathVariable String entidad,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(getAuditLogUseCase.getByEntidad(entidad, id, page, size));
    }
}
