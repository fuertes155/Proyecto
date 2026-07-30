package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.application.bank.dto.RegisterExternalBankAccountRequest;
import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Registra una cuenta bancaria externa como posible destino de payout.
 *
 * "Solo cuentas propias": esta cuenta jamás guarda el nombre/identificación
 * del titular — al ejecutar un payout, esos datos siempre se toman de la
 * identidad KYC del usuario dueño de la wallet, nunca de un campo editable.
 * Eso hace estructuralmente imposible registrar una cuenta "a nombre de
 * otra persona" desde este flujo.
 *
 * Titularidad real: apenas se registra, se dispara el envío del
 * micro-depósito de verificación (ver InitiateBankAccountVerificationUseCase).
 * Si ese envío falla (ej. proveedor caído), el registro NO se revierte —
 * la cuenta queda PENDING y el usuario puede reintentar el envío desde la
 * app; no tiene sentido perder los datos ya capturados por un fallo
 * transitorio del riel de pago. Mientras no esté VERIFIED, el payout hacia
 * ella opera bajo el límite reducido de PayoutLimitService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterExternalBankAccountUseCase {

    private final ExternalBankAccountRepositoryPort externalBankAccountRepository;
    private final BankRepositoryPort bankRepository;
    private final UserRepositoryPort userRepository;
    private final AuditLogService auditLogService;
    private final InitiateBankAccountVerificationUseCase initiateBankAccountVerificationUseCase;

    public ExternalBankAccount execute(UUID userId, RegisterExternalBankAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getKycStatus() != KycStatus.APPROVED) {
            throw new BusinessRuleException("KYC_REQUIRED",
                    "Debe completar la validación de identidad para registrar una cuenta bancaria externa");
        }

        Bank bank = bankRepository.findByCode(request.bankCode())
                .orElseThrow(() -> new BusinessRuleException("BANK_NOT_FOUND", "El banco seleccionado no existe"));

        if (!bank.isActive() || !bank.isSupportsPayout()) {
            throw new BusinessRuleException("BANK_NOT_SUPPORTED",
                    "El banco seleccionado no soporta retiros por el momento");
        }

        ExternalBankAccount account = ExternalBankAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .bankCode(bank.getCode())
                .accountType(request.accountType())
                .accountNumber(request.accountNumber())
                .verificationStatus(BankAccountVerificationStatus.PENDING)
                .active(true)
                .createdAt(Instant.now())
                .build();

        ExternalBankAccount saved = externalBankAccountRepository.save(account);

        auditLogService.logSuccess(userId, "EXTERNAL_BANK_ACCOUNT_REGISTERED",
                "EXTERNAL_BANK_ACCOUNT", saved.getId().toString(),
                String.format("{\"bankCode\":\"%s\",\"accountType\":\"%s\"}", bank.getCode(), request.accountType()));

        try {
            initiateBankAccountVerificationUseCase.execute(userId, saved.getId());
        } catch (Exception e) {
            log.warn("No fue posible enviar el depósito de verificación automáticamente para la cuenta {}: {}",
                    saved.getId(), e.getMessage());
        }

        return saved;
    }
}
