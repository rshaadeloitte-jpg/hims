# API-INVENTORY — iHMS Master API Surface (synthesized)

> Synthesized 2026-08-14. Master inventory derived from `04-api-catalog.md` (3,060-endpoint dump, file:line evidence in
> `_discovery/agent-04/findings.md`), frontend consumer map (`_discovery/agent-03/`), and routing-layer verification
> (`reconciliation-notes.md` + direct source inspection of `CrudOperationsFilter.java`, `IntegrationOperationsFilter.java`,
> `integration/mappings.json`). The full 3,060-row dump lives in 04-api-catalog.md — this document is the **synthesized view**:
> module roll-ups, API families, duplicates, consumers, and migration targets. Unknowns are marked `[UNKNOWN]`.

---

## 1. Surface overview

| Dimension | Value | Evidence |
|---|---|---|
| Explicit JAX-RS endpoints | **3,060** = 3,003 (GET 1,441 / POST 1,369 / PUT 162 / DELETE 31) + 57 ABDM/HFR callbacks | findings.md §1 |
| Service classes | 132 (120 wired into CXF bus `restServices` via services.xml; 11 @Component beans NOT wired → dead routes) | findings.md §2 |
| @Transactional endpoints | **1,056 (35.2%)**; 1,445 @Transactional occurrences total | findings.md §0/§1 |
| Base URL | `http://<host>:8080/IHMS/api/rest` (server.port=8080, context-path=/IHMS, CXF `/api/*`, `<jaxrs:server address="/rest">`) | 04-api-catalog header |
| **Hidden routing layers (not in catalog)** | ① `CrudOperationsFilter` generic CRUD (659 `@CrudApi` entities) — serves 1,109 FE URLs; ② `IntegrationOperationsFilter` report dispatcher (`/api/rest/reports/<name>`, 50 configs); ③ same filter `/ext/api/<mapped>` integration gateway (4 mappings) | reconciliation-notes §1; CrudOperationsFilter.java:123-136; IntegrationOperationsFilter.java:435-468 |
| Frontend consumers | 3,867 unique FE URLs → 3,651 clean → **2,322 matched explicit endpoints, 1,109 via generic CRUD, 222 unresolved** (triage list `_discovery/recon-unmatched-urls.txt`) | reconciliation-notes §1 |
| DB surface | 843 tables / ~898 entities; 659 @CrudApi entities = CRUD-API surface; 719 PL/SQL artifacts (only ~30 called from Java) | reconciliation-notes §4 |
| Security model | Token-based (header `token` or query param); auth via `ServiceLayerAspect`; backend authorization only at `/ext/api` (`allowedUsers`, ROLE_INTEGRATION); RBAC otherwise frontend-gated; POST `/api/rest` bodies HTML-stripped (XSS guard, `secHTMLAllowedAPIs`); referer whitelist; CSP headers | reconciliation-notes §5; IntegrationOperationsFilter.java:383-388,636-654 |

---

## 2. Module roll-up (endpoint counts per module, from 04-api-catalog.md headers)

