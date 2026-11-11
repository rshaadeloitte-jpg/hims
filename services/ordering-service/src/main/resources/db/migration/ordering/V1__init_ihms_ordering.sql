-- ============================================================================
-- V1 — ihms_ordering schema (MIG-033: Ordering & Prescription service, S04)
--       Clean target of the legacy AddServiceOrderServiceImpl mega-transaction
--       (@Transactional(rollbackOn=Exception.class) at :2037): order -> bill
--       (CIMS_ADDBILL :2206) -> charges (CIMS_ADDCHARGETEMP/CIMS_ADDCHARGE
--       :2271-2276) -> lab (T_PATHORDERBOOKING :2273) -> rad
--       (T_RADIOLOGYORDERBOOKINGDETAILS :2274) -> EMR (:2351,2367) -> doctor
--       share (T_DOCTORSHARECHARGESDETAIL :2503,2508). The synchronous chain
--       becomes a saga: clinical_order + order_item + saga_state are written
--       atomically with an OrderInitiated event in the transactional outbox;
--       downstream services (Billing/Lab/Rad/Pharmacy) confirm or fail their
--       step and failures trigger an OrderFailed compensation event that rolls
--       back succeeded reservations. HL7 leg stays DISABLED (:2318,2399).
--
-- ONE-TABLE-ONE-OWNER: ALL tables here belong to the ordering service.
--
-- clinical_order.status_code SMALLINT (06 §A9):
--   1=INITIATED  2=CONFIRMED (all applicable steps SUCCEEDED)
--   3=CANCELLED (cancel guard INITIATED-only; compensates succeeded steps)
--   4=FAILED    (a downstream step failed; rejection_reason persisted)
-- saga_state.status: IN_PROGRESS | COMPLETED | CANCELLED | FAILED
-- saga_state.<step>: PENDING | SUCCEEDED | FAILED | COMPENSATED (NULL = N/A)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_ordering;

SET search_path TO ihms_ordering, public;

