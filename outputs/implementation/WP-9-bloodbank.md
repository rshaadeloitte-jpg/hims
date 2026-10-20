# WP-9 — Blood Bank service (Catalog #13) — IMPLEMENTED
_Worker: bloodbank-implementer · 2026-08-23 · GAP-REGISTER-3 WP-9 · Template: services/adt-service_

## Result
`mvn -f services/bloodbank-service/pom.xml test` → **exit 0**, 38/38 tests green
(24 unit, 9 MockMvc, 5 PG16 Testcontainers integration on `postgres:16-alpine`).

## What was built
NEW `services/bloodbank-service` — package `com.hims.bloodbank`, artifact `bloodbank-service`,
`server.port 8096`, schema `ihms_bloodbank`, Flyway `V1__init_ihms_bloodbank.sql`,
`openapi/bloodbank-service.yaml`. Sibling conventions copied from adt-service:
default-deny SecurityConfig + DEV HTTP-Basic profile, CorrelationIdFilter, ApiError envelope,
explicit sequences (IMP-DEC-003), audit block, surefire argLine docker pin.

### Tables
- `t_blooddonor` (donor_id seq, name, blood_group, donation_status SMALLINT 1/2/3..6/7, last_action_at)
- `t_bloodrequisition` (requisition_id seq, patient_id, blood_group, ordered_volume_ml, requesteddate,
  sample_collected, **requisition_status** SMALLINT 1/2/3/10 CHECK, order_expired, order_validation_hours)
- `t_bloodrequisitiondetail` (component lines; requisition_status mirror, isorderexpired, expiry_reason)
- `t_bloodbag` (donor_id FK, blood_group, volume_ml, collected_at, expirydate DATE, `"crosSMatched"`
  legacy spelling, cross_matched_test_date, requisition_id link, isbagexpired, issued, issued_requisition_id)
- `t_bloodcomponent` (bag_id FK, type, volume_ml)
- NO outbox table — see deferred events below.

### APIs
- `POST/GET /api/v1/donors`, `GET /{id}`, `POST /{id}/touch` (BR-BLD-005 semantics: refreshes
  `last_action_at` so the 72h scan skips the donor).
- `POST /api/v1/requisitions` (patientId required; components[] optional; NO event — see below),
  `GET ?status=|patientId=`, `GET /{id}` full view (header+details+bags+cumulative volumes; read-only —
  BR-BLD-004 is NOT a hidden read-side mutation).
- `POST /{id}/crossmatch {bagId}` → marks bag crossmatched (+test date, links requisition);
  cumulative crossmatched ≥ ordered → status 2 else stays 1; **409** if bag already
  crossmatched/issued/expired; 400 if requisition cancelled/expired.
- `POST /{id}/issue {bagId}` → only crossmatched bags issuable (400 otherwise);
  cumulative issued ≥ ordered → 3 else stays current; 409 issued/expired.
- `POST /{id}/cancel` → 1|2 → 10; releases unissued crossmatched bags; 400 if issued/already 10.
- `POST /api/v1/bags` (+GET) so crossmatch/issue have bags.
- Jobs (all IDEMPOTENT, return summary counts):
  - `POST /api/v1/jobs/decross-expired` — BR-BLD-002 (crossmatched & unissued &
    `crossMatchedTestDate >= 48h` inclusive → decross, requisition recomputes toward 1) +
    BR-BLD-003 (`expirydate = today` → `isbagexpired=true` unless issued; crossmatched unissued
    expired → decross + recalc).
  - `POST /api/v1/jobs/expire-requisitions[?validationHours=]` — BR-BLD-004:
    `requesteddate + validation period <= now AND NOT sample_collected AND status=1`
    → status 10, order_expired=true, detail reason **'Order is expired'** (legacy literal).
  - `POST /api/v1/jobs/cancel-stale-donors` — BR-BLD-005: DNR_NEW/DNR_FIT untouched ≥72h
    (inclusive) → donation_status=7.

### Test coverage (register-mandated cases)
- partial cross-match stays 1; full cross-match → 2; issue partial stays current, full → 3;
  cancel → 10 (unit + PG16 end-to-end).
- decross: <48h untouched, **exactly-48h boundary decrossed** (inclusive), >48h decrossed;
  idempotent re-run returns zeros (unit + PG16 real timestamps).
- donor 72h rule: >72h and exactly-72h → 7; recently-touched kept; post-med-exam statuses
  (3..6) outside the scan (unit + PG16).
- bag expiry: expiring-today flagged, issued skipped, crossmatched expired decrossed (unit + PG16).
- BR-BLD-004: expired → 10 + reason; sample-collected / within-window untouched; re-run idempotent.
- 409/400 guard rails; default-deny 401; error envelope parity.

## Deferred / flagged (per register & catalog — never silently invented)
1. **NO BloodRequisitionCreated event** — catalog #13 / KAFKA-EVENT-CATALOG §3 DEFERS it.
   Flagged in: application class javadoc, RequisitionService javadoc, controller @Operation,
   migration table COMMENT, openapi description. No outbox table exists yet (asserted in PG16 test);
   add it with the first accepted event.
2. **Cron wiring DEFERRED** — legacy runs BR-BLD-002/003/004/005 as daily crons
   (ApplicationJobs.java:769-848, 1045-1061); here they are idempotent one-shot endpoints for an
   orchestrator. No `@Scheduled` job methods added. Flagged in javadoc + openapi.
3. `t_bloodcomponent` registration flow deferred with component intake (table + FK present).
4. RISK A8 DNR enum collision (5 = DNR_REJECTED == DNR_DONATED) documented in entity + migration;
   this service only writes 1/2/7. DB CHECK allows 1..7 for legacy data.
5. Legacy detail/header asymmetry (RISK R7) kept explicit: expiry/cancel mirror status at detail
   level incl. `expiry_reason`.
6. Cross-service patient existence not validated (opaque patientId) — same documented boundary as
   nursing/OT slices.

## Notes for the overseer
- Status recomputation is derived from linked bag state on every mutation, so decross/expiry
  naturally walk requisitions back 2→1 without special-case code; terminal status 10 is never
  overwritten by recalc.
- `requisition_status` is the BR-BLD-001 column; the audit-block boolean `status` column is
  deliberately unmapped in `BloodBankBase` (DB default TRUE) to avoid the type clash.
- Ports: 8096 as assigned; no conflicts.
