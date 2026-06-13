CREATE TABLE scheduled_savings_accounts (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                 UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                    VARCHAR(100)   NOT NULL,
    target_amount           DECIMAL(18, 2),
    contribution_amount     DECIMAL(18, 2) NOT NULL,
    frequency               VARCHAR(20)    NOT NULL,
    debit_day_of_week       INTEGER,
    debit_day_of_month      INTEGER,
    current_balance         DECIMAL(18, 2) NOT NULL DEFAULT 0,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    next_contribution_date  DATE           NOT NULL,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_contribution_amount_positive CHECK (contribution_amount > 0),
    CONSTRAINT chk_target_amount_positive CHECK (target_amount IS NULL OR target_amount > 0),
    CONSTRAINT chk_debit_day_of_week CHECK (debit_day_of_week IS NULL OR debit_day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_debit_day_of_month CHECK (debit_day_of_month IS NULL OR debit_day_of_month BETWEEN 1 AND 28)
);

CREATE INDEX idx_scheduled_savings_user ON scheduled_savings_accounts (user_id);
CREATE INDEX idx_scheduled_savings_status ON scheduled_savings_accounts (status);
CREATE INDEX idx_scheduled_savings_next_date ON scheduled_savings_accounts (next_contribution_date, status);

CREATE TABLE scheduled_contributions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id      UUID           NOT NULL REFERENCES scheduled_savings_accounts (id) ON DELETE CASCADE,
    amount          DECIMAL(18, 2) NOT NULL,
    scheduled_date  DATE           NOT NULL,
    executed_at     TIMESTAMPTZ,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_contribution_amount CHECK (amount > 0)
);

CREATE INDEX idx_contributions_account ON scheduled_contributions (account_id);
CREATE INDEX idx_contributions_scheduled ON scheduled_contributions (scheduled_date, status);

