package com.cooperativa.met.application.investment.usecase;

import com.cooperativa.met.application.investment.dto.InvestmentBreakdownItemResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Le muestra al inversionista, de forma transparente, en qué préstamos
 * concretos quedó fraccionado y emparejado el capital de sus depósitos por
 * el motor P2P (en vez de un producto de inversión aparte y desconectado).
 */
@Service
@RequiredArgsConstructor
public class GetInvestorFundingBreakdownUseCase {

    private final CoreAccountRepositoryPort accountRepository;
    private final InvestmentFractionRepositoryPort fractionRepository;
    private final InvestmentMatchRepositoryPort matchRepository;
    private final PersonalLoanApplicationPort loanRepository;
    private final UserRepositoryPort userRepository;

    public List<InvestmentBreakdownItemResponse> execute(UUID userId) {
        Optional<CoreAccount> accountOpt = accountRepository.findByUserId(userId);
        if (accountOpt.isEmpty()) {
            return List.of();
        }

        List<InvestmentFraction> fractions = fractionRepository.findByInvestorAccountId(accountOpt.get().getId());

        return fractions.stream()
                // SPLIT es solo el marcador histórico del "padre": su monto ya vive
                // repartido en las fracciones hijas (MATCHED/AVAILABLE) que sí se listan.
                .filter(f -> f.getStatus() != InvestmentFractionStatus.SPLIT)
                .map(this::toItem)
                .sorted(Comparator.comparing(
                        InvestmentBreakdownItemResponse::matchedAt,
                        Comparator.nullsFirst(Comparator.reverseOrder())))
                .toList();
    }

    private InvestmentBreakdownItemResponse toItem(InvestmentFraction fraction) {
        List<InvestmentMatch> matches = fraction.getStatus() == InvestmentFractionStatus.AVAILABLE
                ? List.of()
                : matchRepository.findByFractionId(fraction.getId());

        if (matches.isEmpty()) {
            return new InvestmentBreakdownItemResponse(
                    fraction.getId(), "Fondo de liquidez", null, fraction.getAmount(),
                    "DISPONIBLE", fraction.getCreatedAt());
        }

        InvestmentMatch match = matches.get(0);
        PersonalLoanApplication loan = loanRepository.findById(match.getBorrowerLoanId()).orElse(null);

        String borrowerName = "Deudor de la cooperativa";
        String status = fraction.getStatus() == InvestmentFractionStatus.RETURNED ? "DEVUELTO" : "ACTIVO";

        if (loan != null) {
            User borrower = userRepository.findById(loan.getUserId()).orElse(null);
            if (borrower != null) {
                borrowerName = borrower.getFirstName() + " " + borrower.getLastName();
            }
            if (loan.getStatus() == LoanApplicationStatus.PAID_OFF) {
                status = "PAGADO";
            }
        }

        return new InvestmentBreakdownItemResponse(
                fraction.getId(), borrowerName, match.getBorrowerLoanId(), fraction.getAmount(),
                status, match.getMatchedAt());
    }
}
