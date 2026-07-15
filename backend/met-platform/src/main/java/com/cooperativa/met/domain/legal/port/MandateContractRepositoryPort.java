package com.cooperativa.met.domain.legal.port;

import com.cooperativa.met.domain.legal.model.MandateContract;

import java.util.Optional;
import java.util.UUID;

public interface MandateContractRepositoryPort {
    MandateContract save(MandateContract contract);
    Optional<MandateContract> findById(UUID id);
    Optional<MandateContract> findByUserId(UUID userId);
}
