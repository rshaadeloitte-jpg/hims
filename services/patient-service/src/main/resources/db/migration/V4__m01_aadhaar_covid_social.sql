-- ============================================================================
-- V4 — M01 remaining: Aadhaar OTP/biometric audit, COVID tracking, Social Worker assessment
-- Ported from ihms/src:
--   Aadhaar 12-digit (IntegrationServiceImpl aadhaarAuthenticate / aadhaarAuthEncryptKey,
--     patient_registration.aadhaar_no VARCHAR(12)) + biometricDetailsJson (FamilyMemberDetails)
--   COVID flag (CoronaStats.covidStatus / CoronaStatsDetails, PatientServiceHelper:4493 covidStatus → CoronaStats)
--   Social assessment (QuetionnaireServiceImpl isSocialAssessment → module "Social Assesment")
-- ============================================================================

SET search_path TO ihms_patient, public;

-- ---------------------------------------------------------------------------
-- COVID tracking on patient_registration (M01: COVID tracking gap)
-- Legacy stored COVID in T_CORONASTATS (separate table) linked by ihmsPatientId;
-- here we denormalize a current status onto the registration row for fast filter
-- (ponytail: one column beats a join; full history table add when volume proves).
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_patient.patient_registration
    ADD COLUMN IF NOT EXISTS covid_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS covid_updated_at TIMESTAMP(3);

COMMENT ON COLUMN ihms_patient.patient_registration.covid_status IS 'Current COVID status (POSITIVE/NEGATIVE/SUSPECTED/RECOVERED etc) — ported from CoronaStats.covidStatus. NULL = not tracked.';
CREATE INDEX IF NOT EXISTS ix_patient_covid_status ON ihms_patient.patient_registration (covid_status) WHERE covid_status IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Aadhaar verification audit (M01: Aadhaar OTP/biometric gap)
-- Ported from IntegrationServiceImpl.encryptAadhaarAuth / aadhaarAuthenticate (AES/CBC)
-- and FamilyMemberDetails.biometricDetailsJson. Each row is one OTP or biometric
-- verification attempt; OTP is 6-digit, 5-min TTL, max 3 attempts (matches iam OtpService).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_patient.patient_aadhaar_verification (
    id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    patient_id BIGINT NOT NULL REFERENCES ihms_patient.patient_registration (id),
    aadhaar_no VARCHAR(12) NOT NULL,
    verification_type VARCHAR(16) NOT NULL, -- OTP or BIOMETRIC
    otp VARCHAR(6),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP(3),
    device_id VARCHAR(64),
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP(3),

    CONSTRAINT pk_patient_aadhaar_verification PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_aadhaar_verif_patient ON ihms_patient.patient_aadhaar_verification (patient_id);
CREATE INDEX IF NOT EXISTS ix_aadhaar_verif_aadhaar ON ihms_patient.patient_aadhaar_verification (aadhaar_no);
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_aadhaar_verif_seq" START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE ihms_patient.patient_aadhaar_verification IS 'Aadhaar OTP/biometric verification audit (M01 gap). Ported from IntegrationServiceImpl Aadhaar flow + biometricDetailsJson.';

-- ---------------------------------------------------------------------------
-- Social Worker assessment (M01: Social Worker assessment gap)
-- Ported from QuetionnaireServiceImpl isSocialAssessment flag (module "Social Assesment").
-- Stored as JSONB so the questionnaire schema can evolve without DDL (same pattern as file-service annotation).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_patient.patient_social_assessment (
    id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    patient_id BIGINT NOT NULL REFERENCES ihms_patient.patient_registration (id),
    assessment_data JSONB NOT NULL,
    social_worker_id BIGINT,
    remarks TEXT,

    CONSTRAINT pk_patient_social_assessment PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_social_assess_patient ON ihms_patient.patient_social_assessment (patient_id);
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_social_assess_seq" START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE ihms_patient.patient_social_assessment IS 'Social Worker assessment (M01 gap: isSocialAssessment → Social Assesment module). JSONB payload, one row per assessment.';
