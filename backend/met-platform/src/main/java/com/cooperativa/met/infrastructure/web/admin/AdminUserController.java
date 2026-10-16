package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.AdminUserResponse;
import com.cooperativa.met.application.identity.usecase.AdminUserUseCase;
import com.cooperativa.met.domain.identity.model.KycStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserUseCase adminUserUseCase;

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserUseCase.getAllUsers().stream()
                .map(AdminUserResponse::from)
                .toList());
    }

    @PutMapping("/{userId}/kyc")
    public ResponseEntity<AdminUserResponse> updateKycStatus(@PathVariable UUID userId, @RequestParam KycStatus status) {
        return ResponseEntity.ok(AdminUserResponse.from(adminUserUseCase.updateKycStatus(userId, status)));
    }
}
