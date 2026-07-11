package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.investment.model.InvestmentStatus;
import com.cooperativa.met.domain.investment.model.MicroInvestment;
import com.cooperativa.met.domain.investment.port.MicroInvestmentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalculateDailyRoyaltiesUseCase {

    private final MicroInvestmentPort investmentPort;
    private final CoreAccountRepositoryPort accountRepositoryPort;

    @Transactional
    public int execute() {
        log.info("Calculando rendimientos diarios (Regalías) para todas las inversiones activas...");
        List<MicroInvestment> activas = investmentPort.findByStatus(InvestmentStatus.ACTIVE);
        
        int processed = 0;
        for (MicroInvestment inv : activas) {
            // formula: monto * (tasa / 365)
            BigDecimal rendimientoDiario = inv.getMontoInvertido()
                    .multiply(inv.getTasaAplicada())
                    .divide(new BigDecimal("365"), 4, RoundingMode.HALF_UP);
            
            // Sumar a la inversión
            MicroInvestment updatedInv = inv.withRendimientoGanado(inv.getRendimientoGanado().add(rendimientoDiario));
            investmentPort.save(updatedInv);
            
            // Sumar a la billetera (regalías)
            accountRepositoryPort.findByUserId(inv.getUserId()).ifPresent(acc -> {
                // Here we would typically add it to a specific "royalty balance" or interest_balance
                CoreAccount updatedAcc = acc.creditInterest(rendimientoDiario);
                accountRepositoryPort.save(updatedAcc);
            });
            processed++;
        }
        
        return processed;
    }
}
