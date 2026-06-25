package com.cooperativa.met.application.savings.usecase;

import com.cooperativa.met.application.savings.dto.WithdrawSavingsRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.savings.model.SavingsWithdrawal;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.model.WithdrawalType;
import com.cooperativa.met.domain.savings.port.SavingsWithdrawalPort;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WithdrawSavingsUseCase {

    private final ScheduledSavingsAccountPort accountPort;
    private final SavingsWithdrawalPort withdrawalPort;

    @Transactional
    public void execute(UUID userId, WithdrawSavingsRequest request) {
        ScheduledSavingsAccount account = accountPort.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new BusinessRuleException("ACCOUNT_NOT_FOUND", "Cuenta de ahorro programado no encontrada o no pertenece al usuario."));

        if (account.getStatus() == ScheduledSavingsStatus.CANCELLED) {
            throw new BusinessRuleException("ACCOUNT_CANCELLED", "No se pueden realizar retiros de una cuenta cancelada.");
        }

        BigDecimal withdrawAmount = request.getAmount();

        if (withdrawAmount == null || withdrawAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT", "El monto a retirar debe ser mayor a cero.");
        }

        if (request.getWithdrawalType() == WithdrawalType.PARTIAL) {
            BigDecimal maxAllowed = account.getCurrentBalance().multiply(BigDecimal.valueOf(0.40));
            if (withdrawAmount.compareTo(maxAllowed) > 0) {
                throw new BusinessRuleException("LIMIT_EXCEEDED", "El retiro parcial no puede superar el 40% del saldo actual (" + maxAllowed + ").");
            }
            account = account.withBalance(account.getCurrentBalance().subtract(withdrawAmount));
            
        } else if (request.getWithdrawalType() == WithdrawalType.FULL) {
            withdrawAmount = account.getCurrentBalance();
            account = account.withBalance(BigDecimal.ZERO)
                             .withStatus(ScheduledSavingsStatus.CANCELLED);
        } else {
            throw new BusinessRuleException("INVALID_TYPE", "Tipo de retiro no soportado.");
        }

        accountPort.save(account);

        SavingsWithdrawal withdrawal = SavingsWithdrawal.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .userId(userId)
                .amount(withdrawAmount)
                .type(request.getWithdrawalType())
                .createdAt(Instant.now())
                .build();
        
        withdrawalPort.save(withdrawal);
    }
}
