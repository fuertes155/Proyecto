package com.cooperativa.met.domain.lending.port;

import com.cooperativa.met.domain.lending.model.PersonalLoanApplication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalLoanApplicationPort {

    PersonalLoanApplication save(PersonalLoanApplication application);

    Optional<PersonalLoanApplication> findById(UUID id);

    Optional<PersonalLoanApplication> findByIdAndUserId(UUID id, UUID userId);

    List<PersonalLoanApplication> findByUserId(UUID userId);

    List<PersonalLoanApplication> findAll();

    List<PersonalLoanApplication> findByStatus(com.cooperativa.met.domain.lending.model.LoanApplicationStatus status);

    boolean hasPendingApplication(UUID userId);
}
