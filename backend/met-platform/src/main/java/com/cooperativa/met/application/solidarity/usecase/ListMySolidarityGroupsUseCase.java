package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.model.SolidarityMember;
import com.cooperativa.met.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListMySolidarityGroupsUseCase {

    private final SolidarityGroupPort groupPort;
    private final SolidarityMemberPort memberPort;
    private final SolidarityMapper mapper;

    @Transactional(readOnly = true)
    public List<SolidarityGroupResponse> execute(UUID userId) {
        return groupPort.findByUserId(userId).stream()
                .map(group -> toResponse(group, userId))
                .toList();
    }

    private SolidarityGroupResponse toResponse(SolidarityGroup group, UUID userId) {
        SolidarityMember membership = memberPort.findByGroupIdAndUserId(group.getId(), userId).orElseThrow();
        int memberCount = memberPort.countByGroupId(group.getId());
        return mapper.toResponse(group, memberCount, membership.getRole());
    }
}
