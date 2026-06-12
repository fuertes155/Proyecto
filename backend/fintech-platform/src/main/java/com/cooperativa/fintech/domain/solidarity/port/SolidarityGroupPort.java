package com.cooperativa.fintech.domain.solidarity.port;

import com.cooperativa.fintech.domain.solidarity.model.SolidarityGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolidarityGroupPort {

    SolidarityGroup save(SolidarityGroup group);

    Optional<SolidarityGroup> findById(UUID id);

    Optional<SolidarityGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    List<SolidarityGroup> findByUserId(UUID userId);
}
