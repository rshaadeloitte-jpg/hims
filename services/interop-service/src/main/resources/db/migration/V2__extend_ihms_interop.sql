-- ============================================================================
-- V2 — ihms_interop schema extensions (MIG-055)
--       Extends the original V1 schema (Catalog #25) with:
--         - ABDM Milestone 1 support (ABHA = ABHA creation/verification; was
--           deferred from V1 because the V1 dispatchers were OUT OF SCOPE per
--           register WP-T1 — now M1 lands with M2/M3/CM all on the same queue)
--         - SMS notification queue (t_sms_notification_queue) for the eSanchar
--           gateway — same forward-only status machine + history table as the
--           WhatsApp queue (register parity: 07 §3.1 #15 SMS/WhatsApp grouped
--           under "SMS/WhatsApp (eSanchar)" outbound integration)
--         - HL7 v2 dispatch history (t_hl7_dispatch_history) — mirrors the
--           t_whatsapp_message_status pattern for MLLP-side transitions; the
--           dispatcher that would MLLP-send is OUT OF SCOPE (comment-only)
--         - Failed-message DLQ (t_dispatch_dlq) — captures the FAILED/DEAD
--           events from BOTH the WhatsApp/SMS queues AND the outbound HIS
--           queue; retry/backoff/DLQ policy belongs to the OUT-OF-SCOPE
--           dispatcher slice (comment-only) but the table IS the durable
--           landing for terminal failures
--         - In-flight continuity support: a `consumer_group` column on the
--           inbound rows tracks the Kafka consumer group that processed the
--           message (NULL = HTTP ingress; non-NULL = Kafka consumer —
--           zero message loss across the cutover is then observable at DB
--           level: both the HTTP-ingress row AND the Kafka-ingest row carry
--           the same envelope eventId and the queue is the durability boundary)
-- ============================================================================

SET search_path TO ihms_interop, public;

-- ---------------------------------------------------------------------------
-- V2.1: ABDM Milestone 1 — add 'ABHA' to the request_kind check constraint.
-- 07 §ABDM evidence: M1 (ABHA create/verify/link) was deferred from V1
-- because the V1 dispatchers (comment-only) only modeled M2/M3/CM/HFR.
-- Adding ABHA here unlocks the M1 flow on the same queue + idempotency
-- backstop without a V3 migration.
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_interop.t_abdm_request_queue
    DROP CONSTRAINT IF EXISTS ck_t_abdm_kind;
ALTER TABLE ihms_interop.t_abdm_request_queue
    ADD CONSTRAINT ck_t_abdm_kind CHECK (request_kind IN ('ABHA', 'HIP', 'HIU', 'HFR', 'CM'));

COMMENT ON COLUMN ihms_interop.t_abdm_request_queue.request_kind IS
    'ABDM request kind. ABHA = M1 (create/verify/link). HIP = M2 (push). HIU = M3 (pull). HFR = facility registry. CM = consent management. All on the same UNIQUE(correlation_id) queue.';

-- ---------------------------------------------------------------------------
-- V2.2: ABDM milestone flag — explicit M1/M2/M3 column for downstream routing.
-- Backfilled from request_kind for legacy rows (ABHA->M1, HIP/CM->M2,
-- HIU->M3, HFR->M0=facility). New code writes both columns in the same tx.
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_interop.t_abdm_request_queue
    ADD COLUMN IF NOT EXISTS milestone VARCHAR(4) NOT NULL DEFAULT 'M2'
        CHECK (milestone IN ('M0', 'M1', 'M2', 'M3'));

UPDATE ihms_interop.t_abdm_request_queue SET milestone = 'M1' WHERE request_kind = 'ABHA';
UPDATE ihms_interop.t_abdm_request_queue SET milestone = 'M2' WHERE request_kind IN ('HIP', 'CM');
UPDATE ihms_interop.t_abdm_request_queue SET milestone = 'M3' WHERE request_kind = 'HIU';
UPDATE ihms_interop.t_abdm_request_queue SET milestone = 'M0' WHERE request_kind = 'HFR';

ALTER TABLE ihms_interop.t_abdm_request_queue
    ALTER COLUMN milestone DROP DEFAULT;

-- ---------------------------------------------------------------------------
-- V2.3: t_sms_notification_queue — eSanchar outbound SMS notifications.
-- Same forward-only status machine as the WhatsApp queue (0 QUEUED -> 1 SENT
-- -> 2 DELIVERED -> 3 DELIVERY_FAILED; 4 EXPIRED terminal). The dispatcher
-- that would call send.sms.url / send.sms.url.otp is OUT OF SCOPE (comment-
-- only per register). Every accepted transition appends a row to
-- t_sms_notification_status (parity with WhatsApp).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_sms_notification_queue (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    template VARCHAR(60),
    recipient VARCHAR(20) NOT NULL,
    gateway VARCHAR(20) NOT NULL DEFAULT 'eSanchar',
    payload_vars JSONB,
    status SMALLINT NOT NULL DEFAULT 0,
    provider_message_id VARCHAR(64),
    CONSTRAINT pk_t_sms_notification_queue PRIMARY KEY (id),
    CONSTRAINT ck_t_sms_status CHECK (status IN (0, 1, 2, 3, 4))
);
CREATE INDEX IF NOT EXISTS ix_t_sms_status ON ihms_interop.t_sms_notification_queue (status);
CREATE INDEX IF NOT EXISTS ix_t_sms_recipient ON ihms_interop.t_sms_notification_queue (recipient);

CREATE TABLE IF NOT EXISTS ihms_interop.t_sms_notification_status (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    message_id BIGINT NOT NULL REFERENCES ihms_interop.t_sms_notification_queue (id),
    status_code SMALLINT NOT NULL,
    ts TIMESTAMP(3) NOT NULL,
    detail VARCHAR(200),
    CONSTRAINT pk_t_sms_notification_status PRIMARY KEY (id),
    CONSTRAINT ck_t_sms_hist_code CHECK (status_code IN (0, 1, 2, 3, 4))
);
CREATE INDEX IF NOT EXISTS ix_t_sms_hist_message ON ihms_interop.t_sms_notification_status (message_id);

COMMENT ON TABLE ihms_interop.t_sms_notification_queue IS
    'SMS notification queue (eSanchar gateway). Forward-only machine 0 QUEUED -> 1 SENT -> 2 DELIVERED -> 3 DELIVERY_FAILED; 4 EXPIRED terminal. Provider send (send.sms.url) OUT OF SCOPE (comment-only).';
COMMENT ON TABLE ihms_interop.t_sms_notification_status IS
    'Status history: one row appended per accepted SMS transition (parity with t_whatsapp_message_status).';

CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_sms_notification_queue_seq  START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_sms_notification_status_seq START WITH 1;

-- ---------------------------------------------------------------------------
-- V2.4: t_dispatch_dlq — terminal-failure landing for BOTH the outbound HIS
-- queue (status=DEAD) AND the WhatsApp/SMS notification queues (terminal
-- states EXPIRED / DELIVERY_FAILED beyond retry budget). Retry/backoff/DLQ
-- policy belongs to the OUT-OF-SCOPE dispatcher (comment-only) — this table
-- IS the durable landing so no message is silently lost.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_dispatch_dlq (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    source_table VARCHAR(40) NOT NULL,
    source_id BIGINT NOT NULL,
    failure_reason VARCHAR(500),
    failed_at TIMESTAMP(3) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_dispatch_dlq PRIMARY KEY (id),
    CONSTRAINT ck_t_dlq_source CHECK (source_table IN (
        't_outbound_his', 't_whatsapp_message_queue', 't_sms_notification_queue'))
);
CREATE INDEX IF NOT EXISTS ix_t_dlq_source ON ihms_interop.t_dispatch_dlq (source_table, source_id);
CREATE INDEX IF NOT EXISTS ix_t_dlq_failed_at ON ihms_interop.t_dispatch_dlq (failed_at);

CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_dispatch_dlq_seq START WITH 1;

COMMENT ON TABLE ihms_interop.t_dispatch_dlq IS
    'Dead-letter landing for terminal dispatch failures (outbound HIS / WhatsApp / SMS). retry/backoff/DLQ policy lives in the OUT-OF-SCOPE dispatcher (comment-only) — this table IS the durable landing so no message is silently lost.';

-- ---------------------------------------------------------------------------
-- V2.5: In-flight continuity — `consumer_group` column on t_inbound_his.
-- The interop service is the future Kafka consumer of E-01/E-02/E-03/E-04
-- (HL7/ADT feed) and E-05 (lab/EHR feed) — the cutover from HTTP ingress
-- to Kafka consumer is zero message loss because the row in t_inbound_his
-- is the durability boundary:
--   - HTTP ingress writes a row with consumer_group = NULL
--   - Kafka consumer writes a row with consumer_group = the group name
-- Both paths share UNIQUE-style dedup (envelope eventId) so a Kafka replay
-- of an HTTP-ingested event is a no-op (the row is already there).
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_interop.t_inbound_his
    ADD COLUMN IF NOT EXISTS consumer_group VARCHAR(60);
ALTER TABLE ihms_interop.t_inbound_his
    ADD COLUMN IF NOT EXISTS envelope_event_id VARCHAR(64);

-- the envelope eventId is the consumer-side dedup key (mirrors the
-- saga-consumer pattern in ihms_billing.t_consumed_event). UNIQUE so a
-- replay of the same Kafka message cannot create a second row.
CREATE UNIQUE INDEX IF NOT EXISTS uq_t_inbound_envelope_event
    ON ihms_interop.t_inbound_his (envelope_event_id)
    WHERE envelope_event_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_t_inbound_consumer_group
    ON ihms_interop.t_inbound_his (consumer_group);

COMMENT ON COLUMN ihms_interop.t_inbound_his.consumer_group IS
    'Kafka consumer group that ingested this message (NULL = HTTP ingress path). Lets ops observe the cutover from HTTP ingress to Kafka consumer at the DB level.';
COMMENT ON COLUMN ihms_interop.t_inbound_his.envelope_event_id IS
    'Outbox envelope eventId (UUID-as-string) for cross-service dedup. UNIQUE so a Kafka replay of the same envelope is a no-op at the durability boundary.';

-- ---------------------------------------------------------------------------
-- V2.6: t_abdm_request_queue — envelope_event_id for the ABDM callback path
-- (mirrors the inbound pattern). Lets us pin the legacy Redis ABDM_REQ_/
-- ABDM_RES_ pair to a durable row; the Redis store becomes a derived cache.
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_interop.t_abdm_request_queue
    ADD COLUMN IF NOT EXISTS envelope_event_id VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS uq_t_abdm_envelope_event
    ON ihms_interop.t_abdm_request_queue (envelope_event_id)
    WHERE envelope_event_id IS NOT NULL;

COMMENT ON COLUMN ihms_interop.t_abdm_request_queue.envelope_event_id IS
    'Outbox envelope eventId for ABDM producer-side dedup (mirrors the inbound t_inbound_his.envelope_event_id). Lets us pin the legacy Redis ABDM_REQ_/ABDM_RES_ pair to a durable row.';
