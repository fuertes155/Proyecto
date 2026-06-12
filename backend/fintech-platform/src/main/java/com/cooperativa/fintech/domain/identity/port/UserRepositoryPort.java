package com.cooperativa.fintech.domain.identity.port;

import com.cooperativa.fintech.domain.identity.model.DocumentType;
import com.cooperativa.fintech.domain.identity.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByDocument(DocumentType documentType, String documentNumber);

    Optional<User> findByEmail(String email);

    boolean existsByDocument(DocumentType documentType, String documentNumber);

    boolean existsByEmail(String email);
}
