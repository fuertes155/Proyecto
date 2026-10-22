package com.cooperativa.met.infrastructure.persistence;

import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.model.BankAccountType;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.model.ExternalPayout;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalPayoutRepositoryPort;
import com.cooperativa.met.domain.legal.model.MandateContract;
import com.cooperativa.met.domain.legal.model.MandateContractStatus;
import com.cooperativa.met.domain.legal.port.MandateContractRepositoryPort;
import com.cooperativa.met.domain.lending.model.AmortizationInstallment;
import com.cooperativa.met.domain.lending.model.LoanApplicationStatus;
import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;
import com.cooperativa.met.domain.lending.port.AmortizationSchedulePort;
import com.cooperativa.met.domain.lending.port.PersonalLoanApplicationPort;
import com.cooperativa.met.domain.savings.model.ContributionFrequency;
import com.cooperativa.met.domain.savings.model.ContributionStatus;
import com.cooperativa.met.domain.savings.model.ScheduledContribution;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsAccount;
import com.cooperativa.met.domain.savings.model.ScheduledSavingsStatus;
import com.cooperativa.met.domain.savings.model.SavingsWithdrawal;
import com.cooperativa.met.domain.savings.model.WithdrawalType;
import com.cooperativa.met.domain.savings.port.SavingsWithdrawalPort;
import com.cooperativa.met.domain.savings.port.ScheduledContributionPort;
import com.cooperativa.met.domain.savings.port.ScheduledSavingsAccountPort;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adaptadores de bancos, créditos, ahorros, fondos solidarios y mandatos contra H2. */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.cooperativa.met.TestRedisConfig.class)
class FinancialProductsPersistenceIntegrationTest {

