package com.cooperativa.fintech.domain.savings.port;

import com.cooperativa.fintech.domain.savings.model.ScheduledContribution;

import java.util.List;
import java.util.UUID;

public interface ScheduledContributionPort {

    ScheduledContribution save(ScheduledContribution contribution);

    List<ScheduledContribution> findByAccountId(UUID accountId);
}
