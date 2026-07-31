package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.infrastructure.config.CapitalEngineProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchFractionsToDebtorsUseCaseTest {

    @Mock
    private PersonalLoanApplicationPort loanPort;
    @Mock
    private InvestmentFractionRepositoryPort fractionPort;
    @Mock
    private InvestmentMatchRepositoryPort matchPort;
    @Mock
    private LedgerRepositoryPort ledgerPort;
    @Mock
    private CoreAccountRepositoryPort accountPort;

    private MatchFractionsToDebtorsUseCase useCase;
    private CapitalEngineProperties properties;

    private UUID investorAccountId;
    private UUID depositId;

    @BeforeEach
    void setUp() {
        properties = new CapitalEngineProperties();
        useCase = new MatchFractionsToDebtorsUseCase(loanPort, fractionPort, matchPort, ledgerPort, accountPort, properties);

        investorAccountId = UUID.randomUUID();
        depositId = UUID.randomUUID();

        when(matchPort.findByBorrowerLoanId(any())).thenReturn(List.of());
    }

    private PersonalLoanApplication loanNeeding(BigDecimal amount) {
        UUID loanId = UUID.randomUUID();
        UUID borrowerUserId = UUID.randomUUID();
        when(accountPort.findByUserId(borrowerUserId)).thenReturn(Optional.of(
                CoreAccount.builder().id(UUID.randomUUID()).userId(borrowerUserId).status(AccountStatus.ACTIVE)
                        .principalBalance(BigDecimal.ZERO).interestBalance(BigDecimal.ZERO).build()));
        return PersonalLoanApplication.builder()
                .id(loanId).userId(borrowerUserId).amount(amount).status(LoanApplicationStatus.APPROVED).build();
    }

    private InvestmentFraction newFraction(BigDecimal amount) {
        return InvestmentFraction.builder()
                .id(UUID.randomUUID())
                .investorAccountId(investorAccountId)
                .originalDepositId(depositId)
                .amount(amount)
                .status(InvestmentFractionStatus.AVAILABLE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void splitsAFractionAcrossTwoLoansWhenNeitherAloneCoversIt() {
        properties.setConcentrationCapPercentage(new BigDecimal("1.00")); // sin tope, para aislar el split

        PersonalLoanApplication loan1 = loanNeeding(new BigDecimal("10000.00"));
        PersonalLoanApplication loan2 = loanNeeding(new BigDecimal("20000.00"));
        when(loanPort.findByStatus(LoanApplicationStatus.APPROVED)).thenReturn(List.of(loan1, loan2));

        InvestmentFraction fraction = newFraction(new BigDecimal("15000.00"));
        when(fractionPort.findByOriginalDepositId(depositId)).thenReturn(List.of(fraction));

        useCase.execute(List.of(fraction));

        ArgumentCaptor<List<InvestmentFraction>> fractionsCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(fractionPort).saveAll(fractionsCaptor.capture());
        List<InvestmentFraction> saved = fractionsCaptor.getValue();

        // La original queda SPLIT, y se crean 2 piezas MATCHED (10,000 + 5,000), sin remanente AVAILABLE.
        assertEquals(3, saved.size());
        assertEquals(1, saved.stream().filter(f -> f.getStatus() == InvestmentFractionStatus.SPLIT).count());
        List<InvestmentFraction> matchedPieces = saved.stream()
                .filter(f -> f.getStatus() == InvestmentFractionStatus.MATCHED)
                .sorted(Comparator.comparing(InvestmentFraction::getAmount).reversed())
                .toList();
        assertEquals(2, matchedPieces.size());
        assertEquals(new BigDecimal("10000.00"), matchedPieces.get(0).getAmount());
        assertEquals(new BigDecimal("5000.00"), matchedPieces.get(1).getAmount());
        assertEquals(0, saved.stream().filter(f -> f.getStatus() == InvestmentFractionStatus.AVAILABLE).count());

        ArgumentCaptor<List<InvestmentMatch>> matchesCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(matchPort).saveAll(matchesCaptor.capture());
        assertEquals(2, matchesCaptor.getValue().size());

        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(ledgerPort).saveAll(ledgerCaptor.capture());
        assertEquals(4, ledgerCaptor.getValue().size()); // DEBIT+CREDIT por cada uno de los 2 matches
    }

    @Test
    void neverAllocatesMoreThanALoanActuallyNeeds_noOverfunding() {
        properties.setConcentrationCapPercentage(new BigDecimal("1.00"));

        PersonalLoanApplication loan = loanNeeding(new BigDecimal("5000.00"));
        when(loanPort.findByStatus(LoanApplicationStatus.APPROVED)).thenReturn(List.of(loan));

        InvestmentFraction fraction = newFraction(new BigDecimal("10000.00"));
        when(fractionPort.findByOriginalDepositId(depositId)).thenReturn(List.of(fraction));

        useCase.execute(List.of(fraction));

        ArgumentCaptor<List<InvestmentMatch>> matchesCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(matchPort).saveAll(matchesCaptor.capture());
        assertEquals(1, matchesCaptor.getValue().size());

        ArgumentCaptor<List<InvestmentFraction>> fractionsCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(fractionPort).saveAll(fractionsCaptor.capture());
        List<InvestmentFraction> saved = fractionsCaptor.getValue();

        BigDecimal matchedTotal = saved.stream()
                .filter(f -> f.getStatus() == InvestmentFractionStatus.MATCHED)
                .map(InvestmentFraction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableTotal = saved.stream()
                .filter(f -> f.getStatus() == InvestmentFractionStatus.AVAILABLE)
                .map(InvestmentFraction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Antes del fix, la fracción completa de 10,000 se habría asignado al préstamo de 5,000 (sobre-fondeo).
        assertEquals(new BigDecimal("5000.00"), matchedTotal);
        assertEquals(new BigDecimal("5000.00"), availableTotal);
    }

    @Test
    void concentrationCapLimitsHowMuchOfADepositGoesToASingleDebtor() {
        properties.setConcentrationCapPercentage(new BigDecimal("0.20")); // máx 20% del depósito por deudor

        PersonalLoanApplication loan1 = loanNeeding(new BigDecimal("10000.00"));
        PersonalLoanApplication loan2 = loanNeeding(new BigDecimal("10000.00"));
        when(loanPort.findByStatus(LoanApplicationStatus.APPROVED)).thenReturn(List.of(loan1, loan2));

        InvestmentFraction fraction = newFraction(new BigDecimal("10000.00"));
        // Todo el depósito es esta única fracción -> total depósito = 10,000 -> tope por deudor = 2,000
        when(fractionPort.findByOriginalDepositId(depositId)).thenReturn(List.of(fraction));

        useCase.execute(List.of(fraction));

        ArgumentCaptor<List<InvestmentMatch>> matchesCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(matchPort).saveAll(matchesCaptor.capture());
        assertEquals(2, matchesCaptor.getValue().size()); // topó en ambos deudores, sin agotar el resto

        ArgumentCaptor<List<InvestmentFraction>> fractionsCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(fractionPort).saveAll(fractionsCaptor.capture());
        List<InvestmentFraction> saved = fractionsCaptor.getValue();

        List<InvestmentFraction> matchedPieces = saved.stream()
                .filter(f -> f.getStatus() == InvestmentFractionStatus.MATCHED)
                .toList();
        assertTrue(matchedPieces.stream().allMatch(f -> f.getAmount().compareTo(new BigDecimal("2000.00")) == 0));

        BigDecimal availableTotal = saved.stream()
                .filter(f -> f.getStatus() == InvestmentFractionStatus.AVAILABLE)
                .map(InvestmentFraction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 10,000 - 2,000 - 2,000 = 6,000 sin poder colocarse por el tope de concentración
        assertEquals(new BigDecimal("6000.00"), availableTotal);
    }
}
