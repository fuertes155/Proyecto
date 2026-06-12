package com.cooperativa.fintech.application.lending.usecase;

import com.cooperativa.fintech.application.lending.dto.LoanSimulationResponse;
import com.cooperativa.fintech.application.lending.dto.SimulateLoanRequest;
import com.cooperativa.fintech.application.lending.mapper.LendingMapper;
import com.cooperativa.fintech.domain.lending.model.LoanSimulationResult;
import com.cooperativa.fintech.domain.lending.service.FrenchAmortizationCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SimulatePersonalLoanUseCase {

    private final LendingMapper mapper;

    public LoanSimulationResponse execute(SimulateLoanRequest request) {
        LoanSimulationResult result = FrenchAmortizationCalculator.simulate(
                request.amount(),
                request.termMonths(),
                request.annualInterestRate(),
                LocalDate.now()
        );
        return mapper.toResponse(result);
    }
}
