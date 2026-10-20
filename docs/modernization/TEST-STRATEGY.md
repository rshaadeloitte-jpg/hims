# TEST-STRATEGY.md — Behavioral Safety Net for the iHMS Modernization

> Synthesized from: 06-business-rules-catalog.md (36 BR-* rules w/ test cases), _discovery/agent-06/findings.md (workflow traces), 04-backend-architecture.md + _discovery/agent-04/findings.md (3,060 endpoints, generic CRUD filter), 05-database-catalog.md + _discovery/agent-05/findings.md (719 procs, 30 live), 07-integrations-and-search.md (Solr, Redis, 28 integrations), 08-security-rbac-audit.md (frontend-only RBAC), 03-legacy-frontend-inventory.md (JS business-logic hotspots), _discovery/reconciliation-notes.md (222 unmatched URLs).
> Purpose: prove behavioral equivalence of the target system against the legacy at every stage of migration. Research/planning only — no code changes.

---

## 1. Test Pyramid Strategy

Legacy scale: **3,060 backend endpoints** (3,003 CXF JAX-RS + 57 ABDM/HFR callbacks), **~3,857 frontend-called URLs**, **719 stored-proc artifacts**, **~843 tables**, **28 integrations**, **40 modules / 533 routes** (agent-03/04/05). A pure end-to-end approach is economically impossible; a pure unit approach cannot capture cross-module proc/trigger-free logic. The pyramid is inverted relative to classic practice because the *contract* (the API/DB behavior) is what must be preserved.

| Layer | Share | What it proves | Primary tooling |
|---|---|---|---|
| L0 — Golden DB snapshots | 5% | Schema + reference data + sequence state equivalence (Oracle vs PG) | dumps, data-diff scripts |
| L1 — Contract tests (record/replay) | 35% | Request/response equivalence per endpoint family against the legacy server | recorded fixtures + replay harness (see §3.1) |
| L2 — Characterization tests (UI-level) | 25% | FAUG screen behavior ported 1:1 (JS business logic + schema-driven rendering) | browser automation (see §3.2) |
| L3 — DB golden tests (proc & SQL) | 20% | Stored-proc and native-SQL results before/after | Oracle↔PG dual-run + diff (see §3.3) |
| L4 — Unit tests (new code only) | 10% | New-service internals (pure functions, converters, validation) | JUnit/TestNG on target stack |
| L5 — Integration + non-functional | 5% | Integration matrix, Solr parity, load/concurrency, security | per-integration harness (see §3.4-3.5) |

**Guiding principles**
1. **Legacy is the oracle.** Every equivalence question is answered against a running legacy (QA Oracle at `10.10.7.63:1521/orcl` — `05-database-catalog.md:9`) or its captured artifacts. When legacy output is itself suspect (e.g. `BR-BILL-005` non-self concession overwrite, `DNR_REJECTED==DNR_DONATED==5`), the test captures current behavior first (characterization), then the divergence is a *documented decision*, not a silent fix.
2. **The generic CRUD filter is a first-class API family.** 1,109 frontend URLs route through `CrudOperationsFilter` → `GenericServiceImpl.crudadd/crudupdate/cruddelete/crudget(All)` over **659 `@CrudApi` entities** (reconciliation-notes.md:19). Entity CRUD contract tests are required per entity group, not per endpoint.
3. **Test both sides of every state machine.** Status codes are numeric, monotonic-update-guarded, and split across tables (VisitStatus on T_Appointment *and* T_Encounter) — every BR-* with a status effect gets state-transition tests on both tables.
4. **Nothing is deleted during discovery.** Tests are written against current behavior; dead code (see DEAD-CODE-CANDIDATES.md) is tested only to prove non-regression of removal.

---

## 2. Per-Business-Rule Test Derivation

Standard test categories (applied to every BR-*): **P**=Positive, **N**=Negative/error, **B**=Boundary, **Perm**=Permission/authz, **ST**=State-transition, **DI**=Data-integrity (rollback/consistency), **I**=Integration (Solr/proc/async/external). Existing test cases from the catalog (`06-business-rules-catalog.md`, "Tests:" lines) are standardized below; `[catalog]` marks cases already listed by agent 06, everything else is derived from the rule text/evidence.

