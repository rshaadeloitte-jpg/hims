# EXECUTION-BACKLOG — iHMS Modernization Work Packages

> **Source**: research evidence (evidence-register E-01…E-20), MIGRATION-WAVES (W0–W6), MICROSERVICE-CATALOG (S01–S29), FEATURE-MATRIX, STRANGLER-MIGRATION, TEST-STRATEGY, UNKNOWN-BEHAVIOR.
> **Rule**: every work package links to evidence and lists its regression gate. Nothing here may be executed before the corresponding wave exit criteria (MIGRATION-WAVES §3/§5).
> **Status legend**: ⛔ BLOCKED (evidence gate unmet) · ◐ READY-PARTIAL · ✅ CLEARED

---

## WAVE 0 — FOUNDATION & EVIDENCE GATES

### MIG-001 — W0 Evidence-gate verification batch (Oracle + deployed env)
**Objective**: Close UNKNOWN-001/004/011/013/014/015/016/031/032/008/009/010 (read-only).
**Legacy Scope**: `D:\ihms` config; live Oracle 10.10.7.63:1521/orcl.
**Target Scope**: Research registers only — no code.
**Dependencies**: Oracle read-only access + staging access (blockers).
**Files/Modules**: application.properties; DB DDL.
**Database Objects**: ALL_TAB_COLUMNS, user_sequences, ALL_DEPENDENCIES, ALL_TRIGGERS, ALL_INDEXES, ALL_VIEWS; T_REGISTRATION* DDL; T_Appointment/T_Encounter DDL (VisitStatus defaults).
**Tests Required**: n/a (research).
**Migration Steps**: (1) DDL dump (2) default-value capture (3) proc-dependency graph (4) deployed cache/props diff (5) update registers.
**Rollback**: n/a.
**Risks**: production DB read must be non-invasive.
**Acceptance**: all UNKNOWN-xxx listed above resolved or explicitly deferred with owner.
**Complexity**: M. **Blocking**: W1 start.

### MIG-002 — Contract fixtures capture (golden tests from running legacy)
**Objective**: Capture request/response fixtures for flagship screens + 132 API families + live procs (918-referenced scope, prioritized by call-site density).
**Legacy Scope**: running system + Oracle.
**Target Scope**: `test-fixtures/` repository.
**Dependencies**: MIG-001 (env access).
**Tests Required**: replay harness L0.
**Acceptance**: ≥90% of top-50 API families have ≥3 fixtures; proc fixtures for top-30 call-dense procs.
**Complexity**: XL. **Blocking**: W3+ ports' regression gates.

### MIG-003 — Target platform foundation (GitOps, CI/CD, OpenShift-ready)
**Objective**: Container registry, build pipelines (Maven→image), secret management (Vault/External Secrets), GitOps repo, observability stack (Loki/Prometheus/Tempo/Grafana), DORA dashboards.
**Legacy Scope**: build_war.sh, pom.xml.
**Target Scope**: platform repo; image for W1 services.
**Acceptance**: pipeline builds legacy WAR (proof of no-regression baseline) + first target service image.
**Complexity**: XL. **Blocking**: W1 deploy.

### MIG-004 — IAM extraction (target identity first)
**Objective**: OIDC/OAuth2 authorization server (Keycloak or equivalent); migrate token issuance; define RBAC extraction of the 2,078 data-roleM keys (UNKNOWN-014) into permission catalog.
**Legacy Scope**: DefaultAuthenticationImpl, SSOAuthFilter, EmitraRedirectFilter, SpringSecurityConfig, PasswordValidator, LDAP utils, 34 secret keys.
**Target Scope**: auth-service (S22), token cache via Redis, session revocation.
**Security Gates**: remove test backdoor, static-token bypass, password no-op — must NOT replicate E-07 findings.
**Acceptance**: legacy and target authenticate against same IdP; role map exported; secrets in vault with rotation.
**Complexity**: XL. **Blocking**: all waves (identity is shared plane).

### MIG-005 — API gateway & routing replication (5 surfaces)
**Objective**: Gateway (API-first) replicating: (1) CXF bus surface (contracts), (2) CRUD filter family (659 entities), (3) report dispatcher, (4) /ext/api mappings with allowedUsers→OAuth scopes, (5) FHIR + /error paths. Triage 222 unmatched URLs (UNKNOWN-005).
**Legacy Scope**: services.xml, CrudOperationsFilter, IntegrationOperationsFilter, GenericServiceImpl, FhirServer, ErrorPathHandler.
**Target Scope**: gateway routes + contract catalog (OpenAPI per surface).
**Acceptance**: gateway replays captured fixtures bit-compatible (MIG-002); 222 URLs triaged.
**Complexity**: XL. **Blocking**: W1 cutover.

