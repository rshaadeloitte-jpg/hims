-- ============================================================================
-- V2 — M03 ADT gaps (Phase 2, MEDIUM): interim admission, attendant pass,
-- census/TID/death handling. Ported from legacy:
--  - AttendantPassDetails (T_ATTENDANTPASSDETAILS, PassNo/Status/PrintCount)
--  - Interim admission (bed_id NULL sweep -> AUTO_ABSCONDED, BR-IPD-005)
--  - Census (m_ward.iscensus + m_bed occupancy)
--  - TID list (ipd_no / external TID per RGHS)
--  - Death handling (discharge is_expired + death_date + cause)
-- ponytail: single migration, PG bytea not needed.
-- ============================================================================

SET search_path TO ihms_adt, public;

-- ---------------------------------------------------------------------------
-- t_admission delta — interim flag, TID, death cause
-- interim: bed_id NULL + is_interim TRUE (legacy interim admission has no bed)
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_adt.t_admission
    ADD COLUMN IF NOT EXISTS is_interim BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS tid VARCHAR(50),
    ADD COLUMN IF NOT EXISTS death_cause VARCHAR(255),
    ADD COLUMN IF NOT EXISTS death_type VARCHAR(50);

CREATE INDEX IF NOT EXISTS ix_t_admission_tid ON ihms_adt.t_admission (tid) WHERE tid IS NOT NULL;
COMMENT ON COLUMN ihms_adt.t_admission.is_interim IS 'True for interim/unbedded admissions: bed_id NULL, swept to AUTO_ABSCONDED after cutoff (BR-IPD-005).';
COMMENT ON COLUMN ihms_adt.t_admission.tid IS 'External transaction ID (RGHS/TPA TID) linked to admission for M03 TID list.';
COMMENT ON COLUMN ihms_adt.t_admission.death_cause IS 'Death cause when is_expired=true (M03 death handling).';

-- Sequences for attendant pass
CREATE SEQUENCE IF NOT EXISTS ihms_adt.t_attendant_pass_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_adt.t_attendant_pass_no_seq START WITH 1 INCREMENT BY 1;

-- ---------------------------------------------------------------------------
-- t_attendant_pass — clean target of T_ATTENDANTPASSDETAILS
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_adt.t_attendant_pass (
    pass_id BIGINT NOT NULL DEFAULT nextval('ihms_adt.t_attendant_pass_seq'),
    admission_id BIGINT NOT NULL REFERENCES ihms_adt.t_admission (admission_id),
    patient_id BIGINT NOT NULL,
    attendee_name VARCHAR(255),
    pass_no VARCHAR(50) NOT NULL,
    pass_status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    pass_price BIGINT,
    print_count BIGINT NOT NULL DEFAULT 1,
    max_refundable BIGINT,
    return_date TIMESTAMP(3),
    -- audit
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_t_attendant_pass PRIMARY KEY (pass_id),
    CONSTRAINT uq_t_attendant_pass_no UNIQUE (pass_no),
    CONSTRAINT ck_t_attendant_pass_status CHECK (pass_status IN ('ISSUED','RETURNED'))
);
CREATE INDEX IF NOT EXISTS ix_t_attendant_pass_admission ON ihms_adt.t_attendant_pass (admission_id);
CREATE INDEX IF NOT EXISTS ix_t_attendant_pass_patient ON ihms_adt.t_attendant_pass (patient_id);
COMMENT ON TABLE ihms_adt.t_attendant_pass IS 'Attendant pass (ported from T_ATTENDANTPASSDETAILS). pass_no = ATT/<yy>/<seq>, status ISSUED -> RETURNED on return.';

-- Seed TID-linked admission example handled by service, not seed
