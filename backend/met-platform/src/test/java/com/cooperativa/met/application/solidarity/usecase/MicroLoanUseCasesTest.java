package com.cooperativa.met.application.solidarity.usecase;

import com.cooperativa.met.application.solidarity.dto.LoanInstallmentResponse;
import com.cooperativa.met.application.solidarity.dto.MicroLoanResponse;
import com.cooperativa.met.application.solidarity.dto.RequestMicroLoanRequest;
import com.cooperativa.met.application.solidarity.dto.ReviewMicroLoanRequest;
import com.cooperativa.met.application.solidarity.mapper.SolidarityMapper;
import com.cooperativa.met.application.solidarity.service.SolidarityAuthorizationService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.savings.port.DebitSourcePort;
import com.cooperativa.met.domain.solidarity.model.GroupStatus;
import com.cooperativa.met.domain.solidarity.model.InstallmentStatus;
import com.cooperativa.met.domain.solidarity.model.LoanInstallment;
import com.cooperativa.met.domain.solidarity.model.MemberRole;
import com.cooperativa.met.domain.solidarity.model.MicroLoan;
import com.cooperativa.met.domain.solidarity.model.MicroLoanStatus;
import com.cooperativa.met.domain.solidarity.model.PoolTransaction;
import com.cooperativa.met.domain.solidarity.model.PoolTransactionType;
import com.cooperativa.met.domain.solidarity.model.SolidarityGroup;
import com.cooperativa.met.domain.solidarity.model.SolidarityMember;
import com.cooperativa.met.domain.solidarity.port.LoanInstallmentPort;
import com.cooperativa.met.domain.solidarity.port.MicroLoanPort;
import com.cooperativa.met.domain.solidarity.port.PoolTransactionPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityGroupPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityMemberPort;
import com.cooperativa.met.domain.solidarity.port.SolidarityPoolCachePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Ciclo de vida de un micropréstamo del fondo solidario: solicitud, revisión del administrador del
 * grupo y pago de cuotas. Usa el {@link SolidarityAuthorizationService} y el mapper reales: lo que
 * se prueba incluye quién puede hacer qué dentro del grupo.
 */
@ExtendWith(MockitoExtension.class)
class MicroLoanUseCasesTest {

    private static final BigDecimal POOL = new BigDecimal("1000000.00");

    @Mock private SolidarityGroupPort groupPort;
    @Mock private SolidarityMemberPort memberPort;
    @Mock private MicroLoanPort loanPort;
    @Mock private LoanInstallmentPort installmentPort;
    @Mock private PoolTransactionPort transactionPort;
    @Mock private SolidarityPoolCachePort poolCachePort;
    @Mock private DebitSourcePort debitSourcePort;

    private final SolidarityMapper mapper = new SolidarityMapper();
    private SolidarityAuthorizationService authorizationService;

    private final UUID groupId = UUID.randomUUID();
    private final UUID borrowerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID loanId = UUID.randomUUID();

    @BeforeEach
    void setUpAuthorization() {
        authorizationService = new SolidarityAuthorizationService(groupPort, memberPort);
    }

    @Nested
    class Requesting {

        private RequestMicroLoanUseCase requestUseCase;

        @BeforeEach
        void setUp() {
            requestUseCase = new RequestMicroLoanUseCase(authorizationService, loanPort, mapper);
        }

        @Test
        void shouldCreatePendingLoanAtTheGroupRate() {
            givenGroup();
            givenMember(borrowerId, MemberRole.MEMBER);
            when(loanPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MicroLoanResponse response = requestUseCase.execute(borrowerId, groupId,
                    new RequestMicroLoanRequest(new BigDecimal("300000.00"), "Insumos del negocio", 6));

            assertEquals(MicroLoanStatus.PENDING, response.status());
            assertEquals(new BigDecimal("0.0200"), response.interestRate());
            assertEquals(borrowerId, response.borrowerId());
        }

        @Test
        void shouldCapTheLoanAtThePercentageOfThePoolTheGroupAllows() {
            givenGroup();
            givenMember(borrowerId, MemberRole.MEMBER);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> requestUseCase.execute(borrowerId, groupId,
                    new RequestMicroLoanRequest(new BigDecimal("300000.01"), "Insumos", 6)));

            assertEquals("LOAN_AMOUNT_EXCEEDED", ex.getCode());
            verify(loanPort, never()).save(any());
        }

        @Test
        void shouldAllowOnlyOneActiveLoanPerMember() {
            givenGroup();
            givenMember(borrowerId, MemberRole.MEMBER);
            when(loanPort.hasActiveLoan(groupId, borrowerId)).thenReturn(true);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> requestUseCase.execute(borrowerId, groupId,
                    new RequestMicroLoanRequest(new BigDecimal("10000.00"), "Insumos", 6)));

