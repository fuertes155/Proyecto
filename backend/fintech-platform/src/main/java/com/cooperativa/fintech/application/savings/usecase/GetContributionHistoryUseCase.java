package com.cooperativa.fintech.application.savings.usecase;

import com.cooperativa.fintech.application.savings.dto.ContributionResponse;
import com.cooperativa.fintech.application.savings.mapper.ScheduledSavingsMapper;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.savings.port.ScheduledContributionPort;
import com.cooperativa.fintech.domain.savings.port.ScheduledSavingsAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetContributionHistoryUseCase {

    private final ScheduledSavingsAccountPort accountPort;
    private final ScheduledContributionPort contributionPort;
    private final ScheduledSavingsMapper mapper;

    @Transactional(readOnly = true)
    public List<ContributionResponse> execute(UUID userId, UUID accountId) {
        accountPort.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta de ahorro programado no encontrada"));

        return contributionPort.findByAccountId(accountId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
