package com.cooperativa.met.application.savings.usecase;

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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDueContributionsUseCase {

    private final ScheduledSavingsAccountPort accountPort;
    private final ScheduledContributionPort contributionPort;
    private final DebitSourcePort debitSourcePort;
    private final SavingsBalanceCachePort balanceCachePort;

    @Transactional
    public int execute() {
        LocalDate today = LocalDate.now();
        List<ScheduledSavingsAccount> dueAccounts = accountPort.findDueAccounts(today, ScheduledSavingsStatus.ACTIVE);
        int processed = 0;

        for (ScheduledSavingsAccount account : dueAccounts) {
            processAccount(account, today);
            processed++;
        }

        if (processed > 0) {
            log.info("Procesados {} aportes automáticos de ahorro programado", processed);
        }
        return processed;
    }

    private void processAccount(ScheduledSavingsAccount account, LocalDate today) {
        ScheduledContribution pending = ScheduledContribution.builder()
                .id(UUID.randomUUID())
                .accountId(account.getId())
                .amount(account.getContributionAmount())
                .scheduledDate(today)
                .status(ContributionStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        String reference = "SCHED-SAV-" + account.getId();
        boolean debited = debitSourcePort.debit(account.getUserId(), account.getContributionAmount(), reference);

        ScheduledContribution result;
        ScheduledSavingsAccount updatedAccount;

        if (debited) {
            result = contributionPort.save(pending.markCompleted());
            updatedAccount = account
                    .withBalance(account.getCurrentBalance().add(account.getContributionAmount()))
                    .withNextContributionDate(ContributionDateCalculator.calculateNextDate(
                            account.getFrequency(),
                            account.getDebitDayOfWeek(),
                            account.getDebitDayOfMonth(),
                            today
                    ));

            if (updatedAccount.isTargetReached()) {
                updatedAccount = updatedAccount.withStatus(ScheduledSavingsStatus.COMPLETED);
            }
        } else {
            result = contributionPort.save(pending.markFailed("Fondos insuficientes en cuenta origen"));
            updatedAccount = account.withNextContributionDate(ContributionDateCalculator.calculateNextDate(
                    account.getFrequency(),
                    account.getDebitDayOfWeek(),
                    account.getDebitDayOfMonth(),
                    today
            ));
            log.warn("Aporte fallido para cuenta {}: fondos insuficientes", account.getId());
        }

        accountPort.save(updatedAccount);
        balanceCachePort.invalidate(account.getId());
        balanceCachePort.cacheBalance(account.getId(), updatedAccount.getCurrentBalance());
        log.debug("Aporte {} procesado con estado {}", result.getId(), result.getStatus());
    }
}
