package com.cooperativa.met.application.savings.usecase;

import com.cooperativa.met.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.met.application.savings.mapper.ScheduledSavingsMapper;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.port.SavingsBalanceCachePort;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetScheduledSavingsUseCase {

    private final ScheduledSavingsAccountPort accountPort;
    private final SavingsBalanceCachePort balanceCachePort;
    private final ScheduledSavingsMapper mapper;

    @Transactional(readOnly = true)
    public ScheduledSavingsResponse execute(UUID userId, UUID accountId) {
        ScheduledSavingsAccount account = accountPort.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta de ahorro programado no encontrada"));

        BigDecimal cachedBalance = balanceCachePort.getCachedBalance(accountId).orElse(null);
        if (cachedBalance != null && cachedBalance.compareTo(account.getCurrentBalance()) == 0) {
            return mapper.toResponse(account);
        }

        balanceCachePort.cacheBalance(accountId, account.getCurrentBalance());
        return mapper.toResponse(account);
    }
}
