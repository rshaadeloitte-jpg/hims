-- ============================================================================
-- V1 — ihms_rcm schema (RCM & Claims service, S23)
--       WP-R4 (Wave-5 tranche 5, GAP-REGISTER-5)
-- Clean target of the RCM/claims slice of the legacy NHCX cluster
-- (MICROSERVICE-CATALOG #23: services/abdm NHCXServiceImpl + xcg/hcx helpers):
-- t_eligibility_check, t_admission_tid, t_nhcx_request(+detail), plus the
-- transactional outbox table (convention; EMPTY this tranche).
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the rcm service.
-- Scope notes (register WP-R4 / KAFKA-EVENT-CATALOG §3):
--   * HCX/NHCX external gateway calls are OUT OF SCOPE this tranche — requests
--     are persisted with correlation ids and callbacks are simulated locally.
--   * ClaimSubmitted event is DEFERRED — NOTHING writes the outbox table;
--     integration tests assert its absence. InvoiceGenerated/PaymentReceived
--     consumption lands post-saga (comment-only).
--
-- Status machine, SMALLINT 0..3 per register WP-R4 (06 §A9 source block):
--   0=INITIATED 1=SENT 2=ACKNOWLEDGED 3=COMPLETED/ERROR (terminal)
-- NOTE: 06 §A9 as captured only evidences 0=ACCEPTED .. 3=FAILED with 1..2
-- UNKNOWN (STATUS-CODE-DICTIONARY "NHCX request status 1–2 labels"); the
-- register's binding mapping above is used and pinned by tests.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_rcm;

SET search_path TO ihms_rcm, public;

-- ---------------------------------------------------------------------------
-- t_eligibility_check — coverage-eligibility check header
-- status SMALLINT 0..3 (0=INITIATED default; PUT .../response pins 2=ACKNOWLEDGED).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rcm.t_eligibility_check (
    eligibility_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    payer_scheme VARCHAR(60),
    check_status SMALLINT NOT NULL DEFAULT 0,
    request_payload JSONB NOT NULL,
    response_payload JSONB,
    CONSTRAINT pk_t_eligibility_check PRIMARY KEY (eligibility_id),
    CONSTRAINT ck_t_eligibility_check_status CHECK (check_status BETWEEN 0 AND 3)
);
CREATE INDEX IF NOT EXISTS ix_t_eligibility_check_patient ON ihms_rcm.t_eligibility_check (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_eligibility_check_status ON ihms_rcm.t_eligibility_check (check_status);
COMMENT ON TABLE ihms_rcm.t_eligibility_check IS 'Coverage-eligibility check (NHCX kind COVERAGE_ELIGIBILITY). Status SMALLINT 0=INITIATED 1=SENT 2=ACKNOWLEDGED 3=COMPLETED/ERROR (register WP-R4 mapping of 06 §A9); response callback pins status=2. External gateway OUT OF SCOPE — callbacks simulated locally.';

-- ---------------------------------------------------------------------------
-- t_admission_tid — admission -> NHCX transaction id registry
-- admission_id UNIQUE: one TID per admission; duplicate assignment -> API 409.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rcm.t_admission_tid (
    tid_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    admission_id BIGINT NOT NULL,
    tid VARCHAR(60) NOT NULL,
    CONSTRAINT pk_t_admission_tid PRIMARY KEY (tid_id),
    CONSTRAINT uq_t_admission_tid_admission UNIQUE (admission_id)
);
COMMENT ON TABLE ihms_rcm.t_admission_tid IS 'Admission -> NHCX transaction id (tid). uq_t_admission_tid_admission enforces one TID per admission at DB level (API returns 409 TID_EXISTS on reassignment).';

-- ---------------------------------------------------------------------------
-- t_nhcx_request — persisted NHCX exchange request (correlation-id anchored)
-- kind SMALLINT: 1=COVERAGE_ELIGIBILITY 2=PRE_AUTH 3=CLAIM_SUBMIT.
-- status SMALLINT 0..3 DEFAULT 0; correlation_id UNIQUE — callback idempotency key.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rcm.t_nhcx_request (
    request_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    kind SMALLINT NOT NULL,
    patient_id BIGINT NOT NULL,
    bill_id BIGINT,
    admission_id BIGINT,
    request_status SMALLINT NOT NULL DEFAULT 0,
    request_payload JSONB NOT NULL,
    response_payload JSONB,
    correlation_id VARCHAR(64) NOT NULL,
    CONSTRAINT pk_t_nhcx_request PRIMARY KEY (request_id),
    CONSTRAINT ck_t_nhcx_request_kind CHECK (kind IN (1, 2, 3)),
    CONSTRAINT ck_t_nhcx_request_status CHECK (request_status BETWEEN 0 AND 3),
    CONSTRAINT uq_t_nhcx_request_correlation UNIQUE (correlation_id)
);
CREATE INDEX IF NOT EXISTS ix_t_nhcx_request_patient ON ihms_rcm.t_nhcx_request (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_nhcx_request_kind ON ihms_rcm.t_nhcx_request (kind);
CREATE INDEX IF NOT EXISTS ix_t_nhcx_request_status ON ihms_rcm.t_nhcx_request (request_status);
COMMENT ON TABLE ihms_rcm.t_nhcx_request IS 'Persisted NHCX request: kind 1=COVERAGE_ELIGIBILITY 2=PRE_AUTH 3=CLAIM_SUBMIT; status 0=INITIATED 1=SENT 2=ACKNOWLEDGED 3=COMPLETED/ERROR. Callbacks are idempotent by correlation_id (uq_t_nhcx_request_correlation): unknown id -> 404 envelope; same-status repeat is a no-op success; backward transition -> 400 RCM_ILLEGAL_TRANSITION.';

-- ---------------------------------------------------------------------------
-- t_nhcx_request_detail — line-level detail of a request (composite PK mirrors
-- sibling prescription-detail convention: UNIQUE(request_id, line_no)).
-- Populated from payload.lines when present, else a single full-payload row.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rcm.t_nhcx_request_detail (
    request_id BIGINT NOT NULL REFERENCES ihms_rcm.t_nhcx_request (request_id),
    line_no BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    detail JSONB NOT NULL,
    CONSTRAINT pk_t_nhcx_request_detail PRIMARY KEY (request_id, line_no),
    CONSTRAINT uq_t_nhcx_request_line UNIQUE (request_id, line_no)
);
CREATE INDEX IF NOT EXISTS ix_t_nhcx_request_detail_req ON ihms_rcm.t_nhcx_request_detail (request_id);
COMMENT ON TABLE ihms_rcm.t_nhcx_request_detail IS 'Line-level detail of an NHCX request: one row per payload.lines entry (1-based line_no), else a single line_no=1 row carrying the whole payload.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; DDL mirrors sibling services incl.
-- event_id UNIQUE). INTENTIONALLY UNUSED this tranche: ClaimSubmitted is
-- DEFERRED per KAFKA-EVENT-CATALOG §3 — no code path appends here and the
-- integration test asserts count() == 0 after every flow.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rcm.outbox (
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
COMMENT ON TABLE ihms_rcm.outbox IS 'Transactional outbox (convention parity). ClaimSubmitted is DEFERRED per KAFKA-EVENT-CATALOG §3 — nothing writes it in tranche 5; negative-tested.';
CREATE INDEX idx_outbox_dispatch ON ihms_rcm.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_rcm.t_eligibility_check_seq    START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rcm.t_admission_tid_seq        START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rcm.t_nhcx_request_seq         START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rcm.t_nhcx_request_detail_seq  START WITH 1;
