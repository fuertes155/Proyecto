package com.cooperativa.fintech.domain.solidarity.port;

import com.cooperativa.fintech.domain.solidarity.model.MicroLoan;
import com.cooperativa.fintech.domain.solidarity.model.MicroLoanStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MicroLoanPort {

    MicroLoan save(MicroLoan loan);

    Optional<MicroLoan> findById(UUID id);

    List<MicroLoan> findByGroupId(UUID groupId);

    boolean hasActiveLoan(UUID groupId, UUID borrowerId);

    List<MicroLoan> findByGroupIdAndStatus(UUID groupId, MicroLoanStatus status);
}
