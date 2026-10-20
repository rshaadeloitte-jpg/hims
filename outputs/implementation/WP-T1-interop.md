# WP-T1 — Interop & Notifications service (Catalog #25) — IMPLEMENTATION REPORT

_Wave-6 tranche 6 (GAP-REGISTER-6 WP-T1). Reference conventions: ordering-service / cssd-service. Generated 2026-08-23._

## Deliverable

`services/interop-service` — package `com.hims.interop`, port **8105**, schema **ihms_interop**, Flyway `V1__init_ihms_interop.sql`. Spring Boot 3.5.3 / Java 21, identical pom profile to siblings (web, data-jpa, security, actuator, flyway-core+postgresql, springdoc; NO spring-kafka), surefire `-Dapi.version=1.44` pin.

## Schema (V1__init_ihms_interop.sql)

| Table | Notes |
|---|---|
| `t_outbound_his` | event_type(60,N), aggregate_id(40), payload JSONB(N), target_system CHECK IN (HL7,EHR,PRINT), status 0=PENDING/1=SENT/2=FAILED/3=DEAD, attempts, last_error(500), audit cols. **`uq_t_outbound_event UNIQUE(event_type, aggregate_id)`** = ingress idempotency at DB level |
| `t_inbound_his` | message_type(20,N), sender(40), raw_payload TEXT(N), status 0=RECEIVED/1=PROCESSED/2=REJECTED, processed_at NULL |
| `t_abdm_request_queue` | request_kind CHECK IN (HIP,HIU,HFR,CM), **correlation_id(64) UNIQUE NOT NULL**, payload JSONB, status 0..3 per NHCX enums (06 §A9), callback_payload JSONB NULL |
| `t_whatsapp_message_queue` | template(60), recipient(20,N), payload_vars JSONB, status 0=QUEUED..3=READ +4=FAILED, provider_message_id(64) NULL |
| `t_whatsapp_message_status` | message_id FK, status_code, ts, detail(200) — one row per accepted transition |
| `t_ehr_job_details` | job_kind(30), patient_id BIGINT, status 0..3 |
| `outbox` | full sibling envelope DDL incl. `uq_outbox_event_id`; **ZERO emitters — asserted EMPTY by tests** |

Sequences: `t_outbound_his_seq`, `t_inbound_his_seq`, `t_abdm_request_queue_seq`, `t_whatsapp_message_queue_seq`, `t_whatsapp_message_status_seq`, `t_ehr_job_details_seq` (IMP-DEC-003 manual assignment).

## APIs (openapi/interop-service.yaml verified 1:1 programmatically)

| Endpoint | Behavior |
|---|---|
| POST `/api/v1/internal/events` | Internal event-relay ingress (models the future Kafka consumer). Routing: InvoiceGenerated/PaymentReceived → t_outbound_his(**HL7**); LabResultFinalized → **EHR**; PatientRegistered/EncounterCreated/PatientAdmitted/PatientDischarged → **HL7**; unknown → 400 `INTEROP_UNKNOWN_EVENT_TYPE`. Idempotent per (eventType, aggregateId): first call **201** new row, duplicate **200** existing row (incl. concurrent-insert race translated via the DB unique constraint). Tagged internal |
| GET `/api/v1/outbound?status=&target=` | Read model; status 0..3 else BEXA003; target case-insensitive |
| GET `/api/v1/inbound` | All inbound rows, newest first |
| POST `/api/v1/inbound/hl7` | {messageType, sender, rawPayload}; allowed ADT/ORM set else 400 `INTEROP_INVALID_MESSAGE_TYPE`; lands 0=RECEIVED, processed_at NULL |
| POST `/api/v1/abdm/requests` | {requestKind∈HIP/HIU/HFR/CM, correlationId≤64, payload}; dup correlationId → **409 INTEROP_DUPLICATE_CORRELATION_ID** (API check + DB constraint race both pinned); starts status 0=ACCEPTED |
| PUT `/api/v1/abdm/requests/{correlationId}/callback` | Unknown → 404; status must be NHCX 0..3 else 400; **idempotent — first callback wins**, repeats are no-ops returning stored state even with different status/payload |
| POST `/api/v1/whatsapp/messages` | recipient required ≤20 chars; new rows QUEUED |
| PUT `/api/v1/whatsapp/messages/{id}/status` | Forward-only machine: 0→1→2→3 strictly one step; FAILED(4) only from 0/1; everything else 400 `INTEROP_ILLEGAL_TRANSITION`; statusCode 0/null/out-of-range → BEXA003; unknown id 404; every accepted transition appends a history row; providerMessageId stamped when supplied |
| GET `/api/v1/whatsapp/messages[?status=]` · GET `/api/v1/whatsapp/messages/{id}` | List w/ filter; single message + history |
| GET `/api/v1/health/jobs?status=` | t_ehr_job_details read model |

Security: default-deny (`!dev` chain, only actuator-health/api-docs/swagger/error permitted); dev profile HTTP Basic (IMP-DEC-019). Error envelope `{status, errorCode, message}` with BEXA codes throughout.

## Tests — `mvn -f services/interop-service/pom.xml test` → **exit 0, 56 tests, 0 failures, 0 skipped**

