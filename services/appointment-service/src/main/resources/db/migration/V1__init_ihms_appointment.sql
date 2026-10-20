-- ============================================================================
-- V1 — ihms_appointment schema (Appointment & Encounter service, S02) — MIG-031
--
-- Clean target of the legacy appointment/encounter cluster (UNKNOWN-001/002/011):
--   T_APPOINTMENT            -> appointment
--   T_Encounter              -> encounter
--   T_PATIENTTOKEN           -> appointment.token_no (per schedule/day)
--   doctor schedule master   -> doctor_schedule + materialized doctor_slot rows
--   VisitStatus 11/12 writes -> encounter.legacy_visit_status (split-brain fix)
-- plus the transactional outbox for the four domain events.
--
-- ONE-TABLE-ONE-OWNER (§56): every table lives in ihms_appointment only.
-- Conventions mirror sibling services: explicit sequences per table
-- (IMP-DEC-003) and the MBase audit block (added_by/added_datetime/
-- updated_by/updated_datetime).
--
-- STATUS DICTIONARY LOCK (MIG-031 gate, RT-51): the clean appointment status
-- space is locked BEFORE any port:
--   1=BOOKED 2=CHECKED_IN 3=COMPLETED 4=CANCELLED 5=RESCHEDULED 6=NO_SHOW
-- This is NOT the legacy VisitStatus space (STATUS-CODE-DICTIONARY §1, codes
-- 3..12 shared between T_Encounter/T_Appointment). Legacy codes survive only in
-- encounter.legacy_visit_status for kiosk/queue compatibility.
--
-- SPLIT-BRAIN RESOLUTION (BR-OPD-007 / BR-PHAR-001): legacy wrote VisitStatus
-- 11 (bill paid, AddPaymentServiceHelper:477,820 — "only advances if <11") and
-- 12 (pharmacy fully dispensed, MedicationServiceImpl:812) to T_Appointment,
-- while the clinical flow wrote other codes to T_Encounter; kiosk/queue read
-- one side via getLastVisitStatus. Target: both sides of a visit are one
-- aggregate here — billing/dispensing advance encounter.legacy_visit_status
-- with the exact legacy guards, and reads resolve max(appointment side,
-- encounter side) so the two tables can no longer disagree.
--
-- DOUBLE-BOOKING PREVENTION (BR-OPD-002): doctor_slot carries capacity /
-- booked_count and bookings claim a slot with a conditional UPDATE
-- (booked_count < capacity); uq_doctor_slot makes materialization idempotent.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_appointment;

SET search_path TO ihms_appointment, public;

-- ---------------------------------------------------------------------------
-- doctor_schedule — weekly recurring clinic window per doctor/unit.
-- day_of_week ISO-8601 (1=MONDAY .. 7=SUNDAY); slot_minutes drives slot grid.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.doctor_schedule (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    doctor_id BIGINT NOT NULL,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_minutes INTEGER NOT NULL DEFAULT 15 CHECK (slot_minutes > 0),
    department_id BIGINT,
    clinic_id BIGINT,
    cabin VARCHAR(50),
    CONSTRAINT pk_doctor_schedule PRIMARY KEY (id),
    CONSTRAINT ck_doctor_schedule_window CHECK (end_time > start_time)
);
CREATE INDEX IF NOT EXISTS ix_doctor_schedule_doctor ON ihms_appointment.doctor_schedule (doctor_id, status);
COMMENT ON TABLE ihms_appointment.doctor_schedule IS 'Doctor schedule master (legacy AppointmentScheduleServiceImpl). Weekly window split into slotMinutes slots per BR-OPD-002.';

