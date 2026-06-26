package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;
import com.cooperativa.met.domain.investment.port.InvestmentCreditPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPortfolioPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: Cancelar un portfolio de micro-inversiones antes de su vencimiento.
 *
 * Al cancelar, se devuelve solo el capital invertido (sin rendimiento).
 * Todas las posiciones ACTIVE del portfolio se cancelan.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelMicroInvestmentUseCase {

    private final MicroInvestmentPortfolioPort portfolioPort;
    private final MicroInvestmentPort investmentPort;
    private final InvestmentCreditPort creditPort;

    @Transactional
    public void execute(UUID userId, UUID portfolioId) {
        // 1. Verificar que el portfolio existe y pertenece al usuario
        MicroInvestmentPortfolio portfolio = portfolioPort.findById(portfolioId)
                .orElseThrow(() -> new BusinessRuleException(
                        "PORTFOLIO_NOT_FOUND", "Portfolio no encontrado: " + portfolioId));

        if (!portfolio.getUserId().equals(userId)) {
            throw new BusinessRuleException(
                    "UNAUTHORIZED", "No tienes permiso para cancelar este portfolio.");
        }

        if (portfolio.getEstado() != InvestmentStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "INVALID_STATE",
                    "Solo se pueden cancelar portfolios en estado ACTIVE. Estado actual: " + portfolio.getEstado());
        }

        // 2. Obtener posiciones ACTIVE y devolver capital
        List<MicroInvestment> posicionesActivas = investmentPort.findByPortfolioId(portfolioId).stream()
                .filter(inv -> inv.getEstado() == InvestmentStatus.ACTIVE)
                .toList();

        for (MicroInvestment inv : posicionesActivas) {
            // Devolver solo el capital, sin rendimiento
            String reference = "INVEST-CANCEL-" + inv.getId();
            boolean credited = creditPort.credit(inv.getUserId(), inv.getMontoInvertido(), reference);
            if (!credited) {
                throw new IllegalStateException("No se pudo devolver el capital de la posición: " + inv.getId());
            }
            investmentPort.save(inv.cancelar());
            log.info("Posición {} cancelada. Capital {} devuelto al usuario {}", inv.getId(), inv.getMontoInvertido(), userId);
        }

        // 3. Marcar el portfolio como CANCELLED
        portfolioPort.save(portfolio.withEstado(InvestmentStatus.CANCELLED));
        log.info("Portfolio {} cancelado por el usuario {}", portfolioId, userId);
    }
}
