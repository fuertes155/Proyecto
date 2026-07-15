package com.cooperativa.met.infrastructure.web.legal;

import com.cooperativa.met.application.legal.usecase.ConfirmMandateSignatureUseCase;
import com.cooperativa.met.application.legal.usecase.RequestMandateSignatureUseCase;
import com.cooperativa.met.domain.legal.model.MandateContract;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/legal/mandate")
@RequiredArgsConstructor
public class LegalController {

    private final RequestMandateSignatureUseCase requestUseCase;
    private final ConfirmMandateSignatureUseCase confirmUseCase;

    @PostMapping("/request-signature")
    public ResponseEntity<Map<String, String>> requestSignature(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        String txId = requestUseCase.execute(userId);
        return ResponseEntity.ok(Map.of(
                "message", "OTP enviado exitosamente",
                "transactionId", txId
        ));
    }

    @PostMapping("/confirm-signature")
    public ResponseEntity<Map<String, String>> confirmSignature(
            Authentication authentication,
            @RequestBody ConfirmSignatureRequest requestBody,
            HttpServletRequest request) {
        
        UUID userId = UUID.fromString(authentication.getName());
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        
        if (userAgent == null) userAgent = "Unknown";

        MandateContract contract = confirmUseCase.execute(
                userId, 
                requestBody.otpCode(), 
                ipAddress, 
                userAgent, 
                requestBody.transactionId()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Mandato firmado exitosamente",
                "contractId", contract.getId().toString(),
                "hashSha256", contract.getPdfHashSha256()
        ));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    public record ConfirmSignatureRequest(String otpCode, String transactionId) {}
}
