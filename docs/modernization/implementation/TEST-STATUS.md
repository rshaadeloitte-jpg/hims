# TEST-STATUS — iHMS Modernization

> Levels per TEST-STRATEGY.md: L0 unit · L1 contract replay · L2 service integration (PG) · L3 E2E (gateway+React) · L4 performance · L5 UAT.
> **Updated**: 2026-08-25 (MIG-033 ordering 62/62 added; MIG-031 appointment 50/50 + MIG-032 ADT 54/54 — reference 86/86 + item 19/19 + grievance 24/24 + waste 28/28 + linen 32/32 + transport 47/47 + diet 53/53 + equipment 112/112 + queue 28/28 + patient 26/26 + **appointment 50/50** + **adt 54/54**)

### 2026-08-25 additions (MIG-033 Ordering & Prescription saga rewrite, IMP-DEC-042)

`mvn clean test` (ordering-service): **Tests run: 62, Failures: 0, Errors: 0** — BUILD SUCCESS.

| Test | Level | Coverage | Result |
|---|---|---|---|
| OrderSagaTest (15) | L0 | BR matrix: BR-ORD-001 patient+prescriber required & encounter XOR admission; BR-ORD-002 ≥1 item, rate≥0 qty>0, server-side gross; BR-ORD-003 concession [0,100] HALF_UP 2dp ≤gross; BR-ORD-004 doctorSharePercent default 0 validated pre-persist; totals math; outbox OrderInitiated append in same unit-of-work; partition key fallback chain encounterId→admissionId→no-context | ✅ PASS |
| OrderSagaStateMachineTest (12) | L0 | step derivation (billing always; lab/rad/nursing per item type; pharmacy only with linked prescription); confirmStep PENDING→SUCCEEDED idempotent mid-flight AND post-completion; all-applicable-succeeded → CONFIRMED/COMPLETED; failStep → failed FAILED + succeeded COMPENSATED + order FAILED + rejectionReason + one OrderFailed same tx; replay refreshes reason only; cancel compensates + emits CANCELLED_BY_USER and does NOT flip order status itself (caller owns flip) | ✅ PASS |
| PrescriptionServiceTest (13) | L0 | BR-ORD-005 dose+frequency required per line, qty>0, ≥1 line, encounter XOR admission; E-06 create-only; cancel ACTIVE-guard; ihms_ordering.prescription_seq explicit assignment | ✅ PASS |
| OrderControllerTest (10), PrescriptionControllerTest (5) | L1 | @WebMvcTest + SecurityConfig contract: 201/400 legacy error envelope {status:"ERROR",errorCode,message} incl. ORD_ILLEGAL_TRANSITION, ORD_SAGA_STEP_INVALID, BEXA003, BR-ORD codes; internal steps/{step}/confirm|fail routing; status-name query parsing | ✅ PASS |
| OrderingSagaIntegrationTest (7) | L2 | real PG16+Kafka (Testcontainers postgres:16-alpine + confluentinc/cp-kafka:7.6.0): placement writes order+items+saga+OrderInitiated atomically (single-tx proof via rollback injection); happy path all steps → CONFIRMED/saga COMPLETED; lab failure compensates billing end-to-end with rejectionReason persisted; cancel-after-partial-confirm compensation; multi-item prescription persistence incl. BR-ORD-005 atomic reject; guards on confirmed orders; Kafka roundtrip consumes OrderInitiated+PrescriptionCreated+OrderFailed from ihms.ordering.events and asserts outbox rows reach PUBLISHED (jsonb NUMERIC scale normalization handled via decimalValue comparisons) | ✅ PASS |

### 2026-08-25 additions (MIG-032 ADT & Bed Management, IMP-DEC-041)
| Test | Level | Scope | Result |
|---|---|---|---|
| BedStateTransitionTest (7) | L0 | §A5+CLEANING state machine: codes pinned (0/1/2/3/5/6), isrelease claim matrix (BR-IPD-003), transferReleaseTarget=CLEANING(2) unclaimable while dirty, dischargeReleaseTarget pinned literal 6 (RISK R5), canFinishCleaning only from CLEANING, full lifecycle round-trip, unknown code rejected | ✅ PASS |
| ClearanceChecklistLogicTest (4) | L0 | BR-DIS-001 checklist: exactly NURSING/PHARMACY/BILLING/LAB known codes, fresh rows PENDING, cleared rows never revert (no backward edge), discharge allowed only when zero pending | ✅ PASS |
| AdmissionServiceTest (23) | L0 | BERA022 missing doctor, BERB013 duplicate active, ADT_MLC_NUMBER_REQUIRED, admit happy-path seeds detail + 4 clearance rows (attending defaults to admitting doctor), E-03 payload keys, BR-IPD-001 ipdNo template A/IHMS/<yy>/%05d sequential + %05d overflow, BERB005 non-releasable bed, auto-allocation skips foreign reservations / consumes own reservation / BEXB001 when all CAS claims lost, reservation dup ADT_BED_RESERVED + past expiry ADT_REQ_INVALID, transfer old-bed→CLEANING + log + BedTransferred(key patientId) + 2×BedStatusChanged(key bedId), CAS-loss transfer leaves old bed untouched, cleaning-complete guard + event 2->1, discharge blocked ADT_CLEARANCE_PENDING, all-clear discharge exact E-04 keys + bed release literal 6, death records deathDate + isExpired:true, abscond bypasses gate, manual abscond stamps datetime, unknown-department rejection, idempotent re-clear keeps first stamp, sweep only unbedded stale admissions → AUTO_ABSCONDED + PatientDischarged, unknown admission 404 | ✅ PASS |
| AdtControllerTest (14) | L1 | default-deny 401 incl. /bed-reservations; ward CRUD roundtrip; beds available + cleaning-complete endpoint; admit 201 ipdNo shape; MLC body forwarded; BERB013→409 envelope; BERA022→400; transfer BEXB001→409; transfers history; discharge with deathDate; abscond endpoint; clearance list + clearDepartment; reserve 201 ACTIVE; cancel 204 | ✅ PASS |
| AdtServiceIntegrationTest (6) | L2 | real PG16+Kafka (Testcontainers): V1 creates ihms_adt incl. §A5 seeds + isrelease=6; full flow admit(auto-alloc, ipdNo template)→BERB013 dup→BERB005 occupied→transfer CLEANING(2)+log row+cleaning-complete→clearance gate blocks→clear 4 depts→discharge bed literal 6→re-admit on released bed→double-discharge rejected, E-03/E-04 exact payload keys + all four lifecycle types with catalog partition keys; 6-thread parallel bed race exactly-one-winner (2 beds, 4×BEXB001, no double occupancy); concurrent same-patient admit yields exactly one ACTIVE row (uq_t_admission_active); reservation exclusivity (dup ADT_BED_RESERVED, foreign skip, owner CONSUMED); abscond sweep auto-absconds stale unbedded admissions + emits PatientDischarged key 888; outbox→Kafka roundtrip consumes PatientAdmitted/PatientDischarged/BedTransferred/BedStatusChanged on ihms.adt.lifecycle with envelope contract (schemaName patient-admitted.v1, payload.ipdNo) and all outbox rows PUBLISHED | ✅ PASS |