### 2.1 Patient Registration (BR-PAT-001..003)
| Rule | P | N | B | Perm | ST | DI | I |
|---|---|---|---|---|---|---|---|
| BR-PAT-001 MRN generation | series-on valid; temp-MRN `<prefix>-<seq>`; offline client-MRN preserved `[catalog]` | invalid series MRN rejected | temp MRN padding 1/2/3 digits; `patientId→9-digit pad` (ddMMYY+9) `[catalog]` | registerPatient requires valid token (aspect auth) | temp→permanent promotion at visit/admission (`EmergencyAndWalkinVisitServiceImpl.java:678-684`) | MRN collision → BERA011 `[catalog]`; `IsTempMrno` flag persisted on T_Registration | sequence function `cims_getlatestautocodeandincremetitbyone('T AppoinmentTempMrn')` concurrency (2 parallel registrations → distinct MRNs) |
| BR-PAT-002 Duplicate identity | unique patient passes `[catalog]` | same name+mobile+dob → BERA011 `[catalog]`; same healthIdNumber → BEXA040 `[catalog]` | — | — | — | Solr hit must map to same T_Registration row | Solr `patient_risl_qa` lookup `findPatientByIdentity` (duplicate detection must survive Solr retirement — test both Solr-on and Solr-off) |
| BR-PAT-003 Family coupling | familyIdNo+memberId both present `[catalog]` | familyIdNo without memberId → BERP007 `[catalog]` | — | — | — | — | — |

### 2.2 OPD (BR-OPD-001..007)
| Rule | P | N | B | Perm | ST | DI | I |
|---|---|---|---|---|---|---|---|
| BR-OPD-001 Non-residential tariff | resident → 2L; non-resident+config → `config.nonresidentialtariffId` `[catalog]` | — | non-resident, config null → falls back to 2L `[catalog]` | — | — | tariff recorded on T_PATIENTSPONSOR | — |
| BR-OPD-002 Slot collision | slot free `[catalog]` | same-hour same-dept → BERA028 `[catalog]`; same-day same-dept active encounter → BEXA029 `[catalog]` | appointments ≥30 min apart (`:563`); slot default 15 min (`config.appointmentslot`) | — | appointment→encounter creates VisitStatus initial row | no orphan appointment on failed encounter | telemedicine variant (`createAppointment` telemedicine) |
| BR-OPD-003 Visit number template | OPD `V/<PREFIX>/<YY>/<00001>`; emergency `EV/...` `[catalog]` | — | seq > 99,999 → 6 digits `[catalog]` | — | — | sequence per unit (`Unitmaster.getOPDSequenceName`) — no cross-unit collision | — |
| BR-OPD-004 Visit-type sequence machine | new patient seq=1 `[catalog]` | — | revisit within `encountervalidity`+1 days → next seq; after validity → restart `[catalog]` | — | revisit within validity advances; emergency → `isEmergency=true` type | T_Visit/T_Encounter rows consistent | — |
| BR-OPD-005 Sponsor defaulting | company/tariff/priority defaults applied `[catalog]` | missing plan → BERP008 `[catalog]` | — | — | — | `CIMS_ADDPATIENTSPONSOR_NEW_DOC` row + defaults | proc result parity |
| BR-OPD-006 Walk-in doctor | doctor present `[catalog]` | no doctor → BERA022 `[catalog]`; unit-head resolves → BERA029 path | — | — | — | — | — |
| BR-OPD-007 Payment advances VisitStatus | settled → `T_Appointment.VisitStatus=11` `[catalog]` | — | VisitStatus already 12 → unchanged `[catalog]`; `<11` guard (to_number in one path `AddPaymentServiceHelper.java:477`) | — | 11 is checkout/kiosk "billed" signal (`getLastVisitStatus` reads T_Appointment) | **split-brain:** T_Encounter `isCheckout=true` + Solr update in same tx | Solr encounter doc update after payment; kiosk queue reads |

