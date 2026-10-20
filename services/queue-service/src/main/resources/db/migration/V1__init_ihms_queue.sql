-- ============================================================================
-- V1 — ihms_queue schema (Queue & Kiosk service, S29) — MIG-022
-- Clean target of the legacy M27 queue cluster (02-business-module-catalog §M27):
--   PatientQueue / RegistrationQueue transactionals, T_OPDSTATIONMASTER
--   (counter/station master with counter-to-doctor mapping), QMS Redis token
--   queues (pushToQ/removeFromQ, <qName>_S counters).
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the queue service.
--
-- Redis policy: legacy QMS Redis queues are EPHEMERAL by design and are NEVER
-- source-of-truth (modules/opd/migration.md #3). The durable token state
-- machine lives here; the daily-reset token counter is a DB table updated in
-- the issue transaction (same documented improvement as waste barcode_counter,
-- IMP-DEC-033) — legacy Redis counters leaked numbers on rollback.
-- A Redis display projection may be layered on later; it stays optional.
--
-- All migrated tables use explicit sequences (IMP-DEC-003).
-- REST-only slice: no queue-produced events exist in KAFKA-EVENT-CATALOG
-- phase 1 (queue CONSUMES encounter/appointment events, produces none).
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_queue;

SET search_path TO ihms_queue, public;

-- ---------------------------------------------------------------------------
-- Counter master — clean target of T_OPDSTATIONMASTER (+ counter-to-doctor
-- mapping from departmentCabinRoomMapping / roomMasterForQueue evidence)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_queue.counter (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    department_id BIGINT,
    doctor_id BIGINT,
    CONSTRAINT pk_queue_counter PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_queue_counter_dept ON ihms_queue.counter (department_id);
CREATE INDEX IF NOT EXISTS ix_queue_counter_doctor ON ihms_queue.counter (doctor_id);

-- ---------------------------------------------------------------------------
-- Queue token — clean target of PatientQueue/RegistrationQueue.
-- queue_status: 1=WAITING 2=CALLED 3=COMPLETED 4=NO_SHOW (forward-only)
-- priority:     1=NORMAL 2=EMERGENCY (emergency jumps ahead on call-next)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_queue.queue_token (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    queue_date DATE NOT NULL,
    token_no BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    visit_ref VARCHAR(64),
    department_id BIGINT NOT NULL,
    counter_id BIGINT REFERENCES ihms_queue.counter (id),
    priority SMALLINT NOT NULL DEFAULT 1,
    queue_status SMALLINT NOT NULL DEFAULT 1,
    called_at TIMESTAMP(3),
    completed_at TIMESTAMP(3),
    CONSTRAINT pk_queue_token PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_queue_token_date_no ON ihms_queue.queue_token (queue_date, token_no);
CREATE INDEX IF NOT EXISTS ix_queue_token_status ON ihms_queue.queue_token (queue_date, queue_status);
CREATE INDEX IF NOT EXISTS ix_queue_token_counter ON ihms_queue.queue_token (counter_id, queue_status);

-- ---------------------------------------------------------------------------
-- Daily token counter — daily sequence reset. One row per queue date; the
-- number bump happens inside the issuance transaction (UPDATE ... RETURNING),
-- so rollback never burns or leaks a token (IMP-DEC-033 pattern).
-- GENERATED ALWAYS AS IDENTITY would be wrong here: the value must reset per day.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_queue.daily_counter (
    counter_date DATE NOT NULL,
    last_no BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_queue_daily_counter PRIMARY KEY (counter_date)
);

-- ---------------------------------------------------------------------------
-- Sequences (server-owned explicit ids, IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_queue."queue_token_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_queue."counter_seq" START WITH 1 INCREMENT BY 1;
