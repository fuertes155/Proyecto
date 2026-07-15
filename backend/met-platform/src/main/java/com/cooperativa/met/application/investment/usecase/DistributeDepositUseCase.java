package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributeDepositUseCase {

    // Tamaño de fracción sugerido: 10,000 COP
    private static final BigDecimal MAX_FRACTION_SIZE = new BigDecimal("10000.00");

    private final InvestmentFractionRepositoryPort fractionRepositoryPort;
    private final MatchFractionsToDebtorsUseCase matchUseCase;

    @Transactional
    public void execute(UUID investorAccountId, UUID depositId, BigDecimal depositAmount) {
        log.info("Iniciando fraccionamiento para depósito {} de cuenta {}, monto: {}", depositId, investorAccountId, depositAmount);

        // Idempotencia: Verificar que no haya sido fraccionado antes
        if (!fractionRepositoryPort.findByOriginalDepositId(depositId).isEmpty()) {
            log.info("El depósito {} ya fue fraccionado. Saltando proceso.", depositId);
            return;
        }

        List<InvestmentFraction> fractions = new ArrayList<>();
        BigDecimal remainingAmount = depositAmount;

        // Dividir en fracciones de MAX_FRACTION_SIZE
        while (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fractionAmount = remainingAmount.min(MAX_FRACTION_SIZE);
            remainingAmount = remainingAmount.subtract(fractionAmount);

            InvestmentFraction fraction = InvestmentFraction.builder()
                    .id(UUID.randomUUID())
                    .investorAccountId(investorAccountId)
                    .originalDepositId(depositId)
                    .amount(fractionAmount)
                    .status(InvestmentFractionStatus.AVAILABLE)
                    .createdAt(Instant.now())
                    .build();

            fractions.add(fraction);
        }

        fractionRepositoryPort.saveAll(fractions);
        log.info("Depósito {} fraccionado en {} partes.", depositId, fractions.size());

        // Llamar al motor de emparejamiento para asignar las fracciones disponibles
        matchUseCase.execute(fractions);
    }
}

