# iHMS / OHUM PAS NG — Migration Wave Plan (Evidence-Based)

**Status: PRELIMINARY — synthesized from completed discovery (agents 01, 04, 05, 06, 07, 09, 10 + reconciliation). No new exploration performed.**
**Monolith evidence base:** `D:\ihms` — Spring Boot 2.7.5 / CXF JAX-RS (~3,060 endpoints, 132 service classes), EclipseLink/Oracle (~843 tables, 719 PL/SQL artifacts, 30 live-called), Solr Cloud (6 collections, 5 live), Redis (read-through cache), FAUG server-rendered frontend (40 modules, ~290 routes).
**Date:** 2026-08-14.

---

## 1. Executive summary

The 29 candidate bounded contexts (agent-10 findings §6) fall into a clear dependency ordering driven by three structural facts of the monolith:

1. **Master data is a god-node** — `MasterCachedServiceImpl` (514 graph edges, 264 endpoints, 12,710 LOC) owns ~375 Redis cache keys consumed by ~30 services; 609 master entities back the generic CRUD surface (1,109 frontend URLs route through `CrudOperationsFilter` → `GenericServiceImpl`). Nothing can move before the master/cache contract is stabilized.
2. **One mega-transaction** couples Order → Bill → Lab order → Rad order → DoctorShare → EMR → HL7 (`AddServiceOrderServiceHelper.java:341-516`, `AddServiceOrderServiceImpl.java:2206-2399`). Ordering and Billing cannot be separated from each other; Lab/Rad **order booking** cannot move before them.
3. **Proven async patterns already exist** — WhatsApp queue (`T_WHATSAPPMESSAGEQUEUE`, 15-min poller `ApplicationJobs.java:1611`), ABDM queue (`T_ABDMREQUESTQUEUE`, 5-s poller `:1661-1688`), HL7 outbox (`T_OUTBOUNDHIS`) — the strongest evidence that event-driven decomposition is viable.

The wave plan below follows the recommended pattern: **foundation → reference/masters → operational → clinical → financial → highly-integrated → retirement**. It deliberately defers the revenue spine (Wave 4) until the platform and clinical flows are proven, and pairs **Ordering + Billing + Payments** in the same wave because they are one ACID unit today.

---

## 2. Scoring matrix — 29 services / 8 dimensions (1 = low, 5 = high)

Scores are evidence-driven (citations consolidated per module in the Evidence column; per-dimension detail in `MIGRATION-COMPLEXITY.md`).

Legend: **Crit**=business criticality · **Tech**=technical complexity · **Coup**=coupling · **DB**=DB complexity · **Int**=integration complexity · **UserV**=user volume · **DTol**=downtime tolerance (5 = cannot tolerate downtime) · **Risk**=regulatory/clinical risk.