### 2026-08-24 additions (MIG-031 appointment & encounter, IMP-DEC-040)
| Test | Level | Scope | Result |
|---|---|---|---|
| AppointmentStatusDictionaryTest (5) + LegacyVisitStatusSplitBrainTest (3) | L0 | BR-APT-001 locked transition matrix incl. illegal-transition throw; VisitStatus 11/12 split-brain: advanceBillPayment never regresses, advanceDispense strict guard (already-terminal wins BEFORE pending gate; explicit dispense with pending items throws ENC_DISPENSE_BLOCKED) | ✅ PASS |
| AppointmentServiceTest (16) | L0 | booking validation matrix (BEXA003/APPT_DOCTOR_MISMATCH/APPT_DAY_MISMATCH/APPT_DEPT_MISMATCH/APPT_SLOT_NOT_IN_SCHEDULE/APPT_CABIN_MISMATCH), CAS capacity claim APPT_SLOT_TAKEN, token issuance, reschedule stubs, cancel releases capacity, check-in daily OPD no, complete closes encounter | ✅ PASS |
| AppointmentControllerTest (15) | L1 | default-deny, booking/check-in/complete/reschedule/cancel/no-show contracts, legacy error envelopes, missing ?date → 400 BEXA003 (not 500), ISO LocalTime slot times ("09:30:00") | ✅ PASS |
| AppointmentEncounterIntegrationTest (11) | L2 | real PG16+Kafka: Flyway objects, booking+outbox same tx, double-book prevention, 4-thread CAS race exactly-one-winner, cancel/rebook slot release, reschedule link, check-in daily counters + E-02 payload (literal opdNo/token key via Jackson parse), split-brain guards on real columns, complete→EncounterClosed partitionKey=encounterId, AUTO_DAILY_CLOSE job, Kafka roundtrip on ihms.appointment.encounter until all event types seen + outbox PUBLISHED | ✅ PASS |

### 2026-08-22 additions (MIG-030 patient-service kickoff — Wave 3 Clinical Spine, IMP-DEC-039)
| Test | Level | Scope | Result |
|---|---|---|---|
| PatientRegistrationServiceTest (13) | L0 | BR-PAT-001: permanent MRN `ddMMYY+%09d(seq id)` formula (incl. boundary padding), caller mrno rejected as server-owned, firstName/gender required; BR-PAT-002 matrix: Aadhaar→BERA011, VID/Janaadhaar→BERA011, ABHA healthIdNumber→BEXA040 with existing MRN in message (case-insensitive), mobile+name+dob heuristic→BERA011, unique passes + PatientRegistered outbox append verified; update: demographics allow-list never copies id/mrno/status/addedBy, stamps updatedBy, emits PatientUpdated; update re-runs matrix on identity change; unknown patient 404; document linkage requires docType/docRef + known patient | ✅ PASS |
| PatientControllerTest (7) | L1 | default-deny 401 on register/get; register 201 shape (id+mrno); BERA011 and BEXA040 map to 400 legacy-code envelopes not 500; search by aadhaar; get by id; PUT delegation; documents link 201 + list; unmapped path 404 NOT_FOUND | ✅ PASS |
| PatientRegistrationIntegrationTest (6) | L2 | real PG16+Kafka (Testcontainers): Flyway V1 creates ihms_patient (3 tables, sequences, ux_patient_mrno); register writes MRN per formula + outbox row PENDING in same tx (aggregateId=patientId, partitionKey=patientId); duplicate matrix on live indexes (aadhaar BERA011, healthIdNumber BEXA040 with existing MRN, mobile/name/dob BERA011, distinct passes, DB unique index holds); update emits PatientUpdated + server-owned fields untouched; document linkage persists + unknown owner 404; outbox→Kafka round-trip marks PUBLISHED on ihms.patient.registration | ✅ PASS |

### Live-stack E2E (2026-08-22, MIG-030 patient, containerized PG16 + compose Kafka, port 8090)
- Platform PG16 healthy + Kafka KRaft up; patient-service jar booted dev profile: `/actuator/health` **UP** in 4s; Flyway V1 applied
- Security live: anonymous get → **401**
- Registration live: register → id=1, MRN `220826000000001` (=ddMMYY+%09d); aadhaar dup → 400 BERA011 (existing MRN cited); ABHA dup → 400 BEXA040 (`already registered for MRN 220826000000002`); mobile+name+dob → 400 BERA011; caller-supplied mrno → 400 BEXA003 "mrno is server-owned"
- Update live: city change applied, mrno untouched, PatientUpdated emitted; document linked (201) and listed
- Kafka live: `ihms.patient.registration` received 4 messages keyed by patientId — 3×PatientRegistered + 1×PatientUpdated with catalog envelope (eventId/eventType/version/partitionKey/payload.mrno/producerService=patient-service); relay dispatched twice, all rows PUBLISHED
- OpenAPI generated from running service: `openapi/patient-service.yaml`, 4 paths
- CI: `patient-service-build-test` job added to `.github/workflows/ci.yml`


