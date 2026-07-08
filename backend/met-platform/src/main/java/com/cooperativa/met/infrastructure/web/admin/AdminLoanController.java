package com.cooperativa.met.infrastructure.web.admin;

import com.cooperativa.met.application.lending.usecase.AdminLoanUseCase;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/loans")
@RequiredArgsConstructor
public class AdminLoanController {

    private final AdminLoanUseCase adminLoanUseCase;

    @GetMapping
    public ResponseEntity<List<PersonalLoanApplication>> getAllLoans() {
        List<PersonalLoanApplication> loans = adminLoanUseCase.getAllLoans();
        return ResponseEntity.ok(loans);
    }

    @PutMapping("/{loanId}/status")
    public ResponseEntity<PersonalLoanApplication> updateLoanStatus(@PathVariable UUID loanId, @RequestParam LoanApplicationStatus status) {
        PersonalLoanApplication updated = adminLoanUseCase.updateLoanStatus(loanId, status);
        return ResponseEntity.ok(updated);
    }
}
