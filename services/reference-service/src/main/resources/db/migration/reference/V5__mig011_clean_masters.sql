-- ============================================================================
-- V5 — MIG-011 clean target masters (ihms_master) — per AGT-02 design
-- (outputs/design/AGT-02-W1-masters-schema.md; IMP-DEC-021 ratified).
-- Team-authored schema (IMP-DEC-018): unit/facility/location/doctor masters.
-- Conventions: snake_case clean tables, shared MBase-style audit block,
-- explicit per-table sequences (IMP-DEC-003), FKs where ownership is clear.
-- Legacy 1:1 parity tables (V1-V3) remain for the MIG-005 gateway shadow path.
-- ============================================================================

SET search_path TO ihms_master, public;

-- ---------------------------------------------------------------------------
-- unit (clean target of M_UNITMASTER) + unit_config (feature flags) + subunit
-- ---------------------------------------------------------------------------
CREATE TABLE ihms_master.unit (
    id                BIGINT      NOT NULL,
    unit_code         VARCHAR(64) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    description       VARCHAR(500),
    unit_type_id      BIGINT,
    unit_category_id  BIGINT,
    parent_unit_id    BIGINT,
    is_parent         BOOLEAN     NOT NULL DEFAULT FALSE,
    is_administrative BOOLEAN     NOT NULL DEFAULT FALSE,
    is_operational    BOOLEAN     NOT NULL DEFAULT FALSE,
    is_virtual        BOOLEAN     NOT NULL DEFAULT FALSE,
    virtual_unit_id   BIGINT,
    is_confirmatory   BOOLEAN,
    is_offline        BOOLEAN,
    is_central_store  BOOLEAN     NOT NULL DEFAULT FALSE,
    bed_capacity      BIGINT,
    address_line1     VARCHAR(500),
    address_line2     VARCHAR(500),
    address_line3     VARCHAR(500),
    city_id           BIGINT,
    district_id       BIGINT,
    taluka            VARCHAR(255),
    area              VARCHAR(255),
    pincode           VARCHAR(16),
    contact_no        VARCHAR(32),
    std_code          INTEGER,
    country_code_intl INTEGER,
    fax_no            VARCHAR(32),
    email             VARCHAR(255),
    longitude         VARCHAR(32),
    latitude          VARCHAR(32),
    pharmacy_license_no VARCHAR(64),
    clinic_reg_no       VARCHAR(64),
    shop_no             VARCHAR(64),
    trade_no            VARCHAR(64),
    vat_tin_no          VARCHAR(64),
    unit_prefix_for_mrn VARCHAR(16),
    contract_from_date DATE,
    contract_to_date   DATE,
    abdm_hfr_id       VARCHAR(64),
    abdm_id           VARCHAR(64),
    hmis_id           VARCHAR(64),
    hmis_code         VARCHAR(64),
    ehr_code          VARCHAR(64),
    nin_code          VARCHAR(64),
    eaushadhi_store_id   VARCHAR(64),
    eaushadhi_warehouse_id VARCHAR(64),
    eraktkosh_unit_id  VARCHAR(64),
    logo               BYTEA,
    config_json        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    unit_id          BIGINT,
    status           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id  BIGINT,
    updated_unit_id  BIGINT,
    added_by         BIGINT,
    added_datetime   TIMESTAMP(3),
    updated_by       BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_unit PRIMARY KEY (id),
    CONSTRAINT fk_unit_parent FOREIGN KEY (parent_unit_id) REFERENCES ihms_master.unit (id),
    CONSTRAINT uq_unit_code UNIQUE (unit_code)
);
COMMENT ON TABLE ihms_master.unit IS 'Clean target of M_UNITMASTER (Unitmaster.java). Facility/hospital master; unit_config holds feature flags; config_json the long tail.';
CREATE INDEX ix_unit_status ON ihms_master.unit (unit_id, status);