### 2026-08-22 additions (MIG-022 queue-service extraction, IMP-DEC-038)
| Test | Level | Scope | Result |
|---|---|---|---|
| QueueServiceTest (15) | L0 | daily-counter issuance (server-owned tokenNo/status), missing patient/dept → BEXA003, bad priority/server-owned status rejected, emergency jump on call-next, empty-queue guard, deactivated/unknown counter guards, transfer re-queue semantics (calledAt cleared, dept follows target counter), terminal-token transfer rejection, forward-only no-show/complete guards, counter master create with doctor mapping | ✅ PASS |
| QueueControllerTest (7) | L1 | default-deny 401 on tokens+dashboard; issue 201 shape; call-next delegation; dashboard projection shape; BEXA003 envelope not 500; unmapped path 404 NOT_FOUND; counters create/list contract | ✅ PASS |
| QueueServiceIntegrationTest (6) | L2 | real PG16 (Testcontainers): Flyway V1 creates ihms_queue (3 tables + 2 sequences); daily counter increments and persists per date; full lifecycle issue→call→transfer(dept follows)→recall→complete with terminal guard; emergency priority jump persists; dashboard counts/nowServing/waitingList; no-show forward-only guard on real columns | ✅ PASS |

### Live-stack E2E (2026-08-22, MIG-022 queue, containerized PG16 + dev-profile boot, port 8089)
- Platform PG16 (`ihms-pg`) healthy; queue-service jar booted `--spring.profiles.active=dev`: `/actuator/health` **UP** in 3s; Flyway V1 applied to dev PG
- Security live: anonymous dashboard → **401**; dev basic-auth chain green (`devops` creds per grievance-lineage application-dev.yml)
- Lifecycle live: counter OPD-1 created (dept 9, doctorId 77) → tokens 1,2 issued NORMAL + token 3 EMERGENCY → call-next called **token 3** (priority jump) → transfer to OPD-2 (counterId 2, departmentId follows to 10, re-queued WAITING) → recall at OPD-2 → complete (3); FIFO resumed (token 1) → no-show (4)
- Guards live: no-show on WAITING → 400 BEXA003 "Cannot no-show token in status 1"; complete on WAITING → 400
- Dashboard live: `{waiting:1, called:0, completed:1, noShow:1}`, nowServing map, ordered waitingList
- OpenAPI generated from running service: `openapi/queue-service.yaml`, 7 paths
- CI: `queue-service-build-test` job added to `.github/workflows/ci.yml`
- Test-infra lesson recorded: class-level @Transactional tests deadlock REQUIRES_NEW services (ON CONFLICT waits on the outer uncommitted DELETE of the same row) — queue L2 runs WITHOUT the wrapper and cleans via auto-committing JdbcTemplate deletes

### 2026-08-22 additions (MIG-023 sub-slice — Equipment S18 equipment-service finalization, IMP-DEC-037)
| Test | Level | Scope | Result |
|---|---|---|---|
| EquipmentServiceTest (28) | L0 | asset register: sequence id, BEXA003 itemId required, workingStatus default WORKING / invalid rejected, search by unit/accession/item, guarded transfer (log row + department move, unknown asset 404), nonNull update semantics | ✅ PASS |
| PmServiceTest (29) | L0 | PM schedule lifecycle: create (code `IHMS/EQ/PM/yyyy/%05d`, nextDue), due listing, completion with next-due roll-forward, gated search, checklist template CRUD | ✅ PASS |
| WarrantyServiceTest (20) | L0 | warranty/AMC add (seq id, WARRANTY default/trim, server-owned equipmentId pairing, asset guards), per-asset list, expiring windows (default 30d + sentinels + boundary), counts incl. (unspecified) bucket, null-patch update with server-owned skip-list, lifecycle chain | ✅ PASS |
| EquipmentControllerTest (10) | L1 | default-deny 401; register 201 shape; list/get/by-accession/by-item delegation; transfer 201; unmapped path 404 NOT_FOUND envelope | ✅ PASS |
| MasterDataControllerTest (11) | L1 | generic masters CRUD contract: create 201 (code+name), missing code → BEXA003, PUT exact-PATCH semantics (absent keys never copied), delete 200, unknown masterType 400, default-deny 401 | ✅ PASS |
| EquipmentServiceIntegrationTest (14) | L2 | real PG16 (Testcontainers): Flyway V1/V2/V3 apply (ihms_equipment 19 tables); V3 census checks; asset CRUD round-trip via sequences; complaint→resolution lifecycle on live columns; document soft-delete tombstone; usage NUMERIC(8,2) parity + report window [from,to) + NULL-date exclusion; master-data registry incl. id-hijack rejection and active/unit gates | ✅ PASS |

### Live-stack E2E (2026-08-22, MIG-023 sub-slice equipment, containerized PG16 + dev-profile boot, port 8088)
- Platform PG16 (`ihms-pg` from platform compose, docker.io/library/postgres:16-alpine) healthy; equipment-service jar booted `--spring.profiles.active=dev`: `/actuator/health` **UP** in 4s; Flyway applied V1–V3 to dev PG
- Security/contract live: anonymous `/api/v1/equipment` → **401**; authenticated chain via dev basic auth
- Asset register live: register (201, id=1 seq, workingStatus=1 WORKING) → get 200 → transfer 10→22 (201, log row) → transfers list 200
- Complaints live: raise (201, complaintNo `IHMS/EQ/2026/00001`, status 1, departmentId auto-stamped 22 from last transfer) → resolution add (201, resolvedStatus 3); unknown complaint → 404 error envelope
- PM live: schedule create (201, `IHMS/EQ/PM/2026/00001`, month grid stamped) → complete (201, maintenanceNo `PM-2026-00002`, pmStatus 3, next-due roll-forward)
- Warranty live: AMC add (201) → counts `{totalActive:1, expired:0, expiringIn30Days:0, byType:[AMC:1]}`
- Masters live: service-agent create (201, code ACME); Usage live: log 4.5h (201) → date-range report `{entryCount:1, totalHours:4.50}`
- OpenAPI generated from running service: `openapi/equipment-service.yaml`, 29 paths
- CI: `equipment-service-build-test` job added to `.github/workflows/ci.yml`
- Toolchain note: local Docker Desktop 4.86 (Engine 29.7.2) rejects docker-java's legacy API request path (`MinAPIVersion: 1.40`) → Testcontainers bumped 1.20.4→1.21.3 + surefire `-Dapi.version=1.44`; CI Podman unaffected (IMP-DEC-037)

