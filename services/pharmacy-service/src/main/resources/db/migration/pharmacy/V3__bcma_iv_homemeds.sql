-- ============================================================================
-- V3 — Pharmacy BCMA, IV compounding, home medications (Catalog #9, GAP-REGISTER-5 WP-R2 supplement)
-- BCMA five-rights verification log, IV compounding order + lines, discharge home meds.
-- ONE-TABLE-ONE-OWNER: all tables belong to ihms_pharmacy.
-- ============================================================================

SET search_path TO ihms_pharmacy, public;

CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_bcma_verification_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_iv_compound_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_iv_compound_line_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_home_medication_seq START WITH 1;

-- ---------------------------------------------------------------------------
-- t_bcma_verification — five-rights medication administration check (BR-PHAR-BCMA).
-- Captures right-patient / right-drug / right-dose / right-route / right-time
-- scan event with override audit trail.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_bcma_verification (
    bcma_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    patient_id BIGINT NOT NULL,
    sale_id BIGINT NOT NULL REFERENCES ihms_pharmacy.t_itemsale (sale_id),
    line_no INTEGER NOT NULL,
    CONSTRAINT fk_bcma_sale_line FOREIGN KEY (sale_id, line_no) REFERENCES ihms_pharmacy.t_item_sale_detail (sale_id, line_no),
    encounter_id BIGINT,
    scan_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    right_patient BOOLEAN NOT NULL,
    right_drug BOOLEAN NOT NULL,
    right_dose BOOLEAN NOT NULL,
    right_route BOOLEAN NOT NULL,
    right_time BOOLEAN NOT NULL,
    all_rights_passed BOOLEAN NOT NULL,
    overridden BOOLEAN NOT NULL DEFAULT FALSE,
    override_reason VARCHAR(500),
    override_by BIGINT,
    administered BOOLEAN NOT NULL DEFAULT FALSE,
    administered_at TIMESTAMP(3),
    CONSTRAINT pk_t_bcma_verification PRIMARY KEY (bcma_id),
    CONSTRAINT ck_bcma_all_passed CHECK (
        all_rights_passed = (right_patient AND right_drug AND right_dose AND right_route AND right_time)
    )
);
COMMENT ON TABLE ihms_pharmacy.t_bcma_verification IS 'BCMA five-rights verification log. all_rights_passed is TRUE iff all five rights are TRUE (enforced by CK). Override audit trail: overridden + override_reason + override_by logged when a nurse administers despite a rights failure.';

-- ---------------------------------------------------------------------------
-- t_iv_compound — IV compounding order (sterile preparation).
-- Status: 1=PENDING 2=COMPOUNDED 3=VERIFIED 4=ADMINISTERED 5=CANCELLED
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_iv_compound (
    compound_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT,
    prescription_id BIGINT,
    order_date TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_code SMALLINT NOT NULL DEFAULT 1,
    total_volume_ml NUMERIC(10,2),
    beyond_use_date TIMESTAMP(3),
    verified_by BIGINT,
    verified_at TIMESTAMP(3),
    administered_by BIGINT,
    administered_at TIMESTAMP(3),
    remarks VARCHAR(500),
    CONSTRAINT pk_t_iv_compound PRIMARY KEY (compound_id),
    CONSTRAINT ck_iv_compound_status CHECK (status_code IN (1, 2, 3, 4, 5))
);
COMMENT ON TABLE ihms_pharmacy.t_iv_compound IS 'IV compounding order. Status 1=PENDING 2=COMPOUNDED 3=VERIFIED 4=ADMINISTERED 5=CANCELLED. beyond_use_date is the stability expiry of the preparation.';

-- ---------------------------------------------------------------------------
-- t_iv_compound_line — one ingredient in an IV compound.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_iv_compound_line (
    line_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    compound_id BIGINT NOT NULL REFERENCES ihms_pharmacy.t_iv_compound (compound_id),
    item_id BIGINT NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    quantity NUMERIC(10,3) NOT NULL,
    uom VARCHAR(20) NOT NULL,
    batch_id VARCHAR(50),
    expiry_date DATE,
    line_no INT NOT NULL,
    CONSTRAINT pk_t_iv_compound_line PRIMARY KEY (line_id),
    CONSTRAINT uq_iv_compound_line UNIQUE (compound_id, line_no)
);
COMMENT ON TABLE ihms_pharmacy.t_iv_compound_line IS 'One ingredient line of an IV compound. quantity + uom describe the amount added to the preparation.';

-- ---------------------------------------------------------------------------
-- t_home_medication — discharge home medication prescription line.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_home_medication (
    home_med_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT NOT NULL,
    admission_id BIGINT,
    item_id BIGINT NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    duration_days INT,
    quantity NUMERIC(10,2),
    uom VARCHAR(20),
    instructions VARCHAR(500),
    prescribed_by BIGINT,
    prescribed_date TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispensed BOOLEAN NOT NULL DEFAULT FALSE,
    dispensed_at TIMESTAMP(3),
    CONSTRAINT pk_t_home_medication PRIMARY KEY (home_med_id)
);
COMMENT ON TABLE ihms_pharmacy.t_home_medication IS 'Discharge home medication line. Tied to an encounter/admission. dispensed flag tracks whether the patient has collected the medication from pharmacy.';
