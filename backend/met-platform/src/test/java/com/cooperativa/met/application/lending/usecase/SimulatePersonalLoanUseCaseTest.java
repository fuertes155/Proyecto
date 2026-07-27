package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.application.lending.dto.LoanSimulationResponse;
import com.cooperativa.met.application.lending.dto.SimulateLoanRequest;
import com.cooperativa.met.application.lending.mapper.LendingMapper;
import com.cooperativa.met.domain.lending.model.LoanSimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SimulatePersonalLoanUseCaseTest {

    @Mock
    private LendingMapper mapper;

    @InjectMocks
    private SimulatePersonalLoanUseCase simulatePersonalLoanUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSimulateLoan() {
        SimulateLoanRequest request = new SimulateLoanRequest(new BigDecimal("1000000"), 12);

        LoanSimulationResponse response = new LoanSimulationResponse(
                new BigDecimal("1000000"),
                new BigDecimal("1150000"),
                new BigDecimal("0.15"),
                new BigDecimal("95833.33"),
                12,
                Collections.emptyList()
        );

        when(mapper.toResponse(any(LoanSimulationResult.class))).thenReturn(response);

        LoanSimulationResponse result = simulatePersonalLoanUseCase.execute(request);

        assertNotNull(result);
        assertNotNull(result.monthlyPayment());
    }
}