## Slice S0 test inventory

| Test | Level | Scope | Status | Result |
|---|---|---|---|---|
| MasterReadUseCaseTest (9 tests) | L0 | entity registry, BEXA003 invalid id, NOT_FOUND, filter whitelist, legacy offset pagination | ✅ PASS | `mvn verify` 2026-08-14 |
| MasterReadControllerTest (7 tests) | L1 | JSON shape parity (legacy field names), default-deny 401, 400/404 error codes, entities list | ✅ PASS | `mvn verify` 2026-08-14 |
| KafkaOutboxRelayTest (1 test) | L0 | envelope fields per KAFKA-EVENT-CATALOG, partition key, DLQ path, CAS claim | ✅ PASS | `mvn verify` 2026-08-14 |
| OutboxKafkaIntegrationTest (1 test) | L2 | outbox append (same tx) → relay → Kafka → PUBLISHED (Testcontainers PG16 + Kafka) | ✅ PASS | `mvn verify` 2026-08-14 |
| JpaMasterReadRepositoryIntegrationTest (11 tests) | L2 | Flyway quoted tables, sequences, unit-scope semantics, search/pagination, health probes, correlation-id, default-deny on real PG16 | ✅ PASS | `mvn verify` 2026-08-14 |

**Total: reference-service 86 backend tests + item-service 19 backend tests + grievance-service 24 backend tests + waste-service 28 backend tests + linen-service 32 backend tests + transport-service 47 backend tests + diet-service 53 backend tests + equipment-service 112 backend tests + queue-service 28 backend tests + 25 frontend tests — 0 failures, 0 errors.** Run: `mvn -f services/reference-service/pom.xml verify` + `mvn -f services/item-service/pom.xml verify` + `mvn -f services/grievance-service/pom.xml verify` + `mvn -f services/waste-service/pom.xml verify` + `mvn -f services/linen-service/pom.xml verify` + `mvn -f services/transport-service/pom.xml verify` + `mvn -f services/diet-service/pom.xml verify` + `mvn -f services/equipment-service/pom.xml verify` + `mvn -f services/queue-service/pom.xml verify` + `npm test` in frontend/

### 2026-08-20 additions (MIG-020 grievance-service extraction — IMP-DEC-031/032, Wave 2 opened)
| Test | Level | Scope | Result |
|---|---|---|---|
| GrievanceTicketServiceTest (12) | L1 | ticket number rule `IHMS/G/yyyy/%05d` + status/close stamping; status machine (RESOLVED→closeStatus 2+resolvedDate, assignToCreator→closeStatus 3, invalid status → BEXA003, missing ticket → 404); reopen raise→ASSIGNED+PENDING vs plain→NEW; feedback closes ticket; assign keeps issueStatus unchanged; comment validation | ✅ PASS |
| GrievanceTicketControllerTest (7) | L0 | default-deny 401 on raise; 201 ticket shape; dashboard counts map; status change shape; comment 201; list shape; unmapped path 404 NOT_FOUND (not 500) | ✅ PASS |
| GrievanceServiceIntegrationTest (5) | L2 | real PG16 (Testcontainers): Flyway V1 creates ihms_grievance (7 tables + ticket/ticket-no sequences); masters create + hierarchy (category→sub-category, severity, sequence ids); full lifecycle raise→comment→assign→ASSIGNED→RESOLVED→reopen→feedback-close with legacy status semantics; dashboard count buckets (TOTAL/NEW/ASSIGNED/RESOLVED/RAISED-BY-CITIZEN/TODAYS-TICKET); list scoping by unitId / mobileNo / all | ✅ PASS |

### Live-stack E2E (2026-08-20, MIG-020 grievance, Podman stack)
- grievance-service container (`docker.io/hims/grievance-service:0.1.0-SNAPSHOT`, port 8083, dev profile) on `ihms-platform_default`: `/actuator/health` **UP**; Flyway applied V1 to dev PG (`ihms_grievance` + history)
- Security/contract live: anonymous raise → **401**; unmapped path → **404** NOT_FOUND envelope
- Lifecycle live: raise (201, ticketNo `IHMS/G/2026/00001`, issueStatus 1) → comment (201) → assign employee 42 (status stays 1, legacy parity) → status 2 ASSIGNED → status 3 RESOLVED (closeStatus 2 + resolvedDate) → reopen raise (status 2, closeStatus 1, reopenedBy stamped) → feedback (201) → final closeStatus 3 + feedbackId 1
- Dashboard counts live: TOTAL 2 / NEW 1 / ASSIGNED 1 / RESOLVED 0 / RAISED-BY-CITIZEN 1 / MY-LIST 1 / TODAYS-TICKET 2
- Masters live: category + severity + sub-category created (201, sequence ids, unit-scoped)
- List scoping live: all=2, unitId=7→1, mobileNo citizen view→1
- OpenAPI generated from running service: `openapi/grievance-service.yaml`, 15 paths
- CI: `grievance-service-build-test` job added to `.github/workflows/ci.yml` (4 jobs total)

