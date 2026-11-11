-- ============================================================================
-- V5 — M06 Doctor Share module: t_visit_charge, t_doctor_incentive
-- Ported from legacy: T_DOCTORVISITCHARGES (Doctorvisitcharge model),
-- EjsIncentives (incentive tracking for doctors).
--
-- t_visit_charge — visit-type-based charges for doctors (OPD/IPD visit fees).
--   One row per doctor-service-visitType combination.
--
-- t_doctor_incentive — fixed addition/deduction amounts for doctors.
--   incentive_type: ADDITION or DEDUCTION.
--   is_billed=false until downstream accrual consumes the row.
-- ============================================================================

SET search_path TO ihms_billing, public;

-- ---------------------------------------------------------------------------
-- t_visit_charge — visit-type-based doctor charges
-- Ported from legacy T_DOCTORVISITCHARGES (Doctorvisitcharge model).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_visit_charge (
    visit_charge_id         BIGINT       NOT NULL,
    unit_id                 BIGINT,
    status                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id         BIGINT,
    updated_unit_id         BIGINT,
    added_by                BIGINT,
    added_datetime          TIMESTAMP(3),
    updated_by              BIGINT,
    updated_datetime        TIMESTAMP(3),
    synchronized            BOOLEAN      NOT NULL DEFAULT FALSE,

    doctor_id               BIGINT       NOT NULL,
    service_id              BIGINT,
    tariff_service_id       BIGINT,
    department_id           BIGINT,
    ward_id                 BIGINT,
    visit_type              BIGINT,
    class_id                BIGINT,
    priority_id             BIGINT,
    amount                  NUMERIC(19,2) NOT NULL,

    CONSTRAINT pk_t_visit_charge PRIMARY KEY (visit_charge_id),
    CONSTRAINT ck_t_visit_charge_amount_nonneg CHECK (amount >= 0)
);
CREATE INDEX IF NOT EXISTS ix_t_visit_charge_doctor ON ihms_billing.t_visit_charge (doctor_id);
CREATE INDEX IF NOT EXISTS ix_t_visit_charge_service ON ihms_billing.t_visit_charge (service_id);
CREATE INDEX IF NOT EXISTS ix_t_visit_charge_dept ON ihms_billing.t_visit_charge (department_id);
CREATE INDEX IF NOT EXISTS ix_t_visit_charge_visit_type ON ihms_billing.t_visit_charge (visit_type);
COMMENT ON TABLE ihms_billing.t_visit_charge IS 'M06 Doctor Share: visit-type-based doctor charges (OPD/IPD visit fees). Ported from legacy T_DOCTORVISITCHARGES.';

-- ---------------------------------------------------------------------------
-- t_doctor_incentive — fixed addition/deduction amounts for doctors
-- Ported from legacy EjsIncentives model.
-- incentive_type: ADDITION (credit) or DEDUCTION (debit).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_doctor_incentive (
    incentive_id            BIGINT       NOT NULL,
    unit_id                 BIGINT,
    status                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id         BIGINT,
    updated_unit_id         BIGINT,
    added_by                BIGINT,
    added_datetime          TIMESTAMP(3),
    updated_by              BIGINT,
    updated_datetime        TIMESTAMP(3),
    synchronized            BOOLEAN      NOT NULL DEFAULT FALSE,

    doctor_id               BIGINT       NOT NULL,
    incentive_type          VARCHAR(20)  NOT NULL,   -- 'ADDITION' or 'DEDUCTION'
    amount                  NUMERIC(19,2) NOT NULL,
    remark                  VARCHAR(500),
    is_billed               BOOLEAN      NOT NULL DEFAULT FALSE,
    incentive_date          TIMESTAMP(3),

    CONSTRAINT pk_t_doctor_incentive PRIMARY KEY (incentive_id),
    CONSTRAINT ck_t_doctor_incentive_type CHECK (incentive_type IN ('ADDITION', 'DEDUCTION')),
    CONSTRAINT ck_t_doctor_incentive_amount_nonneg CHECK (amount >= 0)
);
CREATE INDEX IF NOT EXISTS ix_t_doctor_incentive_doctor ON ihms_billing.t_doctor_incentive (doctor_id);
CREATE INDEX IF NOT EXISTS ix_t_doctor_incentive_type ON ihms_billing.t_doctor_incentive (incentive_type);
CREATE INDEX IF NOT EXISTS ix_t_doctor_incentive_billed ON ihms_billing.t_doctor_incentive (is_billed);
CREATE INDEX IF NOT EXISTS ix_t_doctor_incentive_doctor_billed ON ihms_billing.t_doctor_incentive (doctor_id, is_billed);
COMMENT ON TABLE ihms_billing.t_doctor_incentive IS 'M06 Doctor Share: fixed addition/deduction amounts for doctors. Ported from legacy EjsIncentives.';

-- ---------------------------------------------------------------------------
-- Sequences
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_visit_charge_seq    START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_doctor_incentive_seq START WITH 1 INCREMENT BY 1 NO CYCLE;
