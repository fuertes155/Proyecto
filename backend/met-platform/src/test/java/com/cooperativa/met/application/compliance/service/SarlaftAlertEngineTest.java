package com.cooperativa.met.application.compliance.service;

import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.compliance.model.AlertSeverity;
import com.cooperativa.met.domain.compliance.model.AlertStatus;
import com.cooperativa.met.domain.compliance.model.AlertType;
import com.cooperativa.met.domain.compliance.model.ComplianceAlert;
import com.cooperativa.met.domain.compliance.port.ComplianceAlertRepositoryPort;
import com.cooperativa.met.infrastructure.config.MetComplianceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Umbrales por defecto de {@link MetComplianceProperties.Sarlaft}: $10.000.000 de monto inusual,
 * 5x el promedio histórico, más de 10 operaciones por día, fraccionamiento = 3 operaciones entre
 * el 80% y el 100% del umbral en 24h, y entrada/salida rápida dentro de 60 minutos.
 *
 * <p>Los llamadores (depósito, transferencia, retiro externo) guardan la transacción ANTES de
 * evaluarla, así que el historial que ve el motor siempre incluye la operación actual. Los tests
 * reproducen eso a propósito.</p>
 */
@ExtendWith(MockitoExtension.class)
class SarlaftAlertEngineTest {

    @Mock
    private CoreTransactionRepositoryPort transactionRepository;
    @Mock
    private ComplianceAlertRepositoryPort alertRepository;

