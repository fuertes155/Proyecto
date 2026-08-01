package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.MovementResponse;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Alimenta la pantalla "Mis movimientos" del app: a diferencia del extracto mensual
 * (CSV, un mes a la vez), esto trae TODO el historial de la cuenta en un solo listado
 * -- depósitos, transferencias, préstamos desembolsados, inversiones en préstamos de
 * otros socios y retiros -- para que el usuario vea de un vistazo toda su actividad.
 */
@Service
@RequiredArgsConstructor
public class GetAccountMovementsUseCase {

    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;

    @Transactional(readOnly = true)
    public List<MovementResponse> execute(UUID userId) {
        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una cuenta para el usuario"));

        return transactionRepository.findByAccountId(account.getId()).stream()
                .sorted(Comparator.comparing(CoreTransaction::getCreatedAt).reversed())
                .map(tx -> toResponse(tx, account.getId()))
                .toList();
    }

    private MovementResponse toResponse(CoreTransaction tx, UUID accountId) {
        boolean isCredit = accountId.equals(tx.getDestinationAccountId());
        return new MovementResponse(
                tx.getId(),
                tx.getType().name(),
                label(tx.getType()),
                tx.getConcept(),
                tx.getAmount(),
                isCredit,
                tx.getStatus() != null ? tx.getStatus().name() : null,
                tx.getCreatedAt()
        );
    }

    private String label(TransactionType type) {
        return switch (type) {
            case DEPOSIT -> "Recarga";
            case WITHDRAWAL -> "Retiro";
            case TRANSFER -> "Transferencia";
            case EXTERNAL_PAYOUT -> "Retiro a cuenta externa";
            case INVESTMENT_FUNDING -> "Inversión en préstamo de otro socio";
            case LOAN_DISBURSEMENT -> "Préstamo desembolsado";
        };
    }
}
