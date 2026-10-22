package com.cooperativa.met.infrastructure.web;

import com.cooperativa.met.application.bank.dto.ExecutePayoutRequest;
import com.cooperativa.met.application.bank.dto.ExternalBankAccountResponse;
import com.cooperativa.met.application.bank.dto.RegisterExternalBankAccountRequest;
import com.cooperativa.met.application.bank.usecase.ConfirmBankAccountVerificationUseCase;
import com.cooperativa.met.application.bank.usecase.ExecuteExternalPayoutUseCase;
import com.cooperativa.met.application.bank.usecase.InitiateBankAccountVerificationUseCase;
import com.cooperativa.met.application.bank.usecase.ListMyExternalBankAccountsUseCase;
import com.cooperativa.met.application.bank.usecase.RegisterExternalBankAccountUseCase;
import com.cooperativa.met.application.legal.usecase.ConfirmMandateSignatureUseCase;
import com.cooperativa.met.application.legal.usecase.RequestMandateSignatureUseCase;
import com.cooperativa.met.application.report.dto.ReportFileResult;
import com.cooperativa.met.application.report.usecase.ExportUserReportUseCase;
import com.cooperativa.met.domain.bank.model.BankAccountType;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.legal.model.MandateContract;
import com.cooperativa.met.infrastructure.config.RateLimitProperties;
import com.cooperativa.met.infrastructure.web.bank.ExternalBankAccountController;
import com.cooperativa.met.infrastructure.web.bank.PayoutController;
import com.cooperativa.met.infrastructure.web.common.GlobalExceptionHandler;
import com.cooperativa.met.infrastructure.web.legal.LegalController;
import com.cooperativa.met.infrastructure.web.report.ReportController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controladores del socio con MockMvc "standalone": mapeo HTTP, validación y JSON reales, sin levantar
 * el contexto de Spring. La autenticación se pone como la deja JwtAuthenticationFilter (principal = UUID).
 */
@ExtendWith(MockitoExtension.class)
class UserFacingControllersTest {

    @Mock private RegisterExternalBankAccountUseCase registerUseCase;
    @Mock private ListMyExternalBankAccountsUseCase listUseCase;
    @Mock private InitiateBankAccountVerificationUseCase initiateUseCase;
    @Mock private ConfirmBankAccountVerificationUseCase confirmUseCase;
    @Mock private ExecuteExternalPayoutUseCase payoutUseCase;
    @Mock private RequestMandateSignatureUseCase requestMandateUseCase;
    @Mock private ConfirmMandateSignatureUseCase confirmMandateUseCase;
    @Mock private ExportUserReportUseCase exportUseCase;

