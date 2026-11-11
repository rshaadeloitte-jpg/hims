SET search_path TO ihms_pharmacy, public;

ALTER TABLE ihms_pharmacy.t_home_medication
    ADD COLUMN IF NOT EXISTS created_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS status BOOLEAN,
    ADD COLUMN IF NOT EXISTS synchronized BOOLEAN,
    ADD COLUMN IF NOT EXISTS unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;

ALTER TABLE ihms_pharmacy.t_iv_compound
    ADD COLUMN IF NOT EXISTS billed_amount NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS created_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS status BOOLEAN,
    ADD COLUMN IF NOT EXISTS synchronized BOOLEAN,
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;

ALTER TABLE ihms_pharmacy.t_iv_compound_line
    ADD COLUMN IF NOT EXISTS created_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS status BOOLEAN,
    ADD COLUMN IF NOT EXISTS synchronized BOOLEAN,
    ADD COLUMN IF NOT EXISTS unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;

CREATE TABLE IF NOT EXISTS ihms_pharmacy.t_drug_mapping (
    mapping_id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    item_id BIGINT,
    item_name VARCHAR(255),
    generic_name VARCHAR(255),
    brand_name VARCHAR(255),
    category VARCHAR(255),
    strength VARCHAR(255),
    uom VARCHAR(255),
    mrp NUMERIC(19,2),
    tax_percent NUMERIC(6,2),
    is_active BOOLEAN,

    CONSTRAINT pk_t_drug_mapping PRIMARY KEY (mapping_id)
);

CREATE SEQUENCE IF NOT EXISTS ihms_pharmacy.t_drug_mapping_seq START WITH 1 INCREMENT BY 1;
