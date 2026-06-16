package com.cooperativa.met.domain.solidarity.port;

import com.cooperativa.met.domain.solidarity.model.PoolTransaction;

import java.util.List;
import java.util.UUID;

public interface PoolTransactionPort {

    PoolTransaction save(PoolTransaction transaction);

    List<PoolTransaction> findByGroupId(UUID groupId);
}