    private MockMvc mvc;
    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new ExternalBankAccountController(registerUseCase, listUseCase, initiateUseCase, confirmUseCase),
                        new PayoutController(payoutUseCase),
                        new LegalController(requestMandateUseCase, confirmMandateUseCase),
                        new ReportController(exportUseCase))
                .setControllerAdvice(new GlobalExceptionHandler(new RateLimitProperties()))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldListOnlyTheAuthenticatedUsersBankAccounts() throws Exception {
        when(listUseCase.execute(userId)).thenReturn(List.of(ExternalBankAccountResponse.builder()
                .id(accountId).bankCode("1007").bankName("Bancolombia").accountType(BankAccountType.SAVINGS)
                .maskedAccountNumber("****6789").verificationStatus(BankAccountVerificationStatus.PENDING)
                .verificationPending(true).createdAt(Instant.now()).build()));

        mvc.perform(get("/v1/accounts/external-bank-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].maskedAccountNumber").value("****6789"))
                .andExpect(jsonPath("$[0].bankName").value("Bancolombia"));
    }

    @Test
    void shouldRegisterAValidBankAccountForTheAuthenticatedUser() throws Exception {
        mvc.perform(post("/v1/accounts/external-bank-accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankCode\":\"1007\",\"accountType\":\"SAVINGS\",\"accountNumber\":\"00123456789\"}"))
                .andExpect(status().isOk());

        verify(registerUseCase).execute(userId, new RegisterExternalBankAccountRequest("1007", BankAccountType.SAVINGS, "00123456789"));
    }

    @Test
    void shouldRejectAccountNumbersWithLettersBeforeCallingTheUseCase() throws Exception {
        mvc.perform(post("/v1/accounts/external-bank-accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bankCode\":\"1007\",\"accountType\":\"SAVINGS\",\"accountNumber\":\"12AB\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.accountNumber").exists());

        verifyNoInteractions(registerUseCase);
    }

    @Test
    void shouldReturnTheBusinessErrorCodeWhenTheVerificationAmountIsWrong() throws Exception {
        doThrow(new BusinessRuleException("INCORRECT_AMOUNT", "El monto no coincide"))
                .when(confirmUseCase).execute(userId, accountId, 2730);

        mvc.perform(post("/v1/accounts/external-bank-accounts/{id}/verify", accountId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":2730}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INCORRECT_AMOUNT"));
    }

    @Test
    void shouldResendTheVerificationDeposit() throws Exception {
        mvc.perform(post("/v1/accounts/external-bank-accounts/{id}/verify/resend", accountId)).andExpect(status().isOk());

        verify(initiateUseCase).execute(userId, accountId);
    }

    @Test
    void shouldPassTheClientIpToThePayoutForFraudChecks() throws Exception {
        String body = "{\"externalBankAccountId\":\"" + accountId + "\",\"amount\":150000,\"pin\":\"rsa\",\"otp\":\"482913\",\"idempotencyKey\":\"k1\"}";

        mvc.perform(post("/v1/accounts/transactions/payout").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-Forwarded-For", "190.85.10.4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Retiro iniciado con éxito"));
        mvc.perform(post("/v1/accounts/transactions/payout").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(request -> { request.setRemoteAddr("181.50.10.2"); return request; }))
                .andExpect(status().isOk());

        verify(payoutUseCase).execute(eq(userId), any(ExecutePayoutRequest.class), eq("190.85.10.4"));
        verify(payoutUseCase).execute(eq(userId), any(ExecutePayoutRequest.class), eq("181.50.10.2"));
    }

    @Test
    void shouldRequireAnIdempotencyKeyForPayouts() throws Exception {
        mvc.perform(post("/v1/accounts/transactions/payout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalBankAccountId\":\"" + accountId + "\",\"amount\":150000,\"pin\":\"rsa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.idempotencyKey").exists());

        verifyNoInteractions(payoutUseCase);
    }

    @Test
    void shouldSignTheMandateRecordingIpAndUserAgent() throws Exception {
        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
        when(requestMandateUseCase.execute(userId)).thenReturn("tx_1");
        UUID contractId = UUID.randomUUID();
        when(confirmMandateUseCase.execute(userId, "482913", "190.85.10.4", "MET/2.3", "tx_1"))
                .thenReturn(MandateContract.builder().id(contractId).pdfHashSha256("abc123").build());

        mvc.perform(post("/v1/legal/mandate/request-signature").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx_1"));
        mvc.perform(post("/v1/legal/mandate/confirm-signature").principal(principal)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"otpCode\":\"482913\",\"transactionId\":\"tx_1\"}")
                        .header("X-Forwarded-For", "190.85.10.4, 10.0.0.1").header("User-Agent", "MET/2.3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractId").value(contractId.toString()))
                .andExpect(jsonPath("$.hashSha256").value("abc123"));
    }

    @Test
    void shouldDownloadTheReportAsAnAttachment() throws Exception {
        byte[] pdf = "%PDF-1.7".getBytes();
        when(exportUseCase.execute(userId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "pdf"))
                .thenReturn(new ReportFileResult("reporte.pdf", pdf, "application/pdf"));

        mvc.perform(get("/v1/reports/export").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"reporte.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void shouldNotServeAnythingWithoutAnAuthenticatedUser() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/v1/accounts/external-bank-accounts")).andExpect(status().isInternalServerError());

        verifyNoInteractions(listUseCase);
    }

    @Test
    void shouldAcceptPrincipalsGivenAsStringsToo() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
        when(listUseCase.execute(userId)).thenReturn(List.of());

        mvc.perform(get("/v1/accounts/external-bank-accounts")).andExpect(status().isOk());

        verify(listUseCase).execute(userId);
    }
}
