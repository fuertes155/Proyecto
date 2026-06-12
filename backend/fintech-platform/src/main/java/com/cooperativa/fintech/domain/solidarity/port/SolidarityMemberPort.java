package com.cooperativa.fintech.domain.solidarity.port;

import com.cooperativa.fintech.domain.solidarity.model.SolidarityMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolidarityMemberPort {

    SolidarityMember save(SolidarityMember member);

    Optional<SolidarityMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    List<SolidarityMember> findByGroupId(UUID groupId);

    int countByGroupId(UUID groupId);
}
