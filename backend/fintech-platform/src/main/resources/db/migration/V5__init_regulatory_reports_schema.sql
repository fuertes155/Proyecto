CREATE TABLE regulatory_reports (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_type     VARCHAR(30)  NOT NULL,
    period_year     SMALLINT     NOT NULL,
    period_month    SMALLINT     NOT NULL,
    entity_code     VARCHAR(10)  NOT NULL DEFAULT 'COOP001',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    file_name       VARCHAR(255),
    file_path       VARCHAR(500),
    file_size_bytes BIGINT,
    record_count    INTEGER      NOT NULL DEFAULT 0,
    checksum_sha256 VARCHAR(64),
    generated_by    UUID REFERENCES users (id),
    error_message   VARCHAR(500),
    generated_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_report_period UNIQUE (report_type, period_year, period_month),
    CONSTRAINT chk_period_month CHECK (period_month BETWEEN 1 AND 12)
);

CREATE INDEX idx_regulatory_reports_period ON regulatory_reports (period_year, period_month);
CREATE INDEX idx_regulatory_reports_status ON regulatory_reports (status);
CREATE INDEX idx_regulatory_reports_type ON regulatory_reports (report_type);
