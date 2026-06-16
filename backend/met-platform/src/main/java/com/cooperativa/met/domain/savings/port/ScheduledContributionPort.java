package com.cooperativa.met.domain.savings.port;

import com.cooperativa.met.domain.savings.model.ScheduledContribution;

import java.util.List;
import java.util.UUID;

public interface ScheduledContributionPort {

    ScheduledContribution save(ScheduledContribution contribution);

    List<ScheduledContribution> findByAccountId(UUID accountId);
}
