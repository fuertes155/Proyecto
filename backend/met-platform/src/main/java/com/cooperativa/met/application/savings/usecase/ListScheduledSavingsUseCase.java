package com.cooperativa.met.application.savings.usecase;

import com.cooperativa.met.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.met.application.savings.mapper.ScheduledSavingsMapper;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
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
