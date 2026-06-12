package com.cooperativa.fintech.application.lending.usecase;

import com.cooperativa.fintech.application.lending.dto.LoanApplicationResponse;
import com.cooperativa.fintech.application.lending.mapper.LendingMapper;
import com.cooperativa.fintech.domain.lending.port.PersonalLoanApplicationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListPersonalLoanApplicationsUseCase {

    private final PersonalLoanApplicationPort applicationPort;
    private final LendingMapper mapper;

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> execute(UUID userId) {
        return applicationPort.findByUserId(userId).stream()
                .map(mapper::toSummary)
                .toList();
    }
}
