package com.cooperativa.fintech.application.solidarity.usecase;

import com.cooperativa.fintech.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.fintech.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.fintech.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityMember;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityMemberPort;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityPoolCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSolidarityGroupUseCase {

    private final SolidarityAuthorizationService authorizationService;
    private final SolidarityMemberPort memberPort;
    private final SolidarityPoolCachePort poolCachePort;
    private final SolidarityMapper mapper;

    @Transactional(readOnly = true)
    public SolidarityGroupResponse execute(UUID userId, UUID groupId) {
        SolidarityGroup group = authorizationService.requireGroup(groupId);
        SolidarityMember membership = authorizationService.requireMembership(groupId, userId);

        poolCachePort.getCachedBalance(groupId)
                .filter(cached -> cached.compareTo(group.getPoolBalance()) == 0)
                .ifPresentOrElse(
                        ignored -> {},
                        () -> poolCachePort.cacheBalance(groupId, group.getPoolBalance())
                );

        int memberCount = memberPort.countByGroupId(groupId);
        return mapper.toResponse(group, memberCount, membership.getRole());
    }
}
