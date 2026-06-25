package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.MaintenanceWindowRequest;
import com.cooperativa.met.application.admin.usecase.ManageMaintenanceUseCase;
import com.cooperativa.met.domain.admin.model.MaintenanceWindow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final ManageMaintenanceUseCase manageMaintenanceUseCase;

    @GetMapping
    public ResponseEntity<List<MaintenanceWindow>> getAll() {
        return ResponseEntity.ok(manageMaintenanceUseCase.getAll());
    }

    @GetMapping("/activa")
    public ResponseEntity<Optional<MaintenanceWindow>> getActiva() {
        return ResponseEntity.ok(manageMaintenanceUseCase.getActiva());
    }

    @PostMapping
    public ResponseEntity<MaintenanceWindow> schedule(
            @Valid @RequestBody MaintenanceWindowRequest request,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageMaintenanceUseCase.schedule(adminId, request, http.getRemoteAddr()));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<MaintenanceWindow> activate(
            @PathVariable UUID id, Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(manageMaintenanceUseCase.activate(adminId, id, http.getRemoteAddr()));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<MaintenanceWindow> deactivate(
            @PathVariable UUID id, Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(manageMaintenanceUseCase.deactivate(adminId, id, http.getRemoteAddr()));
    }
}
