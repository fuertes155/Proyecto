package com.cooperativa.met.application.account.usecase;

import com.cooperativa.met.application.account.dto.TransferRequest;
import com.cooperativa.met.domain.account.model.AccountStatus;
import com.cooperativa.met.domain.account.model.CoreAccount;
import com.cooperativa.met.domain.account.model.CoreTransaction;
import com.cooperativa.met.domain.account.model.TransactionStatus;
import com.cooperativa.met.domain.account.model.TransactionType;
import com.cooperativa.met.domain.account.port.CoreAccountRepositoryPort;
import com.cooperativa.met.domain.account.port.CoreTransactionRepositoryPort;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.infrastructure.security.IdempotencyService;
import com.cooperativa.met.application.account.service.TransferLimitService;
import com.cooperativa.met.infrastructure.security.PinAttemptService;
import com.cooperativa.met.application.security.FraudDetectionService;

@Service
@RequiredArgsConstructor
public class ExecuteTransferUseCase {

    private final CoreAccountRepositoryPort accountRepository;
    private final CoreTransactionRepositoryPort transactionRepository;
    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final OtpService otpService;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;
    private final TransferLimitService transferLimitService;
    private final PinAttemptService pinAttemptService;
    private final FraudDetectionService fraudDetectionService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void execute(UUID userId, TransferRequest request, String ip) {
        // 0. Idempotency Check
        if (request.idempotencyKey() != null) {
            if (!idempotencyService.tryAcquire(request.idempotencyKey())) {
                Optional<String> result = idempotencyService.getResult(request.idempotencyKey());
                if (result.isPresent()) {
                    // Si ya se procesó con éxito, retornamos sin hacer nada (es void)
                    return;
                } else {
                    // Si está en proceso por otra petición concurrente
                    throw new BusinessRuleException("CONCURRENT_REQUEST", "La transferencia está siendo procesada. Por favor espere.");
                }
            }
        }

        // 1. Verify User PIN & Status
        pinAttemptService.checkBlocked(userId);
        
        // 🔥 Fraude: Viaje Imposible y Controles de Velocidad
        fraudDetectionService.checkImpossibleTravel(userId, ip);
        fraudDetectionService.checkTransferVelocity(userId, request.destinationAccountId());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getKycStatus() != com.cooperativa.met.domain.identity.model.KycStatus.APPROVED) {
            throw new BusinessRuleException("KYC_REQUIRED", "Debe completar la validación de identidad biométrica para transferir");
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
                    "TRANSFER", null, "{\"reason\":\"INVALID_PIN\"}");
            pinAttemptService.checkAndRecordFailure(userId); // This throws exception
        }
        pinAttemptService.resetAttempts(userId);

        // 2. Fetch Accounts
        CoreAccount sourceAccount = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No tienes una cuenta activa"));
                
        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("INACTIVE_ACCOUNT", "Tu cuenta no está activa para transferir");
        }

        CoreAccount destAccount = accountRepository.findById(request.destinationAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta destino no encontrada"));
                
        if (destAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("INACTIVE_DEST_ACCOUNT", "La cuenta destino no está activa");
        }
        
        if (sourceAccount.getId().equals(destAccount.getId())) {
            throw new BusinessRuleException("SAME_ACCOUNT", "No puedes transferir a la misma cuenta");
        }

        // 2.5. Verify OTP
        if (request.otp() == null || request.otp().isEmpty()) {
            throw new BusinessRuleException("OTP_REQUIRED", "Se requiere código de seguridad para transferir");
        }
        boolean isOtpValid = otpService.validateOtp(user.getDocumentNumber(), request.otp());
        if (!isOtpValid) {
            auditLogService.logFailure(userId, AuditLogService.TRANSFER_FAILED,
                    "TRANSFER", null, "{\"reason\":\"INVALID_OTP\"}");
            throw new BusinessRuleException("INVALID_OTP", "El código de seguridad es incorrecto o expiró");
        }

        // 2.8. Validate Transfer Limits
        transferLimitService.validateTransferLimits(sourceAccount, request.amount());

        // 3. Process Transfer (Debit & Credit)
        CoreAccount updatedSource = sourceAccount.debitPrincipal(request.amount());
        CoreAccount updatedDest = destAccount.creditPrincipal(request.amount());

        // 4. Save Accounts
        accountRepository.save(updatedSource);
        accountRepository.save(updatedDest);

        // 5. Create Transaction Record
        CoreTransaction transaction = CoreTransaction.builder()
                .id(UUID.randomUUID())
                .sourceAccountId(updatedSource.getId())
                .destinationAccountId(updatedDest.getId())
                .amount(request.amount())
                .concept(request.concept())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
                
        transactionRepository.save(transaction);

        // 6. Audit log — registro exitoso con datos enmascarados (Compliance)
        String maskedAmount = "***";
        String maskedAccount = request.destinationAccountId().toString().substring(0, 4) + "...";
        
        auditLogService.logSuccess(userId, AuditLogService.TRANSFER_EXECUTED,
                "TRANSFER", transaction.getId().toString(),
                String.format("{\"amount\":\"%s\",\"destinationAccountId\":\"%s\"}",
                        maskedAmount, maskedAccount));
                        
        // 7. Save Idempotency Result
        if (request.idempotencyKey() != null) {
            idempotencyService.saveResult(request.idempotencyKey(), transaction.getId().toString());
        }
    }
}