-- ---------------------------------------------------------------------------
-- doctor_slot — materialized bookable slot inventory per (schedule, date).
-- booked_count counts ACTIVE appointments (BOOKED/CHECKED_IN); the booking tx
-- claims capacity with UPDATE ... WHERE booked_count < capacity (atomic guard).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.doctor_slot (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    schedule_id BIGINT NOT NULL REFERENCES ihms_appointment.doctor_schedule (id),
    doctor_id BIGINT NOT NULL,
    department_id BIGINT,
    clinic_id BIGINT,
    slot_date DATE NOT NULL,
    slot_start TIME NOT NULL,
    slot_end TIME NOT NULL,
    capacity SMALLINT NOT NULL DEFAULT 1 CHECK (capacity > 0),
    booked_count SMALLINT NOT NULL DEFAULT 0 CHECK (booked_count >= 0),
    cabin VARCHAR(50),
    CONSTRAINT pk_doctor_slot PRIMARY KEY (id),
    CONSTRAINT ck_doctor_slot_window CHECK (slot_end > slot_start),
    CONSTRAINT ck_doctor_slot_capacity CHECK (booked_count <= capacity),
    CONSTRAINT uq_doctor_slot UNIQUE (schedule_id, slot_date, slot_start)
);
CREATE INDEX IF NOT EXISTS ix_doctor_slot_day ON ihms_appointment.doctor_slot (doctor_id, slot_date, slot_start);
COMMENT ON TABLE ihms_appointment.doctor_slot IS 'Materialized slots per schedule/day. Double-booking prevented by conditional UPDATE booked_count < capacity (BR-OPD-002) + uq_doctor_slot idempotent materialization.';