### MIG-006 — PostgreSQL foundation schema & migration tooling
**Objective**: schema-per-service convention, Flyway/Liquibase baseline, sequence strategy (seed from Oracle LAST_NUMBER; UNKNOWN-free numbering), 894-table mapping ledger, plpgsql conversion scaffold + REFCURSOR support, PII rules.
**Legacy Scope**: generated-procs (702+16), DTOQueries_1..5 (918 proc names), SEQ_CREATE_SCRIPT.sql, POSTGRESQL-MIGRATION design.
**Acceptance**: mapping ledger covers 894 tables; 10 pilot procs ported to plpgsql with fixtures.
**Complexity**: XL. **Blocking**: W1+ ports.

---

## WAVE 1 — REFERENCE & MASTER DATA

### MIG-010 — Master data service (S21) + masters screens
**Objective**: Migrate masters CRUD (CRUD-filter family, 620+ master entities), MasterCachedServiceImpl cache-layer semantics, masters routes (~200 of 636), Solr item/service/icd10cm syncs.
**Legacy Scope**: GenericServiceImpl, MasterCachedServiceImpl (13,331 LOC — sliced, not ported wholesale), modules/masters, fjs-config_masters.json.
**Database Objects**: M_* tables (~608 masters entities); sequences.
**Business Rules**: BR-MST cluster; UNKNOWN-019 (indexed naming) verification.
**Tests**: master CRUD contract replay; cache invalidation; Solr sync (E-16).
**Acceptance**: masters read path 100% target; dual-run parity.
**Complexity**: XL. **Blocking**: MIG-003/004/005/006.

### MIG-011 — Unit/facility/location & doctor masters
**Objective**: Unitmaster (1,691 LOC entity — split into unit + config), Doctor master, department/ward/room; facility-scoped authorization prerequisites.
**Dependencies**: MIG-010 patterns.
**Complexity**: L.

### MIG-012 — Item master + item stock foundation (shared plane ownership)
**Objective**: M_ITEMMASTER + T_ITEMSTOCK ownership decision (stock split: pharmacy/inventory/CSSD consumers share an item-service read model); batch/expiry ordering (UNKNOWN-012).
**Legacy Writers (V6)**: InventoryServiceImpl (44), ExpiryItemServiceImpl (19), ItemStockServiceImpl (13), CSSD (7), Medication (4), Pharmacy (3) + CIMS_UPDATE_CONSIGNEDGRN_ITEMSTOCK.
**Acceptance**: single writer per stock table in target; readers via API/events.
**Complexity**: XL. **Blocking**: W2 pharmacy/inventory.

---

## WAVE 2 — OPERATIONAL QUICK WINS

### MIG-020 — Grievance & feedback (S20)
**Complexity**: S. Full traceability exists in FEATURE-MATRIX F-series.

### MIG-021 — File management & MinIO migration
**Legacy**: CommonUtilServiceImpl addFileToMinio (37 call sites), minio.enabled=false, int.files.path.
**Target**: object storage service (S25 adjacent); file→object mapping.
**Complexity**: M. Risk: document.files.path fallback (NAS) dependencies — inventory before cutover.

### MIG-022 — Queue & kiosk (S29)
**Legacy**: queue module, kiosk route, QueueManagmentServiceImpl, UNKNOWN-026 getLastVisitStatus.
**Complexity**: M.

### MIG-023 — Transport (S17), Linen & laundry (S16), Diet (S14), Bio-medical waste (S19), Equipment (S18)
**Complexity**: S–M each; batch as one wave-2 epic (five service stubs + screens).
**Gates**: eAusadh-equipment interfaces (Equipment Interface docs in artifacts/).

---

## WAVE 3 — CLINICAL SPINE (admission → care → clinical support)

### MIG-030 — Patient identity & registration (S01) + MRN/ABHA
**Objective**: Patient registration (PatientRegApptServiceImpl/PatientServiceImpl core), T_REGISTRATION* (UNKNOWN-016 — DB-only, 120 native-SQL sites) → owned PG schema, MRN generation (atomic+idempotent), duplicate detection (mobile/Aadhaar/Janaadhaar/ABHA), Solr patient index sync inside tx (E-16), RGHSSims/Chiranjeevi/Bhamashah/Janaadhaar calls (E-18).
**Business Rules**: BR-REG cluster; BR-REG-001 (MRN).
**Tests**: duplicate detection matrix; OTP flows (eMitra switch PatientRegApptServiceImpl:3362); Solr rollback-on-error.
**Acceptance**: registration parity incl. solr_status bookkeeping (E-16 evidence: SolrUtil:955).
**Complexity**: XL. **Blocking**: W4 billing dependency on patient/encounter.

