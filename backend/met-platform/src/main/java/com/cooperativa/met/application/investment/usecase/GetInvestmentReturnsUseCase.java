package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.investment.model.InvestmentReturn;
import com.cooperativa.met.domain.investment.port.InvestmentReturnPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: Consultar el historial de rendimientos pagados al usuario.
 */
@Service
@RequiredArgsConstructor
public class GetInvestmentReturnsUseCase {

    private final InvestmentReturnPort returnPort;

    public List<InvestmentReturn> listByUser(UUID userId) {
        return returnPort.findByUserId(userId);
    }
}
