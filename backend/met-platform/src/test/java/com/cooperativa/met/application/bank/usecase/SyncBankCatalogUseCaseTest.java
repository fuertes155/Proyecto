package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PseFinancialInstitution;
import com.cooperativa.met.domain.bank.port.PseGatewayPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncBankCatalogUseCaseTest {

    @Mock
    private BankRepositoryPort bankRepository;

    @Mock
    private PayoutGatewayPort payoutGatewayPort;

    @Mock
    private PseGatewayPort pseGatewayPort;

    @InjectMocks
    private SyncBankCatalogUseCase useCase;

    @Test
    void sandboxTestBanksAreCreatedWithTheirWompiCodes() {
        when(pseGatewayPort.fetchFinancialInstitutions()).thenReturn(List.of(
                new PseFinancialInstitution("1", "Banco que aprueba")));
        when(bankRepository.findAllActive()).thenReturn(List.of(
                Bank.builder().code("BANCOLOMBIA").name("Bancolombia").active(true).build()));
        when(bankRepository.findByCode(anyString())).thenReturn(Optional.empty());

        assertEquals(1, useCase.executePseOnly());

        ArgumentCaptor<Bank> saved = ArgumentCaptor.forClass(Bank.class);
        verify(bankRepository).save(saved.capture());
        assertEquals("BANCOQUEAPRUEBA", saved.getValue().getCode());
        assertEquals("1", saved.getValue().getWompiPseCode());
        assertTrue(saved.getValue().isSupportsPse());
        verifyNoInteractions(payoutGatewayPort);
    }

    @Test
    void longBankNamesFitTheTwentyCharacterCodeWithoutOverwritingAnotherBank() {
        when(pseGatewayPort.fetchFinancialInstitutions()).thenReturn(List.of(
                new PseFinancialInstitution("1066", "Banco Cooperativo Coopcentral S.A.")));
        when(bankRepository.findAllActive()).thenReturn(List.of());
        // Ya existe (inactivo) un banco con el código recortado.
        when(bankRepository.findByCode("BANCOCOOPERATIVOCOOP")).thenReturn(Optional.of(
                Bank.builder().code("BANCOCOOPERATIVOCOOP").name("Otro banco").build()));
        when(bankRepository.findByCode("BANCOCOOPERATIVOCOO2")).thenReturn(Optional.empty());

        useCase.executePseOnly();

        ArgumentCaptor<Bank> saved = ArgumentCaptor.forClass(Bank.class);
        verify(bankRepository).save(saved.capture());
        assertEquals("BANCOCOOPERATIVOCOO2", saved.getValue().getCode());
        assertEquals("Banco Cooperativo Coopcentral S.A.", saved.getValue().getName());
        assertEquals("1066", saved.getValue().getWompiPseCode());
    }
}