### MIG-031 — Appointment & Encounter (S02) — split-brain resolution
**Objective**: T_APPOINTMENT + T_ENCOUNTER lifecycle (UNKNOWN-001/002/011), visit status dictionary lock, encounter Solr 3-day window, Encounter entity 1,518 LOC split.
**Tests**: status transition matrix; encounter daily close job (ApplicationJobs:542).
**Complexity**: XL. **Blocking**: W4.

### MIG-032 — ADT & bed (S03)
**Legacy**: PatientRegAdmissionServiceImpl (admitPatient tx :236), BedManagementServiceImpl (8), PatientTransferDischargeServiceImpl (27 T_ADMISSION writes), interimAdmission.abscond job.
**Database**: T_ADMISSION (no entity — model contract-first; E-11).
**Tests**: admit→bed→transfer→discharge flows; abscond sweep.
**Complexity**: XL.

### MIG-033 — Ordering & prescription (S04) — mega-transaction owner
**Objective**: THE critical bet. AddServiceOrderServiceImpl:2037 mega-transaction (E-10) — order→bill(CIMS_ADDBILL)→charges→lab→rad→EMR→doctor share→item sale. Decide: keep as one transaction in order-service with synchronous calls, or decompose with saga (outbox). HL7 leg stays disabled (E-10).
**Tests**: full-chain fixture replay; failure at each step (rollbackOn=Exception semantics — every checked exception rolls back; target rollbackFor=Exception); idempotency of CIMS_ADDBILL.
**Acceptance**: golden parity on 30 live scenarios; saga (if chosen) with compensation tested.
**Complexity**: XL+. **Blocking**: W4/W5 (billing, lab, rad, pharmacy all consume).

### MIG-034 — Nursing & inpatient care (S12)
**Legacy**: NursingServiceImpl 18,119 LOC (sliced by flow: notes, allocation, vitals, BCMA, care plan), 135 @Transactional.
**Complexity**: XL.

### MIG-035 — OT & surgery (S11)
**Legacy**: OTSchedulerServiceImpl 4,954 LOC, 56 proc call sites.
**Complexity**: L.

### MIG-036 — Chemotherapy + care plan + charting (clinical support)
**Complexity**: M.

---

## WAVE 4 — FINANCIAL / REVENUE SPINE

### MIG-040 — Billing & settlement (S05) — contract-first
**Objective**: T_BILL/T_BILLDETAILS/T_CHARGES (no entities; E-11) modeled as target contracts; client-side billing arithmetic (billing-module.js:2094-2111) replaced by server-validated totals (BR-BILL-001…005; UNKNOWN-020 rounding; UNKNOWN-028 tax buckets); CIMS_ADDBILL/ADDCHARGE ports; concession/copay (getConcessionDetails :1383-1396).
**Tests**: fixture replay of bill receipt family; rounding parity; tax 1/2/3 behavior.
**Complexity**: XL. **Blocking**: W5.

### MIG-041 — Payments & refunds (S06) + advances
**Legacy**: AddPaymentServiceHelper (:477,820 VisitStatus=11 writes), payment DTOs.
**Complexity**: L.

### MIG-042 — RCM & claims (S23), insurance
**Legacy**: RcmServiceImpl, rcm module, HCX client (xcg.abdm.client.hcx), insurance workflows (insurance-rcm.md).
**Complexity**: XL. Integration-heavy (HCX JWE payloads).

### MIG-043 — RMRS & PMRD treasury (S24)
**Legacy**: rmrs module, rmrs.authorityPayment schedule, PMRD.
**Complexity**: L.

---

## WAVE 5 — HIGHLY-INTEGRATED

### MIG-050 — Pharmacy & dispensing (S09)
**Legacy**: PharmacyServiceImpl 49 tx, pharmacy module (3 controllers), item stock updates (:2110), prescription flows.
**Complexity**: XL.

### MIG-051 — Inventory & procurement (S10)
**Legacy**: InventoryServiceImpl 12,941 LOC / 209 proc call sites / 44 native stock writes; GRN/CONSIGNED (CIMS_UPDATE_CONSIGNEDGRN_ITEMSTOCK); auto-indent jobs (unitWise.autoIndent 0 0/10 * * * ?); e-Aushadhi integration.
**Complexity**: XL.

### MIG-052 — Lab (pathology) (S07)
**Legacy**: PathoOrderServiceImpl 14,629 LOC / 74 proc sites; instrument/OLIS interface (UNKNOWN-010); zebra ZPL labels; patho-booking-module.js.
**Complexity**: XL.

