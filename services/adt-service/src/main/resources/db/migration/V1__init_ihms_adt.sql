-- ============================================================================
-- V1 -- ihms_adt schema (ADT & Bed service, MIG-032 / S03) -- WP-5 (Wave 3)
-- Clean target of the legacy ADT cluster (MICROSERVICE-CATALOG #3):
--   ward master, bed status master (STATUS-CODE-DICTIONARY §A5 seeds), bed master,
--   T_ADMISSION (+ admission detail), bed transfer log, bed reservation,
--   discharge clearance checklist, and the transactional outbox for
--   PatientAdmitted (E-03), PatientDischarged (E-04), BedTransferred,
--   BedStatusChanged (E-11 semantics) -> topic ihms.adt.lifecycle.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the adt service.
-- Conventions: explicit sequences created BEFORE dependent tables (IMP-DEC-003;
-- appointment-service V1 ordering lesson), audit block added_by/added_datetime/
-- updated_by/updated_datetime, outbox DDL mirrors sibling services incl.
-- event_id UNIQUE (IMP-DEC-024).
--
-- BedStatusId semantics (§A5 + CLEANING addition; isrelease governs allocation
-- per BR-IPD-003):
--   0 = Available/reset (cancel/transfer legacy reset)   isrelease=true
--   1 = Free                                             isrelease=true
--   2 = CLEANING (post-transfer; housekeeping clears)    isrelease=false
--   3 = Free (free-bed predicate)                        isrelease=true
--   5 = Occupied/retained                                isrelease=false
--   6 = Released post-discharge (BR-DIS-003 pinned literal) isrelease=true
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_adt;

SET search_path TO ihms_adt, public;

-- ---------------------------------------------------------------------------
-- Sequences FIRST (explicit, IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_adt.m_ward_seq                 START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.m_bed_seq                  START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.t_admission_seq            START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.t_admission_detail_seq     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.t_bed_transfer_log_seq     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.t_bed_reservation_seq      START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.t_patientdischargedata_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.r_clearance_checklist_seq  START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.outbox_seq                 START WITH 1 INCREMENT BY 1;

-- ---------------------------------------------------------------------------
-- m_ward -- ward master
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.m_ward (
    ward_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    name VARCHAR(100) NOT NULL,
    ward_type VARCHAR(50),
    iscensus BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_m_ward PRIMARY KEY (ward_id)
);
CREATE INDEX IF NOT EXISTS ix_m_ward_unit ON ihms_adt.m_ward (unit_id, status);
COMMENT ON TABLE ihms_adt.m_ward IS 'Ward master. iscensus=false wards (cradle/non-census beds) do not count toward IPD census.';

-- ---------------------------------------------------------------------------
-- m_bed_status_master -- seeded dictionary §A5 rows (reference data)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.m_bed_status_master (
    bed_status_id SMALLINT NOT NULL,
    description VARCHAR(100) NOT NULL,
    isrelease BOOLEAN NOT NULL,
    CONSTRAINT pk_m_bed_status_master PRIMARY KEY (bed_status_id)
);
COMMENT ON TABLE ihms_adt.m_bed_status_master IS 'Bed status dictionary §A5 (+2=CLEANING). isrelease=true allows allocation (BR-IPD-003 gate -> BERB005). Status 6 pins the BR-DIS-003 discharge-release literal (RISK R5 contract pinned here).';
INSERT INTO ihms_adt.m_bed_status_master (bed_status_id, description, isrelease)
VALUES (0, 'Available (reset on cancel)',        TRUE),
       (1, 'Free',                               TRUE),
       (2, 'Cleaning (post-transfer hold)',      FALSE),
       (3, 'Free (free-bed predicate)',          TRUE),
       (5, 'Occupied/retained',                  FALSE),
       (6, 'Released (post-discharge)',          TRUE)
ON CONFLICT (bed_status_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- m_bed -- bed master; occupancy via atomic CAS on occupied
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.m_bed (
    bed_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    bed_no VARCHAR(50) NOT NULL,
    ward_id BIGINT NOT NULL REFERENCES ihms_adt.m_ward (ward_id),
    category VARCHAR(50),
    building VARCHAR(50),
    occupied BOOLEAN NOT NULL DEFAULT FALSE,
    bed_status_id SMALLINT NOT NULL DEFAULT 1 REFERENCES ihms_adt.m_bed_status_master (bed_status_id),
    isnoncensus BOOLEAN NOT NULL DEFAULT FALSE,
    isot BOOLEAN NOT NULL DEFAULT FALSE,
    ispreop BOOLEAN NOT NULL DEFAULT FALSE,
    ispostop BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_m_bed PRIMARY KEY (bed_id),
    CONSTRAINT ck_m_bed_occupied_status CHECK (
        (occupied = FALSE) OR (bed_status_id = 5)
    )
);
CREATE INDEX IF NOT EXISTS ix_m_bed_ward ON ihms_adt.m_bed (ward_id, status);
CREATE INDEX IF NOT EXISTS ix_m_bed_free ON ihms_adt.m_bed (ward_id)
    WHERE occupied = FALSE AND status = TRUE
      AND isnoncensus = FALSE AND isot = FALSE AND ispreop = FALSE AND ispostop = FALSE;
COMMENT ON TABLE ihms_adt.m_bed IS 'Bed master. BR-IPD-002 free-bed predicate + atomic CAS claim UPDATE m_bed SET occupied=true WHERE bed_id=? AND occupied=false (BEXB001 on loss).';
COMMENT ON INDEX ihms_adt.ix_m_bed_free IS 'Partial index backing the auto-allocation candidate scan (BR-IPD-002).';

-- ---------------------------------------------------------------------------
-- t_admission -- clean target of T_ADMISSION.
-- ACTIVE = is_cancel=false AND discharge_date IS NULL.
-- ipd_no follows BR-IPD-001 template A/<prefix>/<yy>/<%05d seq>, seq from
-- t_admission_seq nextval INSIDE the admission tx (atomic + sequential).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.t_admission (
    admission_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT,
    bed_id BIGINT REFERENCES ihms_adt.m_bed (bed_id),
    ward_id BIGINT REFERENCES ihms_adt.m_ward (ward_id),
    admission_type VARCHAR(50),
    ipd_no VARCHAR(50) NOT NULL,
    doctor_id BIGINT NOT NULL,
    admitted_at TIMESTAMP(3) NOT NULL,
    is_cancel BOOLEAN NOT NULL DEFAULT FALSE,
    abscond_datetime TIMESTAMP(3),
    discharge_id BIGINT,
    discharge_type VARCHAR(50),
    is_expired BOOLEAN,
    death_date TIMESTAMP(3),
    handover_date TIMESTAMP(3),
    discharge_date TIMESTAMP(3),
    CONSTRAINT pk_t_admission PRIMARY KEY (admission_id),
    CONSTRAINT uq_t_admission_ipd_no UNIQUE (ipd_no)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_t_admission_active
    ON ihms_adt.t_admission (patient_id)
    WHERE is_cancel = FALSE AND discharge_date IS NULL;
CREATE INDEX IF NOT EXISTS ix_t_admission_patient ON ihms_adt.t_admission (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_admission_interim ON ihms_adt.t_admission (admitted_at)
    WHERE is_cancel = FALSE AND discharge_date IS NULL AND bed_id IS NULL;
COMMENT ON TABLE ihms_adt.t_admission IS 'Clean target of T_ADMISSION. Active-admission uniqueness (BERB013) enforced by partial unique index uq_t_admission_active AND service guard (legacy RISK R4 closed at DB level). Interim/unbedded admissions (bed_id NULL) are swept to AUTO_ABSCONDED by the abscond job (BR-IPD-005).';
COMMENT ON INDEX ihms_adt.ix_t_admission_interim IS 'Abscond sweep scan: unbedded active admissions older than the configured window.';

-- ---------------------------------------------------------------------------
-- t_admission_detail -- clinical/administrative detail (1:1 with t_admission):
-- attending doctor default = admitting doctor (BR-IPD-001), MLC flag/details
-- (legacy CIMS_ADDADMMLCDETAILS evidence, E-03 producer note), remarks.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.t_admission_detail (
    detail_id BIGINT NOT NULL DEFAULT nextval('ihms_adt.t_admission_detail_seq'),
    admission_id BIGINT NOT NULL REFERENCES ihms_adt.t_admission (admission_id),
    attending_doctor_id BIGINT,
    is_mlc BOOLEAN NOT NULL DEFAULT FALSE,
    mlc_number VARCHAR(50),
    mlc_remarks VARCHAR(500),
    remarks VARCHAR(500),
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_t_admission_detail PRIMARY KEY (detail_id),
    CONSTRAINT uq_t_admission_detail_admission UNIQUE (admission_id),
    CONSTRAINT ck_t_admission_detail_mlc CHECK ((NOT is_mlc) OR (mlc_number IS NOT NULL))
);
COMMENT ON TABLE ihms_adt.t_admission_detail IS '1:1 admission detail. is_mlc=true requires an MLC number (medico-legal case, legacy CIMS_ADDADMMLCDETAILS). Attending doctor defaults to admitting doctor when omitted (BR-IPD-001).';

-- ---------------------------------------------------------------------------
-- t_bed_transfer_log -- one row per patient movement between beds
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.t_bed_transfer_log (
    transfer_id BIGINT NOT NULL DEFAULT nextval('ihms_adt.t_bed_transfer_log_seq'),
    admission_id BIGINT NOT NULL REFERENCES ihms_adt.t_admission (admission_id),
    patient_id BIGINT NOT NULL,
    from_bed_id BIGINT REFERENCES ihms_adt.m_bed (bed_id),
    to_bed_id BIGINT NOT NULL REFERENCES ihms_adt.m_bed (bed_id),
    from_ward_id BIGINT,
    to_ward_id BIGINT,
    reason VARCHAR(255),
    transferred_by BIGINT,
    transferred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_t_bed_transfer_log PRIMARY KEY (transfer_id)
);
CREATE INDEX IF NOT EXISTS ix_t_bed_transfer_log_admission ON ihms_adt.t_bed_transfer_log (admission_id, transferred_at);
COMMENT ON TABLE ihms_adt.t_bed_transfer_log IS 'Bed movement audit. Transfer flow: CAS-claim target -> old bed -> CLEANING (status 2) -> log row -> BedTransferred + BedStatusChanged events.';

-- ---------------------------------------------------------------------------
-- t_bed_reservation -- planned-admission bed hold; actively reserved beds are
-- excluded from auto-allocation for OTHER patients (BR-IPD-002 candidate filter)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.t_bed_reservation (
    reservation_id BIGINT NOT NULL DEFAULT nextval('ihms_adt.t_bed_reservation_seq'),
    bed_id BIGINT NOT NULL REFERENCES ihms_adt.m_bed (bed_id),
    patient_id BIGINT NOT NULL,
    reserved_by BIGINT,
    reserved_at TIMESTAMP(3) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    released_at TIMESTAMP(3),
    res_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_t_bed_reservation PRIMARY KEY (reservation_id),
    CONSTRAINT ck_t_bed_reservation_status CHECK (res_status IN ('ACTIVE','CONSUMED','CANCELLED','EXPIRED')),
    CONSTRAINT ck_t_bed_reservation_window CHECK (expires_at > reserved_at)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_t_bed_reservation_active_bed
    ON ihms_adt.t_bed_reservation (bed_id) WHERE res_status = 'ACTIVE';
COMMENT ON TABLE ihms_adt.t_bed_reservation IS 'One ACTIVE reservation per bed (partial unique index). Admissions consume the reservation of the same patient; other patients never see a reserved bed in auto-allocation. Expiry sweep marks stale rows EXPIRED.';

-- ---------------------------------------------------------------------------
-- r_clearance_checklist -- multi-department discharge clearance (BR-DIS-001):
-- NURSING / PHARMACY / BILLING / LAB must ALL be cleared before DISCHARGED
-- (abscond discharges bypass the gate -- no stay left to settle).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.r_clearance_checklist (
    clearance_id BIGINT NOT NULL DEFAULT nextval('ihms_adt.r_clearance_checklist_seq'),
    admission_id BIGINT NOT NULL REFERENCES ihms_adt.t_admission (admission_id),
    department VARCHAR(20) NOT NULL,
    clr_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cleared_by BIGINT,
    cleared_at TIMESTAMP(3),
    remarks VARCHAR(500),
    CONSTRAINT pk_r_clearance_checklist PRIMARY KEY (clearance_id),
    CONSTRAINT uq_r_clearance_checklist UNIQUE (admission_id, department),
    CONSTRAINT ck_r_clearance_department CHECK (department IN ('NURSING','PHARMACY','BILLING','LAB')),
    CONSTRAINT ck_r_clearance_status CHECK (clr_status IN ('PENDING','CLEARED'))
);
COMMENT ON TABLE ihms_adt.r_clearance_checklist IS 'Discharge clearance gate (BR-DIS-001). Four rows seeded at admission; discharge requires all CLEARED unless the discharge type is ABSCOND/AUTO_ABSCONDED.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope per KAFKA-EVENT-CATALOG §4;
-- event_id UNIQUE per IMP-DEC-024). Events appended in the SAME tx as the
-- business write, relayed to topic ihms.adt.lifecycle:
--   PatientAdmitted    patient-admitted.v1     partition key patientId
--   PatientDischarged  patient-discharged.v1   partition key patientId
--   BedTransferred     bed-transferred.v1      partition key patientId
--   BedStatusChanged   bed-status-changed.v1   partition key bedId (per-bed ordering)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.outbox (
    id               BIGINT NOT NULL DEFAULT nextval('ihms_adt.outbox_seq'),
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
COMMENT ON TABLE ihms_adt.outbox IS 'Transactional outbox: admission/transfer/discharge/bed-cleaning txs write events here atomically; relay publishes to ihms.adt.lifecycle (DLQ dlt.ihms.adt.lifecycle).';
CREATE INDEX IF NOT EXISTS idx_outbox_dispatch ON ihms_adt.outbox (status, created_at) WHERE status = 'PENDING';
