package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.ContributeToPoolRequest;
import com.cooperativa.met.application.solidarity.dto.JoinSolidarityGroupRequest;
import com.cooperativa.met.application.solidarity.dto.MicroLoanResponse;
import com.cooperativa.met.application.solidarity.dto.PoolTransactionResponse;
import com.cooperativa.met.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.met.application.solidarity.dto.SolidarityMemberResponse;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.savings.port.DebitSourcePort;
import com.cooperativa.met.domain.solidarity.model.GroupStatus;
import com.cooperativa.met.domain.solidarity.model.MemberRole;
import com.cooperativa.met.domain.solidarity.model.MicroLoan;
import com.cooperativa.met.domain.solidarity.model.MicroLoanStatus;
import com.cooperativa.met.domain.solidarity.model.PoolTransaction;
import com.cooperativa.met.domain.solidarity.model.PoolTransactionType;
import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.model.SolidarityMember;
import com.cooperativa.met.domain.solidarity.port.MicroLoanPort;
import com.cooperativa.met.domain.solidarity.port.PoolTransactionPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityMemberPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityPoolCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Aportes al fondo, ingreso al grupo y las consultas que solo pueden ver los miembros. */
@ExtendWith(MockitoExtension.class)
class SolidarityGroupUseCasesTest {

    @Mock private SolidarityGroupPort groupPort;
    @Mock private SolidarityMemberPort memberPort;
    @Mock private PoolTransactionPort transactionPort;
    @Mock private MicroLoanPort loanPort;
    @Mock private DebitSourcePort debitSourcePort;
    @Mock private SolidarityPoolCachePort poolCachePort;

    private final SolidarityMapper mapper = new SolidarityMapper();
    private SolidarityAuthorizationService authorizationService;

