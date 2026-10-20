# WP-T3 — Search & Index service (Catalog #27) — IMPLEMENTED

_Tranche 6 (GAP-REGISTER-6). Service: `services/search-service`, package `com.hims.search`,
port **8107**, schema **ihms_search**, Flyway `V1__init_ihms_search.sql`.
Reference conventions mirrored from ordering-service / cssd-service (error envelope +
BEXA codes, default-deny security, MockMvc style, PG16 Testcontainers, outbox DDL,
correlation filter, explicit sequences IMP-DEC-003)._

## What was built

### PG side — index REQUEST journal (the durable part)
`t_index_request` exactly per register: `id` PK seq (`t_index_request_seq`),
`collection VARCHAR(30)` CHECK IN ('patient','encounter','item','icd10cm','healthfactor')
(5 live collections; the legacy **'service' orphan is dropped** per 07 §1.2 — comment-only),
`action SMALLINT` 1=INDEX 2=DELETE, `doc_id VARCHAR(60) NOT NULL`, `payload JSONB`,
`status SMALLINT` 0=PENDING 1=INDEXED 2=FAILED, `attempts INT DEFAULT 0`, audit cols.
`UNIQUE(collection, doc_id, action, status)` deliberately **NOT enforced** — dedupe is via
the latest-row query (`findTopByCollectionAndDocIdAndActionOrderByIdDesc`); a FAILED row
allows a fresh PENDING attempt. Both behaviors pinned by tests.

The convention `outbox` table exists with sibling DDL but has **ZERO emitters**.

### Internal event routing ingress (models the future Kafka consumer)
`POST /api/v1/internal/events {eventType, payload}`:
| eventType | route |
|---|---|
| PatientRegistered | patient INDEX |
| EncounterCreated / PatientAdmitted / PatientDischarged | encounter INDEX |
| InventoryUpdated | item INDEX |
| MasterDataChanged | icd10cm \| healthfactor INDEX (by payload.collection; default healthfactor) |
| unknown | 400 `SEARCH_UNKNOWN_EVENT_TYPE` |

Guards (400 BEXA003): eventType required; payload object required; payload.docId required,
≤60 chars. Journal write (PENDING) + **synchronous dispatch** in one tx: success → 1=INDEXED;
backend failure → 2=FAILED, attempts++ (row persists, never lost).
Dedupe idempotency: duplicate ingress while latest row is PENDING/INDEXED returns the existing
row (HTTP 200 vs 201 for new) with zero writes.

### Indexer backend abstraction
Interface `SearchIndexer` (index/delete/search returning numFound+docs page), TWO implementations:
- `HttpSolrIndexer` — plain RestClient to `SOLR_BASE_URL` (root URL; `/solr/{collection}/update`
  and `/select` appended). Active when `app.search.backend=solr` (prod sets
  `SEARCH_BACKEND=solr`). Generic dynamic-field doc model (`<k>_s` strings + catch-all `text_t`,
  `id`=docId); synchronous commit on writes; edismax select with per-collection qf.
- `InMemoryIndexer` — deterministic fallback (default): case-insensitive substring match over
  values, deterministic docId ordering, rows/start paging.

### Search API
- `GET /api/v1/search?collection=&q=&rows=&start=` → `{collection,q,start,rows,numFound,docs[]}`;
  collection must be one of the 5 LIVE collections else 400 `SEARCH_UNKNOWN_COLLECTION`;
  q required (BEXA003); rows clamped 1..100 (default 10), start ≥ 0.
- `GET /api/v1/search/collections` → the 5 live collections.
- `GET /api/v1/index-requests?collection=&status=` → filtered journal rows, newest first.
- `PUT /api/v1/index-requests/{id}/retry` → guard **FAILED-only** (2→0 then immediate
  re-dispatch); non-FAILED → 400 `SEARCH_ILLEGAL_TRANSITION`; unknown id → 404 `NOT_FOUND`.

### EVENT POLICY
Service **EMITS NOTHING** (pure consumer/relay). No emitter exists anywhere in the codebase
and the integration tests assert the outbox stays EMPTY through EVERY flow (negative outbox
assertion at every stage of both L2 classes, cssd-service convention).

## Backend disclosure (which actually ran)
- **Testcontainers solr:9 round-trip RAN GREEN locally**: `SearchSolrIntegrationTest`
  boots a real `solr:9` container (`solr-precreate patient`) + PG16, wires
  `app.search.backend=solr` against the container URL, ingests `PatientRegistered`,
  and queries the document back FROM SOLR (numFound=1, dynamic fields mapped). Negative
  outbox asserted with the Solr backend active. Docker Desktop was available (solr:9 image
  pulled successfully), so BOTH backends are covered:
  - Solr round-trip (index → query returns doc): **ran against Testcontainers solr:9**
    in this environment — DISCLOSED as the executed path.
  - InMemoryIndexer semantics additionally pinned at L0/L2 (always runs, no Docker needed);
    if Docker is absent the solr test class auto-skips (`disabledWithoutDocker=true`)
    and the InMemory coverage remains the green fallback — per register "whichever the
    child can make green", both are green here.
- `HttpSolrIndexer` wire contract additionally pinned at L0 via MockRestServiceServer
  bound to RestClient.Builder (update JSON shape, commit=true, edismax qf params,
  response parsing, failure → IndexerException).

