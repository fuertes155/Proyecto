package com.cooperativa.met.domain.admin.port;

import com.cooperativa.met.domain.admin.model.OperationLimit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationLimitRepositoryPort {
    List<OperationLimit> findAll();
    Optional<OperationLimit> findByTipo(String tipoOperacion);
    OperationLimit save(OperationLimit limit);
}