CREATE TABLE ihms_master.unit_config (
    id                BIGINT  NOT NULL,
    unit_id           BIGINT  NOT NULL,
    is_med_verify             BOOLEAN NOT NULL DEFAULT FALSE,
    is_centrally_outsourced_lab BOOLEAN NOT NULL DEFAULT FALSE,
    is_cwf_coversheet         SMALLINT,
    is_auto_indent            BOOLEAN NOT NULL DEFAULT FALSE,
    auto_indent_execution_time VARCHAR(16),
    auto_indent_for_unit      BIGINT,
    is_multi_main_store       BOOLEAN NOT NULL DEFAULT FALSE,
    show_patho_location       BOOLEAN NOT NULL DEFAULT FALSE,
    show_patho_subspecialization BOOLEAN NOT NULL DEFAULT FALSE,
    is_pharmacy_upload_presc  BOOLEAN NOT NULL DEFAULT FALSE,
    is_pharmacy_visit_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_op_label_required      BOOLEAN NOT NULL DEFAULT FALSE,
    allow_direct_online_appointment BOOLEAN NOT NULL DEFAULT FALSE,
    allow_lab_normal_result_review BOOLEAN NOT NULL DEFAULT TRUE,
    bill_number_list_integration BOOLEAN NOT NULL DEFAULT FALSE,
    consult_type              SMALLINT NOT NULL DEFAULT 0,
    enable_ehr                BOOLEAN NOT NULL DEFAULT FALSE,
    enable_otp_based_login    BOOLEAN NOT NULL DEFAULT FALSE,
    enable_whatsapp           BOOLEAN NOT NULL DEFAULT FALSE,
    is_eaushadhi_data_sync    BOOLEAN NOT NULL DEFAULT FALSE,
    locale_description        VARCHAR(500),
    status           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id  BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3), updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_unit_config PRIMARY KEY (id),
    CONSTRAINT fk_unit_config_unit FOREIGN KEY (unit_id) REFERENCES ihms_master.unit (id),
    CONSTRAINT uq_unit_config_unit UNIQUE (unit_id)
);

CREATE TABLE ihms_master.subunit (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255),
    CONSTRAINT pk_subunit PRIMARY KEY (id),
    CONSTRAINT uq_subunit_unit_code UNIQUE (unit_id, code)
);

-- ---------------------------------------------------------------------------
-- facility_location — physical hierarchy (building/floor/wing/room/cabin/ward...)
-- ---------------------------------------------------------------------------
CREATE TABLE ihms_master.facility_location (
    id            BIGINT       NOT NULL,
    unit_id       BIGINT,
    location_type VARCHAR(16)  NOT NULL
        CHECK (location_type IN ('BUILDING','FLOOR','WING','ROOM','CABIN','WARD','NURSING_STATION','OT_THEATRE')),
    code          VARCHAR(64),
    name          VARCHAR(255) NOT NULL,
    name_hindi    VARCHAR(255),
    parent_id     BIGINT,
    sequence_no   INTEGER,
    is_unique_sequence BOOLEAN NOT NULL DEFAULT FALSE,
    token_prefix  VARCHAR(16),
    is_room_amenities BOOLEAN   NOT NULL DEFAULT FALSE,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3), updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_facility_location PRIMARY KEY (id),
    CONSTRAINT fk_facility_location_parent FOREIGN KEY (parent_id) REFERENCES ihms_master.facility_location (id),
    CONSTRAINT uq_facility_location_unit_type_code UNIQUE (unit_id, location_type, code)
);
CREATE INDEX ix_facility_location_parent ON ihms_master.facility_location (parent_id);
CREATE INDEX ix_facility_location_unit_status ON ihms_master.facility_location (unit_id, status);
COMMENT ON TABLE ihms_master.facility_location IS 'Clean target of M_BUILDING/FLOOR/WING/ROOM/CABINMASTER (+NURSING_STATION). Hierarchy normalized with parent_id (legacy had no PK FKs). WARD/BED operational state -> ihms_adt (D-3); OT_THEATRE domain-owned by OT (D-3).';

