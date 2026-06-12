package com.cooperativa.fintech.application.solidarity.usecase;

import com.cooperativa.fintech.application.solidarity.dto.JoinSolidarityGroupRequest;
import com.cooperativa.fintech.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.fintech.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.solidarity.model.GroupStatus;
import com.cooperativa.fintech.domain.solidarity.model.MemberRole;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityMember;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JoinSolidarityGroupUseCase {

    private final SolidarityGroupPort groupPort;
    private final SolidarityMemberPort memberPort;
    private final SolidarityMapper mapper;

    @Transactional
    public SolidarityGroupResponse execute(UUID userId, JoinSolidarityGroupRequest request) {
        SolidarityGroup group = groupPort.findByInviteCode(request.inviteCode().toUpperCase())
                .orElseThrow(() -> new BusinessRuleException("INVALID_INVITE_CODE", "Código de invitación inválido"));

        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw new BusinessRuleException("GROUP_CLOSED", "El grupo no acepta nuevos miembros");
        }

        if (memberPort.findByGroupIdAndUserId(group.getId(), userId).isPresent()) {
            throw new BusinessRuleException("ALREADY_MEMBER", "Ya eres miembro de este grupo");
        }

        int memberCount = memberPort.countByGroupId(group.getId());
        if (memberCount >= group.getMaxMembers()) {
            throw new BusinessRuleException("GROUP_FULL", "El grupo ha alcanzado el máximo de miembros");
        }

        memberPort.save(SolidarityMember.builder()
                .id(UUID.randomUUID())
                .groupId(group.getId())
                .userId(userId)
                .role(MemberRole.MEMBER)
                .totalContributed(BigDecimal.ZERO)
                .joinedAt(Instant.now())
                .build());

        return mapper.toResponse(group, memberCount + 1, MemberRole.MEMBER);
    }
}
