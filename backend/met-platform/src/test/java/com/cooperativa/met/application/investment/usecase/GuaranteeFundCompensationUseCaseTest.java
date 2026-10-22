package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.service.GuaranteeFundService;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.investment.model.GuaranteeFundMovement;
import com.cooperativa.met.domain.investment.model.GuaranteeFundMovementType;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.model.LedgerEntryCategory;
import com.cooperativa.met.domain.investment.model.LedgerEntryType;
import com.cooperativa.met.domain.investment.port.GuaranteeFundPort;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.infrastructure.config.CapitalEngineProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Mora sostenida: el Fondo de Garantías cubre el 70% de lo adeudado (nunca el 100%) y lo reparte
 * entre los inversionistas del préstamo en proporción a lo que aportó cada uno.
 */
@ExtendWith(MockitoExtension.class)
class GuaranteeFundCompensationUseCaseTest {

    @Mock private InvestmentMatchRepositoryPort matchPort;
    @Mock private InvestmentFractionRepositoryPort fractionPort;
    @Mock private CoreAccountRepositoryPort accountPort;
    @Mock private LedgerRepositoryPort ledgerPort;
    @Mock private GuaranteeFundPort fundPort;

    private GuaranteeFundCompensationUseCase compensationUseCase;

    private final UUID loanId = UUID.randomUUID();
    private final UUID investorA = UUID.randomUUID();
    private final UUID investorB = UUID.randomUUID();
    private final List<CoreAccount> savedAccounts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        compensationUseCase = new GuaranteeFundCompensationUseCase(matchPort, fractionPort, accountPort, ledgerPort,
                new GuaranteeFundService(fundPort), new CapitalEngineProperties());
    }

    @Test
    void shouldCoverSeventyPercentSplitByHowMuchEachInvestorFunded() {
        // A aportó 30.000 en dos fracciones y B 10.000: A recibe el 75% de la cobertura y B el 25%.
        givenFractions(fraction(investorA, "20000"), fraction(investorA, "10000"), fraction(investorB, "10000"));
        when(fundPort.getBalance()).thenReturn(new BigDecimal("5000000"));
        givenInvestorAccounts();

        compensationUseCase.compensate(loan(), lateInstallment("90000", "10000"));

        Map<UUID, BigDecimal> credited = savedAccounts.stream()
                .collect(Collectors.toMap(CoreAccount::getId, CoreAccount::getPrincipalBalance));
        assertEquals(new BigDecimal("52500.00"), credited.get(investorA));
        assertEquals(new BigDecimal("17500.00"), credited.get(investorB));

        ArgumentCaptor<GuaranteeFundMovement> movement = ArgumentCaptor.forClass(GuaranteeFundMovement.class);
        verify(fundPort).save(movement.capture());
        assertEquals(GuaranteeFundMovementType.PAYOUT, movement.getValue().getType());
        assertEquals(new BigDecimal("70000.00"), movement.getValue().getAmount());

        Map<UUID, LedgerEntry> ledger = ledgerEntries().stream().collect(Collectors.toMap(LedgerEntry::getAccountId, Function.identity()));
        assertEquals(LedgerEntryType.CREDIT, ledger.get(investorA).getEntryType());
        assertEquals(LedgerEntryCategory.GUARANTEE_FUND_PAYOUT, ledger.get(investorB).getCategory());
        assertEquals(loanId, ledger.get(investorB).getTransactionReference());
    }

    @Test
    void shouldPayOnlyWhatTheFundHasWithoutLosingCentsInTheSplit() {
        givenFractions(fraction(investorA, "10000"), fraction(investorB, "20000"));
        when(fundPort.getBalance()).thenReturn(new BigDecimal("1000.00"));
        givenInvestorAccounts();

        compensationUseCase.compensate(loan(), lateInstallment("90000", "10000"));

        BigDecimal paid = savedAccounts.stream().map(CoreAccount::getPrincipalBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("1000.00").compareTo(paid), "se reparte exactamente lo cubierto: " + paid);
    }

    @Test
    void shouldLeaveInvestorsUncoveredWhenTheFundIsEmpty() {
        givenFractions(fraction(investorA, "10000"));
        when(fundPort.getBalance()).thenReturn(BigDecimal.ZERO);

        compensationUseCase.compensate(loan(), lateInstallment("90000", "10000"));

        verify(fundPort, never()).save(any());
        verifyNoInteractions(accountPort, ledgerPort);
    }

    @Test
    void shouldDoNothingForLoansWithoutInvestors() {
        when(matchPort.findByBorrowerLoanId(loanId)).thenReturn(List.of());

        compensationUseCase.compensate(loan(), lateInstallment("90000", "10000"));

        verifyNoInteractions(fractionPort, fundPort, accountPort, ledgerPort);
    }

    private void givenFractions(InvestmentFraction... fractions) {
        List<InvestmentMatch> matches = new ArrayList<>();
        for (InvestmentFraction fraction : fractions) {
            matches.add(InvestmentMatch.builder().id(UUID.randomUUID()).fractionId(fraction.getId()).borrowerLoanId(loanId).build());
            when(fractionPort.findById(fraction.getId())).thenReturn(Optional.of(fraction));
        }
        when(matchPort.findByBorrowerLoanId(loanId)).thenReturn(matches);
    }

    private void givenInvestorAccounts() {
        for (UUID accountId : List.of(investorA, investorB)) {
            org.mockito.Mockito.lenient().when(accountPort.findById(accountId)).thenReturn(Optional.of(CoreAccount.builder()
                    .id(accountId).principalBalance(BigDecimal.ZERO).interestBalance(BigDecimal.ZERO).status(AccountStatus.ACTIVE).build()));
        }
        when(accountPort.save(any())).thenAnswer(inv -> {
            savedAccounts.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    private InvestmentFraction fraction(UUID investorAccountId, String amount) {
        return InvestmentFraction.builder()
                .id(UUID.randomUUID())
                .investorAccountId(investorAccountId)
                .amount(new BigDecimal(amount))
                .status(InvestmentFractionStatus.MATCHED)
                .build();
    }

    private PersonalLoanApplication loan() {
        return PersonalLoanApplication.builder().id(loanId).userId(UUID.randomUUID()).build();
    }

    private AmortizationInstallment lateInstallment(String principal, String interest) {
        return AmortizationInstallment.builder()
                .id(UUID.randomUUID())
                .applicationId(loanId)
                .installmentNumber(3)
                .principalAmount(new BigDecimal(principal))
                .interestAmount(new BigDecimal(interest))
                .status("LATE")
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<LedgerEntry> ledgerEntries() {
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerPort).saveAll(captor.capture());
        return captor.getValue();
    }
}