### 2.3 IPD (BR-IPD-001..005)
| Rule | P | N | B | Perm | ST | DI | I |
|---|---|---|---|---|---|---|---|
| BR-IPD-001 Admission preconditions | clean admission `[catalog]` | no admitting doctor → BERA022; no registration (direct off) → BERB009; active admission exists → BERB013; baby w/o cradle ward → BERB012 `[catalog]` | — | — | — | no T_Admission row left on failure | `CIMS_ADDADMISSIONINFORMATION` + ambulance carry-over (T_AmbulanceServiceDetails copy ≤6h) |
| BR-IPD-002 Bed allocation | first-fit same ward+building+category `[catalog]` | none free → BEXB001 `[catalog]` | **concurrent claims**: two tx claim same bed → one wins (atomic CAS `UPDATE ... WHERE OCCUPIED=false`) `[catalog]` | — | allocation on/off → interim (unbedded) admission path | bed `OCCUPIED` flag + T_ADMISSIONDETAILS consistent | `CIMS_UPDATEBEDOCCUPIEDFLAG` parity |
| BR-IPD-003 Bed status release gate | release status ok `[catalog]` | `isrelease=false` → BERB005 `[catalog]` | master-data change breaks semantics (R5) — pin contract | — | — | — | — |
| BR-IPD-004 IPD number template | `A/<PREFIX>/<YY>/<00001>` `[catalog]` | — | seq overflow → 6 digits | — | — | — | **dead check**: Java `resultStatus==-1 → APP015` cannot fire (proc ends 0) — remove in target, test admission uniqueness relies on BERB013 pre-check (R4) |
| BR-IPD-005 Interim abscond | 3-day threshold `[catalog]` | — | exactly 3 days (`<=`) absconds `[catalog]` | — | ISCANCEL=1 + DISCHARGETYPEID + ABSCONDDATETIME | BEDID=0/null + ISCANCEL=0 predicate | job `${interimAdmission.abscond.schedule}` fires correctly; trigger-free (0 triggers) — logic is job-only |

### 2.4 Discharge (BR-DIS-001..003)
| Rule | P | N | B | Perm | ST | DI | I |
|---|---|---|---|---|---|---|---|
| BR-DIS-001 Discharge preconditions | clean discharge `[catalog]` | no advised discharge → BERB023; dates < admission → err; pending amount >0 (excl. gate-pass) → BERB015; cradle w/ unfinalized bill → BERB011; photo/doc/feedback not a set → BERB023/24/25; each violation `[catalog]` | discharge date == last charge date (≥ boundary) | — | advise (duplicate → BERB018; re-advise updates Status=true) → discharge | zero-bill discharge path builds full final bill (R6: any charge without before-finalization snapshot breaks restore) | transfer-to-other-hospital triggers inter-hospital admission request + COVID referral |
| BR-DIS-002 Discharge execution & death | normal; death; baby delink `[catalog]` | — | — | — | ISDischarged 0→1 + IsClosed '0'→'1' (+ T_TransferRequest closed); death → `T_Registration_Details.isdeath=1`, T_Discharge IsDeath/DeathDate/HandOvrDate | T_Discharge insert + PatientDeathDetails + Solr `death=true` (encounters NOT cleared on death) | Solr clearClosedEncounters (unless death); ABDM discharge summary queue entry |
| BR-DIS-003 Bed release | normal → OCCUPIED=false + BedStatusId=6 `[catalog]` | — | universal (amenities) bed restored to origin; cradle re-mapped `[catalog]` | — | — | BedStatusId literal 6 in Java vs master-driven `isrelease` (R5 drift risk) | — |

