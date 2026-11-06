-- MIG-023 sub-slice (S18): diet order + food item schedule lifecycle — clean target schema.
-- Single-writer ownership: diet-service is the sole writer of ihms_diet
-- (same extraction pattern as ihms_item / ihms_grievance / ihms_waste / ihms_linen /
-- ihms_transport, IMP-DEC-026/032/033/034/035).
--
-- Legacy sources (evidence authority, /mnt/d/ihms read-only):
--   M_DIETITEMMASTER          -> diet_item_master           (Dietitemmaster.java)
--   M_DIETSESSIONMASTER       -> diet_session_master        (Dietsessionmaster.java)
--   M_DIETPLANMASTER          -> diet_plan_master           (Dietplanmaster.java)
--   M_DIETPLANDETAILS         -> diet_plan_details          (Dietplandetails.java)
--   T_TODAYDIETMENU           -> today_diet_menu            (TodayDietItemMenu.java)
--   T_DIETORDERDETAILS        -> diet_order_details         (DietOrderDetails.java)
--   T_DIETITEMSCHEDULEDETAILS -> diet_item_schedule_details (DietItemScheduleDetails.java)
--
-- Status dictionaries (legacy DietServiceImpl + DietActionStatus enum):
--   diet_order_details.order_status:
--     1=UNRELEASED 2=ACTIVE/SIGNED 3=CANCELLED/REPLACED
--   diet_item_schedule_details.item_schedule_status:
--     2=REQUEST_RAISED (default on order); increments toward 6=COMPLETED/LOCKED;
--     rows with item_schedule_status < 6 are replaceable (legacy replace query).
--   diet_item_schedule_details.diet_action_status (enum, nullable):
--     HOLD / CANCEL / REJECT / RELEASE
--
-- Order number rule (legacy getLatestAutocodeSeq on activity 't_dietorderdetails',
-- template null -> raw autocode string): order_number = zero-padded sequence value.
--
-- Cross-domain references deferred (dependency-neutral): diet_order_details and
-- diet_item_schedule_details store admission_id / patient_id / ward_id / bed_id /
-- doctor_id / service_id as opaque caller-supplied FKs (no FK constraint) — the
-- patient/admission/ward/bed/doctor/service decoration read-models (getDietOrderList,
-- getDietOrderListCount, getDietKichenDetails, getWardWisePatDtls, getActiveWards)
-- JOIN t_admission / t_registration / m_bedmaster / m_wardmaster / m_servicemaster and
-- are deferred until the admission/registration integration slice.

CREATE SCHEMA IF NOT EXISTS ihms_diet;

-- ------------------------------------------------------- diet_item_master --

