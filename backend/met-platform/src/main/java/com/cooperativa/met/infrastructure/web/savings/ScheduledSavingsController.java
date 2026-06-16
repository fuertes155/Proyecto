package com.cooperativa.met.infrastructure.web.savings;

import com.cooperativa.met.application.savings.dto.ContributionResponse;
import com.cooperativa.met.application.savings.dto.CreateScheduledSavingsRequest;
import com.cooperativa.met.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.met.application.savings.dto.UpdateScheduledSavingsRequest;
import com.cooperativa.met.application.savings.usecase.CreateScheduledSavingsUseCase;
import com.cooperativa.met.application.savings.usecase.GetContributionHistoryUseCase;
import com.cooperativa.met.application.savings.usecase.GetScheduledSavingsUseCase;
import com.cooperativa.met.application.savings.usecase.ListScheduledSavingsUseCase;
import com.cooperativa.met.application.savings.usecase.UpdateScheduledSavingsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/savings/scheduled")
@RequiredArgsConstructor
public class ScheduledSavingsController {

    private final CreateScheduledSavingsUseCase createUseCase;
    private final ListScheduledSavingsUseCase listUseCase;
    private final GetScheduledSavingsUseCase getUseCase;
    private final UpdateScheduledSavingsUseCase updateUseCase;
    private final GetContributionHistoryUseCase historyUseCase;

    @PostMapping
    public ResponseEntity<ScheduledSavingsResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateScheduledSavingsRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(createUseCase.execute(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<ScheduledSavingsResponse>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(listUseCase.execute(userId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ScheduledSavingsResponse> get(
            Authentication authentication,
            @PathVariable UUID accountId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(getUseCase.execute(userId, accountId));
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<ScheduledSavingsResponse> update(
            Authentication authentication,
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateScheduledSavingsRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(updateUseCase.execute(userId, accountId, request));
    }

    @GetMapping("/{accountId}/contributions")
    public ResponseEntity<List<ContributionResponse>> contributions(
            Authentication authentication,
            @PathVariable UUID accountId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(historyUseCase.execute(userId, accountId));
    }
}