    private final UUID groupId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        authorizationService = new SolidarityAuthorizationService(groupPort, memberPort);
    }

    @Test
    void shouldAddContributionToThePoolAndToTheMembersTotal() {
        ContributeToPoolUseCase contribute = new ContributeToPoolUseCase(authorizationService, groupPort, memberPort,
                transactionPort, debitSourcePort, poolCachePort, mapper);
        when(groupPort.findById(groupId)).thenReturn(Optional.of(group(GroupStatus.ACTIVE, 20)));
        givenMember(MemberRole.MEMBER, "40000.00");
        when(debitSourcePort.debit(userId, new BigDecimal("10000.00"), "SOLID-CONTRIB-" + groupId)).thenReturn(true);
        when(groupPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberPort.countByGroupId(groupId)).thenReturn(7);

        SolidarityGroupResponse response = contribute.execute(userId, groupId, new ContributeToPoolRequest(new BigDecimal("10000.00")));

        assertEquals(new BigDecimal("510000.00"), response.poolBalance());
        assertEquals(7, response.memberCount());
        ArgumentCaptor<SolidarityMember> member = ArgumentCaptor.forClass(SolidarityMember.class);
        verify(memberPort).save(member.capture());
        assertEquals(new BigDecimal("50000.00"), member.getValue().getTotalContributed());
        ArgumentCaptor<PoolTransaction> movement = ArgumentCaptor.forClass(PoolTransaction.class);
        verify(transactionPort).save(movement.capture());
        assertEquals(PoolTransactionType.CONTRIBUTION, movement.getValue().getType());
        verify(poolCachePort).cacheBalance(groupId, new BigDecimal("510000.00"));
    }

    @Test
    void shouldRejectContributionBelowTheGroupMinimum() {
        ContributeToPoolUseCase contribute = new ContributeToPoolUseCase(authorizationService, groupPort, memberPort,
                transactionPort, debitSourcePort, poolCachePort, mapper);
        when(groupPort.findById(groupId)).thenReturn(Optional.of(group(GroupStatus.ACTIVE, 20)));
        givenMember(MemberRole.MEMBER, "0");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> contribute.execute(userId, groupId, new ContributeToPoolRequest(new BigDecimal("4999.99"))));

        assertEquals("BELOW_MIN_CONTRIBUTION", ex.getCode());
        verifyNoInteractions(debitSourcePort);
    }

    @Test
    void shouldNotTouchThePoolWhenTheMemberHasNoFunds() {
        ContributeToPoolUseCase contribute = new ContributeToPoolUseCase(authorizationService, groupPort, memberPort,
                transactionPort, debitSourcePort, poolCachePort, mapper);
        when(groupPort.findById(groupId)).thenReturn(Optional.of(group(GroupStatus.ACTIVE, 20)));
        givenMember(MemberRole.MEMBER, "0");
        when(debitSourcePort.debit(any(), any(), any())).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> contribute.execute(userId, groupId, new ContributeToPoolRequest(new BigDecimal("10000.00"))));

        assertEquals("INSUFFICIENT_FUNDS", ex.getCode());
        verify(groupPort, never()).save(any());
        verifyNoInteractions(transactionPort, poolCachePort);
    }

    @Test
    void shouldJoinWithTheInviteCodeInAnyCase() {
        JoinSolidarityGroupUseCase join = new JoinSolidarityGroupUseCase(groupPort, memberPort, mapper);
        when(groupPort.findByInviteCode("ABCD1234")).thenReturn(Optional.of(group(GroupStatus.ACTIVE, 20)));
        when(memberPort.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.empty());
        when(memberPort.countByGroupId(groupId)).thenReturn(4);

        SolidarityGroupResponse response = join.execute(userId, new JoinSolidarityGroupRequest("abcd1234"));

        assertEquals(5, response.memberCount());
        assertEquals(MemberRole.MEMBER, response.myRole());
        ArgumentCaptor<SolidarityMember> member = ArgumentCaptor.forClass(SolidarityMember.class);
        verify(memberPort).save(member.capture());
        assertEquals(MemberRole.MEMBER, member.getValue().getRole());
        assertEquals(BigDecimal.ZERO, member.getValue().getTotalContributed());
    }

    @Test
    void shouldNotJoinAFullGroup() {
        JoinSolidarityGroupUseCase join = new JoinSolidarityGroupUseCase(groupPort, memberPort, mapper);
        when(groupPort.findByInviteCode("ABCD1234")).thenReturn(Optional.of(group(GroupStatus.ACTIVE, 5)));
        when(memberPort.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.empty());
        when(memberPort.countByGroupId(groupId)).thenReturn(5);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> join.execute(userId, new JoinSolidarityGroupRequest("ABCD1234")));

        assertEquals("GROUP_FULL", ex.getCode());
        verify(memberPort, never()).save(any());
    }

    @Test
    void shouldNotJoinTwiceOrAClosedGroupOrWithAWrongCode() {
        JoinSolidarityGroupUseCase join = new JoinSolidarityGroupUseCase(groupPort, memberPort, mapper);
        when(groupPort.findByInviteCode("ABCD1234")).thenReturn(Optional.of(group(GroupStatus.ACTIVE, 20)));
        when(groupPort.findByInviteCode("CLOSED00")).thenReturn(Optional.of(group(GroupStatus.CLOSED, 20)));
        when(groupPort.findByInviteCode("NOEXISTE")).thenReturn(Optional.empty());
        givenMember(MemberRole.MEMBER, "0");

        assertEquals("ALREADY_MEMBER", assertThrows(BusinessRuleException.class,
                () -> join.execute(userId, new JoinSolidarityGroupRequest("ABCD1234"))).getCode());
        assertEquals("GROUP_CLOSED", assertThrows(BusinessRuleException.class,
                () -> join.execute(userId, new JoinSolidarityGroupRequest("CLOSED00"))).getCode());
        assertEquals("INVALID_INVITE_CODE", assertThrows(BusinessRuleException.class,
                () -> join.execute(userId, new JoinSolidarityGroupRequest("NOEXISTE"))).getCode());
        verify(memberPort, never()).save(any());
    }

    @Test
    void shouldShowGroupDetailsOnlyToMembers() {
        ListGroupMembersUseCase members = new ListGroupMembersUseCase(authorizationService, memberPort, mapper);
        ListGroupLoansUseCase loans = new ListGroupLoansUseCase(authorizationService, loanPort, mapper);
        ListPoolTransactionsUseCase movements = new ListPoolTransactionsUseCase(authorizationService, transactionPort, mapper);
        when(memberPort.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> members.execute(userId, groupId));
        assertThrows(BusinessRuleException.class, () -> loans.execute(userId, groupId));
        assertThrows(BusinessRuleException.class, () -> movements.execute(userId, groupId));

        verifyNoInteractions(loanPort, transactionPort);
    }

    @Test
    void shouldListMembersLoansAndMovementsForMembers() {
        givenMember(MemberRole.ADMIN, "120000.00");
        when(memberPort.findByGroupId(groupId)).thenReturn(List.of(member(MemberRole.ADMIN, "120000.00")));
        when(loanPort.findByGroupId(groupId)).thenReturn(List.of(MicroLoan.builder()
                .id(UUID.randomUUID()).groupId(groupId).borrowerId(userId).amount(new BigDecimal("80000.00"))
                .termMonths(4).status(MicroLoanStatus.PENDING).build()));
        when(transactionPort.findByGroupId(groupId)).thenReturn(List.of(PoolTransaction.builder()
                .id(UUID.randomUUID()).groupId(groupId).type(PoolTransactionType.CONTRIBUTION)
                .amount(new BigDecimal("10000.00")).balanceAfter(new BigDecimal("510000.00")).build()));

        List<SolidarityMemberResponse> members = new ListGroupMembersUseCase(authorizationService, memberPort, mapper).execute(userId, groupId);
        List<MicroLoanResponse> loans = new ListGroupLoansUseCase(authorizationService, loanPort, mapper).execute(userId, groupId);
        List<PoolTransactionResponse> movements = new ListPoolTransactionsUseCase(authorizationService, transactionPort, mapper).execute(userId, groupId);

        assertEquals(new BigDecimal("120000.00"), members.get(0).totalContributed());
        assertEquals(MicroLoanStatus.PENDING, loans.get(0).status());
        assertEquals(new BigDecimal("510000.00"), movements.get(0).balanceAfter());
    }

    @Test
    void shouldListMyGroupsWithMyRoleInEach() {
        ListMySolidarityGroupsUseCase myGroups = new ListMySolidarityGroupsUseCase(groupPort, memberPort, mapper);
        when(groupPort.findByUserId(userId)).thenReturn(List.of(group(GroupStatus.ACTIVE, 20)));
        givenMember(MemberRole.ADMIN, "0");
        when(memberPort.countByGroupId(groupId)).thenReturn(3);

        List<SolidarityGroupResponse> groups = myGroups.execute(userId);

        assertEquals(MemberRole.ADMIN, groups.get(0).myRole());
        assertEquals(3, groups.get(0).memberCount());
        assertEquals(new BigDecimal("150000.00"), groups.get(0).maxLoanAmount());
    }

    @Test
    void shouldFailForUnknownGroup() {
        when(groupPort.findById(groupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authorizationService.requireGroup(groupId));
    }

    private void givenMember(MemberRole role, String contributed) {
        when(memberPort.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(member(role, contributed)));
    }

    private SolidarityMember member(MemberRole role, String contributed) {
        return SolidarityMember.builder().id(UUID.randomUUID()).groupId(groupId).userId(userId).role(role)
                .totalContributed(new BigDecimal(contributed)).joinedAt(Instant.now()).build();
    }

    private SolidarityGroup group(GroupStatus status, int maxMembers) {
        return SolidarityGroup.builder()
                .id(groupId)
                .name("Tenderos del barrio")
                .inviteCode("ABCD1234")
                .minContribution(new BigDecimal("5000.00"))
                .maxLoanPercentage(new BigDecimal("30"))
                .interestRate(new BigDecimal("0.0200"))
                .poolBalance(new BigDecimal("500000.00"))
                .maxMembers(maxMembers)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }
}
