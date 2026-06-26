package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.admin.model.PlatformRevenue;
import com.cooperativa.met.domain.admin.port.PlatformRevenuePort;
import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.port.InvestmentCreditPort;
import com.cooperativa.met.domain.investment.port.InvestmentReturnPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: Procesar posiciones de inversión que han vencido hoy.
 *
 * Ejecutado por el scheduler diario {@code InvestmentMaturationScheduler}.
 * Por cada posición vencida:
 *  1. Calcula rendimiento: capital × tasa × días / 365.
 *  2. Acredita capital + rendimiento al saldo del usuario.
 *  3. Registra el InvestmentReturn para historial.
 *  4. Marca la posición como COMPLETED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessMaturingInvestmentsUseCase {

    private final MicroInvestmentPort investmentPort;
    private final InvestmentReturnPort returnPort;
    private final InvestmentCreditPort creditPort;
    private final PlatformRevenuePort platformRevenuePort;

    @Transactional
    public int execute() {
        LocalDate hoy = LocalDate.now();
        List<MicroInvestment> vencidas = investmentPort.findMaturingOnOrBefore(hoy, InvestmentStatus.ACTIVE);

        int procesadas = 0;
        for (MicroInvestment inv : vencidas) {
            try {
                procesarVencimiento(inv, hoy);
                procesadas++;
            } catch (Exception e) {
                log.error("Error procesando vencimiento de inversión {}: {}", inv.getId(), e.getMessage(), e);
            }
        }

        if (procesadas > 0) {
            log.info("Procesadas {} inversiones vencidas", procesadas);
        }
        return procesadas;
    }

    private void procesarVencimiento(MicroInvestment inv, LocalDate hoy) {
        BigDecimal rendimiento = inv.calcularRendimiento();
        BigDecimal totalAcreditado = inv.calcularTotalAlVencer();

        // Acreditar capital + rendimiento al usuario
        String reference = "INVEST-RETURN-" + inv.getId();
        boolean credited = creditPort.credit(inv.getUserId(), totalAcreditado, reference);

        if (!credited) {
            log.error("No se pudo acreditar {} al usuario {} para inversión {}",
                    totalAcreditado, inv.getUserId(), inv.getId());
            return;
        }

        // Registrar el pago de rendimiento
        returnPort.save(InvestmentReturn.builder()
                .id(UUID.randomUUID())
                .investmentId(inv.getId())
                .userId(inv.getUserId())
                .capital(inv.getMontoInvertido())
                .rendimiento(rendimiento)
                .totalAcreditado(totalAcreditado)
                .fechaPago(hoy)
                .createdAt(Instant.now())
                .build());

        // Actualizar la posición como COMPLETED
        investmentPort.save(inv
                .withRendimientoGanado(rendimiento)
                .withEstado(InvestmentStatus.COMPLETED));

        log.info("Inversión {} vencida: capital={}, rendimiento={}, total={} acreditado al usuario {}",
                inv.getId(), inv.getMontoInvertido(), rendimiento, totalAcreditado, inv.getUserId());
    }
}
