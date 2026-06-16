package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.LoanInstallmentResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.solidarity.model.MicroLoan;
import com.cooperativa.met.domain.solidarity.port.LoanInstallmentPort;
import com.cooperativa.met.domain.solidarity.port.MicroLoanPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListLoanInstallmentsUseCase {

    private final SolidarityAuthorizationService authorizationService;
    private final MicroLoanPort loanPort;
    private final LoanInstallmentPort installmentPort;
    private final SolidarityMapper mapper;

    @Transactional(readOnly = true)
    public List<LoanInstallmentResponse> execute(UUID userId, UUID groupId, UUID loanId) {
        authorizationService.requireMembership(groupId, userId);

        MicroLoan loan = loanPort.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Micropréstamo no encontrado"));

        if (!loan.getGroupId().equals(groupId)) {
            throw new ResourceNotFoundException("Micropréstamo no encontrado");
        }

        return installmentPort.findByLoanId(loanId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
