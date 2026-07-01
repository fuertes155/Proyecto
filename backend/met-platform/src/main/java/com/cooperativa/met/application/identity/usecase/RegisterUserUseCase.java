package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.RegisterUserRequest;
import com.cooperativa.met.application.identity.dto.UserResponse;
import com.cooperativa.met.application.identity.mapper.UserMapper;
import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.EncryptionPort;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final EncryptionPort encryptionPort;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse execute(RegisterUserRequest request) {
        if (userRepository.existsByDocument(request.documentType(), request.documentNumber())) {
            throw new BusinessRuleException("USER_ALREADY_EXISTS", "El documento ya está registrado");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("EMAIL_ALREADY_EXISTS", "El correo ya está registrado");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .email(request.email())
                .phone(request.phone())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .pinHash(encryptionPort.hashPin(request.pin()))
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.APPROVED)
                .termsAccepted(request.termsAccepted())
                .termsAcceptedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }
}
