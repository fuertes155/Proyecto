package com.cooperativa.met.application.identity.service;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.notification.port.EmailSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String DOCUMENT = "1086578123";
    private static final String EMAIL = "socio@example.com";
    private static final String PIN_OTP_KEY = "pin-recovery-otp:" + DOCUMENT;
    private static final String EMAIL_OTP_KEY = "email-verification-otp:" + DOCUMENT;
    private static final String ATTEMPTS_KEY = "otp-attempts:" + DOCUMENT;
    private static final String LOCKOUT_KEY = "otp-lockout:" + DOCUMENT;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private EmailSenderPort emailSender;

    private OtpService otpService;

    // Redis en memoria: así los tests prueban el comportamiento completo (un código usado ya no
    // sirve, el tercer intento fallido bloquea) en vez de solo qué llamadas se hicieron.
    private final Map<String, String> redis = new HashMap<>();
    private final Map<String, Duration> ttls = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenAnswer(inv -> redis.get(inv.<String>getArgument(0)));
        lenient().doAnswer(inv -> {
            redis.put(inv.getArgument(0), inv.getArgument(1));
            ttls.put(inv.getArgument(0), inv.getArgument(2));
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));
        lenient().when(valueOps.increment(anyString())).thenAnswer(inv -> {
            long next = Long.parseLong(redis.getOrDefault(inv.<String>getArgument(0), "0")) + 1;
            redis.put(inv.getArgument(0), String.valueOf(next));
            return next;
        });
        lenient().when(redisTemplate.delete(anyString())).thenAnswer(inv -> redis.remove(inv.<String>getArgument(0)) != null);
        lenient().when(redisTemplate.hasKey(anyString())).thenAnswer(inv -> redis.containsKey(inv.<String>getArgument(0)));

        otpService = new OtpService(redisTemplate, emailSender);
        ReflectionTestUtils.setField(otpService, "logOtpOnFailure", false);
    }

    @Test
    void shouldStoreSixDigitCodeForFiveMinutesAndEmailIt() {
        String code = otpService.generateAndSendOtp(DOCUMENT, EMAIL);

        assertTrue(code.matches("\\d{6}"), "el OTP debe tener 6 dígitos: " + code);
        assertEquals(code, redis.get(PIN_OTP_KEY));
        assertEquals(Duration.ofMinutes(5), ttls.get(PIN_OTP_KEY));
        verify(emailSender).sendPlainText(eq(EMAIL), anyString(), contains(code));
    }

    @Test
    void shouldAcceptCorrectCodeOnlyOnce() {
        String code = otpService.generateAndSendOtp(DOCUMENT, EMAIL);

        assertTrue(otpService.validateOtp(DOCUMENT, code));
        assertFalse(otpService.validateOtp(DOCUMENT, code), "un OTP ya usado no puede volver a servir");
    }

    @Test
    void shouldRejectWhenNoCodeWasIssuedOrItExpired() {
        assertFalse(otpService.validateOtp(DOCUMENT, "123456"));

        // Sin código vigente no hay nada que adivinar: no se cuenta como intento fallido.
        assertFalse(redis.containsKey(ATTEMPTS_KEY));
    }

    @Test
    void shouldLockOutForFifteenMinutesOnThirdWrongCode() {
        String code = otpService.generateAndSendOtp(DOCUMENT, EMAIL);
        String wrong = wrongCode(code);

        assertFalse(otpService.validateOtp(DOCUMENT, wrong));
        assertFalse(otpService.validateOtp(DOCUMENT, wrong));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> otpService.validateOtp(DOCUMENT, wrong));

        assertEquals("TOO_MANY_OTP_ATTEMPTS", ex.getCode());
        assertEquals(Duration.ofMinutes(15), ttls.get(LOCKOUT_KEY));
        assertFalse(redis.containsKey(PIN_OTP_KEY), "el código adivinado a medias queda invalidado");
    }

    @Test
    void shouldRejectEvenTheCorrectCodeWhileLockedOut() {
        String code = otpService.generateAndSendOtp(DOCUMENT, EMAIL);
        redis.put(LOCKOUT_KEY, "LOCKED");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> otpService.validateOtp(DOCUMENT, code));

        assertEquals("OTP_LOCKED", ex.getCode());
        assertEquals(code, redis.get(PIN_OTP_KEY), "un intento bloqueado no consume el código");
    }

    @Test
    void shouldNotIssueNewCodeWhileLockedOut() {
        redis.put(LOCKOUT_KEY, "LOCKED");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> otpService.generateAndSendOtp(DOCUMENT, EMAIL));

        assertEquals("OTP_LOCKED", ex.getCode());
        assertFalse(redis.containsKey(PIN_OTP_KEY));
        verifyNoInteractions(emailSender);
    }

    @Test
    void shouldReportDeliveryFailureInsteadOfPretendingTheCodeWasSent() {
        doThrow(new EmailSenderPort.EmailDeliveryException("SMTP caído"))
                .when(emailSender).sendPlainText(anyString(), anyString(), anyString());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> otpService.generateAndSendOtp(DOCUMENT, EMAIL));

        assertEquals("EMAIL_DELIVERY_FAILED", ex.getCode());
    }

    @Test
    void shouldStillReturnCodeWhenEmailFailsWithDevFallbackEnabled() {
        ReflectionTestUtils.setField(otpService, "logOtpOnFailure", true);
        doThrow(new EmailSenderPort.EmailDeliveryException("SMTP caído"))
                .when(emailSender).sendPlainText(anyString(), anyString(), anyString());

        String code = otpService.generateAndSendOtp(DOCUMENT, EMAIL);

        assertTrue(otpService.validateOtp(DOCUMENT, code));
    }

    @Test
    void shouldAcceptEmailVerificationCodeOnlyOnce() {
        String code = otpService.generateAndSendEmailVerificationOtp(DOCUMENT, EMAIL);

        assertEquals(Duration.ofMinutes(5), ttls.get(EMAIL_OTP_KEY));
        verify(emailSender).sendPlainText(eq(EMAIL), anyString(), contains(code));
        assertTrue(otpService.validateEmailVerificationOtp(DOCUMENT, code));
        assertFalse(otpService.validateEmailVerificationOtp(DOCUMENT, code));
    }

    @Test
    void shouldNotAcceptEmailVerificationCodeForPinRecovery() {
        String emailCode = otpService.generateAndSendEmailVerificationOtp(DOCUMENT, EMAIL);

        assertFalse(otpService.validateOtp(DOCUMENT, emailCode));
    }

    @Test
    void shouldShareLockoutBetweenEmailVerificationAndPinRecovery() {
        String code = otpService.generateAndSendEmailVerificationOtp(DOCUMENT, EMAIL);
        String wrong = wrongCode(code);
        otpService.validateEmailVerificationOtp(DOCUMENT, wrong);
        otpService.validateEmailVerificationOtp(DOCUMENT, wrong);
        assertThrows(BusinessRuleException.class, () -> otpService.validateEmailVerificationOtp(DOCUMENT, wrong));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> otpService.generateAndSendOtp(DOCUMENT, EMAIL));

        assertEquals("OTP_LOCKED", ex.getCode());
    }

    private String wrongCode(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }
}
