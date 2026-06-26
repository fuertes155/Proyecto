package com.cooperativa.met.application.savings.usecase;

import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.model.PlatformRevenue;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.domain.savings.model.ContributionStatus;
import com.cooperativa.met.domain.savings.model.ScheduledContribution;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.port.DebitSourcePort;
import com.cooperativa.met.domain.savings.port.SavingsBalanceCachePort;
import com.cooperativa.met.domain.savings.port.ScheduledContributionPort;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import com.cooperativa.met.domain.savings.service.ContributionDateCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDueContributionsUseCase {

    private final ScheduledSavingsAccountPort accountPort;
    private final ScheduledContributionPort contributionPort;
    private final DebitSourcePort debitSourcePort;
    private final SavingsBalanceCachePort balanceCachePort;

    private final FeeScheduleRepositoryPort feeRepositoryPort;
    private final PlatformRevenuePort platformRevenuePort;

    @Transactional
    public int execute() {
        LocalDate today = LocalDate.now();
        List<ScheduledSavingsAccount> dueAccounts = accountPort.findDueAccounts(today, ScheduledSavingsStatus.ACTIVE);

        // Obtener la tarifa vigente
        Optional<FeeSchedule> depositFeeOpt = feeRepositoryPort.findVigentes().stream()
                .filter(f -> "DEPOSIT_FEE".equals(f.getTipoTarifa()))
                .findFirst();

        int processed = 0;

        for (ScheduledSavingsAccount account : dueAccounts) {
            processAccount(account, today, depositFeeOpt.orElse(null));
            processed++;
        }

        if (processed > 0) {
            log.info("Procesados {} aportes automáticos de ahorro programado", processed);
        }
        return processed;
    }

    private void processAccount(ScheduledSavingsAccount account, LocalDate today, FeeSchedule depositFee) {
        BigDecimal baseAmount = account.getContributionAmount();
        BigDecimal feeAmount = BigDecimal.ZERO;

        if (depositFee != null) {
            if (depositFee.isEsPorcentaje()) {
                feeAmount = baseAmount.multiply(depositFee.getValor()).divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_UP);
            } else {
                feeAmount = depositFee.getValor();
            }
        }

        BigDecimal totalCharge = baseAmount.add(feeAmount);

        ScheduledContribution pending = ScheduledContribution.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .amount(baseAmount) // The saving goal stays pure
                .scheduledDate(today)
                .status(ContributionStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        String reference = "SCHED-SAV-" + account.getId();
        // Cargamos el total (Ahorro + Comisión)
        boolean debited = debitSourcePort.debit(account.getUserId(), totalCharge, reference);

        ScheduledContribution result;
        ScheduledSavingsAccount updatedAccount;

        if (debited) {
            result = contributionPort.save(pending.markCompleted());
            updatedAccount = account
                    .withBalance(account.getCurrentBalance().add(baseAmount))
                    .withNextContributionDate(ContributionDateCalculator.calculateNextDate(
                            account.getFrequency(),
                            account.getDebitDayOfWeek(),
                            account.getDebitDayOfMonth(),
                            today));

            if (updatedAccount.isTargetReached()) {
                updatedAccount = updatedAccount.withStatus(ScheduledSavingsStatus.COMPLETED);
            }

            // Guardar la ganancia de la plataforma si hubo cobro
            if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
                PlatformRevenue revenue = PlatformRevenue.builder()
                        .id(UUID.randomUUID())
                        .userId(account.getUserId())
                        .amount(feeAmount)
                        .description("Comisión por aporte programado")
                        .source("SCHEDULED_SAVINGS_DEPOSIT")
                        .createdAt(Instant.now())
                        .build();
                platformRevenuePort.save(revenue);
                log.info("Comisión de {} cobrada al usuario {}", feeAmount, account.getUserId());
            }

        } else {
            result = contributionPort.save(
                    pending.markFailed("Fondos insuficientes en cuenta origen (Monto requerido: " + totalCharge + ")"));
            updatedAccount = account.withNextContributionDate(ContributionDateCalculator.calculateNextDate(
                    account.getFrequency(),
                    account.getDebitDayOfWeek(),
                    account.getDebitDayOfMonth(),
                    today));
            log.warn("Aporte fallido para cuenta {}: fondos insuficientes", account.getId());
        }

        accountPort.save(updatedAccount);
        balanceCachePort.invalidate(account.getId());
        balanceCachePort.cacheBalance(account.getId(), updatedAccount.getCurrentBalance());
        log.debug("Aporte {} procesado con estado {}", result.getId(), result.getStatus());
    }
}
