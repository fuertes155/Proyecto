package com.cooperativa.fintech.application.lending.usecase;

import com.cooperativa.fintech.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.fintech.application.lending.mapper.LendingMapper;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.fintech.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.fintech.domain.lending.port.PersonalLoanApplicationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPersonalLoanApplicationUseCase {

    private final PersonalLoanApplicationPort applicationPort;
    private final AmortizationSchedulePort schedulePort;
    private final LendingMapper mapper;

    @Transactional(readOnly = true)
    public LoanApplicationResponse execute(UUID userId, UUID applicationId) {
        PersonalLoanApplication application = applicationPort.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de préstamo no encontrada"));

        return mapper.toResponse(application, schedulePort.findByApplicationId(applicationId));
    }
}
