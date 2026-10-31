package com.cooperativa.met.infrastructure.web.identity;

import com.cooperativa.met.application.identity.dto.AuthResponse;
import com.cooperativa.met.application.identity.dto.BiometricRegistrationRequest;
import com.cooperativa.met.application.identity.dto.EnableBiometricLoginRequest;
import com.cooperativa.met.application.identity.dto.PinRecoveryRequest;
import com.cooperativa.met.application.identity.dto.PinRecoveryResetRequest;
import com.cooperativa.met.application.identity.dto.ResendEmailOtpRequest;
import com.cooperativa.met.application.identity.dto.UpdateNotificationsRequest;
import com.cooperativa.met.application.identity.dto.UpdatePinRequest;
import com.cooperativa.met.application.identity.dto.UpdateProfileRequest;
import com.cooperativa.met.application.identity.dto.UserResponse;
import com.cooperativa.met.application.identity.dto.VerifyEmailRequest;
import com.cooperativa.met.application.identity.usecase.GetPublicKeyUseCase;
import com.cooperativa.met.application.identity.usecase.GetUserProfileUseCase;
import com.cooperativa.met.application.identity.usecase.LoginUseCase;
import com.cooperativa.met.application.identity.usecase.LogoutUseCase;
import com.cooperativa.met.application.identity.usecase.ManageBiometricLoginUseCase;
import com.cooperativa.met.application.identity.usecase.RefreshTokenUseCase;
import com.cooperativa.met.application.identity.usecase.RegisterBiometricUseCase;
import com.cooperativa.met.application.identity.usecase.RegisterUserUseCase;
import com.cooperativa.met.application.identity.usecase.RequestPinRecoveryUseCase;
import com.cooperativa.met.application.identity.usecase.ResendEmailOtpUseCase;
import com.cooperativa.met.application.identity.usecase.ResetPinWithOtpUseCase;
import com.cooperativa.met.application.identity.usecase.UpdateNotificationsUseCase;
import com.cooperativa.met.application.identity.usecase.UpdatePinUseCase;
import com.cooperativa.met.application.identity.usecase.UpdateProfileUseCase;
import com.cooperativa.met.application.identity.usecase.VerifyEmailUseCase;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.infrastructure.config.RateLimitProperties;
import com.cooperativa.met.infrastructure.web.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rutas de identidad que no cubre {@link AuthControllerTest}: perfil, PIN, huella, recuperación del
 * PIN, verificación del correo y renovación de la sesión. Se montan sin el contexto completo de Spring,
 * con el manejo de errores real.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerRoutesTest {

    private static final String DOCUMENTO = "{\"documentType\":\"CC\",\"documentNumber\":\"1086578123\"}";
    private static final String CAMBIO_DE_PIN_CON_CODIGO =
            "{\"documentType\":\"CC\",\"documentNumber\":\"1086578123\",\"otpCode\":\"482913\",\"newPin\":\"pin-nuevo-cifrado\"}";

    @Mock private RegisterUserUseCase registerUserUseCase;
    @Mock private RegisterBiometricUseCase registerBiometricUseCase;
    @Mock private GetPublicKeyUseCase getPublicKeyUseCase;
    @Mock private LoginUseCase loginUseCase;
    @Mock private GetUserProfileUseCase getUserProfileUseCase;
    @Mock private RefreshTokenUseCase refreshTokenUseCase;
    @Mock private UpdateProfileUseCase updateProfileUseCase;
    @Mock private UpdatePinUseCase updatePinUseCase;
    @Mock private UpdateNotificationsUseCase updateNotificationsUseCase;
    @Mock private RequestPinRecoveryUseCase requestPinRecoveryUseCase;
    @Mock private ResetPinWithOtpUseCase resetPinWithOtpUseCase;
    @Mock private VerifyEmailUseCase verifyEmailUseCase;
    @Mock private ResendEmailOtpUseCase resendEmailOtpUseCase;
    @Mock private LogoutUseCase logoutUseCase;
    @Mock private ManageBiometricLoginUseCase manageBiometricLoginUseCase;

    private final UUID userId = UUID.randomUUID();
    private final UsernamePasswordAuthenticationToken socio =
            new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(registerUserUseCase, registerBiometricUseCase,
                getPublicKeyUseCase, loginUseCase, getUserProfileUseCase, refreshTokenUseCase, updateProfileUseCase,
                updatePinUseCase, updateNotificationsUseCase, requestPinRecoveryUseCase, resetPinWithOtpUseCase,
                verifyEmailUseCase, resendEmailOtpUseCase, logoutUseCase, manageBiometricLoginUseCase);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new RateLimitProperties()))
                .build();
    }

    private UserResponse perfil(String email, boolean avisosPorCorreo, boolean avisosPush) {
        return new UserResponse(userId, DocumentType.CC, "1086578123", email, "3001234567", "Ana", "Pérez",
                UserStatus.ACTIVE, KycStatus.APPROVED, avisosPorCorreo, avisosPush,
                Instant.parse("2026-10-01T15:00:00Z"), false);
    }

    @Test
    void entregaLaLlavePublicaConLaQueLaAppCifraElPin() throws Exception {
        when(getPublicKeyUseCase.execute()).thenReturn(Map.of("publicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A"));

        mvc.perform(get("/v1/auth/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A"));
    }

    @Test
    void verificaElCorreoConElCodigoDeSeisDigitos() throws Exception {
        mvc.perform(post("/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"CC\",\"documentNumber\":\"1086578123\",\"otpCode\":\"482913\"}"))
                .andExpect(status().isOk());

        verify(verifyEmailUseCase).execute(new VerifyEmailRequest(DocumentType.CC, "1086578123", "482913"));
    }

    @Test
    void rechazaUnCodigoDeVerificacionQueNoSonSeisDigitos() throws Exception {
        mvc.perform(post("/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"CC\",\"documentNumber\":\"1086578123\",\"otpCode\":\"48a9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.otpCode").exists());

        verifyNoInteractions(verifyEmailUseCase);
    }

    @Test
    void reenviaElCodigoDeVerificacionDelCorreo() throws Exception {
        mvc.perform(post("/v1/auth/resend-email-otp").contentType(MediaType.APPLICATION_JSON).content(DOCUMENTO))
                .andExpect(status().isOk());

        verify(resendEmailOtpUseCase).execute(new ResendEmailOtpRequest(DocumentType.CC, "1086578123"));
    }

    @Test
    void registraLaBiometriaDelSocioAutenticado() throws Exception {
        mvc.perform(post("/v1/auth/biometric").principal(socio).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentImageBase64\":\"ZnJlbnRl\",\"documentBackImageBase64\":\"cmV2ZXJzbw==\","
                                + "\"selfieImageBase64\":\"c2VsZmll\",\"signatureImageBase64\":\"Zmlyb2E=\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro biométrico completado"));

        verify(registerBiometricUseCase).execute(userId,
                new BiometricRegistrationRequest("ZnJlbnRl", "cmV2ZXJzbw==", "c2VsZmll", "Zmlyb2E="));
    }

    @Test
    void renuevaLaSesionConElTokenDeRenovacion() throws Exception {
        when(refreshTokenUseCase.execute("refresh-123"))
                .thenReturn(AuthResponse.of(userId, "access-nuevo", "refresh-nuevo", 900_000L));

        mvc.perform(post("/v1/auth/refresh").header(HttpHeaders.AUTHORIZATION, "Bearer refresh-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-nuevo"));
    }

    @Test
    void rechazaLaRenovacionSiElTokenNoVieneComoBearer() throws Exception {
        mvc.perform(post("/v1/auth/refresh").header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46YWRtaW4="))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verifyNoInteractions(refreshTokenUseCase);
    }

    @Test
    void rechazaLaRenovacionSinCabeceraDeAutorizacion() throws Exception {
        mvc.perform(post("/v1/auth/refresh"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        verifyNoInteractions(refreshTokenUseCase);
    }

    @Test
    void devuelveElPerfilDelSocioAutenticado() throws Exception {
        when(getUserProfileUseCase.execute(userId)).thenReturn(perfil("ana@correo.com", true, true));

        mvc.perform(get("/v1/auth/me").principal(socio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@correo.com"))
                .andExpect(jsonPath("$.kycStatus").value("APPROVED"));
    }

    @Test
    void actualizaElCorreoYElTelefonoDelSocioAutenticado() throws Exception {
        when(updateProfileUseCase.execute(eq(userId), any(UpdateProfileRequest.class)))
                .thenReturn(perfil("nuevo@correo.com", true, true));

        mvc.perform(put("/v1/auth/me").principal(socio).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuevo@correo.com\",\"phone\":\"3001234567\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuevo@correo.com"));

        ArgumentCaptor<UpdateProfileRequest> datos = ArgumentCaptor.forClass(UpdateProfileRequest.class);
        verify(updateProfileUseCase).execute(eq(userId), datos.capture());
        assertEquals("nuevo@correo.com", datos.getValue().getEmail());
        assertEquals("3001234567", datos.getValue().getPhone());
    }

    @Test
    void rechazaUnCorreoMalEscritoAlActualizarElPerfil() throws Exception {
        mvc.perform(put("/v1/auth/me").principal(socio).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana-arroba-correo\",\"phone\":\"3001234567\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("Formato de email inválido"));

        verifyNoInteractions(updateProfileUseCase);
    }

    @Test
    void cambiaElPinDelSocioAutenticado() throws Exception {
        mvc.perform(put("/v1/auth/pin").principal(socio).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPin\":\"pin-actual-cifrado\",\"newPin\":\"pin-nuevo-cifrado\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdatePinRequest> pines = ArgumentCaptor.forClass(UpdatePinRequest.class);
        verify(updatePinUseCase).execute(eq(userId), pines.capture());
        assertEquals("pin-actual-cifrado", pines.getValue().getCurrentPin());
        assertEquals("pin-nuevo-cifrado", pines.getValue().getNewPin());
    }

    @Test
    void activaElIngresoConHuellaEnEsteCelular() throws Exception {
        String llave = "k".repeat(43);

        mvc.perform(put("/v1/auth/biometric-login").principal(socio).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"pin-cifrado\",\"biometricToken\":\"" + llave + "\"}"))
                .andExpect(status().isOk());

        verify(manageBiometricLoginUseCase).enable(userId, new EnableBiometricLoginRequest("pin-cifrado", llave));
    }

    @Test
    void rechazaUnaLlaveDeHuellaDemasiadoCorta() throws Exception {
        mvc.perform(put("/v1/auth/biometric-login").principal(socio).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"pin-cifrado\",\"biometricToken\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.biometricToken").exists());

        verifyNoInteractions(manageBiometricLoginUseCase);
    }

    @Test
    void desactivaElIngresoConHuella() throws Exception {
        mvc.perform(delete("/v1/auth/biometric-login").principal(socio))
                .andExpect(status().isOk());

        verify(manageBiometricLoginUseCase).disable(userId);
    }

    @Test
    void guardaLasPreferenciasDeNotificaciones() throws Exception {
        when(updateNotificationsUseCase.execute(eq(userId), any(UpdateNotificationsRequest.class)))
                .thenReturn(perfil("ana@correo.com", false, true));

        mvc.perform(put("/v1/auth/notifications").principal(socio).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailNotificationsEnabled\":false,\"pushNotificationsEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailNotificationsEnabled").value(false))
                .andExpect(jsonPath("$.pushNotificationsEnabled").value(true));
    }

    @Test
    void pideElCodigoParaRecuperarElPin() throws Exception {
        mvc.perform(post("/v1/auth/pin-recovery/request").contentType(MediaType.APPLICATION_JSON).content(DOCUMENTO))
                .andExpect(status().isOk());

        ArgumentCaptor<PinRecoveryRequest> pedido = ArgumentCaptor.forClass(PinRecoveryRequest.class);
        verify(requestPinRecoveryUseCase).execute(pedido.capture());
        assertEquals(DocumentType.CC, pedido.getValue().getDocumentType());
        assertEquals("1086578123", pedido.getValue().getDocumentNumber());
    }

    @Test
    void cambiaElPinConElCodigoDeRecuperacion() throws Exception {
        mvc.perform(post("/v1/auth/pin-recovery/reset").contentType(MediaType.APPLICATION_JSON)
                        .content(CAMBIO_DE_PIN_CON_CODIGO))
                .andExpect(status().isOk());

        ArgumentCaptor<PinRecoveryResetRequest> cambio = ArgumentCaptor.forClass(PinRecoveryResetRequest.class);
        verify(resetPinWithOtpUseCase).execute(cambio.capture());
        assertEquals("482913", cambio.getValue().getOtpCode());
        assertEquals("pin-nuevo-cifrado", cambio.getValue().getNewPin());
    }

    @Test
    void respondeConElCodigoDeNegocioSiElCodigoDeRecuperacionNoSirve() throws Exception {
        doThrow(new BusinessRuleException("INVALID_OTP", "El código no es válido o ya venció"))
                .when(resetPinWithOtpUseCase).execute(any());

        mvc.perform(post("/v1/auth/pin-recovery/reset").contentType(MediaType.APPLICATION_JSON)
                        .content(CAMBIO_DE_PIN_CON_CODIGO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"))
                .andExpect(jsonPath("$.message").value("El código no es válido o ya venció"));
    }

    @Test
    void cierraLaSesionConElTokenVengaONoConElPrefijoBearer() throws Exception {
        mvc.perform(post("/v1/auth/logout").principal(socio).header(HttpHeaders.AUTHORIZATION, "Bearer jwt-123"))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/auth/logout").principal(socio).header(HttpHeaders.AUTHORIZATION, "jwt-sin-prefijo"))
                .andExpect(status().isOk());

        verify(logoutUseCase).execute(userId, "jwt-123");
        verify(logoutUseCase).execute(userId, "jwt-sin-prefijo");
    }

    // --- Errores de quien llama: deben responder 4xx, no 500 (un 500 es una falla del servidor) ---

    @Test
    void unJsonMalEscritoEsUnErrorDeQuienLlamaNoDelServidor() throws Exception {
        mvc.perform(post("/v1/auth/pin-recovery/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\": \"CC\", \"documentNumber\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(requestPinRecoveryUseCase);
    }

    @Test
    void unTipoDeDocumentoQueNoExisteEsUnErrorDeQuienLlama() throws Exception {
        mvc.perform(post("/v1/auth/pin-recovery/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"XX\",\"documentNumber\":\"1086578123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(requestPinRecoveryUseCase);
    }

    @Test
    void unaRutaQueNoExisteRespondeNoEncontrado() throws Exception {
        mvc.perform(get("/v1/auth/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unMetodoQueLaRutaNoAdmiteRespondeNoPermitido() throws Exception {
        mvc.perform(delete("/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("POST")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