CREATE TABLE IF NOT EXISTS ihms_diet.diet_item_master (
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
    diet_item_type VARCHAR(255),
    diet_item_ien BIGINT,
    description VARCHAR(255),
    energy BIGINT,
    diet_type_ien VARCHAR(255),
    diet_session_id VARCHAR(255),
    local_unit VARCHAR(255),
    CONSTRAINT pk_diet_item_master PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_diet_item_master_unit ON ihms_diet.diet_item_master (unit_id);

-- ---------------------------------------------------- diet_session_master --

CREATE TABLE IF NOT EXISTS ihms_diet.diet_session_master (
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
    description VARCHAR(255),
    from_time TIMESTAMP(3),
    to_time TIMESTAMP(3),
    diet_sequence_number BIGINT,
    CONSTRAINT pk_diet_session_master PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_diet_session_master_unit ON ihms_diet.diet_session_master (unit_id);

-- ------------------------------------------------------ diet_plan_master --

CREATE TABLE IF NOT EXISTS ihms_diet.diet_plan_master (
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
    code VARCHAR(64),
    description VARCHAR(255),
    plan_information BYTEA,
    service_id BIGINT,
    CONSTRAINT pk_diet_plan_master PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_diet_plan_master_unit ON ihms_diet.diet_plan_master (unit_id);

-- ----------------------------------------------------- diet_plan_details --

CREATE TABLE IF NOT EXISTS ihms_diet.diet_plan_details (
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
    plan_id BIGINT REFERENCES ihms_diet.diet_plan_master (id),
    item_id BIGINT,
    timing BIGINT,
    item_qty VARCHAR(64),
    item_unit VARCHAR(64),
    item_cal VARCHAR(64),
    item_ch VARCHAR(64),
    item_fat VARCHAR(64),
    item_expected_cal VARCHAR(64),
    item_instruction BYTEA,
    item_protein NUMERIC(19,4),
    CONSTRAINT pk_diet_plan_details PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_diet_plan_details_plan ON ihms_diet.diet_plan_details (plan_id);

-- ------------------------------------------------------- today_diet_menu --

CREATE TABLE IF NOT EXISTS ihms_diet.today_diet_menu (
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
    diet_type_id BIGINT,
    description VARCHAR(255),
    diet_session_id BIGINT,
    item_id BIGINT,
    CONSTRAINT pk_today_diet_menu PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_today_diet_menu_unit ON ihms_diet.today_diet_menu (unit_id);
CREATE INDEX IF NOT EXISTS ix_today_diet_menu_type ON ihms_diet.today_diet_menu (diet_type_id);
CREATE INDEX IF NOT EXISTS ix_today_diet_menu_session ON ihms_diet.today_diet_menu (diet_session_id);

-- ---------------------------------------------------- diet_order_details --

CREATE TABLE IF NOT EXISTS ihms_diet.diet_order_details (
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
    order_number VARCHAR(64),
    patient_id BIGINT,
    admission_id BIGINT,
    ward_id BIGINT,
    bed_id BIGINT,
    doctor_id BIGINT,
    service_id BIGINT,
    from_date TIMESTAMP(3),
    to_date TIMESTAMP(3),
    remarks VARCHAR(500),
    order_id BIGINT,
    order_status BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_diet_order_details PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_diet_order_details_unit ON ihms_diet.diet_order_details (unit_id);
CREATE INDEX IF NOT EXISTS ix_diet_order_details_admission ON ihms_diet.diet_order_details (admission_id);
CREATE INDEX IF NOT EXISTS ix_diet_order_details_patient ON ihms_diet.diet_order_details (patient_id);
CREATE INDEX IF NOT EXISTS ix_diet_order_details_status ON ihms_diet.diet_order_details (order_status);

-- -------------------------------------------- diet_item_schedule_details --

CREATE TABLE IF NOT EXISTS ihms_diet.diet_item_schedule_details (
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
    admission_id BIGINT,
    order_id BIGINT REFERENCES ihms_diet.diet_order_details (id),
    session_id BIGINT,
    item_ids VARCHAR(1000),
    item_schedule_status BIGINT NOT NULL DEFAULT 2,
    diet_action_status VARCHAR(16),
    reason_for_hold VARCHAR(500),
    diet_status_time TIMESTAMP(3),
    food_ordered_date TIMESTAMP(3),
    present_calories BIGINT,
    CONSTRAINT pk_diet_item_schedule_details PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_diet_schedule_unit ON ihms_diet.diet_item_schedule_details (unit_id);
CREATE INDEX IF NOT EXISTS ix_diet_schedule_order ON ihms_diet.diet_item_schedule_details (order_id);
CREATE INDEX IF NOT EXISTS ix_diet_schedule_admission ON ihms_diet.diet_item_schedule_details (admission_id);
CREATE INDEX IF NOT EXISTS ix_diet_schedule_session ON ihms_diet.diet_item_schedule_details (session_id);
CREATE INDEX IF NOT EXISTS ix_diet_schedule_food_date ON ihms_diet.diet_item_schedule_details (food_ordered_date);

-- -------------------------------------------------------------- sequences --
-- Explicit sequences per migrated table (IMP-DEC-003; no BIGSERIAL).

CREATE SEQUENCE IF NOT EXISTS ihms_diet.diet_item_master_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_diet.diet_session_master_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_diet.diet_plan_master_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_diet.diet_plan_details_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_diet.today_diet_menu_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_diet.diet_order_details_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_diet.diet_item_schedule_details_seq START WITH 1 INCREMENT BY 1;
-- Order-number sequence (legacy CIMS_GETLATESTAUTOCODEANDINCREMETITBYONE on
-- activity 't_dietorderdetails').
CREATE SEQUENCE IF NOT EXISTS ihms_diet.diet_order_number_seq START WITH 1 INCREMENT BY 1;
