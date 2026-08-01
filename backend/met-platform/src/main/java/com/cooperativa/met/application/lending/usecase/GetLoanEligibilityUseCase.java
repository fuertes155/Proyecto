package com.cooperativa.met.application.lending.usecase;

import com.cooperativa.met.application.lending.dto.LoanEligibilityRequest;
import com.cooperativa.met.application.lending.dto.LoanEligibilityResponse;
import com.cooperativa.met.application.lending.mapper.LendingMapper;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.lending.model.CreditScoreResult;
import com.cooperativa.met.domain.lending.model.LoanEligibilityDecision;
import com.cooperativa.met.domain.lending.port.CreditBureauPort;
import com.cooperativa.met.domain.lending.service.CreditScoringEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Consulta preliminar de cuánto y en qué plazo puede pedir prestado el usuario,
 * según su score real en DataCrédito y su saldo de ahorro, sin crear una solicitud.
 *
 * Permite que el simulador de préstamo (UI) muestre límites personalizados por
 * usuario en vez de un rango genérico igual para todos, reutilizando la misma
 * política de riesgo ({@link CreditScoringEngine}) que aplica
 * {@code SubmitPersonalLoanApplicationUseCase} al momento de la solicitud real.
 */
@Service
@RequiredArgsConstructor
public class GetLoanEligibilityUseCase {

    private final UserRepositoryPort userRepository;
    private final CoreAccountRepositoryPort accountRepository;
    private final CreditBureauPort creditBureauPort;
    private final LendingMapper mapper;

    public LoanEligibilityResponse execute(UUID userId, LoanEligibilityRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Usuario no encontrado"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("USER_NOT_ACTIVE", "Cuenta no activa");
        }

        if (request.acceptedHabeasData() == null || !request.acceptedHabeasData()) {
            throw new BusinessRuleException("HABEAS_DATA_REQUIRED",
                    "Debes aceptar los términos de Habeas Data para consultar tu reporte de crédito.");
        }

        CreditScoreResult scoreResult = creditBureauPort.checkScore(
                userId,
                user.getDocumentNumber(),
                user.getFirstName(),
                user.getLastName(),
                null
        );

        // Si el score ya descalifica, no hace falta consultar el saldo de ahorro
        LoanEligibilityDecision preliminary = CreditScoringEngine.evaluate(scoreResult.getScore(), BigDecimal.ZERO);
        if (!preliminary.isApproved()) {
            return mapper.toResponse(preliminary);
        }

        CoreAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("NO_ACCOUNT",
                        "El usuario no tiene billetera virtual para validar el saldo"));

        LoanEligibilityDecision eligibility = CreditScoringEngine.evaluate(scoreResult.getScore(), account.getPrincipalBalance());
        return mapper.toResponse(eligibility);
    }
}
