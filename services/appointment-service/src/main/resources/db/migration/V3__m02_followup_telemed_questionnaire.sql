-- ============================================================================
-- V3 — M02 OPD Follow-up, Telemedicine, Questionnaire, OPD Card
-- Tables: t_follow_up_visit, t_telemedicine_room, t_questionnaire
-- ============================================================================

SET search_path TO ihms_appointment, public;

-- ---------------------------------------------------------------------------
-- t_follow_up_visit — Follow-up visit scheduling linked to parent appointment/encounter
-- Ported from legacy T_Followupvisitinfo (FollowUpVisitInfo.java)
-- Supports recurrence rules, slot reservation, and parent encounter validation.
-- Status: 1=SCHEDULED, 2=COMPLETED, 3=CANCELLED
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.t_follow_up_visit (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    -- Parent linkage (validates parent encounter exists)
    parent_appointment_id BIGINT REFERENCES ihms_appointment.appointment (id),
    parent_encounter_id BIGINT REFERENCES ihms_appointment.encounter (id),

    -- Follow-up details
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT,
    department_id BIGINT,
    clinic_id BIGINT,

    -- Scheduling
    follow_up_date DATE NOT NULL,
    slot_start TIME,
    slot_end TIME,
    schedule_id BIGINT REFERENCES ihms_appointment.doctor_schedule (id),
    token_no INTEGER,
    cabin VARCHAR(50),

    -- Recurrence
    recurrence_rule VARCHAR(100),  -- e.g., 'FREQ=WEEKLY;COUNT=4;BYDAY=MO' (RRULE subset)
    recurrence_end_date DATE,
    parent_follow_up_id BIGINT REFERENCES ihms_appointment.t_follow_up_visit (id),

    -- Status
    follow_up_status SMALLINT NOT NULL DEFAULT 1 CHECK (follow_up_status BETWEEN 1 AND 3),
    -- 1=SCHEDULED, 2=COMPLETED, 3=CANCELLED

    -- Portal flags (legacy parity)
    is_from_patient_portal BOOLEAN NOT NULL DEFAULT FALSE,
    patient_portal_id BIGINT,
    patient_portal_unit_id BIGINT,
    portal_patient_id BIGINT,

    -- Cancellation
    is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_by BIGINT,
    cancelled_date TIMESTAMP(3),

    CONSTRAINT pk_t_follow_up_visit PRIMARY KEY (id),
    CONSTRAINT ck_followup_date CHECK (follow_up_date >= CURRENT_DATE OR follow_up_status IN (2,3))
);
CREATE INDEX IF NOT EXISTS ix_followup_patient ON ihms_appointment.t_follow_up_visit (patient_id, follow_up_date);
CREATE INDEX IF NOT EXISTS ix_followup_parent_appt ON ihms_appointment.t_follow_up_visit (parent_appointment_id);
CREATE INDEX IF NOT EXISTS ix_followup_parent_enc ON ihms_appointment.t_follow_up_visit (parent_encounter_id);
CREATE INDEX IF NOT EXISTS ix_followup_doctor_date ON ihms_appointment.t_follow_up_visit (doctor_id, follow_up_date);
COMMENT ON TABLE ihms_appointment.t_follow_up_visit IS 'Follow-up visit scheduling (legacy T_Followupvisitinfo). Recurrence via recurrence_rule (RRULE subset). Status 1=SCHEDULED 2=COMPLETED 3=CANCELLED.';

-- ---------------------------------------------------------------------------
-- t_telemedicine_room — Telemedicine consultation room (stub behind app.telemedicine.enabled=false)
-- Creates roomId + videoLink placeholder; real provider (Jitsi/Twilio) integrated later.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.t_telemedicine_room (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    -- Linkage
    appointment_id BIGINT NOT NULL REFERENCES ihms_appointment.appointment (id),
    encounter_id BIGINT REFERENCES ihms_appointment.encounter (id),

    -- Room identity
    room_id VARCHAR(64) NOT NULL UNIQUE,        -- UUID or generated code
    video_link VARCHAR(512),                    -- stub link (e.g., https://tele.example/room/xxx)

    -- Lifecycle
    room_status SMALLINT NOT NULL DEFAULT 1 CHECK (room_status BETWEEN 1 AND 4),
    -- 1=CREATED, 2=ACTIVE, 3=ENDED, 4=EXPIRED

    started_at TIMESTAMP(3),
    ended_at TIMESTAMP(3),
    expires_at TIMESTAMP(3),

    CONSTRAINT pk_t_telemedicine_room PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_telemed_appt ON ihms_appointment.t_telemedicine_room (appointment_id);
CREATE INDEX IF NOT EXISTS ix_telemed_enc ON ihms_appointment.t_telemedicine_room (encounter_id);
COMMENT ON TABLE ihms_appointment.t_telemedicine_room IS 'Telemedicine room stub (M02). Real video provider behind app.telemedicine.enabled=false. room_status: 1=CREATED 2=ACTIVE 3=ENDED 4=EXPIRED.';

-- ---------------------------------------------------------------------------
-- t_questionnaire — Pre-consult symptom questionnaire (JSONB answers)
-- Ported from legacy T_QuestionAnswer / QuestionnaireService
-- Module: 'Patient Assessment' (pre-consult) or 'Doctor Assessment' (clinician review)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.t_questionnaire (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    -- Linkage
    appointment_id BIGINT REFERENCES ihms_appointment.appointment (id),
    encounter_id BIGINT REFERENCES ihms_appointment.encounter (id),
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT,

    -- Questionnaire identity
    module VARCHAR(50) NOT NULL,                -- 'Patient Assessment' | 'Doctor Assessment' | 'Social Assessment'
    module_type VARCHAR(20) NOT NULL DEFAULT 'PRE_CONSULT', -- 'PRE_CONSULT' | 'CLINICIAN_REVIEW'

    -- Answers stored as JSONB
    -- Structure: [{questionId: 1, answerIds: [5,6], answerText: "free text", answerGrp: 2}, ...]
    answers JSONB NOT NULL DEFAULT '[]'::jsonb,

    -- Flags
    is_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMP(3),
    clinician_review_flag BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP(3),

    CONSTRAINT pk_t_questionnaire PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_questionnaire_appt ON ihms_appointment.t_questionnaire (appointment_id);
CREATE INDEX IF NOT EXISTS ix_questionnaire_enc ON ihms_appointment.t_questionnaire (encounter_id);
CREATE INDEX IF NOT EXISTS ix_questionnaire_patient ON ihms_appointment.t_questionnaire (patient_id, module);
COMMENT ON TABLE ihms_appointment.t_questionnaire IS 'Pre-consult questionnaire (legacy T_QuestionAnswer parity). answers JSONB stores [{questionId, answerIds[], answerText, answerGrp}]. module: Patient Assessment/Doctor Assessment/Social Assessment.';

-- ---------------------------------------------------------------------------
-- Explicit sequences (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_appointment.t_follow_up_visit_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_appointment.t_telemedicine_room_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_appointment.t_questionnaire_seq START WITH 1;
