package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.application.account.service.AccountReversalService;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.bank.model.ExternalPayout;
import com.cooperativa.met.domain.bank.port.ExternalPayoutRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPayoutWebhookUseCaseTest {

    private static final BigDecimal AMOUNT = new BigDecimal("150000");

    @Mock private ExternalPayoutRepositoryPort externalPayoutRepository;
    @Mock private CoreTransactionRepositoryPort transactionRepository;
    @Mock private CoreAccountRepositoryPort accountRepository;
    @Mock private AccountReversalService accountReversalService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ProcessPayoutWebhookUseCase webhookUseCase;

    private final UUID payoutId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();
    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldCompleteTheWithdrawalWhenTheRailConfirmsIt() {
        givenPendingPayout(null, TransactionStatus.PENDING);
        givenSourceAccount();

        webhookUseCase.execute("PAYOUT-" + payoutId, true, null, null);

        assertEquals(TransactionStatus.COMPLETED, savedTransaction().getStatus());
        assertNotNull(savedPayout().getSettledAt());
        verifyNoInteractions(accountReversalService);
        verify(auditLogService).logSuccess(userId, "EXTERNAL_PAYOUT_SETTLED", "EXTERNAL_PAYOUT", transactionId.toString(), "{}");
    }

    @Test
    void shouldReturnTheMoneyWhenTheRailRejectsAfterAccepting() {
        givenPendingPayout(null, TransactionStatus.PENDING);
        givenSourceAccount();

        webhookUseCase.execute("PAYOUT-" + payoutId, false, "INVALID_ACCOUNT", "Cuenta inexistente");

        verify(accountReversalService).creditWithRetry(sourceAccountId, AMOUNT);
        assertEquals(TransactionStatus.REVERSED, savedTransaction().getStatus());
        assertEquals("INVALID_ACCOUNT", savedPayout().getFailureCode());
        verify(auditLogService).logFailure(eq(userId), eq("EXTERNAL_PAYOUT_REVERSED"), eq("EXTERNAL_PAYOUT"),
                eq(transactionId.toString()), anyString());
    }

    @Test
    void shouldIgnoreDuplicateEventForAnAlreadySettledPayout() {
        givenPendingPayout(Instant.now(), TransactionStatus.PENDING);

        webhookUseCase.execute("PAYOUT-" + payoutId, false, "X", "Y");

        verifyNoInteractions(transactionRepository, accountReversalService, auditLogService);
    }

    @Test
    void shouldIgnoreEventWhenTransactionIsNoLongerPending() {
        givenPendingPayout(null, TransactionStatus.COMPLETED);

        webhookUseCase.execute("PAYOUT-" + payoutId, false, "X", "Y");

        verifyNoInteractions(accountReversalService, accountRepository, auditLogService);
    }

    @Test
    void shouldIgnoreUnknownPayout() {
        when(externalPayoutRepository.findById(payoutId)).thenReturn(Optional.empty());

        webhookUseCase.execute("PAYOUT-" + payoutId, true, null, null);

        verifyNoInteractions(transactionRepository, accountReversalService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"VERIFY-123", "PAYOUT-no-es-uuid", ""})
    void shouldIgnoreReferencesThatAreNotPayouts(String reference) {
        webhookUseCase.execute(reference, true, null, null);

        verifyNoInteractions(externalPayoutRepository, transactionRepository, accountReversalService);
    }

    private void givenPendingPayout(Instant settledAt, TransactionStatus transactionStatus) {
        when(externalPayoutRepository.findById(payoutId)).thenReturn(Optional.of(ExternalPayout.builder()
                .id(payoutId).coreTransactionId(transactionId).settledAt(settledAt).build()));
        org.mockito.Mockito.lenient().when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(CoreTransaction.builder()
                .id(transactionId)
                .sourceAccountId(sourceAccountId)
                .amount(AMOUNT)
                .type(TransactionType.EXTERNAL_PAYOUT)
                .status(transactionStatus)
                .build()));
    }

    private void givenSourceAccount() {
        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(CoreAccount.builder().id(sourceAccountId).userId(userId).build()));
    }

    private CoreTransaction savedTransaction() {
        ArgumentCaptor<CoreTransaction> captor = ArgumentCaptor.forClass(CoreTransaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    private ExternalPayout savedPayout() {
        ArgumentCaptor<ExternalPayout> captor = ArgumentCaptor.forClass(ExternalPayout.class);
        verify(externalPayoutRepository).save(captor.capture());
        return captor.getValue();
    }
}
