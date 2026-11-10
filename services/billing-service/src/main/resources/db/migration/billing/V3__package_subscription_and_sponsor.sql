-- ============================================================================
-- V3 — Package subscription + sponsor change stub (M04 gap slice)
-- Ported from old IHMS T_PATIENTPACKAGESUBSCRIPTION (patientId, visitAdmId,
-- packageId, chargeId, fromDate/toDate, status) + sponsor change
-- UpdatePatientSponsorNewDto (companyId/planId mid-bill).
-- Minimal: subscription tied to patient+package optionally to bill; sponsor
-- stored on bill + subscription for audit. Reuses ihms_billing sequences.
-- ============================================================================

SET search_path TO ihms_billing, public;

-- sponsor columns on bill (stub for mid-bill sponsor change, BR-SPONSOR-001)
ALTER TABLE ihms_billing.t_bill
    ADD COLUMN IF NOT EXISTS sponsor_company_id BIGINT,
    ADD COLUMN IF NOT EXISTS sponsor_plan_id BIGINT;

CREATE INDEX IF NOT EXISTS ix_t_bill_sponsor ON ihms_billing.t_bill (sponsor_company_id);

COMMENT ON COLUMN ihms_billing.t_bill.sponsor_company_id IS 'M04 sponsor change stub: companyId to bill non-self portion to (UpdatePatientSponsorNewDto port). NULL = self-pay. Set via PUT /bills/{id}/sponsor.';
COMMENT ON COLUMN ihms_billing.t_bill.sponsor_plan_id IS 'Associated planId for sponsor (health plan / tariff).';

-- t_package_subscription — clean target of T_PATIENTPACKAGESUBSCRIPTION
CREATE TABLE IF NOT EXISTS ihms_billing.t_package_subscription (
    subscription_id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    patient_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    bill_id BIGINT, -- nullable: subscription may precede bill
    subscription_no VARCHAR(32) NOT NULL,
    from_date TIMESTAMP(3),
    to_date TIMESTAMP(3),
    covered_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sponsor_company_id BIGINT,

    CONSTRAINT pk_t_package_subscription PRIMARY KEY (subscription_id),
    CONSTRAINT ck_t_package_covered_nonneg CHECK (covered_amount >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_package_subscription_no ON ihms_billing.t_package_subscription (subscription_no);
CREATE INDEX IF NOT EXISTS ix_package_subscription_patient ON ihms_billing.t_package_subscription (patient_id);
CREATE INDEX IF NOT EXISTS ix_package_subscription_package ON ihms_billing.t_package_subscription (package_id);
CREATE INDEX IF NOT EXISTS ix_package_subscription_bill ON ihms_billing.t_package_subscription (bill_id);

CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_package_subscription_seq START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_package_subscription_no_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

COMMENT ON TABLE ihms_billing.t_package_subscription IS 'Package subscription (M04 gap, minimal slice). Ported from T_PATIENTPACKAGESUBSCRIPTION: patient subscribes to packageId, optionally tied to billId, validity from_date..to_date, covered_amount via billing rule. is_active=false = cancelled/released.';
