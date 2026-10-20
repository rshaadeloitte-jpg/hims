# WP-R3 — CSSD service (Catalog #15) — implementation report

Tranche 5, GAP-REGISTER-5 WP-R3. NEW `services/cssd-service` (port **8102**, schema **ihms_cssd**),
package `com.hims.cssd`, mirroring services/ordering-service conventions exactly
(Spring Boot 3.5.3 / Java 21, Flyway `V1__init_ihms_cssd.sql` with explicit sequences
(IMP-DEC-003), default-deny security config + dev-profile Basic fallback, error envelope +
BEXA-style codes, CorrelationIdFilter, MockMvc default-deny tests, PG16 Testcontainers).

## What was implemented

### Schema (V1__init_ihms_cssd.sql)
| Table | Notes |
|---|---|
| `t_kit` | kit_id PK seq; kit_code VARCHAR(30) UNIQUE NOT NULL; procedure_type VARCHAR(50); status SMALLINT DEFAULT 1 CHECK IN (1..5); sterilized_at TIMESTAMP NULL; expiry_days INT NOT NULL DEFAULT 14 CHECK > 0; audit cols |
| `t_kit_item` | composite PK = UNIQUE(kit_id, item_id); item_id BIGINT NOT NULL; qty INT CHECK > 0 |
| `t_sterilization_cycle` | cycle_id PK seq; kit_id FK NOT NULL; started_at NOT NULL; completed_at NULL; method VARCHAR(30) NOT NULL; result SMALLINT NULL CHECK IN (1=PASS, 2=FAIL) |
| `t_kit_issue` | issue_id PK seq; kit_id FK NOT NULL; issued_to_ref VARCHAR(80) NOT NULL; issued_at NOT NULL |
| `outbox` | transactional outbox TABLE per schema convention — see event policy below |

### Lifecycle guards (all pinned by unit + integration tests)
Status machine: **1=ASSEMBLED → 2=STERILIZING → 3=STERILE / 5=REJECTED → 4=ISSUED**
- assemble: `kitCode` required (≤30 chars) + unique (`CSSD_DUPLICATE_KIT_CODE`); ≥1 item line;
  per-line `itemId` non-null and `qty > 0`; `expiryDays` optional (>0, default 14).
- start cycle: ONLY from ASSEMBLED or STERILE (reprocess path tested); `method` required;
  moves kit to STERILIZING in the same tx.
- complete cycle: once-only (`CSSD_CYCLE_ALREADY_COMPLETED` on re-complete);
  result must parse PASS|FAIL (BEXA003 otherwise); PASS → STERILE + `sterilized_at` stamped,
  FAIL → REJECTED (**kit unusable**: issue → 400, further cycles → 400).
- issue: ONLY from STERILE (`CSSD_ILLEGAL_TRANSITION` otherwise; REJECTED kits explicitly
  called out as unusable in the message); `issuedToRef` required; **single active issue per
  kit** is enforced structurally — issuing flips the kit to ISSUED and no transition returns
  it to STERILE without a fresh PASS cycle; second issue attempt pinned by test.

### APIs (openapi/cssd-service.yaml matches controller paths 1:1)
- POST `/api/v1/kits` {kitCode, procedureType, items[{itemId,qty}], expiryDays?} → 201
- GET `/api/v1/kits/{id}` · GET `/api/v1/kits?status=&procedureType=` (numeric or name filter)
- POST `/api/v1/kits/{id}/cycles` {method}
- PUT `/api/v1/cycles/{id}/complete` {result: PASS|FAIL}
- POST `/api/v1/kits/{id}/issues` {issuedToRef} → 201 · GET `/api/v1/kits/{id}/issues`

### Event policy (register: KitIssued / SterilizationCompleted REJECTED phase 1)
- The service emits NOTHING to the outbox on any transition. It has no emitter dependency at
  all (no OutboxService/Kafka wiring in its constructor graph), pinned by an L0 test asserting
  exactly one repository-save per lifecycle transition and nothing else.
- Explicit NEGATIVE outbox assertions in the PG16 integration test: `outbox` count == 0 after
  migration/boot, after every rejected guard, after the full happy lifecycle, and after the
  FAIL→REJECTED path.
- Consuming ProcedureScheduled / InventoryUpdated is post-saga — comment-only, never invented here.

