package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.admin.dto.ReverseTransactionRequest;
import com.cooperativa.met.application.admin.usecase.ReverseTransactionUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/transactions")
@RequiredArgsConstructor
public class TransactionAdminController {

    private final ReverseTransactionUseCase reverseTransactionUseCase;

    @PostMapping("/reverse")
    public ResponseEntity<Map<String, String>> reverse(
            @Valid @RequestBody ReverseTransactionRequest request,
            Authentication auth, HttpServletRequest http) {
        UUID adminId = (UUID) auth.getPrincipal();
        reverseTransactionUseCase.execute(adminId, request, http.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "message", "Reversión de transacción registrada correctamente",
                "transactionId", request.transactionId()
        ));
    }
}
