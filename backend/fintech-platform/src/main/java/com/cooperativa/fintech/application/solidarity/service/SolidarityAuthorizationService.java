package com.cooperativa.fintech.application.solidarity.service;

import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.solidarity.model.MemberRole;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.fintech.domain.solidarity.model.SolidarityMember;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.fintech.domain.solidarity.port.SolidarityMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolidarityAuthorizationService {

    private final SolidarityGroupPort groupPort;
    private final SolidarityMemberPort memberPort;

    public SolidarityGroup requireGroup(UUID groupId) {
        return groupPort.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo solidario no encontrado"));
    }

    public SolidarityMember requireMembership(UUID groupId, UUID userId) {
        return memberPort.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessRuleException("NOT_A_MEMBER", "No eres miembro de este grupo"));
    }

    public SolidarityMember requireAdmin(UUID groupId, UUID userId) {
        SolidarityMember member = requireMembership(groupId, userId);
        if (member.getRole() != MemberRole.ADMIN) {
            throw new BusinessRuleException("NOT_ADMIN", "Solo administradores pueden realizar esta acción");
        }
        return member;
    }
}
