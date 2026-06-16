package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.MicroLoanResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.met.domain.solidarity.port.MicroLoanPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListGroupLoansUseCase {

    private final SolidarityAuthorizationService authorizationService;
    private final MicroLoanPort loanPort;
    private final SolidarityMapper mapper;

    @Transactional(readOnly = true)
    public List<MicroLoanResponse> execute(UUID userId, UUID groupId) {
        authorizationService.requireMembership(groupId, userId);
        return loanPort.findByGroupId(groupId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
