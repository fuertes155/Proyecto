package com.cooperativa.met.domain.investment.port;

import com.cooperativa.met.domain.investment.model.InvestmentInstrument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentInstrumentPort {

    List<InvestmentInstrument> findAll();

    List<InvestmentInstrument> findActivos();

    Optional<InvestmentInstrument> findById(UUID id);

    InvestmentInstrument save(InvestmentInstrument instrument);

    void deleteById(UUID id);
}
