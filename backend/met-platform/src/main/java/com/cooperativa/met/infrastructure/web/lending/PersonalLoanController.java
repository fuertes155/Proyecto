package com.cooperativa.met.infrastructure.web.lending;

import com.cooperativa.met.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.met.application.lending.dto.LoanSimulationResponse;
import com.cooperativa.met.application.lending.dto.SimulateLoanRequest;
import com.cooperativa.met.application.lending.dto.SubmitLoanApplicationRequest;
import com.cooperativa.met.application.lending.usecase.GetPersonalLoanApplicationUseCase;
import com.cooperativa.met.application.lending.usecase.ListPersonalLoanApplicationsUseCase;
import com.cooperativa.met.application.lending.usecase.SimulatePersonalLoanUseCase;
import com.cooperativa.met.application.lending.usecase.SubmitPersonalLoanApplicationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/loans")
@RequiredArgsConstructor
public class PersonalLoanController {

    private final SimulatePersonalLoanUseCase simulateUseCase;
    private final SubmitPersonalLoanApplicationUseCase submitUseCase;
    private final ListPersonalLoanApplicationsUseCase listUseCase;
    private final GetPersonalLoanApplicationUseCase getUseCase;

    @PostMapping("/simulate")
    public ResponseEntity<LoanSimulationResponse> simulate(@Valid @RequestBody SimulateLoanRequest request) {
        return ResponseEntity.ok(simulateUseCase.execute(request));
    }

    @PostMapping("/applications")
    public ResponseEntity<LoanApplicationResponse> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitLoanApplicationRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(submitUseCase.execute(userId, request));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<LoanApplicationResponse>> list(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(listUseCase.execute(userId));
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<LoanApplicationResponse> get(
            Authentication authentication,
            @PathVariable UUID applicationId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(getUseCase.execute(userId, applicationId));
    }
}
