package com.cooperativa.met.infrastructure.web.solidarity;

import com.cooperativa.met.application.solidarity.dto.ContributeToPoolRequest;
import com.cooperativa.met.application.solidarity.dto.CreateSolidarityGroupRequest;
import com.cooperativa.met.application.solidarity.dto.JoinSolidarityGroupRequest;
import com.cooperativa.met.application.solidarity.dto.LoanInstallmentResponse;
import com.cooperativa.met.application.solidarity.dto.MicroLoanResponse;
import com.cooperativa.met.application.solidarity.dto.PoolTransactionResponse;
import com.cooperativa.met.application.solidarity.dto.RequestMicroLoanRequest;
import com.cooperativa.met.application.solidarity.dto.ReviewMicroLoanRequest;
import com.cooperativa.met.application.solidarity.dto.SolidarityGroupResponse;
import com.cooperativa.met.application.solidarity.dto.SolidarityMemberResponse;
import com.cooperativa.met.application.solidarity.usecase.ContributeToPoolUseCase;
import com.cooperativa.met.application.solidarity.usecase.CreateSolidarityGroupUseCase;
import com.cooperativa.met.application.solidarity.usecase.GetSolidarityGroupUseCase;
import com.cooperativa.met.application.solidarity.usecase.JoinSolidarityGroupUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListGroupLoansUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListGroupMembersUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListLoanInstallmentsUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListMySolidarityGroupsUseCase;
import com.cooperativa.met.application.solidarity.usecase.ListPoolTransactionsUseCase;
import com.cooperativa.met.application.solidarity.usecase.RepayInstallmentUseCase;
import com.cooperativa.met.application.solidarity.usecase.RequestMicroLoanUseCase;
import com.cooperativa.met.application.solidarity.usecase.ReviewMicroLoanUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/solidarity")
@RequiredArgsConstructor
public class SolidarityController {

    private final CreateSolidarityGroupUseCase createGroupUseCase;
    private final JoinSolidarityGroupUseCase joinGroupUseCase;
    private final ListMySolidarityGroupsUseCase listGroupsUseCase;
    private final GetSolidarityGroupUseCase getGroupUseCase;
    private final ListGroupMembersUseCase listMembersUseCase;
    private final ContributeToPoolUseCase contributeUseCase;
    private final RequestMicroLoanUseCase requestLoanUseCase;
    private final ReviewMicroLoanUseCase reviewLoanUseCase;
    private final RepayInstallmentUseCase repayInstallmentUseCase;
    private final ListGroupLoansUseCase listLoansUseCase;
    private final ListLoanInstallmentsUseCase listInstallmentsUseCase;
    private final ListPoolTransactionsUseCase listTransactionsUseCase;

    @PostMapping("/groups")
    public ResponseEntity<SolidarityGroupResponse> createGroup(
            Authentication authentication,
            @Valid @RequestBody CreateSolidarityGroupRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(createGroupUseCase.execute(userId, request));
    }

    @PostMapping("/groups/join")
    public ResponseEntity<SolidarityGroupResponse> joinGroup(
            Authentication authentication,
            @Valid @RequestBody JoinSolidarityGroupRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(joinGroupUseCase.execute(userId, request));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<SolidarityGroupResponse>> listGroups(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(listGroupsUseCase.execute(userId));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<SolidarityGroupResponse> getGroup(
            Authentication authentication,
            @PathVariable UUID groupId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(getGroupUseCase.execute(userId, groupId));
    }

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<List<SolidarityMemberResponse>> listMembers(
            Authentication authentication,
            @PathVariable UUID groupId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(listMembersUseCase.execute(userId, groupId));
    }

    @PostMapping("/groups/{groupId}/contributions")
    public ResponseEntity<SolidarityGroupResponse> contribute(
            Authentication authentication,
            @PathVariable UUID groupId,
            @Valid @RequestBody ContributeToPoolRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(contributeUseCase.execute(userId, groupId, request));
    }

    @PostMapping("/groups/{groupId}/loans")
    public ResponseEntity<MicroLoanResponse> requestLoan(
            Authentication authentication,
            @PathVariable UUID groupId,
            @Valid @RequestBody RequestMicroLoanRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(requestLoanUseCase.execute(userId, groupId, request));
    }

    @PostMapping("/groups/{groupId}/loans/{loanId}/review")
    public ResponseEntity<MicroLoanResponse> reviewLoan(
            Authentication authentication,
            @PathVariable UUID groupId,
            @PathVariable UUID loanId,
            @Valid @RequestBody ReviewMicroLoanRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(reviewLoanUseCase.execute(userId, groupId, loanId, request));
    }

    @PostMapping("/groups/{groupId}/loans/{loanId}/installments/{installmentId}/pay")
    public ResponseEntity<LoanInstallmentResponse> payInstallment(
            Authentication authentication,
            @PathVariable UUID groupId,
            @PathVariable UUID loanId,
            @PathVariable UUID installmentId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(repayInstallmentUseCase.execute(userId, groupId, loanId, installmentId));
    }

    @GetMapping("/groups/{groupId}/loans")
    public ResponseEntity<List<MicroLoanResponse>> listLoans(
            Authentication authentication,
            @PathVariable UUID groupId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(listLoansUseCase.execute(userId, groupId));
    }

    @GetMapping("/groups/{groupId}/loans/{loanId}/installments")
    public ResponseEntity<List<LoanInstallmentResponse>> listInstallments(
            Authentication authentication,
            @PathVariable UUID groupId,
            @PathVariable UUID loanId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(listInstallmentsUseCase.execute(userId, groupId, loanId));
    }

    @GetMapping("/groups/{groupId}/transactions")
    public ResponseEntity<List<PoolTransactionResponse>> listTransactions(
            Authentication authentication,
            @PathVariable UUID groupId) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(listTransactionsUseCase.execute(userId, groupId));
    }
}
