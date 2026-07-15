CREATE TABLE mandate_contracts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    document_number VARCHAR(50) NOT NULL,
    pdf_content BYTEA NOT NULL,
    pdf_hash_sha256 VARCHAR(64) NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(255) NOT NULL,
    otp_transaction_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL
);
