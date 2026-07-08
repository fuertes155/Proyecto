package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.identity.usecase.AdminUserUseCase;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
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
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = adminUserUseCase.getAllUsers();
        // Nota: En producción esto debería retornar un DTO en vez del Dominio,
        // pero para MVP usamos el Dominio.
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}/kyc")
    public ResponseEntity<User> updateKycStatus(@PathVariable UUID userId, @RequestParam KycStatus status) {
        User updated = adminUserUseCase.updateKycStatus(userId, status);
        return ResponseEntity.ok(updated);
    }
}
