package com.cooperativa.met.infrastructure.web.account;

import com.cooperativa.met.application.account.dto.CoreAccountResponse;
import com.cooperativa.met.application.account.dto.TransferRequest;
import com.cooperativa.met.application.account.dto.VerifyRecipientResponse;
import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.application.account.usecase.ExecuteTransferUseCase;
import com.cooperativa.met.application.account.usecase.GetMyAccountUseCase;
import com.cooperativa.met.application.account.usecase.VerifyRecipientUseCase;
import com.cooperativa.met.application.account.usecase.RequestTransferOtpUseCase;
import com.cooperativa.met.application.account.usecase.DepositUseCase;
import com.cooperativa.met.application.account.usecase.GeneratePseLinkUseCase;
import com.cooperativa.met.application.account.dto.GeneratePseLinkRequest;
import com.cooperativa.met.application.account.dto.GeneratePseLinkResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class CoreAccountController {

    private final GetMyAccountUseCase getMyAccountUseCase;
    private final VerifyRecipientUseCase verifyRecipientUseCase;
    private final ExecuteTransferUseCase executeTransferUseCase;
    private final RequestTransferOtpUseCase requestTransferOtpUseCase;
    private final DepositUseCase depositUseCase;
    private final GeneratePseLinkUseCase generatePseLinkUseCase;

    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken jwtToken) {
            return (UUID) jwtToken.getPrincipal();
        }
        throw new IllegalStateException("No se pudo obtener el ID del usuario autenticado");
    }

    @GetMapping("/me")
    public ResponseEntity<CoreAccountResponse> getMyAccount() {
        return ResponseEntity.ok(getMyAccountUseCase.execute(getAuthenticatedUserId()));
    }

    @GetMapping("/verify")
    public ResponseEntity<VerifyRecipientResponse> verifyRecipient(@RequestParam String identifier) {
        return ResponseEntity.ok(verifyRecipientUseCase.execute(identifier, getAuthenticatedUserId()));
    }

    @PostMapping("/transactions/transfer/otp/request")
    public ResponseEntity<Void> requestTransferOtp() {
        requestTransferOtpUseCase.execute(getAuthenticatedUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transactions/transfer")
    public ResponseEntity<Map<String, String>> transfer(@Valid @RequestBody TransferRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }
        executeTransferUseCase.execute(getAuthenticatedUserId(), request, ip);
        return ResponseEntity.ok(Map.of("message", "Transferencia realizada con éxito"));
    }

    @PostMapping("/deposit")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deposit(@Valid @RequestBody DepositRequest request) {
        depositUseCase.execute(getAuthenticatedUserId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deposit-pse")
    public ResponseEntity<GeneratePseLinkResponse> generatePseLink(@Valid @RequestBody GeneratePseLinkRequest request) {
        return ResponseEntity.ok(generatePseLinkUseCase.execute(getAuthenticatedUserId(), request));
    }
}
