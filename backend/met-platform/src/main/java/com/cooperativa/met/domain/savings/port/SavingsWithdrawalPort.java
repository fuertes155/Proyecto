package com.cooperativa.met.domain.savings.port;

import com.cooperativa.met.domain.savings.model.SavingsWithdrawal;

public interface SavingsWithdrawalPort {
    SavingsWithdrawal save(SavingsWithdrawal withdrawal);
}
