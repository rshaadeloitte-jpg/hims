CREATE SEQUENCE IF NOT EXISTS dbt_claim_seq START WITH 1;

CREATE TABLE IF NOT EXISTS dbt_claims (
    id BIGINT PRIMARY KEY DEFAULT nextval('dbt_claim_seq'),
    beneficiary_name VARCHAR(255) NOT NULL,
    scheme VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(50),
    utr_no VARCHAR(100),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dbt_claims_status ON dbt_claims(status);
CREATE INDEX IF NOT EXISTS idx_dbt_claims_scheme ON dbt_claims(scheme);
