CREATE SEQUENCE IF NOT EXISTS mortuary_chamber_seq START WITH 1;

CREATE TABLE IF NOT EXISTS mortuary_chambers (
    id BIGINT PRIMARY KEY DEFAULT nextval('mortuary_chamber_seq'),
    chamber_no VARCHAR(50) NOT NULL UNIQUE,
    body_name VARCHAR(255),
    status VARCHAR(50) DEFAULT 'AVAILABLE',
    admitted_at TIMESTAMP,
    death_cert_no VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_mortuary_chambers_status ON mortuary_chambers(status);
