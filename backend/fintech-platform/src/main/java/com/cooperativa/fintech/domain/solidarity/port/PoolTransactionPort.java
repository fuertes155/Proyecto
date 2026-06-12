package com.cooperativa.fintech.domain.solidarity.port;

import com.cooperativa.fintech.domain.solidarity.model.PoolTransaction;

import java.util.List;
import java.util.UUID;

public interface PoolTransactionPort {

    PoolTransaction save(PoolTransaction transaction);

    List<PoolTransaction> findByGroupId(UUID groupId);
}
