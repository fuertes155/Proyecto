package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.application.account.service.AccountReversalService;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.bank.model.ExternalPayout;
import com.cooperativa.met.domain.bank.port.ExternalPayoutRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Aplica la confirmación asíncrona del riel de pago sobre un payout que
 * quedó en estado PENDING. Idempotente: si el {@code core_transaction} ya
 * no está PENDING (evento duplicado o fuera de orden), no hace nada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPayoutWebhookUseCase {

    private static final String REFERENCE_PREFIX = "PAYOUT-";

    private final ExternalPayoutRepositoryPort externalPayoutRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final CoreAccountRepositoryPort accountRepository;
    private final AccountReversalService accountReversalService;
    private final AuditLogService auditLogService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void execute(String reference, boolean approved, String failureCode, String failureMessage) {
        UUID payoutId = parsePayoutId(reference);
        if (payoutId == null) {
            log.warn("Referencia de payout con formato inesperado, se ignora: {}", reference);
            return;
        }

        ExternalPayout payout = externalPayoutRepository.findById(payoutId).orElse(null);
        if (payout == null) {
            log.warn("No se encontró un payout local para la referencia {}", reference);
            return;
        }
        if (payout.getSettledAt() != null) {
            log.info("Payout {} ya fue liquidado previamente, se ignora evento duplicado", payoutId);
            return;
        }

        CoreTransaction transaction = transactionRepository.findById(payout.getCoreTransactionId())
                .orElseThrow(() -> new IllegalStateException("No existe core_transaction para el payout " + payoutId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.info("La transacción {} ya no está PENDING (status={}), se ignora evento duplicado",
                    transaction.getId(), transaction.getStatus());
            return;
        }

        CoreAccount account = accountRepository.findById(transaction.getSourceAccountId())
                .orElseThrow(() -> new IllegalStateException("No existe la cuenta origen de la transacción " + transaction.getId()));

        if (approved) {
            transactionRepository.save(transaction.toBuilder().status(TransactionStatus.COMPLETED).build());
            externalPayoutRepository.save(payout.markSettled());
            auditLogService.logSuccess(account.getUserId(), "EXTERNAL_PAYOUT_SETTLED",
                    "EXTERNAL_PAYOUT", transaction.getId().toString(), "{}");
            log.info("Payout {} confirmado por el riel de pago", payoutId);
        } else {
            accountReversalService.creditWithRetry(transaction.getSourceAccountId(), transaction.getAmount());
            transactionRepository.save(transaction.toBuilder().status(TransactionStatus.REVERSED).build());
            externalPayoutRepository.save(payout.markFailed(failureCode, failureMessage));
            auditLogService.logFailure(account.getUserId(), "EXTERNAL_PAYOUT_REVERSED",
                    "EXTERNAL_PAYOUT", transaction.getId().toString(),
                    String.format("{\"failureCode\":\"%s\"}", failureCode));
            log.warn("Payout {} rechazado por el riel de pago (code={}), monto revertido a la wallet", payoutId, failureCode);
        }
    }

    private UUID parsePayoutId(String reference) {
        if (reference == null || !reference.startsWith(REFERENCE_PREFIX)) {
            return null;
        }
        try {
            return UUID.fromString(reference.substring(REFERENCE_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
