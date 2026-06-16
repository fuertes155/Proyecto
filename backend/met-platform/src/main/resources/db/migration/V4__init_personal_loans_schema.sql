CREATE TABLE personal_loan_applications (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount              DECIMAL(18, 2) NOT NULL,
    term_months         SMALLINT       NOT NULL,
    annual_interest_rate DECIMAL(7, 4) NOT NULL,
    monthly_payment     DECIMAL(18, 2) NOT NULL,
    total_interest      DECIMAL(18, 2) NOT NULL,
    total_payment       DECIMAL(18, 2) NOT NULL,
    purpose             VARCHAR(255)   NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'SUBMITTED',
    rejection_reason    VARCHAR(255),
    submitted_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_loan_amount CHECK (amount >= 500000),
    CONSTRAINT chk_loan_term CHECK (term_months BETWEEN 6 AND 60)
);

CREATE INDEX idx_personal_loans_user ON personal_loan_applications (user_id);
CREATE INDEX idx_personal_loans_status ON personal_loan_applications (status);

CREATE TABLE personal_loan_amortization (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id      UUID           NOT NULL REFERENCES personal_loan_applications (id) ON DELETE CASCADE,
    installment_number  SMALLINT       NOT NULL,
    payment_amount      DECIMAL(18, 2) NOT NULL,
    principal_amount    DECIMAL(18, 2) NOT NULL,
    interest_amount     DECIMAL(18, 2) NOT NULL,
    remaining_balance   DECIMAL(18, 2) NOT NULL,
    due_date            DATE           NOT NULL
);

CREATE INDEX idx_amortization_application ON personal_loan_amortization (application_id);
