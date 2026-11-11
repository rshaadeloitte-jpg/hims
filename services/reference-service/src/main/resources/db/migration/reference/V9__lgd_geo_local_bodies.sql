-- ============================================================================
-- V9 — LGD geographic hierarchy + local bodies (ihms_master)
-- Schema + read API only; no import (platform/data-import owns population).
-- ============================================================================
SET search_path TO ihms_master, public;

CREATE TABLE ihms_master.geo_sub_district (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    district_id BIGINT NOT NULL REFERENCES ihms_master.geo_district (id),
    code VARCHAR(32) NOT NULL,
    lgd_version INTEGER,
    name VARCHAR(255) NOT NULL,
    name_local VARCHAR(255),
    census_2001_code VARCHAR(16),
    census_2011_code VARCHAR(16),
    CONSTRAINT pk_geo_sub_district PRIMARY KEY (id),
    CONSTRAINT uq_geo_sub_district_district_code UNIQUE (district_id, code)
);

CREATE TABLE ihms_master.geo_village (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    sub_district_id BIGINT NOT NULL REFERENCES ihms_master.geo_sub_district (id),
    code VARCHAR(32) NOT NULL,
    lgd_version INTEGER,
    name VARCHAR(255) NOT NULL,
    name_local VARCHAR(255),
    village_status VARCHAR(64),
    census_2001_code VARCHAR(16),
    census_2011_code VARCHAR(16),
    CONSTRAINT pk_geo_village PRIMARY KEY (id),
    CONSTRAINT uq_geo_village_code UNIQUE (code)
);

CREATE TABLE ihms_master.geo_block (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    district_id BIGINT NOT NULL REFERENCES ihms_master.geo_district (id),
    code VARCHAR(32) NOT NULL,
    lgd_version INTEGER,
    name VARCHAR(255) NOT NULL,
    name_local VARCHAR(255),
    CONSTRAINT pk_geo_block PRIMARY KEY (id),
    CONSTRAINT uq_geo_block_code UNIQUE (code)
);

CREATE TABLE ihms_master.geo_block_village (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    block_id BIGINT NOT NULL REFERENCES ihms_master.geo_block (id),
    village_id BIGINT NOT NULL REFERENCES ihms_master.geo_village (id),
    CONSTRAINT pk_geo_block_village PRIMARY KEY (id),
    CONSTRAINT uq_geo_block_village_pair UNIQUE (block_id, village_id)
);

CREATE TABLE ihms_master.lgd_local_body (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    state_id BIGINT NOT NULL REFERENCES ihms_master.geo_state (id),
    body_category VARCHAR(3) NOT NULL CHECK (body_category IN ('PRI','ULB','TLB')),
    type_code VARCHAR(16),
    type_name VARCHAR(255),
    code VARCHAR(32) NOT NULL,
    lgd_version INTEGER,
    name VARCHAR(255) NOT NULL,
    name_local VARCHAR(255),
    parent_code VARCHAR(32),
    census_2001_code VARCHAR(16),
    census_2011_code VARCHAR(16),
    CONSTRAINT pk_lgd_local_body PRIMARY KEY (id),
    CONSTRAINT uq_lgd_local_body_code UNIQUE (code)
);

CREATE TABLE ihms_master.lgd_ward (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    local_body_id BIGINT NOT NULL REFERENCES ihms_master.lgd_local_body (id),
    ward_category VARCHAR(3) NOT NULL CHECK (ward_category IN ('PRI','ULB')),
    code VARCHAR(32) NOT NULL,
    ward_number VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    name_local VARCHAR(255),
    CONSTRAINT pk_lgd_ward PRIMARY KEY (id),
    CONSTRAINT uq_lgd_ward_code UNIQUE (code)
);

-- Coverage: a ULB ward's reach over district + sub-district (uLBWardforStateWithCov).
CREATE TABLE ihms_master.lgd_ward_coverage (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    ward_id BIGINT NOT NULL REFERENCES ihms_master.lgd_ward (id),
    district_id BIGINT NOT NULL REFERENCES ihms_master.geo_district (id),
    sub_district_id BIGINT REFERENCES ihms_master.geo_sub_district (id),
    CONSTRAINT pk_lgd_ward_coverage PRIMARY KEY (id),
    CONSTRAINT uq_lgd_ward_coverage_pair UNIQUE NULLS NOT DISTINCT (ward_id, district_id, sub_district_id)
);

CREATE TABLE ihms_master.lgd_village_gram_panchayat (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    village_id BIGINT NOT NULL REFERENCES ihms_master.geo_village (id),
    local_body_id BIGINT NOT NULL REFERENCES ihms_master.lgd_local_body (id),
    CONSTRAINT pk_lgd_village_gram_panchayat PRIMARY KEY (id),
    CONSTRAINT uq_lgd_village_gp_pair UNIQUE (village_id, local_body_id)
);

-- sequences
CREATE SEQUENCE ihms_master.geo_sub_district_seq START WITH 1;
CREATE SEQUENCE ihms_master.geo_village_seq START WITH 1;
CREATE SEQUENCE ihms_master.geo_block_seq START WITH 1;
CREATE SEQUENCE ihms_master.geo_block_village_seq START WITH 1;
CREATE SEQUENCE ihms_master.lgd_local_body_seq START WITH 1;
CREATE SEQUENCE ihms_master.lgd_ward_seq START WITH 1;
CREATE SEQUENCE ihms_master.lgd_ward_coverage_seq START WITH 1;
CREATE SEQUENCE ihms_master.lgd_village_gram_panchayat_seq START WITH 1;

-- FK indexes
CREATE INDEX ix_geo_sub_district_district ON ihms_master.geo_sub_district (district_id);
CREATE INDEX ix_geo_village_sub_district ON ihms_master.geo_village (sub_district_id);
CREATE INDEX ix_geo_block_district ON ihms_master.geo_block (district_id);
CREATE INDEX ix_geo_block_village_block ON ihms_master.geo_block_village (block_id);
CREATE INDEX ix_geo_block_village_village ON ihms_master.geo_block_village (village_id);
CREATE INDEX ix_lgd_local_body_state ON ihms_master.lgd_local_body (state_id);
CREATE INDEX ix_lgd_ward_local_body ON ihms_master.lgd_ward (local_body_id);
CREATE INDEX ix_lgd_ward_coverage_ward ON ihms_master.lgd_ward_coverage (ward_id);
CREATE INDEX ix_lgd_ward_coverage_district ON ihms_master.lgd_ward_coverage (district_id);
CREATE INDEX ix_lgd_ward_coverage_sub_district ON ihms_master.lgd_ward_coverage (sub_district_id);
CREATE INDEX ix_lgd_village_gp_village ON ihms_master.lgd_village_gram_panchayat (village_id);
CREATE INDEX ix_lgd_village_gp_local_body ON ihms_master.lgd_village_gram_panchayat (local_body_id);

-- lower(name) indexes
CREATE INDEX ix_geo_village_lower_name ON ihms_master.geo_village (lower(name));
CREATE INDEX ix_lgd_local_body_lower_name ON ihms_master.lgd_local_body (lower(name));
CREATE INDEX ix_geo_block_lower_name ON ihms_master.geo_block (lower(name));