### 2.5 Billing (BR-BILL-001..008)
| Rule | P | N | B | Perm | ST | DI | I |
|---|---|---|---|---|---|---|---|
| BR-BILL-001 Bill creation & numbering | draft header (Status=true, IsSettled=false, BillNo placeholder, CashCounterId=1); final number from `getLatestAutocodeSeq` `[catalog]` | — | sequence formatting boundary | — | InterORFinal interim→final | net = Total−Tax−Concession recomputed at finalize; IsCancelled=0 default | `CIMS_ADDBILL` parity |
| BR-BILL-002 Doctor share | share>0 → doctorShare=total×%/100, hospital=total−share `[catalog]` | — | share=0 → no row; OT service (`otProcedure=true`) → none `[catalog]` | — | IsBilled=false → later billing | CEILING (DB) vs HALF_UP (DTO) rounding — golden test both | `CIMS_ADDDOCTORSHARE` parity |
| BR-BILL-003 Patho/Rad auto-order | patho bill → booking + 1 detail per `M_PathoTestSample`; rad analog `[catalog]` | — | 0 samples `[catalog]`; non-patho → none `[catalog]` | — | — | SampleCollectionNO=1..n, SampleNo=0, IsOutSourced=false, PathologistId=0 | `CIMS_BILLFORINVESTIGATIONNEW` result 0 gates; `CIMS_GETPATHOTESTLISTFORBILL` parity |
| BR-BILL-004 Tax calc | before-concession; after-concession `[catalog]` | — | no taxId in master → 0 `[catalog]`; `allowNumberOfTaxCount=2` (2 of 3 labels) | — | — | TaxAmount=Σ taxN; added to SelfAmount | Taxmaster.applyafterconcession golden diff |
| BR-BILL-005 Co-pay/concession split | self-company; non-self-company `[catalog]` | — | deductable 0 → 100 (full self-pay) | — | — | **divergence test**: self company ADD plan%+bill%; non-self REPLACES (R2) — must be golden-mastered and decision-logged | sponsor re-pricing after bill (findAllServicesForBill) |
| BR-BILL-006 Settlement & checkout | full payment → settled + isCheckout `[catalog]` | — | partial → unsettled `[catalog]`; balances ≤0 threshold | — | OPD → VisitStatus=11 | `CIMS_UPDATEBILLFORCONCESSION`; changeAmount (`invstopdpostbilling`) | Solr encounter update; receipt no template |
| BR-BILL-007 Advance adjustment FIFO | full advance; partial; multiple order `[catalog]` | — | — | — | — | balance>0 advances consumed in id order; consumed recorded paymentMode=7 | `CIMS_ADDPAYMENTADVANCEDETAILS` parity |
| BR-BILL-008 Refund/cancel restore | advance refund (txn 3); bill cancel restore (txn 4) `[catalog]` | zero advance balance → BEXAR002 `[catalog]` | — | — | cancelbill marker; T_Advance Used−/Balance+; charges `*BeforeFinalization` + IsCancelled; T_Itemsale BillID=0/IsBilled=false; doctor-share status=false | restore amounts must equal pre-finalization snapshot | T_Charges temp/consumed interplay |

