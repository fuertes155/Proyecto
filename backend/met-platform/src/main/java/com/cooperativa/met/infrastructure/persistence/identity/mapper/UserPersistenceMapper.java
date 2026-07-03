package com.cooperativa.met.infrastructure.persistence.identity.mapper;

import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.infrastructure.persistence.identity.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .documentType(entity.getDocumentType())
                .documentNumber(entity.getDocumentNumber())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .pinHash(entity.getPinHash())
                .biometricHash(entity.getBiometricHash())
                .status(entity.getStatus())
                .kycStatus(entity.getKycStatus())
                .termsAccepted(entity.isTermsAccepted())
                .termsAcceptedAt(entity.getTermsAcceptedAt())
                .emailNotificationsEnabled(entity.isEmailNotificationsEnabled())
                .pushNotificationsEnabled(entity.isPushNotificationsEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setDocumentType(user.getDocumentType());
        entity.setDocumentNumber(user.getDocumentNumber());
        entity.setEmail(user.getEmail());
        entity.setPhone(user.getPhone());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setPinHash(user.getPinHash());
        entity.setBiometricHash(user.getBiometricHash());
        entity.setStatus(user.getStatus());
        entity.setKycStatus(user.getKycStatus());
        entity.setTermsAccepted(user.isTermsAccepted());
        entity.setTermsAcceptedAt(user.getTermsAcceptedAt());
        entity.setEmailNotificationsEnabled(user.isEmailNotificationsEnabled());
        entity.setPushNotificationsEnabled(user.isPushNotificationsEnabled());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}
