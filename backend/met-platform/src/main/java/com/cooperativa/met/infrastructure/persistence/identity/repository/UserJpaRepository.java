package com.cooperativa.met.infrastructure.persistence.identity.repository;

import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.infrastructure.persistence.identity.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

    boolean existsByEmail(String email);
}