### 2.6 Pharmacy / Pathology / OT / Blood Bank
| Rule | P | N | B | Perm | ST | DI | I |
|---|---|---|---|---|---|---|---|
| BR-PHAR-001 Dispense completes visit | full dispense → VisitStatus=12 `[catalog]` | — | last item sale triggers 12 `[catalog]` | — | detail DISPENSED=true → item-sale DISPENSED=true → visit 12 (monotonic `<12` guard) | all details of all item sales dispensed | kiosk `getLastVisitStatus` sees 12 |
| BR-PHAR-002 Prescription closure on sale | full qty → IsClosed='1', PendingQty=0 `[catalog]` | — | partial → PendingQty−=qty `[catalog]`; compound details closed | — | isverified/isdispensed/ispartialdispense flags | master IsClosed='1' | `CIMS_ADDITEMSALESDETAILS_NEW` parity (BatchId) |
| BR-PATH-001 Sample locks removal | no sample → cancel ok; refund-config → cancel ok `[catalog]` | collected (isaccepted=true) w/o config → LEX001 `[catalog]` | — | — | booking ISCANCELLED=true; nursing orderstatus=DISCONTINUE; procedure/EMR status=false | charge row ISCANCELLED=true | Solr/EMR side-effects after cancel |
| BR-OT-001 OT scheduling prereqs | admitted; OPD-visit-only `[catalog]` | neither → BEOT004 `[catalog]` | — | — | schedule/OT-details/procedure rows created | dependent-surgery discount%; procedure rate 0 if tariff missing | `CIMS_ADDUPDATEPATIENTPROCEDURESCHEDULEMASTER` + OT details procs |
| BR-OT-002 OT pre-billing | pre-bill on → consumed=true direct charge `[catalog]` | — | — | — | — | doctor-share skipped; `isOtProcessDone=false` filter | ordering via T_EMR_Services + tempchargeid link |
| BR-BLD-001 Requisition status machine | full cross-match → 2; full issue → 3; cancel/expire → 10 `[catalog]` | — | partial cross-match stays 1 `[catalog]`; cumulative vol ≥ ordered vol threshold | — | 1→2→3 (issue); 1→10 | **header/details asymmetry** (R7): job sets details=10, header constant 10 only at creation/cancel | cross-match count + SMS; notification |
| BR-BLD-002 Auto decross 48h | <48h untouched; ≥48h decrossed `[catalog]` | — | exactly 2880 min `[catalog]` | — | crosSMatched=false, req→1, count−1, reason=0 | BloodBagDecross row | daily 00:00 job `bloodbag.auto.decross` |
| BR-BLD-003 Bag expiry | expiring today → isbagexpired=true `[catalog]` | — | already-issued skipped; crossmatched → decross+req→1 `[catalog]` | — | — | — | daily 23:00 job |
| BR-BLD-004 Requisition expiry | expired → details 10 `[catalog]` | — | `requesteddate + orderValidationPeriod(h) ≤ sysdate`; collected → untouched `[catalog]` | — | isorderexpired=true, reason "Order is expired" | **UNKNOWN-011**: `orderValidationPeriod` value source (QA props:72h, risl artifact:24h/72h dup) — test must use the deployed value | job `${bloodorder.cancellation.schedule}` |
| BR-BLD-005 Donor auto-cancel | — | — | 72h boundary `[catalog]`; post-med-exam statuses untouched `[catalog]` | — | DNR_NEW(1)/DNR_FIT(2) → 7 | DNR_REJECTED==DNR_DONATED==5 collision (A8) — status assertions must distinguish meaning by context | job `${donor.cancellation.schedule}` |

**Permission dimension (all rules):** given the aspect-based auth (agent-04 §8) with **per-path check commented out** (`ServiceLayerAspect.java:421-426`, agent-08 §3.1), permission tests prove: (a) unauthenticated → 401; (b) any valid token → endpoint executes regardless of role (documents current no-authz behavior as characterization, to be reverted to enforcement in target); (c) `@NotSecure`/`@Login` paths run as fake user id=1 (audit contamination — assert ADDEDBY).

---

## 3. Test Harness Design

### 3.1 Contract tests against legacy (record/replay) — L1
- **Fixture capture**: a capture proxy in front of the legacy REST bus records `(method, path, query, headers, body) → (status, headers, body)` per request. Because there are **3,060 endpoints**, capture by **endpoint family**: key every fixture by `classPath/methodPath` (agent-04 §2 table = the family list) rather than by full URL. Target capture of ≥1 representative request/response per family (132 families) + edge variants per BR table §2.
- **Fixture store**: `_discovery/contract-fixtures/<family>/<case>.{req,res}.json` with PII anonymized (see §4). Store the legacy response as the *golden*.
- **Replay**: target service test double — same path/query/body in → assert status, JSON shape (field-by-field on `views/` DTO fields — 155 view files define the wire format), error codes (BEXA*/BERB*/BERA*), and side-effect DB deltas (see L3).
- **Generic CRUD family**: 1,109 FE URLs route via the CRUD filter over 659 `@CrudApi` entities (reconciliation-notes.md:19). Generate contract tests **per entity group** (master add/get/update/delete + `crudgetAll` pagination) from the `@CrudApi`/`@CrudCached` annotation set; assert the CRUD filter's `__crfls/__crfli/__crflm` param handling (`CrudOperationsFilter.java:76`).
- **Report dispatcher family**: `reports/<reportName>` is served from `int.files.path/reports/*.json` + Nashorn `reports.js` (IntegrationOperationsFilter:269-278) — capture per-report config + output fixture; assert the report config source of truth is preserved (see UNKNOWN-013).

