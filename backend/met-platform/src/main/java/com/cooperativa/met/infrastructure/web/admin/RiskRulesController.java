package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.RiskRuleRequest;
import com.cooperativa.met.application.admin.usecase.ManageRiskRulesUseCase;
import com.cooperativa.met.domain.admin.model.RiskRule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/risk-rules")
@RequiredArgsConstructor
public class RiskRulesController {

    private final ManageRiskRulesUseCase manageRiskRulesUseCase;

    @GetMapping
    public ResponseEntity<List<RiskRule>> getAll() {
        return ResponseEntity.ok(manageRiskRulesUseCase.getAll());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<RiskRule>> getActivas() {
        return ResponseEntity.ok(manageRiskRulesUseCase.getActivas());
    }

    @PostMapping
    public ResponseEntity<RiskRule> create(
            @Valid @RequestBody RiskRuleRequest request,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageRiskRulesUseCase.create(adminId, request, http.getRemoteAddr()));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RiskRule> toggle(
            @PathVariable UUID id,
            @RequestParam boolean activo,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(manageRiskRulesUseCase.toggleActivo(adminId, id, activo, http.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable UUID id,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        manageRiskRulesUseCase.delete(adminId, id, http.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Regla de riesgo eliminada"));
    }
}
