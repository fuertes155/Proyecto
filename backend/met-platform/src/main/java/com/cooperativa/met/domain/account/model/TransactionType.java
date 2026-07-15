package com.cooperativa.met.domain.account.model;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    INVESTMENT_FUNDING, // Debit side of the ledger
    LOAN_DISBURSEMENT   // Credit side of the ledger
}
