package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Confirma la titularidad de una cuenta bancaria externa comparando el
 * monto que el usuario reporta haber visto en su extracto contra el
 * micro-depósito enviado por {@link InitiateBankAccountVerificationUseCase}.
 * Limita intentos para que no sea adivinable por fuerza bruta (el rango es
 * amplio, pero igual se acota).
 */
@Service
@RequiredArgsConstructor
public class ConfirmBankAccountVerificationUseCase {

    private static final int MAX_ATTEMPTS = 5;

    private final ExternalBankAccountRepositoryPort externalBankAccountRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void execute(UUID userId, UUID externalBankAccountId, int submittedAmount) {
        ExternalBankAccount account = externalBankAccountRepository.findById(externalBankAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));

        if (!account.getUserId().equals(userId)) {
            auditLogService.logFailure(userId, "EXTERNAL_BANK_ACCOUNT_VERIFY_FORBIDDEN",
                    "EXTERNAL_BANK_ACCOUNT", account.getId().toString(), "{\"reason\":\"ACCOUNT_NOT_OWNED_BY_USER\"}");
            throw new BusinessRuleException("FORBIDDEN", "Esta cuenta bancaria no te pertenece");
        }
        if (account.getVerificationStatus() == BankAccountVerificationStatus.VERIFIED) {
            throw new BusinessRuleException("ALREADY_VERIFIED", "Esta cuenta ya está verificada");
        }
        if (account.getPendingVerificationAmount() == null) {
            throw new BusinessRuleException("NO_VERIFICATION_PENDING",
                    "No hay una verificación en curso. Solicita un nuevo depósito de verificación.");
        }
        if (account.getVerificationAttempts() >= MAX_ATTEMPTS) {
            externalBankAccountRepository.save(account.clearPendingVerification());
            throw new BusinessRuleException("MAX_ATTEMPTS_EXCEEDED",
                    "Superaste el número de intentos permitidos. Solicita un nuevo depósito de verificación.");
        }

        if (account.getPendingVerificationAmount() == submittedAmount) {
            externalBankAccountRepository.save(account.verify());
            auditLogService.logSuccess(userId, "EXTERNAL_BANK_ACCOUNT_VERIFIED",
                    "EXTERNAL_BANK_ACCOUNT", account.getId().toString(), "{}");
            return;
        }

        ExternalBankAccount updated = account.withIncrementedAttempts();
        boolean attemptsExhausted = updated.getVerificationAttempts() >= MAX_ATTEMPTS;
        externalBankAccountRepository.save(attemptsExhausted ? updated.clearPendingVerification() : updated);

        auditLogService.logFailure(userId, "EXTERNAL_BANK_ACCOUNT_VERIFICATION_FAILED",
                "EXTERNAL_BANK_ACCOUNT", account.getId().toString(),
                String.format("{\"attempts\":%d}", updated.getVerificationAttempts()));

        if (attemptsExhausted) {
            throw new BusinessRuleException("MAX_ATTEMPTS_EXCEEDED",
                    "Superaste el número de intentos permitidos. Solicita un nuevo depósito de verificación.");
        }
        throw new BusinessRuleException("INCORRECT_AMOUNT",
                "El monto ingresado no coincide. Revisa tu extracto bancario e intenta de nuevo.");
    }
}
