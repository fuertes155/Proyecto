CREATE TABLE solidarity_groups (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                VARCHAR(100)   NOT NULL,
    description         VARCHAR(500),
    creator_id          UUID           NOT NULL REFERENCES users (id),
    invite_code         VARCHAR(8)     NOT NULL,
    min_contribution    DECIMAL(18, 2) NOT NULL DEFAULT 10000.00,
    max_loan_percentage DECIMAL(5, 2)  NOT NULL DEFAULT 30.00,
    interest_rate       DECIMAL(5, 4)  NOT NULL DEFAULT 0.0050,
    pool_balance        DECIMAL(18, 2) NOT NULL DEFAULT 0,
    max_members         SMALLINT       NOT NULL DEFAULT 20,
    status              VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_solidarity_invite_code UNIQUE (invite_code),
    CONSTRAINT chk_min_contribution CHECK (min_contribution > 0),
    CONSTRAINT chk_max_loan_pct CHECK (max_loan_percentage > 0 AND max_loan_percentage <= 100)
);

CREATE INDEX idx_solidarity_groups_creator ON solidarity_groups (creator_id);
CREATE INDEX idx_solidarity_groups_status ON solidarity_groups (status);

CREATE TABLE solidarity_members (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id            UUID           NOT NULL REFERENCES solidarity_groups (id) ON DELETE CASCADE,
    user_id             UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role                VARCHAR(20)    NOT NULL DEFAULT 'MEMBER',
    total_contributed   DECIMAL(18, 2) NOT NULL DEFAULT 0,
    joined_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_solidarity_member UNIQUE (group_id, user_id)
);

CREATE INDEX idx_solidarity_members_user ON solidarity_members (user_id);

CREATE TABLE micro_loans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id        UUID           NOT NULL REFERENCES solidarity_groups (id),
    borrower_id     UUID           NOT NULL REFERENCES users (id),
    amount          DECIMAL(18, 2) NOT NULL,
    purpose         VARCHAR(255)   NOT NULL,
    term_months     SMALLINT       NOT NULL,
    interest_rate   DECIMAL(5, 4)  NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    requested_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMPTZ,
    reviewed_by     UUID REFERENCES users (id),
    disbursed_at    TIMESTAMPTZ,
    rejection_reason VARCHAR(255),
    CONSTRAINT chk_loan_amount CHECK (amount > 0),
    CONSTRAINT chk_term_months CHECK (term_months BETWEEN 1 AND 24)
);

CREATE INDEX idx_micro_loans_group ON micro_loans (group_id);
CREATE INDEX idx_micro_loans_borrower ON micro_loans (borrower_id);
CREATE INDEX idx_micro_loans_status ON micro_loans (status);

CREATE TABLE loan_installments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    loan_id             UUID           NOT NULL REFERENCES micro_loans (id) ON DELETE CASCADE,
    installment_number  SMALLINT       NOT NULL,
    principal_amount    DECIMAL(18, 2) NOT NULL,
    interest_amount     DECIMAL(18, 2) NOT NULL,
    total_amount        DECIMAL(18, 2) NOT NULL,
    due_date            DATE           NOT NULL,
    paid_at             TIMESTAMPTZ,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_installment_amounts CHECK (total_amount = principal_amount + interest_amount)
);

CREATE INDEX idx_installments_loan ON loan_installments (loan_id);
CREATE INDEX idx_installments_due ON loan_installments (due_date, status);

CREATE TABLE pool_transactions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id        UUID           NOT NULL REFERENCES solidarity_groups (id),
    member_id       UUID           REFERENCES solidarity_members (id),
    loan_id         UUID           REFERENCES micro_loans (id),
    type            VARCHAR(30)    NOT NULL,
    amount          DECIMAL(18, 2) NOT NULL,
    balance_after   DECIMAL(18, 2) NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pool_tx_group ON pool_transactions (group_id);
CREATE INDEX idx_pool_tx_created ON pool_transactions (created_at);
