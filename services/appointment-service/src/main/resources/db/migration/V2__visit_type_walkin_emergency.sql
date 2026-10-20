-- ============================================================================
-- V2 — Visit type for walk-in and emergency registration (M02 gaps)
-- Ported from old IHMS: EmergencyregtypeMaster, walk-in visit flow.
-- Adds visit_type column to distinguish APPOINTMENT vs WALK_IN vs EMERGENCY.
-- ============================================================================

SET search_path TO ihms_appointment, public;

-- ---------------------------------------------------------------------------
-- Visit type on appointment (M02: walk-in visit flow, emergency registration)
-- Default 'APPOINTMENT' preserves backward compatibility with existing rows.
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_appointment.appointment
    ADD COLUMN IF NOT EXISTS visit_type VARCHAR(16) NOT NULL DEFAULT 'APPOINTMENT';

COMMENT ON COLUMN ihms_appointment.appointment.visit_type IS 'Visit type: APPOINTMENT (pre-booked), WALK_IN (no slot), EMERGENCY (M02 gap: walk-in + emergency registration)';

CREATE INDEX IF NOT EXISTS ix_appointment_visit_type ON ihms_appointment.appointment (visit_type);