## OUT OF SCOPE / comment-only items (per register — nothing implemented silently)
1. **Redis current_pat_*/current_enc_* mirrors (100s TTL)** — OUT OF SCOPE (promote-or-drop
   decision open). Comment-only in `application.yml`; no Redis dependency exists.
2. **Exact legacy edismax qf boost weights** (mrno^18, mobile^16, civilid^16, janaadhar^17,
   name^8-12 per 07 §1.4/§1.7) — COMMENT-ONLY; modernized `_s` field names make 1:1 weight
   carry-over unverifiable until the managed schema lands. Config-encoded starting weights
   in `app.search.patient-qf` (tunable without redeploy).
3. **'service' collection orphan** — dropped per 07 §1.2; comment-only in migration + config.
4. **action DELETE routes** (encounter retention/archive job per 07 §1.7) — column + indexer
   delete() exist; NO event/API creates DELETE journal rows today (comment-only caller).
5. **Real Kafka consumer of sibling topics** — modeled as the internal ingress endpoint;
   production replacement documented at every site (same pattern as interop/reporting T1/T2).
6. **SolrCloud topology, managed schema, DR rsync replacement, circuit breaker/degrade-to-DB
   patient search fallback** (07 §1.7 modernization notes) — not in tranche scope.
7. **Encounter TTL/retention behavior** (legacy 3-day window) — out of scope.

## Test table (mvn -f services/search-service/pom.xml test → exit 0, 46 tests, 0 failures)

| Class | Level | Tests | Pins |
|---|---|---|---|
| SearchIngestServiceTest | L0 unit | 20 | routing table (all 6 events + MasterDataChanged default/by-collection/foreign), unknown-event 400, blank/missing guards, oversized docId, journal PENDING→dispatch→INDEXED (2 saves), backend-failure→FAILED+attempts++ row survives, dedupe latest-row (PENDING hit, INDEXED hit, FAILED allows fresh row), search guards (unknown/orphan collection, blank q, clamp), live-collections set, retry (404 unknown, SEARCH_ILLEGAL_TRANSITION from PENDING+INDEXED, FAILED→PENDING→re-dispatch→INDEXED, second failure attempts++) |
| HttpSolrIndexerTest | L0 wire | 6 | update JSON {add:[{doc:{id,<k>_s,text_t}}]} + commit=true, delete-by-id body, edismax select w/ configured patient qf boosts, default text_t boost for non-patient, server-error → IndexerException, null values skipped |
| InMemoryIndexerTest | L0 unit | 6 | round-trip, case-insensitive substring over any value, deterministic order + rows/start paging, upsert-by-docId + null drop, delete + unknown collection empty, blank guards |
| SearchApiControllerTest | MockMvc | 11 | default-deny 401 on ALL endpoints, ingest 201-new/200-dedupe, unknown-event 400 envelope, search envelope, unknown-collection 400, missing q BEXA003, collections list, journal listing, retry 200/404/400 envelopes |
| SearchServiceIntegrationTest | L2 PG16 Testcontainers | 2 | Flyway V1 applies (t_index_request + outbox + t_index_request_seq), full pipeline on real DB: guards pre-write, all five collection routes INDEXED, JSONB payload + audit columns persisted, persistence-level dedupe (single row for duplicate key), search round-trip via InMemoryIndexer, retry flow incl. illegal transition, listing filters — **NEGATIVE OUTBOX asserted at every stage incl. final** |
| SearchServiceIntegrationTest (Solr variant) = SearchSolrIntegrationTest | L2 PG16 + solr:9 Testcontainers | 1 | REAL Solr round-trip (ingest → INDEXED → query returns doc from Solr, mrno dynamic field mapping), second independent doc, negative outbox with Solr backend active |

Total: **46 tests, 46 passed, 0 failed, 0 skipped** (`Tests run: 46, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS).

## Files
```
services/search-service/pom.xml                                   (mirror of cssd/ordering pom; surefire api.version pin)
services/search-service/src/main/resources/application.yml        (port 8107, ihms_search, backend/qf config, Redis OUT-OF-SCOPE comment)
services/search-service/src/main/resources/application-dev.yml
services/search-service/src/main/resources/db/migration/V1__init_ihms_search.sql
.../search/SearchServiceApplication.java                          (no relay: emits NOTHING)
.../configuration/{SecurityConfig,DevSecurityConfig,SearchIndexerConfiguration}.java
.../infrastructure/web/CorrelationIdFilter.java
.../api/{ApiError,GlobalExceptionHandler,InternalEventController,SearchController,IndexRequestController}.java
.../application/SearchIngestService.java
.../application/exceptions/{SearchRuleException,InvalidSearchRequestException,SearchNotFoundException}.java
.../domain/model/{SearchBase,IndexRequest}.java
.../domain/repository/IndexRequestRepository.java
.../infrastructure/indexer/{SearchIndexer,InMemoryIndexer,HttpSolrIndexer}.java
.../infrastructure/outbox/{OutboxEvent,OutboxRepository}.java     (convention table; ZERO emitters)
src/test/java/com/hims/search/... (6 test classes listed above)
openapi/search-service.yaml                                       (paths verified 1:1 against controllers programmatically)
```

Acceptance: `mvn -f services/search-service/pom.xml test` → **exit 0**.
