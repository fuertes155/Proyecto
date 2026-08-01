package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.dto.InvestmentPortfolioSummaryResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.investment.model.InvestmentFraction;
import com.cooperativa.met.domain.investment.model.InvestmentFractionStatus;
import com.cooperativa.met.domain.investment.model.InvestmentMatch;
import com.cooperativa.met.domain.investment.port.InvestmentFractionRepositoryPort;
import com.cooperativa.met.domain.investment.port.InvestmentMatchRepositoryPort;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Le muestra al inversionista un resumen agregado de en qué está trabajando
 * su capital (activo generando rendimiento, disponible por asignar, ya
 * recuperado) sin exponer a qué socios concretos quedó emparejado: la
 * identidad de otro socio es dato sensible y solo la ve un administrador
 * (ver AdminInvestmentController / GetInvestorFundingBreakdownUseCase).
 */
@Service
@RequiredArgsConstructor
public class GetInvestorPortfolioSummaryUseCase {

    private final CoreAccountRepositoryPort accountRepository;
    private final InvestmentFractionRepositoryPort fractionRepository;
    private final InvestmentMatchRepositoryPort matchRepository;
    private final PersonalLoanApplicationPort loanRepository;

    public InvestmentPortfolioSummaryResponse execute(UUID userId) {
        Optional<CoreAccount> accountOpt = accountRepository.findByUserId(userId);
        if (accountOpt.isEmpty()) {
            return empty();
        }

        List<InvestmentFraction> fractions = fractionRepository.findByInvestorAccountId(accountOpt.get().getId())
                // SPLIT es solo el marcador histórico del "padre": su monto ya vive
                // repartido en las fracciones hijas (MATCHED/AVAILABLE) que sí se cuentan.
                .stream()
                .filter(f -> f.getStatus() != InvestmentFractionStatus.SPLIT)
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal active = BigDecimal.ZERO;
        BigDecimal available = BigDecimal.ZERO;
        BigDecimal paidOff = BigDecimal.ZERO;
        BigDecimal returned = BigDecimal.ZERO;
        Set<UUID> loansFunded = new HashSet<>();

        for (InvestmentFraction fraction : fractions) {
            total = total.add(fraction.getAmount());

            if (fraction.getStatus() == InvestmentFractionStatus.AVAILABLE) {
                available = available.add(fraction.getAmount());
                continue;
            }
            if (fraction.getStatus() == InvestmentFractionStatus.RETURNED) {
                returned = returned.add(fraction.getAmount());
                continue;
            }

            List<InvestmentMatch> matches = matchRepository.findByFractionId(fraction.getId());
            if (matches.isEmpty()) {
                active = active.add(fraction.getAmount());
                continue;
            }

            InvestmentMatch match = matches.get(0);
            loansFunded.add(match.getBorrowerLoanId());
            PersonalLoanApplication loan = loanRepository.findById(match.getBorrowerLoanId()).orElse(null);
            if (loan != null && loan.getStatus() == LoanApplicationStatus.PAID_OFF) {
                paidOff = paidOff.add(fraction.getAmount());
            } else {
                active = active.add(fraction.getAmount());
            }
        }

        return new InvestmentPortfolioSummaryResponse(total, active, available, paidOff, returned, loansFunded.size());
    }

    private InvestmentPortfolioSummaryResponse empty() {
        return new InvestmentPortfolioSummaryResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
}
