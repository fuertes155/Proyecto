package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.dto.PortfolioResponse;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.InvestmentStrategy;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;
import com.cooperativa.met.domain.investment.port.InvestmentCreditPort;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;
import com.cooperativa.met.domain.investment.port.InvestmentReturnPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPortfolioPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Vida de una micro-inversión: rendimiento diario, vencimiento, cancelación y consulta. */
@ExtendWith(MockitoExtension.class)
class MicroInvestmentLifecycleUseCasesTest {

    @Mock private MicroInvestmentPort investmentPort;
    @Mock private MicroInvestmentPortfolioPort portfolioPort;
    @Mock private InvestmentReturnPort returnPort;
    @Mock private InvestmentCreditPort creditPort;
    @Mock private InvestmentInstrumentPort instrumentPort;
    @Mock private CoreAccountRepositoryPort accountPort;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    @Test
    void shouldPayCapitalPlusYieldAtMaturityAndCloseThePosition() {
        ProcessMaturingInvestmentsUseCase maturity = new ProcessMaturingInvestmentsUseCase(investmentPort, returnPort, creditPort);
        MicroInvestment due = position("1000000", "0.0730", 100, InvestmentStatus.ACTIVE);
        when(investmentPort.findMaturingOnOrBefore(any(), eq(InvestmentStatus.ACTIVE))).thenReturn(List.of(due));
        when(creditPort.credit(userId, new BigDecimal("1020000.00"), "INVEST-RETURN-" + due.getId())).thenReturn(true);

        assertEquals(1, maturity.execute());

        ArgumentCaptor<InvestmentReturn> paid = ArgumentCaptor.forClass(InvestmentReturn.class);
        verify(returnPort).save(paid.capture());
        assertEquals(new BigDecimal("20000.00"), paid.getValue().getRendimiento());
        ArgumentCaptor<MicroInvestment> closed = ArgumentCaptor.forClass(MicroInvestment.class);
        verify(investmentPort).save(closed.capture());
        assertEquals(InvestmentStatus.COMPLETED, closed.getValue().getEstado());
    }

    @Test
    void shouldKeepPositionOpenWhenTheMoneyCouldNotBeCreditedAndContinueWithTheRest() {
        ProcessMaturingInvestmentsUseCase maturity = new ProcessMaturingInvestmentsUseCase(investmentPort, returnPort, creditPort);
        MicroInvestment failsCredit = position("1000000", "0.0730", 100, InvestmentStatus.ACTIVE);
        MicroInvestment blowsUp = position("500000", "0.0730", 100, InvestmentStatus.ACTIVE);
        MicroInvestment ok = position("200000", "0.0730", 100, InvestmentStatus.ACTIVE);
        when(investmentPort.findMaturingOnOrBefore(any(), eq(InvestmentStatus.ACTIVE))).thenReturn(List.of(failsCredit, blowsUp, ok));
        when(creditPort.credit(eq(userId), any(), eq("INVEST-RETURN-" + failsCredit.getId()))).thenReturn(false);
        when(creditPort.credit(eq(userId), any(), eq("INVEST-RETURN-" + blowsUp.getId()))).thenThrow(new IllegalStateException("BD"));
        when(creditPort.credit(eq(userId), any(), eq("INVEST-RETURN-" + ok.getId()))).thenReturn(true);

        int processed = maturity.execute();

        // La que no se pudo acreditar cuenta como "procesada" (no lanzó), pero no se cierra ni se registra pago.
        assertEquals(2, processed);
        verify(returnPort, times(1)).save(any());
        verify(investmentPort, times(1)).save(any());
    }

    @Test
    void shouldAccrueDailyYieldToThePositionAndToTheWithdrawableEarnings() {
        CalculateDailyRoyaltiesUseCase royalties = new CalculateDailyRoyaltiesUseCase(investmentPort, accountPort);
        MicroInvestment active = position("3650000", "0.10", 90, InvestmentStatus.ACTIVE).withRendimientoGanado(new BigDecimal("2000"));
        when(investmentPort.findByStatus(InvestmentStatus.ACTIVE)).thenReturn(List.of(active));
        when(accountPort.findByUserId(userId)).thenReturn(Optional.of(CoreAccount.builder()
                .id(UUID.randomUUID()).userId(userId).principalBalance(BigDecimal.ZERO)
                .interestBalance(new BigDecimal("500.00")).status(AccountStatus.ACTIVE).build()));

        assertEquals(1, royalties.execute());

        ArgumentCaptor<MicroInvestment> position = ArgumentCaptor.forClass(MicroInvestment.class);
        verify(investmentPort).save(position.capture());
        assertEquals(new BigDecimal("3000.0000"), position.getValue().getRendimientoGanado());
        ArgumentCaptor<CoreAccount> account = ArgumentCaptor.forClass(CoreAccount.class);
        verify(accountPort).save(account.capture());
        assertEquals(new BigDecimal("1500.0000"), account.getValue().getInterestBalance());
    }

    @Test
    void shouldReturnOnlyTheCapitalOfActivePositionsWhenCancelling() {
        CancelMicroInvestmentUseCase cancel = new CancelMicroInvestmentUseCase(portfolioPort, investmentPort, creditPort);
        MicroInvestment active = position("400000", "0.08", 90, InvestmentStatus.ACTIVE);
        MicroInvestment completed = position("100000", "0.08", 90, InvestmentStatus.COMPLETED);
        when(portfolioPort.findById(portfolioId)).thenReturn(Optional.of(portfolio(userId, InvestmentStatus.ACTIVE)));
        when(investmentPort.findByPortfolioId(portfolioId)).thenReturn(List.of(active, completed));
        when(creditPort.credit(userId, new BigDecimal("400000"), "INVEST-CANCEL-" + active.getId())).thenReturn(true);

        cancel.execute(userId, portfolioId);

        ArgumentCaptor<MicroInvestment> cancelled = ArgumentCaptor.forClass(MicroInvestment.class);
        verify(investmentPort).save(cancelled.capture());
        assertEquals(InvestmentStatus.CANCELLED, cancelled.getValue().getEstado());
        ArgumentCaptor<MicroInvestmentPortfolio> savedPortfolio = ArgumentCaptor.forClass(MicroInvestmentPortfolio.class);
        verify(portfolioPort).save(savedPortfolio.capture());
        assertEquals(InvestmentStatus.CANCELLED, savedPortfolio.getValue().getEstado());
    }

