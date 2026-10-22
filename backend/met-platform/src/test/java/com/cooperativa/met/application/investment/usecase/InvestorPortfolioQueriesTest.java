package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.dto.GuaranteeFundSummaryResponse;
import com.cooperativa.met.application.investment.dto.InvestmentBreakdownItemResponse;
import com.cooperativa.met.application.investment.dto.InvestmentPortfolioSummaryResponse;
import com.cooperativa.met.application.investment.service.GuaranteeFundService;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.investment.model.GuaranteeFundMovement;
import com.cooperativa.met.domain.investment.model.GuaranteeFundMovementType;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.port.GuaranteeFundPort;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.infrastructure.config.CapitalEngineProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestorPortfolioQueriesTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    @Mock private CoreAccountRepositoryPort accountRepository;
    @Mock private InvestmentFractionRepositoryPort fractionRepository;
    @Mock private InvestmentMatchRepositoryPort matchRepository;
    @Mock private PersonalLoanApplicationPort loanRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private GuaranteeFundPort guaranteeFundPort;
    @Mock private MatchFractionsToDebtorsUseCase matchUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID activeLoan = UUID.randomUUID();
    private final UUID paidOffLoan = UUID.randomUUID();

    @Test
    void shouldSummarizeWhereEachPesoOfTheInvestorIs() {
        givenInvestorFractions();
        GetInvestorPortfolioSummaryUseCase summary = new GetInvestorPortfolioSummaryUseCase(
                accountRepository, fractionRepository, matchRepository, loanRepository);

        InvestmentPortfolioSummaryResponse response = summary.execute(userId);

        // El padre SPLIT (50.000) no se cuenta: su dinero ya vive en las fracciones hijas.
        assertEquals(new BigDecimal("50000"), response.totalInvested());
        assertEquals(new BigDecimal("10000"), response.availableAmount());
        assertEquals(new BigDecimal("20000"), response.activeAmount());
        assertEquals(new BigDecimal("10000"), response.paidOffAmount());
        assertEquals(new BigDecimal("10000"), response.returnedAmount());
        assertEquals(2, response.loansFundedCount());
    }

    @Test
    void shouldListWhoTheInvestorIsFundingNewestFirst() {
        givenInvestorFractions();
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(User.builder().firstName("Luis").lastName("Gómez").build()));
        GetInvestorFundingBreakdownUseCase breakdown = new GetInvestorFundingBreakdownUseCase(
                accountRepository, fractionRepository, matchRepository, loanRepository, userRepository);

        List<InvestmentBreakdownItemResponse> items = breakdown.execute(userId);

        // Más reciente primero: lo disponible se fecha por su creación, lo prestado por el emparejamiento.
        assertEquals(List.of("DISPONIBLE", "ACTIVO", "PAGADO", "DEVUELTO"),
                items.stream().map(InvestmentBreakdownItemResponse::status).toList());
        assertEquals("Fondo de liquidez", items.get(0).borrowerName());
        assertEquals("Luis Gómez", items.get(2).borrowerName());
        assertEquals(paidOffLoan, items.get(2).loanId());
    }

    @Test
    void shouldReturnEmptyResultsForUsersWithoutAnAccount() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        InvestmentPortfolioSummaryResponse summary = new GetInvestorPortfolioSummaryUseCase(
                accountRepository, fractionRepository, matchRepository, loanRepository).execute(userId);
        List<InvestmentBreakdownItemResponse> items = new GetInvestorFundingBreakdownUseCase(
                accountRepository, fractionRepository, matchRepository, loanRepository, userRepository).execute(userId);

        assertEquals(BigDecimal.ZERO, summary.totalInvested());
        assertTrue(items.isEmpty());
        verifyNoInteractions(fractionRepository);
    }

    @Test
    void shouldSplitDepositIntoFractionsOfAtMostTenThousand() {
        DistributeDepositUseCase distribute = new DistributeDepositUseCase(fractionRepository, matchUseCase);
        UUID depositId = UUID.randomUUID();
        when(fractionRepository.findByOriginalDepositId(depositId)).thenReturn(List.of());

        distribute.execute(accountId, depositId, new BigDecimal("25000.00"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InvestmentFraction>> saved = ArgumentCaptor.forClass(List.class);
        verify(fractionRepository).saveAll(saved.capture());
        assertEquals(List.of(new BigDecimal("10000.00"), new BigDecimal("10000.00"), new BigDecimal("5000.00")),
                saved.getValue().stream().map(InvestmentFraction::getAmount).toList());
        assertTrue(saved.getValue().stream().allMatch(f -> f.getStatus() == InvestmentFractionStatus.AVAILABLE));
        verify(matchUseCase).execute(saved.getValue());
    }

    @Test
    void shouldNotSplitTheSameDepositTwice() {
        DistributeDepositUseCase distribute = new DistributeDepositUseCase(fractionRepository, matchUseCase);
        UUID depositId = UUID.randomUUID();
        when(fractionRepository.findByOriginalDepositId(depositId)).thenReturn(List.of(fraction(InvestmentFractionStatus.AVAILABLE, "10000")));

        distribute.execute(accountId, depositId, new BigDecimal("25000.00"));

        verify(fractionRepository, never()).saveAll(anyList());
        verifyNoInteractions(matchUseCase);
    }

    @Test
    void shouldReportGuaranteeFundBalanceRulesAndMovements() {
        when(guaranteeFundPort.getBalance()).thenReturn(new BigDecimal("830000"));
        when(guaranteeFundPort.findAllOrderByCreatedAtDesc()).thenReturn(List.of(GuaranteeFundMovement.builder()
                .id(UUID.randomUUID()).type(GuaranteeFundMovementType.CONTRIBUTION).amount(new BigDecimal("1000"))
                .concept("Aporte").createdAt(Instant.now()).build()));
        GetGuaranteeFundSummaryUseCase fundSummary = new GetGuaranteeFundSummaryUseCase(
                new GuaranteeFundService(guaranteeFundPort), guaranteeFundPort, new CapitalEngineProperties());

        GuaranteeFundSummaryResponse response = fundSummary.execute();

        assertEquals(new BigDecimal("830000"), response.balance());
        assertEquals(new BigDecimal("0.70"), response.coverageRatio());
        assertEquals(90, response.activationDaysLate());
        assertEquals("CONTRIBUTION", response.movements().get(0).type());
    }

    private void givenInvestorFractions() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(CoreAccount.builder().id(accountId).userId(userId).build()));
        InvestmentFraction split = fraction(InvestmentFractionStatus.SPLIT, "50000");
        InvestmentFraction available = fraction(InvestmentFractionStatus.AVAILABLE, "10000");
        InvestmentFraction returned = fraction(InvestmentFractionStatus.RETURNED, "10000");
        InvestmentFraction fundingActive = fraction(InvestmentFractionStatus.MATCHED, "20000");
        InvestmentFraction fundingPaidOff = fraction(InvestmentFractionStatus.MATCHED, "10000");
        when(fractionRepository.findByInvestorAccountId(accountId)).thenReturn(List.of(split, available, returned, fundingActive, fundingPaidOff));
        org.mockito.Mockito.lenient().when(matchRepository.findByFractionId(returned.getId())).thenReturn(List.of(match(returned, activeLoan, 4)));
        when(matchRepository.findByFractionId(fundingActive.getId())).thenReturn(List.of(match(fundingActive, activeLoan, 2)));
        when(matchRepository.findByFractionId(fundingPaidOff.getId())).thenReturn(List.of(match(fundingPaidOff, paidOffLoan, 3)));
        when(loanRepository.findById(activeLoan)).thenReturn(Optional.of(loan(activeLoan, LoanApplicationStatus.DISBURSED)));
        when(loanRepository.findById(paidOffLoan)).thenReturn(Optional.of(loan(paidOffLoan, LoanApplicationStatus.PAID_OFF)));
    }

    private InvestmentFraction fraction(InvestmentFractionStatus status, String amount) {
        return InvestmentFraction.builder()
                .id(UUID.randomUUID())
                .investorAccountId(accountId)
                .amount(new BigDecimal(amount))
                .status(status)
                .createdAt(NOW.minus(Duration.ofDays(1)))
                .build();
    }

    private InvestmentMatch match(InvestmentFraction fraction, UUID loanId, int daysAgo) {
        return InvestmentMatch.builder().id(UUID.randomUUID()).fractionId(fraction.getId()).borrowerLoanId(loanId)
                .matchedAt(NOW.minus(Duration.ofDays(daysAgo))).build();
    }

    private PersonalLoanApplication loan(UUID id, LoanApplicationStatus status) {
        return PersonalLoanApplication.builder().id(id).userId(UUID.randomUUID()).status(status).build();
    }

    private static <T> T any(Class<T> type) {
        return org.mockito.ArgumentMatchers.any(type);
    }
}
