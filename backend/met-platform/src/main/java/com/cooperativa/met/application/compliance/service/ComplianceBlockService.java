package com.cooperativa.met.application.compliance.service;

import com.cooperativa.met.domain.identity.model.KycStatus;
import com.cooperativa.met.domain.identity.model.UserStatus;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Bloquea a un usuario por coincidencia en lista restrictiva en su PROPIA
 * transacción. Necesario porque el llamador (ej. RegisterBiometricUseCase)
 * lanza una excepción justo después para abortar su propio flujo — si el
 * bloqueo viviera en esa misma transacción, se revertiría junto con ella y
 * el usuario quedaría activo pese al MATCH.
 */
@Service
@RequiredArgsConstructor
public class ComplianceBlockService {

    private final UserRepositoryPort userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void blockUser(UUID userId) {
        userRepository.findById(userId).ifPresent(user ->
                userRepository.save(user.withStatus(UserStatus.BLOCKED).withKycStatus(KycStatus.REJECTED)));
    }
}
