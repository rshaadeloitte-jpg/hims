-- ============================================================================
-- V4 — FHIR R4 contract-only stub + e-Aushadhi drug catalogue sync queue.
--       WP-T1 extension (GAP-REGISTER-6): the interop service exposes a
--       FHIR R4 read surface (Patient / Observation) as CONTRACT-ONLY STUBS
--       — no HAPI FHIR server, no persistence; the endpoints return the
--       FHIR R4 JSON shape so consumers can integrate against the contract
--       while the production HAPI FHIR server (xcg/genserv/FhirServer.java in
--       the monolith — 07 §ABDM evidence) is wired in via the same Kafka
--       consumer that drives InteropEventService.
--
--       e-Aushadhi sync queue (t_eaushadhi_sync_queue): drug catalogue sync
--       from the national e-Aushadhi portal. Status machine mirrors the
--       WhatsApp/SMS forward-only pattern (register parity):
--         0=PENDING -> 1=SYNCED -> 2=FAILED
--       Every accepted transition appends a row to t_eaushadhi_sync_status.
--       The actual e-Aushadhi API call is OUT OF SCOPE (comment-only per
--       register); the queue row + status machine ARE the deliverable.
--
-- EVENT POLICY (register WP-T1): the interop service is a CONSUMER/RELAY,
-- not a producer — it EMITS NOTHING. Nothing is ever written to the outbox.
-- ============================================================================

SET search_path TO ihms_interop, public;

-- ---------------------------------------------------------------------------
-- t_eaushadhi_sync_queue — drug catalogue sync queue from e-Aushadhi portal.
-- Status machine SMALLINT: 0=PENDING 1=SYNCED 2=FAILED.
-- Forward-only: 0->1 (success) or 0->2 (failure); 1 and 2 are terminal.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_eaushadhi_sync_queue (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    drug_code VARCHAR(64) NOT NULL,
    drug_name VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(255),
    category VARCHAR(64),
    sync_status SMALLINT NOT NULL DEFAULT 0,
    sync_attempts INT NOT NULL DEFAULT 0,
    last_sync_at TIMESTAMP(3),
    last_error VARCHAR(500),
    CONSTRAINT pk_t_eaushadhi_sync_queue PRIMARY KEY (id),
    CONSTRAINT ck_t_eaushadhi_status CHECK (sync_status IN (0, 1, 2))
);
CREATE INDEX IF NOT EXISTS ix_t_eaushadhi_status ON ihms_interop.t_eaushadhi_sync_queue (sync_status);
CREATE INDEX IF NOT EXISTS ix_t_eaushadhi_drug_code ON ihms_interop.t_eaushadhi_sync_queue (drug_code);
COMMENT ON TABLE ihms_interop.t_eaushadhi_sync_queue IS 'e-Aushadhi drug catalogue sync queue. Forward-only machine 0=PENDING -> 1=SYNCED -> 2=FAILED. Actual e-Aushadhi API call OUT OF SCOPE (comment-only).';

-- ---------------------------------------------------------------------------
-- t_eaushadhi_sync_status — status history for one sync operation.
-- One row per accepted transition.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_eaushadhi_sync_status (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    sync_queue_id BIGINT NOT NULL REFERENCES ihms_interop.t_eaushadhi_sync_queue (id),
    status_code SMALLINT NOT NULL,
    ts TIMESTAMP(3) NOT NULL,
    detail VARCHAR(200),
    CONSTRAINT pk_t_eaushadhi_sync_status PRIMARY KEY (id),
    CONSTRAINT ck_t_eaushadhi_hist_code CHECK (status_code IN (0, 1, 2))
);
CREATE INDEX IF NOT EXISTS ix_t_eaushadhi_hist_queue ON ihms_interop.t_eaushadhi_sync_status (sync_queue_id);
COMMENT ON TABLE ihms_interop.t_eaushadhi_sync_status IS 'Status history: one row appended per accepted e-Aushadhi sync transition.';

-- ---------------------------------------------------------------------------
-- Sequences for new tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_eaushadhi_sync_queue_seq  START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_eaushadhi_sync_status_seq START WITH 1;
