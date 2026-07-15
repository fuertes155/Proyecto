package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.infrastructure.persistence.investment.entity.LoanFundingMatchJpaEntity;
import com.cooperativa.met.infrastructure.persistence.investment.repository.LoanFundingMatchJpaRepository;
import com.cooperativa.met.infrastructure.persistence.lending.entity.PersonalLoanApplicationJpaEntity;
import com.cooperativa.met.infrastructure.persistence.lending.repository.PersonalLoanApplicationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchInvestmentToLoansUseCase {

    private final LoanFundingMatchJpaRepository matchRepository;
    private final PersonalLoanApplicationJpaRepository loanRepository;
    private final CoreTransactionRepositoryPort transactionRepository;

    private static final BigDecimal MAX_FRACTION_AMOUNT = new BigDecimal("10000.00");

    @Transactional
    public void distributeInvestment(MicroInvestment investment) {
        BigDecimal remainingAmount = investment.getMontoInvertido();
        
        // Find approved loans waiting for funding (FIFO Queue)
        // Note: For this architectural test, we assume APPROVED status means waiting for funds
        List<PersonalLoanApplicationJpaEntity> pendingLoans = loanRepository.findAll(); 

        for (PersonalLoanApplicationJpaEntity loan : pendingLoans) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Calculate matching amount (min of remaining, max fraction, or loan amount)
            BigDecimal matchAmount = remainingAmount.min(MAX_FRACTION_AMOUNT).min(loan.getAmount());

            if (matchAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 1. Create Pivot Table entry (Matching Engine)
                LoanFundingMatchJpaEntity match = LoanFundingMatchJpaEntity.builder()
                        .id(UUID.randomUUID())
                        .microInvestmentId(investment.getId())
                        .loanApplicationId(loan.getId())
                        .investorUserId(investment.getUserId())
                        .borrowerUserId(loan.getUserId())
                        .matchedAmount(matchAmount)
                        .matchedAt(Instant.now())
                        .build();
                matchRepository.save(match);

                // 2. Double-Entry Ledger System Update
                recordDoubleEntryLedger(investment.getUserId(), loan.getUserId(), matchAmount);

                remainingAmount = remainingAmount.subtract(matchAmount);
                log.info("Matched {} COP from Investor {} to Borrower {}", matchAmount, investment.getUserId(), loan.getUserId());
            }
        }
        
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Could not fully distribute investment {}. Unmatched funds: {}", investment.getId(), remainingAmount);
            // In a real system, the remaining goes to a generic liquidity pool.
        }
    }

    private void recordDoubleEntryLedger(UUID investorId, UUID borrowerId, BigDecimal amount) {
        // Debit the Investor (Investment Funding Vault)
        CoreTransaction debitTx = CoreTransaction.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(investorId) // Simplified: Ideally the generic Vault Account ID
                .destinationAccountId(investorId)
                .type(TransactionType.INVESTMENT_FUNDING)
                .status(TransactionStatus.COMPLETED)
                .amount(amount.negate()) // Debit is negative impact to vault/investor availability
                .concept("Fondeo de crédito fraccionado")
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(debitTx);

        // Credit the Borrower (Loan Disbursement)
        CoreTransaction creditTx = CoreTransaction.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(borrowerId) 
                .destinationAccountId(borrowerId)
                .type(TransactionType.LOAN_DISBURSEMENT)
                .status(TransactionStatus.COMPLETED)
                .amount(amount) // Credit is positive impact to borrower
                .concept("Desembolso fraccionado fondeado")
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(creditTx);
    }
}
