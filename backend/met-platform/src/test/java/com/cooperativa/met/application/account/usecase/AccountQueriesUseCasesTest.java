package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.AccountStatementResult;
import com.cooperativa.met.application.account.dto.CoreAccountResponse;
import com.cooperativa.met.application.account.dto.HomeSummaryResponse;
import com.cooperativa.met.application.account.dto.MovementResponse;
import com.cooperativa.met.application.account.dto.RecentRecipientResponse;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPortfolioPort;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Lo que el socio ve de su propia cuenta: inicio, movimientos, destinatarios frecuentes y extracto. */
@ExtendWith(MockitoExtension.class)
class AccountQueriesUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-10-15T15:00:00Z");

    @Mock private CoreAccountRepositoryPort accountRepository;
    @Mock private CoreTransactionRepositoryPort transactionRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private ScheduledSavingsAccountPort savingsAccountPort;
    @Mock private MicroInvestmentPortfolioPort portfolioPort;
    @Mock private PersonalLoanApplicationPort loanApplicationPort;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @Test
    void shouldSummarizeHomeWithActiveSavingsInvestmentsAndPendingLoans() {
        givenMyAccount();
        when(transactionRepository.findByAccountId(accountId)).thenReturn(manyTransactions(12));
        when(savingsAccountPort.findByUserId(userId)).thenReturn(List.of(
                savings(ScheduledSavingsStatus.ACTIVE, "300000"), savings(ScheduledSavingsStatus.ACTIVE, null),
                savings(ScheduledSavingsStatus.CANCELLED, "999999")));
        when(portfolioPort.findByUserId(userId)).thenReturn(List.of(
                portfolio(InvestmentStatus.ACTIVE, "500000"), portfolio(InvestmentStatus.COMPLETED, "800000")));
        when(loanApplicationPort.findByUserId(userId)).thenReturn(List.of(
                loan(LoanApplicationStatus.IN_REVIEW), loan(LoanApplicationStatus.SUBMITTED), loan(LoanApplicationStatus.PAID_OFF)));

        HomeSummaryResponse home = new GetHomeSummaryUseCase(accountRepository, transactionRepository, savingsAccountPort,
                portfolioPort, loanApplicationPort).execute(userId);

        assertEquals(10, home.recentTransactions().size(), "solo las 10 más recientes");
        assertEquals(NOW, home.recentTransactions().get(0).createdAt());
        assertEquals(new BigDecimal("300000"), home.savingsTotal());
        assertEquals(new BigDecimal("500000"), home.investmentsTotal());
        assertEquals(2, home.pendingLoans());
        assertEquals("ACTIVE", home.accountStatus());
    }

    @Test
    void shouldLabelEachMovementAndTellIncomingFromOutgoing() {
        givenMyAccount();
        UUID other = UUID.randomUUID();
        when(transactionRepository.findByAccountId(accountId)).thenReturn(new ArrayList<>(List.of(
                tx(TransactionType.TRANSFER, accountId, other, NOW.minus(Duration.ofDays(2)), "Arriendo"),
                tx(TransactionType.DEPOSIT, null, accountId, NOW, "Recarga PSE"),
                tx(TransactionType.EXTERNAL_PAYOUT, accountId, null, NOW.minus(Duration.ofDays(1)), null))));

        List<MovementResponse> movements = new GetAccountMovementsUseCase(accountRepository, transactionRepository).execute(userId);

        assertEquals(List.of("Recarga", "Retiro a cuenta externa", "Transferencia"),
                movements.stream().map(MovementResponse::typeLabel).toList());
        assertTrue(movements.get(0).isCredit());
        assertFalse(movements.get(2).isCredit());
        assertEquals("COMPLETED", movements.get(0).status());
    }

    @Test
    void shouldSuggestLastFiveDistinctPeopleIPaid() {
        givenMyAccount();
        List<CoreTransaction> history = new ArrayList<>();
        List<UUID> destinations = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            UUID destination = UUID.randomUUID();
            destinations.add(destination);
            history.add(tx(TransactionType.TRANSFER, accountId, destination, NOW.minus(Duration.ofHours(i)), "pago " + i));
            org.mockito.Mockito.lenient().when(accountRepository.findById(destination)).thenReturn(Optional.of(CoreAccount.builder()
                    .id(destination).userId(destination).accountNumber("10" + i).build()));
            org.mockito.Mockito.lenient().when(userRepository.findById(destination)).thenReturn(Optional.of(User.builder()
                    .firstName("Socio").lastName(String.valueOf(i)).build()));
        }
        history.add(tx(TransactionType.TRANSFER, accountId, destinations.get(0), NOW.minus(Duration.ofDays(9)), "repetido"));
        history.add(tx(TransactionType.TRANSFER, UUID.randomUUID(), accountId, NOW, "me pagaron"));
        when(transactionRepository.findByAccountId(accountId)).thenReturn(history);

        List<RecentRecipientResponse> recipients = new GetRecentRecipientsUseCase(accountRepository, transactionRepository, userRepository)
                .execute(userId);

        assertEquals(List.of("Socio 0", "Socio 1", "Socio 2", "Socio 3", "Socio 4"),
                recipients.stream().map(RecentRecipientResponse::ownerName).toList());
    }

    @Test
    void shouldReturnNoRecipientsForUsersWithoutAnAccount() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertTrue(new GetRecentRecipientsUseCase(accountRepository, transactionRepository, userRepository).execute(userId).isEmpty());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldBuildMonthlyCsvStatementEscapingConcepts() {
        givenMyAccount();
        when(transactionRepository.findByAccountId(accountId)).thenReturn(List.of(
                tx(TransactionType.DEPOSIT, null, accountId, Instant.parse("2026-09-30T23:59:59Z"), "mes anterior"),
                tx(TransactionType.TRANSFER, accountId, UUID.randomUUID(), Instant.parse("2026-10-03T10:15:00Z"), "Pago \"arriendo\", octubre"),
                tx(TransactionType.DEPOSIT, null, accountId, Instant.parse("2026-10-01T08:00:00Z"), "Recarga"),
                tx(TransactionType.DEPOSIT, null, accountId, Instant.parse("2026-11-01T00:00:00Z"), "mes siguiente")));

        AccountStatementResult statement = new GenerateAccountStatementUseCase(accountRepository, transactionRepository)
                .execute(userId, 2026, 10);

        assertEquals("extracto_1000012345_202610.csv", statement.fileName());
        assertEquals("""
                Fecha,Tipo,Descripcion,Monto,Direccion
                2026-10-01 08:00,DEPOSIT,Recarga,50000,CREDITO
                2026-10-03 10:15,TRANSFER,"Pago ""arriendo"", octubre",50000,DEBITO
                """, new String(statement.content(), StandardCharsets.UTF_8));
    }

    @Test
    void shouldNeutralizeFormulasThatASenderPutInTheConcept() {
        // El concepto lo escribe quien envía la transferencia; si empieza por = + - @, Excel lo
        // ejecutaría como fórmula al abrir el extracto de quien la recibe (CSV injection).
        givenMyAccount();
        when(transactionRepository.findByAccountId(accountId)).thenReturn(List.of(
                tx(TransactionType.TRANSFER, UUID.randomUUID(), accountId, Instant.parse("2026-10-05T10:00:00Z"),
                        "=HYPERLINK(\"http://evil.example\",\"Reclama tu premio\")"),
                tx(TransactionType.TRANSFER, UUID.randomUUID(), accountId, Instant.parse("2026-10-06T10:00:00Z"), "@SUM(A1:A9)"),
                tx(TransactionType.TRANSFER, UUID.randomUUID(), accountId, Instant.parse("2026-10-07T10:00:00Z"), "Pago normal")));

        String csv = new String(new GenerateAccountStatementUseCase(accountRepository, transactionRepository)
                .execute(userId, 2026, 10).content(), StandardCharsets.UTF_8);

        assertTrue(csv.contains(",\"'=HYPERLINK(\"\"http://evil.example\"\",\"\"Reclama tu premio\"\")\","), csv);
        assertTrue(csv.contains(",'@SUM(A1:A9),"), csv);
        assertTrue(csv.contains(",Pago normal,"), csv);
    }

    @Test
    void shouldShowMyAccountBalances() {
        givenMyAccount();

        CoreAccountResponse response = new GetMyAccountUseCase(accountRepository).execute(userId);

        assertEquals("1000012345", response.accountNumber());
        assertEquals(new BigDecimal("1200000"), response.principalBalance());
    }

    @Test
    void shouldFailQueriesWhenUserHasNoAccount() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> new GetMyAccountUseCase(accountRepository).execute(userId));
        assertThrows(ResourceNotFoundException.class,
                () -> new GetAccountMovementsUseCase(accountRepository, transactionRepository).execute(userId));
        assertThrows(ResourceNotFoundException.class,
                () -> new GenerateAccountStatementUseCase(accountRepository, transactionRepository).execute(userId, 2026, 10));
    }

    private void givenMyAccount() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(CoreAccount.builder()
                .id(accountId)
                .userId(userId)
                .accountNumber("1000012345")
                .principalBalance(new BigDecimal("1200000"))
                .interestBalance(new BigDecimal("35000"))
                .status(AccountStatus.ACTIVE)
                .build()));
    }

    private List<CoreTransaction> manyTransactions(int count) {
        List<CoreTransaction> list = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            list.add(tx(TransactionType.DEPOSIT, null, accountId, NOW.minus(Duration.ofHours(i)), "Recarga " + i));
        }
        return list;
    }

    private CoreTransaction tx(TransactionType type, UUID source, UUID destination, Instant createdAt, String concept) {
        return CoreTransaction.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(source)
                .destinationAccountId(destination)
                .amount(new BigDecimal("50000"))
                .concept(concept)
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .createdAt(createdAt)
                .build();
    }

    private ScheduledSavingsAccount savings(ScheduledSavingsStatus status, String balance) {
        return ScheduledSavingsAccount.builder()
                .id(UUID.randomUUID()).userId(userId).status(status)
                .currentBalance(balance == null ? null : new BigDecimal(balance)).build();
    }

    private MicroInvestmentPortfolio portfolio(InvestmentStatus status, String total) {
        return MicroInvestmentPortfolio.builder().id(UUID.randomUUID()).userId(userId).estado(status).montoTotal(new BigDecimal(total)).build();
    }

    private PersonalLoanApplication loan(LoanApplicationStatus status) {
        return PersonalLoanApplication.builder().id(UUID.randomUUID()).userId(userId).status(status).build();
    }
}
