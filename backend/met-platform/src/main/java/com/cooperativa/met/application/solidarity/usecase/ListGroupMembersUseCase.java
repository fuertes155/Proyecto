package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.SolidarityMemberResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.met.domain.solidarity.port.SolidarityMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListGroupMembersUseCase {

    private final SolidarityAuthorizationService authorizationService;
    private final SolidarityMemberPort memberPort;
    private final SolidarityMapper mapper;

    @Transactional(readOnly = true)
    public List<SolidarityMemberResponse> execute(UUID userId, UUID groupId) {
        authorizationService.requireMembership(groupId, userId);
        return memberPort.findByGroupId(groupId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
