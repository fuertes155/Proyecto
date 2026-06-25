package com.cooperativa.met.infrastructure.persistence.admin.adapter;

import com.cooperativa.met.domain.admin.model.Admin;
import com.cooperativa.met.domain.admin.port.AdminRepositoryPort;
import com.cooperativa.met.infrastructure.persistence.admin.entity.AdminJpaEntity;
import com.cooperativa.met.infrastructure.persistence.admin.repository.AdminJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminRepositoryAdapter implements AdminRepositoryPort {

    private final AdminJpaRepository jpaRepository;

    @Override
    public Optional<Admin> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<Admin> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Admin save(Admin admin) {
        AdminJpaEntity entity = toEntity(admin);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Admin> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    private Admin toDomain(AdminJpaEntity e) {
        return Admin.builder()
                .id(e.getId())
                .username(e.getUsername())
                .passwordHash(e.getPasswordHash())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .role(e.getRole())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private AdminJpaEntity toEntity(Admin a) {
        AdminJpaEntity e = new AdminJpaEntity();
        e.setId(a.getId() != null ? a.getId() : UUID.randomUUID());
        e.setUsername(a.getUsername());
        e.setPasswordHash(a.getPasswordHash());
        e.setFullName(a.getFullName());
        e.setEmail(a.getEmail());
        e.setRole(a.getRole());
        e.setStatus(a.getStatus());
        e.setCreatedAt(a.getCreatedAt() != null ? a.getCreatedAt() : Instant.now());
        e.setUpdatedAt(a.getUpdatedAt() != null ? a.getUpdatedAt() : Instant.now());
        return e;
    }
}
