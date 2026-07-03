package com.cooperativa.met.infrastructure.web.identity;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.application.identity.dto.BiometricRegistrationRequest;
import com.cooperativa.met.application.identity.dto.LoginRequest;
import com.cooperativa.met.application.identity.dto.RegisterUserRequest;
import com.cooperativa.met.application.identity.dto.UserResponse;
import com.cooperativa.met.application.identity.usecase.GetUserProfileUseCase;
import com.cooperativa.met.application.identity.usecase.LoginUseCase;
import com.cooperativa.met.application.identity.usecase.RefreshTokenUseCase;
import com.cooperativa.met.application.identity.usecase.RegisterBiometricUseCase;
import com.cooperativa.met.application.identity.usecase.RegisterUserUseCase;
import com.cooperativa.met.application.identity.usecase.UpdateProfileUseCase;
import com.cooperativa.met.application.identity.usecase.UpdatePinUseCase;
import com.cooperativa.met.application.identity.usecase.UpdateNotificationsUseCase;
import com.cooperativa.met.application.identity.dto.UpdateProfileRequest;
import com.cooperativa.met.application.identity.dto.UpdatePinRequest;
import com.cooperativa.met.application.identity.dto.UpdateNotificationsRequest;
import org.springframework.web.bind.annotation.PutMapping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final RegisterBiometricUseCase registerBiometricUseCase;
    private final LoginUseCase loginUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final UpdatePinUseCase updatePinUseCase;
    private final UpdateNotificationsUseCase updateNotificationsUseCase;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerUserUseCase.execute(request));
    }

    @PostMapping("/biometric")
    public ResponseEntity<Map<String, String>> registerBiometric(
            @Valid @RequestBody BiometricRegistrationRequest request) {
        registerBiometricUseCase.execute(request);
        return ResponseEntity.ok(Map.of("message", "Registro biométrico completado"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @org.springframework.web.bind.annotation.RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        // Esperado: Authorization: Bearer <refreshToken>
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new com.cooperativa.met.domain.common.exception.BusinessRuleException(
                    "INVALID_TOKEN",
                    "Falta Authorization Bearer para refresh token"
            );
        }
        String refreshToken = authorization.substring(7);
        return ResponseEntity.ok(refreshTokenUseCase.execute(refreshToken));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(getUserProfileUseCase.execute(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(updateProfileUseCase.execute(userId, request));
    }

    @PutMapping("/pin")
    public ResponseEntity<Void> updatePin(
            Authentication authentication,
            @Valid @RequestBody UpdatePinRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        updatePinUseCase.execute(userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/notifications")
    public ResponseEntity<UserResponse> updateNotifications(
            Authentication authentication,
            @Valid @RequestBody UpdateNotificationsRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(updateNotificationsUseCase.execute(userId, request));
    }
}
