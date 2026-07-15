package com.cooperativa.met.application.legal.usecase;

import com.cooperativa.met.application.identity.service.OtpService;
import com.cooperativa.met.application.legal.service.PdfGeneratorService;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.domain.legal.model.MandateContract;
import com.cooperativa.met.domain.legal.model.MandateContractStatus;
import com.cooperativa.met.domain.legal.port.MandateContractRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmMandateSignatureUseCase {

    private final UserRepositoryPort userRepository;
    private final OtpService otpService;
    private final PdfGeneratorService pdfGeneratorService;
    private final MandateContractRepositoryPort mandateContractRepository;

    @Transactional
    public MandateContract execute(UUID userId, String otpCode, String ipAddress, String userAgent, String otpTxId) {
        log.info("Usuario {} confirmando firma de mandato con OTP", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USR_01", "Usuario no encontrado"));

        // 1. Validar OTP
        boolean isValid = otpService.validateOtp(user.getDocumentNumber(), otpCode);
        if (!isValid) {
            throw new BusinessRuleException("LEGAL_01", "Código OTP inválido o expirado");
        }

        // 2. Comprobar si ya firmó
        if (mandateContractRepository.findByUserId(userId).isPresent()) {
            throw new BusinessRuleException("LEGAL_02", "El usuario ya tiene un mandato firmado");
        }

        Instant signedAt = Instant.now();

        // 3. Generar PDF y Hash
        PdfGeneratorService.PdfResult pdfResult = pdfGeneratorService.generateMandatePdf(
                user.getFirstName() + " " + user.getLastName(),
                user.getDocumentNumber(),
                ipAddress,
                signedAt
        );

        // 4. Guardar contrato
        MandateContract contract = MandateContract.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .documentNumber(user.getDocumentNumber())
                .pdfContent(pdfResult.pdfContent())
                .pdfHashSha256(pdfResult.hashSha256())
                .signedAt(signedAt)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .otpTransactionId(otpTxId)
                .status(MandateContractStatus.SIGNED)
                .build();

        mandateContractRepository.save(contract);
        log.info("Contrato de mandato firmado y guardado exitosamente para el usuario {}", userId);

        return contract;
    }
}