### 3.2 Characterization tests for JS business logic — L2
- Legacy UI logic lives in 71 controllers + 1,308 schemas + 2,942 templates (agent-03 §1.1). Hotspots to characterize *first* (agent-03 §3): billing math (`calculateTotalBillAmount`, 3-tax/concession/self-nonself), age/DOB two-way (age ≤115), validation.js rules, schema `dynamicAspects` merge semantics (open question agent-03 §6.7), Aadhaar base64 encoding.
- **Browser automation of FAUG screens** against the legacy: Playwright/Puppeteer script per flagship screen (walk-in visit, OPD billing, admission, discharge, dispensing — the 12-point screens in agent-03 §2) driving the legacy UI, recording: computed totals, validation messages, route/state transitions, API call sequence (via w-api-call.js bridge interception).
- Output = *characterization scripts* replayed against the React target: same actions → same visible values + same API sequence + same gvars. Where the target intentionally diverges (e.g. server-side totals), the divergence must be a documented decision with a golden-master test (BR-BILL-005).
- i18n: `data-translate` dictionaries are backend-served — assert label parity per locale (`ls_locale_language`).

### 3.3 Database golden tests — L3
- **Proc golden tests**: for each of the **30 procs live via `namedStoredProcQ()`** (agent-05 §6.1) run against Oracle with fixed input fixtures → capture result sets + table deltas; port to PG (plpgsql + REFCURSOR) and diff. Extend to the ~35 procs evidenced in agent-06 workflow traces (`CIMS_ADDPATIENTSPONSOR_NEW_DOC`, `CIMS_ADDADMISSIONDETAILS`, `CIMS_ADDBEDDISCHARGE`, `CIMS_ADDPAYMENT*`, `CIMS_ADDITEMSALESDETAILS_NEW`, `CIMS_BILLFORINVESTIGATIONNEW`, etc.).
- **Native-SQL golden tests**: ~436 `getQ(` call sites + ~2,000 SQL literals; priority = the 9 dual-dialect-branch sites (agent-05 §5.3) and the Oracle-construct hotspots (TRUNC/SYSDATE/ROWNUM/MERGE/LISTAGG/DECODE — agent-05 §5.2, §10). Unit-run each translated SQL against PG with identical data → row-set diff.
- **Sequence golden tests**: per-table `<TABLE>_SEQ` recreation (`05-database-catalog.md:89-97`) — assert `NEXTVAL` continuity (start at Oracle `LAST_NUMBER`) and no collision with existing data (risk: sequence ID collision, RISK-REGISTER).
- **Date semantics**: Oracle `TRUNC(SYSDATE)` vs PG `date_trunc` boundaries (85 sites in ApplicationJobs) — golden tests around midnight/UTC boundaries (times are GMT-written).
- **Boolean semantics**: `Boolean` ↔ char `'1'/'0'` ↔ PG `'t'/'f'` (agent-05 §10.9) — golden tests on every status write path (T_Encounter, M_BEDMASTER.OCCUPIED, ISDEATH, etc.).

### 3.4 Solr index verification tests
- Per collection (patient, encounter, item, icd10cm, healthfactor — 5 live; `service_risl_qa` orphaned per agent-07 §1.2): index-parity test — after identical DB writes, assert equivalent docs and field values (use the doc models in `dto/solr/*`, field lists documented in agent-07 §1.2).
- **Query-equivalence tests**: the 15 SolrUtil search methods (`searchPatientInSolr`, `searchPatientInEncounterForCheckinCheckout`, `searchExactPatientInSolr`, ABDM HID matching with Levenshtein ≤3 / DOB ±1yr…) — replay same query against old + new index, assert top-N + boosts semantics (`mrno^18, mobile^16...` bq weights are load-bearing, agent-07 §1.7).
- **Failure-mode tests**: Solr down → empty results (silent degradation, agent-07 §1.6) — assert target degrades *loudly* with fallback (circuit breaker) or documented same behavior.
- **Retention test**: `solr.encounter.retain.days=3` rolling window — assert closeDailyEncountersJob behavior; target decision on archive collection (record-retention risk).
- **Rollback compensation test**: tx failure → Solr doc + Redis keys deleted (`ServiceLayerAspect:1164-1182`) — assert no orphan docs.

