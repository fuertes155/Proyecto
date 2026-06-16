CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_type   VARCHAR(10)  NOT NULL,
    document_number VARCHAR(20)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    pin_hash        VARCHAR(255),
    biometric_hash  VARCHAR(255),
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    kyc_status      VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_document UNIQUE (document_type, document_number),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_kyc_status ON users (kyc_status);

CREATE TABLE biometric_registrations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    document_image  TEXT,
    selfie_image    TEXT,
    liveness_score  DECIMAL(5, 4),
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_biometric_user ON biometric_registrations (user_id);

CREATE TABLE compliance_checks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    list_type       VARCHAR(20)  NOT NULL,
    result          VARCHAR(20)  NOT NULL,
    checked_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    details         JSONB
);

CREATE INDEX idx_compliance_user ON compliance_checks (user_id);
CREATE INDEX idx_compliance_list ON compliance_checks (list_type, result);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (token_hash);