-- ---------------------------------------------------------------------------
-- Geographic reference (global rows: unit_id NULL — D-6)
-- ---------------------------------------------------------------------------
CREATE TABLE ihms_master.geo_country (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(8)  NOT NULL,
    name VARCHAR(120) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    nationality_name VARCHAR(120),
    CONSTRAINT pk_geo_country PRIMARY KEY (id),
    CONSTRAINT uq_geo_country_code UNIQUE (code)
);

CREATE TABLE ihms_master.geo_state (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    country_id BIGINT NOT NULL REFERENCES ihms_master.geo_country (id),
    code VARCHAR(16) NOT NULL, name VARCHAR(120) NOT NULL, name_hindi VARCHAR(120),
    abdm_code VARCHAR(32),
    CONSTRAINT pk_geo_state PRIMARY KEY (id),
    CONSTRAINT uq_geo_state_country_code UNIQUE (country_id, code)
);

CREATE TABLE ihms_master.geo_district (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    state_id BIGINT NOT NULL REFERENCES ihms_master.geo_state (id),
    code VARCHAR(16) NOT NULL, name VARCHAR(120) NOT NULL,
    census_2011_code VARCHAR(16),
    zone_id BIGINT,
    abdm_district_code VARCHAR(32),
    CONSTRAINT pk_geo_district PRIMARY KEY (id),
    CONSTRAINT uq_geo_district_state_code UNIQUE (state_id, code)
);

CREATE TABLE ihms_master.geo_city (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    district_id BIGINT REFERENCES ihms_master.geo_district (id),
    state_id BIGINT NOT NULL REFERENCES ihms_master.geo_state (id),
    code VARCHAR(16) NOT NULL, name VARCHAR(120) NOT NULL,
    census_code VARCHAR(16),
    CONSTRAINT pk_geo_city PRIMARY KEY (id),
    CONSTRAINT uq_geo_city_district_code UNIQUE (district_id, code)
);

CREATE TABLE ihms_master.geo_area (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    city_id BIGINT NOT NULL REFERENCES ihms_master.geo_city (id),
    code VARCHAR(32) NOT NULL, name VARCHAR(120) NOT NULL,
    CONSTRAINT pk_geo_area PRIMARY KEY (id),
    CONSTRAINT uq_geo_area_city_code UNIQUE (city_id, code)
);

-- ---------------------------------------------------------------------------
-- Doctor master (doctor, doctor_type, specialization, doctor_unit, doctor_department)
-- ---------------------------------------------------------------------------
CREATE TABLE ihms_master.doctor_type (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_doctor_type PRIMARY KEY (id),
    CONSTRAINT uq_doctor_type_unit_code UNIQUE (unit_id, code)
);

CREATE TABLE ihms_master.specialization (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    print_order INTEGER,
    ledger_name VARCHAR(64),
    is_doctor_show BOOLEAN NOT NULL DEFAULT TRUE,
    is_department_show BOOLEAN NOT NULL DEFAULT TRUE,
    is_recalculate_service BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_specialization PRIMARY KEY (id),
    CONSTRAINT uq_specialization_unit_code UNIQUE (unit_id, code)
);
COMMENT ON TABLE ihms_master.specialization IS 'Clean target of M_SPECIALIZATION. UNCERTAIN: which master SPECIALIZATIONID references must be confirmed at MIG-001 (D-2/D-6).';

