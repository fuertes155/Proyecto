package com.cooperativa.met.application.legal.usecase;

import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.application.legal.service.PdfGeneratorService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.investment.service.LegalSignatureService;
import com.cooperativa.met.domain.legal.model.MandateContract;
import com.cooperativa.met.domain.legal.model.MandateContractStatus;
import com.cooperativa.met.domain.legal.port.MandateContractRepositoryPort;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Firma electrónica del mandato: OTP, PDF con los metadatos legales y su huella SHA-256. */
@ExtendWith(MockitoExtension.class)
class MandateSignatureUseCasesTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private OtpService otpService;
    @Mock private MandateContractRepositoryPort mandateRepository;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldSendTheSigningCodeToTheUsersEmail() {
        givenUser();

        String transactionId = new RequestMandateSignatureUseCase(userRepository, otpService).execute(userId);

        assertTrue(transactionId.startsWith("tx_"));
        verify(otpService).generateAndSendOtp("1086578123", "socio@example.com");
    }

    @Test
    void shouldStoreASignedPdfWithItsHashAndTheLegalEvidence() throws Exception {
        givenUser();
        when(otpService.validateOtp("1086578123", "482913")).thenReturn(true);
        when(mandateRepository.findByUserId(userId)).thenReturn(Optional.empty());

        MandateContract contract = confirm().execute(userId, "482913", "190.85.10.4", "MET/2.3 Android", "tx_1");

        assertEquals(MandateContractStatus.SIGNED, contract.getStatus());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contract.getPdfContent())),
                contract.getPdfHashSha256(), "la huella permite probar que el PDF no se alteró después de firmar");
        String pdf = pdfText(contract.getPdfContent());
        assertTrue(pdf.contains("Ana Pérez"), pdf);
        assertTrue(pdf.contains("1086578123"), pdf);
        assertTrue(pdf.contains("190.85.10.4"), pdf);
        assertEquals("MET/2.3 Android", contract.getUserAgent());
        verify(mandateRepository).save(contract);
    }

    @Test
    void shouldNotSignWithAWrongCode() {
        givenUser();
        when(otpService.validateOtp("1086578123", "000000")).thenReturn(false);

        assertEquals("LEGAL_01", assertThrows(BusinessRuleException.class,
                () -> confirm().execute(userId, "000000", "ip", "ua", "tx")).getCode());
        verify(mandateRepository, never()).save(any());
    }

    @Test
    void shouldNotSignTwice() {
        givenUser();
        when(otpService.validateOtp("1086578123", "482913")).thenReturn(true);
        when(mandateRepository.findByUserId(userId)).thenReturn(Optional.of(MandateContract.builder().id(UUID.randomUUID()).build()));

        assertEquals("LEGAL_02", assertThrows(BusinessRuleException.class,
                () -> confirm().execute(userId, "482913", "ip", "ua", "tx")).getCode());
        verify(mandateRepository, never()).save(any());
    }

    @Test
    void shouldFailForUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertEquals("USR_01", assertThrows(BusinessRuleException.class,
                () -> new RequestMandateSignatureUseCase(userRepository, otpService).execute(userId)).getCode());
    }

    @Test
    void shouldBurnTheSmsOtpOfTheLegacySignatureServiceAfterOneUse() throws Exception {
        LegalSignatureService legacy = new LegalSignatureService();
        String transactionId = legacy.generateAndSendOtp("3001234567");
        @SuppressWarnings("unchecked")
        String otp = ((java.util.Map<String, String>) org.springframework.test.util.ReflectionTestUtils
                .getField(legacy, "otpCache")).get(transactionId);

        assertFalse(legacy.validateOtp(transactionId, "no-es"));
        assertTrue(legacy.validateOtp(transactionId, otp));
        assertFalse(legacy.validateOtp(transactionId, otp), "un OTP de firma solo sirve una vez");

        byte[] pdf = legacy.generateMandatePdf("Ana", "$100.000", "2026-10-21");
        assertTrue(new String(pdf, StandardCharsets.UTF_8).contains("Usuario: Ana"));
        assertEquals(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(pdf)), legacy.calculateSha256(pdf));
    }

    private ConfirmMandateSignatureUseCase confirm() {
        return new ConfirmMandateSignatureUseCase(userRepository, otpService, new PdfGeneratorService(), mandateRepository);
    }

    private void givenUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId).firstName("Ana").lastName("Pérez").documentNumber("1086578123").email("socio@example.com").build()));
    }

    private static String pdfText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }
}
