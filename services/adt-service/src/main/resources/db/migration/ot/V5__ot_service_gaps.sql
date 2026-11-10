-- ============================================================================
-- V5 — OT Service Module M12 gaps (WHO Surgical Checklist, Anesthesia,
-- OT Staff Scheduling, OT Billing Estimation, Nurse Schedule Events)
-- ============================================================================

CREATE TABLE IF NOT EXISTS ihms_ot.t_surgical_checklist (
    checklist_id BIGINT NOT NULL,
    schedule_id BIGINT REFERENCES ihms_ot.t_otschedule(schedule_id),
    patient_id BIGINT NOT NULL,
    phase VARCHAR(50) NOT NULL CHECK (phase IN ('SIGN_IN','TIME_OUT','SIGN_OUT')),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_by BIGINT,
    completed_at TIMESTAMP(3),
    CONSTRAINT pk_surgical_checklist PRIMARY KEY (checklist_id)
);
CREATE INDEX IF NOT EXISTS ix_surgical_checklist_schedule ON ihms_ot.t_surgical_checklist (schedule_id);

CREATE TABLE IF NOT EXISTS ihms_ot.t_checklist_item (
    item_id BIGINT NOT NULL,
    checklist_id BIGINT NOT NULL REFERENCES ihms_ot.t_surgical_checklist(checklist_id),
    item_name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('PATIENT_IDENTITY','SITE_MARKING','ANESTHESIA_CHECK','EQUIPMENT_CHECK')),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(500),
    CONSTRAINT pk_checklist_item PRIMARY KEY (item_id)
);
CREATE INDEX IF NOT EXISTS ix_checklist_item_checklist ON ihms_ot.t_checklist_item (checklist_id);

CREATE TABLE IF NOT EXISTS ihms_ot.t_checklist_completion (
    completion_id BIGINT NOT NULL,
    checklist_id BIGINT NOT NULL REFERENCES ihms_ot.t_surgical_checklist(checklist_id),
    overall_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (overall_status IN ('PENDING','COMPLETED','PARTIAL')),
    completed_by BIGINT,
    completed_at TIMESTAMP(3),
    CONSTRAINT pk_checklist_completion PRIMARY KEY (completion_id)
);

CREATE TABLE IF NOT EXISTS ihms_ot.t_anesthesia_record (
    record_id BIGINT NOT NULL,
    schedule_id BIGINT REFERENCES ihms_ot.t_otschedule(schedule_id),
    patient_id BIGINT NOT NULL,
    asa_grade VARCHAR(10),
    airway_assessment VARCHAR(200),
    comorbidities VARCHAR(500),
    pre_anesthesia_notes VARCHAR(500),
    intra_op_notes VARCHAR(500),
    post_anesthesia_notes VARCHAR(500),
    pacu_stay_minutes INTEGER,
    recovery_score INTEGER CHECK (recovery_score BETWEEN 1 AND 10),
    complications VARCHAR(500),
    CONSTRAINT pk_anesthesia_record PRIMARY KEY (record_id)
);

CREATE TABLE IF NOT EXISTS ihms_ot.t_anesthesia_drug (
    drug_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL REFERENCES ihms_ot.t_anesthesia_record(record_id),
    drug_name VARCHAR(200) NOT NULL,
    dose VARCHAR(100),
    route VARCHAR(50),
    administered_at TIMESTAMP(3),
    CONSTRAINT pk_anesthesia_drug PRIMARY KEY (drug_id)
);

CREATE TABLE IF NOT EXISTS ihms_ot.t_vital_sign_record (
    vital_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL REFERENCES ihms_ot.t_anesthesia_record(record_id),
    recorded_at TIMESTAMP(3) NOT NULL,
    heart_rate INTEGER,
    bp_systolic INTEGER,
    bp_diastolic INTEGER,
    spo2 INTEGER,
    respiratory_rate INTEGER,
    temperature NUMERIC(5,2),
    CONSTRAINT pk_vital_sign_record PRIMARY KEY (vital_id)
);

CREATE TABLE IF NOT EXISTS ihms_ot.t_ot_staff_schedule (
    staff_schedule_id BIGINT NOT NULL,
    schedule_id BIGINT REFERENCES ihms_ot.t_otschedule(schedule_id),
    staff_role VARCHAR(50) NOT NULL CHECK (staff_role IN ('SURGEON','ANESTHETIST','NURSE')),
    staff_id BIGINT NOT NULL,
    assigned_start TIMESTAMP(3) NOT NULL,
    assigned_end TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_ot_staff_schedule PRIMARY KEY (staff_schedule_id),
    CONSTRAINT ck_staff_window CHECK (assigned_end > assigned_start)
);
CREATE INDEX IF NOT EXISTS ix_staff_schedule_schedule ON ihms_ot.t_ot_staff_schedule (schedule_id);

CREATE TABLE IF NOT EXISTS ihms_ot.t_ot_room (
    room_id BIGINT NOT NULL,
    room_name VARCHAR(50) NOT NULL,
    room_type VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_ot_room PRIMARY KEY (room_id)
);

CREATE TABLE IF NOT EXISTS ihms_ot.t_ot_billing_estimate (
    estimate_id BIGINT NOT NULL,
    schedule_id BIGINT REFERENCES ihms_ot.t_otschedule(schedule_id),
    procedure_code VARCHAR(50),
    total_estimated NUMERIC(12,2) NOT NULL DEFAULT 0,
    surgeon_fee NUMERIC(12,2) DEFAULT 0,
    anesthetist_fee NUMERIC(12,2) DEFAULT 0,
    ot_charges NUMERIC(12,2) DEFAULT 0,
    implant_cost NUMERIC(12,2) DEFAULT 0,
    consumable_cost NUMERIC(12,2) DEFAULT 0,
    CONSTRAINT pk_ot_billing_estimate PRIMARY KEY (estimate_id)
);

CREATE TABLE IF NOT EXISTS ihms_ot.t_ot_procedure_cost (
    cost_id BIGINT NOT NULL,
    estimate_id BIGINT NOT NULL REFERENCES ihms_ot.t_ot_billing_estimate(estimate_id),
    item_name VARCHAR(200) NOT NULL,
    item_type VARCHAR(50) CHECK (item_type IN ('IMPLANT','CONSUMABLE','SURGEON','ANESTHETIST','OT')),
    amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_ot_procedure_cost PRIMARY KEY (cost_id)
);
CREATE INDEX IF NOT EXISTS ix_procedure_cost_estimate ON ihms_ot.t_ot_procedure_cost (estimate_id);

CREATE TABLE IF NOT EXISTS ihms_ot.t_ot_nurse_schedule (
    nurse_schedule_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    duty_date DATE NOT NULL,
    shift_type VARCHAR(50) NOT NULL CHECK (shift_type IN ('DAY','NIGHT','EVENING')),
    duty_roster VARCHAR(50),
    emergency_call BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_ot_nurse_schedule PRIMARY KEY (nurse_schedule_id)
);
CREATE INDEX IF NOT EXISTS ix_nurse_schedule_staff ON ihms_ot.t_ot_nurse_schedule (staff_id);
CREATE INDEX IF NOT EXISTS ix_nurse_schedule_date ON ihms_ot.t_ot_nurse_schedule (duty_date);
