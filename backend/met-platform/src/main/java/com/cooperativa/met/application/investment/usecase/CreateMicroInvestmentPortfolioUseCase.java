package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.dto.CreatePortfolioRequest;
import com.cooperativa.met.application.investment.dto.MicroInvestmentResponse;
import com.cooperativa.met.application.investment.dto.PortfolioResponse;
import com.cooperativa.met.domain.admin.model.FeeSchedule;
import com.cooperativa.met.domain.admin.model.PlatformRevenue;
import com.cooperativa.met.domain.admin.port.FeeScheduleRepositoryPort;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.domain.investment.model.*;
import com.cooperativa.met.domain.investment.port.*;
import com.cooperativa.met.domain.investment.service.DistributionStrategyService;
import com.cooperativa.met.domain.savings.port.DebitSourcePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: Crear un portfolio de micro-inversiones.
 *
 * Flujo:
 *  1. Valida instrumentos disponibles.
 *  2. Calcula distribución según estrategia.
 *  3. Cobra comisión INVESTMENT_FEE.
 *  4. Débita el monto total del saldo del usuario.
 *  5. Persiste portfolio + posiciones individuales.
 *  6. Registra ingresos de la plataforma.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateMicroInvestmentPortfolioUseCase {

    private final InvestmentInstrumentPort instrumentPort;
    private final MicroInvestmentPortfolioPort portfolioPort;
    private final MicroInvestmentPort investmentPort;
    private final DebitSourcePort debitSourcePort;
    private final FeeScheduleRepositoryPort feeRepositoryPort;
    private final PlatformRevenuePort platformRevenuePort;

    @Transactional
    public PortfolioResponse execute(UUID userId, CreatePortfolioRequest request) {

        // 1. Resolver estrategia (EQUAL por defecto)
        InvestmentStrategy estrategia = resolveStrategy(request.estrategia());

        // 2. Obtener instrumentos activos y calcular distribución
        List<InvestmentInstrument> instrumentos = instrumentPort.findActivos();
        Map<InvestmentInstrument, BigDecimal> distribucion =
                DistributionStrategyService.distribuir(request.montoTotal(), instrumentos, estrategia);

        // 3. Calcular comisión de inversión
        BigDecimal feeAmount = calcularComision(request.montoTotal());
        BigDecimal totalCharge = request.montoTotal().add(feeAmount);

        // 4. Débitar monto total + comisión del saldo del usuario
        String reference = "MICRO-INV-" + userId + "-" + System.currentTimeMillis();
        boolean debited = debitSourcePort.debit(userId, totalCharge, reference);
        if (!debited) {
            throw new IllegalStateException(
                    "Saldo insuficiente para invertir. Se requieren: " + totalCharge);
        }

        // 5. Persistir portfolio
        MicroInvestmentPortfolio portfolio = portfolioPort.save(
                MicroInvestmentPortfolio.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .montoTotal(request.montoTotal())
                        .estrategia(estrategia)
                        .estado(InvestmentStatus.ACTIVE)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());

        // 6. Persistir posiciones individuales
        LocalDate hoy = LocalDate.now();
        List<MicroInvestment> posiciones = new ArrayList<>();

        for (Map.Entry<InvestmentInstrument, BigDecimal> entry : distribucion.entrySet()) {
            InvestmentInstrument inst = entry.getKey();
            BigDecimal monto = entry.getValue();

            MicroInvestment inv = investmentPort.save(
                    MicroInvestment.builder()
                            .id(UUID.randomUUID())
                            .portfolioId(portfolio.getId())
                            .instrumentId(inst.getId())
                            .userId(userId)
                            .montoInvertido(monto)
                            .tasaAplicada(inst.getTasaAnual())
                            .plazoDias(inst.getPlazoDias())
                            .fechaInicio(hoy)
                            .fechaVencimiento(hoy.plusDays(inst.getPlazoDias()))
                            .rendimientoGanado(BigDecimal.ZERO)
                            .estado(InvestmentStatus.ACTIVE)
                            .createdAt(Instant.now())
                            .build());
            posiciones.add(inv);
        }

        // 7. Registrar ingresos de la plataforma si hubo comisión
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            platformRevenuePort.save(PlatformRevenue.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .amount(feeAmount)
                    .description("Comisión por creación de portfolio de micro-inversión")
                    .source("MICRO_INVESTMENT_FEE")
                    .createdAt(Instant.now())
                    .build());
            log.info("Comisión de inversión {} cobrada al usuario {}", feeAmount, userId);
        }

        log.info("Portfolio {} creado para usuario {} con {} posiciones. Total: {}",
                portfolio.getId(), userId, posiciones.size(), request.montoTotal());

        // 8. Construir respuesta
        List<MicroInvestmentResponse> posicionesResponse = posiciones.stream()
                .map(inv -> {
                    InvestmentInstrument inst = distribucion.keySet().stream()
                            .filter(i -> i.getId().equals(inv.getInstrumentId()))
                            .findFirst().orElseThrow();
                    return MicroInvestmentResponse.from(inv, inst.getNombre());
                })
                .toList();

        return PortfolioResponse.from(portfolio, posicionesResponse);
    }

    private InvestmentStrategy resolveStrategy(String estrategiaStr) {
        if (estrategiaStr == null || estrategiaStr.isBlank()) {
            return InvestmentStrategy.EQUAL;
        }
        try {
            return InvestmentStrategy.valueOf(estrategiaStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estrategia inválida: " + estrategiaStr
                    + ". Las opciones son: EQUAL, WEIGHTED, RISK_BASED");
        }
    }

    private BigDecimal calcularComision(BigDecimal monto) {
        Optional<FeeSchedule> feeOpt = feeRepositoryPort.findVigentes().stream()
                .filter(f -> "INVESTMENT_FEE".equals(f.getTipoTarifa()))
                .findFirst();

        if (feeOpt.isEmpty()) return BigDecimal.ZERO;

        FeeSchedule fee = feeOpt.get();
        if (fee.isEsPorcentaje()) {
            return monto.multiply(fee.getValor()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return fee.getValor();
    }
}
