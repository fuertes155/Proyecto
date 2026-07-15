package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.DepositRequest;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import com.cooperativa.met.application.investment.usecase.DistributeDepositUseCase;

@Service
@RequiredArgsConstructor
public class DepositUseCase {

    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final DistributeDepositUseCase distributeDepositUseCase;

    @Transactional
    public void execute(UUID userId, DepositRequest request) {
        if (request.getAmount() == null || request.getAmount().doubleValue() <= 0) {
            throw new BusinessRuleException("DEP_ERR_01", "El monto a recargar debe ser mayor a cero");
        }

        // 1. Obtener la cuenta del usuario actual
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("DEP_ERR_02", "No tienes una cuenta activa para recargar"));

        if (!account.getStatus().name().equals("ACTIVE")) {
            throw new BusinessRuleException("DEP_ERR_03", "Tu cuenta no está activa");
        }

        // 2. Sumar el monto al balance
        CoreAccount updatedAccount = account.creditPrincipal(request.getAmount());
        accountRepository.save(updatedAccount);

        // 3. Crear el registro de la transacción
        CoreTransaction transaction = CoreTransaction.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(updatedAccount.getId())
                .destinationAccountId(updatedAccount.getId())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(request.getAmount())
                .concept("Recarga vía " + request.getMethod())
                .createdAt(Instant.now())
                .build();

        transactionRepository.save(transaction);

        // 4. Fraccionar y emparejar la inversión automáticamente
        distributeDepositUseCase.execute(updatedAccount.getId(), transaction.getId(), request.getAmount());
    }
}
