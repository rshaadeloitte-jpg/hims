# WP-5 — ADT & Bed service (Catalog #3) — IMPLEMENTATION REPORT

_Worker: adt-implementer · Tranche 2 (GAP-REGISTER-2) · 2026-08-23_

## Deliverable

NEW `services/adt-service` (`com.hims.adt`, artifact `adt-service`, `server.port 8092`,
schema `ihms_adt`, topic `ihms.adt.events` + DLQ `dlt.ihms.adt.events`). Structure is a
sibling clone of `services/appointment-service`: pom incl. surefire `-Dapi.version=1.44`
argLine, DevSecurityConfig (profile `dev`, HTTP Basic IMP-DEC-019) + default-deny
SecurityConfig, GlobalExceptionHandler + ApiError envelope, CorrelationIdFilter,
transactional outbox infra (OutboxEvent / OutboxRepository / OutboxService /
OutboxPublisher w/ CAS claim + DLQ routing / KafkaOutboxRelay), layering
api/application/domain/infrastructure, Flyway V1, openapi spec.

## Acceptance items

### 1. `mvn -f services/adt-service/pom.xml test` → exit 0

```
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS (exit 0)
  AdtControllerTest ............ 8   (MockMvc contract tests, default-deny enforced)
  AdmissionServiceTest ....... 11   (L1 rules, no Spring context)
  AdtServiceIntegrationTest ... 1   (PG16 Testcontainers — EXECUTED, Docker present)
```

Integration test is `@Testcontainers(disabledWithoutDocker = true)` on `postgres:16-alpine`
with `@ServiceConnection`; it ran to completion (Flyway V1 applied on real PG16).

### 2. Domain & masters

- `m_ward` (ward_id seq PK, name, ward_type, iscensus flag, unit_id, audit block) —
  POST/GET(list ?unitId)/GET/{id}/PUT `/api/v1/wards`.
- `m_bed_status_master` seeded in V1 per STATUS-CODE-DICTIONARY §A5:
  **0**=Available (reset on cancel/transfer, isrelease=true), **1**=Free (true),
  **3**=Free predicate (true), **5**=Occupied/retained (**false**), **6**=Released
  post-discharge (true). BR-DIS-003's hard-pinned literal 6 (RISK R5) is pinned by seed +
  comment; `isrelease` governs allocation (BR-IPD-003).
- `m_bed` (bed_id seq PK, bed_no, ward_id FK, category, building, occupied DEFAULT false,
  bed_status_id FK DEFAULT 1, isnoncensus/isot/ispreop/ispostop DEFAULT false, audit block)
  — POST/GET(list ?wardId)/GET/{id}/PUT `/api/v1/beds`;
  GET `/api/v1/beds/available?wardId=` returns beds matching the BR-IPD-002 free-bed
  predicate joined against master `isrelease=true`. Partial index `ix_m_bed_free` backs the scan.
- `t_admission`: admission_id seq PK, patient_id, encounter_id, bed_id, ward_id, unit_id,
  admission_type, ipd_no UNIQUE, doctor_id, admitted_at, is_cancel DEFAULT false,
  discharge block (discharge_id, discharge_type, is_expired, discharge_date) null until
  discharge. Partial unique index `uq_t_admission_active` (patient_id WHERE is_cancel=false
  AND discharge_date IS NULL) is a DB backstop for BERB013.
- Sequences: m_ward_seq, m_bed_seq, t_admission_seq, t_patientdischargedata_seq (IMP-DEC-003).

### 3. Business rules

| Rule | Implementation | Test |
|---|---|---|
| BR-IPD-001 doctor required | missing `doctorId` → 400 **BERA022** | unit + MockMvc |
| BR-IPD-001 no duplicate active admission | active = is_cancel=false AND discharge_date IS NULL → 409 **BERB013** (+ partial unique index backstop); no outbox writes on failure | unit + integration |
| BR-IPD-002 auto-allocation | bedId omitted → candidates from `findFreeBeds()` (occupied=false AND status=true AND isnoncensus/isot/ispreop/ispostop=false), sorted preferring wards of the admitting unit then lowest bed_id; each claimed via atomic CAS `UPDATE m_bed SET occupied=true, status=5 WHERE bed_id=? AND occupied=false`; all claims lost or no candidate → 409 **BEXB001** | unit (first-fit + preference + CAS-loss) |
| BR-IPD-002 explicit bedId | load bed → 404 if absent; CAS claim; loss → 409 BEXB001 | unit + integration (claim of occupied bed returns 0 rows) |
| BR-IPD-003 release gate | chosen bed's `m_bed_status_master.isrelease` must be true else 400 **BERB005** (both auto list and explicit path) | unit + integration |
| ipdNo | `'IPD-' || admission_id`, UNIQUE | unit + integration |
| Transfer | target CAS claim, old bed released occupied=false status=**0** (§A5 transfer reset), admission bed/ward updated | unit + integration |
| BR-DIS-003 discharge release | bed occupied=false, bed_status_id=**6** pinned; double-discharge/cancelled → 400 | unit + integration |

### 4. Outbox events — exact catalog parity

Type strings verified verbatim vs KAFKA-EVENT-CATALOG E-03/E-04/E-11:

| Event | Type string | Schema | Topic | Partition key | Payload keys (exact set asserted in tests) |
|---|---|---|---|---|---|
| E-03 | `PatientAdmitted` | patient-admitted.v1 | ihms.adt.events | `patientId` | admissionId, patientId, encounterId, bedId, wardId, unitId, admissionType, ipdNo, doctorId, admittedAt |
| E-04 | `PatientDischarged` | patient-discharged.v1 | ihms.adt.events | `patientId` | dischargeId, patientId, admissionId, encounterId, dischargeType, isExpired, date, balanceDue(null ok phase 1) |
| E-11 | `BedChanged` | bed-changed.v1 | ihms.adt.events | `bedId` (BEDID per catalog) | bedId, admissionId, oldStatus, newStatus, changedAt |

The PG16 integration test asserts for each row: event_type string, version v1,
aggregate type/id, partition key, and the payload key set via JSON parse
(`containsExactlyInAnyOrder`) — including BedChanged oldStatus/newStatus transitions
(1→5 transfer claim, 5→6 discharge release). Discharge emits PatientDischarged **plus**
a BedChanged for the released bed, per register.

### 5. Sibling conventions & spec parity

- pom modeled on appointment-service incl. surefire argLine fix; same dependency set
  (web/data-jpa/security/actuator/kafka/flyway/postgres/springdoc/testcontainers).
- openapi/adt-service.yaml covers ALL paths: `/api/v1/wards(/{id})`,
  `/api/v1/beds(/available)(/{id})`, `/api/v1/admissions(/{id})(/transfer)(/discharge)`
  with error responses documented (400/404/409 incl. BERA022/BERB013/BEXB001).
- Queries: GET `/api/v1/admissions/{id}`, GET `/api/v1/admissions?patientId=&active=`
  (integration asserts discharged patients drop out of active=true).

## Deferred (documented, not in register scope)

- BR-DIS-001 advised-discharge/billing preconditions and balanceDue computation (lands
  with Billing slice); BR-IPD-004 IPD number template formatting ('IPD-<id>' used per
  register); BR-IPD-005 interim-abscond job; universal-bed restore & cradle re-map of
  BR-DIS-003 (needs amenity/cradle flags on m_bed); Redis discharge lock + Solr indexing;
  HL7 A01 via Interop consumers.

## Verification commands

```
mvn -f services/adt-service/pom.xml test          # exit 0, 20/20, PG16 executed
grep '"PatientAdmitted"' services/adt-service/src/main/java -r
```
