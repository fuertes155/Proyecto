package com.cooperativa.fintech.domain.identity.port;

import com.cooperativa.fintech.domain.identity.model.BiometricRegistration;

import java.util.Optional;
import java.util.UUID;

public interface BiometricRegistrationPort {

    BiometricRegistration save(BiometricRegistration registration);

    Optional<BiometricRegistration> findLatestByUserId(UUID userId);
}