CREATE TABLE ihms_master.doctor (
    id BIGINT NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    middle_name VARCHAR(120),
    last_name VARCHAR(120),
    gender_id BIGINT,
    dob DATE,
    photo BYTEA,
    digital_signature BYTEA,
    marital_status SMALLINT,
    mobile_code BIGINT,
    mobile_no VARCHAR(16),
    email_id VARCHAR(255),
    iam_user_id BIGINT,
    employee_number VARCHAR(64),
    pf_number VARCHAR(64),
    pan_number VARCHAR(32),
    date_of_joining DATE,
    access_card_number VARCHAR(64),
    registration_number VARCHAR(64),
    doctor_no VARCHAR(64),
    prefix_id BIGINT,
    is_nurse BOOLEAN NOT NULL DEFAULT FALSE,
    education VARCHAR(255),
    experience VARCHAR(120),
    specialization_id BIGINT REFERENCES ihms_master.specialization (id),
    subspecialization_id BIGINT,
    doctor_type_id BIGINT REFERENCES ihms_master.doctor_type (id),
    is_concession_authority BOOLEAN NOT NULL DEFAULT FALSE,
    is_resident_doctor BOOLEAN NOT NULL DEFAULT FALSE,
    is_hod BOOLEAN NOT NULL DEFAULT FALSE,
    phone_no VARCHAR(32),
    contact_number_one VARCHAR(16),
    contact_number_two VARCHAR(16),
    country_id BIGINT, state_id BIGINT, city_id BIGINT, pincode VARCHAR(16), area VARCHAR(120),
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_doctor PRIMARY KEY (id),
    CONSTRAINT uq_doctor_iam_user UNIQUE (iam_user_id)
);
CREATE INDEX ix_doctor_unit_status ON ihms_master.doctor (unit_id, status);
CREATE INDEX ix_doctor_name ON ihms_master.doctor (last_name, first_name);
CREATE INDEX ix_doctor_specialization ON ihms_master.doctor (specialization_id);
COMMENT ON TABLE ihms_master.doctor IS 'Clean target of M_DOCTORMASTER (Doctormaster.java). Identity vs clinical split; logical iam_user_id to IAM (D-2); APPLICABLEUNITS -> doctor_unit.';
COMMENT ON COLUMN ihms_master.doctor.mobile_no IS 'Legacy MOBILENO is Long; VARCHAR(16) preserves leading zeros (MIG-001 conversion audit).';

CREATE TABLE ihms_master.doctor_unit (
    id BIGINT NOT NULL, unit_id BIGINT NOT NULL, doctor_id BIGINT NOT NULL REFERENCES ihms_master.doctor (id),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_doctor_unit PRIMARY KEY (id),
    CONSTRAINT uq_doctor_unit UNIQUE (doctor_id, unit_id)
);

CREATE TABLE ihms_master.doctor_department (
    id BIGINT NOT NULL, unit_id BIGINT,
    doctor_id BIGINT NOT NULL REFERENCES ihms_master.doctor (id),
    department_id BIGINT NOT NULL,
    is_default_doctor BOOLEAN NOT NULL DEFAULT FALSE,
    max_patients_per_day INTEGER,
    building_ids JSONB,
    is_unit_head BOOLEAN NOT NULL DEFAULT FALSE,
    clinical_level_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_doctor_department PRIMARY KEY (id),
    CONSTRAINT uq_doctor_department UNIQUE (doctor_id, department_id)
);

-- ---------------------------------------------------------------------------
-- Sequences (IMP-DEC-003: explicit per migrated table; seeded MAX(id)+1 by MIG-001)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE ihms_master.unit_seq              START WITH 1;
CREATE SEQUENCE ihms_master.unit_config_seq       START WITH 1;
CREATE SEQUENCE ihms_master.subunit_seq           START WITH 1;
CREATE SEQUENCE ihms_master.facility_location_seq START WITH 1;
CREATE SEQUENCE ihms_master.geo_country_seq       START WITH 1;
CREATE SEQUENCE ihms_master.geo_state_seq         START WITH 1;
CREATE SEQUENCE ihms_master.geo_district_seq      START WITH 1;
CREATE SEQUENCE ihms_master.geo_city_seq          START WITH 1;
CREATE SEQUENCE ihms_master.geo_area_seq          START WITH 1;
CREATE SEQUENCE ihms_master.doctor_type_seq       START WITH 1;
CREATE SEQUENCE ihms_master.specialization_seq    START WITH 1;
CREATE SEQUENCE ihms_master.doctor_seq            START WITH 1;
CREATE SEQUENCE ihms_master.doctor_unit_seq       START WITH 1;
CREATE SEQUENCE ihms_master.doctor_department_seq START WITH 1;
