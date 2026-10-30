package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.GeneratePseLinkRequest;
import com.cooperativa.met.application.account.dto.GeneratePseLinkResponse;
import com.cooperativa.met.application.account.service.PaymentReturnUrlResolver;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.adapter.wompi.WompiSignatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneratePseLinkUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String RETURN_PAGE = "https://met.test/api/v1/payments/wompi/return";

    @Mock
    private CoreAccountRepositoryPort accountRepository;

    @Mock
    private UserRepositoryPort userRepository;

    private GeneratePseLinkUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
                CoreAccount.builder().userId(USER_ID).status(AccountStatus.ACTIVE).build()));
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(
                User.builder().id(USER_ID).kycStatus(KycStatus.APPROVED).build()));
    }

    private void configure(String gateway, String mockBaseUrl, String redirectUrl) {
        useCase = new GeneratePseLinkUseCase(accountRepository, userRepository, new PaymentReturnUrlResolver(redirectUrl));
        ReflectionTestUtils.setField(useCase, "wompiPublicKey", "pub_test_abc");
        ReflectionTestUtils.setField(useCase, "wompiIntegritySecret", "test_integrity_abc123");
        ReflectionTestUtils.setField(useCase, "wompiCheckoutUrl", "https://checkout.wompi.co/p/");
        ReflectionTestUtils.setField(useCase, "paymentsGateway", gateway);
        ReflectionTestUtils.setField(useCase, "mockGatewayBaseUrl", mockBaseUrl);
    }

    @Test
    void rechazaElEnlaceMientrasLaIdentidadNoEsteAprobada() {
        // El webhook solo acredita a socios con identidad aprobada: sin esto, el pago quedaba cobrado
        // en Wompi y nunca llegaba a la cuenta.
        configure("wompi", "", RETURN_PAGE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(
                User.builder().id(USER_ID).kycStatus(KycStatus.PENDING).build()));

        assertEquals("KYC_REQUIRED", assertThrows(BusinessRuleException.class,
                () -> useCase.execute(USER_ID, request("50000", "metapp://deposit/success"))).getCode());
        verifyNoInteractions(accountRepository);
    }

    @Test
    void armaElEnlaceFirmadoConLaPaginaDeRegresoCodificada() {
        configure("wompi", "", RETURN_PAGE);

        GeneratePseLinkResponse response = useCase.execute(USER_ID, request("50000", "metapp://deposit/success"));

        assertTrue(response.getPaymentUrl().startsWith("https://checkout.wompi.co/p/?"), response.getPaymentUrl());
        MultiValueMap<String, String> query = query(response.getPaymentUrl());
        assertEquals("pub_test_abc", query.getFirst("public-key"));
        assertEquals("COP", query.getFirst("currency"));
        assertEquals("5000000", query.getFirst("amount-in-cents"));
        assertEquals(response.getTransactionId(), query.getFirst("reference"));
        assertEquals(WompiSignatures.integrity(response.getTransactionId(), 5_000_000L, "COP", "test_integrity_abc123"),
                query.getFirst("signature:integrity"));
        // La URL configurada gana sobre el deep link que manda la app.
        assertEquals(RETURN_PAGE, query.getFirst("redirect-url"));
    }

    @Test
    void sinPaginaDeRegresoConfiguradaConservaElEnlaceProfundoCodificado() {
        configure("wompi", "", "");

        GeneratePseLinkResponse response = useCase.execute(USER_ID, request("50000", "metapp://deposit/success"));

        assertEquals("metapp://deposit/success", query(response.getPaymentUrl()).getFirst("redirect-url"));
    }

    @Test
    void enModoWompiIgnoraLaUrlDeLaPasarelaSimulada() {
        // Render define MET_PAYMENTS_MOCK_GATEWAY_BASE_URL; con gateway=wompi no debe usarse.
        configure("wompi", "https://met-backend.onrender.com", RETURN_PAGE);

        GeneratePseLinkResponse response = useCase.execute(USER_ID, request("50000", null));

        assertTrue(response.getPaymentUrl().startsWith("https://checkout.wompi.co/p/"), response.getPaymentUrl());
    }

    @Test
    void enModoSimuladoEnlazaALaPasarelaSimulada() {
        configure("mock", "https://met-backend.onrender.com/", "");

        GeneratePseLinkResponse response = useCase.execute(USER_ID, request("50000", null));

        assertTrue(response.getPaymentUrl().startsWith("https://met-backend.onrender.com/api/v1/mock-payment-gateway?"),
                response.getPaymentUrl());
    }

    private static GeneratePseLinkRequest request(String amount, String returnUrl) {
        GeneratePseLinkRequest request = new GeneratePseLinkRequest();
        request.setAmount(new BigDecimal(amount));
        request.setReturnUrl(returnUrl);
        return request;
    }

    private static MultiValueMap<String, String> query(String url) {
        MultiValueMap<String, String> raw = UriComponentsBuilder.fromUriString(url).build().getQueryParams();
        MultiValueMap<String, String> decoded = new org.springframework.util.LinkedMultiValueMap<>();
        raw.forEach((key, values) -> values.forEach(v -> decoded.add(key, URLDecoder.decode(v, StandardCharsets.UTF_8))));
        return decoded;
    }
}
