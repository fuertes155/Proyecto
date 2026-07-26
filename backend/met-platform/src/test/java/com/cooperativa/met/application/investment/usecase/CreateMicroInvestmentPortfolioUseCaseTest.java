package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPortfolioPort;
import com.cooperativa.met.domain.savings.port.DebitSourcePort;
import com.cooperativa.met.application.investment.dto.CreatePortfolioRequest;
import com.cooperativa.met.application.investment.dto.PortfolioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateMicroInvestmentPortfolioUseCaseTest {

    @Mock private InvestmentInstrumentPort instrumentPort;
    @Mock private MicroInvestmentPortfolioPort portfolioPort;
    @Mock private MicroInvestmentPort investmentPort;
    @Mock private DebitSourcePort debitSourcePort;
    @Mock private FeeScheduleRepositoryPort feeRepositoryPort;
    @Mock private PlatformRevenuePort platformRevenuePort;

    @InjectMocks
    private CreateMicroInvestmentPortfolioUseCase useCase;

    private UUID userId;
    private InvestmentInstrument activeInstrument;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeInstrument = InvestmentInstrument.builder()
                .id(UUID.randomUUID())
                .nombre("Fondo A")
                .tasaAnual(new BigDecimal("0.08"))
                .plazoDias(90)
                .activo(true)
                .montoMinimo(BigDecimal.ZERO)
                .build();
    }

    @Test
    void execute_createsPortfolioSuccessfully_withNoFees() {
        // Arrange
        CreatePortfolioRequest request = new CreatePortfolioRequest(
                new BigDecimal("1000000.00"), "EQUAL"
        );
        MicroInvestmentPortfolio savedPortfolio = MicroInvestmentPortfolio.builder()
                .id(UUID.randomUUID()).userId(userId)
                .montoTotal(request.montoTotal())
                .estado(InvestmentStatus.ACTIVE)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        MicroInvestment savedInvestment = MicroInvestment.builder()
                .id(UUID.randomUUID()).portfolioId(savedPortfolio.getId())
                .instrumentId(activeInstrument.getId())
                .userId(userId).montoInvertido(request.montoTotal())
                .tasaAplicada(activeInstrument.getTasaAnual())
                .plazoDias(90)
                .rendimientoGanado(BigDecimal.ZERO)
                .estado(InvestmentStatus.ACTIVE).createdAt(Instant.now())
                .build();

        when(instrumentPort.findActivos()).thenReturn(List.of(activeInstrument));
        when(feeRepositoryPort.findVigentes()).thenReturn(List.of()); // Sin comisiones
        when(debitSourcePort.debit(eq(userId), any(), any())).thenReturn(true);
        when(portfolioPort.save(any())).thenReturn(savedPortfolio);
        when(investmentPort.save(any())).thenReturn(savedInvestment);

        // Act
        PortfolioResponse result = useCase.execute(userId, request);

        // Assert
        assertNotNull(result);
        verify(portfolioPort).save(any(MicroInvestmentPortfolio.class));
        verify(investmentPort, atLeastOnce()).save(any(MicroInvestment.class));
        // Sin comisión → no se guarda revenue
        verifyNoInteractions(platformRevenuePort);
    }

    @Test
    void execute_createsPortfolioAndChargesFee_whenFeeScheduleExists() {
        // Arrange: Comisión del 1%
        CreatePortfolioRequest request = new CreatePortfolioRequest(
                new BigDecimal("1000000.00"), "EQUAL"
        );
        FeeSchedule fee = FeeSchedule.builder()
                .tipoTarifa("INVESTMENT_FEE").valor(new BigDecimal("1")).esPorcentaje(true).build();
        MicroInvestmentPortfolio savedPortfolio = MicroInvestmentPortfolio.builder()
                .id(UUID.randomUUID()).userId(userId).montoTotal(request.montoTotal())
                .estado(InvestmentStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        MicroInvestment savedInvestment = MicroInvestment.builder()
                .id(UUID.randomUUID()).rendimientoGanado(BigDecimal.ZERO)
                .estado(InvestmentStatus.ACTIVE).createdAt(Instant.now()).build();

        when(instrumentPort.findActivos()).thenReturn(List.of(activeInstrument));
        when(feeRepositoryPort.findVigentes()).thenReturn(List.of(fee));
        // Débito = 1,000,000 + 10,000 (1%) = 1,010,000
        when(debitSourcePort.debit(eq(userId), eq(new BigDecimal("1010000.00")), any())).thenReturn(true);
        when(portfolioPort.save(any())).thenReturn(savedPortfolio);
        when(investmentPort.save(any())).thenReturn(savedInvestment);

        // Act
        useCase.execute(userId, request);

        // Assert: se registra el ingreso de la comisión
        verify(platformRevenuePort).save(any());
    }

    @Test
    void execute_throwsIllegalState_whenInsufficientBalance() {
        CreatePortfolioRequest request = new CreatePortfolioRequest(
                new BigDecimal("1000000.00"), "EQUAL"
        );
        when(instrumentPort.findActivos()).thenReturn(List.of(activeInstrument));
        when(feeRepositoryPort.findVigentes()).thenReturn(List.of());
        when(debitSourcePort.debit(any(), any(), any())).thenReturn(false); // Saldo insuficiente

        assertThrows(IllegalStateException.class, () -> useCase.execute(userId, request));
        verify(portfolioPort, never()).save(any());
    }

    @Test
    void execute_throwsIllegalArgument_whenInvalidStrategy() {
        CreatePortfolioRequest request = new CreatePortfolioRequest(
                new BigDecimal("1000000.00"), "INVALID_STRATEGY"
        );

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(userId, request));
    }

    @Test
    void execute_usesEqualStrategyByDefault_whenStrategyIsNull() {
        CreatePortfolioRequest request = new CreatePortfolioRequest(
                new BigDecimal("1000000.00"), null // null → EQUAL por defecto
        );
        MicroInvestmentPortfolio savedPortfolio = MicroInvestmentPortfolio.builder()
                .id(UUID.randomUUID()).montoTotal(request.montoTotal())
                .estado(InvestmentStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        MicroInvestment savedInv = MicroInvestment.builder()
                .id(UUID.randomUUID()).rendimientoGanado(BigDecimal.ZERO)
                .estado(InvestmentStatus.ACTIVE).createdAt(Instant.now()).build();

        when(instrumentPort.findActivos()).thenReturn(List.of(activeInstrument));
        when(feeRepositoryPort.findVigentes()).thenReturn(List.of());
        when(debitSourcePort.debit(any(), any(), any())).thenReturn(true);
        when(portfolioPort.save(any())).thenReturn(savedPortfolio);
        when(investmentPort.save(any())).thenReturn(savedInv);

        // No debe lanzar excepción
        assertDoesNotThrow(() -> useCase.execute(userId, request));
    }
}
