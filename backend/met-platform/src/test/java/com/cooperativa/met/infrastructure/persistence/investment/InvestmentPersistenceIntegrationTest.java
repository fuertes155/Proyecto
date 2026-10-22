package com.cooperativa.met.infrastructure.persistence.investment;

import com.cooperativa.met.domain.investment.model.GuaranteeFundMovement;
import com.cooperativa.met.domain.investment.model.GuaranteeFundMovementType;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.InvestmentStrategy;
import com.cooperativa.met.domain.investment.model.LedgerEntry;
import com.cooperativa.met.domain.investment.model.LedgerEntryCategory;
import com.cooperativa.met.domain.investment.model.LedgerEntryType;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;
import com.cooperativa.met.domain.investment.port.GuaranteeFundPort;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentReturnPort;
import com.cooperativa.met.domain.investment.port.LedgerRepositoryPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPortfolioPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adaptadores de persistencia del motor de inversión contra H2. */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
class InvestmentPersistenceIntegrationTest {

    @Autowired private InvestmentFractionRepositoryPort fractionPort;
    @Autowired private InvestmentMatchRepositoryPort matchPort;
    @Autowired private LedgerRepositoryPort ledgerPort;
    @Autowired private GuaranteeFundPort guaranteeFundPort;
    @Autowired private InvestmentInstrumentPort instrumentPort;
    @Autowired private MicroInvestmentPortfolioPort portfolioPort;
    @Autowired private MicroInvestmentPort investmentPort;
    @Autowired private InvestmentReturnPort returnPort;

    @Test
    void shouldTrackFractionsAndTheLoansTheyFund() {
        UUID investorAccount = UUID.randomUUID();
        UUID depositId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        List<InvestmentFraction> saved = fractionPort.saveAll(List.of(
                fraction(investorAccount, depositId, "10000.00"), fraction(investorAccount, depositId, "5000.00")));
        InvestmentFraction matched = fractionPort.save(saved.get(0).withStatus(InvestmentFractionStatus.MATCHED));
        InvestmentMatch match = matchPort.save(InvestmentMatch.builder()
                .id(UUID.randomUUID()).fractionId(matched.getId()).borrowerLoanId(loanId).matchedAt(Instant.now()).build());
        matchPort.saveAll(List.of(InvestmentMatch.builder()
                .id(UUID.randomUUID()).fractionId(saved.get(1).getId()).borrowerLoanId(UUID.randomUUID()).matchedAt(Instant.now()).build()));

        assertEquals(2, fractionPort.findByOriginalDepositId(depositId).size());
        assertEquals(2, fractionPort.findByInvestorAccountId(investorAccount).size());
        assertEquals(InvestmentFractionStatus.MATCHED, fractionPort.findById(matched.getId()).orElseThrow().getStatus());
        assertTrue(fractionPort.findByStatus(InvestmentFractionStatus.AVAILABLE).stream().anyMatch(f -> f.getId().equals(saved.get(1).getId())));
        assertEquals(List.of(match.getId()), matchPort.findByBorrowerLoanId(loanId).stream().map(InvestmentMatch::getId).toList());
        assertEquals(loanId, matchPort.findByFractionId(matched.getId()).get(0).getBorrowerLoanId());
        assertEquals(loanId, matchPort.findById(match.getId()).orElseThrow().getBorrowerLoanId());
    }

    @Test
    void shouldKeepTheLedgerPerAccountAndPerTransaction() {
        UUID accountId = UUID.randomUUID();
        UUID reference = UUID.randomUUID();
        ledgerPort.save(ledger(accountId, reference, LedgerEntryType.DEBIT, "10000.00"));
        ledgerPort.saveAll(List.of(ledger(accountId, reference, LedgerEntryType.CREDIT, "10200.00"),
                ledger(UUID.randomUUID(), reference, LedgerEntryType.CREDIT, "50.00")));

        assertEquals(2, ledgerPort.findByAccountId(accountId).size());
        assertEquals(3, ledgerPort.findByTransactionReference(reference).size());
    }

    @Test
    void shouldComputeTheGuaranteeFundBalanceFromItsMovements() {
        BigDecimal before = guaranteeFundPort.getBalance();
        UUID reference = UUID.randomUUID();
        guaranteeFundPort.save(movement(GuaranteeFundMovementType.CONTRIBUTION, "1000.00", reference));
        guaranteeFundPort.save(movement(GuaranteeFundMovementType.PAYOUT, "300.00", reference));

        assertEquals(0, before.add(new BigDecimal("700.00")).compareTo(guaranteeFundPort.getBalance()));
        assertEquals(2, guaranteeFundPort.findByTransactionReference(reference).size());
        assertFalse(guaranteeFundPort.findAllOrderByCreatedAtDesc().isEmpty());
    }

