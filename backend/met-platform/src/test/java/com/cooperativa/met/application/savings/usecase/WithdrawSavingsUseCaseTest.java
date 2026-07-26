package com.cooperativa.met.application.savings.usecase;

import com.cooperativa.met.application.savings.dto.WithdrawSavingsRequest;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.model.WithdrawalType;
import com.cooperativa.met.domain.savings.port.SavingsWithdrawalPort;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawSavingsUseCaseTest {

    @Mock private ScheduledSavingsAccountPort accountPort;
    @Mock private SavingsWithdrawalPort withdrawalPort;

    @InjectMocks
    private WithdrawSavingsUseCase useCase;

    private UUID userId;
    private UUID accountId;
    private ScheduledSavingsAccount activeAccount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        activeAccount = ScheduledSavingsAccount.builder()
                .id(accountId)
                .userId(userId)
                .currentBalance(new BigDecimal("1000000.00"))
                .status(ScheduledSavingsStatus.ACTIVE)
                .build();
    }

    @Test
    void execute_partialWithdraw_succeedsWithin40Percent() {
        // Arrange: 40% de 1,000,000 = 400,000
        WithdrawSavingsRequest request = buildRequest(new BigDecimal("300000.00"), WithdrawalType.PARTIAL);
        when(accountPort.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(activeAccount));

        // Act
        assertDoesNotThrow(() -> useCase.execute(userId, request));

        // Assert: la cuenta se actualizó con el saldo reducido
        ArgumentCaptor<ScheduledSavingsAccount> captor = ArgumentCaptor.forClass(ScheduledSavingsAccount.class);
        verify(accountPort).save(captor.capture());
        assertEquals(new BigDecimal("700000.00"), captor.getValue().getCurrentBalance());
        verify(withdrawalPort).save(any());
    }

    @Test
    void execute_fullWithdraw_cancelAccountAndDrainsBalance() {
        // Arrange
        WithdrawSavingsRequest request = buildRequest(new BigDecimal("1000000.00"), WithdrawalType.FULL);
        when(accountPort.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(activeAccount));

        // Act
        assertDoesNotThrow(() -> useCase.execute(userId, request));

        // Assert: cuenta queda CANCELLED con balance 0
        ArgumentCaptor<ScheduledSavingsAccount> captor = ArgumentCaptor.forClass(ScheduledSavingsAccount.class);
        verify(accountPort).save(captor.capture());
        assertEquals(BigDecimal.ZERO, captor.getValue().getCurrentBalance());
        assertEquals(ScheduledSavingsStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void execute_throwsAccountNotFound_whenAccountDoesNotBelongToUser() {
        when(accountPort.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());
        WithdrawSavingsRequest request = buildRequest(new BigDecimal("100.00"), WithdrawalType.PARTIAL);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, request));

        assertEquals("ACCOUNT_NOT_FOUND", ex.getCode());
        verifyNoInteractions(withdrawalPort);
    }

    @Test
    void execute_throwsAccountCancelled_whenAccountIsCancelled() {
        ScheduledSavingsAccount cancelled = activeAccount.withStatus(ScheduledSavingsStatus.CANCELLED);
        when(accountPort.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(cancelled));
        WithdrawSavingsRequest request = buildRequest(new BigDecimal("100.00"), WithdrawalType.PARTIAL);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, request));

        assertEquals("ACCOUNT_CANCELLED", ex.getCode());
        verifyNoInteractions(withdrawalPort);
    }

    @Test
    void execute_throwsInvalidAmount_whenAmountIsZero() {
        when(accountPort.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(activeAccount));
        WithdrawSavingsRequest request = buildRequest(BigDecimal.ZERO, WithdrawalType.PARTIAL);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, request));

        assertEquals("INVALID_AMOUNT", ex.getCode());
    }

    @Test
    void execute_throwsLimitExceeded_whenPartialWithdrawExceeds40Percent() {
        // 40% de 1,000,000 = 400,000. Solicitar 500,000 debe fallar.
        when(accountPort.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(activeAccount));
        WithdrawSavingsRequest request = buildRequest(new BigDecimal("500000.00"), WithdrawalType.PARTIAL);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(userId, request));

        assertEquals("LIMIT_EXCEEDED", ex.getCode());
        verify(accountPort, never()).save(any());
    }

    private WithdrawSavingsRequest buildRequest(BigDecimal amount, WithdrawalType type) {
        WithdrawSavingsRequest req = new WithdrawSavingsRequest();
        req.setAccountId(accountId);
        req.setAmount(amount);
        req.setWithdrawalType(type);
        return req;
    }
}
