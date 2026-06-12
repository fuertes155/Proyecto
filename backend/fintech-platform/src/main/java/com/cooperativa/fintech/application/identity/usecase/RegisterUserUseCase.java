package com.cooperativa.fintech.application.identity.usecase;

import com.cooperativa.fintech.application.identity.dto.RegisterUserRequest;
import com.cooperativa.fintech.application.identity.dto.UserResponse;
import com.cooperativa.fintech.application.identity.mapper.UserMapper;
import com.cooperativa.fintech.domain.common.exception.BusinessRuleException;
import com.cooperativa.fintech.domain.identity.model.KycStatus;
import com.cooperativa.fintech.domain.identity.model.User;
import com.cooperativa.fintech.domain.identity.model.UserStatus;
import com.cooperativa.fintech.domain.identity.port.EncryptionPort;
import com.cooperativa.fintech.domain.identity.port.UserRepositoryPort;
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
                .status(UserStatus.PENDING_VERIFICATION)
                .kycStatus(KycStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }
}