### 3.5 Integration test matrix
Per integration in agent-07 §3.1 (28 integrations), a row: | Integration | Direction | Contract test (fixture) | Simulator/stub | Failure-injection | Notes |. Key entries:
| ABDM (HIP/HIU/HFR/ABHA) | both | capture callback + request payloads (JWE) | ABDM sandbox | timeout/5xx → retry jobs (5s fixedDelay, node-sharded counters) | milestones M1/M2 gating |
| HCX | out | JWE encrypt/decrypt round-trip (key files) | HCX sandbox | — | `abdm.keys.file.path` |
| Aadhaar/Janaadhaar/biometric | out | capture + replay | mock provider | outage blocks registration (sync-in-tx risk) | — |
| e-Mitra | both | kiosk flow | mock | — | merchant-code RISLTEST test values |
| EJS | both | incentives calc golden | — | — | `ejs.asha.incentive.schedule` |
| Chiranjeevi | out | — | stub | — | INSURANCESCHEMEID=2 |
| RGHS + SIMS | both | — | stub | — | nightly push |
| e-Aushadhi | both | SOAP-style Basic-auth | stub | ack DTOs + `reTriggerEAushadhiIndent` AOP | hard-coded creds (risk) |
| EHR/EWD | out | file generation parity | — | currently disabled — characterize, then decide | duplicate `ehr.sync.schedule` (UNKNOWN) |
| HL7 v2 inbound | in | fixture messages ADT/ORM/ORU/DFT/REF/OMP → Inboundhis drain | — | job disabled in checked-in source | dormant — verify at deployment |
| HL7 outbound | out | Outboundhis row parity after each clinical event | HL7 sink | — | no active sender found |
| FHIR R4 server | in/out | Patient resource GET | — | — | bean w/o servlet registration (UNKNOWN-009) |
| TIUXML | out | file output diff | — | — | — |
| MinIO | out | upload/download | MinIO container | disabled by default → filesystem fallback parity | — |
| eSign | out | — | stub | — | no Java caller (UNKNOWN-010) |
| SMS/WhatsApp | out | queue drain + retrigger (max 5) | stub | — | `triggerWhatsappReportsInQueue` |
| SSO | in | token validation + `__token` cookie | stub SSO | — | no server-side password check |
| Print server | out | ws:// print payload | stub | — | — |
| Backman DxH 500 | in | OLIS jar — runtime wiring unknown | vendor | — | UNKNOWN |
| Jeevanvahini / Mera Aspataal / eRaktkosh / NMC / PCTS / Kaysons / scan-print / Bhamashah / eUpkaran / EXT LIMS | out | fixture capture | stubs | — | EXT LIMS disabled in checked-in config (`ISEXTLMISENABLED` DB flag) |

---

## 4. Test Data Strategy

