-- ============================================================================
-- V5 — BCMA Module (M30): Five-Rights Verification, IV Push, Exceptions, Stock
-- ============================================================================
SET search_path TO ihms_bcma, public;

CREATE SEQUENCE IF NOT EXISTS ihms_bcma.t_bcma_verification_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bcma.t_verification_log_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bcma.t_ivp_administration_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bcma.t_ivp_drug_config_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bcma.t_bcma_exception_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bcma.t_override_reason_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bcma.t_medication_stock_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bcma.t_stock_alert_seq START WITH 1;

CREATE TABLE IF NOT EXISTS ihms_bcma.t_bcma_verification (
    bcma_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    patient_id BIGINT NOT NULL,
    drug_ndc VARCHAR(50) NOT NULL,
    encounter_id BIGINT,
    scan_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    right_patient BOOLEAN NOT NULL DEFAULT FALSE,
    right_drug BOOLEAN NOT NULL DEFAULT FALSE,
    right_dose BOOLEAN NOT NULL DEFAULT FALSE,
    right_route BOOLEAN NOT NULL DEFAULT FALSE,
    right_time BOOLEAN NOT NULL DEFAULT FALSE,
    all_rights_passed BOOLEAN NOT NULL DEFAULT FALSE,
    overridden BOOLEAN NOT NULL DEFAULT FALSE,
    override_reason VARCHAR(500),
    override_by BIGINT,
    administered BOOLEAN NOT NULL DEFAULT FALSE,
    administered_at TIMESTAMP(3),
    administered_by BIGINT,
    wristband_barcode VARCHAR(100),
    CONSTRAINT pk_t_bcma_verification PRIMARY KEY (bcma_id),
    CONSTRAINT ck_bcma_all_passed CHECK (all_rights_passed = (right_patient AND right_drug AND right_dose AND right_route AND right_time))
);
COMMENT ON TABLE ihms_bcma.t_bcma_verification IS 'BCMA five-rights verification. Enforced check: all_rights_passed = AND of five rights.';

CREATE TABLE IF NOT EXISTS ihms_bcma.t_verification_log (
    log_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    bcma_id BIGINT NOT NULL REFERENCES ihms_bcma.t_bcma_verification(bcma_id),
    patient_id BIGINT NOT NULL,
    verification_event VARCHAR(50) NOT NULL,
    result BOOLEAN NOT NULL DEFAULT FALSE,
    scanned_by BIGINT,
    scan_timestamp TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_t_verification_log PRIMARY KEY (log_id)
);
COMMENT ON TABLE ihms_bcma.t_verification_log IS 'Audit trail for five-rights verification events.';

CREATE TABLE IF NOT EXISTS ihms_bcma.t_ivp_administration (
    ivp_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    patient_id BIGINT NOT NULL,
    drug_name VARCHAR(200) NOT NULL,
    dose_amount NUMERIC(10,3),
    dose_unit VARCHAR(20),
    rate_ml_per_min NUMERIC(8,2),
    dilution_required BOOLEAN NOT NULL DEFAULT FALSE,
    dilution_volume_ml NUMERIC(8,2),
    administered_by BIGINT,
    administration_time TIMESTAMP(3),
    status VARCHAR(20) DEFAULT 'PENDING',
    ward_id BIGINT,
    CONSTRAINT pk_t_ivp_administration PRIMARY KEY (ivp_id),
    CONSTRAINT ck_ivp_status CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED'))
);
COMMENT ON TABLE ihms_bcma.t_ivp_administration IS 'IV push administration record with rate and dilution verification.';

CREATE TABLE IF NOT EXISTS ihms_bcma.t_ivp_drug_config (
    config_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    drug_ndc VARCHAR(50) NOT NULL,
    max_rate_ml_per_min NUMERIC(8,2),
    requires_dilution BOOLEAN NOT NULL DEFAULT FALSE,
    standard_dilution_volume_ml NUMERIC(8,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_t_ivp_drug_config PRIMARY KEY (config_id),
    CONSTRAINT uq_ivp_config_ndc UNIQUE (drug_ndc)
);
COMMENT ON TABLE ihms_bcma.t_ivp_drug_config IS 'IV push drug configuration: rate limits and dilution rules.';

CREATE TABLE IF NOT EXISTS ihms_bcma.t_bcma_exception (
    exception_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    patient_id BIGINT NOT NULL,
    drug_ndc VARCHAR(50),
    exception_type VARCHAR(50) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    override_nurse BIGINT,
    exception_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    ward_id BIGINT,
    scanned_by BIGINT,
    overridden BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_bcma_exception PRIMARY KEY (exception_id),
    CONSTRAINT ck_bcma_exception_type CHECK (exception_type IN ('OVERRIDE','CLINICAL_JUDGMENT','SYSTEM_ERROR','MANUAL_ENTRY'))
);
COMMENT ON TABLE ihms_bcma.t_bcma_exception IS 'Exception / override workflow for BCMA with clinical justification.';

CREATE TABLE IF NOT EXISTS ihms_bcma.t_override_reason (
    reason_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    code VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_t_override_reason PRIMARY KEY (reason_id),
    CONSTRAINT uq_override_code UNIQUE (code)
);
COMMENT ON TABLE ihms_bcma.t_override_reason IS 'Standardized override reason codes for clinical justification.';

CREATE TABLE IF NOT EXISTS ihms_bcma.t_medication_stock (
    stock_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    drug_ndc VARCHAR(50) NOT NULL,
    batch_id VARCHAR(50),
    quantity_on_hand NUMERIC(10,3),
    expiry_date DATE,
    storage_location VARCHAR(100),
    last_updated TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_t_medication_stock PRIMARY KEY (stock_id)
);
COMMENT ON TABLE ihms_bcma.t_medication_stock IS 'Real-time medication stock with batch tracking and expiry.';

CREATE TABLE IF NOT EXISTS ihms_bcma.t_stock_alert (
    alert_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    drug_ndc VARCHAR(50) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    message VARCHAR(200),
    severity VARCHAR(20) DEFAULT 'WARNING',
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP(3),
    CONSTRAINT pk_t_stock_alert PRIMARY KEY (alert_id),
    CONSTRAINT ck_stock_alert_type CHECK (alert_type IN ('LOW_STOCK','EXPIRY','BATCH_RECALL','FULL'))
);
COMMENT ON TABLE ihms_bcma.t_stock_alert IS 'Stock alerts: low stock, expiry, batch recall.';
