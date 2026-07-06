package com.cooperativa.met.infrastructure.persistence.identity.adapter;

import com.cooperativa.met.domain.identity.model.DocumentType;
import com.cooperativa.met.domain.identity.model.User;
import com.cooperativa.met.domain.identity.port.UserRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.identity.mapper.UserPersistenceMapper;
import com.cooperativa.met.infrastructure.persistence.identity.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        return mapper.toDomain(repository.save(mapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByDocument(DocumentType documentType, String documentNumber) {
        return repository.findByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return repository.findByPhone(phone).map(mapper::toDomain);
    }

    @Override
    public boolean existsByDocument(DocumentType documentType, String documentNumber) {
        return repository.existsByDocumentTypeAndDocumentNumber(documentType, documentNumber);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}
