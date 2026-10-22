package com.cooperativa.met.application.savings.usecase;

import com.cooperativa.met.application.savings.dto.ScheduledSavingsResponse;
import com.cooperativa.met.application.savings.dto.UpdateScheduledSavingsRequest;
import com.cooperativa.met.application.savings.mapper.ScheduledSavingsMapper;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.model.PlatformRevenue;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.savings.model.ContributionFrequency;
import com.cooperativa.met.domain.savings.model.ContributionStatus;
import com.cooperativa.met.domain.savings.model.ScheduledContribution;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.port.DebitSourcePort;
import com.cooperativa.met.domain.savings.port.SavingsBalanceCachePort;
import com.cooperativa.met.domain.savings.port.ScheduledContributionPort;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Débito automático de los ahorros programados (con su comisión) y cambios de estado pedidos por el socio. */
@ExtendWith(MockitoExtension.class)
class ScheduledSavingsProcessingTest {

    @Mock private ScheduledSavingsAccountPort accountPort;
    @Mock private ScheduledContributionPort contributionPort;
    @Mock private DebitSourcePort debitSourcePort;
    @Mock private SavingsBalanceCachePort balanceCachePort;
    @Mock private FeeScheduleRepositoryPort feeRepositoryPort;
    @Mock private PlatformRevenuePort platformRevenuePort;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @ParameterizedTest(name = "comisión {0} (porcentaje={1}) → cobro total {2}")
    @CsvSource({
            "1.5, true, 101500.00, 1500.00",
            "2500, false, 102500, 2500"
    })
    void shouldChargeSavingPlusFeeButOnlyAddTheSavingToTheGoal(String feeValue, boolean percentage, String totalCharge, String fee) {
        givenDueAccount("400000", "1000000");
        givenDepositFee(feeValue, percentage);
        when(debitSourcePort.debit(userId, new BigDecimal(totalCharge), "SCHED-SAV-" + accountId)).thenReturn(true);
        givenContributionsSaved();

        assertEquals(1, processor().execute());

        ScheduledSavingsAccount saved = savedAccount();
        assertEquals(new BigDecimal("500000"), saved.getCurrentBalance(), "la comisión no se suma a la meta");
        assertTrue(saved.getNextContributionDate().isAfter(LocalDate.now()));
        assertEquals(ScheduledSavingsStatus.ACTIVE, saved.getStatus());
        assertEquals(ContributionStatus.COMPLETED, savedContribution().getStatus());

        ArgumentCaptor<PlatformRevenue> revenue = ArgumentCaptor.forClass(PlatformRevenue.class);
        verify(platformRevenuePort).save(revenue.capture());
        assertEquals(new BigDecimal(fee), revenue.getValue().getAmount());
        verify(balanceCachePort).cacheBalance(accountId, new BigDecimal("500000"));
    }

    @Test
    void shouldCompleteTheGoalWhenTheTargetIsReached() {
        givenDueAccount("900000", "1000000");
        when(feeRepositoryPort.findVigentes()).thenReturn(List.of());
        when(debitSourcePort.debit(eq(userId), eq(new BigDecimal("100000")), anyString())).thenReturn(true);
        givenContributionsSaved();

        processor().execute();

        assertEquals(ScheduledSavingsStatus.COMPLETED, savedAccount().getStatus());
        verifyNoInteractions(platformRevenuePort);
    }

    @Test
    void shouldRecordFailedContributionAndTryAgainNextPeriod() {
        givenDueAccount("400000", "1000000");
        when(feeRepositoryPort.findVigentes()).thenReturn(List.of());
        when(debitSourcePort.debit(any(), any(), any())).thenReturn(false);
        givenContributionsSaved();

        processor().execute();

        ScheduledContribution failed = savedContribution();
        assertEquals(ContributionStatus.FAILED, failed.getStatus());
        assertTrue(failed.getFailureReason().contains("Fondos insuficientes"));
        ScheduledSavingsAccount saved = savedAccount();
        assertEquals(new BigDecimal("400000"), saved.getCurrentBalance());
        assertTrue(saved.getNextContributionDate().isAfter(LocalDate.now()));
        verify(platformRevenuePort, never()).save(any());
    }

