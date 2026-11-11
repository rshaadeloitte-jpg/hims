-- ============================================================================
-- V1 — ihms_rad schema (Radiology service, S08)
--       WP-R1 (Wave-5 tranche 5, GAP-REGISTER-5)
-- Clean target of the legacy radiology order booking cluster
-- (MICROSERVICE-CATALOG #8): t_radorderbooking(+detail).
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the radiology service.
-- Event note (register WP-R1 / KAFKA-EVENT-CATALOG §3): RadOrderCreated is
-- DEFERRED ("E deferred") — NO outbox table exists in ihms_rad yet; add it with
-- the first accepted event (bloodbank WP-9 pattern). The post-saga OrderCreated
-- event is out of scope for this tranche.
--
-- Status machine (header), SMALLINT per 06 §A9:
--   1=SCHEDULED -> start -> 2=IN_PROGRESS -> report -> 3=COMPLETED
--   cancel allowed from any non-COMPLETED status -> 4=CANCELLED
-- Priority: 1=ROUTINE 2=URGENT. Modality: XRAY/CT/MRI/US.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_rad;

SET search_path TO ihms_rad, public;

-- ---------------------------------------------------------------------------
-- t_radorderbooking — clean target of the legacy radiology order booking header
-- encounter_id XOR admission_id (at most one; DB-level guard mirrors the API rule).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rad.t_radorderbooking (
    rad_order_id BIGINT NOT NULL, unit_id BIGINT,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT,
    admission_id BIGINT,
    modality VARCHAR(50) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 1,
    status SMALLINT NOT NULL DEFAULT 1,
    is_image_captured BOOLEAN NOT NULL DEFAULT FALSE,
    report_text TEXT,
    reported_by VARCHAR(100),
    CONSTRAINT pk_t_radorderbooking PRIMARY KEY (rad_order_id),
    CONSTRAINT ck_t_radorderbooking_status CHECK (status IN (1, 2, 3, 4)),
    CONSTRAINT ck_t_radorderbooking_priority CHECK (priority IN (1, 2)),
    CONSTRAINT ck_t_radorderbooking_modality CHECK (modality IN ('XRAY', 'CT', 'MRI', 'US')),
    CONSTRAINT ck_t_radorderbooking_ctx CHECK (NOT (encounter_id IS NOT NULL AND admission_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_t_radorderbooking_patient ON ihms_rad.t_radorderbooking (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_radorderbooking_encounter ON ihms_rad.t_radorderbooking (encounter_id);
CREATE INDEX IF NOT EXISTS ix_t_radorderbooking_admission ON ihms_rad.t_radorderbooking (admission_id);
CREATE INDEX IF NOT EXISTS ix_t_radorderbooking_status ON ihms_rad.t_radorderbooking (status);
COMMENT ON TABLE ihms_rad.t_radorderbooking IS 'Clean target of the legacy radiology order booking header. Status machine SMALLINT: 1 SCHEDULED -> start -> 2 IN_PROGRESS -> report -> 3 COMPLETED; cancel from any non-COMPLETED status -> 4 CANCELLED. ck_t_radorderbooking_ctx enforces encounterId XOR admissionId at DB level (API returns 400 BEXA003 when both are supplied).';

-- ---------------------------------------------------------------------------
-- t_radorder_detail — one procedure/item line
-- qty NUMERIC(12,2) CHECK qty > 0; UNIQUE(rad_order_id, line_no).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rad.t_radorder_detail (
    rad_order_id BIGINT NOT NULL REFERENCES ihms_rad.t_radorderbooking (rad_order_id),
    line_no BIGINT NOT NULL,
    unit_id BIGINT,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL,
    qty NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_t_radorder_detail PRIMARY KEY (rad_order_id, line_no),
    CONSTRAINT uq_radorder_line UNIQUE (rad_order_id, line_no),
    CONSTRAINT ck_t_radorder_detail_qty CHECK (qty > 0)
);
CREATE INDEX IF NOT EXISTS ix_t_radorder_detail_order ON ihms_rad.t_radorder_detail (rad_order_id);
COMMENT ON TABLE ihms_rad.t_radorder_detail IS 'One procedure/item line of a rad order; line_no is 1-based within the header. ck_t_radorder_detail_qty pins qty > 0 at DB level (API validates identically, BEXA003).';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- NOTE: no outbox table — RadOrderCreated DEFERRED per KAFKA-EVENT-CATALOG §3;
-- add the transactional outbox with the first accepted event.
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_rad.t_radorderbooking_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rad.t_radorder_detail_seq  START WITH 1;
