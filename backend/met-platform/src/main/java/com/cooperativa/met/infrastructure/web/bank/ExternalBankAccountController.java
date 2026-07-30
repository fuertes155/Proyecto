package com.cooperativa.met.infrastructure.web.bank;

import com.cooperativa.met.application.bank.dto.ConfirmBankAccountVerificationRequest;
import com.cooperativa.met.application.bank.dto.ExternalBankAccountResponse;
import com.cooperativa.met.application.bank.dto.RegisterExternalBankAccountRequest;
import com.cooperativa.met.application.bank.usecase.ConfirmBankAccountVerificationUseCase;
import com.cooperativa.met.application.bank.usecase.InitiateBankAccountVerificationUseCase;
import com.cooperativa.met.application.bank.usecase.ListMyExternalBankAccountsUseCase;
import com.cooperativa.met.application.bank.usecase.RegisterExternalBankAccountUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts/external-bank-accounts")
@RequiredArgsConstructor
public class ExternalBankAccountController {

    private final RegisterExternalBankAccountUseCase registerExternalBankAccountUseCase;
    private final ListMyExternalBankAccountsUseCase listMyExternalBankAccountsUseCase;
    private final InitiateBankAccountVerificationUseCase initiateBankAccountVerificationUseCase;
    private final ConfirmBankAccountVerificationUseCase confirmBankAccountVerificationUseCase;

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

    @GetMapping
    public ResponseEntity<List<ExternalBankAccountResponse>> list() {
        return ResponseEntity.ok(listMyExternalBankAccountsUseCase.execute(getAuthenticatedUserId()));
    }

    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterExternalBankAccountRequest request) {
        registerExternalBankAccountUseCase.execute(getAuthenticatedUserId(), request);
        return ResponseEntity.ok().build();
    }

    /** Reenvía el micro-depósito de verificación (ej. si el envío automático del registro falló). */
    @PostMapping("/{id}/verify/resend")
    public ResponseEntity<Void> resendVerification(@PathVariable UUID id) {
        initiateBankAccountVerificationUseCase.execute(getAuthenticatedUserId(), id);
        return ResponseEntity.ok().build();
    }

    /** Confirma la titularidad: el usuario reporta el monto exacto visto en su extracto bancario. */
    @PostMapping("/{id}/verify")
    public ResponseEntity<Void> confirmVerification(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmBankAccountVerificationRequest request) {
        confirmBankAccountVerificationUseCase.execute(getAuthenticatedUserId(), id, request.amount());
        return ResponseEntity.ok().build();
    }
}
