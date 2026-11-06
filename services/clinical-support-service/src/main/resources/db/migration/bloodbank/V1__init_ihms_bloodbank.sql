-- ============================================================================
-- V1 — ihms_bloodbank schema (Blood Bank service, Catalog #13) — WP-9 (Wave-3 tranche 3)
-- Clean target of the legacy blood-bank cluster (MICROSERVICE-CATALOG #13):
-- donor registry, blood requisitions (BR-BLD-001), bags, components.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the bloodbank service.
-- Conventions mirror sibling services (IMP-DEC-003 explicit sequences; audit
-- block added_by/added_datetime/updated_by/updated_datetime).
--
-- BR-BLD-001 requisition status (SMALLINT): 1=new, 2=cross-matched,
--   3=issued, 10=cancelled/expired.
-- BR-BLD-002 decross timer: crossMatchedTestDate >= 48h (2880 min) on
--   crossmatched unissued bags -> decross, requisition back toward 1.
-- BR-BLD-003 bag expiry: expirydate = today -> isbagexpired=true unless issued.
-- BR-BLD-004 requisition expiry: requesteddate + order_validation_hours elapsed,
--   sample not collected, status=1 -> 10 + order_expired=true, reason
--   'Order is expired' (detail level).
-- BR-BLD-005 donor staleness: DNR_NEW(1)/DNR_FIT(2) untouched >= 72h -> 7.
--   DNR enum collision RISK A8: legacy 5 = DNR_REJECTED == DNR_DONATED; this
--   schema only writes 1/2/7.
--
-- NOTE: NO outbox table yet — BloodRequisitionCreated is DEFERRED per
-- KAFKA-EVENT-CATALOG §3; add the outbox with the first accepted event.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_bloodbank;

SET search_path TO ihms_bloodbank, public;

-- ---------------------------------------------------------------------------
-- t_blooddonor — donor registry
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_blooddonor (
    donor_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    name VARCHAR(200) NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    donation_status SMALLINT NOT NULL DEFAULT 1,
    last_action_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_t_blooddonor PRIMARY KEY (donor_id),
    CONSTRAINT ck_t_blooddonor_status CHECK (donation_status IN (1, 2, 3, 4, 5, 6, 7))
);
CREATE INDEX IF NOT EXISTS ix_t_blooddonor_stale
    ON ihms_bloodbank.t_blooddonor (donation_status, last_action_at);
COMMENT ON TABLE ihms_bloodbank.t_blooddonor IS 'Donor registry. donation_status: 1=DNR_NEW, 2=DNR_FIT, 3..6=post-med-exam stages (5 ambiguous per RISK A8 — never written here), 7=DNR_CANCELLATION (BR-BLD-005 auto-cancel after 72h idle).';
COMMENT ON COLUMN ihms_bloodbank.t_blooddonor.last_action_at IS 'Refreshed by POST /api/v1/donors/{id}/touch; gates the BR-BLD-005 72h staleness scan.';

-- ---------------------------------------------------------------------------
-- t_bloodrequisition — BR-BLD-001 status machine
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodrequisition (
    requisition_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    ordered_volume_ml INTEGER NOT NULL CHECK (ordered_volume_ml > 0),
    requesteddate TIMESTAMP(3) NOT NULL,
    sample_collected BOOLEAN NOT NULL DEFAULT FALSE,
    requisition_status SMALLINT NOT NULL DEFAULT 1,
    order_expired BOOLEAN NOT NULL DEFAULT FALSE,
    order_validation_hours INTEGER NOT NULL DEFAULT 72,
    CONSTRAINT pk_t_bloodrequisition PRIMARY KEY (requisition_id),
    CONSTRAINT ck_t_bloodrequisition_status CHECK (requisition_status IN (1, 2, 3, 10))
);
CREATE INDEX IF NOT EXISTS ix_t_bloodrequisition_status ON ihms_bloodbank.t_bloodrequisition (requisition_status);
CREATE INDEX IF NOT EXISTS ix_t_bloodrequisition_patient ON ihms_bloodbank.t_bloodrequisition (patient_id);
COMMENT ON TABLE ihms_bloodbank.t_bloodrequisition IS 'Blood requisition. requisition_status per BR-BLD-001: 1=new; 2=cross-matched when cumulative crossmatched volume >= ordered_volume_ml; 3=issued when cumulative issued volume >= ordered_volume_ml; 10=cancelled/expired. NOTE: BloodRequisitionCreated event DEFERRED (KAFKA-EVENT-CATALOG §3) — nothing emitted on insert.';
COMMENT ON COLUMN ihms_bloodbank.t_bloodrequisition.order_expired IS 'Set only by POST /api/v1/jobs/expire-requisitions (BR-BLD-004) — never on read.';

-- ---------------------------------------------------------------------------
-- t_bloodrequisitiondetail — component lines mirroring the header status
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodrequisitiondetail (
    detail_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    requisition_id BIGINT NOT NULL REFERENCES ihms_bloodbank.t_bloodrequisition (requisition_id),
    component VARCHAR(100) NOT NULL,
    volume_ml INTEGER NOT NULL CHECK (volume_ml > 0),
    requisition_status SMALLINT NOT NULL DEFAULT 1,
    isorderexpired BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_reason VARCHAR(200),
    CONSTRAINT pk_t_bloodrequisitiondetail PRIMARY KEY (detail_id),
    CONSTRAINT fk_t_bloodreqdetail_req FOREIGN KEY (requisition_id)
        REFERENCES ihms_bloodbank.t_bloodrequisition (requisition_id)
);
CREATE INDEX IF NOT EXISTS ix_t_bloodreqdetail_req ON ihms_bloodbank.t_bloodrequisitiondetail (requisition_id);
COMMENT ON TABLE ihms_bloodbank.t_bloodrequisitiondetail IS 'Component lines; requisition_status mirrors the BR-BLD-001 header (legacy detail/header asymmetry RISK R7 kept explicit). expiry_reason carries the legacy literal Order is expired on BR-BLD-004.';

-- ---------------------------------------------------------------------------
-- t_bloodbag — collected bags (components in t_bloodcomponent)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodbag (
    bag_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    donor_id BIGINT NOT NULL REFERENCES ihms_bloodbank.t_blooddonor (donor_id),
    blood_group VARCHAR(5) NOT NULL,
    volume_ml INTEGER NOT NULL CHECK (volume_ml > 0),
    collected_at TIMESTAMP(3) NOT NULL,
    expirydate DATE NOT NULL,
    "crosSMatched" BOOLEAN NOT NULL DEFAULT FALSE,
    cross_matched_test_date TIMESTAMP(3),
    requisition_id BIGINT REFERENCES ihms_bloodbank.t_bloodrequisition (requisition_id),
    isbagexpired BOOLEAN NOT NULL DEFAULT FALSE,
    issued BOOLEAN NOT NULL DEFAULT FALSE,
    issued_requisition_id BIGINT REFERENCES ihms_bloodbank.t_bloodrequisition (requisition_id),
    CONSTRAINT pk_t_bloodbag PRIMARY KEY (bag_id)
);
CREATE INDEX IF NOT EXISTS ix_t_bloodbag_decross
    ON ihms_bloodbank.t_bloodbag ("crosSMatched", issued, cross_matched_test_date);
CREATE INDEX IF NOT EXISTS ix_t_bloodbag_expiry ON ihms_bloodbank.t_bloodbag (expirydate, issued);
COMMENT ON TABLE ihms_bloodbank.t_bloodbag IS 'Blood bag. crosSMatched keeps the legacy column spelling. cross_matched_test_date starts the BR-BLD-002 48h decross timer; BR-BLD-003 sets isbagexpired when expirydate=today unless issued. requisition_id links a crossmatched bag to its requisition.';
COMMENT ON COLUMN ihms_bloodbank.t_bloodbag.expirydate IS 'Date-only per legacy; BR-BLD-003 compares = today.';

-- ---------------------------------------------------------------------------
-- t_bloodcomponent — component split of a bag
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodcomponent (
    component_id BIGINT NOT NULL,
    bag_id BIGINT NOT NULL REFERENCES ihms_bloodbank.t_bloodbag (bag_id),
    type VARCHAR(50) NOT NULL,
    volume_ml INTEGER NOT NULL CHECK (volume_ml > 0),
    CONSTRAINT pk_t_bloodcomponent PRIMARY KEY (component_id)
);
CREATE INDEX IF NOT EXISTS ix_t_bloodcomponent_bag ON ihms_bloodbank.t_bloodcomponent (bag_id);
COMMENT ON TABLE ihms_bloodbank.t_bloodcomponent IS 'Component split of a bag (PCV/FFP/platelets etc.); registration flow deferred with component intake.';

-- ---------------------------------------------------------------------------
-- Sequences (IMP-DEC-003 explicit server-owned sequences)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_blooddonor_seq            START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodrequisition_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodrequisitiondetail_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodbag_seq              START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodcomponent_seq        START WITH 1;
