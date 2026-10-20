-- ============================================================================
-- V1 — ihms_kiosk schema (Kiosk Service, M31) — self-registration,
-- self-appointment, kiosk queue, lab result upload.
-- Clean target of legacy kiosk / self-registration modules.
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS ihms_kiosk;
SET search_path TO ihms_kiosk, public;

CREATE TABLE IF NOT EXISTS ihms_kiosk.kiosk_registration (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_name VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    email VARCHAR(128),
    age INTEGER,
    gender VARCHAR(16),
    registration_date DATE NOT NULL DEFAULT CURRENT_DATE,
    registration_token VARCHAR(64) NOT NULL,
    CONSTRAINT pk_kiosk_reg PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_kiosk_reg_token ON ihms_kiosk.kiosk_registration (registration_token);

CREATE TABLE IF NOT EXISTS ihms_kiosk.kiosk_appointment (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT,
    department_id BIGINT,
    appointment_date DATE NOT NULL,
    slot_start TIME,
    slot_end TIME,
    visit_type VARCHAR(32) DEFAULT 'SELF_APPOINTMENT',
    status_code SMALLINT NOT NULL DEFAULT 1,
    token_no INTEGER,
    CONSTRAINT pk_kiosk_appt PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ihms_kiosk.kiosk_queue (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    department_id BIGINT,
    queue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    queue_status SMALLINT NOT NULL DEFAULT 1,
    priority SMALLINT NOT NULL DEFAULT 1,
    token_no BIGINT NOT NULL,
    called_at TIMESTAMP(3),
    completed_at TIMESTAMP(3),
    CONSTRAINT pk_kiosk_queue PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_kiosk_queue_date_token ON ihms_kiosk.kiosk_queue (queue_date, token_no);

CREATE TABLE IF NOT EXISTS ihms_kiosk.kiosk_lab_result (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    test_order_id BIGINT,
    result_data TEXT,
    file_token VARCHAR(64),
    uploaded_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_kiosk_lab PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS ihms_kiosk."kiosk_reg_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_kiosk."kiosk_appt_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_kiosk."kiosk_queue_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_kiosk."kiosk_lab_seq" START WITH 1 INCREMENT BY 1;