| # | Module | Endpoints | Dominant services (class: endpoints) | Tx emphasis | Primary frontend consumers |
|---|---|---|---|---|---|
| M1 | Custom (OPD/IPD/Billing workflows) | **1,204** | InventoryServiceImpl 235; MasterCachedServiceImpl 264; PathoOrderServiceImpl 133; PharmacyServiceImpl 102; AddServiceOrderServiceImpl 49; EmergencyAndWalkinVisitServiceImpl 45; MdmServiceImpl 55; BedManagementServiceImpl 33; AddUserServiceImpl 55 | heavy (126 tx in Inventory, 50 in PathoOrder) | inventory-module.js (1.08 MB), billing-module.js, patho-modules.js, pharmacy-module*.js, masters-modules.js, admission-module.js |
| M2 | PAS (Patient Admin/Billing/Masters) | **998** | BloodBankServiceImpl 113; PatientServiceImpl 94; OTSchedulerServiceImpl 80; MasterServiceImpl 72; EquipmentServiceImpl 51; ExpiryItemServiceImpl 47; RMRSServiceImpl 40; IPDConfigServiceImpl 35; DietServiceImpl 30; AppointmentServiceImpl 29; TransportServiceImpl 31; DoctorServiceImpl 25 | broad | blood-bank-module.js, operation-theatre.js, equipment-modules.js, rmrs-module.js, transport js |
| M3 | Nursing | **300** | NursingServiceImpl 300 (154 GET/131 POST/10 PUT/5 DELETE; ~135 tx) | heavy | nursing-module.js + nursing-module-ext.js + nursing-note-module.js (1.7 MB total) |
| M4 | EJS (ASHA/Outreach) | **125** | EjsDetailsServiceImpl 54; EjsAncPncChildServiceImpl 20; EjsSupervisorServiceImpl 12 | light | ejanswathya/ejs screens |
| M5 | AASMAAN (Maternal-Child/Telemed) | 78 | AasmaanCaseServiceImpl 44; AasmaanDeviceServiceImpl 8; AasmaanInstitutionServiceImpl 8 | moderate | aasmaan module (external device-facing) |
| M6 | ABDM (ABHA/HPR/NHCX) | 73 | AbhaServiceImpl 50; HPRServiceImpl 13; NHCXServiceImpl 10 | light (9 tx) | abdm-module.js; NDHM/ABDM integration |
| M7 | ABDM/HFR **callbacks** | 57 | ABDMController 42 + HFRController 15 (`com.ohum.xcg.abdm.server`) | none (writes response directly) | inbound NDHM (external) |
| M8 | CSSD | 46 | CSSDServiceImpl 46 | moderate | cssd screens |
| M9 | File Management | 24 | FileManagementServiceImpl 24 | some | dms screen |
| M10 | Integrations (RGHS/NMC/eRaktKosh/eUpkaran/ABDM/WhatsApp) | 22 | IntegrationServiceImpl 22 | some | external schemes |
| M11 | Queue Management | 23 | QueueManagmentServiceImpl 23 | some | queue sub-app + counters |
| M12 | Patient Portal | 23 | PatientPortalServiceImpl 22; PatientHistoryServiceImpl 1 | some | patient portal (ohumlite) |
| M13 | CWF | 17 | CWFServiceImpl 17 | some | cwf mapping screens |
| M14 | Biomedical Waste | 14 | BioMedicalWasteManagementServiceImpl 14 | some | biomedicalwaste |
| M15 | Linen & Laundry | 14 | LinenLaundryServiceImpl 14 | some | linenlaundry |
| M16 | Authentication | 12 | AuthenticationServiceImpl 12 | none (aspect-based) | login screens, SSO |
| M17 | Notifications | 5 | SendNotificationServiceImpl 5 | some | commons |
| M18 | Clinical | 4 | ClinicalServiceImpl 4 | light | nursing/doctor screens |
| M19 | EHR (SSO/Sync) | 8 | EHRUtilServiceImpl 8 | light | cross-platform EHR sync (job-driven) |
| M20 | Payments | 6 | PaymentTransferServiceImpl 6 | 1 tx | payment transfer |
| M21 | RCM (Chiranjeevi/RGHS) | 7 | RcmServiceImpl 7 | none | rcm/chiranjeeviapp |
| M22 | Patient Portal extras | (in M21/M17) | — | — | — |

**God services** (top-5 by endpoint count): NursingServiceImpl (300), MasterCachedServiceImpl (264, ~12.7k LOC), InventoryServiceImpl (235), PathoOrderServiceImpl (133), BloodBankServiceImpl (113). These five ≈ 1,045 endpoints (~34% of the explicit surface).

---

## 3. API families (the four routing layers + recurring families)

### 3.1 Generic CRUD family — `CrudOperationsFilter` (data-driven, NOT in the 3,060 catalog)

Evidence: CrudOperationsFilter.java — `CRUD_API_PATT = {context}/api/rest/([^/]+)(.*)` (line 123); entity registry = all classes annotated `@CrudApi` in `com.ohum.models` (**659 entities**, lines 125-136); route key = `@CrudApi.value()` else `entitySimpleName.toLowerCase()`.

**URL grammar (evidence from doFilter, lines 334-587):**

