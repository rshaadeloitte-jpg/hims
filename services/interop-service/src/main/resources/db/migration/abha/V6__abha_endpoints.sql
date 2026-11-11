CREATE SEQUENCE IF NOT EXISTS abha_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE hpr_registration (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    abha_id VARCHAR(255),
    health_id VARCHAR(255),
    name VARCHAR(255),
    gender VARCHAR(50),
    date_of_birth VARCHAR(50),
    mobile VARCHAR(20),
    email VARCHAR(255),
    status VARCHAR(50)
);

CREATE TABLE hpr_verification (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    verification_type VARCHAR(255),
    verification_value VARCHAR(255),
    status VARCHAR(50),
    remarks TEXT
);

CREATE TABLE ehr_file (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    abha_id VARCHAR(255),
    document_type VARCHAR(255),
    document_reference VARCHAR(500),
    status VARCHAR(50),
    file_path VARCHAR(1000)
);

CREATE TABLE ehr_consent (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    consent_request_id VARCHAR(255),
    abha_id VARCHAR(255),
    hip_id VARCHAR(255),
    hiu_id VARCHAR(255),
    status VARCHAR(50),
    purpose VARCHAR(500)
);

CREATE TABLE eaushadhi_drug (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    drug_code VARCHAR(100),
    drug_name VARCHAR(500),
    manufacturer VARCHAR(500),
    dosage_form VARCHAR(100),
    strength VARCHAR(100),
    is_generic BOOLEAN DEFAULT FALSE
);

CREATE TABLE generic_substitute (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    brand_drug_code VARCHAR(100),
    generic_drug_code VARCHAR(100),
    substitution_ratio NUMERIC(10, 4)
);

CREATE TABLE fhir_resource (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    content TEXT,
    status VARCHAR(50)
);

CREATE TABLE fhir_bundle (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    bundle_type VARCHAR(100),
    content TEXT,
    total_entries INTEGER DEFAULT 0
);

CREATE TABLE whatsapp_notification (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    mobile VARCHAR(20),
    template_id VARCHAR(255),
    status VARCHAR(50),
    failure_reason TEXT
);

CREATE TABLE notification_queue (
    id BIGINT PRIMARY KEY DEFAULT nextval('abha_seq'),
    added_by VARCHAR(255) NOT NULL,
    added_datetime TIMESTAMP NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_datetime TIMESTAMP NOT NULL,
    notification_type VARCHAR(255),
    payload TEXT,
    status VARCHAR(50),
    retry_count INTEGER DEFAULT 0
);