### 2026-08-20 additions (MIG-023 sub-slice — Linen & laundry S16 linen-service extraction, IMP-DEC-034)
| Test | Level | Scope | Result |
|---|---|---|---|
| LinenRequestServiceTest (17) | L1 | requisition number rule `{unitPrefix}/LR/yy/%05d` + default prefix; 7-state machine guards (approve PENDING→ACCEPTED, wrong status → BEXA003, handover requires handedOverTo); receive-main-store stamps receivedQuantity+damagedQuantity+reason; dispatch ACCUMULATES damagedQuantity (1+2=3); complete stamps receivedBy+finalQuantity; soft delete status=false+requestStatus=0; updateDetails requires PENDING + ownership check; unknown request → 404 | ✅ PASS |
| LinenRequestControllerTest (9) | L0 | default-deny 401 on raise; 201 request shape (requisitionNo + requestStatus 1); dashboard counts map; approve/handover/complete delegation; details list shape; soft delete shape; unmapped path 404 NOT_FOUND (not 500) | ✅ PASS |
| LinenServiceIntegrationTest (6) | L2 | real PG16 (Testcontainers): Flyway V1 creates ihms_linen (2 tables + 3 sequences); full 7-state lifecycle raise→approve→handover→receive-main-store→process→dispatch→complete with detail stamping + damaged accumulation + counts; soft delete excludes from list; store sees source AND destination; requisitionNo search; forward-only guard rejects re-approve | ✅ PASS |

### Live-stack E2E (2026-08-20, MIG-023 sub-slice linen, Podman stack)
- linen-service container (`docker.io/hims/linen-service:0.1.0-SNAPSHOT`, port 8085, dev profile) on `ihms-platform_default`: `/actuator/health` **UP**; Flyway applied V1 to dev PG (`ihms_linen` + history)
- Security/contract live: anonymous list → **401**; unmapped path → **404** NOT_FOUND envelope; unknown request → **404**
- Lifecycle live: raise (201, requisitionNo `RISL/LR/26/00001`, requestStatus 1, 2 detail lines qty 10/4) → approve (status 2) → approve again → **400** BEXA003 guard → handover LaundryA (status 3) → receive-main-store (status 4, line1 received 9 + damaged 1 + reason torn) → process (status 5) → dispatch (status 6, line1 dispatched 8 + damaged ACCUMULATED 1+1=2) → complete WardNurse (status 7, line1 final 8) → counts (TOTAL 1 / COMPLETED 1)
- Store visibility live: store 10 (source) → 1, store 20 (destination) → 1, store 30 → 0
- Search live: `RISL/LR` → 1 hit; `ZZZZ` → 0
- Soft delete live: status=false + requestStatus=0; list after delete → 1 active (the completed one)
- OpenAPI generated from running service: `openapi/linen-service.yaml`, 10 paths
- CI: `linen-service-build-test` job added to `.github/workflows/ci.yml` (6 jobs total)

### 2026-08-20 additions (MIG-023 sub-slice — Transport S17 transport-service extraction, IMP-DEC-035)
| Test | Level | Scope | Result |
|---|---|---|---|
| VehicleRequestServiceTest (16) | L1 | raise: patient auto-approve vs doctor pending; tripType 0=drop/1=pickup/3=deadbody flags; requestMode joined into staffInstrumentReq ("12"); invalid payload → BEXA003; allocate flips vehicle+driver isAvailable=false + doctor isCharged=true; allocate missing vehicle → 404; allocate cancelled request → BEXA003; approve stamps approvedBy + re-approve guard; vehicle-out inserts detail + flips isVehicleOut + re-out guard; vehicle-in completes + stamps detail endOdmReading/actualFare + releases vehicle+driver + not-out guard; cancel flips isCancelled + releases + re-cancel guard; counts TOTAL/EMERGENCY/NORMAL/SCHEDULED dictionary split; unknown request → 404 | ✅ PASS |
| FleetServiceTest (7) | L1 | createVehicle defaults isAvailable=true + audit stamp; addFuel defaults dateTime; addMaintenance flips vehicle isAvailable=false; approveMaintenance flips flag + re-approve guard; completeMaintenance stamps actualCost+maintenanceDate + releases vehicle + re-complete guard; unknown maintenance → 404 | ✅ PASS |
| VehicleRequestControllerTest (9) | L0 | default-deny 401 on raise; 201 request shape (requisitionBy + isApproved); dashboard counts map; allocate/vehicle-out/vehicle-in/cancel delegation; details list shape; unmapped path 404 NOT_FOUND (not 500) | ✅ PASS |
| FleetControllerTest (8) | L0 | default-deny 401 on createVehicle; 201 vehicle shape; drivers list; fuel 201; maintenance 201; approve/complete maintenance delegation; unmapped path 404 NOT_FOUND | ✅ PASS |
| TransportServiceIntegrationTest (7) | L2 | real PG16 (Testcontainers): Flyway V1 creates ihms_transport (8 tables); full lifecycle raise→allocate→vehicle-out→vehicle-in with vehicle+driver availability flips + detail stamping + counts; counts dictionary split (Emergency seeded → EMERGENCY 1 / NORMAL 0); cancel releases vehicle+driver; maintenance lifecycle flips vehicle unavailable→approve→complete releases; forward-only guard rejects vehicle-in when not out; list day-window + requisitionBy search + mobileNo filter | ✅ PASS |

### Live-stack E2E (2026-08-20, MIG-023 sub-slice transport, Podman stack)
- transport-service container (`docker.io/hims/transport-service:0.1.0-SNAPSHOT`, port 8086, dev profile) on `ihms-platform_default`: `/actuator/health` **UP**; Flyway applied V1 to dev PG (`ihms_transport` 8 tables + history)
- Security/contract live: anonymous list → **401**; unmapped path → **404** NOT_FOUND envelope; unknown maintenance → **404**
- Lifecycle live: raise (201, isApproved=true, isDropTrip=true, staffInstrumentReq=12) → allocate vehicle+driver (isCharged=false patient) → vehicle+driver isAvailable **false** → vehicle-out (isVehicleOut=true, detail startOdmReading 1000) → vehicle-in (completed=true, detail endOdmReading 1050 + actualFare 500) → vehicle+driver isAvailable **true** → vehicle-in again → **400** BEXA003 guard
- Cancel live: allocate → isAvailable false → cancel (isCancelled=true) → isAvailable **true**
- Maintenance live: raise (vehicle isAvailable **false**) → approve (isApproved=true) → complete (isCompleted=true, actualCost 1800, vehicle isAvailable **true**) → re-complete → **400** BEXA003 guard
- Fuel live: add (201, dateTime defaulted) → list 1 row
- Dictionaries live: request-type Emergency + ambulance-type ALS created (201), list returns them
- Counts live: TOTAL 2 / EMERGENCY 0 / NORMAL 0 / SCHEDULED 0 (no dictionary split seeded in dev)
- OpenAPI generated from running service: `openapi/transport-service.yaml`, 21 paths
- CI: `transport-service-build-test` job added to `.github/workflows/ci.yml` (7 jobs total)

