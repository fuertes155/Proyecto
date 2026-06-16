package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.MicroLoanResponse;
import com.cooperativa.met.application.solidarity.dto.RequestMicroLoanRequest;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.solidarity.model.MicroLoan;
import com.cooperativa.met.domain.solidarity.model.MicroLoanStatus;
import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.port.MicroLoanPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestMicroLoanUseCase {

    private final SolidarityAuthorizationService authorizationService;
    private final MicroLoanPort loanPort;
    private final SolidarityMapper mapper;

    @Transactional
    public MicroLoanResponse execute(UUID userId, UUID groupId, RequestMicroLoanRequest request) {
        SolidarityGroup group = authorizationService.requireGroup(groupId);
        authorizationService.requireMembership(groupId, userId);

        if (loanPort.hasActiveLoan(groupId, userId)) {
            throw new BusinessRuleException("ACTIVE_LOAN_EXISTS", "Ya tienes un micropréstamo activo en este grupo");
        }

        if (request.amount().compareTo(group.maxLoanAmount()) > 0) {
            throw new BusinessRuleException("LOAN_AMOUNT_EXCEEDED",
                    "El monto máximo disponible es $" + group.maxLoanAmount());
        }

        if (request.amount().compareTo(group.getPoolBalance()) > 0) {
            throw new BusinessRuleException("INSUFFICIENT_POOL", "Fondos insuficientes en el fondo solidario");
        }

        MicroLoan loan = MicroLoan.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .borrowerId(userId)
                .amount(request.amount())
                .purpose(request.purpose())
                .termMonths(request.termMonths())
                .interestRate(group.getInterestRate())
                .status(MicroLoanStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        return mapper.toResponse(loanPort.save(loan));
    }
}