| Route | Verb | Semantics | Key params |
|---|---|---|---|
| `/api/rest/<entity>` | GET | List with pagination (`crudgetAll`) | `page`, `count`, `search`, `searchFld`, filters from `@CrudApi.allowedFilters` |
| `/api/rest/<entity>/all` | GET | Full list, all rows (`crudgetAllAll`) | same |
| `/api/rest/<entity>/<id>` | GET | Single entity (`crudget`) | `_type=json\|xml` |
| `/api/rest/<entity>` | POST | Add one (`crudadd`) + audit event `ADD.MASTER` | JSON/XML body; `_type` |
| `/api/rest/<entity>/multiple` | POST | Bulk add (`crudaddMultiple`) | JSON/XML array |
| `/api/rest/<entity>/upload` | POST | File upload → parse JSON/XML/CSV(xls/xlsx) → bulk add | `isHdrRow` (skip header row) |
| `/api/rest/<entity>/<id>` | PUT | Update (`crudupdate`) + audit `UPDATE.MASTER` | `nonNull=yes`, `__cufl=<field list>` (partial update) |
| `/api/rest/<entity>/<id>` | DELETE | Delete (`crudremove`) + cache invalidation | — |

Response through `RequestResponseInterceptor.processResponse` (JSON/XML). Cache coherence via `@CrudCached`/`@CrudCachedSet` annotations (`updateCachedObject`). Entity base: `MBase` (`getFilterMap`, `fromCsv`, `getTypeReference`).

**Scale**: 1,109 of 3,651 frontend URLs resolve here (e.g. `accidenttype/all`, `bedmaster/upload`, `citymaster/{id}`) — all master screens render through one family. **Migration target**: this family can be replaced by one framework-standard CRUD engine (or DB-generated API); the 659-entity registry is the contract.

### 3.2 Report dispatcher family — `IntegrationOperationsFilter` `/api/rest/reports/<name>`

- 50 config keys in `integration/reports/reports.json`; POST-only; token required; output HTML/PDF/CSV.
- Full inventory in **REPORT-INVENTORY.md §3**.

### 3.3 Integration API family — `/ext/api/*` (mappingMap, 4 live mappings)

Evidence: IntegrationOperationsFilter.java:243-255 (loads `int.files.path/mappings.json`), 435-468 (dispatch), `D:\ihms\integration\mappings.json`. Gateway = `int.api.path.prefix /ext/api` (application.properties:367). Auth: `allowedUsers` empty (no restriction) or must contain username with `ROLE_INTEGRATION` (lines 442-444).

| Meth | Ext path | → origPath (internal) | Templates | ContentType |
|---|---|---|---|---|
| POST | `/ext/api/login` | `/auth/loginf` | auth/it.txt → auth/ot.json | application/json |
| GET | `/ext/api/all_departments` | `/departmentmaster` (generic CRUD) | ot.json | application/json |
| POST | `/ext/api/all_doctors` | `/doctorlist/getDoctorMasterDetailsListbyDepartmentId` | doctors/it.json, ot.json | application/json |
| POST | `/ext/api/doctor_schedule` | `/appointmentschedule/getAppointmentScheduleNew` | appointments/it.json, ot.json | application/json |

Transformation via Nashorn `scripts/api.js` (`Faug.templatize`). **Only 4 mappings today** — the gateway is the sanctioned pattern for external system exposure.

### 3.4 ABDM/HFR callback family (57 endpoints, `com.ohum.xcg.abdm.server`)

