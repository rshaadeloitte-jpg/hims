-- ============================================================================
-- V3 — KDL/machine interface, critical alert delivery, report print templates
-- (GAP-REGISTER WP-7 HIGH priority — KDL protocol parsing, critical alert
-- fan-out delivery tracking, 201 legacy print template storage/retrieval)
-- Ported from legacy:
--   - KDL: LabKDLResultListDto, LabLoadListForKDLDto, ReleaseAllLabKDLDto
--   - Critical alert: NotifyPatientCriticalDetails (informPassedTo/By, ward, bed, email)
--   - Print templates: M_PATHTEMPLATEMASTER (code, description, template blob, isclinical),
--     M_PATHOTEMPLATEMASTER (code, description, template text, genderId, serviceId, organId)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_lab;

SET search_path TO ihms_lab, public;

-- ---------------------------------------------------------------------------
-- t_kdl_result — stores parsed KDL/machine protocol results before mapping
-- to patho parameter rows. Raw payload from instrument (HL7/ASTM/file-drop).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_lab.t_kdl_result (
    kdl_result_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    machine_id BIGINT REFERENCES ihms_lab.t_machineconfiguration (machine_id),
    protocol VARCHAR(50) NOT NULL,
    raw_payload TEXT NOT NULL,
    parsed_payload JSONB,
    lab_number VARCHAR(50),
    mrn_no VARCHAR(50),
    patient_name VARCHAR(200),
    test_name VARCHAR(200),
    sample_status VARCHAR(50),
    is_mapped BOOLEAN NOT NULL DEFAULT FALSE,
    mapped_detail_id BIGINT,
    received_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_t_kdl_result PRIMARY KEY (kdl_result_id)
);
CREATE INDEX IF NOT EXISTS ix_t_kdl_result_machine ON ihms_lab.t_kdl_result (machine_id);
CREATE INDEX IF NOT EXISTS ix_t_kdl_result_lab_number ON ihms_lab.t_kdl_result (lab_number);
CREATE INDEX IF NOT EXISTS ix_t_kdl_result_mrn ON ihms_lab.t_kdl_result (mrn_no);
CREATE INDEX IF NOT EXISTS ix_t_kdl_result_unmapped ON ihms_lab.t_kdl_result (is_mapped) WHERE is_mapped = FALSE;
COMMENT ON TABLE ihms_lab.t_kdl_result IS 'Parsed KDL/machine protocol results. Raw instrument payload stored before mapping to patho parameter rows (Catalog #7 machine interface).';

-- ---------------------------------------------------------------------------
-- t_critical_alert_delivery — enhanced fan-out tracking for critical alerts
-- (WhatsApp/SMS/email delivery status per NotifyPatientCriticalDetails)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_lab.t_critical_alert_delivery (
    delivery_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    booking_id BIGINT NOT NULL,
    detail_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT,
    mrn_no VARCHAR(50),
    patient_name VARCHAR(200),
    test_id BIGINT,
    test_name VARCHAR(200),
    result_value VARCHAR(500),
    reference_range VARCHAR(100),
    inform_passed_to VARCHAR(200),
    inform_passed_by VARCHAR(200),
    doc_id BIGINT,
    ward VARCHAR(100),
    bed_no VARCHAR(50),
    email_id VARCHAR(200),
    channel VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    delivered_at TIMESTAMP(3),
    failure_reason VARCHAR(500),
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_critical_alert_delivery PRIMARY KEY (delivery_id),
    CONSTRAINT ck_t_critical_alert_delivery_channel CHECK (channel IN ('WHATSAPP', 'SMS', 'EMAIL', 'CALL')),
    CONSTRAINT ck_t_critical_alert_delivery_status CHECK (delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'RETRY'))
);
CREATE INDEX IF NOT EXISTS ix_t_critical_alert_delivery_booking ON ihms_lab.t_critical_alert_delivery (booking_id);
CREATE INDEX IF NOT EXISTS ix_t_critical_alert_delivery_patient ON ihms_lab.t_critical_alert_delivery (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_critical_alert_delivery_status ON ihms_lab.t_critical_alert_delivery (delivery_status) WHERE delivery_status IN ('PENDING', 'FAILED', 'RETRY');
COMMENT ON TABLE ihms_lab.t_critical_alert_delivery IS 'Enhanced fan-out delivery tracking for critical alerts (WhatsApp/SMS/email). Ported from NotifyPatientCriticalDetails.';

-- ---------------------------------------------------------------------------
-- t_report_print_template — 201 legacy print templates storage
-- (M_PATHTEMPLATEMASTER + M_PATHOTEMPLATEMASTER unified)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_lab.t_report_print_template (
    template_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    template_content TEXT,
    template_blob BYTEA,
    gender_id BIGINT,
    service_id BIGINT,
    organ_id BIGINT,
    is_clinical BOOLEAN NOT NULL DEFAULT FALSE,
    template_type VARCHAR(20) NOT NULL DEFAULT 'REPORT',
    CONSTRAINT pk_t_report_print_template PRIMARY KEY (template_id),
    CONSTRAINT uq_t_report_print_template_code UNIQUE (code),
    CONSTRAINT ck_t_report_print_template_type CHECK (template_type IN ('REPORT', 'LABEL', 'RECEIPT', 'BARCODE'))
);
CREATE INDEX IF NOT EXISTS ix_t_report_print_template_type ON ihms_lab.t_report_print_template (template_type);
CREATE INDEX IF NOT EXISTS ix_t_report_print_template_gender ON ihms_lab.t_report_print_template (gender_id);
CREATE INDEX IF NOT EXISTS ix_t_report_print_template_service ON ihms_lab.t_report_print_template (service_id);
COMMENT ON TABLE ihms_lab.t_report_print_template IS 'Report print template storage (201 legacy templates). Ported from M_PATHTEMPLATEMASTER (blob) + M_PATHOTEMPLATEMASTER (text). template_type: REPORT/LABEL/RECEIPT/BARCODE.';

-- ---------------------------------------------------------------------------
-- Sequences (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_lab.t_kdl_result_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_lab.t_critical_alert_delivery_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_lab.t_report_print_template_seq START WITH 1;