### 2026-08-21 additions (MIG-023 sub-slice — Diet S18 diet-service extraction, IMP-DEC-036)
| Test | Level | Scope | Result |
|---|---|---|---|
| DietOrderServiceTest (20) | L1 | raise: serviceId required → BERA035; orderNumber {%05d} from diet_order_number_seq; today-menu pre-check; sign flips orderStatus 1→2; replace: new order for same patient/admission marks old active order status 3; update-status; filter by admission; latest active order; food-items order-for-today (status starts 2, calories stamped, itemIds joined); advance 2→3 (guard at 6); uncheck →2; action-status HOLD/CANCEL/REJECT/RELEASE + reason stamping; remove item keeps remaining itemIds; kitchen list status>=3; unknown order/food → 404 | ✅ PASS |
| DietMasterServiceTest (12) | L1 | diet item/session/plan master create+read+list; plan details add/list; today-menu upsert (same dietType+session+item updates, else insert) + remove + list; invalid payload → BEXA003; unknown id → 404 | ✅ PASS |
| DietOrderControllerTest (8) | L0 | default-deny 401 on raise; 201 order shape (orderNumber + orderStatus); sign delegation; food-items 201 shape; advance/uncheck/action-status delegation; kitchen list shape; unmapped path 404 NOT_FOUND (not 500) | ✅ PASS |
| DietMasterControllerTest (6) | L0 | default-deny 401 on create item; 201 item/session/plan shapes; plan details delegation; today-menu upsert delegation; unmapped path 404 NOT_FOUND | ✅ PASS |
| DietServiceIntegrationTest (7) | L2 | real PG16 (Testcontainers): Flyway V1 creates ihms_diet (7 tables + 8 sequences); masters CRUD round-trip; diet plan master + details (bytea plan_information); raise→sign→food-items→advance→uncheck→hold full lifecycle on committed REQUIRES_NEW tx; replace marks old order status 3; remove item; kitchen list; sequence reset + TRUNCATE isolation | ✅ PASS |

### Live-stack E2E (2026-08-21, MIG-023 sub-slice diet, Podman stack)
- diet-service container (`docker.io/hims/diet-service:0.1.0-SNAPSHOT`, port 8087, dev profile) on `ihms-platform_default`: `/actuator/health` **UP**; Flyway applied V1 to dev PG (`ihms_diet` 7 tables + history)
- Security/contract live: anonymous list → **401**; missing serviceId on raise → **400 BERA035**
- Masters live: session (Breakfast) + item (Rice) + today-menu (dietType 5) created (201)
- Order lifecycle live: raise (201, orderNumber 00001, orderStatus 1) → sign (orderStatus 2) → food-items (status 2, calories 500) → advance (3) → uncheck (2) → hold (HOLD + reason) → remove item 11 (keeps "10") → kitchen list (status>=3, 1 row)
- Replace live: second order for same patient/admission (orderNumber 00002, status 2) → first order now status **3** (replaced)
- Persistence live: after container restart, admission-1 orders still present (00002 active, 00001 replaced); fresh patient 500 path raise→food→advance green
- OpenAPI generated from running service: `openapi/diet-service.yaml`, 22 paths
- CI: `diet-service-build-test` job added to `.github/workflows/ci.yml` (8 jobs total)

### 2026-08-20 additions (MIG-023 sub-slice — Bio-medical waste S19 waste-service extraction, IMP-DEC-033)
| Test | Level | Scope | Result |
|---|---|---|---|
| BioMedicalWasteServiceTest (13) | L1 | requisition number rule `{unitPrefix}/BW/yy/%05d` + default prefix; bag expansion (qty=3→3 bags, qty=0 skipped); 5-state machine guards (approve PENDING→ACCEPTED, wrong status → BEXA003, handover requires handedOverTo, handover/receive/process stamp bags); soft delete status=false+requestStatus=0; updateBagStatus advances bag+1 + re-derives min + appends handedOverTo; barcode validation blank → BEXA003; unknown request → 404 | ✅ PASS |
| BioMedicalWasteControllerTest (9) | L0 | default-deny 401 on raise; 201 request shape (requisitionNo + requestStatus 1); dashboard counts map; approve/handover delegation; bags list shape; barcode weight shape; soft delete shape; unmapped path 404 NOT_FOUND (not 500) | ✅ PASS |
| WasteServiceIntegrationTest (6) | L2 | real PG16 (Testcontainers): Flyway V1 creates ihms_waste (4 tables + 4 sequences); bag master create + list (unit-scoped, sequence ids); full lifecycle raise→approve→handover→receive(per-bag weight)→process with bag expansion + unique 13-char barcodes + firm stamping + counts; barcode counter increments across requests (same yyyyMM prefix, +1); soft delete excludes from list; numeric search matches barcode / non-numeric matches requisitionNo | ✅ PASS |

### Live-stack E2E (2026-08-20, MIG-023 sub-slice BMW, Podman stack)
- waste-service container (`docker.io/hims/waste-service:0.1.0-SNAPSHOT`, port 8084, dev profile) on `ihms-platform_default`: `/actuator/health` **UP**; Flyway applied V1 to dev PG (`ihms_waste` + history)
- Security/contract live: anonymous list → **401**; unmapped path → **404** NOT_FOUND envelope; unknown request → **404**
- Lifecycle live: bag master create (201, unit-scoped) → raise (201, requisitionNo `RISL/BW/26/00001`, requestStatus 1, qty=2 expanded to 2 bags with barcodes `2026080000001`/`2026080000002`, qty=0 skipped) → barcode validate (true) → approve (status 2) → approve again → **400** BEXA003 guard → handover EcoFirm (status 3) → receive StoreKeeper + bag-1 weight 2.5 (status 4) → weight-by-barcode (2.500) → process EcoFirm/John (status 5, all bags COMPLETED + firm stamped) → counts (TOTAL 1 / COMPLETED 1)
- Search live: numeric search by barcode → 1 hit; non-numeric `RISL/BW` → 1 hit
- Soft delete live: status=false + requestStatus=0; list after delete → empty
- OpenAPI generated from running service: `openapi/waste-service.yaml`, 13 paths
- CI: `waste-service-build-test` job added to `.github/workflows/ci.yml` (5 jobs total)