| # | Service (candidate) | Crit | Tech | Coup | DB | Int | UserV | DTol | Risk | Wave | Evidence (one line) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Patient Identity & Registration | 5 | 5 | 5 | 4 | 4 | 5 | 4 | 4 | 3 | `T_REGISTRATION*` has **no entity mapping** (reconciliation §4); Solr index realtime-in-txn (`PatientServiceImpl.java:5610`); MRN rules (`BR-PAT-001/002`); read by every module |
| 2 | Appointment & Encounter (OPD) | 5 | 4 | 5 | 4 | 3 | 5 | 4 | 3 | 3 | `T_ENCOUNTER` = highest-shared table (52 refs, agent-05 §7); visit txn bundles queue+MLC+FPV+HL7+Solr+ABDM (`EmergencyAndWalkinVisitServiceImpl.java:1379-1937`); VisitStatus 11/12 split-brain (agent-06 §C.1) |
| 3 | ADT & Bed | 5 | 4 | 4 | 4 | 3 | 4 | 3 | 4 | 3 | `T_ADMISSION` shared tenant record (agent-10 §3); bed CAS allocation (`BR-IPD-002`); discharge/death/MLC rules (`BR-DIS-001..003`); HL7 A01 in txn (`HelperServiceImpl.java:307`) |
| 4 | Ordering & Prescription (Clinical) | 5 | 5 | 5 | 5 | 4 | 4 | 4 | 5 | 4 | **Mega-txn owner** — 1 ACID unit across bill+lab+rad+share+EMR+HL7 (`AddServiceOrderServiceHelper.java:341-516`); charge lifecycle + concession asymmetry (`BR-BILL-001..005`, `BR-PATH-001`) |
| 5 | Billing & Settlement | 5 | 5 | 5 | 5 | 4 | 5 | 4 | 5 | 4 | `T_BILL` = settlement spine; bill-no continuity; tax/concession rules (`BR-BILL-004/005`); GST retrospective since 2017-07-01 (`BaseServiceImpl.java:521-533`); 77 schemas/93 templates frontend |
| 6 | Payments & Refunds | 5 | 4 | 4 | 4 | 3 | 4 | 4 | 5 | 4 | Payment proc chain updates bill rows (`AddPaymentServiceHelper.java:291-348,463`); advance FIFO + refund restore (`BR-BILL-007/008`); money movement = highest financial risk |
| 7 | Lab (Pathology) | 5 | 4 | 4 | 4 | 4 | 4 | 3 | 4 | 3 | `PathoOrderServiceImpl` 14,629 LOC god class (agent-04 §9); booking created **inside** mega-txn (`:473-516`); WhatsApp result send (`PathoOrderServiceHelper.java:1130`); result release double-verify |
| 8 | Radiology | 4 | 3 | 4 | 3 | 2 | 3 | 3 | 3 | 3 | Booking inside mega-txn (`AddServiceOrderServiceHelper.java:579-599`); otherwise small surface (9 schemas/6 templates) |
| 9 | Pharmacy & Dispensing | 5 | 4 | 5 | 5 | 3 | 4 | 3 | 4 | 5 | **Dual-write stock hotspot** (`PharmacyServiceImpl.java:2140` CIMS_ADDITEMSTOCK vs Inventory ownership); VisitStatus 12 (`BR-PHAR-001`); prescription closure (`BR-PHAR-002`); dispensing safety |
| 10 | Inventory & Procurement | 4 | 5 | 5 | 4 | 4 | 3 | 3 | 4 | 5 | `InventoryServiceImpl` 12,070 LOC god class (235 endpoints); `T_ITEMSTOCK` highest-contention unit-of-work; eAushadhi/RMSC + Kaysons; 2-level approvals; 70 schemas/196 templates |
| 11 | OT & Surgery | 4 | 4 | 3 | 3 | 3 | 2 | 3 | 5 | 3 | Scheduling preconditions + pre-billing (`BR-OT-001/002`); Solr patient search (`OTSchedulerServiceImpl.java:4879`); surgical/anesthesia safety |
| 12 | Nursing & Inpatient Care | 5 | 5 | 3 | 3 | 2 | 4 | 4 | 4 | 3 | `NursingServiceImpl` 17,350 LOC / 300 endpoints (agent-04 §9); 371 templates — largest frontend surface; ROWNUM queries (`:7124`); charting/partograph; legal documentation weight |
| 13 | Blood Bank | 4 | 3 | 3 | 3 | 3 | 2 | 2 | 5 | 3 | Requisition/bag state machines + jobs (`BR-BLD-001..005`); eRaktkosh jobs (`ApplicationJobs.java:1205,1383`); transfusion safety = life-critical |
| 14 | Diet | 2 | 2 | 2 | 2 | 1 | 2 | 2 | 2 | 2 | Smallest modules — self-contained tables (`T_DIETORDER`); `SYSDATE/to_char` queries only (`DietServiceImpl.java:154`) |
| 15 | CSSD | 3 | 2 | 3 | 2 | 2 | 2 | 2 | 3 | 3 | Consumes `ProcedureScheduled` + `InventoryUpdated` events; otherwise self-contained kit/sterilization tables |
| 16 | Linen & Laundry | 2 | 1 | 2 | 2 | 1 | 2 | 2 | 1 | 2 | `T_LINENREQUEST(+DETAILS)` self-contained (agent-10 §1.2 #15); single thin service |
| 17 | Transport | 2 | 2 | 2 | 2 | 1 | 2 | 2 | 2 | 2 | `T_TRANSPORTREQUEST/T_VEHICLEMAINTENANCE/M_AMBULANCEMASTER` self-contained; reads admission/patient |
| 18 | Equipment Management | 3 | 2 | 2 | 3 | 2 | 1 | 2 | 2 | 2 | `T_EQUIPMENT/T_EQPCOMPLAINT/T_EQPPM*` + CLOB docs; spares from Inventory; AMC to Finance |
| 19 | Bio-Medical Waste | 2 | 2 | 2 | 2 | 1 | 1 | 2 | 2 | 2 | `T_BIOMEDICALWASTE(+DETAILS)` self-contained; reads ward |
| 20 | Grievance & Feedback | 2 | 2 | 2 | 2 | 1 | 1 | 2 | 1 | 2 | `T_PATIENTCOMPLAINTS/T_FEEDBACKTEMPLATE`; reads patient; low clinical/financial risk |
| 21 | Reference Master Data | 5 | 5 | 5 | 4 | 3 | 4 | 4 | 2 | 1 | **God-node 514 edges** (agent-10 §1.2 #23); ~375 cache keys consumed by ~30 services; 300 config-driven master screens |
| 22 | Security & IAM | 5 | 4 | 5 | 3 | 4 | 5 | 5 | 5 | 0 | Auth woven into `ServiceLayerAspect` per request; session tokens in cache (`session.timeout`); LDAP + Rajasthan SSO; token compromise = total breach |
| 23 | RCM & Claims | 3 | 3 | 3 | 2 | 4 | 1 | 2 | 3 | 4 | NHCX/HCX JWE flows (`NHCXServiceImpl.java:45-124`); reads bill/admission; consumes `PaymentReceived` |
| 24 | RMRS & PMRD (Treasury) | 3 | 2 | 3 | 2 | 2 | 1 | 2 | 4 | 5 | Treasury collections read payment data (`RMRSServiceImpl.java:1361-1366` ROWNUM=1); govt-accountability regulatory |
| 25 | Interop & Notifications | 4 | 5 | 4 | 4 | 5 | 3 | 2 | 3 | 5 | 28 external integrations concentrate here (agent-07 §3); `HL7Helper` 118 SELECTs/19 NEXTVAL; ABDM 50 endpoints + 8 jobs; 4 queue tables |
| 26 | Reporting & Analytics | 4 | 4 | 4 | 3 | 3 | 2 | 2 | 3 | 5 | Read-model over everything; Jasper/Velocity; **report dispatcher = hidden routing layer** (reconciliation §1); GSTR/NHM/RGHS regulatory SLAs |
| 27 | Search & Index (Solr) | 4 | 4 | 5 | 1 | 3 | 5 | 4 | 2 | 1 | Solr non-negotiable (agent-07 §1); ~15 search methods embedded in registration/admission/ABDM flows; 3-day encounter retention gap |
| 28 | Govt Programs (EJS/Aasmaan/Chiranjeevi) | 3 | 3 | 2 | 3 | 3 | 1 | 2 | 3 | 5 | 61 EJS + 35 aasmaan entities; aasmaan tables unmapped (convention naming — verify); incentives → finance |
| 29 | Queue & Kiosk | 3 | 3 | 3 | 2 | 2 | 4 | 4 | 2 | 2 | QMS queue state lives in Redis; `QueueManagmentServiceImpl.java:1477-1759`; user-facing token counters |

**Frontend-module mapping (40 modules → 29 services):** reconciliation §2 confirms agent-02's 40 `fjs-config_*.json` modules map onto agent-10's 29 service candidates; masters → 21, billing/payment/advance/credit-note → 5+6, inventory/allocations → 10, op/queue/kiosk → 2+29, ip → 3, nursing/careplan/charting/shifthandover/worklist/tiuxmls → 12, pathology → 7, radiology → 8, pharmacy/chemotherapy/bcma → 9, bloodBank → 13, ot → 11, cssd → 15, diet → 14, linenlaundry → 16, transport → 17, equipments → 18, biomedicalwaste → 19, grievance/enquiry → 20, rmrs → 24, rcm/nhcx → 23, pmrd/fileManagement/commons → 25, reports/biReport → 26, ejs/aasmaan → 28.

---

## 3. Wave plan

### Wave 0 — Foundation (platform, TO-BE components + IAM extraction)

**Items:** API gateway (TO-BE) · IAM service (22) · master-data cache contract governance (design+freeze, service extraction in Wave 1) · event backbone: Kafka + outbox relay (TO-BE) · CDC Oracle→PG (Debezium/LogMiner, TO-BE) · observability platform (structured logs, tracing, metrics, alerting — see `OBSERVABILITY.md`) · Solr index-ownership decision · CI/CD + containerized deployment · docit.ctrl watchdog replacement.

**Why first (evidence):**
- **Master contract**: `MasterCachedServiceImpl` (514 edges) + ~375 `@CrudCached` keys are consumed by ~30 services; the cache key names, TTLs and invalidation paths (`CrudOperationsFilter.java:139-330`) are an implicit API — freezing and documenting them is a precondition for any extraction. Also `Redis` vs `MemoryCacheImpl` ambiguity (agent-07 §2.2: checked-in bean is MemoryCacheImpl; Redis impls commented out — multi-node prod = per-node divergent master data) must be resolved before services split.
- **IAM**: every endpoint is authorized per-request in `ServiceLayerAspect.handleWorkflow` via session-cache token lookup; login is LDAP + Rajasthan SSO (`AuthenticationServiceImpl.java:66-293`, SSOAuthFilter). The token format (opaque bearer in cache, `session.timeout` TTL, `UNQ_SESS_USER_` keys) is the shared security contract; new services cannot authenticate independently until token-introspection is fixed.
- **Event backbone**: the legacy already contains three DB-polled pipelines (WhatsApp/ABDM/HL7) — these are proven patterns; the relay that lets old and new sides exchange events must exist before Wave 1 ("Interop runs as integration broker early", agent-10 §5).
- **Solr ownership**: patient/encounter collections are written realtime-in-txn by 6+ services plus 4 batch drainers (agent-07 §1.3); the Search service (27) must own the ingestion contract before Patient (1) moves — decided here, implemented in Wave 1.

**Exit criteria:** gateway routes 100% of traffic to the unchanged legacy with zero behavioral diff (contract parity harness green) · login→gateway→legacy token flow works end-to-end with the same opaque bearer · master cache contract frozen + documented (key catalog, TTL, invalidation) · Kafka cluster + outbox + CDC running for 3 pilot tables (idempotent, tested failover) · structured logs carry `meth/uri/corr/rip` semantics end-to-end; correlation-id spans gateway→legacy · health/readiness + alerting live; `docit.ctrl` watchdog replaced by orchestration liveness for the new platform (legacy watchdog retained) · IAM service (22) extracted: `T_USERDETAILS/T_USERROLES/T_USERUNITDETAILS/T_USERSTOREDETAILS/T_USERWARDDETAILS/M_MENUMASTER` owned in PG, roles/grants served by API, `UserCreated` event live · CI/CD for new services + feature-flag/release train established.

**Dependencies:** none (platform-only). Uses `09-react-migration-design.md` §12 sequencing: core platform first.

---

### Wave 1 — Reference & Master Data (low-risk, platform-critical)

**Items:** Reference Master Data (21) · Search & Index pipeline (27) · masters frontend (300 generic master screens on React, config-driven from `mastersConfigration.json`).

**Why here (evidence):** the Reference service is the highest-coupling node after IAM — every wave below depends on tariff/rate/service/item lookups (`BillServiceImpl.java:104` rate lookup, `PatientBillEstimationServiceImpl`, RCM eligibility). It is moved *second* (not Wave 0) because extraction of 609 entities + cache ownership is large (DB complexity 4) and benefits from a proven platform. Search (27) rides with it because 3 of 6 Solr collections are **master-owned** (item, icd10cm, healthfactor — flag-drained by `syncItemsToSolrJob`/`syncIcd10cmToSolrJob`/`syncHealthFactorToSolrJob`, 5-s fixedDelay); owning the master→Solr contract and the patient/encounter event ingestion in the same service avoids split ownership.

**Exit criteria:** `M_*` masters live in PG schema `ihms_master` (1:1 quoted tables, per-table sequences preserved, agent-05 §8) · all ~375 cache keys served from the new service; `MasterDataChanged/TariffChanged/PackageChanged` events live; cache invalidation end-to-end verified · generic CRUD parity for the 1,109 CRUD-filter URLs (`CrudOperationsFilter` semantics: `crudadd/crudupdate/crudget/crudgetAll`, `MBase.validate`, REQUIRES_NEW batch) · master screens (React) live against new APIs · Search service consumes master + patient/encounter events; edismax boost semantics preserved (bq weights load-bearing, agent-07 §1.7); 3-day encounter retention decision made (archive collection vs delete) · full Solr reindex + reconciliation script live · rollback rehearsal passed (see STRANGLER §6).

**Dependencies:** Wave 0 (cache contract, gateway, events, CDC).

---

### Wave 2 — Operational & Low-Risk (quick wins, muscle-building)

**Items:** Queue & Kiosk (29) · Linen (16) · Diet (14) · Transport (17) · Equipment (18) · Bio-Medical Waste (19) · Grievance & Feedback (20).

**Why here (evidence):** lowest coupling (agent-10 §8: Low-Med for this cluster), self-contained tables (no shared-tenant hotspots — `T_LINENREQUEST`, `T_BIOMEDICALWASTE`, `T_DIETORDER`, `T_TRANSPORTREQUEST`, `T_EQUIPMENT`, `T_PATIENTQUEUE` are read by few), no money movement, minimal integration (Diet/BMW/Linen = 1), smallest procs footprint (`SYSDATE/to_char` only — `DietServiceImpl.java:154`). These modules build the strangler muscle (CDC cutover, gateway flip, React screen migration, dual-read reconciliation) at negligible clinical/financial risk before the hard modules. **Caveat:** Queue & Kiosk is user-facing (token counters, high volume) — QMS queue state lives in Redis (`QMSUtil:28-126`, `queueManagment.*`); during transition all queue traffic must stay on one side (no split QMS state), and Redis queue keys are shared infra (not per-service tables).

**Exit criteria:** 7 modules live on independent PG schemas · React screens served through gateway to new services · per-module gateway flip + rollback rehearsed twice · reconciliation reports (row-count + checksum vs Oracle) green for 5 business days · WhatsApp/ABDM/HL7 pollers unaffected (no dependency on these tables) · token/queue continuity proven for kiosk counters.

**Dependencies:** Wave 0 + 1 (masters/cache, gateway, CDC, events). Reads of patient/admission still cross to legacy via API/dual-read.

---

### Wave 3 — Clinical (admission front → care delivery → clinical support)

**Items (three sub-drops within the wave):**
- **3a Admission Front:** Patient Identity (1) → Appointment & Encounter (2) → ADT & Bed (3)
- **3b Care Delivery:** Lab result/workflow side (7) · Radiology result/workflow side (8) · Nursing (12)
- **3c Clinical Support:** OT & Surgery (11) · CSSD (15) · Blood Bank (13)

**Why here (evidence):** patient identity/encounter/admission are the shared tenant records every downstream module keys off (agent-10 §3) — they must move before nursing/OT/blood-bank/ordering, and their event streams (`PatientRegistered`, `EncounterCreated`, `PatientAdmitted`) are prerequisites for the Solr pipeline and for Wave 4 billing (sponsor-by-admission `AddServiceOrderServiceHelper.java:1213`). They cannot move earlier than Wave 2 because Patient carries the **unmapped `T_REGISTRATION*` schema** (reconciliation §4 — critical data risk), realtime-in-txn Solr indexing, ABDM linkage and MRN rules — the data-migration rigor proven in Wave 2 applies first. Lab/Rad (3b) migrate their **result/worklist/report side** (independent tables `T_PATHOPATIENTPARAMETERDETAILS`, machine interface, WhatsApp result send) while **order booking stays in the monolith** — booking is created inside the mega-txn (`AddServiceOrderServiceHelper.java:473-516, 579-599`) and cannot move until Wave 4. Nursing (12) is a huge frontend (371 templates) but technically self-contained (reads encounter/admission only). OT (3c) consumes `ProcedureScheduled` + patient search; CSSD consumes procedure + inventory events; Blood Bank is self-contained with its own state machines (`BR-BLD-001..005`).

**Exit criteria:** `PatientRegistered/EncounterCreated/PatientAdmitted/LabResultFinalized` streams live end-to-end (produce → outbox → Kafka → consumers incl. Solr + Interop relay) · MRNO/IPDNO continuity proven (sequences preserved, agent-05 §4) · duplicate-patient rules (`BR-PAT-002` BERA011/BEXA040) and MRN rules (`BR-PAT-001`) parity-tested · patient Solr search parity (Redis-mirror + fallback, agent-07 §1.6) · discharge/death handling parity (`BR-DIS-002`: `isdeath`, Solr death flag, encounter clear) · bed CAS allocation parity (`BR-IPD-002` `BEXB001`) · lab/rad result release + critical alerts + WhatsApp delivery live from new services · encounter/patient partitioning (volume question agent-10 §10.2) resolved and applied · Solr written **only** via events from new services (no direct `addBean`) · nursing coversheet/notes/vitals/plan-of-care live · OT schedule + pre-billing temp-charge parity (`BR-OT-002` — temp charge path still touches legacy billing, documented hand-off) · blood-bank requisition/cross-match/issue machines live · dual-read reconciliation green for `T_ENCOUNTER/T_ADMISSION/T_REGISTRATION*` during transition.

**Dependencies:** Waves 0–2; within-wave ordering is strict: 1 → 2 → 3 (encounter/admission IDs before OPD/IPD modules); Lab/Rad booking tables stay Oracle until Wave 4 (documented boundary).

---

### Wave 4 — Financial / Revenue Spine (the mega-transaction bet)

**Items:** Ordering & Prescription (4) + Billing & Settlement (5) + Payments & Refunds (6) + RCM & Claims (23).

**Why here (evidence):** this is the single hardest coupling in the system — one `@Transactional(rollbackOn=Exception.class)` spans bill header/lines, doctor share, lab order, rad order, concession, EMR write, package subscription and HL7 ORM (`AddServiceOrderServiceHelper.java:341-516`, `AddServiceOrderServiceImpl.java:2206-2399`). Agent-10's draft placed Revenue in Wave 2 ("highest criticality, core bet"); **this plan deliberately defers it to Wave 4** so that: (a) the platform, master contract, CDC and clinical event streams are battle-tested; (b) billing screens (77 schemas/93 templates) migrate against a proven React+gateway; (c) financial risk exposure is compressed into one well-prepared window instead of being the first wave. The cost of deferral is explicit: Lab/Rad **order booking**, pharmacy item-sale billing (`T_ITEMSALE.BillID`), OT pre-billing and all charge creation remain legacy until this wave — a documented, bounded dependency. Within the wave, ordering and billing are inseparable (saga replaces the mega-txn), payments follow (payment procs update bill rows — `AddPaymentServiceHelper.java:463`), RCM follows payments (`PaymentReceived` drives claim stage).

**Exit criteria:** mega-txn decomposed into choreographed saga (Order→Bill→Payment) with PG outbox → Kafka, compensation flows reusing existing cancellation logic (`CIMS_SETTLEDBILLCANCELLATION/CIMS_UNSETTLEDBILLCANCELLATION`, `BillServiceImpl.java:70-91`) · `InvoiceGenerated/PaymentReceived/LabOrderCreated/RadOrderCreated` events live · bill-number continuity (templates `BR-BILL-001`, sequence reseed) · tax (`BR-BILL-004`), concession (`BR-BILL-005` asymmetry — golden-master test), doctor share (`BR-BILL-002` CEILING rounding), advance FIFO (`BR-BILL-007`), refund/cancellation restore (`BR-BILL-008`) all parity-tested · settlement state machine (`BR-BILL-006`, `VisitStatus=11` checkout + Solr) exact · RCM eligibility/pre-auth/claims live on HCX · GST retrospective rule (2017-07-01) verified · no dual-write: bill tables single-writer.

**Dependencies:** Wave 3 (encounter/admission/patient events, sponsor-by-admission reads), Waves 0–1 (tariff/master contract, cache), Wave 0 (outbox/saga platform). **Lab/Rad booking cuts over in this wave** (ownership of `T_PATHORDERBOOKING` booking path + `CIMS_ADDPATHORDERBOOKING` conversion).

---

### Wave 5 — Highly-Integrated (pharmacy/inventory, interop, reports, govt)

**Items:** Pharmacy & Dispensing (9) + Inventory & Procurement (10) · Interop & Notifications (25) · Reporting & Analytics (26) · RMRS & PMRD (24) · Govt Programs (28).

**Why here (evidence):**
- **Pharmacy/Inventory** share the highest-contention unit-of-work — `T_ITEMSTOCK` (agent-10 §3; dual-write hotspot `PharmacyServiceImpl.java:2140`). The stock-ownership decision (single Inventory-owned stock API vs per-store split — agent-10 §6.3 open question) gates both; they move together under one inventory contract after `InventoryUpdated/DispensingCompleted` streams from Wave 4 are live (pharmacy bills via item sale → billing). Inventory is also a god class (12,070 LOC) and carries eAushadhi/Kaysons integration.
- **Interop** is deliberately last-despite-central: its queue tables (`T_WHATSAPPMESSAGEQUEUE/STATUS`, `T_ABDMREQUESTQUEUE`, `T_OUTBOUNDHIS`, `T_EHRJOBDETAILS`) keep serving the monolith pollers throughout; a relay in Wave 0 forwards events to it; full extraction only makes sense once every domain event it consumes (PatientRegistered … InvoiceGenerated, PaymentReceived, LabResultFinalized, PatientDischarged) is produced by new services — i.e., after Wave 4.
- **Reporting** is the read-model over everything (agent-10 §2.1 #26) — needs PG read-models from all domains; regulatory exports (GSTR/NHM/RGHS) have SLA constraints (agent-10 §7.3).
- **RMRS** consumes `PaymentReceived` (treasury receipts); **Govt Programs** read patient and consume finance events (incentives).

**Exit criteria:** stock single-writer API live; Pharmacy consumes it (dual-write eliminated, `PharmacyServiceImpl.java:2140` path retired) · `InventoryUpdated/PurchaseOrderPlaced/GRNPosted` streams live; auto-indent/reorder jobs on new services; eAushadhi/eRaktkosh/RGHS-SIMS/PCTS/NMC/EHR jobs migrated with cron parity · Interop on PG: queue tables migrated with **in-flight message continuity** (no lost WhatsApp/ABDM/HL7 messages across cutover); ABDM v2/v3 + HCX callback routing preserved; FHIR surface (dormant bean — decision: activate or retire) · Reporting read-models + Jasper/Velocity parity; report dispatcher emulated (`reports.js` Nashorn replaced) · regulatory report SLAs met · RMRS treasury reconciliation parity · Govt incentive runs parity (`ejs.asha.incentive.schedule`) · 222 unresolved URLs triaged (reconciliation §1) · Solr 3-day encounter retention gap closed (archive collection).

**Dependencies:** Wave 4 (billing/payment events, item-sale billing), Wave 3 (clinical events), Waves 0–1.

---

### Wave 6 — Monolith Retirement

**Items:** final cutover of remaining procs/routes · FAUG retirement · Oracle decommission · dead-code triage · sequence/ID final sync · rollback-switch removal.

**Evidence/scope:** only 30 of 719 PL/SQL artifacts are confirmed live-called (agent-05 §6, reconciliation §4) — the remaining ~689 are dead-code candidates to triage, not port · 222 unmatched frontend URLs resolved (port or declare dead) · `ext.js` backend-injection mystery resolved · `docit.ctrl` + legacy watchdog removed once orchestration liveness covers the monolith shard · MemoryCacheImpl/Redis ambiguity resolved · session-cache semantics unified · committed `logs/` (~500 MB error logs) purged · `T_REGISTRATION_Int`/temp tables retired.

**Exit criteria:** Oracle read-only for 1 month, then decommissioned · last procs retired · `IHMS.log` writers gone · rollback switch (STRANGLER §6) removed after 90-day observation · SLOs green · audit retention satisfied (T_TRX_HISTORY etc. archived to audit store).

**Dependencies:** all waves.

---

## 4. Sequencing constraints (why the order is what it is)

1. **Master data first** — the 514-edge god node + ~375 cache keys make Reference a de-facto platform; tariff/rate lookups gate billing, ordering and RCM (`BillServiceImpl.java:104`). Moving any consumer before the cache contract is frozen breaks ~30 services at once.
2. **IAM/token contract before anything** — auth is per-request in `ServiceLayerAspect`; the opaque bearer in cache (`session.timeout`, `UNQ_SESS_USER_`) is the only thing every frontend call carries. New services inherit it via gateway introspect; no service can move before this.
3. **Solr index ownership decided early** — patient/encounter/item/icd10cm/healthfactor collections have 6+ realtime writers + 4 batch drainers + rollback compensation in the aspect (agent-07 §1.3/§1.6). If Patient (1) moves before Search (27) owns ingestion, index drift and dual-write to Solr are guaranteed; the 3-day encounter retention is a data-loss window that must be closed by design.
4. **Encounter/admission events before clinical/financial** — billing keys off `opdIpdId`/`admissionId` (`AddServiceOrderServiceHelper.java:1213`), pharmacy dispenses by ward/admission (`:482`), nursing/diet/OT/blood-bank all read them (agent-10 §3). `EncounterCreated/PatientAdmitted` must be live before Wave 3 modules consume them.
5. **Billing cannot move before service ordering, and ordering cannot move without billing** — they share ONE transaction; the split is a saga design decision, not a sequencing one. They move in the same wave (4), and the mega-txn stays intact in the monolith until then — which is exactly why Lab/Rad **booking** (created inside that txn) is the boundary that holds until Wave 4.
6. **Payments after billing** — payment procs write bill rows (`CIMS_UPDATEBILLFORCONCESSION`, `AddPaymentServiceHelper.java:463`); the settlement state must be queryable from the new Billing service before Payments can move.
7. **Pharmacy and Inventory move together** (Wave 5) — the stock dual-write cannot be split: a decision on stock ownership (open question agent-10 §6.3) must precede both, and pharmacy billing depends on Wave 4 item-sale billing.
8. **Interop last despite centrality** — its job is consuming; it stays as the monolith-side broker (Wave 0 relay) until every producer event is live (≥ Wave 4). Moving it earlier would force dual-write on queue tables.
9. **Reports read-model only** — Reporting consumes everything; it also holds the hidden report-dispatcher routing layer (reconciliation §1) which must be emulated at the gateway before reports screens flip.
10. **Frontend-first, module-by-module** — the React SPA (agent-09, §1.1/§12) can call legacy APIs through the gateway unchanged (keep server contract); each wave flips its own screens. Frontend migration does NOT gate backend extraction (and vice versa) — this is the hybrid strangler (STRANGLER-MIGRATION §4).
11. **Triage before retirement** — 222 unmatched URLs, ~689 unused procs, `T_REGISTRATION*` schema truth, `patportal` contents, FhirServer reachability are all unresolved; each must be dispositioned before Wave 6 can complete.
12. **Critical cutover tables** — `T_ENCOUNTER`, `T_BILL`, `T_ITEMSTOCK` follow the freeze→event-forward→backfill→cut-reads protocol (agent-10 §5); they are the only tables where a lost update would be visible to patients.

---

## 5. Wave Gantt table

| Wave | Items | Rationale (evidence) | Exit criteria | Dependencies |
|---|---|---|---|---|
| **0 — Foundation** | Gateway (TO-BE); IAM (22); master cache contract; Kafka+outbox relay; CDC; observability; Solr ownership; CI/CD; watchdog replacement | God-node cache contract + token contract are platform preconditions (agent-10 §6; agent-07 §2.2); async queues prove event backbone viable (agent-10 §4.3) | 100% traffic legacy-parity via gateway; token flow intact; contract frozen; CDC on 3 pilot tables; logs/traces/metrics/alerting live; IAM extracted | none |
| **1 — Reference & Masters** | Reference Master (21); Search pipeline (27); masters frontend | Highest-coupling node after IAM; master-owned Solr collections; generic CRUD surface (1,109 URLs) | M_* on PG; cache keys served + invalidation events; CRUD-filter parity; master screens on React; Solr event-fed; reindex+reconcile; rollback rehearsal | W0 |
| **2 — Operational** | Queue/Kiosk (29); Linen (16); Diet (14); Transport (17); Equipment (18); BMW (19); Grievance (20) | Lowest coupling/risk cluster (agent-10 §8); self-contained tables; no money, minimal integration | 7 PG schemas live; screens via gateway; 2× rollback rehearsals; 5-day reconciliation green; QMS Redis state unsplit | W0, W1 |
| **3 — Clinical** | 3a: Patient (1), OPD (2), ADT (3) · 3b: Lab results (7), Rad results (8), Nursing (12) · 3c: OT (11), CSSD (15), Blood Bank (13) | Shared tenant records + event streams precede all care modules; unmapped T_REGISTRATION needs proven data migration; Lab/Rad booking stays legacy until W4 (mega-txn) | Patient/encounter/admission streams live; MRNO/IPDNO + dup-patient parity; discharge/death parity; bed CAS parity; lab/rad result+release live; Solr event-fed only; nursing/OT/blood-bank live; T_ENCOUNTER/T_ADMISSION reconciliation green | W0–W2 |
| **4 — Financial** | Ordering (4); Billing (5); Payments (6); RCM (23) | Mega-txn decomposition = core bet (agent-10 §9 risk 1); deferred for platform maturity; billing↔ordering inseparable | Saga+outbox+compensation live; InvoiceGenerated/PaymentReceived events; bill-no/tax/concession/share/advance/refund parity (BR-BILL-001..008); settlement state machine; RCM live; no dual-write | W0, W1, W3 (events); Lab/Rad booking cutover here |
| **5 — Highly-Integrated** | Pharmacy (9); Inventory (10); Interop (25); Reporting (26); RMRS (24); Govt Programs (28) | Stock dual-write resolution; Interop consumes-all (needs W4 events); Reporting read-model over all; treasury/govt downstream of payments | Stock single-writer; integration jobs migrated with cron parity; Interop on PG with in-flight continuity; report dispatcher emulated; regulatory SLAs; 222 URLs triaged; Solr retention gap closed | W4 (events), W3, W0–W1 |
| **6 — Retirement** | Remaining procs/routes; FAUG off; Oracle decommission; dead-code triage; sequence sync; rollback removal | Only 30/719 procs confirmed live (agent-05 §6); 222 URLs unresolved; logs hygiene | Oracle read-only 1 month → decommissioned; rollback switch off after 90 days; SLOs green | all |

**Timeline note (TO-BE):** wave boundaries are gate-based (exit criteria), not calendar-based; each wave includes a rollback rehearsal before the next begins. Sequencing risk is concentrated in Wave 4 (saga correctness) and Wave 5 (stock single-writer + in-flight integration continuity) — the two heaviest exit-criteria sets.

---

## 6. Open decisions that can re-order waves

1. **Stock ownership** (agent-10 §6.3): if Pharmacy keeps per-store stock, Pharmacy/Inventory split changes and Inventory may move earlier/later (agent-10 §8 note).
2. **Encounter/BILL partitioning + retention** (agent-10 §7.2) affects Wave 3/4 data-migration design.
3. **Regulatory reporting SLA** (agent-10 §7.3) could pull a Reporting read-model earlier than Wave 5.
4. **Audit stream** (agent-10 §7.4, T_AUDITEVENT via `ServiceLayerAspect`) — if a dedicated audit service is required, it rides Wave 0 (see OBSERVABILITY §5).
5. **Oracle coexistence duration** (agent-10 §7.5) — hybrid-read reporting option (state govt keeps Oracle warehouse) would shrink Wave 6 scope.
6. **Multi-unit tenancy on PG** (agent-10 §7.6, `UnitID` on nearly every table) — schema decision gates Wave 1 DDL.
