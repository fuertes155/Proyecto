package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminLoanUseCase {

    private final PersonalLoanApplicationPort loanApplicationPort;

    public List<PersonalLoanApplication> getAllLoans() {
        return loanApplicationPort.findAll();
    }

    public PersonalLoanApplication updateLoanStatus(UUID loanId, LoanApplicationStatus status) {
        PersonalLoanApplication loan = loanApplicationPort.findById(loanId)
                .orElseThrow(() -> new BusinessRuleException("LOAN_NOT_FOUND", "Solicitud de crédito no encontrada"));

        // Simplificado para MVP
        PersonalLoanApplication updated = loan.toBuilder().status(status).build();
        return loanApplicationPort.save(updated);
    }
}
