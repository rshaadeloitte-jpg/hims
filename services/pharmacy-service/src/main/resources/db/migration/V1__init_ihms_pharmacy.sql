-- ============================================================================
-- V1 — ihms_pharmacy schema (Pharmacy & Dispensing service, S09)
--       GAP-REGISTER-5 WP-R2 (Wave-5 tranche 5, Catalog #9)
-- Clean targets of the legacy pharmacy fragment (ItemSaleServiceImpl /
-- T_ITEMSALE + T_ITEMSALEDETAIL / T_PRESCRIPTIONDISPENSE +
-- T_PRESCRIPTIONDISPENSEDETAILS semantics), plus drug interventions, ward
-- picklists, and the transactional outbox.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the pharmacy service.
-- STOCK STAYS IN item-service (single-writer D-1): the legacy dual-write hotspot
-- CIMS_ADDITEMSTOCK (PharmacyServiceImpl:2140) is RETIRED — this schema holds no
-- balance/ledger tables; dispense borrows via the item-service movement API.
--
-- Status machine (t_itemsale.status_code), SMALLINT per 06 §A9:
--   1=PENDING 2=PARTIALLY_DISPENSED 3=FULLY_DISPENSED
--   BR-PHAR-001 header flag: t_itemsale.dispensed=true <=> ALL details dispensed.
--
-- EVENTS: DispensingCompleted is PROPOSED per GAP-REGISTER-5 WP-R2 (no
-- multi-consumer evidence — NOT in KAFKA-EVENT-CATALOG) — NOTHING writes the
-- outbox table this tranche; it is provisioned per convention and the PG16
-- integration test asserts it stays EMPTY. PrescriptionCreated (E-06) /
-- InventoryUpdated consumption is a post-saga concern (comment-only).
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_pharmacy;

SET search_path TO ihms_pharmacy, public;

-- ---------------------------------------------------------------------------
-- t_itemsale — item-sale header (BR-PHAR-001 owner)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_itemsale (
    sale_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    visit_ref VARCHAR(50),
    prescription_id BIGINT,
    status_code SMALLINT NOT NULL DEFAULT 1,
    dispensed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_itemsale PRIMARY KEY (sale_id),
    CONSTRAINT ck_t_itemsale_status CHECK (status_code IN (1, 2, 3))
);
CREATE INDEX IF NOT EXISTS ix_t_itemsale_patient ON ihms_pharmacy.t_itemsale (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_itemsale_prescription ON ihms_pharmacy.t_itemsale (prescription_id);
CREATE INDEX IF NOT EXISTS ix_t_itemsale_status ON ihms_pharmacy.t_itemsale (status_code);
COMMENT ON TABLE ihms_pharmacy.t_itemsale IS 'Clean target of the legacy item-sale header. Status machine SMALLINT: 1=PENDING 2=PARTIALLY_DISPENSED 3=FULLY_DISPENSED (terminal). BR-PHAR-001: dispensed=true iff ALL detail lines dispensed. visit_ref provisioned for legacy parity but not carried by the WP-R2 API contract (stays NULL).';

-- ---------------------------------------------------------------------------
-- t_item_sale_detail — one dispensed-drug line
-- qty INT CHECK qty > 0; pending_qty tracks what remains; UNIQUE(sale_id, line_no).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_item_sale_detail (
    sale_id BIGINT NOT NULL REFERENCES ihms_pharmacy.t_itemsale (sale_id),
    line_no BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL,
    batch_id VARCHAR(50),
    qty INTEGER NOT NULL,
    pending_qty INTEGER NOT NULL,
    dispensed BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_partial_dispense BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_item_sale_detail PRIMARY KEY (sale_id, line_no),
    CONSTRAINT uq_t_item_sale_detail_line UNIQUE (sale_id, line_no),
    CONSTRAINT ck_t_item_sale_detail_qty CHECK (qty > 0),
    CONSTRAINT ck_t_item_sale_detail_pending CHECK (pending_qty >= 0 AND pending_qty <= qty)
);
CREATE INDEX IF NOT EXISTS ix_t_item_sale_detail_sale ON ihms_pharmacy.t_item_sale_detail (sale_id);
COMMENT ON TABLE ihms_pharmacy.t_item_sale_detail IS 'One drug/item line of an item sale; line_no is 1-based within the header. BR-PHAR-001: dispensed=true once pending_qty reaches 0 via dispensing; partial dispenses decrement pending_qty and set is_partial_dispense=true. ck_ pins qty > 0 and 0 <= pending_qty <= qty at DB level (API validates identically, BEXA003).';

-- ---------------------------------------------------------------------------
-- t_prescription_dispense — LOCAL projection master of a prescription's
-- dispensing state (legacy T_PRESCRIPTIONDISPENSE semantics).
-- Ordering-service remains OWNER of prescription truth via E-06; cross-service
-- write-back of these flags into ihms_clinical is DEFERRED post-saga.
-- BR-PHAR-002 master rule: is_closed='1' when ALL detail rows are closed.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_prescription_dispense (
    dispense_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    prescription_id BIGINT NOT NULL,
    emr_no VARCHAR(50),
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_prescription_dispense PRIMARY KEY (dispense_id),
    CONSTRAINT uq_t_prescription_dispense_rx UNIQUE (prescription_id)
);
COMMENT ON TABLE ihms_pharmacy.t_prescription_dispense IS 'LOCAL projection of a prescription dispensing state (legacy T_PRESCRIPTIONDISPENSE). One row per prescription_id. BR-PHAR-002: is_closed=true iff ALL projected detail rows closed. EMR number stamped via PUT /api/v1/prescriptions/{id}/emr-no.';

-- ---------------------------------------------------------------------------
-- t_prescription_dispense_detail — projected line (legacy
-- T_PRESCRIPTIONDISPENSEDETAILS semantics). Seeded from each sale line that
-- carries the prescriptionId; BR-PHAR-002: full-qty dispense closes the line
-- (is_closed='1', pending_qty=0); partial decrements pending_qty by the amount.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_prescription_dispense_detail (
    dispense_id BIGINT NOT NULL REFERENCES ihms_pharmacy.t_prescription_dispense (dispense_id),
    line_no BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL,
    qty INTEGER NOT NULL,
    pending_qty INTEGER NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_prescription_dispense_detail PRIMARY KEY (dispense_id, line_no),
    CONSTRAINT uq_t_prescription_dispense_detail_line UNIQUE (dispense_id, line_no),
    CONSTRAINT ck_t_prx_dispense_detail_qty CHECK (qty > 0),
    CONSTRAINT ck_t_prx_dispense_detail_pending CHECK (pending_qty >= 0 AND pending_qty <= qty)
);
CREATE INDEX IF NOT EXISTS ix_t_prx_dispense_detail_master ON ihms_pharmacy.t_prescription_dispense_detail (dispense_id);
COMMENT ON TABLE ihms_pharmacy.t_prescription_dispense_detail IS 'One projected prescription line for BR-PHAR-002: full-qty sale-line dispense -> is_closed=true pending_qty=0; partial -> pending_qty -= dispensed qty; master closes when all lines closed.';

-- ---------------------------------------------------------------------------
-- t_drug_intervention — pharmacist clinical interventions
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_drug_intervention (
    intervention_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    sale_id BIGINT,
    intervention_type VARCHAR(50) NOT NULL,
    notes VARCHAR(500),
    pharmacist_id BIGINT NOT NULL,
    CONSTRAINT pk_t_drug_intervention PRIMARY KEY (intervention_id)
);
CREATE INDEX IF NOT EXISTS ix_t_drug_intervention_patient ON ihms_pharmacy.t_drug_intervention (patient_id);
COMMENT ON TABLE ihms_pharmacy.t_drug_intervention IS 'Pharmacist clinical intervention record (intervention_type, notes, pharmacist_id); listed per patient (GET ?patientId=).';

-- ---------------------------------------------------------------------------
-- t_ward_picklist — ward pick list rows
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_ward_picklist (
    picklist_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    ward_id BIGINT NOT NULL,
    sale_id BIGINT NOT NULL REFERENCES ihms_pharmacy.t_itemsale (sale_id),
    picked_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_t_ward_picklist PRIMARY KEY (picklist_id)
);
CREATE INDEX IF NOT EXISTS ix_t_ward_picklist_sale ON ihms_pharmacy.t_ward_picklist (sale_id);
COMMENT ON TABLE ihms_pharmacy.t_ward_picklist IS 'Ward pick list row tying a ward to an item sale with server-stamped picked_at.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; DDL mirrors sibling services incl. event_id
-- UNIQUE). PHARMACY POLICY: NOTHING writes here in tranche 5 — DispensingCompleted
-- is PROPOSED (not in KAFKA-EVENT-CATALOG) and must NOT be emitted; the PG16
-- integration test asserts row count stays 0 after every flow so any future
-- accidental emission fails CI.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.outbox (
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
COMMENT ON TABLE ihms_pharmacy.outbox IS 'Transactional outbox, provisioned per sibling convention. INTENTIONALLY EMPTY this tranche: DispensingCompleted is PROPOSED (no catalog consumers) — negative assertion lives in PharmacyServiceIntegrationTest.';
CREATE INDEX idx_outbox_dispatch ON ihms_pharmacy.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_itemsale_seq                 START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_drug_intervention_seq        START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_ward_picklist_seq            START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_prescription_dispense_seq    START WITH 1;
