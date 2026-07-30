package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.application.lending.dto.LoanSimulationResponse;
import com.cooperativa.met.application.lending.dto.SimulateLoanRequest;
import com.cooperativa.met.application.lending.mapper.LendingMapper;
import com.cooperativa.met.domain.lending.model.LoanSimulationResult;
import com.cooperativa.met.domain.lending.model.RiskTier;
import com.cooperativa.met.domain.lending.service.FrenchAmortizationCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SimulatePersonalLoanUseCase {

    private final LendingMapper mapper;

    public LoanSimulationResponse execute(SimulateLoanRequest request) {
        // Simulador público (sin score todavía, se consulta al enviar la solicitud real):
        // usamos la tasa de la banda de riesgo medio del motor de scoring como estimado.
        BigDecimal estimatedRate = RiskTier.RIESGO_MEDIO.getAnnualInterestRate();

        LoanSimulationResult result = FrenchAmortizationCalculator.simulate(
                request.amount(),
                request.termMonths(),
                estimatedRate,
                LocalDate.now()
        );
        return mapper.toResponse(result);
    }
}
