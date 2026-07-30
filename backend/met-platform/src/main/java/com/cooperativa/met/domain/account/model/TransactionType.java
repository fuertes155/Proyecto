package com.cooperativa.met.domain.account.model;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    EXTERNAL_PAYOUT,    // Transferencia hacia una cuenta bancaria externa (riel de payout)
    INVESTMENT_FUNDING, // Debit side of the ledger
    LOAN_DISBURSEMENT   // Credit side of the ledger
}
