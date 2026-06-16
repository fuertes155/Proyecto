package com.cooperativa.met.infrastructure.web.identity;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.application.identity.dto.BiometricRegistrationRequest;
import com.cooperativa.met.application.identity.dto.LoginRequest;
import com.cooperativa.met.application.identity.dto.RegisterUserRequest;
import com.cooperativa.met.application.identity.dto.UserResponse;
import com.cooperativa.met.application.identity.usecase.GetUserProfileUseCase;
import com.cooperativa.met.application.identity.usecase.LoginUseCase;
import com.cooperativa.met.application.identity.usecase.RegisterBiometricUseCase;
import com.cooperativa.met.application.identity.usecase.RegisterUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final RegisterBiometricUseCase registerBiometricUseCase;
    private final LoginUseCase loginUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;

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

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(getUserProfileUseCase.execute(userId));
    }
}
