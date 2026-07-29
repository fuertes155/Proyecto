package com.cooperativa.met.infrastructure.web.account;

import com.cooperativa.met.application.account.dto.HomeSummaryResponse;
import com.cooperativa.met.application.account.usecase.GetHomeSummaryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

// Ruta en singular ("account") para calzar con el contrato ya usado por el cliente
// (HomeRemoteRepository en mobile/web_admin), que envuelve la respuesta en {"data": ...}.
@RestController
@RequestMapping("/v1/account")
@RequiredArgsConstructor
public class HomeController {

    private final GetHomeSummaryUseCase getHomeSummaryUseCase;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, HomeSummaryResponse>> getSummary(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(Map.of("data", getHomeSummaryUseCase.execute(userId)));
    }
}
