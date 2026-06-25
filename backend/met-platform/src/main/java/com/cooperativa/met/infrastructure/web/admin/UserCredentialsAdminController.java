package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.ResetCredentialsRequest;
import com.cooperativa.met.application.admin.usecase.ResetUserCredentialsUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
public class UserCredentialsAdminController {

    private final ResetUserCredentialsUseCase resetUserCredentialsUseCase;

    @PostMapping("/{userId}/reset-credentials")
    public ResponseEntity<Map<String, String>> resetCredentials(
            @PathVariable UUID userId,
            @Valid @RequestBody ResetCredentialsRequest request,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        resetUserCredentialsUseCase.execute(adminId, userId, request, http.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "Credenciales del usuario reseteadas. El usuario deberá registrar nuevo PIN.",
                "userId", userId.toString()
        ));
    }
}
