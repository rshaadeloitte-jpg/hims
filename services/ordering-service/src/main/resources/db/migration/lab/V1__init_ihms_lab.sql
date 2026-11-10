-- ============================================================================
-- V1 — ihms_lab schema (Lab/Pathology service, S07) — WP-7 (Wave-3 tranche 3)
-- Clean target of the legacy Patho result-side cluster (MICROSERVICE-CATALOG #7):
-- T_PATHORDERBOOKING(+DETAIL), T_PATHOPATIENTPARAMETERDETAILS,
-- T_MACHINECONFIGURATION, plus the transactional outbox for E-05
-- LabResultFinalized.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the lab service.
-- Scope note (GAP-REGISTER WP-7 / MIGRATION-WAVES): order BOOKING stays monolith
-- until Wave 4; this schema is the RESULT/WORKLIST slice only. Patho masters stay
-- in Reference (API-borrowed). t_patientrequisition is NOT owned here yet.
--
-- Status machine (header + mirrored details), SMALLINT:
--   1=ORDERED 2=SAMPLE_COLLECTED 3=IN_PROGRESS 4=FINALIZED 5=CANCELLED
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_lab;

SET search_path TO ihms_lab, public;

-- ---------------------------------------------------------------------------
-- t_pathorderbooking — clean target of T_PATHORDERBOOKING
-- booking_no = 'LAB-' || booking_id; is_out_sourced mirrors IsOutSourced.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_lab.t_pathorderbooking (
    booking_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT,
    booking_no VARCHAR(50) NOT NULL,
    status_code SMALLINT NOT NULL DEFAULT 1,
    is_out_sourced BOOLEAN NOT NULL DEFAULT FALSE,
    finalized_at TIMESTAMP(3),
    CONSTRAINT pk_t_pathorderbooking PRIMARY KEY (booking_id),
    CONSTRAINT uq_t_pathorderbooking_no UNIQUE (booking_no),
    CONSTRAINT ck_t_pathorderbooking_status CHECK (status_code BETWEEN 1 AND 5)
);
CREATE INDEX IF NOT EXISTS ix_t_pathorderbooking_patient ON ihms_lab.t_pathorderbooking (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_pathorderbooking_encounter ON ihms_lab.t_pathorderbooking (encounter_id);
CREATE INDEX IF NOT EXISTS ix_t_pathorderbooking_status ON ihms_lab.t_pathorderbooking (status_code);
COMMENT ON TABLE ihms_lab.t_pathorderbooking IS 'Clean target of T_PATHORDERBOOKING. Status machine: 1 ORDERED -> collect -> 2 SAMPLE_COLLECTED -> start -> 3 IN_PROGRESS -> all details final -> 4 FINALIZED; cancel from 1|2|3 -> 5 CANCELLED. Column status_code carries the SMALLINT machine (status stays the shared audit flag per sibling convention).';

-- ---------------------------------------------------------------------------
-- t_pathorderdetail — clean target of T_PATHORDERDETAIL (one test line)
-- detail status mirrors header while flowing; is_final/finalized_at track
-- per-detail result finality (register extension, documented in WP-7 report).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_lab.t_pathorderdetail (
    detail_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    booking_id BIGINT NOT NULL REFERENCES ihms_lab.t_pathorderbooking (booking_id),
    test_code VARCHAR(50) NOT NULL,
    test_name VARCHAR(200) NOT NULL,
    detail_status SMALLINT NOT NULL DEFAULT 1,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    finalized_at TIMESTAMP(3),
    CONSTRAINT pk_t_pathorderdetail PRIMARY KEY (detail_id),
    CONSTRAINT ck_t_pathorderdetail_status CHECK (detail_status BETWEEN 1 AND 5)
);
CREATE INDEX IF NOT EXISTS ix_t_pathorderdetail_booking ON ihms_lab.t_pathorderdetail (booking_id);
CREATE INDEX IF NOT EXISTS ix_t_pathorderdetail_open ON ihms_lab.t_pathorderdetail (booking_id)
    WHERE is_final = FALSE;
COMMENT ON TABLE ihms_lab.t_pathorderdetail IS 'Test line of a patho booking. detail_status mirrors header machine; is_final=true marks a finalized result set — booking becomes FINALIZED when no open details remain.';

-- ---------------------------------------------------------------------------
-- t_pathopatientparameterdetails — clean target of T_PATHOPATIENTPARAMETERDETAILS
-- Upsert keyed on (detail_id, parameter_name); finalize flips is_final + stamps
-- finalized_at on every row of the detail.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_lab.t_pathopatientparameterdetails (
    param_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    detail_id BIGINT NOT NULL REFERENCES ihms_lab.t_pathorderdetail (detail_id),
    parameter_name VARCHAR(200) NOT NULL,
    value VARCHAR,
    unit VARCHAR(50),
    reference_range VARCHAR(100),
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    finalized_at TIMESTAMP(3),
    CONSTRAINT pk_t_pathopatientparameterdetails PRIMARY KEY (param_id),
    CONSTRAINT uq_param_per_detail UNIQUE (detail_id, parameter_name)
);
CREATE INDEX IF NOT EXISTS ix_param_detail ON ihms_lab.t_pathopatientparameterdetails (detail_id);
COMMENT ON TABLE ihms_lab.t_pathopatientparameterdetails IS 'Result parameters. uq_param_per_detail backs the service upsert (re-entry replaces value/unit/range and resets is_final until re-finalized).';

-- ---------------------------------------------------------------------------
-- t_machineconfiguration — clean target of T_MACHINECONFIGURATION
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_lab.t_machineconfiguration (
    machine_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    name VARCHAR(200) NOT NULL,
    protocol VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_t_machineconfiguration PRIMARY KEY (machine_id)
);
COMMENT ON TABLE ihms_lab.t_machineconfiguration IS 'Analyser/machine interface config (protocol e.g. HL7 / ASTM / file-drop). Instrument integrations themselves (eUpkaran, EXT LIMS staging [disabled], Backman OLIS) are deferred Catalog #7 external integrations; machine-interface auth [UNKNOWN].';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope per KAFKA-EVENT-CATALOG §4;
-- DDL mirrors sibling services incl. event_id UNIQUE). Event:
--   LabResultFinalized  lab-result-finalized.v1  (E-05, partition key encounterId)
--   -> topic ihms.lab.events via relay.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_lab.outbox (
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
COMMENT ON TABLE ihms_lab.outbox IS 'Transactional outbox: whole-booking finalization writes E-05 LabResultFinalized here atomically with the FINALIZED status flip; relay publishes to ihms.lab.events.';
CREATE INDEX idx_outbox_dispatch ON ihms_lab.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_lab.t_pathorderbooking_seq             START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_lab.t_pathorderdetail_seq              START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_lab.t_pathopatientparameterdetails_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_lab.t_machineconfiguration_seq         START WITH 1;
