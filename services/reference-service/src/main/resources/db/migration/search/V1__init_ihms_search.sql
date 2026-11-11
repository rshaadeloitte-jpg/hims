-- ============================================================================
-- V1 — ihms_search schema (Search & Index service, Catalog #27)
--       WP-T3 (Wave-5 tranche 6, GAP-REGISTER-6)
-- Clean target of the Solr indexing pipeline's DURABLE side: the
-- t_index_request journal. Solr document state itself lives in Solr (07 §1);
-- this schema owns only the request journal + the transactional outbox TABLE
-- per schema convention.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the search service.
--
-- EVENT POLICY (register WP-T3): this service EMITS NOTHING (it is a consumer
-- of sibling events via the internal event-routing ingress; the future Kafka
-- consumer is documented, not modeled). NOTHING is ever written to the outbox;
-- the integration test asserts it stays EMPTY through EVERY flow (negative
-- outbox assertion, cssd-service convention).
--
-- Journal status machine (SMALLINT):
--   0=PENDING -> dispatch -> 1=INDEXED | 2=FAILED
--   retry: 2=FAILED -> 0=PENDING (guard FAILED-only) -> dispatch again.
--   action SMALLINT 1=INDEX 2=DELETE (DELETE reserved for the encounter
--   retention/archive job — OUT OF SCOPE per 07 §1.7; no route creates it).
--
-- 'service' collection ORPHAN dropped per 07 §1.2 (no indexer, no reader,
-- absent from prod DR sync): the 5 live collections are
--   patient / encounter / item / icd10cm / healthfactor.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_search;

SET search_path TO ihms_search, public;

-- ---------------------------------------------------------------------------
-- t_index_request — index REQUEST journal (the durable part).
-- UNIQUE(collection, doc_id, action, status) is INTENTIONALLY NOT ENFORCED per
-- register WP-T3: dedupe is via the latest-row query
-- (findTopByCollectionAndDocIdAndActionOrderBy...); a FAILED row allows a new
-- PENDING row for the same key (retry semantics), so a hard UNIQUE constraint
-- would be wrong. Pinned by tests.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_search.t_index_request (
    id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    collection VARCHAR(30) NOT NULL,
    action SMALLINT NOT NULL DEFAULT 1,
    doc_id VARCHAR(60) NOT NULL,
    payload JSONB,
    status SMALLINT NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_index_request PRIMARY KEY (id),
    CONSTRAINT ck_t_index_request_collection CHECK (collection IN ('patient','encounter','item','icd10cm','healthfactor')),
    CONSTRAINT ck_t_index_request_action CHECK (action IN (1, 2)),
    CONSTRAINT ck_t_index_request_status CHECK (status IN (0, 1, 2))
);
CREATE INDEX IF NOT EXISTS ix_t_index_request_lookup ON ihms_search.t_index_request (collection, doc_id, action);
CREATE INDEX IF NOT EXISTS ix_t_index_request_status ON ihms_search.t_index_request (status);
COMMENT ON TABLE ihms_search.t_index_request IS 'Index REQUEST journal (durable side of the Solr pipeline; Solr state lives in Solr). 5 live collections per 07 §1.2 — the legacy service collection orphan is dropped (comment-only). Status 0=PENDING 1=INDEXED 2=FAILED; action 1=INDEX 2=DELETE (DELETE reserved for the encounter retention job, OUT OF SCOPE). UNIQUE(collection,doc_id,action,status) deliberately NOT enforced — dedupe via latest-row query (FAILED allows a fresh PENDING row).';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope DDL mirrors sibling services incl.
-- event_id UNIQUE). EVENT POLICY: ZERO emitters — search-service consumes
-- sibling events and emits NOTHING; this table stays EMPTY (asserted by tests).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_search.outbox (
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
COMMENT ON TABLE ihms_search.outbox IS 'Transactional outbox TABLE per schema convention — ZERO emitters: search-service is a pure consumer/relay (register WP-T3 EVENT POLICY); integration test asserts it stays empty through EVERY flow.';
CREATE INDEX idx_outbox_dispatch ON ihms_search.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_search.t_index_request_seq START WITH 1;
