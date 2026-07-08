package com.cooperativa.met.application.identity.usecase;

import com.cooperativa.met.domain.common.exception.BusinessRuleException;
import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserUseCase {

    private final UserRepositoryPort userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateKycStatus(UUID userId, KycStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Usuario no encontrado"));
        
        User updated = user.withKycStatus(status);
        return userRepository.save(updated);
    }
}