    @Test
    void shouldPauseResumeAndCancelFollowingTheAllowedTransitions() {
        UpdateScheduledSavingsUseCase update = new UpdateScheduledSavingsUseCase(accountPort, balanceCachePort, new ScheduledSavingsMapper());
        when(accountPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        givenOwnedAccount(ScheduledSavingsStatus.ACTIVE);
        assertEquals(ScheduledSavingsStatus.PAUSED, update.execute(userId, accountId, new UpdateScheduledSavingsRequest(null, ScheduledSavingsStatus.PAUSED)).status());

        givenOwnedAccount(ScheduledSavingsStatus.PAUSED);
        ScheduledSavingsResponse resumed = update.execute(userId, accountId,
                new UpdateScheduledSavingsRequest(new BigDecimal("80000"), ScheduledSavingsStatus.ACTIVE));
        assertEquals(ScheduledSavingsStatus.ACTIVE, resumed.status());
        assertEquals(new BigDecimal("80000"), resumed.contributionAmount());

        givenOwnedAccount(ScheduledSavingsStatus.PAUSED);
        assertEquals(ScheduledSavingsStatus.CANCELLED, update.execute(userId, accountId, new UpdateScheduledSavingsRequest(null, ScheduledSavingsStatus.CANCELLED)).status());
    }

    @Test
    void shouldRejectTransitionsThatMakeNoSense() {
        UpdateScheduledSavingsUseCase update = new UpdateScheduledSavingsUseCase(accountPort, balanceCachePort, new ScheduledSavingsMapper());

        givenOwnedAccount(ScheduledSavingsStatus.PAUSED);
        assertEquals("INVALID_STATUS_TRANSITION", assertThrows(BusinessRuleException.class, () -> update.execute(userId, accountId,
                new UpdateScheduledSavingsRequest(null, ScheduledSavingsStatus.PAUSED))).getCode());

        givenOwnedAccount(ScheduledSavingsStatus.ACTIVE);
        assertEquals("INVALID_STATUS_TRANSITION", assertThrows(BusinessRuleException.class, () -> update.execute(userId, accountId,
                new UpdateScheduledSavingsRequest(null, ScheduledSavingsStatus.ACTIVE))).getCode());
        assertEquals("INVALID_STATUS_TRANSITION", assertThrows(BusinessRuleException.class, () -> update.execute(userId, accountId,
                new UpdateScheduledSavingsRequest(null, ScheduledSavingsStatus.COMPLETED))).getCode());

        givenOwnedAccount(ScheduledSavingsStatus.COMPLETED);
        assertEquals("ACCOUNT_NOT_MODIFIABLE", assertThrows(BusinessRuleException.class, () -> update.execute(userId, accountId,
                new UpdateScheduledSavingsRequest(new BigDecimal("5000"), null))).getCode());

        verify(accountPort, never()).save(any());
    }

    @Test
    void shouldNotLetUsersEditSomeoneElsesSavings() {
        UpdateScheduledSavingsUseCase update = new UpdateScheduledSavingsUseCase(accountPort, balanceCachePort, new ScheduledSavingsMapper());
        when(accountPort.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> update.execute(userId, accountId,
                new UpdateScheduledSavingsRequest(new BigDecimal("5000"), null)));
    }

    private ProcessDueContributionsUseCase processor() {
        return new ProcessDueContributionsUseCase(accountPort, contributionPort, debitSourcePort, balanceCachePort,
                feeRepositoryPort, platformRevenuePort);
    }

    private void givenDueAccount(String balance, String target) {
        when(accountPort.findDueAccounts(any(), eq(ScheduledSavingsStatus.ACTIVE))).thenReturn(List.of(account(ScheduledSavingsStatus.ACTIVE)
                .toBuilder().currentBalance(new BigDecimal(balance)).targetAmount(new BigDecimal(target)).build()));
    }

    private void givenOwnedAccount(ScheduledSavingsStatus status) {
        when(accountPort.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account(status)));
    }

    private void givenDepositFee(String value, boolean percentage) {
        when(feeRepositoryPort.findVigentes()).thenReturn(List.of(
                FeeSchedule.builder().tipoTarifa("WITHDRAWAL_FEE").valor(new BigDecimal("9999")).vigentDesde(Instant.now()).build(),
                FeeSchedule.builder().tipoTarifa("DEPOSIT_FEE").valor(new BigDecimal(value)).esPorcentaje(percentage).vigentDesde(Instant.now()).build()));
    }

    private void givenContributionsSaved() {
        when(contributionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ScheduledSavingsAccount account(ScheduledSavingsStatus status) {
        return ScheduledSavingsAccount.builder()
                .id(accountId)
                .userId(userId)
                .name("Vacaciones")
                .targetAmount(new BigDecimal("1000000"))
                .contributionAmount(new BigDecimal("100000"))
                .frequency(ContributionFrequency.WEEKLY)
                .debitDayOfWeek(5)
                .currentBalance(new BigDecimal("400000"))
                .status(status)
                .nextContributionDate(LocalDate.now())
                .createdAt(Instant.now())
                .build();
    }

    private ScheduledSavingsAccount savedAccount() {
        ArgumentCaptor<ScheduledSavingsAccount> captor = ArgumentCaptor.forClass(ScheduledSavingsAccount.class);
        verify(accountPort).save(captor.capture());
        return captor.getValue();
    }

    private ScheduledContribution savedContribution() {
        ArgumentCaptor<ScheduledContribution> captor = ArgumentCaptor.forClass(ScheduledContribution.class);
        verify(contributionPort).save(captor.capture());
        return captor.getValue();
    }
}