### 2026-08-20 additions (MIG-011 follow-up — subunit + geo_* service coverage, IMP-DEC-030)
| Test | Level | Scope | Result |
|---|---|---|---|
| Mig011GeoSubunitIntegrationTest (4) | L2 | real PG16: subunit unit-scoped CRUD round-trip (seq id 5001, wrong-unit miss, nonNull update, unitId required on list); geo hierarchy create country→state→district→city→area with unit_id NULL (D-6 global scope, ids 6001/7001/8001/9001/10001); geo list WITHOUT unitId + parent-FK filters bound as Long (type-aware coercion); default search field = name for geo; non-numeric FK filter → BEXA003; geo update/remove without unitId | ✅ PASS |

### 2026-08-20 additions (MIG-012b FEFO batch availability — IMP-DEC-029, UNKNOWN-012 resolved)
| Test | Level | Scope | Result |
|---|---|---|---|
| StockControllerTest +3 (8 total) | L1 | available-batches 200 FEFO view shape, missing params → 400 BEXA003 (new param handlers, not 500 catch-all), default-deny 401 | ✅ PASS |
| ItemServiceIntegrationTest +2 (6 total) | L2 | real PG16: FEFO ordering NEAR→FAR→NOEXP (null expiry last) + expired/freeze-recalled/zero-stock exclusion; param validation + unknown item 404 | ✅ PASS |

### Live-stack E2E (2026-08-20, MIG-012b FEFO, Podman stack)
- `GET /api/v1/stock/available-batches` live: 5 batches stocked (NEAR 2027 / FAR 2028 / NOEXP null / EXP 2020 / RECALL freeze-recalled) → returns exactly `[NEAR, FAR, NOEXP]` in FEFO order; expired + recalled excluded
- Zero-stock exclusion live: issue −100 from NEAR → re-query returns `[FAR, NOEXP]`
- Security/contract live: anonymous → 401; missing params → 400 BEXA003 envelope; unknown store → 404 NOT_FOUND
- OpenAPI regenerated from running service: 10 paths (new `/api/v1/stock/available-batches`)

### 2026-08-20 additions (MIG-012 item-service extraction — IMP-DEC-026/027)
| Test | Level | Scope | Result |
|---|---|---|---|
| StockServiceTest (5) | L0 | single-writer movement math (add/subtract), balance lock-or-create, version increment, ledger prev/new, missing-item 404 | ✅ PASS |
| StockControllerTest (5) | L1 | movement/balances/ledger contract, default-deny 401, BEXA003 invalid qty, unmapped path 404 (not 500) | ✅ PASS |
| ItemServiceIntegrationTest (4) | L2 | real PG16 (Testcontainers): Flyway V1 schema + sequences + outbox table; item/store/batch masters round-trip (sequence ids, audit stamp); single-writer stock flow (balance reuse, ledger ordering, 3 outbox events same-tx); rollback-on-missing-item (zero surviving rows) | ✅ PASS |

### Live-stack E2E (2026-08-20, Podman compose stack, real PG16+Kafka)
- item-service container (`docker.io/hims/item-service:0.1.0-SNAPSHOT`) on `ihms-platform_default` network: `/actuator/health` **UP**; anonymous `/api/v1/**` → **401**; unmapped path → **404** NOT_FOUND envelope; runs non-root
- Flyway: `ihms_item.flyway_schema_history` created by item-service V1 (15 tables incl. outbox); stale untracked dev schema (old V6 DDL, 0 rows) dropped and recreated under Flyway management
- Masters write path live: item + store created (201, server-owned sequence ids, audit block stamped)
- Stock single-writer flow live: opening +500 → issue −120 → GRN +50; balance 430, version 3, ledger prev/new chain correct
- Outbox → Kafka live: 5 events PUBLISHED (2 MasterDataChanged → `ihms.item.master-data-changed`, 3 InventoryUpdated → `ihms.item.stock-movement` per E-09 routing IMP-DEC-027); envelope verified on-topic

### 2026-08-17 additions (AGT-10 red-team findings → fixes, IMP-DEC-014)
| Test | Level | Scope | Result |
|---|---|---|---|
| MasterReadUseCaseTest legacyOffsetFormula (11 param invocations) | L0 | (page/count)*count parity vs GenericQuery.paginate | ✅ PASS |
| JpaMasterReadRepositoryIntegrationTest.useCase_legacyOffsetWindowSemantics | L2 | real PG16 row windows for page 2/6/10 · count 3/3/4 | ✅ PASS |
| JpaMasterReadRepositoryIntegrationTest.findScoped_addeddatetimeRangeAndsWithOrGroup | L2 | legacy precedence: range AND (OR-group); Timestamp binding fix | ✅ PASS |
| JpaMasterReadRepositoryIntegrationTest.unitmasterParityColumns_exist_afterV3 | L2 | ISCONFIRMATORY/ISADMINISTRATIVE/ISOFFLINE after V3 | ✅ PASS |

### Live-stack E2E (2026-08-17, Podman compose stack, real PG16+Kafka)
- `docker-compose up -d` (Podman compat socket): postgres/redis/kafka/solr/solr-zk **5/5 healthy** (images from docker.io, pinned)
- Containerized reference-service: `/actuator/health` + liveness + readiness **UP**; unauthenticated `/api/v1/masters/**` → **401**; X-Request-ID echoed (200); runs as **non-root** (`hims`)
- Outbox → Kafka round trip **live**: 2 events `MasterDataChanged` → topic `ihms.master.master-data-changed` → `PUBLISHED`; envelope verified (eventId/eventType/version/occurredAt/producerService/entityId/partitionKey/idempotencyKey/payload); retry path proven live (event published on attempt 2 after broker listener fix)
- `podman build` reference-service image: **OK** (multi-stage, 294 MB, non-root, healthcheck)