    @Autowired private BankRepositoryPort bankRepository;
    @Autowired private ExternalBankAccountRepositoryPort externalBankAccountRepository;
    @Autowired private ExternalPayoutRepositoryPort payoutRepository;
    @Autowired private PersonalLoanApplicationPort loanRepository;
    @Autowired private AmortizationSchedulePort schedulePort;
    @Autowired private ScheduledSavingsAccountPort savingsRepository;
    @Autowired private ScheduledContributionPort contributionPort;
    @Autowired private SavingsWithdrawalPort withdrawalPort;
    @Autowired private SolidarityGroupPort groupPort;
    @Autowired private SolidarityMemberPort memberPort;
    @Autowired private MicroLoanPort microLoanPort;
    @Autowired private LoanInstallmentPort installmentPort;
    @Autowired private PoolTransactionPort poolTransactionPort;
    @Autowired private MandateContractRepositoryPort mandateRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 6);

    @Test
    void shouldStoreBanksAccountsAndPayouts() {
        Bank bank = bankRepository.save(Bank.builder().code("T" + suffix).name("Banco prueba").wompiBankId("1507")
                .wompiPseCode("1040").supportsPse(true).supportsPayout(true).active(true).updatedAt(Instant.now()).build());
        assertEquals("1507", bankRepository.findByCode(bank.getCode()).orElseThrow().getWompiBankId());
        assertTrue(bankRepository.findAllActive().stream().anyMatch(b -> b.getCode().equals(bank.getCode())));

        UUID userId = UUID.randomUUID();
        ExternalBankAccount account = externalBankAccountRepository.save(ExternalBankAccount.builder()
                .id(UUID.randomUUID()).userId(userId).bankCode(bank.getCode()).accountType(BankAccountType.SAVINGS)
                .accountNumber("00123456789").verificationStatus(BankAccountVerificationStatus.PENDING)
                .pendingVerificationAmount(2731).active(true).createdAt(Instant.now()).build());
        externalBankAccountRepository.save(account.toBuilder().id(UUID.randomUUID()).active(false).build());
        ExternalBankAccount found = externalBankAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals("00123456789", found.getAccountNumber(), "el número se guarda cifrado pero se lee en claro");
        assertEquals(2731, found.getPendingVerificationAmount());
        assertEquals(1, externalBankAccountRepository.findActiveByUserId(userId).size());

        UUID coreTransactionId = UUID.randomUUID();
        ExternalPayout payout = payoutRepository.save(ExternalPayout.builder()
                .id(UUID.randomUUID()).coreTransactionId(coreTransactionId).externalBankAccountId(account.getId())
                .createdAt(Instant.now()).build());
        payoutRepository.save(payout.withRailReference("rail-" + suffix));
        assertEquals(payout.getId(), payoutRepository.findByCoreTransactionId(coreTransactionId).orElseThrow().getId());
        assertEquals(payout.getId(), payoutRepository.findByRailReference("rail-" + suffix).orElseThrow().getId());
        assertEquals("rail-" + suffix, payoutRepository.findById(payout.getId()).orElseThrow().getRailReference());
    }

    @Test
    void shouldStoreLoanApplicationsAndTheirSchedule() {
        UUID userId = UUID.randomUUID();
        PersonalLoanApplication application = loanRepository.save(PersonalLoanApplication.builder()
                .id(UUID.randomUUID()).userId(userId).amount(new BigDecimal("3000000.00")).termMonths(2)
                .annualInterestRate(new BigDecimal("0.2400")).monthlyPayment(new BigDecimal("1530000.00"))
                .totalInterest(new BigDecimal("60000.00")).totalPayment(new BigDecimal("3060000.00")).purpose("Capital de trabajo")
                .status(LoanApplicationStatus.IN_REVIEW).submittedAt(Instant.now()).createdAt(Instant.now()).updatedAt(Instant.now()).build());

        assertTrue(loanRepository.hasPendingApplication(userId));
        assertEquals(1, loanRepository.findByUserId(userId).size());
        assertEquals(application.getId(), loanRepository.findByIdAndUserId(application.getId(), userId).orElseThrow().getId());
        assertTrue(loanRepository.findByIdAndUserId(application.getId(), UUID.randomUUID()).isEmpty());
        assertTrue(loanRepository.findByStatus(LoanApplicationStatus.IN_REVIEW).stream().anyMatch(a -> a.getId().equals(application.getId())));
        assertTrue(loanRepository.findAll().stream().anyMatch(a -> a.getId().equals(application.getId())));

        LocalDate today = LocalDate.now();
        schedulePort.saveAll(application.getId(), List.of(
                installment(application.getId(), 1, today.minusDays(1)), installment(application.getId(), 2, today.plusMonths(1))));
        assertEquals(List.of(1, 2), schedulePort.findByApplicationId(application.getId()).stream()
                .map(AmortizationInstallment::getInstallmentNumber).toList());
        List<AmortizationInstallment> due = schedulePort.findPendingInstallmentsByDueDateBeforeOrEqual(today);
        assertTrue(due.stream().anyMatch(i -> i.getApplicationId().equals(application.getId()) && i.getInstallmentNumber() == 1));
        assertTrue(due.stream().noneMatch(i -> i.getApplicationId().equals(application.getId()) && i.getInstallmentNumber() == 2));
    }

    @Test
    void shouldStoreScheduledSavingsTheirContributionsAndWithdrawals() {
        UUID userId = UUID.randomUUID();
        ScheduledSavingsAccount account = savingsRepository.save(ScheduledSavingsAccount.builder()
                .id(UUID.randomUUID()).userId(userId).name("Vacaciones").targetAmount(new BigDecimal("1000000.00"))
                .contributionAmount(new BigDecimal("100000.00")).frequency(ContributionFrequency.WEEKLY).debitDayOfWeek(5)
                .currentBalance(new BigDecimal("400000.00")).status(ScheduledSavingsStatus.ACTIVE)
                .nextContributionDate(LocalDate.now()).createdAt(Instant.now()).updatedAt(Instant.now()).build());

        assertTrue(savingsRepository.findDueAccounts(LocalDate.now(), ScheduledSavingsStatus.ACTIVE).stream()
                .anyMatch(a -> a.getId().equals(account.getId())));
        assertTrue(savingsRepository.findDueAccounts(LocalDate.now().minusDays(1), ScheduledSavingsStatus.ACTIVE).stream()
                .noneMatch(a -> a.getId().equals(account.getId())));
        assertEquals(account.getId(), savingsRepository.findByIdAndUserId(account.getId(), userId).orElseThrow().getId());
        assertTrue(savingsRepository.findByIdAndUserId(account.getId(), UUID.randomUUID()).isEmpty());
        assertEquals(1, savingsRepository.findByUserId(userId).size());
        assertEquals(ContributionFrequency.WEEKLY, savingsRepository.findById(account.getId()).orElseThrow().getFrequency());

        contributionPort.save(ScheduledContribution.builder()
                .id(UUID.randomUUID()).accountId(account.getId()).amount(new BigDecimal("100000.00")).scheduledDate(LocalDate.now())
                .status(ContributionStatus.COMPLETED).executedAt(Instant.now()).createdAt(Instant.now()).build());
        assertEquals(1, contributionPort.findByAccountId(account.getId()).size());

        SavingsWithdrawal withdrawal = withdrawalPort.save(SavingsWithdrawal.builder()
                .id(UUID.randomUUID()).accountId(account.getId()).userId(userId).amount(new BigDecimal("50000.00"))
                .type(WithdrawalType.PARTIAL).createdAt(Instant.now()).build());
        assertEquals(WithdrawalType.PARTIAL, withdrawal.getType());
    }

    @Test
    void shouldStoreSolidarityGroupsMembersLoansAndPoolMovements() {
        UUID creator = UUID.randomUUID();
        String inviteCode = ("G" + suffix + "XX").substring(0, 8).toUpperCase();
        SolidarityGroup group = groupPort.save(SolidarityGroup.builder()
                .id(UUID.randomUUID()).name("Tenderos").description("Grupo de prueba").creatorId(creator).inviteCode(inviteCode)
                .minContribution(new BigDecimal("5000.00")).maxLoanPercentage(new BigDecimal("30.00")).interestRate(new BigDecimal("0.0200"))
                .poolBalance(new BigDecimal("500000.00")).maxMembers(20).status(GroupStatus.ACTIVE)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build());
        assertEquals(group.getId(), groupPort.findByInviteCode(inviteCode).orElseThrow().getId());
        assertTrue(groupPort.existsByInviteCode(inviteCode));

        memberPort.save(member(group.getId(), creator, MemberRole.ADMIN));
        UUID borrower = UUID.randomUUID();
        memberPort.save(member(group.getId(), borrower, MemberRole.MEMBER));
        assertEquals(2, memberPort.countByGroupId(group.getId()));
        assertEquals(2, memberPort.findByGroupId(group.getId()).size());
        assertEquals(MemberRole.ADMIN, memberPort.findByGroupIdAndUserId(group.getId(), creator).orElseThrow().getRole());
        assertEquals(List.of(group.getId()), groupPort.findByUserId(borrower).stream().map(SolidarityGroup::getId).toList());

        MicroLoan loan = microLoanPort.save(MicroLoan.builder()
                .id(UUID.randomUUID()).groupId(group.getId()).borrowerId(borrower).amount(new BigDecimal("100000.00"))
                .purpose("Insumos").termMonths(2).interestRate(new BigDecimal("0.0200")).status(MicroLoanStatus.DISBURSED)
                .requestedAt(Instant.now()).disbursedAt(Instant.now()).build());
        assertTrue(microLoanPort.hasActiveLoan(group.getId(), borrower));
        assertFalse(microLoanPort.hasActiveLoan(group.getId(), creator));
        assertEquals(1, microLoanPort.findByGroupIdAndStatus(group.getId(), MicroLoanStatus.DISBURSED).size());
        assertEquals(1, microLoanPort.findByGroupId(group.getId()).size());

        List<LoanInstallment> plan = installmentPort.saveAll(List.of(
                loanInstallment(loan.getId(), 1, InstallmentStatus.PAID), loanInstallment(loan.getId(), 2, InstallmentStatus.PENDING)));
        assertFalse(installmentPort.allPaid(loan.getId()));
        installmentPort.save(plan.get(1).markPaid());
        assertTrue(installmentPort.allPaid(loan.getId()));
        assertEquals(2, installmentPort.findByLoanId(loan.getId()).size());
        assertEquals(InstallmentStatus.PAID, installmentPort.findById(plan.get(1).getId()).orElseThrow().getStatus());

        poolTransactionPort.save(PoolTransaction.builder()
                .id(UUID.randomUUID()).groupId(group.getId()).loanId(loan.getId()).type(PoolTransactionType.LOAN_DISBURSEMENT)
                .amount(new BigDecimal("100000.00")).balanceAfter(new BigDecimal("400000.00")).description("Desembolso")
                .createdAt(Instant.now()).build());
        assertEquals(PoolTransactionType.LOAN_DISBURSEMENT, poolTransactionPort.findByGroupId(group.getId()).get(0).getType());
        assertEquals(MicroLoanStatus.DISBURSED, microLoanPort.findById(loan.getId()).orElseThrow().getStatus());
        assertEquals(0, new BigDecimal("500000.00").compareTo(groupPort.findById(group.getId()).orElseThrow().getPoolBalance()));
    }

    @Test
    void shouldStoreTheSignedMandateWithItsPdf() {
        UUID userId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.7 mandato".getBytes(StandardCharsets.US_ASCII);
        MandateContract saved = mandateRepository.save(MandateContract.builder()
                .id(UUID.randomUUID()).userId(userId).documentNumber("1086578123").pdfContent(pdf).pdfHashSha256("hash")
                .signedAt(Instant.now()).ipAddress("190.85.10.4").userAgent("MET/1.0").otpTransactionId("otp-1")
                .status(MandateContractStatus.SIGNED).build());

        assertArrayEquals(pdf, mandateRepository.findById(saved.getId()).orElseThrow().getPdfContent());
        mandateRepository.save(saved.withStatus(MandateContractStatus.REVOKED));
        assertEquals(MandateContractStatus.REVOKED, mandateRepository.findByUserId(userId).orElseThrow().getStatus());
    }

    private AmortizationInstallment installment(UUID applicationId, int number, LocalDate dueDate) {
        return AmortizationInstallment.builder()
                .id(UUID.randomUUID()).applicationId(applicationId).installmentNumber(number)
                .paymentAmount(new BigDecimal("1530000.00")).principalAmount(new BigDecimal("1500000.00"))
                .interestAmount(new BigDecimal("30000.00")).remainingBalance(new BigDecimal("1500000.00"))
                .dueDate(dueDate).penaltyInterestAmount(BigDecimal.ZERO).status("PENDING").build();
    }

    private SolidarityMember member(UUID groupId, UUID userId, MemberRole role) {
        return SolidarityMember.builder()
                .id(UUID.randomUUID()).groupId(groupId).userId(userId).role(role).totalContributed(BigDecimal.ZERO).joinedAt(Instant.now()).build();
    }

    private LoanInstallment loanInstallment(UUID loanId, int number, InstallmentStatus status) {
        return LoanInstallment.builder()
                .id(UUID.randomUUID()).loanId(loanId).installmentNumber(number).principalAmount(new BigDecimal("50000.00"))
                .interestAmount(new BigDecimal("2000.00")).totalAmount(new BigDecimal("52000.00"))
                .dueDate(LocalDate.now().plusMonths(number)).status(status).build();
    }
}
