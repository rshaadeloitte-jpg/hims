-- ============================================================================
-- V2 — ihms_pharmacy: Sale Returns + Drug Allergy / Interaction (M07 gap)
-- Ported from legacy PharmacyServiceImpl.saleReturn + DrugInteraction
-- pattern. Tied to t_itemsale; returns POST a stock ADD movement
-- (item-service single-writer D-1).
--
-- Tables added:
--   t_sale_return      — header (one per original sale / batch of lines)
--   t_sale_return_line — per-item return with qty, reason, refund amount
--   t_drug_allergy     — patient allergy list (legacy t_patientallergy parity)
--   t_drug_interaction — patient-level interaction warnings (FUTURE)
--
-- Status machine for t_sale_return (SMALLINT):
--   1=INITIATED -> 2=APPROVED -> 3=COMPLETED
--                 -> 4=REJECTED (terminal)
-- BR-PHAR-003: every APPROVED return POSTs an ADD movement to item-service
-- (single writer D-1) — NO double-write to legacy dual-stock tables.
-- BR-PHAR-004: drug-allergy check fires on every dispense; warning recorded
-- without blocking (override by pharmacist required via intervention row).
-- ============================================================================

SET search_path TO ihms_pharmacy, public;

CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_sale_return (
    return_id BIGINT NOT NULL PRIMARY KEY,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    original_sale_id BIGINT NOT NULL REFERENCES ihms_pharmacy.t_itemsale (sale_id),
    patient_id BIGINT NOT NULL,
    return_reason VARCHAR(100) NOT NULL,         -- DAMAGED | WRONG_ITEM | PATIENT_REFUSED | EXPIRED | OTHER
    status_code SMALLINT NOT NULL DEFAULT 1,     -- 1=INITIATED 2=APPROVED 3=COMPLETED 4=REJECTED
    total_refund_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    approved_by BIGINT,
    approved_at TIMESTAMP(3),
    completed_at TIMESTAMP(3),
    remarks VARCHAR(500),
    CONSTRAINT ck_t_sale_return_status CHECK (status_code IN (1, 2, 3, 4))
);
CREATE INDEX IF NOT EXISTS ix_t_sale_return_sale ON ihms_pharmacy.t_sale_return (original_sale_id);
CREATE INDEX IF NOT EXISTS ix_t_sale_return_patient ON ihms_pharmacy.t_sale_return (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_sale_return_status ON ihms_pharmacy.t_sale_return (status_code);
COMMENT ON TABLE ihms_pharmacy.t_sale_return IS 'Pharmacy sale return (M07 gap; legacy PharmacySaleReturnBillDto parity). Status: 1=INITIATED 2=APPROVED 3=COMPLETED 4=REJECTED. total_refund_amount computed from approved line refund amounts.';

CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_sale_return_line (
    return_id BIGINT NOT NULL REFERENCES ihms_pharmacy.t_sale_return (return_id),
    line_no BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL,
    batch_id VARCHAR(50),
    return_qty INTEGER NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL,
    refund_amount NUMERIC(15,2) NOT NULL,
    line_reason VARCHAR(200),
    CONSTRAINT pk_t_sale_return_line PRIMARY KEY (return_id, line_no),
    CONSTRAINT uq_t_sale_return_line UNIQUE (return_id, line_no),
    CONSTRAINT ck_t_sale_return_line_qty CHECK (return_qty > 0),
    CONSTRAINT ck_t_sale_return_line_amount CHECK (refund_amount >= 0)
);
CREATE INDEX IF NOT EXISTS ix_t_sale_return_line_return ON ihms_pharmacy.t_sale_return_line (return_id);
COMMENT ON TABLE ihms_pharmacy.t_sale_return_line IS 'Per-item return line; return_qty>0 and refund_amount>=0 at DB level.';

CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_drug_allergy (
    allergy_id BIGINT NOT NULL PRIMARY KEY,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    allergen VARCHAR(200) NOT NULL,                -- drug name or class
    allergy_type VARCHAR(50),                      -- DRUG | CLASS | FOOD
    severity VARCHAR(20) NOT NULL,                 -- MILD | MODERATE | SEVERE | ANAPHYLAXIS
    reaction_description VARCHAR(500),
    recorded_by BIGINT,
    recorded_at TIMESTAMP(3) NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_t_drug_allergy_severity CHECK (severity IN ('MILD','MODERATE','SEVERE','ANAPHYLAXIS'))
);
CREATE INDEX IF NOT EXISTS ix_t_drug_allergy_patient ON ihms_pharmacy.t_drug_allergy (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_drug_allergy_allergen ON ihms_pharmacy.t_drug_allergy (allergen);
COMMENT ON TABLE ihms_pharmacy.t_drug_allergy IS 'Patient drug allergy list (legacy t_patientallergy parity). Severity gates override rules on dispense (BR-PHAR-004).';

CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_drug_interaction (
    interaction_id BIGINT NOT NULL PRIMARY KEY,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    drug1_id BIGINT NOT NULL,
    drug2_id BIGINT NOT NULL,
    interaction_type VARCHAR(50) NOT NULL,         -- DRUG_DRUG | DRUG_FOOD | DRUG_DISEASE
    severity VARCHAR(20) NOT NULL,                 -- MINOR | MODERATE | MAJOR | CONTRAINDICATED
    description VARCHAR(500),
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by BIGINT,
    acknowledged_at TIMESTAMP(3),
    recorded_at TIMESTAMP(3) NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_t_drug_interaction_severity CHECK (severity IN ('MINOR','MODERATE','MAJOR','CONTRAINDICATED'))
);
CREATE INDEX IF NOT EXISTS ix_t_drug_interaction_patient ON ihms_pharmacy.t_drug_interaction (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_drug_interaction_drugs ON ihms_pharmacy.t_drug_interaction (drug1_id, drug2_id);
COMMENT ON TABLE ihms_pharmacy.t_drug_interaction IS 'Patient drug-drug / drug-food / drug-disease interaction warning (M07 gap). Acknowledged flag stores pharmacist override.';

CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_sale_return_seq       START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_drug_allergy_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_drug_interaction_seq  START WITH 1;