## Run log

| Date | Command | Result |
|---|---|---|
| 2026-08-14 | `mvn verify` (reference-service) | ✅ BUILD SUCCESS — 29/29 (incl. Testcontainers PG16 + Kafka, requires Docker daemon) |
| 2026-08-17 | `mvn verify` (reference-service, Podman compat socket) | ✅ BUILD SUCCESS — 31/31 — Docker→**Podman** engine switch (BLK-003 resolved; IMP-DEC-013) |
| 2026-08-17 | `mvn verify` after AGT-10 parity fixes (offset formula, datetime grouping, V3 columns) | ✅ BUILD SUCCESS — **45/45** (0 failures/errors; see inventory below) |
| 2026-08-17 | `mvn verify` after MIG-010 write path (dev basic-auth profile, write use case + controller + L2) | ✅ BUILD SUCCESS — **69/69** (0 failures/errors) |
| 2026-08-17 | `mvn verify` after AGT-10 W1 red-team must-fixes (F-1..F-19) | ✅ BUILD SUCCESS — **77/77** (injection regressions ×4, audit-forgery, required-fields, DLQ routing, rollback atomicity, dev-auth) |
| 2026-08-17 | `mvn verify` after MIG-011 clean masters (V5/V6 + 4 entities + registry) | ✅ BUILD SUCCESS — **83/83** |
| 2026-08-20 | `mvn verify` (item-service, first full run; Testcontainers PG16 via Podman, Ryuk disabled for rootless) | ✅ BUILD SUCCESS — **14/14** (fixes: outbox table in V1, FK seed in test, JSONB binding, IDENTITY id mapping, ledger ordering, outbox-count assertion) |
| 2026-08-20 | `mvn verify` (reference-service after V6 removal + ihms_item test assertion removed + 404 handler) | ✅ BUILD SUCCESS — **82/82** |
| 2026-08-20 | `mvn verify` (item-service after MIG-012b FEFO: available-batches endpoint + param 400 handlers + 5 new tests) | ✅ BUILD SUCCESS — **19/19** |
| 2026-08-20 | `mvn verify` (reference-service after MIG-011 follow-up: subunit + geo_* entities/registry, MasterScope, type-aware filter coercion, 4 new L2 tests) | ✅ BUILD SUCCESS — **86/86** |

### 2026-08-17 additions (MIG-011 clean target masters — IMP-DEC-025)
| Test | Level | Scope | Result |
|---|---|---|---|
| Mig011CleanMastersIntegrationTest (6) | L2 | real PG16: V5 14 tables + V6 ihms_item 14 tables + sequences apply; doctor CRUD round-trip (seq id, search, nonNull update, delete); doctor-type/specialization create; facility-location hierarchy; required fields; pg_trgm extension in public | ✅ PASS |

| 2026-08-17 | `npm test` (frontend) after AGT-03 fixes (F-6/F-8/F-18) | ✅ 25/25 (client 9, zodSchema 7, MasterList 3, MasterEdit 3, MasterForm 3) + `npm run build` PASS |

### AGT-10 W1 red-team resolution (2026-08-17)
All must-fix items resolved and verified: F-1 documented (caller-supplied scope until MIG-004) · F-2 injection hardening + 4 regression tests · F-3 client-id rejected · F-4 audit columns server-owned (create+update tests) · F-5 delete semantics recorded (IMP-DEC-022) · F-6 frontend scoped edit fixed (unitId on GET/PUT) · F-7 locked-until-MIG-004 documented · F-8 dev creds tree-shaken from prod bundle · F-9 UNIQUE outbox.event_id (V4) · F-10 DLQ topic routing + test · F-11 rollback-atomicity test (REQUIRES_NEW) · F-12 registry requiredFields · F-13 null-skip test de-vacuolized · F-14 update-miss 404/BEXA031 · F-16 nextval allowlist · F-17 correlation cap 128 · F-19 change hygiene (committed with tests)


### 2026-08-17 additions (MIG-010 Wave 1 write path, IMP-DEC-020)
| Test | Level | Scope | Result |
|---|---|---|---|
| MasterWriteUseCaseTest (13) | L0 | crudadd/crudupdate/crudremove parity: sequence ids, BEXA003/BEXA031, nonNull/updFlds, unit-scope hardening, outbox op=create/update/remove | ✅ PASS |
| MasterWriteControllerTest (5) | L1 | POST 201, PUT 200, DELETE 200, default-deny 401, unknown entity 400 | ✅ PASS |
| DevBasicAuthTest (3) | L1 | dev-profile basic auth (devops) OK, wrong password 401, business endpoints still default-deny | ✅ PASS |
| JpaMasterReadRepositoryIntegrationTest write-path (3) | L2 | real PG16: create via sequence → read-back + outbox row; update preserves id/unitId + op=update row; remove → gone + op=remove row | ✅ PASS |


## Fixed during S0 (regression notes)
- Hibernate identifier strategy: legacy requires verbatim quoted names (`"M_UNITMASTER"`, `"UnitID"`, `"apiToken"`) → `PhysicalNamingStrategyStandardImpl` + `globally_quoted_identifiers` (records IMP-DEC-007 verification)
- Kafka: custom ProducerFactory overrides Boot auto-config → `@ServiceConnection` bootstrap injection broke; removed custom config, properties moved to application.yml (IMP-DEC-012)
- JSONB payload binding via `@JdbcTypeCode(SqlTypes.JSON)`

## Pending (blocked)
- L1 contract replay fixtures from running legacy: ⛔ BLOCKED (MIG-002, BLK-002)
- Solr parity drills: W5 (MIG-058)
- Security regression suite (authN real): W0 exit, MIG-004
- L3 E2E (gateway + React): blocked on MIG-005 gateway slice
- L4 performance baseline vs legacy: not started (W1+)
