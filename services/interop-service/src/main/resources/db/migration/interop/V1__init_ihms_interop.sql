-- ============================================================================
-- V1 — ihms_interop schema (Interop & Notifications service, Catalog #25)
--       WP-T1 (Wave-6 tranche 6, GAP-REGISTER-6)
-- Clean target of the interop slice: HIS outbound/inbound message queues,
-- ABDM request queue, WhatsApp message queue (+status history), EHR job
-- details, plus the transactional outbox TABLE per schema convention.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the interop service.
--
-- EVENT POLICY (register WP-T1): the interop service is a CONSUMER/RELAY,
-- not a producer — it EMITS NOTHING. Nothing is ever written to the outbox;
-- the integration test asserts it stays empty through EVERY flow (negative
-- outbox assertion). The internal event-relay ingress API
-- (POST /api/v1/internal/events) models the FUTURE KAFKA CONSUMER that will
-- feed these queues; production cutover replaces HTTP ingress with a Kafka
-- listener - in-flight continuity across the cutover is a deployment concern,
-- NOT modeled here (comment-only per register).
--
-- EXTERNAL GATEWAYS = QUEUE ROWS + STATUS MACHINES ONLY (register scope bet):
--   - HL7 MLLP transport (outbound dispatch + inbound parse)  : comment-only
--   - ABDM gateway calls (HIP/HIU/HFR/CM, eSanchar)           : comment-only
--   - WhatsApp provider send / webhook receiver               : comment-only
-- The queue rows and their status machines ARE the deliverable.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_interop;

SET search_path TO ihms_interop, public;

-- ---------------------------------------------------------------------------
-- t_outbound_his — outbound HIS messages destined for external systems
-- (HL7 / EHR / PRINT). Fed by the internal event-relay ingress; a future
-- dispatcher (comment-only, OUT OF SCOPE) would drain PENDING rows over MLLP.
-- Idempotency: UNIQUE(event_type, aggregate_id) — duplicate ingress of the
-- same event returns the existing row (pinned by tests). Ingress requires a
-- non-null aggregateId so the uniqueness key is always populated.
-- Status machine SMALLINT: 0=PENDING 1=SENT 2=FAILED 3=DEAD
-- (0->1 success, retries 0->2->0.., terminal 3=DEAD after max attempts —
-- retry/dead-lettering belongs to the OUT-OF-SCOPE dispatcher; only the
-- states are modeled here).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_outbound_his (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    event_type VARCHAR(60) NOT NULL,
    aggregate_id VARCHAR(40),
    payload JSONB NOT NULL,
    target_system VARCHAR(30) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    CONSTRAINT pk_t_outbound_his PRIMARY KEY (id),
    CONSTRAINT uq_t_outbound_event UNIQUE (event_type, aggregate_id),
    CONSTRAINT ck_t_outbound_target CHECK (target_system IN ('HL7', 'EHR', 'PRINT')),
    CONSTRAINT ck_t_outbound_status CHECK (status IN (0, 1, 2, 3))
);
CREATE INDEX IF NOT EXISTS ix_t_outbound_status ON ihms_interop.t_outbound_his (status);
CREATE INDEX IF NOT EXISTS ix_t_outbound_target ON ihms_interop.t_outbound_his (target_system);
COMMENT ON TABLE ihms_interop.t_outbound_his IS 'Outbound HIS queue (HL7/EHR/PRINT targets). Fed by internal event-relay ingress; idempotent per UNIQUE(event_type, aggregate_id). Dispatcher draining PENDING rows over HL7 MLLP is OUT OF SCOPE (comment-only); status machine 0=PENDING 1=SENT 2=FAILED 3=DEAD modeled only.';

-- ---------------------------------------------------------------------------
-- t_inbound_his — raw inbound HL7 v2 messages received from external systems.
-- Ingress validates messageType against the allowed ADT/ORM set (service-side,
-- BEXA003 otherwise). Parsing/routing into clinical services and marking
-- PROCESSED/REJECTED belongs to the OUT-OF-SCOPE inbound dispatcher
-- (comment-only) — rows land as 0=RECEIVED with processed_at NULL.
-- Status machine SMALLINT: 0=RECEIVED 1=PROCESSED 2=REJECTED.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_inbound_his (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    message_type VARCHAR(20) NOT NULL,
    sender VARCHAR(40),
    raw_payload TEXT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    processed_at TIMESTAMP(3),
    CONSTRAINT pk_t_inbound_his PRIMARY KEY (id),
    CONSTRAINT ck_t_inbound_status CHECK (status IN (0, 1, 2))
);
CREATE INDEX IF NOT EXISTS ix_t_inbound_status ON ihms_interop.t_inbound_his (status);
COMMENT ON TABLE ihms_interop.t_inbound_his IS 'Inbound HL7 v2 queue (ADT^A01/ADT^A45/ORM etc). Rows land 0=RECEIVED; parse/dispatch to PROCESSED/REJECTED is the OUT-OF-SCOPE inbound dispatcher (comment-only).';

-- ---------------------------------------------------------------------------
-- t_abdm_request_queue — outbound ABDM gateway requests (HIP/HIU/HFR/CM kinds)
-- correlated by correlation_id (UNIQUE — the legacy Redis correlation store
-- ABDM_REQ_<reqId> becomes this durable row, 07 §ABDM). Gateway call itself is
-- OUT OF SCOPE (comment-only); callbacks arrive on
-- PUT /api/v1/abdm/requests/{correlationId}/callback and are IDEMPOTENT:
-- first callback wins, repeats are no-ops returning the stored state.
-- Status SMALLINT 0..3 per 06 §A9 NHCX request-status enums (0=ACCEPTED ..
-- 3=FAILED); intermediate names below are service labels for the documented
-- numeric range — endpoints 0 and 3 are catalog-evidenced (disclosed in report).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_abdm_request_queue (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    request_kind VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    payload JSONB,
    status SMALLINT NOT NULL DEFAULT 0,
    callback_payload JSONB,
    CONSTRAINT pk_t_abdm_request_queue PRIMARY KEY (id),
    CONSTRAINT uq_t_abdm_correlation UNIQUE (correlation_id),
    CONSTRAINT ck_t_abdm_kind CHECK (request_kind IN ('HIP', 'HIU', 'HFR', 'CM')),
    CONSTRAINT ck_t_abdm_status CHECK (status IN (0, 1, 2, 3))
);
CREATE INDEX IF NOT EXISTS ix_t_abdm_status ON ihms_interop.t_abdm_request_queue (status);
COMMENT ON TABLE ihms_interop.t_abdm_request_queue IS 'ABDM gateway request queue; correlation_id UNIQUE (409 on duplicate at API + DB). Callbacks idempotent (first wins). Gateway dispatch itself OUT OF SCOPE (comment-only). Status 0..3 per 06 §A9 NHCX enums.';

-- ---------------------------------------------------------------------------
-- t_whatsapp_message_queue — outbound WhatsApp notifications (write-behind
-- queue replacing legacy T_WHATSAPPMESSAGEQUEUE drained every 15 min, 07 §).
-- Provider send job is OUT OF SCOPE (comment-only); the forward-only status
-- machine IS implemented on PUT /api/v1/whatsapp/messages/{id}/status:
--   0=QUEUED -> 1=SENT -> 2=DELIVERED -> 3=READ (strictly one step forward)
--   4=FAILED allowed ONLY from 0 or 1 (provider rejected before delivery);
--   anything else (backward jump, skip, FAILED from DELIVERED/READ) -> 400.
-- Every accepted transition appends a row to t_whatsapp_message_status.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_whatsapp_message_queue (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    template VARCHAR(60),
    recipient VARCHAR(20) NOT NULL,
    payload_vars JSONB,
    status SMALLINT NOT NULL DEFAULT 0,
    provider_message_id VARCHAR(64),
    CONSTRAINT pk_t_whatsapp_message_queue PRIMARY KEY (id),
    CONSTRAINT ck_t_wa_status CHECK (status IN (0, 1, 2, 3, 4))
);
CREATE INDEX IF NOT EXISTS ix_t_wa_status ON ihms_interop.t_whatsapp_message_queue (status);
COMMENT ON TABLE ihms_interop.t_whatsapp_message_queue IS 'WhatsApp notification queue. Forward-only machine 0 QUEUED -> 1 SENT -> 2 DELIVERED -> 3 READ; 4 FAILED only from 0/1; illegal transitions 400 at API (pinned by tests). Provider send/receive jobs OUT OF SCOPE (comment-only).';

-- ---------------------------------------------------------------------------
-- t_whatsapp_message_status — provider webhook history for one message
-- (replaces legacy T_WHATSAPPMESSAGESTATUS). One row per accepted transition.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_whatsapp_message_status (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    message_id BIGINT NOT NULL REFERENCES ihms_interop.t_whatsapp_message_queue (id),
    status_code SMALLINT NOT NULL,
    ts TIMESTAMP(3) NOT NULL,
    detail VARCHAR(200),
    CONSTRAINT pk_t_whatsapp_message_status PRIMARY KEY (id),
    CONSTRAINT ck_t_wa_hist_code CHECK (status_code IN (0, 1, 2, 3, 4))
);
CREATE INDEX IF NOT EXISTS ix_t_wa_hist_message ON ihms_interop.t_whatsapp_message_status (message_id);
COMMENT ON TABLE ihms_interop.t_whatsapp_message_status IS 'Status history: one row appended per accepted WhatsApp transition (webhook emulation via PUT .../status).';

-- ---------------------------------------------------------------------------
-- t_ehr_job_details — EHR export/sync job read-model. PHASE-1 NOTE: no write
-- path exists yet — job creation rides the OUT-OF-SCOPE dispatcher slice
-- (comment-only, disclosed in WP report); GET /api/v1/health/jobs?status=
-- exposes the read model so ops can observe jobs once dispatchers land.
-- Status SMALLINT 0..3: 0=QUEUED 1=RUNNING 2=DONE 3=FAILED.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.t_ehr_job_details (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    job_kind VARCHAR(30),
    patient_id BIGINT,
    status SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_ehr_job_details PRIMARY KEY (id),
    CONSTRAINT ck_t_ehr_job_status CHECK (status IN (0, 1, 2, 3))
);
CREATE INDEX IF NOT EXISTS ix_t_ehr_job_status ON ihms_interop.t_ehr_job_details (status);
COMMENT ON TABLE ihms_interop.t_ehr_job_details IS 'EHR job read-model (0=QUEUED 1=RUNNING 2=DONE 3=FAILED). No phase-1 write path — creation rides the OUT-OF-SCOPE dispatcher slice (comment-only, disclosed).';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope DDL mirrors sibling services incl.
-- event_id UNIQUE). PHASE-1 EVENT POLICY: interop EMITS NOTHING — it is a
-- consumer/relay, not a producer; NO emitter exists; this table stays EMPTY
-- (asserted by tests).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_interop.outbox (
    id               BIGSERIAL    PRIMARY KEY,
    event_id         UUID         NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    version          VARCHAR(16)  NOT NULL DEFAULT 'v1',
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     BIGINT       NOT NULL,
    partition_key    VARCHAR(100) NOT NULL,
    payload          JSONB        NOT NULL,
    idempotency_key  VARCHAR(255),
    producer_service VARCHAR(100) NOT NULL,
    occurred_at      TIMESTAMP(3) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts         INTEGER      NOT NULL DEFAULT 0,
    published_at     TIMESTAMP(3),
    created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id)
);
COMMENT ON TABLE ihms_interop.outbox IS 'Transactional outbox TABLE per schema convention — ZERO emitters: the interop service emits NOTHING (register WP-T1); integration test asserts it stays empty through every flow.';
CREATE INDEX idx_outbox_dispatch ON ihms_interop.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_outbound_his_seq             START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_inbound_his_seq              START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_abdm_request_queue_seq       START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_whatsapp_message_queue_seq   START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_whatsapp_message_status_seq  START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_ehr_job_details_seq          START WITH 1;