    @Test
    void shouldManageInstrumentsPortfoliosPositionsAndReturns() {
        InvestmentInstrument instrument = instrumentPort.save(InvestmentInstrument.builder()
                .id(UUID.randomUUID()).nombre("CDT prueba").descripcion("90 días").tasaAnual(new BigDecimal("0.0950"))
                .plazoDias(90).montoMinimo(new BigDecimal("50000.00")).cupoMaximo(new BigDecimal("100000000.00"))
                .activo(true).creadoPor(UUID.randomUUID()).createdAt(Instant.now()).updatedAt(Instant.now()).build());
        InvestmentInstrument paused = instrumentPort.save(instrument.toBuilder().id(UUID.randomUUID()).nombre("Pausado").activo(false).build());
        assertTrue(instrumentPort.findActivos().stream().anyMatch(i -> i.getId().equals(instrument.getId())));
        assertTrue(instrumentPort.findActivos().stream().noneMatch(i -> i.getId().equals(paused.getId())));
        assertTrue(instrumentPort.findAll().stream().anyMatch(i -> i.getId().equals(paused.getId())));
        instrumentPort.deleteById(paused.getId());
        assertTrue(instrumentPort.findById(paused.getId()).isEmpty());

        UUID userId = UUID.randomUUID();
        MicroInvestmentPortfolio portfolio = portfolioPort.save(MicroInvestmentPortfolio.builder()
                .id(UUID.randomUUID()).userId(userId).montoTotal(new BigDecimal("200000.00")).estrategia(InvestmentStrategy.EQUAL)
                .estado(InvestmentStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now()).build());
        assertEquals(InvestmentStrategy.EQUAL, portfolioPort.findById(portfolio.getId()).orElseThrow().getEstrategia());
        assertEquals(1, portfolioPort.findByUserId(userId).size());

        LocalDate today = LocalDate.now();
        MicroInvestment due = investmentPort.save(position(portfolio, instrument, userId, today.minusDays(1)));
        MicroInvestment notYet = investmentPort.save(position(portfolio, instrument, userId, today.plusDays(30)));
        List<UUID> maturing = investmentPort.findMaturingOnOrBefore(today, InvestmentStatus.ACTIVE).stream().map(MicroInvestment::getId).toList();
        assertTrue(maturing.contains(due.getId()));
        assertFalse(maturing.contains(notYet.getId()));
        assertEquals(2, investmentPort.findByPortfolioId(portfolio.getId()).size());
        assertEquals(2, investmentPort.findByUserId(userId).size());
        assertTrue(investmentPort.findByStatus(InvestmentStatus.ACTIVE).stream().anyMatch(i -> i.getId().equals(due.getId())));
        assertEquals(0, new BigDecimal("0.0950").compareTo(investmentPort.findById(due.getId()).orElseThrow().getTasaAplicada()));

        returnPort.save(InvestmentReturn.builder()
                .id(UUID.randomUUID()).investmentId(due.getId()).userId(userId).capital(new BigDecimal("100000.00"))
                .rendimiento(new BigDecimal("2342.47")).totalAcreditado(new BigDecimal("102342.47"))
                .fechaPago(today).createdAt(Instant.now()).build());
        assertEquals(0, new BigDecimal("2342.47").compareTo(returnPort.findByUserId(userId).get(0).getRendimiento()));
    }

    private InvestmentFraction fraction(UUID investorAccount, UUID depositId, String amount) {
        return InvestmentFraction.builder()
                .id(UUID.randomUUID()).investorAccountId(investorAccount).originalDepositId(depositId)
                .amount(new BigDecimal(amount)).status(InvestmentFractionStatus.AVAILABLE).createdAt(Instant.now()).build();
    }

    private LedgerEntry ledger(UUID accountId, UUID reference, LedgerEntryType type, String amount) {
        return LedgerEntry.builder()
                .id(UUID.randomUUID()).accountId(accountId).transactionReference(reference).entryType(type)
                .category(LedgerEntryCategory.FUNDING_DISBURSEMENT).amount(new BigDecimal(amount))
                .concept("prueba").createdAt(Instant.now()).build();
    }

    private GuaranteeFundMovement movement(GuaranteeFundMovementType type, String amount, UUID reference) {
        return GuaranteeFundMovement.builder()
                .id(UUID.randomUUID()).type(type).amount(new BigDecimal(amount)).transactionReference(reference)
                .concept("prueba").createdAt(Instant.now()).build();
    }

    private MicroInvestment position(MicroInvestmentPortfolio portfolio, InvestmentInstrument instrument, UUID userId, LocalDate vencimiento) {
        return MicroInvestment.builder()
                .id(UUID.randomUUID()).portfolioId(portfolio.getId()).instrumentId(instrument.getId()).userId(userId)
                .montoInvertido(new BigDecimal("100000.00")).tasaAplicada(new BigDecimal("0.0950")).plazoDias(90)
                .fechaInicio(vencimiento.minusDays(90)).fechaVencimiento(vencimiento).rendimientoGanado(BigDecimal.ZERO)
                .estado(InvestmentStatus.ACTIVE).createdAt(Instant.now()).build();
    }
}
