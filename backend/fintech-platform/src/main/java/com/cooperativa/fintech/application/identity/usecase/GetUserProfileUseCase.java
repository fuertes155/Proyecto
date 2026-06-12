package com.cooperativa.fintech.application.identity.usecase;

import com.cooperativa.fintech.application.identity.dto.UserResponse;
import com.cooperativa.fintech.application.identity.mapper.UserMapper;
import com.cooperativa.fintech.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.fintech.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserProfileUseCase {

    private final UserRepositoryPort userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse execute(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}