### MIG-053 — Radiology (S08)
**Legacy**: RadiologyServiceImpl, DICOM/PACS question (artifacts/docs; UNKNOWN gap if any).
**Complexity**: L.

### MIG-054 — Blood bank (S13)
**Legacy**: BloodBankServiceImpl 7,836 LOC / 48 tx; DNR collision UNKNOWN-003; e-Raktkosh (E-18).
**Complexity**: L–M.

### MIG-055 — Interop & notifications (S25) — ABDM/HCX/HL7/FHIR/eSanchar
**Legacy**: xcg.abdm (ABDMController, HFRController, HIP/HIU/ABHA/PHR/HPR), NHCXServiceImpl, HAPI FHIR server (UNKNOWN-009), util.hl7, SendNotificationServiceImpl (41 @Async), WhatsApp queue.
**Complexity**: XL. **Gates**: FHIR reachability, ABDM sandbox fixtures.

### MIG-056 — Govt programs (S28) — EJS/Aasmaan/Chiranjeevi/RGHS/PCTS/Mera Aspataal/Jeevanvahini/Kaysons
**Complexity**: L each, aggregated M.
**Note**: EJS ASHA incentive job (ejs.asha.incentive 0 0 23 * * ?).

### MIG-057 — Reporting & analytics (S26) + report dispatcher
**Legacy**: IntegrationOperationsFilter reportMap, reports.js Nashorn, generic_reports.vm, REPORT-INVENTORY (50 config keys), JasperReports 6.0.3.
**Complexity**: XL. Print/PDF families (openhtmltopdf/iText) + qz-tray WebSocket print (application.properties:726).

### MIG-058 — Search & index pipeline (S27) — Solr continuity
**Objective**: transactional outbox (Kafka) → indexer → Solr; replace in-tx Solr writes (E-16) without changing search behavior; encounter rolling window preserved; initial full reindex from PG.
**Tests**: index consistency drill; outage behavior (search degraded vs blocked).
**Complexity**: XL. **Risks**: THE high-risk decoupling — do not change query semantics.

---

## WAVE 6 — MONOLITH RETIREMENT

### MIG-060 — Remaining features/URLs (222-triage leftovers; UNKNOWN-005 residue)
**Complexity**: varies.

### MIG-061 — Oracle retirement & data finalization
**Objective**: freeze legacy; final CDC catch-up; switch gateway; Oracle read-only; archive; T_REGISTRATION* final sync.
**Complexity**: XL.

### MIG-062 — Legacy decommission + docit.ctrl replacement (UNKNOWN-029) + kill switch
**Complexity**: M.

### MIG-063 — Final security hardening sweep (post-cutover pen test, secrets purge from git history — note: D:\ihms history contains secrets; target repo must be fresh)
**Complexity**: L.

---

## CROSS-CUTTING / EVERY WAVE

- **MIG-C01** — Register updates with every code change (living docs rule, README §6).
- **MIG-C02** — Contract fixtures commit per API family (TEST-STRATEGY).
- **MIG-C03** — Per-wave rollback drill (STRANGLER §8; gateway switch-back).
- **MIG-C04** — Data migration job + reconciliation per owned schema (per-service, not global).
- **MIG-C05** — Security gate per wave: no secrets in code, OAuth scopes per /ext/api mapping, audit event parity (UNKNOWN-035).

---

## Backlog scoring summary (risk × effort — see MIGRATION-COMPLEXITY + MIGRATION-WAVES §2)

| ID | Wave | Complexity | Primary risk |
|---|---|---|---|
| MIG-033 | W3 | XL+ | Mega-transaction decomposition (E-10) |
| MIG-058 | W5 | XL | Solr continuity (E-16) |
| MIG-030/031/032 | W3 | XL | Identity + status split-brain (UNKNOWN-001/002/016) |
| MIG-040 | W4 | XL | Client-side math → server (E-15) |
| MIG-004/005 | W0 | XL | IAM + 5 routing surfaces (E-04/E-07) |
| MIG-002/006 | W0 | XL | Fixtures + 918-proc port scope (E-08) |

## Sequencing constraints
1. W0 gates (MIG-001/002) precede everything.
2. MIG-004 (IAM) precedes all cutovers — legacy authz absence (E-06) makes contract-first authz mandatory, not optional.
3. MIG-033 must precede W4/W5 financial ports (all downstream consumers share the order→bill chain).
4. MIG-058 may run in parallel with W3-W5 read-path migrations (search is read-side) but must complete before Oracle retirement.
5. No wave may begin before its predecessor's exit criteria (MIGRATION-WAVES §5) plus rollback drill.