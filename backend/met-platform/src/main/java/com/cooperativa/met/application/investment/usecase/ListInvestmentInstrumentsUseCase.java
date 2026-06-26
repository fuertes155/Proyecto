package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.investment.model.InvestmentInstrument;
import com.cooperativa.met.domain.investment.port.InvestmentInstrumentPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso: Listar instrumentos de inversión disponibles para el usuario.
 */
@Service
@RequiredArgsConstructor
public class ListInvestmentInstrumentsUseCase {

    private final InvestmentInstrumentPort instrumentPort;

    /** Retorna sólo los instrumentos activos para mostrarlos en la app. */
    public List<InvestmentInstrument> listActivos() {
        return instrumentPort.findActivos();
    }

    /** Retorna todos los instrumentos (para el panel admin). */
    public List<InvestmentInstrument> listAll() {
        return instrumentPort.findAll();
    }
}
