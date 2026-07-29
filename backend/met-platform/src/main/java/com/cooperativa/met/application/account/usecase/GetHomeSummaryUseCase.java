package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.HomeSummaryResponse;
import com.cooperativa.met.application.account.dto.HomeSummaryResponse.RecentTransactionResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPortfolioPort;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetHomeSummaryUseCase {

    private static final int MAX_RECENT_TRANSACTIONS = 10;
    private static final Set<LoanApplicationStatus> PENDING_LOAN_STATUSES =
            Set.of(LoanApplicationStatus.SUBMITTED, LoanApplicationStatus.IN_REVIEW);

    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final ScheduledSavingsAccountPort savingsAccountPort;
    private final MicroInvestmentPortfolioPort investmentPortfolioPort;
    private final PersonalLoanApplicationPort loanApplicationPort;

    @Transactional(readOnly = true)
    public HomeSummaryResponse execute(UUID userId) {
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una cuenta para el usuario"));

        List<RecentTransactionResponse> recentTransactions = transactionRepository.findByAccountId(account.getId())
                .stream()
                .sorted(Comparator.comparing(CoreTransaction::getCreatedAt).reversed())
                .limit(MAX_RECENT_TRANSACTIONS)
                .map(tx -> new RecentTransactionResponse(
                        tx.getId(),
                        tx.getType().name(),
                        tx.getAmount(),
                        tx.getConcept(),
                        tx.getCreatedAt(),
                        account.getId().equals(tx.getDestinationAccountId())
                ))
                .toList();

        BigDecimal savingsTotal = savingsAccountPort.findByUserId(userId).stream()
                .filter(s -> s.getStatus() == ScheduledSavingsStatus.ACTIVE)
                .map(s -> s.getCurrentBalance() == null ? BigDecimal.ZERO : s.getCurrentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal investmentsTotal = investmentPortfolioPort.findByUserId(userId).stream()
                .filter(p -> p.getEstado() == InvestmentStatus.ACTIVE)
                .map(p -> p.getMontoTotal() == null ? BigDecimal.ZERO : p.getMontoTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int pendingLoans = (int) loanApplicationPort.findByUserId(userId).stream()
                .filter(l -> PENDING_LOAN_STATUSES.contains(l.getStatus()))
                .count();

        return new HomeSummaryResponse(
                userId,
                account.getPrincipalBalance(),
                account.getInterestBalance(),
                account.getAccountNumber(),
                account.getStatus().name(),
                recentTransactions,
                savingsTotal,
                investmentsTotal,
                pendingLoans
        );
    }
}
