package com.cooperativa.met.infrastructure.web.bank;

import com.cooperativa.met.application.bank.dto.ExecutePayoutRequest;
import com.cooperativa.met.application.bank.usecase.ExecuteExternalPayoutUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Retiro/transferencia a una cuenta bancaria externa propia del usuario.
 * El código de OTP se solicita con el mismo endpoint que ya usan las
 * transferencias internas ({@code POST /v1/accounts/transactions/transfer/otp/request})
 * — el OTP en este backend está atado al usuario, no a un tipo de operación.
 */
@RestController
@RequestMapping("/v1/accounts/transactions")
@RequiredArgsConstructor
public class PayoutController {

    private final ExecuteExternalPayoutUseCase executeExternalPayoutUseCase;

    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken jwtToken) {
            Object principal = jwtToken.getPrincipal();
            if (principal instanceof UUID uuid) {
                return uuid;
            } else if (principal instanceof String str) {
                return UUID.fromString(str);
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                return UUID.fromString(userDetails.getUsername());
            }
        }
        throw new IllegalStateException("No se pudo obtener el ID del usuario autenticado");
    }

    @PostMapping("/payout")
    public ResponseEntity<Map<String, String>> payout(@Valid @RequestBody ExecutePayoutRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }
        executeExternalPayoutUseCase.execute(getAuthenticatedUserId(), request, ip);
        return ResponseEntity.ok(Map.of("message", "Retiro iniciado con éxito"));
    }
}
