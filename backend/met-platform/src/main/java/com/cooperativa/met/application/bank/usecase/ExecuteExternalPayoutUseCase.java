package com.cooperativa.met.application.bank.usecase;

import com.cooperativa.met.application.account.service.AccountReversalService;
import com.cooperativa.met.application.bank.dto.ExecutePayoutRequest;
import com.cooperativa.met.application.bank.service.PayoutLimitService;
import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.application.security.FraudDetectionService;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.bank.model.Bank;
import com.cooperativa.met.domain.bank.model.BankAccountVerificationStatus;
import com.cooperativa.met.domain.bank.model.ExternalBankAccount;
import com.cooperativa.met.domain.bank.model.ExternalPayout;
import com.cooperativa.met.domain.bank.port.BankRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalBankAccountRepositoryPort;
import com.cooperativa.met.domain.bank.port.ExternalPayoutRepositoryPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayPort;
import com.cooperativa.met.domain.bank.port.PayoutGatewayRequest;
import com.cooperativa.met.domain.bank.port.PayoutGatewayResult;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import com.cooperativa.met.infrastructure.security.IdempotencyService;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Ejecuta un retiro/payout hacia una cuenta bancaria externa del propio
 * usuario. Reutiliza el mismo pipeline de seguridad de
 * {@code ExecuteTransferUseCase} (PIN, OTP, fraude, KYC, idempotencia),
 * pero a diferencia de una transferencia interna, la confirmación del
 * riel llega de forma asíncrona (webhook) — por eso NO se envuelve todo
 * el método en una única transacción SERIALIZABLE:
 *
 * <ol>
 *   <li>Fase 1 (transacción corta): valida límites, debita la cuenta y
 *       crea el {@code core_transaction} en estado PENDING.</li>
 *   <li>Fase 2 (fuera de la transacción): llama al proveedor del riel de
 *       pago (Wompi). Mantener esta llamada HTTP dentro de una
 *       transacción de BD retendría locks/optimistic-locks durante toda
 *       la latencia de red — antipatrón para un pool de conexiones
 *       compartido.</li>
 *   <li>Fase 3 (transacción corta): si el proveedor aceptó, guarda la
 *       referencia del riel. Si lo rechazó (o falló la llamada), revierte
 *       el débito y marca la transacción como FAILED.</li>
 * </ol>
 *
 * La reversión de la Fase 3 usa {@link AccountReversalService}, que
 * reintenta ante conflictos de bloqueo optimista (dos escrituras
 * concurrentes sobre la misma cuenta) en vez de perder el dinero
 * silenciosamente si se agotan los reintentos, el error se propaga y
 * requiere reconciliación manual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteExternalPayoutUseCase {

    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final ExternalBankAccountRepositoryPort externalBankAccountRepository;
    private final ExternalPayoutRepositoryPort externalPayoutRepository;
    private final BankRepositoryPort bankRepository;
    private final PayoutGatewayPort payoutGatewayPort;
    private final PayoutLimitService payoutLimitService;
    private final AccountReversalService accountReversalService;
    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final OtpService otpService;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;
    private final PinAttemptService pinAttemptService;
    private final FraudDetectionService fraudDetectionService;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    }

    private record PhaseOneResult(CoreTransaction transaction, ExternalPayout payout) {
    }

    public void execute(UUID userId, ExecutePayoutRequest request, String ip) {
        if (request.idempotencyKey() != null) {
            if (!idempotencyService.tryAcquire(request.idempotencyKey())) {
                Optional<String> result = idempotencyService.getResult(request.idempotencyKey());
                if (result.isPresent()) {
                    return;
                }
                throw new BusinessRuleException("CONCURRENT_REQUEST", "El retiro está siendo procesado. Por favor espere.");
            }
        }

        pinAttemptService.checkBlocked(userId);
        fraudDetectionService.checkImpossibleTravel(userId, ip);
        fraudDetectionService.checkTransferVelocity(userId, request.externalBankAccountId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getKycStatus() != KycStatus.APPROVED) {
            throw new BusinessRuleException("KYC_REQUIRED", "Debe completar la validación de identidad para retirar");
        }

        boolean pinValid = false;
        try {
            String plainPin = encryptionPort.decryptRsa(request.pin());
            pinValid = encryptionPort.verifyPin(plainPin, user.getPinHash());
        } catch (Exception e) {
            // E2EE decryption failed
        }
        if (!pinValid) {
            auditLogService.logFailure(userId, AuditLogService.TRANSFER_FAILED,
                    "EXTERNAL_PAYOUT", null, "{\"reason\":\"INVALID_PIN\"}");
            pinAttemptService.checkAndRecordFailure(userId); // lanza excepción
        }
        pinAttemptService.resetAttempts(userId);

        ExternalBankAccount externalAccount = externalBankAccountRepository.findById(request.externalBankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria destino no encontrada"));

        if (!externalAccount.getUserId().equals(userId)) {
            // Señal de posible enumeración/toma de cuenta: alguien intentó pagar
            // hacia una cuenta bancaria externa que no le pertenece. Se audita
            // como evento propio, no solo como una BusinessRuleException más.
            auditLogService.logFailure(userId, "EXTERNAL_PAYOUT_FORBIDDEN_DESTINATION",
                    "EXTERNAL_BANK_ACCOUNT", externalAccount.getId().toString(),
                    "{\"reason\":\"ACCOUNT_NOT_OWNED_BY_USER\"}");
            throw new BusinessRuleException("FORBIDDEN_DESTINATION", "La cuenta bancaria destino no te pertenece");
        }
        if (!externalAccount.isActive() || externalAccount.getVerificationStatus() == BankAccountVerificationStatus.REJECTED) {
            throw new BusinessRuleException("BANK_ACCOUNT_NOT_USABLE", "Esta cuenta bancaria no está disponible para retiros");
        }

        Bank bank = bankRepository.findByCode(externalAccount.getBankCode())
                .orElseThrow(() -> new BusinessRuleException("BANK_NOT_FOUND", "Banco destino no encontrado"));
        if (!bank.isActive() || !bank.isSupportsPayout()) {
            throw new BusinessRuleException("BANK_NOT_SUPPORTED", "El banco destino no soporta retiros actualmente");
        }
        if (bank.getWompiBankId() == null || bank.getWompiBankId().isBlank()) {
            throw new BusinessRuleException("BANK_NOT_SYNCED", "Este banco aún no está disponible para retiros, intenta más tarde");
        }

        if (request.otp() == null || request.otp().isEmpty()) {
            throw new BusinessRuleException("OTP_REQUIRED", "Se requiere código de seguridad para retirar");
        }
        boolean otpValid = otpService.validateOtp(user.getDocumentNumber(), request.otp());
        if (!otpValid) {
            auditLogService.logFailure(userId, AuditLogService.TRANSFER_FAILED,
                    "EXTERNAL_PAYOUT", null, "{\"reason\":\"INVALID_OTP\"}");
            throw new BusinessRuleException("INVALID_OTP", "El código de seguridad es incorrecto o expiró");
        }

        PhaseOneResult phase1 = transactionTemplate.execute(status -> {
            CoreAccount sourceAccount = accountRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("No tienes una cuenta activa"));
            if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
                throw new BusinessRuleException("INACTIVE_ACCOUNT", "Tu cuenta no está activa para retirar");
            }

            boolean destinationVerified = externalAccount.getVerificationStatus() == BankAccountVerificationStatus.VERIFIED;
            payoutLimitService.validate(sourceAccount, request.amount(), destinationVerified);

            // Solo se puede retirar de las ganancias (interestBalance), no del capital
            // (principalBalance) — el capital permanece invertido en la plataforma.
            if (request.amount().compareTo(sourceAccount.getInterestBalance()) > 0) {
                throw new BusinessRuleException("INSUFFICIENT_EARNINGS",
                        "El monto supera tus ganancias disponibles para retiro");
            }

            CoreAccount updatedSource = sourceAccount.debitInterest(request.amount());
            accountRepository.save(updatedSource);

            CoreTransaction transaction = CoreTransaction.builder()
                    .id(UUID.randomUUID())
                    .sourceAccountId(updatedSource.getId())
                    .destinationAccountId(null)
                    .amount(request.amount())
                    .concept(request.concept())
                    .type(TransactionType.EXTERNAL_PAYOUT)
                    .status(TransactionStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();
            CoreTransaction savedTransaction = transactionRepository.save(transaction);

            ExternalPayout payout = ExternalPayout.builder()
                    .id(UUID.randomUUID())
                    .coreTransactionId(savedTransaction.getId())
                    .externalBankAccountId(externalAccount.getId())
                    .createdAt(Instant.now())
                    .build();
            ExternalPayout savedPayout = externalPayoutRepository.save(payout);

            return new PhaseOneResult(savedTransaction, savedPayout);
        });

        PayoutGatewayResult gatewayResult;
        try {
            String reference = "PAYOUT-" + phase1.payout().getId();
            PayoutGatewayRequest gatewayRequest = new PayoutGatewayRequest(
                    reference,
                    request.amount(),
                    bank.getWompiBankId(),
                    externalAccount.getAccountType().name(),
                    externalAccount.getAccountNumber(),
                    user.getDocumentType().name(),
                    user.getDocumentNumber(),
                    user.getFirstName() + " " + user.getLastName(),
                    user.getEmail(),
                    phase1.payout().getId());
            gatewayResult = payoutGatewayPort.initiatePayout(gatewayRequest);
        } catch (Exception e) {
            log.error("Error al iniciar payout {} en el proveedor: {}", phase1.payout().getId(), e.getMessage(), e);
            gatewayResult = new PayoutGatewayResult(null, PayoutGatewayResult.PayoutGatewayStatus.REJECTED,
                    "GATEWAY_ERROR", "Error de comunicación con el proveedor de pagos");
        }

        boolean accepted = gatewayResult.status() == PayoutGatewayResult.PayoutGatewayStatus.ACCEPTED;
        PayoutGatewayResult finalResult = gatewayResult;

        transactionTemplate.executeWithoutResult(status -> {
            if (accepted) {
                externalPayoutRepository.save(phase1.payout().withRailReference(finalResult.railReference()));
            } else {
                accountReversalService.creditWithRetry(phase1.transaction().getSourceAccountId(), request.amount());

                transactionRepository.save(phase1.transaction().toBuilder()
                        .status(TransactionStatus.FAILED)
                        .build());

                externalPayoutRepository.save(phase1.payout()
                        .markFailed(finalResult.failureCode(), finalResult.failureMessage()));
            }
        });

        if (!accepted) {
            auditLogService.logFailure(userId, "EXTERNAL_PAYOUT_FAILED",
                    "EXTERNAL_PAYOUT", phase1.transaction().getId().toString(),
                    String.format("{\"failureCode\":\"%s\"}", finalResult.failureCode()));
            throw new BusinessRuleException(
                    finalResult.failureCode() != null ? finalResult.failureCode() : "PAYOUT_REJECTED",
                    finalResult.failureMessage() != null ? finalResult.failureMessage()
                            : "El retiro fue rechazado por el proveedor de pagos");
        }

        String maskedAmount = "***";
        auditLogService.logSuccess(userId, "EXTERNAL_PAYOUT_INITIATED",
                "EXTERNAL_PAYOUT", phase1.transaction().getId().toString(),
                String.format("{\"amount\":\"%s\",\"bankCode\":\"%s\"}", maskedAmount, bank.getCode()));

        if (request.idempotencyKey() != null) {
            idempotencyService.saveResult(request.idempotencyKey(), phase1.transaction().getId().toString());
        }
    }
}
