-- ============================================================================
-- V4 — outbox event_id uniqueness (AGT-10 F-9 / IMP-DEC-024)
-- The transactional outbox is the consumer dedupe boundary: consumers dedupe by
-- eventId, so it must be unique. (idempotency_key remains informational; a
-- deterministic per-request key arrives with the gateway request-id contract.)
-- ============================================================================

SET search_path TO ihms_master, public;

ALTER TABLE outbox ADD CONSTRAINT uq_outbox_event_id UNIQUE (event_id);
COMMENT ON CONSTRAINT uq_outbox_event_id ON outbox IS 'Consumers dedupe by eventId (KAFKA-EVENT-CATALOG). Added V4 per AGT-10 F-9.';