- **ABDMController** (42): dual-versioned — v2 `POST /abdm/{versionNumber}/{flow}/{endpoint}` (consent-requests/on-init, consents/hip/notify, consents/hiu/notify, links/link/init, links/link/confirm, care-contexts/discover, health-information/hip/request, patients/on-find, users/auth/on-*, subscriptions/*, heartbeat GET) **and** v3 `POST /abdm/api/v3/...` (consent/request/hip/notify, hip/link/care-context/{init,confirm}, hip/patient/care-context/discover, hip/patient/share, hip/health-information/request, hip/token/on-generate-token, hiu/*, link/on_carecontext, links/context/on-notify, patients/sms/on-notify) **plus** catch-all `POST /{versionNumber}/{endpoint}/on_{action}` (ABDMController.java:1340) **and** data-transfer endpoints `/abdm/health-information/transfer/{uniqId}` (+ `.../uniqId/{uniqId}` old form).
- **HFRController** (15): `/abdm-hfr/{versionNumber}/facility/*` — get-master-data, lgd/states|districts|subdistricts, software-details, basic/detailed/additional-information, search, submit-facility, fetch-facility-type/-sub-type, get-owner-subtype, get-specialities, saveFacilityNumber.
- All write `HttpServletResponse` directly; no service-layer tx. **Migration target**: isolate as a versioned inbound gateway; the v2/v3 double implementation and the catch-all are compliance/risk hotspots (see duplicates §4).

### 3.5 Other recurring families

| Family | Members | Notes |
|---|---|---|
| **Authentication** | `/auth/loginf`, `loginb` (Basic), `loginh` (header), `loginJ` (apiToken/apiKey), `loginExt`/`loginextf` (KDL), `loginotp`, `loginPatient`, `getuserrole`, `getuserroleOtp`, `changeUserRole`, `logout` | 12 endpoints, 11 POST + 1 PUT; SSO via `usermanagement/loginsso`; OTP variants; session in cache (`UserView`) |
| **Patient search** | `patient/getPatientGeneralDetailsListForSearch`, `patientDetailReg/getPatientGeneralDetailsSearch`, `patient/searchPatientFromSolr`, `patient/searchPatientForBloodBank`, `patient/getPatient`, `abha/findPatient`… | Solr-backed search (3-day rolling encounter window); heavily used by every registration screen |
| **File upload** | `uploadFile/{mastername}`, `uploadFile/servicemapping*`, `upload/{mastername}`, `fileService/*` (24), `ejs-details/uploadphoto*`, `pharmacy/uploadPrescription` | POI/CSV parse via FileStoreUtil; master bulk loads via generic CRUD `/upload` |
| **Stored-proc views** | ~30 procs actively called (namedStoredProcQ); `*ResultDto` views + `@StoredProcNamedParam` params | 719 generated procs exist; only ~30 confirmed live — rest `[UNKNOWN]` usage (DEAD-CODE-CANDIDATES.md) |
| **Print/PDF** | 49 `*Report` + 72 `print*` endpoints | see REPORT-INVENTORY.md §4 |
| **External scheme integrations** | `integration/*` (RGHS SIMS, NMC, e-RaktKosh, eUpkaran, Jan Aadhaar OTP, WhatsApp), `rcm/*`, `nhcx/*` | encrypted payloads (RSA-OAEP-256 JWE for NHCX), token exchanges |
| **Queue** | `queueManagment/*` (23) + `fetchToken`, `appointment/*Queue*` (building/cabin/department lists) | token/queue state machine; standalone queue sub-app |
| **File serving** | `patient/{filename}` servePdf, `billReceipt/downloadFile(Open)`, `fileService/downloadFile`, `ejs downloadMedia`, `RMRS` print GETs | byte[]/stream responses |

---

## 4. Duplicates, overloads and suspicious bindings

### 4.1 Exact duplicates (verb+path) — 2 groups

| # | Endpoint | Duplicate detail | Behavior |
|---|---|---|---|
| 1 | `POST /fileService/uploadHospitalLocation` | **defined twice** in FileManagementServiceImpl (multipart form vs JSON body overload) | CXF selects by Content-Type — must be verified per caller; same method name |
| 2 | `GET /queueManagment/addToQueue` **+** `POST /queueManagment/addToQueue` | GET → `createOPDQueueByGET`, POST → `addToQueue` (different methods, same path) | intentional overload; documented |

### 4.2 Same-path/different-verb groups — 28 groups (mostly CRUD GET/POST pairs)

e.g. `/abha/insurancePlanRequest` (GET fetchById vs POST generic fetch), `/cssd/*` GET-verb mutations (`approveBioMedicalRequest` etc. are GETs that mutate — REST anti-pattern), `/nursingList/getPatientNote` vs `getPatientNote/{id}`, `/RMRS/print*` GET print endpoints. **Catalog flag #5.** The `GET-as-mutation` pattern is systemic (e.g. `GET /bioMedicalWasteManagement/approveBioMedicalRequest`, `GET /linenLaundry/approveLinenRequest`, `GET /cssd/updateCssdRequisitionForRinsing`, `GET /patient/updateBedOccupiedFlag` PUT variant…) — ~80+ GET endpoints mutate state.

### 4.3 ABDM dual-versioning (v2 + v3 + catch-all)

`POST /abdm/{versionNumber}/links/link/on-add-contexts` appears **twice** (onLinkAddContext vs onLinkAddContextv3 — catalog lines 3192-3193). v2 and v3 paths cover the same callbacks (consent notify, health-info request, link init/confirm, care-context discover, patient share, sms notify, context notify) with different payload shapes — dual-maintenance risk. Catch-all `POST /{versionNumber}/{endpoint}/on_{action}` may swallow unknown routes.

### 4.4 Method-binding anomalies (parser flags — verify before trusting catalog mapping)

| Row | Anomaly | Risk |
|---|---|---|
| `GET /addUserService/checkDescriptionInServiceMaster` | bound to `getActiveAdmissionOfRegisteredPatient` | wrong-method binding (copy-paste in code or parser gap) |
| `POST /patient/updateJanaadharDetails` | bound to `getPatient` | same |
| `POST /nursingList/patientDetailsByNewMRN` | bound to a **private** method | unreachable? |
| `GET /serviceOrder/getSponsors` | declared **PUT** (verb misuse) | client must use PUT |
| `GET /patient/getPatientEorders` | bound to `getPatientByAddhar` | likely copy-paste bug — returns Aadhaar-lookup data under "e-orders" |
| `POST /radiology/addRadioTestMaster` vs `POST /radiology/addRadTestMaster` | two URLs, different return types (void vs DTO) — near-duplicate | verify both callers |
| `GET /patientRegAppt/pdfAppointmentFaceSheet` + `...Html` + `...FromAppList` | 3 near-identical PDF endpoints (String[] vs Map) | consolidation candidate |
| `getUserwise*ReportOld` ×3 dispatcher keys | legacy duplicates still POSTed by FE schemas | dead once screens migrate |

### 4.5 Cross-class same-name endpoints (not duplicates, but confusion risk)

`/abha/getCard` vs `/hpr/getCard`; `getPatient` across patient/patientDetailReg/patientPortalService/nursingList; `addRefund` in `/addRefund`, `/refund`, `/payment`; `printTokenSheet` (patientRegAppt) vs `printToken` (queueManagment); `updateStatusProcedureCSSDKitDetails` PUT vs GET mutations in CSSD.

---

## 5. Consumer analysis (frontend ↔ backend)

| Backend surface | FE consumers (files) | # unique FE URLs | Notes |
|---|---|---|---|
| `nursingList/*` (300) | nursing-module.js, nursing-module-ext.js, nursing-note-module.js, nursing-charting-module.js, bcma-module.js, operation-theatre.js | ~293 | largest single surface |
| `inventoryStockList/*` (235) | inventory-module.js (301+ API refs) | ~226 | + expiryitem, companyapprovedamount, planmaster |
| `master/*` (264) | masters-modules.js, masters-ext.js, report-module.js (janta) | ~195 | cached god-node; also `masterConfigService/*` (72) |
| `bloodtestmaster/*` (113) | blood-bank-module.js | ~115 | 16 report routes too |
| `pathoOrder/*` (133) | patho-modules.js, patho-booking-module.js | ~114 | incl. lab report prints |
| `pharmacy/*` (102) | pharmacy-module.js/new/op, opPharmacy-new.js | ~98 | + medication/* |
| `patient/*` (94) | common-module.js, appointment-module.js, admission-module.js, enquiry | ~83 | search-typeahead everywhere |
| `otscheduler/*` (80) | operation-theatre.js | ~79 | |
| `reports/*` (dispatcher) | modules/reports/schemas/*.json crudops + report-module.js | ~47 | see REPORT-INVENTORY.md |
| Generic CRUD `/<entity>/*` | mastersConfigration.json + masters-ext.js (schema-driven), plus every module's dropdown/list crudops | 1,109 | all 659 entities |
| `/ext/api/*` | external consumers [UNKNOWN — not in FE repo] | 4 | integration gateway |
| ABDM/HFR callbacks | NDHM/HFR (external) | 57 | inbound only |
| Unresolved FE URLs | — | **222** | triage file `recon-unmatched-urls.txt`; includes FE typos (`diagosismaster`, `referraltypemastermaster`, `proceduretypemaster//{}`, `expiryitem//getgrnnoforrettosupplier`, `billReceiptdownloadFileOpen`) that may resolve via CRUD filter or be dead |

**Tx hotspots** (cross-entity writes — service-boundary design input): admission chain `patientRegAddmission/adddata` (T_Admission+T_AdmissionDetails+bed+advance, PatientRegAdmissionServiceImpl.java:237-241); payment `addPayment/` (T_Bill/T_LedBill/T_TledBill+advances+receipts); service order `serviceOrder/addServicechargesNew` (T_Charges+bill+package consumption, 22 tx methods); discharge chain `patientTransferDischarge/*` (admission status+bill settle+bed release+doctor checkout); `nursingList/*` (135 tx methods); Order→Bill→Lab→Rad→DoctorShare→EMR→HL7 mega-transaction (reconciliation §6).

---

## 6. Target-API notes (migration guidance at family granularity)

| Family | Target API note |
|---|---|
| Generic CRUD (659 entities) | Replace with one engine-level CRUD API (e.g. generated REST/graph); preserve `/all`, `/multiple`, `/upload`, `__cufl`, `allowedFilters` semantics — frontend depends on them via `mastersConfigration.json` |
| `/auth/*` | Consolidate 7 login variants into one OAuth2/OIDC flow; keep `loginh`/`loginJ` server-to-server variants behind an API gateway; OTP flows map to OIDC OTP grant |
| Report dispatcher | Promote to a scheduled/on-demand reporting service; move 50 SQL configs into versioned migrations; replace Nashorn/Faug templating with server-side templates; PDF via standard renderer |
| `/ext/api` gateway | Convert to gateway routes (4 today); keep transform contracts (it/ot templates) as data |
| ABDM/HFR | Single versioned inbound gateway; retire v2 or freeze; move catch-all behind explicit route table |
| `nursingList`/`inventoryStockList`/`pathoOrder` god-services | Split by bounded context (nursing orders, medication admin, notes; stock, indents, PO/GRN; lab orders, results, reporting) — 300+ endpoint services are the top decomposition risk |
| `master/*` cached endpoints | Keep as read-model/cache service (Solr/Redis-backed) — highest-frequency reads |
| Print endpoints (121) | Centralize in print/reporting service; see REPORT-INVENTORY.md |
| Stored-proc views | Keep only ~30 live procs; drop/regenerate the rest |

---

## 7. Top risks (API-specific)

1. **1,109-URL data-driven CRUD layer + 222 unresolved FE URLs** — the CRUD grammar is undocumented runtime behavior; typos still resolve silently through the filter. Migration must generate the CRUD contract from the 659-entity registry and triage all 222 URLs.
2. **God services** — top-5 services = 34% of endpoints, 1,045 endpoints with 300+ tx methods in one class (NursingServiceImpl); any API redesign must decompose these first.
3. **ABDM v2/v3/catch-all triple paths** — inbound compliance surface with duplicated handlers; misrouting affects consent/health-data flows.
4. **GET-mutation pattern** (~80+ endpoints) — cache/CSRF/idempotency hazards; must be normalized in target.
5. **Exact duplicate `POST /fileService/uploadHospitalLocation`** — content-type-dependent dispatch is fragile.
6. **Method-binding anomalies** (§4.4) — at least 4 rows show signature/URL mismatches that are either bugs or parser gaps; each must be confirmed against source before migration mapping.
7. **11 @Component service beans not wired into CXF** — may be dead or accessed via internal dispatch `[UNKNOWN]`.
8. **No backend authorization granularity** — role checks are frontend-gated; `/ext/api` is the only server-enforced gate. Target must add server-side RBAC/scope checks.

---

*Sources: 04-api-catalog.md (full dump), _discovery/agent-04/findings.md, _discovery/agent-03/api-map-per-file.md + api-endpoints-normalized.txt, _discovery/reconciliation-notes.md, CrudOperationsFilter.java, IntegrationOperationsFilter.java, integration/mappings.json, 03-legacy-frontend-inventory.md.*
