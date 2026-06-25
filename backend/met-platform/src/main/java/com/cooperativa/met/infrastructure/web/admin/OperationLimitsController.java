package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.OperationLimitRequest;
import com.cooperativa.met.application.admin.usecase.ManageOperationLimitsUseCase;
import com.cooperativa.met.domain.admin.model.OperationLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/limits")
@RequiredArgsConstructor
public class OperationLimitsController {

    private final ManageOperationLimitsUseCase manageOperationLimitsUseCase;

    @GetMapping
    public ResponseEntity<List<OperationLimit>> getAll() {
        return ResponseEntity.ok(manageOperationLimitsUseCase.getAll());
    }

    @PutMapping
    public ResponseEntity<OperationLimit> update(
            @Valid @RequestBody OperationLimitRequest request,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(manageOperationLimitsUseCase.update(adminId, request, http.getRemoteAddr()));
    }
}