-- ---------------------------------------------------------------------------
-- appointment — clean target of T_APPOINTMENT (+T_PATIENTTOKEN as token_no).
-- appt_status dictionary lock: 1=BOOKED 2=CHECKED_IN 3=COMPLETED 4=CANCELLED
-- 5=RESCHEDULED 6=NO_SHOW. rescheduled_from_id links rebooked appointments.
-- legacy_visit_status mirrors the split-brain VisitStatus writes (see header).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.appointment (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    token_no INTEGER NOT NULL CHECK (token_no > 0),
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL REFERENCES ihms_appointment.doctor_schedule (id),
    department_id BIGINT,
    clinic_id BIGINT,
    appt_date DATE NOT NULL,
    slot_start TIME NOT NULL,
    slot_end TIME NOT NULL,
    cabin VARCHAR(50),
    appt_status SMALLINT NOT NULL DEFAULT 1 CHECK (appt_status BETWEEN 1 AND 6),
    rescheduled_from_id BIGINT REFERENCES ihms_appointment.appointment (id),
    encounter_id BIGINT,
    legacy_visit_status SMALLINT CHECK (legacy_visit_status IS NULL OR legacy_visit_status >= 0),
    CONSTRAINT pk_appointment PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_appointment_day ON ihms_appointment.appointment (appt_date, doctor_id, slot_start);
CREATE INDEX IF NOT EXISTS ix_appointment_patient ON ihms_appointment.appointment (patient_id);
-- Token uniqueness per schedule/day (T_PATIENTTOKEN parity; race-safe issuance).
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointment_token
    ON ihms_appointment.appointment (schedule_id, appt_date, token_no) WHERE status = TRUE;
COMMENT ON TABLE ihms_appointment.appointment IS 'Clean target of T_APPOINTMENT (+T_PATIENTTOKEN as token_no). Locked lifecycle 1=BOOKED 2=CHECKED_IN 3=COMPLETED 4=CANCELLED 5=RESCHEDULED 6=NO_SHOW (MIG-031 dictionary lock).';

-- ---------------------------------------------------------------------------
-- daily_encounter_counter — atomic per-(date,unit) OPD encounter number source
-- (daily-reset semantics like queue-service daily_counter, IMP-DEC-038):
-- bumped inside the check-in transaction via INSERT .. ON CONFLICT .. RETURNING
-- so a rollback never burns a number. Backs BR-OPD-003 OPD number template.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.daily_encounter_counter (
    counter_date DATE NOT NULL,
    unit_id BIGINT NOT NULL DEFAULT 0,
    last_no INTEGER NOT NULL DEFAULT 0 CHECK (last_no >= 0),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_daily_encounter_counter PRIMARY KEY (counter_date, unit_id)
);
COMMENT ON TABLE ihms_appointment.daily_encounter_counter IS 'Per-day OPD encounter number allocator; incremented atomically inside the check-in tx (rollback never burns a number). Queue-service counter compatible.';

-- ---------------------------------------------------------------------------
-- encounter — clean target of T_Encounter. One open encounter per visit;
-- legacy_visit_status carries the resolved VisitStatus 11/12 split-brain
-- state with the legacy advance guards; is_closed/closed_* driven by complete
-- or the daily closing job (ApplicationJobs:542 parity).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.encounter (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    appointment_id BIGINT REFERENCES ihms_appointment.appointment (id),
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    department_id BIGINT,
    clinic_id BIGINT,
    encounter_date DATE NOT NULL,
    encounter_no INTEGER NOT NULL CHECK (encounter_no > 0),
    token_no INTEGER,
    cabin VARCHAR(50),
    visitstatus2 VARCHAR(10) NOT NULL DEFAULT '2',
    legacy_visit_status SMALLINT CHECK (legacy_visit_status IS NULL OR legacy_visit_status IN (11, 12)),
    pharmacy_pending BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP(3),
    closed_by BIGINT,
    close_reason VARCHAR(100),
    CONSTRAINT pk_encounter PRIMARY KEY (id),
    CONSTRAINT uq_encounter_daily UNIQUE (encounter_date, unit_id, encounter_no)
);
CREATE INDEX IF NOT EXISTS ix_encounter_patient ON ihms_appointment.encounter (patient_id, encounter_date);
CREATE INDEX IF NOT EXISTS ix_encounter_open_stale ON ihms_appointment.encounter (encounter_date) WHERE is_closed = FALSE;
COMMENT ON TABLE ihms_appointment.encounter IS 'Clean target of T_Encounter. legacy_visit_status holds resolved VisitStatus (11=billed, 12=dispensed) applying legacy guards (<11 advances, 12 needs no pending pharmacy items). Daily close job closes stale open encounters.';
COMMENT ON INDEX ix_encounter_open_stale IS 'Drives the daily encounter closing job (ApplicationJobs:542 parity).';

-- ---------------------------------------------------------------------------
-- Explicit sequences (IMP-DEC-003) — server-owned ids assigned before persist.
-- Created before their tables so DEFAULT nextval(...) resolves.
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_appointment.doctor_schedule_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_appointment.doctor_slot_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_appointment.appointment_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_appointment.encounter_seq        START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_appointment.outbox_seq           START WITH 1;

-- ---------------------------------------------------------------------------
-- outbox — transactional outbox (IMP-DEC-004). Events: AppointmentBooked,
-- AppointmentCancelled, EncounterCreated (E-02), EncounterClosed → topic
-- ihms.appointment.encounter, partition key patientId / encounterId.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_appointment.outbox (
    id               BIGINT NOT NULL DEFAULT nextval('ihms_appointment.outbox_seq'),
    event_id         UUID         NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    version          VARCHAR(16)  NOT NULL DEFAULT 'v1',
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     BIGINT       NOT NULL,
    partition_key    VARCHAR(100) NOT NULL,
    payload          JSONB        NOT NULL,
    idempotency_key  VARCHAR(255),
    producer_service VARCHAR(100) NOT NULL,
    occurred_at      TIMESTAMP(3) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts         INTEGER      NOT NULL DEFAULT 0,
    published_at     TIMESTAMP(3),
    created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_outbox PRIMARY KEY (id),
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id)
);
COMMENT ON TABLE ihms_appointment.outbox IS 'Transactional outbox: business tx appends AppointmentBooked / AppointmentCancelled / EncounterCreated / EncounterClosed here atomically; relay publishes to ihms.appointment.encounter (partition key patientId, encounterId for EncounterClosed), retry x5 then DLQ dlt.ihms.appointment.encounter.';
CREATE INDEX IF NOT EXISTS idx_outbox_dispatch ON ihms_appointment.outbox (status, created_at) WHERE status IN ('PENDING', 'FAILED');
