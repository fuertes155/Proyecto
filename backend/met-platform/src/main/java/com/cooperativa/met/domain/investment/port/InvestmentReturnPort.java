package com.cooperativa.met.domain.investment.port;

import com.cooperativa.met.domain.investment.model.InvestmentReturn;

import java.util.List;
import java.util.UUID;

public interface InvestmentReturnPort {

    InvestmentReturn save(InvestmentReturn investmentReturn);

    List<InvestmentReturn> findByUserId(UUID userId);
}
