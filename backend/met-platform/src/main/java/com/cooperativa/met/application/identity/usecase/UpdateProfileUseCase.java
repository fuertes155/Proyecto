package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.application.identity.dto.UpdateProfileRequest;
import com.cooperativa.met.application.identity.dto.UserResponse;
import com.cooperativa.met.application.identity.mapper.UserMapper;
import com.cooperativa.met.domain.common.exception.ResourceNotFoundException;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final UserRepositoryPort userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse execute(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
                
        User updatedUser = user.withProfile(request.getEmail(), request.getPhone());
        User savedUser = userRepository.save(updatedUser);
        
        return userMapper.toResponse(savedUser);
    }
}
