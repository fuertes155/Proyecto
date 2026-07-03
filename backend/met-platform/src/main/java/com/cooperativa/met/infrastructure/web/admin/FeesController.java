package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.FeeScheduleRequest;
import com.cooperativa.met.application.admin.usecase.ManageFeesUseCase;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/fees")
@RequiredArgsConstructor
public class FeesController {

    private final ManageFeesUseCase manageFeesUseCase;

    @GetMapping
    public ResponseEntity<List<FeeSchedule>> getAll() {
        return ResponseEntity.ok(manageFeesUseCase.getAll());
    }

    @GetMapping("/vigentes")
    public ResponseEntity<List<FeeSchedule>> getVigentes() {
        return ResponseEntity.ok(manageFeesUseCase.getVigentes());
    }

    @PostMapping
    public ResponseEntity<FeeSchedule> create(
            @Valid @RequestBody FeeScheduleRequest request,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageFeesUseCase.create(adminId, request, http.getRemoteAddr()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeeSchedule> update(
            @PathVariable UUID id,
            @Valid @RequestBody FeeScheduleRequest request,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(manageFeesUseCase.update(adminId, id, request, http.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        manageFeesUseCase.delete(adminId, id, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