## Deviations (documented, none silent)
1. **No Kafka/relay infrastructure.** Unlike ordering-service, cssd-service does not include
   spring-kafka / OutboxService / relay beans because it has ZERO events in phase 1 (both
   candidates REJECTED). The outbox *table* still exists per schema convention and the entity +
   repository exist read-side for the negative assertions. Commented in pom.xml, OutboxEvent,
   OutboxRepository, application class.
2. **≥1 item line required at assembly** (mirrors sibling "lines ≥ 1" convention). The register
   lists the items array without stating the min-count rule; treated as implied by the shape and
   validated BEXA003-style. Documented in CssdKitService javadoc.
3. **Expiry not enforced at issue time.** `expiry_days` is stored/returned but no expired-kit
   issuance guard exists because the register's lifecycle-guard list does not include one — adding
   it would exceed the spec. Flagged here as a phase-2 candidate.
4. **Duplicate kitCode → 400 CSSD_DUPLICATE_KIT_CODE** (not 409): register doesn't assign a code;
   kept consistent with the sibling all-400-except-404 convention. DB-level `uq_t_kit_code` backs it.

## Deferred items
- Saga consumption of ProcedureScheduled / InventoryUpdated (post-saga, comment-only).
- KitIssued / SterilizationCompleted emission if a consumer ever materializes (catalog REJECTED).
- Expiry enforcement at issue time (see deviation 3).

## Test results

`mvn -f services/cssd-service/pom.xml test` → **BUILD SUCCESS, exit 0**

| Test class | Tests | Result | Covers |
|---|---|---|---|
| `application.CssdKitServiceTest` (L0) | 18 | ✅ 0 failures/errors | assemble guards (kitCode/unique/items/qty/expiry default+positive), start-cycle guard ASSEMBLED/STERILE-only + unknown kit 404 + method required, complete-once + PASS/FAIL enum, FAIL→REJECTED unusable, reprocess STERILE→cycle→STERILE, issue STERILE-only/single-active-issue/issuedToRef, issues-listing 404, negative-event structural assertion |
| `api.KitControllerTest` (L1 MockMvc) | 9 | ✅ | default-deny (401 on all 7 endpoints), create 201, no-items 400 envelope, duplicate code 400 envelope, illegal-transition 400 envelope w/ message, complete PASS mapping + unknown cycle 404, issue 201 + unknown kit 404, named/numeric status filter |
| `infrastructure.persistence.CssdServiceIntegrationTest` (L2 PG16) | 3 | ✅ | Flyway V1 applies (5 tables + 3 sequences present), full happy lifecycle on real PG16 incl. DB-level unique kit_code + every NEGATIVE outbox assertion (count==0 throughout), FAIL→REJECTED path + negative outbox assertion |

**Total: 30 tests, 0 failures, 0 errors, 0 skipped.**

## Files written
```
services/cssd-service/pom.xml
services/cssd-service/src/main/java/com/hims/cssd/CssdServiceApplication.java
services/cssd-service/src/main/java/com/hims/cssd/api/{ApiError,GlobalExceptionHandler,KitController}.java
services/cssd-service/src/main/java/com/hims/cssd/application/CssdKitService.java
services/cssd-service/src/main/java/com/hims/cssd/application/exceptions/{CssdRuleException,InvalidKitRequestException,CssdNotFoundException}.java
services/cssd-service/src/main/java/com/hims/cssd/configuration/{SecurityConfig,DevSecurityConfig}.java
services/cssd-service/src/main/java/com/hims/cssd/domain/model/{CssdBase,Kit,KitItem,KitItemKey,SterilizationCycle,KitIssue}.java
services/cssd-service/src/main/java/com/hims/cssd/domain/repository/{KitRepository,KitItemRepository,SterilizationCycleRepository,KitIssueRepository}.java
services/cssd-service/src/main/java/com/hims/cssd/infrastructure/outbox/{OutboxEvent,OutboxRepository}.java
services/cssd-service/src/main/java/com/hims/cssd/infrastructure/web/CorrelationIdFilter.java
services/cssd-service/src/main/resources/application.yml
services/cssd-service/src/main/resources/application-dev.yml
services/cssd-service/src/main/resources/db/migration/V1__init_ihms_cssd.sql
services/cssd-service/src/test/java/com/hims/cssd/application/CssdKitServiceTest.java
services/cssd-service/src/test/java/com/hims/cssd/api/KitControllerTest.java
services/cssd-service/src/test/java/com/hims/cssd/infrastructure/persistence/CssdServiceIntegrationTest.java
openapi/cssd-service.yaml
outputs/implementation/WP-R3-cssd.md   ← this report
```
No other service or shared file was touched.
