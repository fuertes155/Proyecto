package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.ContributeToPoolRequest;
import com.cooperativa.met.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.savings.port.DebitSourcePort;
import com.cooperativa.met.domain.solidarity.model.PoolTransaction;
import com.cooperativa.met.domain.solidarity.model.PoolTransactionType;
import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.model.SolidarityMember;
import com.cooperativa.met.domain.solidarity.port.PoolTransactionPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityMemberPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityPoolCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContributeToPoolUseCase {

    private final SolidarityAuthorizationService authorizationService;
    private final SolidarityGroupPort groupPort;
    private final SolidarityMemberPort memberPort;
    private final PoolTransactionPort transactionPort;
    private final DebitSourcePort debitSourcePort;
    private final SolidarityPoolCachePort poolCachePort;
    private final SolidarityMapper mapper;

    @Transactional
    public SolidarityGroupResponse execute(UUID userId, UUID groupId, ContributeToPoolRequest request) {
        SolidarityGroup group = authorizationService.requireGroup(groupId);
        SolidarityMember member = authorizationService.requireMembership(groupId, userId);

        if (request.amount().compareTo(group.getMinContribution()) < 0) {
            throw new BusinessRuleException("BELOW_MIN_CONTRIBUTION",
                    "El aporte mínimo del grupo es $" + group.getMinContribution());
        }

        String reference = "SOLID-CONTRIB-" + groupId;
        if (!debitSourcePort.debit(userId, request.amount(), reference)) {
            throw new BusinessRuleException("INSUFFICIENT_FUNDS", "Fondos insuficientes para el aporte");
        }

        var newBalance = group.getPoolBalance().add(request.amount());
        SolidarityGroup updatedGroup = groupPort.save(group.withPoolBalance(newBalance));
        SolidarityMember updatedMember = memberPort.save(member.addContribution(request.amount()));

        transactionPort.save(PoolTransaction.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .memberId(updatedMember.getId())
                .type(PoolTransactionType.CONTRIBUTION)
                .amount(request.amount())
                .balanceAfter(newBalance)
                .description("Aporte solidario")
                .createdAt(Instant.now())
                .build());

        poolCachePort.invalidate(groupId);
        poolCachePort.cacheBalance(groupId, newBalance);

        int memberCount = memberPort.countByGroupId(groupId);
        return mapper.toResponse(updatedGroup, memberCount, updatedMember.getRole());
    }
}