    private SarlaftAlertEngine engine;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        engine = new SarlaftAlertEngine(transactionRepository, alertRepository, new MetComplianceProperties());
    }

    @Test
    void shouldStayQuietForAnOrdinaryOperation() {
        evaluate(TransactionType.TRANSFER, "150000",
                tx(TransactionType.DEPOSIT, "200000", Duration.ofDays(10)),
                tx(TransactionType.TRANSFER, "80000", Duration.ofDays(4)));

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldRaiseHighAlertAboveTheAbsoluteThreshold() {
        evaluate(TransactionType.DEPOSIT, "12000000");

        ComplianceAlert alert = alerts().get(AlertType.UNUSUAL_AMOUNT);
        assertEquals(AlertSeverity.HIGH, alert.getSeverity());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals(userId, alert.getUserId());
        assertEquals(transactionId, alert.getTransactionId());
    }

    @Test
    void shouldNotFlagAmountExactlyAtTheThreshold() {
        evaluate(TransactionType.DEPOSIT, "10000000");

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldRaiseMediumAlertForAmountFarAboveThisAccountsAverage() {
        // La operación actual no debe inflar el promedio contra el que se compara.
        evaluate(TransactionType.TRANSFER, "600000",
                tx(TransactionType.DEPOSIT, "100000", Duration.ofDays(30)),
                tx(TransactionType.DEPOSIT, "100000", Duration.ofDays(20)));

        assertEquals(AlertSeverity.MEDIUM, alerts().get(AlertType.UNUSUAL_AMOUNT).getSeverity());
    }

    @Test
    void shouldFlagTheEleventhOperationOfTheDay() {
        evaluate(TransactionType.DEPOSIT, "10000", earlierSmallTransactionsToday(10));

        assertTrue(alerts().containsKey(AlertType.UNUSUAL_FREQUENCY));
    }

    @Test
    void shouldAllowTheTenthOperationOfTheDay() {
        evaluate(TransactionType.DEPOSIT, "10000", earlierSmallTransactionsToday(9));

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreOperationsOlderThan24HoursForFrequency() {
        List<CoreTransaction> old = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            old.add(tx(TransactionType.DEPOSIT, "10000", Duration.ofHours(25 + i)));
        }

        evaluate(TransactionType.DEPOSIT, "10000", old.toArray(CoreTransaction[]::new));

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldFlagStructuringOnThirdOperationJustBelowTheThreshold() {
        // Fraccionamiento ("pitufeo"): partir ~$28M en tres operaciones de ~$9M para no superar el umbral.
        evaluate(TransactionType.DEPOSIT, "9500000",
                tx(TransactionType.DEPOSIT, "9000000", Duration.ofHours(5)),
                tx(TransactionType.DEPOSIT, "9200000", Duration.ofHours(2)));

        Map<AlertType, ComplianceAlert> alerts = alerts();
        assertEquals(AlertSeverity.HIGH, alerts.get(AlertType.STRUCTURING_PATTERN).getSeverity());
        assertFalse(alerts.containsKey(AlertType.UNUSUAL_AMOUNT), "ninguna operación supera el umbral por sí sola");
    }

    @Test
    void shouldNotFlagStructuringWithOnlyTwoOperationsNearTheThreshold() {
        evaluate(TransactionType.DEPOSIT, "9500000",
                tx(TransactionType.DEPOSIT, "9000000", Duration.ofHours(5)));

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldFlagMoneyThatLeavesRightAfterArriving() {
        evaluate(TransactionType.EXTERNAL_PAYOUT, "950000",
                tx(TransactionType.DEPOSIT, "1000000", Duration.ofMinutes(10)));

        assertEquals(AlertSeverity.HIGH, alerts().get(AlertType.RAPID_IN_OUT).getSeverity());
    }

    @Test
    void shouldNotFlagRapidInOutWhenTheDepositIsOlderThanTheWindow() {
        evaluate(TransactionType.WITHDRAWAL, "950000",
                tx(TransactionType.DEPOSIT, "1000000", Duration.ofMinutes(90)));

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldNotFlagRapidInOutWhenAmountsAreNotSimilar() {
        evaluate(TransactionType.TRANSFER, "300000",
                tx(TransactionType.DEPOSIT, "1000000", Duration.ofMinutes(10)));

        verify(alertRepository, never()).save(any());
    }

    @Test
    void shouldNeverBlockTheOperationWhenEvaluationFails() {
        when(transactionRepository.findByAccountId(accountId)).thenThrow(new IllegalStateException("BD caída"));

        assertDoesNotThrow(() ->
                engine.evaluate(userId, accountId, transactionId, TransactionType.TRANSFER, new BigDecimal("50000000")));

        verify(alertRepository, never()).save(any());
    }

    /** Evalúa una operación nueva; el historial incluye las anteriores más la propia operación. */
    private void evaluate(TransactionType type, String amount, CoreTransaction... earlier) {
        List<CoreTransaction> history = new ArrayList<>(List.of(earlier));
        history.add(tx(transactionId, type, amount, Duration.ZERO));
        when(transactionRepository.findByAccountId(accountId)).thenReturn(history);

        engine.evaluate(userId, accountId, transactionId, type, new BigDecimal(amount));
    }

    private CoreTransaction[] earlierSmallTransactionsToday(int count) {
        CoreTransaction[] transactions = new CoreTransaction[count];
        for (int i = 0; i < count; i++) {
            transactions[i] = tx(TransactionType.DEPOSIT, "10000", Duration.ofHours(1 + i));
        }
        return transactions;
    }

    private CoreTransaction tx(TransactionType type, String amount, Duration ago) {
        return tx(UUID.randomUUID(), type, amount, ago);
    }

    private CoreTransaction tx(UUID id, TransactionType type, String amount, Duration ago) {
        return CoreTransaction.builder()
                .id(id)
                .destinationAccountId(accountId)
                .amount(new BigDecimal(amount))
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .createdAt(now.minus(ago))
                .build();
    }

    private Map<AlertType, ComplianceAlert> alerts() {
        ArgumentCaptor<ComplianceAlert> captor = ArgumentCaptor.forClass(ComplianceAlert.class);
        verify(alertRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream().collect(Collectors.toMap(ComplianceAlert::getAlertType, Function.identity()));
    }
}
