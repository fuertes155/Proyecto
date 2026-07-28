package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.EmergencyLockRequest;
import com.cooperativa.met.application.admin.usecase.EmergencyLockUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/emergency-lock")
@RequiredArgsConstructor
public class EmergencyLockController {

    private final EmergencyLockUseCase emergencyLockUseCase;

    @PostMapping
    public ResponseEntity<Map<String, String>> lock(
            @Valid @RequestBody EmergencyLockRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        UUID adminId = UUID.fromString(authentication.getName());
        emergencyLockUseCase.execute(adminId, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "Bloqueo de emergencia ejecutado correctamente",
                "scope", request.scope(),
                "targetId", request.targetId()
        ));
    }
}
