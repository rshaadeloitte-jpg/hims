# WP-R1 — Radiology service (Catalog #8) — IMPLEMENTED

Tranche 5, GAP-REGISTER-5 WP-R1. NEW `services/radiology-service`, package `com.hims.radiology`,
server.port **8100**, schema **ihms_rad**, Flyway `V1__init_ihms_rad.sql`.
Reference conventions mirrored from `services/ordering-service` (outbox-era layout, error envelope,
security, tests) and `services/bloodbank-service` (no-event / no-outbox pattern).

## What was implemented

### Tables (Flyway V1, schema ihms_rad)
- `t_radorderbooking` — rad_order_id PK (explicit sequence `t_radorderbooking_seq`), patient_id NOT NULL,
  encounter_id NULL, admission_id NULL, modality VARCHAR(50) NOT NULL CHECK IN (XRAY, CT, MRI, US),
  priority SMALLINT NOT NULL DEFAULT 1 CHECK IN (1,2), status SMALLINT NOT NULL DEFAULT 1 CHECK IN (1..4),
  is_image_captured BOOLEAN NOT NULL DEFAULT FALSE, report_text TEXT, reported_by VARCHAR(100), audit cols.
  DB-level guards: `ck_t_radorderbooking_ctx` (NOT (encounter AND admission)) mirrors the API XOR rule;
  status/priority/modality CHECKs pin the enums at the schema level.
- `t_radorder_detail` — composite PK (rad_order_id FK, line_no), item_id BIGINT NOT NULL,
  qty NUMERIC(12,2) CHECK qty > 0, UNIQUE(rad_order_id, line_no); explicit sequence per convention.
- NO outbox table (RadOrderCreated DEFERRED — see events).

### APIs (all under `/api/v1/rad-orders`, spec-parity clean with openapi/radiology-service.yaml)
| Endpoint | Behavior |
|---|---|
| POST `` | Create booking {patientId, encounterId?, admissionId?, modality, priority?, lines[{itemId,qty}]}; guards below; 201 |
| GET `` | List ?patientId= & ?status= (1..4 or name SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED) & ?modality= |
| GET `/{id}` | Full order with lines; 404 envelope when unknown |
| POST `/{id}/start` | Guard: SCHEDULED(1) -> IN_PROGRESS(2) only; else 400 RAD_ILLEGAL_TRANSITION |
| POST `/{id}/capture-image` | Sets is_image_captured=true; 400 RAD_ILLEGAL_TRANSITION if already COMPLETED |
| POST `/{id}/report` | Body {reportText, reportedBy}; requires IN_PROGRESS -> COMPLETED(3), persists reportText/reportedBy; BEXA003 for blank payloads |
| POST `/{id}/cancel` | Guard: not COMPLETED -> CANCELLED(4); else 400 RAD_ILLEGAL_TRANSITION |

### Creation guards (all 400, BEXA003-style codes)
patientId required; modality required and ∈ {XRAY, CT, MRI, US} (case-insensitive input, stored upper);
priority (when supplied) ∈ {1=routine, 2=urgent}, defaults routine when null; >=1 line required;
encounterId XOR admissionId (at most one); every line needs itemId and qty > 0.

### Events policy
- **RadOrderCreated: DEFERRED** per KAFKA-EVENT-CATALOG §3 ("E deferred") — NOTHING is written to any
  outbox on booking or on any lifecycle transition. There is no outbox table in ihms_rad at all
  (bloodbank WP-9 pattern). The integration test asserts the absence explicitly (below).
- Post-saga OrderCreated (ordering->billing choreography): out of scope this tranche — comment-only in
  `RadOrderService` javadoc and the migration header. Never silently invented.

### Cross-cutting (sibling conventions)
Default-deny SecurityConfig (+ dev-only HTTP Basic profile), ApiError envelope {status, errorCode, message},
BEXA-style codes, GlobalExceptionHandler incl. BEXA003 param mapping + NOT_FOUND, CorrelationIdFilter
(X-Request-ID), springdoc, actuator probes, PG16 Testcontainers integration style, pom.xml mirroring
ordering/bloodbank (Spring Boot 3.5.3, Java 21, surefire `-Dapi.version=1.44`).

## Tests — 25 total, all green

| Test class | Count | Covers |
|---|---|---|
| `application.RadOrderServiceTest` (L0 unit) | 12 | Every create guard (patient/modality/priority/>=1 line/XOR/itemId/qty>0, default priority+routine, modality normalization), start guard SCHEDULED-only, capture guard COMPLETED-only + idempotent re-capture, report requires IN_PROGRESS + reportText/reportedBy required + completes, cancel guard COMPLETED-only (scheduled & in-progress cancellable), 404 code |
| `api.RadOrderControllerTest` (MockMvc, default-deny) | 12 | All 7 endpoints unauthorized without auth; create 201 with lines; unknown-modality / no-lines / both-contexts -> 400 BEXA003 envelope; start & capture & cancel illegal transition -> 400 RAD_ILLEGAL_TRANSITION; report 200 completed; get unknown -> 404 NOT_FOUND envelope; list named+numeric status filter and modality filter; unknown status name -> 400 |
| `infrastructure.persistence.RadiologyServiceIntegrationTest` (PG16 Testcontainers) | 1 | Flyway V1 applies on real postgres:16-alpine; explicit sequences live; full flow create(SCHEDULED)->start->capture->report(COMPLETED) persisted row check; second-order cancel path; list filters; guards rejected on real data; **negative outbox assertion**: information_schema shows ZERO outbox tables AND zero outbox-named relations in ihms_rad before/after booking/lifecycle/rejected creates (asserted 5× through the flow); DB-level ck_t_radorderbooking_ctx exercised on an isolated rolled-back connection |

**Result:** `mvn -f services/radiology-service/pom.xml test` → `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS (exit 0). No tests deleted or disabled to go green.

## Deviations (documented, minimal)
1. **No MBase boolean `status` audit column** on ihms_rad tables: the register names the lifecycle column
   literally `status SMALLINT`; a same-named MBase boolean audit column cannot coexist. Dropped the boolean
   from `RadOrderBase`/DDL (comment in code). All other audit cols (added_by/added_datetime/updated_by/
   updated_datetime/unit_id/synchronized/...) kept per convention.
2. **capture-image literal register semantics**: the register says "400 if already COMPLETED" only, so capture
   remains allowed while SCHEDULED/IN_PROGRESS (including re-capture) and is rejected once COMPLETED.
   Cancel-after-capture likewise stays possible until completion — pinned by tests exactly as specified.
3. **DB-level ctx-check test mechanics**: violating `ck_t_radorderbooking_ctx` inside the Spring test tx aborts
   the PG transaction; the check therefore runs on an isolated auto-rolled-back JDBC connection (commented in test).
4. Priority defaults to 1 (routine) when omitted — register defines the enum but not a mandated default;
   chosen to mirror legacy DEFAULT 1 and documented in spec + code.

## Deferred items (flagged, per register)
- RadOrderCreated emission (+ transactional outbox table in ihms_rad) — post-saga, first accepted event adds it.
- OrderCreated saga wiring (ordering->billing) — post-saga per KAFKA-EVENT-CATALOG §3; comment-only here.
