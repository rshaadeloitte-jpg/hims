CREATE SEQUENCE IF NOT EXISTS pmrd_form_seq START WITH 1;

CREATE TABLE IF NOT EXISTS pmrd_forms (
    id BIGINT PRIMARY KEY DEFAULT nextval('pmrd_form_seq'),
    form_type VARCHAR(20) NOT NULL,
    beneficiary_name VARCHAR(255),
    status VARCHAR(50) DEFAULT 'DRAFT',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pmrd_forms_type ON pmrd_forms(form_type);
CREATE INDEX IF NOT EXISTS idx_pmrd_forms_status ON pmrd_forms(status);
