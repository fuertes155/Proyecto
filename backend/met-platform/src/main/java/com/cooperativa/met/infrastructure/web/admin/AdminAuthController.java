package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.AdminAuthResponse;
import com.cooperativa.met.application.admin.dto.AdminLoginRequest;
import com.cooperativa.met.application.admin.usecase.AdminLoginUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminLoginUseCase adminLoginUseCase;

    @PostMapping("/login")
    public ResponseEntity<AdminAuthResponse> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(adminLoginUseCase.execute(request, ip));
    }
}
