package com.cooperativa.met.infrastructure.persistence.identity.mapper;

import com.cooperativa.met.domain.identity.model.BiometricRegistration;
import com.cooperativa.met.infrastructure.persistence.identity.entity.BiometricRegistrationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class BiometricPersistenceMapper {

    public BiometricRegistration toDomain(BiometricRegistrationJpaEntity entity) {
        return BiometricRegistration.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .documentImage(entity.getDocumentImage())
                .selfieImage(entity.getSelfieImage())
                .livenessScore(entity.getLivenessScore())
                .verified(entity.isVerified())
                .verifiedAt(entity.getVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public BiometricRegistrationJpaEntity toEntity(BiometricRegistration registration) {
        BiometricRegistrationJpaEntity entity = new BiometricRegistrationJpaEntity();
        entity.setId(registration.getId());
        entity.setUserId(registration.getUserId());
        entity.setDocumentImage(registration.getDocumentImage());
        entity.setSelfieImage(registration.getSelfieImage());
        entity.setLivenessScore(registration.getLivenessScore());
        entity.setVerified(registration.isVerified());
        entity.setVerifiedAt(registration.getVerifiedAt());
        entity.setCreatedAt(registration.getCreatedAt());
        return entity;
    }
}
