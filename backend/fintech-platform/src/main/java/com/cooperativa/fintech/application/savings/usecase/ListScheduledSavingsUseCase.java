package com.cooperativa.fintech.application.savings.usecase;

import com.cooperativa.fintech.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.fintech.application.savings.mapper.ScheduledSavingsMapper;
import com.cooperativa.fintech.domain.savings.port.ScheduledSavingsAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListScheduledSavingsUseCase {

    private final ScheduledSavingsAccountPort accountPort;
    private final ScheduledSavingsMapper mapper;

    @Transactional(readOnly = true)
    public List<ScheduledSavingsResponse> execute(UUID userId) {
        return accountPort.findByUserId(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