            assertEquals("ACTIVE_LOAN_EXISTS", ex.getCode());
        }

        @Test
        void shouldRefuseOutsiders() {
            givenGroup();
            when(memberPort.findByGroupIdAndUserId(groupId, borrowerId)).thenReturn(Optional.empty());

            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> requestUseCase.execute(borrowerId, groupId,
                    new RequestMicroLoanRequest(new BigDecimal("10000.00"), "Insumos", 6)));

            assertEquals("NOT_A_MEMBER", ex.getCode());
            verifyNoInteractions(loanPort);
        }
    }

    @Nested
    class Reviewing {

        private ReviewMicroLoanUseCase reviewUseCase;

        @BeforeEach
        void setUp() {
            reviewUseCase = new ReviewMicroLoanUseCase(authorizationService, loanPort, groupPort, installmentPort,
                    transactionPort, poolCachePort, mapper);
        }

        @Test
        void shouldDisburseFromThePoolAndCreateTheInstallmentPlan() {
            givenMember(adminId, MemberRole.ADMIN);
            givenLoan(MicroLoanStatus.PENDING, new BigDecimal("300000.00"));
            givenGroup();
            when(loanPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MicroLoanResponse response = reviewUseCase.execute(adminId, groupId, loanId, new ReviewMicroLoanRequest(true, null));

            assertEquals(MicroLoanStatus.DISBURSED, response.status());
            ArgumentCaptor<SolidarityGroup> group = ArgumentCaptor.forClass(SolidarityGroup.class);
            verify(groupPort).save(group.capture());
            assertEquals(new BigDecimal("700000.00"), group.getValue().getPoolBalance());

            ArgumentCaptor<List<LoanInstallment>> plan = installmentPlan();
            assertEquals(6, plan.getValue().size());

            PoolTransaction movement = poolTransaction();
            assertEquals(PoolTransactionType.LOAN_DISBURSEMENT, movement.getType());
            assertEquals(new BigDecimal("700000.00"), movement.getBalanceAfter());
            verify(poolCachePort).cacheBalance(groupId, new BigDecimal("700000.00"));
        }

        @Test
        void shouldRecordTheReasonWhenRejecting() {
            givenMember(adminId, MemberRole.ADMIN);
            givenLoan(MicroLoanStatus.PENDING, new BigDecimal("300000.00"));
            when(loanPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MicroLoanResponse response = reviewUseCase.execute(adminId, groupId, loanId,
                    new ReviewMicroLoanRequest(false, "Ya tiene mora en otro grupo"));

            assertEquals(MicroLoanStatus.REJECTED, response.status());
            assertEquals("Ya tiene mora en otro grupo", response.rejectionReason());
            verify(groupPort, never()).save(any());
            verifyNoInteractions(installmentPort, transactionPort);
        }

        @Test
        void shouldNotDisburseMoreThanThePoolHas() {
            givenMember(adminId, MemberRole.ADMIN);
            givenLoan(MicroLoanStatus.PENDING, new BigDecimal("1500000.00"));
            givenGroup();

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> reviewUseCase.execute(adminId, groupId, loanId, new ReviewMicroLoanRequest(true, null)));

            assertEquals("INSUFFICIENT_POOL", ex.getCode());
            verify(groupPort, never()).save(any());
        }

        @Test
        void shouldOnlyLetGroupAdminsReview() {
            givenMember(borrowerId, MemberRole.MEMBER);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> reviewUseCase.execute(borrowerId, groupId, loanId, new ReviewMicroLoanRequest(true, null)));

            assertEquals("NOT_ADMIN", ex.getCode());
            verifyNoInteractions(loanPort);
        }

        @Test
        void shouldNotReviewTheSameLoanTwice() {
            givenMember(adminId, MemberRole.ADMIN);
            givenLoan(MicroLoanStatus.DISBURSED, new BigDecimal("300000.00"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> reviewUseCase.execute(adminId, groupId, loanId, new ReviewMicroLoanRequest(true, null)));

            assertEquals("LOAN_NOT_PENDING", ex.getCode());
        }

        @Test
        void shouldNotLetAdminOfOneGroupReviewLoansOfAnother() {
            givenMember(adminId, MemberRole.ADMIN);
            when(loanPort.findById(loanId)).thenReturn(Optional.of(loan(UUID.randomUUID(), MicroLoanStatus.PENDING, BigDecimal.TEN)));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> reviewUseCase.execute(adminId, groupId, loanId, new ReviewMicroLoanRequest(true, null)));

            assertEquals("LOAN_GROUP_MISMATCH", ex.getCode());
        }
    }

    @Nested
    class Repaying {

        private RepayInstallmentUseCase repayUseCase;
        private final UUID installmentId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            repayUseCase = new RepayInstallmentUseCase(authorizationService, installmentPort, loanPort, groupPort,
                    transactionPort, debitSourcePort, poolCachePort, mapper);
        }

        @Test
        void shouldChargeTheBorrowerAndReturnTheMoneyToThePool() {
            givenRepayableInstallment(InstallmentStatus.PENDING);
            when(debitSourcePort.debit(borrowerId, new BigDecimal("52000.00"), "SOLID-REPAY-" + installmentId)).thenReturn(true);
            when(installmentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            givenGroup();

            LoanInstallmentResponse response = repayUseCase.execute(borrowerId, groupId, loanId, installmentId);

            assertEquals(InstallmentStatus.PAID, response.status());
            ArgumentCaptor<SolidarityGroup> group = ArgumentCaptor.forClass(SolidarityGroup.class);
            verify(groupPort).save(group.capture());
            assertEquals(new BigDecimal("1052000.00"), group.getValue().getPoolBalance());
            assertEquals(PoolTransactionType.LOAN_REPAYMENT, poolTransaction().getType());
            verify(loanPort, never()).save(any());
        }

        @Test
        void shouldCloseTheLoanWithTheLastInstallment() {
            givenRepayableInstallment(InstallmentStatus.PENDING);
            when(debitSourcePort.debit(eq(borrowerId), any(), any())).thenReturn(true);
            when(installmentPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            givenGroup();
            when(installmentPort.allPaid(loanId)).thenReturn(true);

            repayUseCase.execute(borrowerId, groupId, loanId, installmentId);

            ArgumentCaptor<MicroLoan> loan = ArgumentCaptor.forClass(MicroLoan.class);
            verify(loanPort).save(loan.capture());
            assertEquals(MicroLoanStatus.REPAID, loan.getValue().getStatus());
        }

        @Test
        void shouldNotMarkPaidWhenTheBorrowerHasNoFunds() {
            givenRepayableInstallment(InstallmentStatus.PENDING);
            when(debitSourcePort.debit(eq(borrowerId), any(), any())).thenReturn(false);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> repayUseCase.execute(borrowerId, groupId, loanId, installmentId));

            assertEquals("INSUFFICIENT_FUNDS", ex.getCode());
            verify(installmentPort, never()).save(any());
            verify(groupPort, never()).save(any());
        }

        @Test
        void shouldNotChargeTwiceForTheSameInstallment() {
            givenRepayableInstallment(InstallmentStatus.PAID);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> repayUseCase.execute(borrowerId, groupId, loanId, installmentId));

            assertEquals("ALREADY_PAID", ex.getCode());
            verifyNoInteractions(debitSourcePort);
        }

        @Test
        void shouldOnlyLetTheBorrowerPay() {
            UUID otherMember = UUID.randomUUID();
            givenMember(otherMember, MemberRole.MEMBER);
            when(loanPort.findById(loanId)).thenReturn(Optional.of(loan(groupId, MicroLoanStatus.DISBURSED, new BigDecimal("300000.00"))));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> repayUseCase.execute(otherMember, groupId, loanId, installmentId));

            assertEquals("NOT_BORROWER", ex.getCode());
            verifyNoInteractions(debitSourcePort);
        }

        @Test
        void shouldRejectInstallmentOfAnotherLoan() {
            givenMember(borrowerId, MemberRole.MEMBER);
            when(loanPort.findById(loanId)).thenReturn(Optional.of(loan(groupId, MicroLoanStatus.DISBURSED, new BigDecimal("300000.00"))));
            when(installmentPort.findById(installmentId)).thenReturn(Optional.of(installment(UUID.randomUUID(), InstallmentStatus.PENDING)));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> repayUseCase.execute(borrowerId, groupId, loanId, installmentId));

            assertEquals("INSTALLMENT_MISMATCH", ex.getCode());
        }

        @Test
        void shouldRejectPaymentsBeforeDisbursement() {
            givenMember(borrowerId, MemberRole.MEMBER);
            when(loanPort.findById(loanId)).thenReturn(Optional.of(loan(groupId, MicroLoanStatus.PENDING, new BigDecimal("300000.00"))));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> repayUseCase.execute(borrowerId, groupId, loanId, installmentId));

            assertEquals("LOAN_NOT_DISBURSED", ex.getCode());
        }

        private void givenRepayableInstallment(InstallmentStatus status) {
            givenMember(borrowerId, MemberRole.MEMBER);
            when(loanPort.findById(loanId)).thenReturn(Optional.of(loan(groupId, MicroLoanStatus.DISBURSED, new BigDecimal("300000.00"))));
            when(installmentPort.findById(installmentId)).thenReturn(Optional.of(installment(loanId, status)));
        }

        private LoanInstallment installment(UUID ofLoan, InstallmentStatus status) {
            return LoanInstallment.builder()
                    .id(installmentId)
                    .loanId(ofLoan)
                    .installmentNumber(1)
                    .principalAmount(new BigDecimal("50000.00"))
                    .interestAmount(new BigDecimal("2000.00"))
                    .totalAmount(new BigDecimal("52000.00"))
                    .dueDate(LocalDate.now().plusMonths(1))
                    .status(status)
                    .build();
        }
    }

    @Nested
    class ListingInstallments {

        @Test
        void shouldHideLoansOfOtherGroups() {
            ListLoanInstallmentsUseCase listUseCase = new ListLoanInstallmentsUseCase(authorizationService, loanPort, installmentPort, mapper);
            givenMember(borrowerId, MemberRole.MEMBER);
            when(loanPort.findById(loanId)).thenReturn(Optional.of(loan(UUID.randomUUID(), MicroLoanStatus.DISBURSED, BigDecimal.TEN)));

            assertThrows(ResourceNotFoundException.class, () -> listUseCase.execute(borrowerId, groupId, loanId));

            verifyNoInteractions(installmentPort);
        }

        @Test
        void shouldListTheInstallmentsOfTheLoan() {
            ListLoanInstallmentsUseCase listUseCase = new ListLoanInstallmentsUseCase(authorizationService, loanPort, installmentPort, mapper);
            givenMember(borrowerId, MemberRole.MEMBER);
            when(loanPort.findById(loanId)).thenReturn(Optional.of(loan(groupId, MicroLoanStatus.DISBURSED, BigDecimal.TEN)));
            when(installmentPort.findByLoanId(loanId)).thenReturn(List.of(
                    LoanInstallment.builder().id(UUID.randomUUID()).loanId(loanId).installmentNumber(1).status(InstallmentStatus.PAID).build(),
                    LoanInstallment.builder().id(UUID.randomUUID()).loanId(loanId).installmentNumber(2).status(InstallmentStatus.PENDING).build()));

            List<LoanInstallmentResponse> installments = listUseCase.execute(borrowerId, groupId, loanId);

            assertEquals(List.of(1, 2), installments.stream().map(LoanInstallmentResponse::installmentNumber).toList());
        }
    }

    private void givenGroup() {
        when(groupPort.findById(groupId)).thenReturn(Optional.of(SolidarityGroup.builder()
                .id(groupId)
                .name("Tenderos del barrio")
                .inviteCode("ABCD1234")
                .minContribution(new BigDecimal("5000.00"))
                .maxLoanPercentage(new BigDecimal("30"))
                .interestRate(new BigDecimal("0.0200"))
                .poolBalance(POOL)
                .maxMembers(20)
                .status(GroupStatus.ACTIVE)
                .createdAt(Instant.now())
                .build()));
    }

    private void givenMember(UUID userId, MemberRole role) {
        when(memberPort.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(SolidarityMember.builder()
                .id(UUID.randomUUID()).groupId(groupId).userId(userId).role(role).totalContributed(BigDecimal.ZERO).build()));
    }

    private void givenLoan(MicroLoanStatus status, BigDecimal amount) {
        when(loanPort.findById(loanId)).thenReturn(Optional.of(loan(groupId, status, amount)));
    }

    private MicroLoan loan(UUID ofGroup, MicroLoanStatus status, BigDecimal amount) {
        return MicroLoan.builder()
                .id(loanId)
                .groupId(ofGroup)
                .borrowerId(borrowerId)
                .amount(amount)
                .purpose("Insumos")
                .termMonths(6)
                .interestRate(new BigDecimal("0.0200"))
                .status(status)
                .requestedAt(Instant.now())
                .build();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<LoanInstallment>> installmentPlan() {
        ArgumentCaptor<List<LoanInstallment>> captor = ArgumentCaptor.forClass(List.class);
        verify(installmentPort).saveAll(captor.capture());
        return captor;
    }

    private PoolTransaction poolTransaction() {
        ArgumentCaptor<PoolTransaction> captor = ArgumentCaptor.forClass(PoolTransaction.class);
        verify(transactionPort).save(captor.capture());
        return captor.getValue();
    }
}
