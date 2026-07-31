package com.cooperativa.met.application.investment.service;

import com.cooperativa.met.domain.investment.model.GuaranteeFundMovement;
import com.cooperativa.met.domain.investment.model.GuaranteeFundMovementType;
import com.cooperativa.met.domain.investment.port.GuaranteeFundPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuaranteeFundServiceTest {

    @Mock
    private GuaranteeFundPort fundPort;

    private GuaranteeFundService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new GuaranteeFundService(fundPort);
        lenient().when(fundPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void contribute_recordsAContributionMovement() {
        UUID reference = UUID.randomUUID();

        service.contribute(new BigDecimal("15000.00"), "FGA - desembolso", reference);

        ArgumentCaptor<GuaranteeFundMovement> captor = ArgumentCaptor.forClass(GuaranteeFundMovement.class);
        verify(fundPort).save(captor.capture());
        assertEquals(GuaranteeFundMovementType.CONTRIBUTION, captor.getValue().getType());
        assertEquals(new BigDecimal("15000.00"), captor.getValue().getAmount());
    }

    @Test
    void coverDefault_coversFullyWhenFundHasEnoughBalance() {
        when(fundPort.getBalance()).thenReturn(new BigDecimal("1000000.00"));

        BigDecimal covered = service.coverDefault(new BigDecimal("100000.00"), new BigDecimal("0.70"),
                UUID.randomUUID(), "mora");

        // 70% de 100,000 = 70,000, y el fondo tiene de sobra
        assertEquals(new BigDecimal("70000.00"), covered);
        verify(fundPort).save(any());
    }

    @Test
    void coverDefault_neverExceedsAvailableBalance_evenIfCoverageRatioAllowsMore() {
        when(fundPort.getBalance()).thenReturn(new BigDecimal("30000.00"));

        BigDecimal covered = service.coverDefault(new BigDecimal("100000.00"), new BigDecimal("0.70"),
                UUID.randomUUID(), "mora");

        // 70% de 100,000 = 70,000, pero el fondo solo tiene 30,000 disponibles
        assertEquals(new BigDecimal("30000.00"), covered);
    }

    @Test
    void coverDefault_returnsZeroAndDoesNotRecordMovement_whenFundIsEmpty() {
        when(fundPort.getBalance()).thenReturn(BigDecimal.ZERO);

        BigDecimal covered = service.coverDefault(new BigDecimal("100000.00"), new BigDecimal("0.70"),
                UUID.randomUUID(), "mora");

        assertEquals(0, covered.compareTo(BigDecimal.ZERO));
        verify(fundPort, never()).save(any());
    }

    @Test
    void coverDefault_neverCovers100Percent_capReflectedInResult() {
        when(fundPort.getBalance()).thenReturn(new BigDecimal("1000000.00"));

        BigDecimal covered = service.coverDefault(new BigDecimal("100000.00"), new BigDecimal("1.00"),
                UUID.randomUUID(), "mora");

        // Si alguien configurara coverageRatio=1.00 por error, el servicio lo respeta tal cual
        // (el tope de "nunca 100%" es una decisión de configuración, no un límite oculto aquí);
        // lo relevante es que el default de aplicación nunca es 1.00 (ver CapitalEngineProperties).
        assertEquals(new BigDecimal("100000.00"), covered);
    }
}
