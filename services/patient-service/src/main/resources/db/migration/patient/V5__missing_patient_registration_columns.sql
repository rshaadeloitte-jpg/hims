SET search_path TO ihms_patient, public;

ALTER TABLE ihms_patient.patient_registration
    ADD COLUMN IF NOT EXISTS civil_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS family_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS family_member_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_death BOOLEAN,
    ADD COLUMN IF NOT EXISTS marital_status VARCHAR(255),
    ADD COLUMN IF NOT EXISTS passport_no VARCHAR(255),
    ADD COLUMN IF NOT EXISTS prefix VARCHAR(255),
    ADD COLUMN IF NOT EXISTS religion VARCHAR(255);
