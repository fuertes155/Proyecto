package com.cooperativa.met.domain.admin.port;

import com.cooperativa.met.domain.admin.model.Admin;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepositoryPort {

    Optional<Admin> findByUsername(String username);

    Optional<Admin> findById(UUID id);

    Admin save(Admin admin);

    java.util.List<Admin> findAll();
}