    @Test
    void shouldAbortCancellationIfAnyCapitalCannotBeReturned() {
        CancelMicroInvestmentUseCase cancel = new CancelMicroInvestmentUseCase(portfolioPort, investmentPort, creditPort);
        when(portfolioPort.findById(portfolioId)).thenReturn(Optional.of(portfolio(userId, InvestmentStatus.ACTIVE)));
        when(investmentPort.findByPortfolioId(portfolioId)).thenReturn(List.of(position("400000", "0.08", 90, InvestmentStatus.ACTIVE)));
        when(creditPort.credit(eq(userId), any(), anyString())).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> cancel.execute(userId, portfolioId));

        verify(portfolioPort, never()).save(any());
    }

    @Test
    void shouldOnlyLetTheOwnerCancelAnActivePortfolio() {
        CancelMicroInvestmentUseCase cancel = new CancelMicroInvestmentUseCase(portfolioPort, investmentPort, creditPort);
        UUID otherPortfolio = UUID.randomUUID();
        when(portfolioPort.findById(portfolioId)).thenReturn(Optional.of(portfolio(UUID.randomUUID(), InvestmentStatus.ACTIVE)));
        when(portfolioPort.findById(otherPortfolio)).thenReturn(Optional.of(portfolio(userId, InvestmentStatus.COMPLETED)));

        assertEquals("UNAUTHORIZED", assertThrows(BusinessRuleException.class, () -> cancel.execute(userId, portfolioId)).getCode());
        assertEquals("INVALID_STATE", assertThrows(BusinessRuleException.class, () -> cancel.execute(userId, otherPortfolio)).getCode());
        verifyNoInteractions(creditPort);
    }

    @Test
    void shouldShowPortfolioWithProjectedYieldOnlyToItsOwner() {
        GetInvestmentPortfolioUseCase query = new GetInvestmentPortfolioUseCase(portfolioPort, investmentPort, instrumentPort);
        MicroInvestment withInstrument = position("1000000", "0.0730", 100, InvestmentStatus.ACTIVE);
        MicroInvestment orphan = position("1000000", "0.0730", 100, InvestmentStatus.ACTIVE).toBuilder().instrumentId(UUID.randomUUID()).build();
        when(portfolioPort.findById(portfolioId)).thenReturn(Optional.of(portfolio(userId, InvestmentStatus.ACTIVE)));
        when(investmentPort.findByPortfolioId(portfolioId)).thenReturn(List.of(withInstrument, orphan));
        when(instrumentPort.findById(withInstrument.getInstrumentId()))
                .thenReturn(Optional.of(InvestmentInstrument.builder().id(withInstrument.getInstrumentId()).nombre("CDT 100 días").build()));
        when(instrumentPort.findById(orphan.getInstrumentId())).thenReturn(Optional.empty());

        PortfolioResponse response = query.getById(userId, portfolioId);

        assertEquals(new BigDecimal("40000.00"), response.rendimientoTotalProyectado());
        assertEquals("CDT 100 días", response.posiciones().get(0).instrumentNombre());
        assertEquals("Instrumento no encontrado", response.posiciones().get(1).instrumentNombre());
        assertEquals("UNAUTHORIZED", assertThrows(BusinessRuleException.class,
                () -> query.getById(UUID.randomUUID(), portfolioId)).getCode());
    }

    @Test
    void shouldListAllPortfoliosOfTheUser() {
        GetInvestmentPortfolioUseCase query = new GetInvestmentPortfolioUseCase(portfolioPort, investmentPort, instrumentPort);
        when(portfolioPort.findByUserId(userId)).thenReturn(List.of(portfolio(userId, InvestmentStatus.ACTIVE), portfolio(userId, InvestmentStatus.COMPLETED)));
        when(investmentPort.findByPortfolioId(any())).thenReturn(List.of());

        assertEquals(List.of("ACTIVE", "COMPLETED"), query.listByUser(userId).stream().map(PortfolioResponse::estado).toList());
    }

    private MicroInvestment position(String monto, String tasa, int plazoDias, InvestmentStatus estado) {
        return MicroInvestment.builder()
                .id(UUID.randomUUID())
                .portfolioId(portfolioId)
                .instrumentId(UUID.randomUUID())
                .userId(userId)
                .montoInvertido(new BigDecimal(monto))
                .tasaAplicada(new BigDecimal(tasa))
                .plazoDias(plazoDias)
                .fechaInicio(LocalDate.now().minusDays(plazoDias))
                .fechaVencimiento(LocalDate.now())
                .rendimientoGanado(BigDecimal.ZERO)
                .estado(estado)
                .createdAt(Instant.now())
                .build();
    }

    private MicroInvestmentPortfolio portfolio(UUID owner, InvestmentStatus estado) {
        return MicroInvestmentPortfolio.builder()
                .id(portfolioId)
                .userId(owner)
                .montoTotal(new BigDecimal("2000000"))
                .estrategia(InvestmentStrategy.EQUAL)
                .estado(estado)
                .createdAt(Instant.now())
                .build();
    }
}
