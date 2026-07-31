package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.admin.model.PlatformRevenue;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentReturnPort;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.domain.investment.service.margin.MarginModel;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.infrastructure.config.CapitalEngineProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDistributionUseCaseTest {

    @Mock
    private InvestmentMatchRepositoryPort matchPort;
    @Mock
    private InvestmentFractionRepositoryPort fractionPort;
    @Mock
    private CoreAccountRepositoryPort accountPort;
    @Mock
    private LedgerRepositoryPort ledgerPort;
    @Mock
    private InvestmentReturnPort investmentReturnPort;
    @Mock
    private PlatformRevenuePort platformRevenuePort;

    private PaymentDistributionUseCase useCase;
    private CapitalEngineProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CapitalEngineProperties();
        properties.setMarginModel(MarginModel.INTEREST_COMMISSION);
        properties.setMarginRate(new BigDecimal("0.02"));
        useCase = new PaymentDistributionUseCase(matchPort, fractionPort, accountPort, ledgerPort,
                investmentReturnPort, platformRevenuePort, properties);
    }

    @Test
    void distributesPrincipalAndNetYieldProportionallyBetweenTwoInvestors_andRecordsPlatformCommission() {
        UUID loanId = UUID.randomUUID();
        UUID borrowerUserId = UUID.randomUUID();
        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(loanId).userId(borrowerUserId).amount(new BigDecimal("10000.00"))
                .status(LoanApplicationStatus.APPROVED).build();

        AmortizationInstallment installment = AmortizationInstallment.builder()
                .id(UUID.randomUUID()).applicationId(loanId).installmentNumber(1)
                .principalAmount(new BigDecimal("50000.00"))
                .interestAmount(new BigDecimal("9500.00"))
                .status("PAID").build();

        UUID investor1AccountId = UUID.randomUUID();
        UUID investor2AccountId = UUID.randomUUID();
        UUID investor1UserId = UUID.randomUUID();
        UUID investor2UserId = UUID.randomUUID();
        UUID fraction1Id = UUID.randomUUID();
        UUID fraction2Id = UUID.randomUUID();

        when(matchPort.findByBorrowerLoanId(loanId)).thenReturn(List.of(
                InvestmentMatch.builder().id(UUID.randomUUID()).fractionId(fraction1Id).borrowerLoanId(loanId).build(),
                InvestmentMatch.builder().id(UUID.randomUUID()).fractionId(fraction2Id).borrowerLoanId(loanId).build()));

        when(fractionPort.findById(fraction1Id)).thenReturn(Optional.of(InvestmentFraction.builder()
                .id(fraction1Id).investorAccountId(investor1AccountId).amount(new BigDecimal("6000.00"))
                .status(InvestmentFractionStatus.MATCHED).build()));
        when(fractionPort.findById(fraction2Id)).thenReturn(Optional.of(InvestmentFraction.builder()
                .id(fraction2Id).investorAccountId(investor2AccountId).amount(new BigDecimal("4000.00"))
                .status(InvestmentFractionStatus.MATCHED).build()));

        when(accountPort.findById(investor1AccountId)).thenReturn(Optional.of(CoreAccount.builder()
                .id(investor1AccountId).userId(investor1UserId).status(AccountStatus.ACTIVE)
                .principalBalance(BigDecimal.ZERO).interestBalance(BigDecimal.ZERO).build()));
        when(accountPort.findById(investor2AccountId)).thenReturn(Optional.of(CoreAccount.builder()
                .id(investor2AccountId).userId(investor2UserId).status(AccountStatus.ACTIVE)
                .principalBalance(BigDecimal.ZERO).interestBalance(BigDecimal.ZERO).build()));

        useCase.distribute(loan, installment);

        // Interés generado 9500 * 2% = 190 comisión; rendimiento neto 9310, repartido 60/40.
        ArgumentCaptor<PlatformRevenue> revenueCaptor = ArgumentCaptor.forClass(PlatformRevenue.class);
        verify(platformRevenuePort).save(revenueCaptor.capture());
        assertEquals(new BigDecimal("190.00"), revenueCaptor.getValue().getAmount());

        ArgumentCaptor<CoreAccount> accountCaptor = ArgumentCaptor.forClass(CoreAccount.class);
        verify(accountPort, org.mockito.Mockito.times(2)).save(accountCaptor.capture());
        List<CoreAccount> savedAccounts = accountCaptor.getAllValues();

        CoreAccount investor1Saved = savedAccounts.stream().filter(a -> a.getId().equals(investor1AccountId)).findFirst().orElseThrow();
        CoreAccount investor2Saved = savedAccounts.stream().filter(a -> a.getId().equals(investor2AccountId)).findFirst().orElseThrow();

        assertEquals(new BigDecimal("30000.00"), investor1Saved.getPrincipalBalance()); // 50,000 * 60%
        assertEquals(new BigDecimal("5586.00"), investor1Saved.getInterestBalance());   // 9,310 * 60%
        assertEquals(new BigDecimal("20000.00"), investor2Saved.getPrincipalBalance()); // residuo: 50,000 - 30,000
        assertEquals(new BigDecimal("3724.00"), investor2Saved.getInterestBalance());   // residuo: 9,310 - 5,586

        ArgumentCaptor<List<InvestmentReturn>> ignored = null;
        verify(investmentReturnPort, org.mockito.Mockito.times(2)).save(any(InvestmentReturn.class));

        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerPort).saveAll(ledgerCaptor.capture());
        assertEquals(4, ledgerCaptor.getValue().size()); // capital + rendimiento por cada uno de los 2 inversionistas
    }

    @Test
    void doesNothingWhenLoanHasNoFundingMatches() {
        UUID loanId = UUID.randomUUID();
        PersonalLoanApplication loan = PersonalLoanApplication.builder()
                .id(loanId).userId(UUID.randomUUID()).amount(new BigDecimal("10000.00"))
                .status(LoanApplicationStatus.APPROVED).build();
        AmortizationInstallment installment = AmortizationInstallment.builder()
                .id(UUID.randomUUID()).applicationId(loanId).installmentNumber(1)
                .principalAmount(new BigDecimal("50000.00")).interestAmount(new BigDecimal("9500.00")).build();

        when(matchPort.findByBorrowerLoanId(loanId)).thenReturn(List.of());

        useCase.distribute(loan, installment);

        verify(platformRevenuePort, org.mockito.Mockito.never()).save(any());
        verify(accountPort, org.mockito.Mockito.never()).save(any());
    }
}
