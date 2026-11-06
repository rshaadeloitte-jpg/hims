-- ============================================================================
-- V1 — ihms_cssd schema (CSSD service, Catalog #15)
--       WP-R3 (Wave-5 tranche 5, GAP-REGISTER-5)
-- Clean target of the CSSD slice (Central Sterile Supply Department):
-- t_kit(+t_kit_item), t_sterilization_cycle, t_kit_issue, plus the
-- transactional outbox TABLE per schema convention.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the cssd service.
--
-- EVENT POLICY (register WP-R3 / KAFKA-EVENT-CATALOG §3): KitIssued /
-- SterilizationCompleted are REJECTED phase 1 (no cross-service consumer
-- evidence) — NOTHING is ever written to the outbox; the integration test
-- asserts it stays empty through the full lifecycle. Consuming
-- ProcedureScheduled / InventoryUpdated is post-saga — NOT modeled here.
--
-- Kit status machine (SMALLINT):
--   1=ASSEMBLED 2=STERILIZING 3=STERILE 4=ISSUED 5=REJECTED
--   cycle start ONLY from 1 or 3 (reprocess); PASS->3 (+sterilized_at),
--   FAIL->5 (unusable); issue ONLY from 3; single active issue per kit.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_cssd;

SET search_path TO ihms_cssd, public;

-- ---------------------------------------------------------------------------
-- t_kit — sterilization kit header. kit_code UNIQUE NOT NULL.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_cssd.t_kit (
    kit_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    kit_code VARCHAR(30) NOT NULL,
    procedure_type VARCHAR(50),
    status SMALLINT NOT NULL DEFAULT 1,
    sterilized_at TIMESTAMP(3),
    expiry_days INT NOT NULL DEFAULT 14,
    CONSTRAINT pk_t_kit PRIMARY KEY (kit_id),
    CONSTRAINT uq_t_kit_code UNIQUE (kit_code),
    CONSTRAINT ck_t_kit_status CHECK (status IN (1, 2, 3, 4, 5)),
    CONSTRAINT ck_t_kit_expiry CHECK (expiry_days > 0)
);
CREATE INDEX IF NOT EXISTS ix_t_kit_status ON ihms_cssd.t_kit (status);
CREATE INDEX IF NOT EXISTS ix_t_kit_procedure ON ihms_cssd.t_kit (procedure_type);
COMMENT ON TABLE ihms_cssd.t_kit IS 'Sterilization kit header. Status machine SMALLINT: 1 ASSEMBLED -> cycle -> 2 STERILIZING -> PASS 3 STERILE / FAIL 5 REJECTED (unusable) -> issue 4 ISSUED (single active issue, structural via STERILE-only guard).';

-- ---------------------------------------------------------------------------
-- t_kit_item — one instrument line of a kit.
-- qty INT CHECK qty > 0; UNIQUE(kit_id, item_id) as composite PK
-- (same convention as t_prescription_detail in ordering-service).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_cssd.t_kit_item (
    kit_id BIGINT NOT NULL REFERENCES ihms_cssd.t_kit (kit_id),
    item_id BIGINT NOT NULL,
    qty INT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_t_kit_item PRIMARY KEY (kit_id, item_id),
    CONSTRAINT ck_t_kit_item_qty CHECK (qty > 0)
);
COMMENT ON TABLE ihms_cssd.t_kit_item IS 'One instrument line of a kit; ck_t_kit_item_qty pins qty > 0 at DB level (API validates identically, BEXA003).';

-- ---------------------------------------------------------------------------
-- t_sterilization_cycle — one sterilization run against a kit.
-- result SMALLINT NULL while running: 1=PASS 2=FAIL (set on complete).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_cssd.t_sterilization_cycle (
    cycle_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    kit_id BIGINT NOT NULL REFERENCES ihms_cssd.t_kit (kit_id),
    started_at TIMESTAMP(3) NOT NULL,
    completed_at TIMESTAMP(3),
    method VARCHAR(30) NOT NULL,
    result SMALLINT,
    CONSTRAINT pk_t_sterilization_cycle PRIMARY KEY (cycle_id),
    CONSTRAINT ck_t_cycle_result CHECK (result IS NULL OR result IN (1, 2))
);
CREATE INDEX IF NOT EXISTS ix_t_cycle_kit ON ihms_cssd.t_sterilization_cycle (kit_id);
COMMENT ON TABLE ihms_cssd.t_sterilization_cycle IS 'One sterilization run (AUTOCLAVE etc). Complete-once: completed_at set with result 1=PASS (kit -> STERILE + sterilized_at) or 2=FAIL (kit -> REJECTED).';

-- ---------------------------------------------------------------------------
-- t_kit_issue — issue of a STERILE kit to an OT schedule / procedure ref.
-- issued_to_ref NOT NULL; single active issue per kit is enforced by the
-- service guard (issue ONLY from STERILE; issuing flips kit to ISSUED).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_cssd.t_kit_issue (
    issue_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    kit_id BIGINT NOT NULL REFERENCES ihms_cssd.t_kit (kit_id),
    issued_to_ref VARCHAR(80) NOT NULL,
    issued_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_t_kit_issue PRIMARY KEY (issue_id)
);
CREATE INDEX IF NOT EXISTS ix_t_issue_kit ON ihms_cssd.t_kit_issue (kit_id);
COMMENT ON TABLE ihms_cssd.t_kit_issue IS 'Issue of a STERILE kit to an OT schedule/procedure reference. Single-active-issue: only STERILE kits can be issued and issuing moves them to ISSUED.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope DDL mirrors sibling services incl.
-- event_id UNIQUE). PHASE-1 EVENT POLICY: KitIssued / SterilizationCompleted are
-- REJECTED per KAFKA-EVENT-CATALOG (no consumer evidence) — NO emitter exists;
-- this table stays EMPTY (asserted by tests).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_cssd.outbox (
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
COMMENT ON TABLE ihms_cssd.outbox IS 'Transactional outbox TABLE per schema convention — ZERO emitters in phase 1: KitIssued/SterilizationCompleted REJECTED per KAFKA-EVENT-CATALOG; integration test asserts it stays empty.';
CREATE INDEX idx_outbox_dispatch ON ihms_cssd.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_cssd.t_kit_seq                 START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_cssd.t_sterilization_cycle_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_cssd.t_kit_issue_seq           START WITH 1;
