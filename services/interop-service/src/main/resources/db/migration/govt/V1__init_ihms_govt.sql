-- ============================================================================
-- V1 — ihms_govt schema (Govt Programs service, Catalog #28)
--       WP-T4 (Wave-6 tranche 6, GAP-REGISTER-6)
-- Clean target of the govt-programs slice: Chiranjeevi beneficiary enrolment
-- (+ TID linkage), Aasmaan case management (+notes), EJS ASHA/supervisor/
-- indicator entries and incentives (+ the rate table used by compute), plus
-- the transactional outbox TABLE per schema convention.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the govt service.
--
-- LEGACY TYPES: legacy BigInteger ids -> BIGINT per POSTGRESQL-MIGRATION.
--
-- FLAG (UNVERIFIED AGAINST ORACLE, register WP-T4): the Aasmaan table names
--   t_aasmaan_case / t_aasmaan_case_note are taken AS-IS from the catalog and
--   have NOT been verified against the legacy Oracle schema. If the Oracle
--   names differ, rename HERE ONLY — nothing else in this service hard-codes
--   them beyond these DDL statements + the @Table annotations. Flagged again
--   in outputs/implementation/WP-T4-govt.md.
--
-- EVENT POLICY (register WP-T4 / KAFKA-EVENT-CATALOG): IncentiveComputed /
-- CaseUpdated are DEFERRED ("both deferred/proposed per catalog") — NOTHING is
-- ever written to the outbox; the integration test asserts it stays empty
-- through EVERY flow. External PCTS pushes and Chiranjeevi-Yojana scheme-side
-- pushes are OUT OF SCOPE (comment-only) — no inbound endpoint, no outbound
-- emitter is modeled here. EJS module 125 / Aasmaan module 78 full endpoint
-- parity and their ANDROID/media/profile tables likewise stay OUT OF SCOPE;
-- only the core slices below are the tranche deliverable.
--
-- Status machines (SMALLINT):
--   t_aasmaan_case.status:  1=OPEN 2=IN_PROGRESS 3=CLOSED (forward-only)
--   t_ejs_incentive.status: 1=COMPUTED 2=APPROVED 3=PAID
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_govt;

SET search_path TO ihms_govt, public;

-- ---------------------------------------------------------------------------
-- t_beneficiary_enrolment — Chiranjeevi enrolment of a patient.
-- scheme CHECK constrained to CHIRANJEEVI only (other schemes are a later
-- slice); enrolment_no UNIQUE NOT NULL (dup -> API 409).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_beneficiary_enrolment (
    enrolment_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    patient_id BIGINT NOT NULL,
    scheme VARCHAR(30) NOT NULL,
    enrolment_no VARCHAR(40) NOT NULL,
    enrolled_on DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_t_beneficiary_enrolment PRIMARY KEY (enrolment_id),
    CONSTRAINT uq_enrolment_no UNIQUE (enrolment_no),
    CONSTRAINT ck_enrolment_scheme CHECK (scheme IN ('CHIRANJEEVI'))
);
CREATE INDEX IF NOT EXISTS ix_enrolment_patient ON ihms_govt.t_beneficiary_enrolment (patient_id);
COMMENT ON TABLE ihms_govt.t_beneficiary_enrolment IS 'Chiranjeevi beneficiary enrolment; enrolment_no UNIQUE (API 409 GOVT_DUPLICATE_ENROLMENT_NO on dup). scheme pinned to CHIRANJEEVI at DB level; external Chiranjeevi-Yojana push sync stays OUT OF SCOPE (comment-only).';

-- ---------------------------------------------------------------------------
-- t_reference_tid — scheme-side TID linked to an enrolment.
-- tid UNIQUE NOT NULL (dup -> API 409).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_reference_tid (
    tid_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    enrolment_id BIGINT NOT NULL REFERENCES ihms_govt.t_beneficiary_enrolment (enrolment_id),
    tid VARCHAR(60) NOT NULL,
    CONSTRAINT pk_t_reference_tid PRIMARY KEY (tid_id),
    CONSTRAINT uq_tid UNIQUE (tid)
);
CREATE INDEX IF NOT EXISTS ix_tid_enrolment ON ihms_govt.t_reference_tid (enrolment_id);
COMMENT ON TABLE ihms_govt.t_reference_tid IS 'Scheme-side TID linkage for an enrolment; tid UNIQUE (API 409 GOVT_DUPLICATE_TID on dup, 404 for unknown enrolmentNo).';

-- ---------------------------------------------------------------------------
-- t_aasmaan_case — Aasmaan case header. FLAG: table name UNVERIFIED against
-- the legacy Oracle schema (see header comment).
-- Forward-only status machine SMALLINT: 1=OPEN -> 2=IN_PROGRESS -> 3=CLOSED.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_aasmaan_case (
    case_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    patient_id BIGINT NOT NULL,
    case_type VARCHAR(40) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    opened_on DATE,
    closed_on DATE,
    CONSTRAINT pk_t_aasmaan_case PRIMARY KEY (case_id),
    CONSTRAINT ck_aasmaan_status CHECK (status IN (1, 2, 3))
);
CREATE INDEX IF NOT EXISTS ix_aasmaan_patient ON ihms_govt.t_aasmaan_case (patient_id);
CREATE INDEX IF NOT EXISTS ix_aasmaan_status ON ihms_govt.t_aasmaan_case (status);
COMMENT ON TABLE ihms_govt.t_aasmaan_case IS 'Aasmaan case (FLAG: table name UNVERIFIED against legacy Oracle). Forward-only status SMALLINT: 1 OPEN -> 2 IN_PROGRESS -> 3 CLOSED (adjacent steps only; illegal moves are API 400 GOVT_ILLEGAL_TRANSITION). Closing stamps closed_on. CaseUpdated event DEFERRED — nothing emitted.';

-- ---------------------------------------------------------------------------
-- t_aasmaan_case_note — free-text note on a case. Guard: notes are rejected
-- (API 400 GOVT_CASE_CLOSED) when the case is CLOSED. FLAG: table name
-- UNVERIFIED against the legacy Oracle schema.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_aasmaan_case_note (
    note_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    case_id BIGINT NOT NULL REFERENCES ihms_govt.t_aasmaan_case (case_id),
    note TEXT NOT NULL,
    author VARCHAR(80),
    CONSTRAINT pk_t_aasmaan_case_note PRIMARY KEY (note_id)
);
CREATE INDEX IF NOT EXISTS ix_aasmaan_note_case ON ihms_govt.t_aasmaan_case_note (case_id);
COMMENT ON TABLE ihms_govt.t_aasmaan_case_note IS 'Aasmaan case note (FLAG: table name UNVERIFIED against legacy Oracle). Closed-case guard enforced in the service layer (400 GOVT_CASE_CLOSED).';

-- ---------------------------------------------------------------------------
-- t_ejs_supervisor — EJS supervisor reference data. NO creation API in this
-- slice (rows are inserted as reference data); supervisorId on ASHA creation
-- must reference an existing row.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_ejs_supervisor (
    supervisor_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(120),
    CONSTRAINT pk_t_ejs_supervisor PRIMARY KEY (supervisor_id),
    CONSTRAINT uq_supervisor_code UNIQUE (code)
);
COMMENT ON TABLE ihms_govt.t_ejs_supervisor IS 'EJS supervisor reference data — no API in this slice (module-125 parity OUT OF SCOPE); referenced optionally by t_ejs_asha.supervisor_id.';

-- ---------------------------------------------------------------------------
-- t_ejs_asha — ASHA worker under an optional supervisor.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_ejs_asha (
    asha_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    asha_code VARCHAR(20) NOT NULL,
    name VARCHAR(120),
    supervisor_id BIGINT REFERENCES ihms_govt.t_ejs_supervisor (supervisor_id),
    CONSTRAINT pk_t_ejs_asha PRIMARY KEY (asha_id),
    CONSTRAINT uq_asha_code UNIQUE (asha_code)
);
CREATE INDEX IF NOT EXISTS ix_asha_supervisor ON ihms_govt.t_ejs_asha (supervisor_id);
COMMENT ON TABLE ihms_govt.t_ejs_asha IS 'EJS ASHA worker; asha_code UNIQUE. Full module-125 endpoint parity OUT OF SCOPE (comment-only).';

-- ---------------------------------------------------------------------------
-- t_ejs_indicator_entry — monthly indicator value per ASHA.
-- UNIQUE(asha_id, indicator_code, period_year, period_month) — the API upserts
-- by this key; period_month pinned to 1..12 at DB level.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_ejs_indicator_entry (
    entry_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    asha_id BIGINT NOT NULL REFERENCES ihms_govt.t_ejs_asha (asha_id),
    indicator_code VARCHAR(20) NOT NULL,
    period_year INT,
    period_month INT,
    value INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_ejs_indicator_entry PRIMARY KEY (entry_id),
    CONSTRAINT uq_indicator_period UNIQUE (asha_id, indicator_code, period_year, period_month),
    CONSTRAINT ck_indicator_month CHECK (period_month BETWEEN 1 AND 12)
);
CREATE INDEX IF NOT EXISTS ix_indicator_asha_period ON ihms_govt.t_ejs_indicator_entry (asha_id, period_year, period_month);
COMMENT ON TABLE ihms_govt.t_ejs_indicator_entry IS 'Monthly EJS indicator entry; API upserts on UNIQUE(asha_id, indicator_code, period_year, period_month). ck_indicator_month pins period_month 1..12.';

-- ---------------------------------------------------------------------------
-- t_incentive_rate — rate table for incentive compute (seeded with 3 rows here;
-- seed codes/rates are illustrative — register names no concrete values).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_incentive_rate (
    indicator_code VARCHAR(20) NOT NULL,
    rate NUMERIC(19,2) NOT NULL,
    CONSTRAINT pk_t_incentive_rate PRIMARY KEY (indicator_code)
);
COMMENT ON TABLE ihms_govt.t_incentive_rate IS 'Rate table for incentive compute: amount = SUM(entry.value x rate). Seeded with 3 illustrative rows (codes/rates not named by the register — disclosed in WP report). Entries whose code has no rate row contribute 0 to the sum (documented behavior).';
INSERT INTO ihms_govt.t_incentive_rate (indicator_code, rate) VALUES
    ('ANC_CHECKUP',         50.00),
    ('DELIVERY',           600.00),
    ('CHILD_IMMUNISATION',  75.00);

-- ---------------------------------------------------------------------------
-- t_ejs_incentive — computed/approved/paid incentive per ASHA + period.
-- No UNIQUE key across (asha_id, period): recompute replaces prior COMPUTED
-- rows but deliberately KEEPS APPROVED/PAID history, so a COMPUTED and a PAID
-- row may legitimately coexist for one period.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.t_ejs_incentive (
    incentive_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    asha_id BIGINT NOT NULL REFERENCES ihms_govt.t_ejs_asha (asha_id),
    period_year INT,
    period_month INT,
    amount NUMERIC(19,2) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    computed_at TIMESTAMP(3),
    CONSTRAINT pk_t_ejs_incentive PRIMARY KEY (incentive_id),
    CONSTRAINT ck_incentive_amount CHECK (amount >= 0),
    CONSTRAINT ck_incentive_status CHECK (status IN (1, 2, 3))
);
CREATE INDEX IF NOT EXISTS ix_incentive_asha_period ON ihms_govt.t_ejs_incentive (asha_id, period_year, period_month);
CREATE INDEX IF NOT EXISTS ix_incentive_status ON ihms_govt.t_ejs_incentive (status);
COMMENT ON TABLE ihms_govt.t_ejs_incentive IS 'EJS incentive. Status machine SMALLINT: 1 COMPUTED -> approve -> 2 APPROVED -> pay -> 3 PAID (guarded, API 400 GOVT_ILLEGAL_TRANSITION otherwise). Recompute REPLACES prior COMPUTED rows only — APPROVED/PAID rows are immutable history. IncentiveComputed event DEFERRED — nothing emitted.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope DDL mirrors sibling services incl.
-- event_id UNIQUE). PHASE EVENT POLICY: IncentiveComputed / CaseUpdated are
-- DEFERRED per KAFKA-EVENT-CATALOG — NO emitter exists; this table stays EMPTY
-- (asserted by the integration test through every flow).
-- External PCTS / Chiranjeevi-Yojana pushes would land here if they ever become
-- in-scope — comment-only today.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_govt.outbox (
    id               BIGSERIAL    PRIMARY KEY,
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
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id)
);
COMMENT ON TABLE ihms_govt.outbox IS 'Transactional outbox TABLE per schema convention — ZERO emitters: IncentiveComputed/CaseUpdated DEFERRED per KAFKA-EVENT-CATALOG; integration test asserts it stays empty through every flow.';
CREATE INDEX idx_outbox_dispatch ON ihms_govt.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_govt.t_beneficiary_enrolment_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_govt.t_reference_tid_seq         START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_govt.t_aasmaan_case_seq          START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_govt.t_aasmaan_case_note_seq     START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_govt.t_ejs_supervisor_seq        START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_govt.t_ejs_asha_seq              START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_govt.t_ejs_indicator_entry_seq   START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_govt.t_ejs_incentive_seq         START WITH 1;