| Suite | Count | Covers |
|---|---|---|
| InteropEventServiceTest (L0) | 10 | routing map (all 7 catalog events), unknown type, guards, idempotency (duplicate no-write, same-aggregate-different-event is new row, DataIntegrityViolation race translation), PENDING defaults |
| AbdmServiceTest (L0) | 9 | all 4 kinds, kind/correlation guards, duplicate 409 (+constraint race), callback 404, NHCX range guard, first-callback-wins idempotency |
| WhatsappServiceTest (L0) | 10 | strict one-step forward path, skips/backwards illegal, FAILED from 0/1 legal & from DELIVERED/READ illegal, terminal READ closed, history rows per transition, providerMessageId stamping, recipient/template guards, 404 |
| InboundServiceTest (L0) | 4 | allowed set accepted landing RECEIVED, unsupported types rejected, rawPayload/sender guards |
| InteropControllerTest (@WebMvcTest, SecurityConfig) | 18 | default-deny on ALL endpoints, 201/200 ingest distinction, error envelopes for every rule code, 409 conflict envelope, 404 envelopes, filters, unknown route NOT_FOUND envelope |
| InteropServiceIntegrationTest (PG16 Testcontainers, Docker present) | 5 | Flyway applies all 7 tables + 6 sequences; ingress routing/idempotency against real DB; `uq_t_outbound_event` + `uq_t_abdm_correlation` pinned with raw SQL probes in isolated txs; ABDM callback idempotency on real persistence; WhatsApp machine + history through DB; inbound lands RECEIVED; **negative outbox assertions interleaved and final in every flow** |

No tests deleted or disabled (`@Testcontainers(disabledWithoutDocker = true)` mirrors cssd-service convention).

## Stub disclosures (external boundaries contract-stubbed BY DESIGN — nothing silently invented)

1. **HL7 MLLP transport (outbound dispatch + inbound listener)** — OUT OF SCOPE. Queue rows + status machines are the deliverable; no dispatcher drains `t_outbound_his` PENDING rows and no MLLP server feeds `t_inbound_his` (rows land RECEIVED, processed_at NULL). Retry/dead-letter transitions of the outbound machine are modeled as states only.
2. **ABDM gateway calls (HIP/HIU/HFR/CM) + eSanchar** — OUT OF SCOPE. No JWE/JWS, session tokens (ABDM_SES_TKN) or gateway HTTP calls; the correlation-id queue row IS the durable replacement for legacy Redis `ABDM_REQ_/ABDM_RES_`.
3. **WhatsApp provider send / webhook receiver jobs** — OUT OF SCOPE (legacy 15-min drain window job). `PUT .../status` emulates webhook delivery.
4. **EHR job creation** — `t_ehr_job_details` has NO phase-1 write path (job creation rides the dispatcher slice); `GET /api/v1/health/jobs` reads an empty table until dispatchers land.
5. **In-flight queue continuity across the Kafka cutover** — comment-only (register: cutover concern).
6. **NHCX intermediate status names** — register pins "0..3 per 06 §A9"; catalog evidences endpoints 0=ACCEPTED and 3=FAILED only. Intermediate names 1=IN_PROGRESS/2=COMPLETED are service labels for the documented numeric range (flagged here and in migration comments).
7. **Inbound HL7 allowed-type inventory** — exact legacy type list unverified against Oracle; implemented set = {ADT^A01/A02/A03/A04/A08/A45, ORM^O01, ORU^R01} covering register examples ("ADT^A01/ADT^A45/ORM etc") plus standard ADT/order/result types. Single constant, trivially extendable at cutover.
8. **Event-routing set** = KAFKA-EVENT-CATALOG E-01/E-02/E-03/E-04/E-05/E-07/E-08 identifiers exactly; other sibling events would need catalog additions before ingress accepts them.

## Comment-only items (per register OUT-OF-SCOPE markers)

Dispatchers listed above; plus documented comment blocks in `InteropServiceApplication`, `V1__init_ihms_interop.sql`, `OutboxEvent`/`OutboxRepository`, each application service, and each controller @Tag description. Nothing outside those comments was implemented.

## Event policy

The service **EMITS NOTHING** (consumer/relay, not a producer): no emitter code exists, no outbox dependency in any write path, and L2 asserts `outbox.count() == 0` at boot and after EVERY flow (ingress, ABDM create/callback, WhatsApp machine, inbound receive).

## Files

```
services/interop-service/pom.xml
services/interop-service/src/main/resources/application.yml | application-dev.yml
services/interop-service/src/main/resources/db/migration/V1__init_ihms_interop.sql
services/interop-service/src/main/java/com/hims/interop/            (InteropServiceApplication)
  api/        ApiError, GlobalExceptionHandler, InternalEventController, OutboundController,
              InboundController, AbdmController, WhatsappController, HealthJobController
  application/InteropEventService, InboundService, AbdmService, WhatsappService,
              EhrJobQueryService + exceptions/{InteropRuleException, InvalidRequestException,
              InteropNotFoundException, ConflictException}
  domain/model/{InteropBase, OutboundHis, InboundHis, AbdmRequestQueue,
              WhatsappMessageQueue, WhatsappMessageStatus, EhrJobDetails}
  domain/repository/ (7 repositories)
  infrastructure/outbox/{OutboxEvent, OutboxRepository}
  infrastructure/web/CorrelationIdFilter
  configuration/{SecurityConfig, DevSecurityConfig}
services/interop-service/src/test/java/com/hims/interop/
  api/InteropControllerTest
  application/{InteropEventServiceTest, InboundServiceTest, AbdmServiceTest, WhatsappServiceTest}
  infrastructure/persistence/InteropServiceIntegrationTest
openapi/interop-service.yaml          (paths verified 1:1 vs controllers programmatically)
outputs/implementation/WP-T1-interop.md
```
