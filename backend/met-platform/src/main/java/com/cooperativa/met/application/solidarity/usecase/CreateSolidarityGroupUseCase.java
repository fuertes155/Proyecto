package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.CreateSolidarityGroupRequest;
import com.cooperativa.met.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.solidarity.model.GroupStatus;
import com.cooperativa.met.domain.solidarity.model.MemberRole;
import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.model.SolidarityMember;
import com.cooperativa.met.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityMemberPort;
import com.cooperativa.met.domain.solidarity.service.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateSolidarityGroupUseCase {

    private final UserRepositoryPort userRepository;
    private final SolidarityGroupPort groupPort;
    private final SolidarityMemberPort memberPort;
    private final SolidarityMapper mapper;

    @Transactional
    public SolidarityGroupResponse execute(UUID userId, CreateSolidarityGroupRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Usuario no encontrado"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("USER_NOT_ACTIVE", "Cuenta no activa");
        }

        String inviteCode = generateUniqueInviteCode();
        BigDecimal minContribution = request.minContribution() != null
                ? request.minContribution()
                : new BigDecimal("10000.00");
        int maxMembers = request.maxMembers() != null ? request.maxMembers() : 20;

        SolidarityGroup group = SolidarityGroup.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .description(request.description())
                .creatorId(userId)
                .inviteCode(inviteCode)
                .minContribution(minContribution)
                .maxLoanPercentage(new BigDecimal("30.00"))
                .interestRate(new BigDecimal("0.0050"))
                .poolBalance(BigDecimal.ZERO)
                .maxMembers(maxMembers)
                .status(GroupStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        SolidarityGroup saved = groupPort.save(group);

        memberPort.save(SolidarityMember.builder()
                .id(UUID.randomUUID())
                .groupId(saved.getId())
                .userId(userId)
                .role(MemberRole.ADMIN)
                .totalContributed(BigDecimal.ZERO)
                .joinedAt(Instant.now())
                .build());

        return mapper.toResponse(saved, 1, MemberRole.ADMIN);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = InviteCodeGenerator.generate(8);
        } while (groupPort.existsByInviteCode(code));
        return code;
    }
}
