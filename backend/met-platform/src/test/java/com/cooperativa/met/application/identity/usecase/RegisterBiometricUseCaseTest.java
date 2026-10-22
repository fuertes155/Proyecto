package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.compliance.service.ComplianceBlockService;
import com.cooperativa.met.application.identity.dto.BiometricRegistrationRequest;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.BiometricRegistration;
import com.cooperativa.met.domain.identity.model.ComplianceListType;
import com.cooperativa.met.domain.identity.model.ComplianceResult;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.BiometricRegistrationPort;
import com.cooperativa.met.domain.identity.port.ComplianceCheckPort;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterBiometricUseCaseTest {

    private static final String JPEG = base64(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10});
    private static final String PNG = base64(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
    private static final String PDF = base64("%PDF-1.7 documento".getBytes(StandardCharsets.US_ASCII));

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private BiometricRegistrationPort biometricRegistrationPort;
    @Mock
    private ComplianceCheckPort complianceCheckPort;
    @Mock
    private ComplianceBlockService complianceBlockService;
    @Mock
    private EncryptionPort encryptionPort;
    @Mock
    private CoreAccountRepositoryPort coreAccountRepository;

    @InjectMocks
    private RegisterBiometricUseCase registerBiometricUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldApproveKycAndOpenAnEmptyAccountWhenEverythingChecksOut() {
        givenUser(KycStatus.PENDING);
        when(complianceCheckPort.checkUser(eq(userId), any())).thenReturn(ComplianceResult.CLEAR);
        when(encryptionPort.encrypt(anyString())).thenAnswer(inv -> "enc:" + inv.getArgument(0));

        registerBiometricUseCase.execute(userId, new BiometricRegistrationRequest(JPEG, PNG, JPEG, PNG));

        ArgumentCaptor<BiometricRegistration> registration = ArgumentCaptor.forClass(BiometricRegistration.class);
        verify(biometricRegistrationPort).save(registration.capture());
        assertTrue(registration.getValue().isVerified());
        assertEquals("enc:" + JPEG, registration.getValue().getSelfieImage(), "las fotos se guardan cifradas");

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertEquals(KycStatus.APPROVED, user.getValue().getKycStatus());
        assertEquals(UserStatus.ACTIVE, user.getValue().getStatus());

        ArgumentCaptor<CoreAccount> account = ArgumentCaptor.forClass(CoreAccount.class);
        verify(coreAccountRepository).save(account.capture());
        assertEquals(userId, account.getValue().getUserId());
        assertTrue(account.getValue().getAccountNumber().matches("10\\d{8}"), account.getValue().getAccountNumber());
        assertEquals(0, account.getValue().getPrincipalBalance().compareTo(BigDecimal.ZERO));
        assertEquals(AccountStatus.ACTIVE, account.getValue().getStatus());

        for (ComplianceListType listType : ComplianceListType.values()) {
            verify(complianceCheckPort).persistCheck(userId, listType, ComplianceResult.CLEAR, null);
        }
    }

    @Test
    void shouldBlockUserAndStopOnRestrictiveListMatch() {
        givenUser(KycStatus.PENDING);
        when(complianceCheckPort.checkUser(userId, ComplianceListType.SARLAFT)).thenReturn(ComplianceResult.MATCH);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> registerBiometricUseCase.execute(userId, new BiometricRegistrationRequest(JPEG, JPEG, JPEG, JPEG)));

        assertEquals("COMPLIANCE_MATCH", ex.getCode());
        verify(complianceBlockService).blockUser(userId);
        verify(biometricRegistrationPort, never()).save(any());
        verify(coreAccountRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectPdfDisguisedAsPhoto() {
        givenUser(KycStatus.PENDING);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> registerBiometricUseCase.execute(userId, new BiometricRegistrationRequest(JPEG, PDF, JPEG, JPEG)));

        assertEquals("INVALID_IMAGE_FORMAT", ex.getCode());
        assertTrue(ex.getMessage().contains("reverso"), ex.getMessage());
        verifyNoInteractions(complianceCheckPort, biometricRegistrationPort, coreAccountRepository);
    }

    @Test
    void shouldRejectTextThatIsNotBase64() {
        givenUser(KycStatus.PENDING);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> registerBiometricUseCase.execute(userId, new BiometricRegistrationRequest("no es base64!!", JPEG, JPEG, JPEG)));

        assertEquals("INVALID_IMAGE_FORMAT", ex.getCode());
        verifyNoInteractions(complianceCheckPort);
    }

    @Test
    void shouldNotRunKycTwice() {
        givenUser(KycStatus.APPROVED);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> registerBiometricUseCase.execute(userId, new BiometricRegistrationRequest(JPEG, JPEG, JPEG, JPEG)));

        assertEquals("KYC_ALREADY_APPROVED", ex.getCode());
        verifyNoInteractions(complianceCheckPort, coreAccountRepository);
    }

    private void givenUser(KycStatus kycStatus) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder()
                .id(userId)
                .status(UserStatus.ACTIVE)
                .kycStatus(kycStatus)
                .build()));
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
