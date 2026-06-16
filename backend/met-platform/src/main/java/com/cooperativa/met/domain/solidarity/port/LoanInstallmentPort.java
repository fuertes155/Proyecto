package com.cooperativa.met.domain.solidarity.port;

import com.cooperativa.met.domain.solidarity.model.LoanInstallment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanInstallmentPort {

    List<LoanInstallment> saveAll(List<LoanInstallment> installments);

    LoanInstallment save(LoanInstallment installment);

    List<LoanInstallment> findByLoanId(UUID loanId);

    Optional<LoanInstallment> findById(UUID id);

    boolean allPaid(UUID loanId);
}
