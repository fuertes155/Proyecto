package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.compliance.dto.ComplianceAlertResponse;
import com.cooperativa.met.application.compliance.dto.ReviewAlertRequest;
import com.cooperativa.met.application.compliance.dto.RestrictiveListMatchResponse;
import com.cooperativa.met.application.compliance.usecase.ListComplianceAlertsUseCase;
import com.cooperativa.met.application.compliance.usecase.ListRestrictiveListMatchesUseCase;
import com.cooperativa.met.application.compliance.usecase.ReviewComplianceAlertUseCase;
import com.cooperativa.met.application.compliance.usecase.RefreshRestrictiveListsUseCase;
import com.cooperativa.met.application.compliance.usecase.TriggerRestrictiveListRefreshUseCase;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Panel de cumplimiento SARLAFT: alertas de operaciones inusuales y
 * coincidencias contra listas restrictivas (OFAC/ONU).
 */
@RestController
@RequestMapping("/v1/admin/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ListComplianceAlertsUseCase listAlertsUseCase;
    private final ReviewComplianceAlertUseCase reviewAlertUseCase;
    private final ListRestrictiveListMatchesUseCase listMatchesUseCase;
    private final TriggerRestrictiveListRefreshUseCase triggerRefreshUseCase;

    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AlertStatus alertStatus = parseStatus(status);
        List<ComplianceAlertResponse> alerts = listAlertsUseCase.execute(alertStatus, page, size);
        long total = listAlertsUseCase.countByStatus(alertStatus);
        return ResponseEntity.ok(Map.of("items", alerts, "total", total));
    }

    @PostMapping("/alerts/{id}/review")
    public ResponseEntity<ComplianceAlertResponse> reviewAlert(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewAlertRequest request,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        AlertStatus newStatus = parseStatus(request.status());
        return ResponseEntity.ok(reviewAlertUseCase.execute(adminId, id, newStatus, request.notes(), http.getRemoteAddr()));
    }

    @GetMapping("/restrictive-list-matches")
    public ResponseEntity<List<RestrictiveListMatchResponse>> getMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listMatchesUseCase.execute(page, size));
    }

    @PostMapping("/restrictive-lists/refresh")
    public ResponseEntity<Map<ComplianceListType, RefreshRestrictiveListsUseCase.RefreshResult>> refreshLists(
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(triggerRefreshUseCase.execute(adminId, http.getRemoteAddr()));
    }

    private AlertStatus parseStatus(String status) {
        try {
            return AlertStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_STATUS", "Estado inválido. Use: OPEN, UNDER_REVIEW, DISMISSED, REPORTED");
        }
    }
}
