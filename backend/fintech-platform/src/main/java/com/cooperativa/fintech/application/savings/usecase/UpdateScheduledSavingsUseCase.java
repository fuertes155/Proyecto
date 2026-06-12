package com.cooperativa.fintech.application.savings.usecase;

import com.cooperativa.fintech.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.fintech.application.savings.dto.UpdateScheduledSavingsRequest;
import com.cooperativa.fintech.application.savings.mapper.ScheduledSavingsMapper;
import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.fintech.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.fintech.domain.savings.port.SavingsBalanceCachePort;
import com.cooperativa.fintech.domain.savings.port.ScheduledSavingsAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateScheduledSavingsUseCase {

    private final ScheduledSavingsAccountPort accountPort;
    private final SavingsBalanceCachePort balanceCachePort;
    private final ScheduledSavingsMapper mapper;

    @Transactional
    public ScheduledSavingsResponse execute(UUID userId, UUID accountId, UpdateScheduledSavingsRequest request) {
        ScheduledSavingsAccount account = accountPort.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta de ahorro programado no encontrada"));

        if (account.getStatus() == ScheduledSavingsStatus.CANCELLED
                || account.getStatus() == ScheduledSavingsStatus.COMPLETED) {
            throw new BusinessRuleException("ACCOUNT_NOT_MODIFIABLE", "La cuenta no puede modificarse en su estado actual");
        }

        ScheduledSavingsAccount updated = account;

        if (request.contributionAmount() != null) {
            updated = updated.withContributionAmount(request.contributionAmount());
        }

        if (request.status() != null) {
            updated = applyStatusChange(updated, request.status());
        }

        ScheduledSavingsAccount saved = accountPort.save(updated);
        balanceCachePort.invalidate(accountId);
        balanceCachePort.cacheBalance(accountId, saved.getCurrentBalance());
        return mapper.toResponse(saved);
    }

    private ScheduledSavingsAccount applyStatusChange(ScheduledSavingsAccount account, ScheduledSavingsStatus newStatus) {
        return switch (newStatus) {
            case PAUSED -> {
                if (account.getStatus() != ScheduledSavingsStatus.ACTIVE) {
                    throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "Solo cuentas activas pueden pausarse");
                }
                yield account.withStatus(ScheduledSavingsStatus.PAUSED);
            }
            case ACTIVE -> {
                if (account.getStatus() != ScheduledSavingsStatus.PAUSED) {
                    throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "Solo cuentas pausadas pueden reactivarse");
                }
                yield account.withStatus(ScheduledSavingsStatus.ACTIVE);
            }
            case CANCELLED -> account.withStatus(ScheduledSavingsStatus.CANCELLED);
            default -> throw new BusinessRuleException("INVALID_STATUS_TRANSITION", "Transición de estado no permitida");
        };
    }
}
