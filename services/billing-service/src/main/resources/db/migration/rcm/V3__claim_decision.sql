-- ============================================================================
-- V3 — claim decision statuses: expand t_nhcx_request.request_status to
-- include 4=APPROVED and 5=REJECTED for claim approve/reject flow.
-- Previous: CHECK (request_status BETWEEN 0 AND 3), now 0..5.
-- ============================================================================
SET search_path TO ihms_rcm, public;

ALTER TABLE ihms_rcm.t_nhcx_request DROP CONSTRAINT IF EXISTS ck_t_nhcx_request_status;
ALTER TABLE ihms_rcm.t_nhcx_request ADD CONSTRAINT ck_t_nhcx_request_status CHECK (request_status BETWEEN 0 AND 5);
