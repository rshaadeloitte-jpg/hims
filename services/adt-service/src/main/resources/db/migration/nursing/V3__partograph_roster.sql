-- ============================================================================
-- V3 — ihms_nursing: Partograph (labour monitoring) and Nurse Shift Roster
-- Ported from legacy Partograph matrix (PatientPartographDetailsDto,
-- PartographHFConstants) and nurse shift rostering (NurseShiftDetailsDto,
-- NurseDepartmentAllocationDetailsResultDto).
--
-- Partograph tracks labour progress: cervix dilation, foetal descent,
-- vitals, membrane status, and delivery outcome. Nurse shift roster
-- manages duty assignments by ward + shift per day.
-- ============================================================================

SET search_path TO ihms_nursing, public;

-- ---------------------------------------------------------------------------
-- t_partograph — labour monitoring chart per admission
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_partograph (
    partograph_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,          -- opaque id in phase 1 (no FK by design)
    admission_id BIGINT NOT NULL,        -- opaque id in phase 1 (no FK by design)
    opd_ipd_id BIGINT,                   -- legacy port: links to opd/ipd context
    opd_ipd BIGINT,                      -- 1=OPD 2=IPD discriminator (legacy)
    parity VARCHAR(20),                  -- G/P parity (e.g. G2P1)
    start_of_labour_date VARCHAR(50),    -- legacy string-format labour start
    membrane_status VARCHAR(20),         -- INTACT / RUPTURED / ARTIFICIAL
    membrane_rupture_date TIMESTAMP(3),
    membrane_vital_abb VARCHAR(50),      -- legacy abbreviation (e.g. "SROM", "ARM")
    cervix_dilation NUMERIC(4,1),        -- cm (0-10)
    descent INTEGER,                     -- station (-5 to +5)
    systolic_bp INTEGER,
    diastolic_bp INTEGER,
    heart_rate INTEGER,
    temperature NUMERIC(4,1),            -- °C
    respiration_rate INTEGER,
    oxygen_saturation NUMERIC(5,2),      -- %
    contraction_freq INTEGER,            -- per 10 min
    contraction_duration INTEGER,        -- seconds
    foetal_heart_rate INTEGER,           -- bpm
    moulding INTEGER,                    -- 0/1/2/3
    liquor VARCHAR(20),                  -- CLEAR / MECONIUM / BLOOD / ABSENT
    drugs_and_fluid TEXT,
    oxytocin VARCHAR(100),
    urine_output INTEGER,                -- mL
    urine_acetone VARCHAR(20),
    urine_protein VARCHAR(20),
    delivery_date_time VARCHAR(50),       -- legacy string-format delivery timestamp
    delivery_outcome VARCHAR(50),        -- NVD / CS / ASSISTED / etc.
    baby_weight NUMERIC(6,2),            -- grams
    apgar_score_1min INTEGER,
    apgar_score_5min INTEGER,
    apgar_score_10min INTEGER,
    apgar_score_15min INTEGER,
    apgar_score_20min INTEGER,
    placenta_type VARCHAR(50),
    blood_loss_ml INTEGER,
    remarks TEXT,
    CONSTRAINT pk_t_partograph PRIMARY KEY (partograph_id),
    CONSTRAINT ck_t_partograph_cervix CHECK (cervix_dilation IS NULL OR (cervix_dilation >= 0 AND cervix_dilation <= 10)),
    CONSTRAINT ck_t_partograph_descent CHECK (descent IS NULL OR (descent >= -5 AND descent <= 5)),
    CONSTRAINT ck_t_partograph_bp CHECK (systolic_bp IS NULL OR (systolic_bp >= 0 AND systolic_bp <= 300)),
    CONSTRAINT ck_t_partograph_temp CHECK (temperature IS NULL OR (temperature >= 30 AND temperature <= 45))
);
CREATE INDEX IF NOT EXISTS ix_t_partograph_patient ON ihms_nursing.t_partograph (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_partograph_admission ON ihms_nursing.t_partograph (admission_id);
COMMENT ON TABLE ihms_nursing.t_partograph IS 'Labour partograph (M09 gap). Tracks cervix dilation, descent, vitals, membrane status, delivery outcome per admission. patient_id/admission_id opaque ids in phase 1.';

-- ---------------------------------------------------------------------------
-- t_nurse_roster — duty rostering by ward + shift per day
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_nurse_roster (
    roster_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    roster_date DATE NOT NULL,
    ward_id BIGINT NOT NULL,
    nurse_id BIGINT NOT NULL,
    shift VARCHAR(20) NOT NULL,          -- MORNING / EVENING / NIGHT
    bed_id BIGINT,                       -- legacy port (optional bed assignment)
    is_allied BOOLEAN NOT NULL DEFAULT FALSE,  -- allied staff flag
    CONSTRAINT pk_t_nurse_roster PRIMARY KEY (roster_id),
    CONSTRAINT uq_t_nurse_roster_ward_shift UNIQUE (roster_date, ward_id, nurse_id, shift)
);
CREATE INDEX IF NOT EXISTS ix_t_nurse_roster_date_ward ON ihms_nursing.t_nurse_roster (roster_date, ward_id);
CREATE INDEX IF NOT EXISTS ix_t_nurse_roster_nurse ON ihms_nursing.t_nurse_roster (nurse_id, roster_date);
COMMENT ON TABLE ihms_nursing.t_nurse_roster IS 'Nurse shift roster (M09 gap). One assignment per nurse per ward+shift+day. shift=MORNING/EVENING/NIGHT.';

-- Sequences
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_partograph_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_nurse_roster_seq START WITH 1;