- **Oracle → PG fixture pairs**: for every golden test, extract a *minimal deterministic dataset* from the QA Oracle (schema owner `IHMSDOCITDEVMODS_PROD_V1`, `10.10.7.63:1521/orcl`) via fixed seed SQL; load the identical dataset into PG (same IDs, same sequence states). Record source query + row count per fixture.
- **Anonymization**: patient-facing data (T_REGISTRATION family, T_Encounter, Solr docs, HL7/ABDM payloads) is anonymized at fixture capture: names→deterministic pseudonyms (deterministic = same input → same output, preserving duplicate-detection test semantics), mobile/aadhaar/ABHA → valid-format dummy numbers with parity checksums preserved where the code validates them (validateAdharNumber, validateMobileNoByAPI), DOB shifted by a fixed offset (preserves age-rule boundaries), `healthIdNumber` uniqueness preserved (BR-PAT-002). **Never commit raw PII fixtures** (08-security: secrets + PII in repo is an existing risk — do not extend it).
- **Secrets handling**: fixtures must not contain the live DB password / LDAP / SSO / MinIO / eAushadi / SSO-mobile-key values present in application.properties (08-security §9 #6). Use env-injected overrides.
- **Sequence-state fixtures**: per-table sequence `LAST_NUMBER` captured alongside data (sequence ID collision risk — RISK-REGISTER).

---

## 5. CI Gates

| Gate | Where | Enforced at | Fail criteria |
|---|---|---|---|
| G1 Lint/build (target) | PR | every commit | compile/lint errors |
| G2 Contract replay (L1) | nightly | feature branch merge | any fixture mismatch beyond recorded tolerance; new endpoint families without fixtures fail (coverage gate: ≥1 fixture per family, 132/132) |
| G3 DB golden (L3) | nightly + on schema change | schema migration PR | row-set diff ≠ 0 |
| G4 Solr parity | nightly | Solr config PR | query top-N mismatch |
| G5 Characterization smoke (L2) | nightly (5 flagship screens) | UI PR | visible-value or API-sequence drift |
| G6 Integration matrix | weekly | integration PR | stub-vs-fixture mismatch; missing failure-injection case |
| G7 Regression risk gate | nightly | any PR | BR-* rule tests (from §2) executed against target — any rule whose tests are not yet ported blocks the affected module |

Coverage thresholds (targets): 100% of 36 BR-* rules have ≥1 automated test; 100% of 132 endpoint families have ≥1 fixture; 30 live procs 100% golden-tested; 5 Solr collections 100% parity-tested; 659 CrudApi entities ≥90% covered by generated entity-group contract tests.

---

## 6. Regression Test Prioritization by Module Risk

Risk score = evidence-based (module size + tx load + cross-module coupling + state-machine density). Ordered list:

| Priority | Module | Risk basis (evidence) | Tests to run on every migration increment |
|---|---|---|---|
| 1 | Billing (OP/IP/payment/refund/doctor-share) | BR-BILL-001..008; ~2,800 LOC helper; cross-module mega-tx (order→bill→lab→rad→share→EMR→HL7, agent-06 §1.5); R2 concession asymmetry; R6 restore risk | BR-BILL-001..008 full set + golden procs CIMS_ADDBILL/CIMS_ADDPAYMENT*/CIMS_ADDDOCTORSHARE + billing JS math characterization |
| 2 | Admission/IPD + bed allocation | BR-IPD-001..005; CAS bed claim; R4 dead APP015 path | BR-IPD-001..005 + bed-claim concurrency + abscond job |
| 3 | Discharge | BR-DIS-001..003; death handling; Solr clearing | BR-DIS-001..003 + death/baby paths + zero-bill discharge |
| 4 | Pharmacy dispensing | BR-PHAR-001..002; VisitStatus 12 split-brain | BR-PHAR-001..002 + CIMS_ADDITEMSALESDETAILS_NEW + kiosk getLastVisitStatus |
| 5 | Patient registration + identity | BR-PAT-001..003; Solr duplicate detection; T_REGISTRATION native-only (agent-05 §5.4) | BR-PAT-001..003 + Solr patient search parity + temp-MRN promotion |
| 6 | OPD visit + appointment | BR-OPD-001..007; slot collisions; visit-status machine | BR-OPD-001..007 + slot CAS + telemedicine |
| 7 | Blood bank | BR-BLD-001..005; 4 daily jobs; DNR collision | BR-BLD-001..005 + job boundary tests (2880 min, 72h, 23:00) |
| 8 | Pathology/Radiology orders | BR-PATH-001, BR-BILL-003 | charge-removal locks (LEX001) + auto-order creation |
| 9 | OT | BR-OT-001..002 | scheduling prereqs + pre-billing |
| 10 | Master data + generic CRUD | 659 CrudApi entities; 264-endpoint MasterCachedServiceImpl god class | generated entity-group contract tests + cache invalidation |
| 11 | Nursing / Inventory / Patho god services | 300/235/133 endpoints, single-tx files (agent-04 §9 #2) | fixture per family + long-tx lock tests |
| 12 | Jobs (55 schedules) | ApplicationJobs 121 raw SELECTs, 14 tx methods (agent-04 §5) | job boundary tests (abscond, decross, expiry, donor, encounter close) |
| 13 | Integrations | agent-07 §3 matrix | per-matrix row per increment |
| 14 | Security (auth/session) | agent-08 §9 | token expiry/sliding renewal, single-session eviction, secretTokenForOnlineAppointment behavior captured as characterization |
