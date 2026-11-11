-- V4 — Nursing Service M09 gaps: additional indexes + audit columns for services
SET search_path TO ihms_nursing, public;

CREATE INDEX IF NOT EXISTS ix_t_incident_report_review ON ihms_nursing.t_incident_report (review_status, occurred_at);
CREATE INDEX IF NOT EXISTS ix_t_shift_handover_ward_shift ON ihms_nursing.t_shift_handover (ward_id, shift, added_datetime);
COMMENT ON INDEX ix_t_shift_handover_ward_shift IS 'SBAR lookup by ward+shift';