-- ---------------------------------------------------------------------------
-- clinical_order — order header (BR-ORD-001 identity/context; BR-ORD-003 totals)
-- encounter_id XOR admission_id (DB-level guard mirrors the API rule).
-- Totals are SERVER-COMPUTED from order_item lines; never client-supplied.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_ordering.clinical_order (
    order_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT,
    admission_id BIGINT,
    prescriber_id BIGINT NOT NULL,
    ward_id BIGINT,
    status_code SMALLINT NOT NULL DEFAULT 1,
    gross_total NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (gross_total >= 0),
    concession_percent NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (concession_percent BETWEEN 0 AND 100),
    concession_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (concession_amount >= 0),
    net_total NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (net_total >= 0),
    rejection_reason VARCHAR(500),
    pharmacy_prescription_id BIGINT,
    CONSTRAINT pk_clinical_order PRIMARY KEY (order_id),
    CONSTRAINT ck_clinical_order_status CHECK (status_code IN (1, 2, 3, 4)),
    CONSTRAINT ck_clinical_order_ctx CHECK (NOT (encounter_id IS NOT NULL AND admission_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_clinical_order_patient ON ihms_ordering.clinical_order (patient_id);
CREATE INDEX IF NOT EXISTS ix_clinical_order_encounter ON ihms_ordering.clinical_order (encounter_id);
CREATE INDEX IF NOT EXISTS ix_clinical_order_admission ON ihms_ordering.clinical_order (admission_id);
CREATE INDEX IF NOT EXISTS ix_clinical_order_status ON ihms_ordering.clinical_order (status_code);
COMMENT ON TABLE ihms_ordering.clinical_order IS 'Clean target of the legacy AddServiceOrder header (MIG-033 saga owner). Status machine SMALLINT: 1=INITIATED (OrderInitiated emitted in the SAME tx) -> all applicable saga steps SUCCEEDED -> 2=CONFIRMED; cancel guard INITIATED-only -> 3=CANCELLED (compensates); downstream failure -> 4=FAILED with rejection_reason persisted. ck_clinical_order_ctx enforces encounterId XOR admissionId.';

-- ---------------------------------------------------------------------------
-- order_item — one clinical service line (BR-ORD-002 line integrity,
-- BR-ORD-004 doctor-share tags). item_type drives which saga step applies:
-- LAB -> lab step, RADIOLOGY -> radiology step, NURSING -> nursing step,
-- CONSULTATION -> billing only.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_ordering.order_item (
    order_id BIGINT NOT NULL REFERENCES ihms_ordering.clinical_order (order_id),
    line_no BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_type VARCHAR(20) NOT NULL CHECK (item_type IN ('LAB', 'RADIOLOGY', 'NURSING', 'CONSULTATION')),
    item_id BIGINT NOT NULL,
    service_name VARCHAR(200) NOT NULL,
    rate NUMERIC(19,2) NOT NULL CHECK (rate >= 0),
    qty INTEGER NOT NULL CHECK (qty > 0),
    doctor_share_percent NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (doctor_share_percent BETWEEN 0 AND 100),
    CONSTRAINT pk_order_item PRIMARY KEY (order_id, line_no),
    CONSTRAINT uq_order_item_line UNIQUE (order_id, line_no)
);
CREATE INDEX IF NOT EXISTS ix_order_item_order ON ihms_ordering.order_item (order_id);
COMMENT ON TABLE ihms_ordering.order_item IS 'One clinical service line of an order (MIG-033): item_type IN (LAB,RADIOLOGY,NURSING,CONSULTATION) selects the downstream saga consumer(s); rate*qty feeds server-computed gross_total; doctor_share_percent (BR-ORD-004) travels in the OrderInitiated payload so Billing can write its doctor-share charges equivalent to legacy T_DOCTORSHARECHARGESDETAIL.';

-- ---------------------------------------------------------------------------
-- prescription — clean target of the legacy prescription header
-- encounter_id XOR admission_id (BR-ORD-001 parity).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_ordering.prescription (
    prescription_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT,
    admission_id BIGINT,
    prescriber_id BIGINT NOT NULL,
    ward_id BIGINT,
    status_code SMALLINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_prescription PRIMARY KEY (prescription_id),
    CONSTRAINT ck_prescription_status CHECK (status_code IN (1, 2, 3)),
    CONSTRAINT ck_prescription_ctx CHECK (NOT (encounter_id IS NOT NULL AND admission_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_prescription_patient ON ihms_ordering.prescription (patient_id);
CREATE INDEX IF NOT EXISTS ix_prescription_encounter ON ihms_ordering.prescription (encounter_id);
CREATE INDEX IF NOT EXISTS ix_prescription_admission ON ihms_ordering.prescription (admission_id);
CREATE INDEX IF NOT EXISTS ix_prescription_status ON ihms_ordering.prescription (status_code);
COMMENT ON TABLE ihms_ordering.prescription IS 'Clean target of the legacy prescription header. Status machine SMALLINT: 1 ACTIVE -> cancel -> 3 CANCELLED (guard ACTIVE only); 2 COMPLETED reserved for a later completion flow. ck_prescription_ctx enforces encounterId XOR admissionId at DB level (API returns 400 BEXA003 when both are supplied).';

-- ---------------------------------------------------------------------------
-- prescription_item — one drug/item line (BR-ORD-005: dose + frequency REQUIRED
-- per line — clinical safety gate over the legacy nullable columns).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_ordering.prescription_item (
    prescription_id BIGINT NOT NULL REFERENCES ihms_ordering.prescription (prescription_id),
    line_no BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL,
    qty NUMERIC(12,2) NOT NULL,
    dose VARCHAR(50) NOT NULL,
    frequency VARCHAR(50) NOT NULL,
    CONSTRAINT pk_prescription_item PRIMARY KEY (prescription_id, line_no),
    CONSTRAINT uq_prescription_item_line UNIQUE (prescription_id, line_no),
    CONSTRAINT ck_prescription_item_qty CHECK (qty > 0)
);
CREATE INDEX IF NOT EXISTS ix_prescription_item_rx ON ihms_ordering.prescription_item (prescription_id);
COMMENT ON TABLE ihms_ordering.prescription_item IS 'One drug/item line of a prescription; line_no is 1-based within the header. BR-ORD-005 pins qty > 0 AND non-null dose/frequency at DB level (API validates identically, BEXA003).';

-- ---------------------------------------------------------------------------
-- saga_state — one row per placed order (UNIQUE order_id). Step columns are
-- NULL when not applicable (e.g. no LAB items -> lab_step IS NULL). A step
-- failure flips the order FAILED and compensates every SUCCEEDED step in the
-- SAME tx as the OrderFailed event (strict integrity, IMP-DEC-004).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_ordering.saga_state (
    saga_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    order_id BIGINT NOT NULL UNIQUE,
    -- NOTE: named saga_status because the shared audit block already owns `status BOOLEAN`
    saga_status VARCHAR(20) NOT NULL,
    billing_step VARCHAR(20),
    lab_step VARCHAR(20),
    radiology_step VARCHAR(20),
    nursing_step VARCHAR(20),
    pharmacy_step VARCHAR(20),
    failed_step VARCHAR(20),
    failure_reason VARCHAR(500),
    CONSTRAINT pk_saga_state PRIMARY KEY (saga_id),
    CONSTRAINT fk_saga_state_order FOREIGN KEY (order_id)
        REFERENCES ihms_ordering.clinical_order (order_id),
    CONSTRAINT ck_saga_state_status CHECK (saga_status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT ck_saga_state_billing   CHECK (billing_step   IS NULL OR billing_step   IN ('PENDING', 'SUCCEEDED', 'FAILED', 'COMPENSATED')),
    CONSTRAINT ck_saga_state_lab       CHECK (lab_step       IS NULL OR lab_step       IN ('PENDING', 'SUCCEEDED', 'FAILED', 'COMPENSATED')),
    CONSTRAINT ck_saga_state_radiology CHECK (radiology_step IS NULL OR radiology_step IN ('PENDING', 'SUCCEEDED', 'FAILED', 'COMPENSATED')),
    CONSTRAINT ck_saga_state_nursing   CHECK (nursing_step   IS NULL OR nursing_step   IN ('PENDING', 'SUCCEEDED', 'FAILED', 'COMPENSATED')),
    CONSTRAINT ck_saga_state_pharmacy  CHECK (pharmacy_step  IS NULL OR pharmacy_step  IN ('PENDING', 'SUCCEEDED', 'FAILED', 'COMPENSATED'))
);
CREATE INDEX IF NOT EXISTS ix_saga_state_order ON ihms_ordering.saga_state (order_id);
COMMENT ON TABLE ihms_ordering.saga_state IS 'Saga coordination state for the OrderInitiated choreography (MIG-033). BILLING applies to every order (the legacy mega-txn always wrote a bill); LAB/RADIOLOGY/NURSING apply when matching order_item rows exist; PHARMACY applies when the order carries pharmacy_prescription_id. All-applicable-steps-SUCCEEDED => order CONFIRMED + saga COMPLETED; any step failure => order FAILED + SUCCEEDED steps COMPENSATED + OrderFailed event in the same tx.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope per KAFKA-EVENT-CATALOG §4;
-- event_id UNIQUE). Events on topic ihms.ordering.events via the relay:
--   OrderInitiated      order-initiated.v1      (partition key encounterId,
--                        fallback '<admissionId>', fallback 'no-context:<id>')
--   OrderFailed         order-failed.v1         (compensation — SAME tx as the
--                        FAILED/CANCELLED flip so it can never be lost)
--   PrescriptionCreated prescription-created.v1 (E-06)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_ordering.outbox (
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
COMMENT ON TABLE ihms_ordering.outbox IS 'Transactional outbox: order placement writes OrderInitiated here atomically with clinical_order+order_item+saga_state; step failure/cancel writes OrderFailed atomically with the status flip; prescription creation writes E-06 PrescriptionCreated.';
CREATE INDEX idx_outbox_dispatch ON ihms_ordering.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_ordering.clinical_order_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_ordering.prescription_seq   START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_ordering.saga_state_seq     START WITH 1;
