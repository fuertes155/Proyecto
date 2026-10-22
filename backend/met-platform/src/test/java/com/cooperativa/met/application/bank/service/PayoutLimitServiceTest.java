package com.cooperativa.met.application.bank.service;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.admin.model.OperationLimit;
import com.cooperativa.met.domain.admin.port.OperationLimitRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Límites de retiro a bancos externos: más estrictos mientras la cuenta destino no esté verificada. */
@ExtendWith(MockitoExtension.class)
class PayoutLimitServiceTest {

    @Mock private OperationLimitRepositoryPort operationLimitRepository;
    @Mock private CoreTransactionRepositoryPort transactionRepository;

    @InjectMocks
    private PayoutLimitService payoutLimitService;

    private final CoreAccount account = CoreAccount.builder().id(UUID.randomUUID()).build();

    @Test
    void shouldApplyTheStricterLimitToUnverifiedDestinations() {
        givenLimit("EXTERNAL_PAYOUT_UNVERIFIED", true, 500_000, 200_000);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> payoutLimitService.validate(account, new BigDecimal("200001"), false));

        assertEquals("PER_TX_LIMIT_EXCEEDED", ex.getCode());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldCountWhatWasAlreadyWithdrawnToday() {
        givenLimit("EXTERNAL_PAYOUT", true, 5_000_000, 2_000_000);
        givenWithdrawnToday("4000000");

        assertEquals("DAILY_LIMIT_EXCEEDED", assertThrows(BusinessRuleException.class,
                () -> payoutLimitService.validate(account, new BigDecimal("1000001"), true)).getCode());
        assertDoesNotThrow(() -> payoutLimitService.validate(account, new BigDecimal("1000000"), true));
    }

    @Test
    void shouldBlockWithdrawalsWhenAnAdminDisabledThem() {
        givenLimit("EXTERNAL_PAYOUT", false, 5_000_000, 2_000_000);

        assertEquals("PAYOUT_DISABLED", assertThrows(BusinessRuleException.class,
                () -> payoutLimitService.validate(account, BigDecimal.TEN, true)).getCode());
    }

    @Test
    void shouldFailClosedWhenNoLimitIsConfigured() {
        when(operationLimitRepository.findByTipo("EXTERNAL_PAYOUT")).thenReturn(Optional.empty());

        assertEquals("PAYOUT_LIMIT_NOT_CONFIGURED", assertThrows(BusinessRuleException.class,
                () -> payoutLimitService.validate(account, BigDecimal.TEN, true)).getCode());
    }

    private void givenLimit(String tipo, boolean activo, long diario, long porTransaccion) {
        when(operationLimitRepository.findByTipo(tipo)).thenReturn(Optional.of(OperationLimit.builder()
                .tipoOperacion(tipo).activo(activo).montoDiarioMax(diario).montoPorTransaccionMax(porTransaccion).build()));
    }

    private void givenWithdrawnToday(String amount) {
        when(transactionRepository.sumOutgoingByAccountIdAndTypeAndDateRange(eq(account.getId()), eq(TransactionType.EXTERNAL_PAYOUT), any(), any()))
                .thenReturn(new BigDecimal(amount));
    }
}
