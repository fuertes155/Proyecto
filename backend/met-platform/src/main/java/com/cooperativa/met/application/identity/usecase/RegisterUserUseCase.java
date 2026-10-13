package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.RegisterUserRequest;
import com.cooperativa.met.application.identity.dto.UserResponse;
import com.cooperativa.met.application.identity.mapper.UserMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.PersonName;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.application.identity.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final UserMapper userMapper;
    private final OtpService otpService;

    @Transactional
    public UserResponse execute(RegisterUserRequest request) {
        if (userRepository.existsByDocument(request.documentType(), request.documentNumber())) {
            throw new BusinessRuleException("USER_ALREADY_EXISTS", "El documento ya está registrado");
        }
        // El registro simplificado no trae correo: el usuario lo agrega después desde su perfil.
        String email = StringUtils.hasText(request.email()) ? request.email() : null;
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("EMAIL_ALREADY_EXISTS", "El correo ya está registrado");
        }
        PersonName name = resolveName(request);

        String plainPin;
        try {
            plainPin = encryptionPort.decryptRsa(request.pin());
        } catch (Exception e) {
            throw new BusinessRuleException("SECURITY_ERROR", "Error de seguridad en la encriptación de extremo a extremo durante el registro");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .email(email)
                .phone(request.phone())
                .firstName(name.firstName())
                .lastName(name.lastName())
                .pinHash(encryptionPort.hashPin(plainPin))
                // Sin correo no hay nada que verificar: la cuenta queda activa de una vez.
                .status(email != null ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE)
                .kycStatus(KycStatus.PENDING)
                .termsAccepted(request.termsAccepted())
                .termsAcceptedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        if (email != null) {
            otpService.generateAndSendEmailVerificationOtp(savedUser.getDocumentNumber(), email);
        }
        return userMapper.toResponse(savedUser);
    }

    private PersonName resolveName(RegisterUserRequest request) {
        if (StringUtils.hasText(request.firstName()) && StringUtils.hasText(request.lastName())) {
            return new PersonName(request.firstName(), request.lastName());
        }
        return PersonName.fromFullName(request.fullName());
    }
}
