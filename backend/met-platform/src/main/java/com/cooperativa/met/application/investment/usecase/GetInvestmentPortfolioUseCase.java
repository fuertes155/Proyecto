package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.dto.MicroInvestmentResponse;
import com.cooperativa.met.application.investment.dto.PortfolioResponse;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.model.MicroInvestmentPortfolio;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPortfolioPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: Obtener portfolios de micro-inversiones del usuario con sus posiciones.
 */
@Service
@RequiredArgsConstructor
public class GetInvestmentPortfolioUseCase {

    private final MicroInvestmentPortfolioPort portfolioPort;
    private final MicroInvestmentPort investmentPort;
    private final InvestmentInstrumentPort instrumentPort;

    /** Lista todos los portfolios del usuario con sus posiciones detalladas. */
    public List<PortfolioResponse> listByUser(UUID userId) {
        return portfolioPort.findByUserId(userId).stream()
                .map(portfolio -> buildResponse(portfolio))
                .toList();
    }

    /** Obtiene un portfolio específico por id. */
    public PortfolioResponse getById(UUID portfolioId) {
        MicroInvestmentPortfolio portfolio = portfolioPort.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio no encontrado: " + portfolioId));
        return buildResponse(portfolio);
    }

    private PortfolioResponse buildResponse(MicroInvestmentPortfolio portfolio) {
        List<MicroInvestment> posiciones = investmentPort.findByPortfolioId(portfolio.getId());

        List<MicroInvestmentResponse> posicionesResponse = posiciones.stream()
                .map(inv -> {
                    String nombre = instrumentPort.findById(inv.getInstrumentId())
                            .map(i -> i.getNombre())
                            .orElse("Instrumento no encontrado");
                    return MicroInvestmentResponse.from(inv, nombre);
                })
                .toList();

        return PortfolioResponse.from(portfolio, posicionesResponse);
    }
}
