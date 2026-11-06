-- MIG-023 sub-slice (S19): bio-medical waste management — clean target schema.
-- Single-writer ownership: waste-service is the sole writer of ihms_waste
-- (same extraction pattern as ihms_item / ihms_grievance, IMP-DEC-026/032).
--
-- Legacy sources (evidence authority, /mnt/d/ihms read-only):
--   M_WASTEBAGMASTER            -> waste_bag_master        (Wastebagmaster.java, @CrudApi allowedFilters=status)
--   T_BIOMEDICALWASTE           -> bio_medical_waste       (BioMedicalWaste.java)
--   T_BIOMEDICALWASTEDETAILS    -> bio_medical_waste_details (BioMedicalWasteDetails.java)
--   Redis counter bmw_{yyyy}_{MM}_{unitId} -> barcode_counter (moved into the DB
--   transaction so barcode allocation rolls back with the request — legacy Redis
--   counters leaked numbers on rollback).
--
-- Status dictionary (legacy BioMedicalWasteRequestStatus enum):
--   1=PENDING 2=ACCEPTED 3=HANDOVERED 4=RECEIVED 5=COMPLETED
-- Requisition number rule (legacy biowaste.no.template=%s/BW/%d/%s +
-- getLatestAutocodeSeq): {unitPrefix}/BW/{year-2000}/{%05d seq}.

CREATE SCHEMA IF NOT EXISTS ihms_waste;

-- ---------------------------------------------------------------- masters --

CREATE TABLE IF NOT EXISTS ihms_waste.waste_bag_master (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_waste_bag_master PRIMARY KEY (id),
    CONSTRAINT uq_waste_bag_master_unit_code UNIQUE (unit_id, code)
);

-- ------------------------------------------------------------- requests --

CREATE TABLE IF NOT EXISTS ihms_waste.bio_medical_waste (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    requisition_no VARCHAR(64) NOT NULL,
    store_id BIGINT,
    to_store_id BIGINT,
    request_date TIMESTAMP(3),
    request_status BIGINT NOT NULL DEFAULT 1,
    handed_over_to VARCHAR(255),
    received_by VARCHAR(255),
    CONSTRAINT pk_bio_medical_waste PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_bio_medical_waste_unit ON ihms_waste.bio_medical_waste (unit_id);
CREATE INDEX IF NOT EXISTS ix_bio_medical_waste_store ON ihms_waste.bio_medical_waste (store_id);
CREATE INDEX IF NOT EXISTS ix_bio_medical_waste_req_date ON ihms_waste.bio_medical_waste (request_date);
CREATE INDEX IF NOT EXISTS ix_bio_medical_waste_req_status ON ihms_waste.bio_medical_waste (request_status);

CREATE TABLE IF NOT EXISTS ihms_waste.bio_medical_waste_details (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    bio_medical_waste_id BIGINT NOT NULL REFERENCES ihms_waste.bio_medical_waste (id),
    bag_id BIGINT REFERENCES ihms_waste.waste_bag_master (id),
    quantity BIGINT NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    weight NUMERIC(18, 3),
    barcode_no VARCHAR(32),
    bag_status BIGINT NOT NULL DEFAULT 1,
    handed_over_to VARCHAR(255),
    firm_name VARCHAR(255),
    contact_person VARCHAR(255),
    contact_no BIGINT,
    CONSTRAINT pk_bio_medical_waste_details PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_bmw_details_request ON ihms_waste.bio_medical_waste_details (bio_medical_waste_id);
CREATE INDEX IF NOT EXISTS ix_bmw_details_barcode ON ihms_waste.bio_medical_waste_details (barcode_no);
CREATE INDEX IF NOT EXISTS ix_bmw_details_bag_status ON ihms_waste.bio_medical_waste_details (bag_status);

-- Monthly barcode counter (legacy Redis key bmw_{yyyy}_{MM}_{unitId}).
-- counter_key = 'bmw_{yyyy}_{MM}_{unitId}'; value = last issued counter.
CREATE TABLE IF NOT EXISTS ihms_waste.barcode_counter (
    counter_key VARCHAR(64) NOT NULL,
    counter_value BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_barcode_counter PRIMARY KEY (counter_key)
);

-- -------------------------------------------------------------- sequences --
-- Explicit sequences per migrated table (IMP-DEC-003; no BIGSERIAL).

CREATE SEQUENCE IF NOT EXISTS ihms_waste.waste_bag_master_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_waste.bio_medical_waste_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_waste.bio_medical_waste_details_seq START WITH 1 INCREMENT BY 1;
-- Requisition-number sequence (legacy CIMS_GETLATESTAUTOCODEANDINCREMETITBYONE
-- on activity 'T_BioMedicalWaste').
CREATE SEQUENCE IF NOT EXISTS ihms_waste.bio_medical_waste_req_no_seq START WITH 1 INCREMENT BY 1;
