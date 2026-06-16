package com.cooperativa.met.domain.identity.port;

import com.cooperativa.met.domain.identity.model.BiometricRegistration;

import java.util.Optional;
import java.util.UUID;

public interface BiometricRegistrationPort {

    BiometricRegistration save(BiometricRegistration registration);

    Optional<BiometricRegistration> findLatestByUserId(UUID userId);
}
