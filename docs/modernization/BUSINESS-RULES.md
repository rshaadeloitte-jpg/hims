# BUSINESS RULES — Consolidated Catalog (iHMS / OHUM PAS NG)

**Synthesized from:** `06-business-rules-catalog.md` (primary, BR-PAT-001..BR-BLD-005) + `_discovery/agent-06/findings.md` (workflow traces, status evidence) + `04-backend-architecture.md`, `04-api-catalog.md`, `03-legacy-frontend-inventory.md`, `05-database-catalog.md`, `10-target-architecture.md`.
**Date:** 2026-08-14 | **Mode:** Synthesis-only — no code re-exploration, no code changes.
**ID preservation:** All 36 original rules from the 06 catalog keep their exact `BR-*` IDs. Rules marked **(ADDED — agent-06 findings)** originate from `_discovery/agent-06/findings.md` and have been backported into `06-business-rules-catalog.md` + FEATURE-MATRIX (2026-08-22); each is backed by explicit evidence in `_discovery/agent-06/findings.md`.
**Layer legend:** JS = FAUG client (`public/js/*.js`), API = CXF JAX-RS endpoint, SVC = service impl (Java), DB = Oracle proc / SQL (`generated-procs/*.sql`), INT = Solr/HL7/ABDM/MinIO side effect.

---

## BR-PAT-001 — Patient MRN generation strategy
- **Module**: Patient Registration
- **Trigger**: `registerPatient` (new patient) — `POST api/rest/patient/registerPatient` (`pas/PatientServiceImpl.java:3519`, batch, `@Transactional(rollbackOn=Exception.class)`)
- **Conditions**: `mrnoFromSeries` / `autoGenerateTempMrnNo` properties; appointment vs walk-in flag; offline-unit flag (`unit.getIsOffline()`)
- **Input**: `PatientAppointmentandRegDto.mRNO`, placeOfBirthId, unitId
- **Rule**:
  1. `mrnoFromSeries=true` → `helper.validateOrGenerateMrno(...)`; blank result → `IsTempMrno=true`.
  2. else `autoGenerateTempMrnNo=true` AND `isAppt` → temp MRN `<tempMrnPrefix>-<3-digit seq>` via SQL fn `cims_getlatestautocodeandincremetitbyone('T AppoinmentTempMrn', ...)` (pad to 3).
  3. Offline unit → client-supplied MRN preserved as-is.
  4. Permanent upgrade path: `setPermanentMrno = ddMMYY + leftPad(patientId,9,'0')` on `T_Registration_Int`, `IsTempMrno=0`; hex variant `generateHexMrno`.
- **Output**: MRN assigned on T_Registration; `IsTempMrno` flag; error via `getApplicationExceptionErrorMessage`
- **Error**: `BERA011` (MRN collision — see BR-PAT-002)
- **Source Evidence**:
  - JS: `walkinVisit-schema.json` MRN field (optional per dynamicAspects); temp-MRN badge — `03-legacy-frontend-inventory.md:81`
  - SVC: `custom/PatientServiceHelper.java:373-425` (three MRN modes), `:378` (IsTempMrno for blank series MRN), `:726-729` (`CIMS_ADDPATIENTREGISTRATION`), `:802-804` (`CIMS_ADDPATIENTREGISTRATIONDETAILS`); `BaseServiceImpl.java:987-989` (generateHexMrno), `:1007-1017` (setPermanentMrno)
  - SVC upgrade: `EmergencyAndWalkinVisitServiceImpl.java:678-684` (temp→permanent at visit), `PatientRegAdmissionServiceImpl.java:378-386` (at admission)
  - CFG: `application.properties` — `mrnoFromSeries=false`, `autoGenerateTempMrnNo=false`
- **Tables**: T_Registration, T_Registration_Int, M_MRNSeries
- **APIs**: `POST /api/rest/patient/registerPatient`
- **UI dependency**: MRN field optional; temp-MRN badge; MRN lookup typeahead (`patient-search-list.html`)
- **Migration requirement**: port auto-code infra (`cims_getlatestautocodeandincremetitbyone`) to a sequence/ID-service; keep temp→permanent promotion hooks at visit/admission; offline-unit client-MRN path must survive
- **Test cases**:
  - + series mode on: MRN generated from series
  - + appointment temp MRN padding 1/2/3-digit
  - + offline unit reuses client MRN
  - − invalid series/config → error
  - boundary: patientId → 9-digit pad; MRN collision → `BERA011`

## BR-PAT-002 — Duplicate patient identity detection
- **Module**: Patient Registration
- **Trigger**: `registerPatient` before insert
- **Conditions**: none (always checked)
- **Input**: identity fields (name/mobile/DOB/healthIdNumber)
- **Rule**: Solr lookup `findPatientByIdentity(0L, body)`; non-null hit → throw `BERA011`. Health ID (ABDM) duplicate via Solr healthIdNumber search → `BEXA040` with existing MRN.
- **Output**: registration proceeds or error
- **Error**: `BERA011` (duplicate identity), `BEXA040` (duplicate healthIdNumber)
- **Source Evidence**:
  - JS: patient typeahead search (`patient-search-list.html`, min-length triggers) — `03-legacy-frontend-inventory.md:292`
  - SVC: `custom/PatientServiceHelper.java:661-669` (BERA011), `:783-790` (BEXA040); `util/SolrUtil.java:198-203` (indexPatientInSolr, `solrEnabled` flag)
  - CFG: `application.properties` — `solr.patient.collection.name=patient_risl_qa`
- **Tables**: Solr collection `patient_risl_qa` (read); T_REGISTRATION (write)
- **APIs**: `POST /api/rest/patient/registerPatient`
- **UI dependency**: search-as-you-type identity fields; duplicate warning modal
- **Migration requirement**: duplicate-identity heuristics and healthId uniqueness must survive Solr retirement (10-target-architecture.md:62 — patient identity moves to Patient Identity service; Solr owned by Search service)
- **Test cases**:
  - + unique identity passes
  - − same name/mobile/dob → `BERA011`
  - − same healthIdNumber → `BEXA040`

## BR-PAT-003 — Family-Id / member coupling
- **Module**: Patient Registration
- **Trigger**: `registerPatient` payload containing `familyIdNo`
- **Conditions**: `familyIdNo` provided
- **Input**: `familyIdNo`, `memberId`
- **Rule**: `familyIdNo` without `memberId` → `BERP007`.
- **Output**: error or registration
- **Error**: `BERP007`
- **Source Evidence**: SVC `custom/PatientServiceHelper.java:704-706`
- **Tables**: T_REGISTRATION (family linkage columns)
- **APIs**: `POST /api/rest/patient/registerPatient`
- **UI dependency**: familyIdNo/memberId fields on walk-in schema
- **Migration requirement**: carry pair-validation into the Patient Identity service contract
- **Test cases**:
  - + both familyIdNo and memberId → OK
  - − familyId only → `BERP007`

## BR-OPD-001 — Non-residential tariff selection
- **Module**: OPD Visit (also IPD admission)
- **Trigger**: `addwalkinVisitNew`; `admitPatientInt`
- **Conditions**: patient `resStateId != config.state`
- **Input**: `resStateId`, `config.state`, `config.nonresidentialtariffId`
- **Rule**: non-resident → sponsor tariff = `config.nonresidentialtariffId`; resident → hard-coded tariff literal `2L`. Same logic in IPD admission path.
- **Output**: sponsor row with selected tariff
- **Error**: none specified (falls back to `2L` when config missing — RISK R9)
- **Source Evidence**:
  - SVC: `EmergencyAndWalkinVisitServiceImpl.java:410-419`; `PatientRegAdmissionServiceImpl.java:366-375`
  - RISK: `_discovery/agent-06/findings.md` R9 — hard-coded `2L` fallback when `config.nonresidentialtariffId` null (both OPD+IPD)
- **Tables**: T_PATIENTSPONSOR
- **APIs**: `POST /api/rest/walkinVisit/addwalkinVisit`, `POST /api/rest/patientRegAddmission/adddata`
- **UI dependency**: resStateId field on walk-in schema
- **Migration requirement**: config-driven tariff with documented fallback; the `2L` literal must be config-ized (findings R9)
- **Test cases**:
  - + resident → default tariff
  - + non-resident with config → nonresidentialtariffId
  - − non-resident without config → falls to `2L` (documented fallback)

## BR-OPD-002 — Appointment slot collision rules
- **Module**: OPD Appointment
- **Trigger**: `addwalkinVisitNew` appointment path
- **Conditions**: appointment vs walk-in; same-day; same department/unit
- **Input**: patientId, departmentId, unitId, appointmentDateTime
- **Rule**:
  - (a) Active (status=true, iscancelled=0) same-day same-dept encounter for the same patient → `BEXA029`.
  - (b) Same-day hour window (HH:00:00–HH:59:59) same dept+unit appointment → `BERA028`.
  - (c) Appointments spaced ≥ 30 min apart; slot duration = `config.appointmentslot` (default 15 min).
- **Output**: appointment created or error
- **Error**: `BEXA029`, `BERA028`
- **Source Evidence**: SVC `EmergencyAndWalkinVisitServiceImpl.java:537-546` (BEXA029), `:547-593` (window rules), `:582-589` (BERA028), `:563` (30-min spacing), `:423-426` (slot duration config)
- **Tables**: T_Encounter, T_Appointment
- **APIs**: `POST /api/rest/walkinVisit/addwalkinVisit`; `appointmentschedule/*` (slot queries)
- **UI dependency**: slot picker (`doctor-appintment-slots-schema.json`); least-full-cabin policy
- **Migration requirement**: port collision predicates to the Appointment service; keep config `appointmentslot`
- **Test cases**:
  - + slot free → booked
  - − same-hour → `BERA028`
  - − duplicate dept/date active encounter → `BEXA029`

## BR-OPD-003 — OPD/emergency visit number template
- **Module**: OPD Visit
- **Trigger**: encounter creation in `addwalkinVisitNew`
- **Conditions**: always
- **Input**: unitprefixformrno, year (YYYY−2000), per-unit sequence (`Unitmaster.getOPDSequenceName(unitId)`)
- **Rule**: `opdNo = String.format(opdNoTemplate, unitprefixformrno, YEAR-2000, %05d seq)` → template `V/%s/%d/%s`; emergency uses `emerNoTemplate` `EV/%s/%d/%s`.
- **Output**: `V/<PREFIX>/<YY>/<00001>` (or `EV/...`)
- **Error**: none
- **Source Evidence**:
  - SVC: `EmergencyAndWalkinVisitServiceImpl.java:1291-1308`
  - CFG: `application.properties` — `opd.no.template=V/%s/%d/%s`, `emer.no.template=EV/%s/%d/%s`
- **Tables**: T_Encounter
- **APIs**: `POST /api/rest/walkinVisit/addwalkinVisit`
- **UI dependency**: OPD number displayed on confirmation screens (`op:opFaceSheet`)
- **Migration requirement**: number generation must move to the Appointment & Encounter service (sequence per unit)
- **Test cases**:
  - + normal visit → `V/PRE/26/00001`
  - + emergency → `EV/PRE/26/00001`
  - boundary: seq > 99999 (5-digit overflow → 6 digits)

## BR-OPD-004 — Visit-type sequence state machine
- **Module**: OPD Visit
- **Trigger**: `addwalkinVisitNew` encounter typing
- **Conditions**: prior visits by same doctor; `Visittypemaster.encountervalidity`
- **Input**: doctorId, patientId, visitTypeId
- **Rule**: revisit by same doctor within `encountervalidity` (+1 day) advances to the next sequence visit type; no prior visit → sequence=1 type; emergency → visit type with `isEmergency=true`.
- **Output**: chosen `T_Visit` type row + `T_Encounter.VisitTypeID`
- **Error**: none
- **Source Evidence**: SVC `EmergencyAndWalkinVisitServiceImpl.java:1052-1107` (state machine `:1063-1107`)
- **Tables**: M_VISITTYPEMASTER, T_Visit, T_Encounter
- **APIs**: `POST /api/rest/walkinVisit/addwalkinVisit`
- **UI dependency**: visit-type dropdown filtered by department (walkinVisit-schema)
- **Migration requirement**: preserve visit-sequence logic (same doctor + validity window) in the new Encounter service
- **Test cases**:
  - + new patient → sequence 1
  - + revisit within validity → next sequence
  - + revisit after validity → restart at 1

## BR-OPD-005 — Sponsor defaulting on OPD visit
- **Module**: OPD Visit / Billing sponsor
- **Trigger**: encounter save in `addwalkinVisitNew`
- **Conditions**: sponsor rows provided/absent
- **Input**: companyId, tariffId, priorityId, planId
- **Rule**: Company default `config.selfcompanyid`, tariff `config.selftariffid`, `priorityId=1`; a plan is required per sponsor, else `BERP008`. Stored via `CIMS_ADDPATIENTSPONSOR_NEW_DOC`. (Registration path also applies sponsor defaults: tariffId from sponsor[0], insuranceSchemeId/planId fallback — `PatientServiceHelper.java:281-291`.)
- **Output**: sponsor row on T_PATIENTSPONSOR
- **Error**: `BERP008` (missing plan)
- **Source Evidence**:
  - SVC: `EmergencyAndWalkinVisitServiceImpl.java:1359-1365` (defaults), `:1729-1794` (proc call); `PatientServiceHelper.java:281-291`
  - DB: proc `CIMS_ADDPATIENTSPONSOR_NEW_DOC` (30 live-called set, 05-database-catalog.md:130)
- **Tables**: T_PATIENTSPONSOR
- **APIs**: `POST /api/rest/walkinVisit/addwalkinVisit`
- **UI dependency**: patientSponsor(planId) field; eligibility button
- **Migration requirement**: sponsor defaults are a pricing prerequisite — must be explicit in Billing service contract
- **Test cases**:
  - + defaults applied (selfcompanyid/selftariffid/priority=1)
  - − missing plan → `BERP008`

## BR-OPD-006 — Walk-in requires doctor
- **Module**: OPD Walk-in
- **Trigger**: walk-in (non-appointment) path in `addwalkinVisitNew`
- **Conditions**: no appointment; `appointmentAgainstUnitHead` config
- **Input**: doctorId
- **Rule**: Non-appointment walk-in with no doctorId → `BERA022`. Exception: `appointmentAgainstUnitHead` resolves the unit-head doctor; if still unresolved → `BERA029`.
- **Output**: encounter created with doctor or error
- **Error**: `BERA022`, `BERA029`
- **Source Evidence**: SVC `EmergencyAndWalkinVisitServiceImpl.java:380-388`, `:628-630`
- **Tables**: T_Encounter, M_DOCTORMASTER
- **APIs**: `POST /api/rest/walkinVisit/addwalkinVisit`
- **UI dependency**: doctor picker cascade (dept→building→cabin→doctor)
- **Migration requirement**: unit-head resolution logic must port to the Appointment & Encounter service
- **Test cases**:
  - + doctor present → OK
  - − none → `BERA022`
  - boundary: unit-head configured → resolved; not configured → `BERA029`

## BR-OPD-007 — Appointment payment advances VisitStatus (billing → visit state)
- **Module**: Billing / OPD checkout
- **Trigger**: `addBillPayment`
- **Conditions**: OPD bill settlement; VisitStatus < 11
- **Input**: bill payment result, encounterId
- **Rule**: After OPD bill settlement, `T_Appointment.VisitStatus=11` (only if currently < 11) and, when checkout enabled, `T_Encounter.isCheckout=true` + Solr encounter update.
- **Output**: VisitStatus 11 on T_Appointment; isCheckout flag
- **Error**: none
- **Source Evidence**:
  - SVC: `AddPaymentServiceHelper.java:438-447` (checkout+Solr), `:477-478` (VisitStatus=11), `:820`
  - JS: kiosk/queue reads `getLastVisitStatus` (`api/rest/walkinVisit/getLastVisitStatus`, 04-api-catalog.md:535)
  - RISK R1: VisitStatus 11 written to **T_Appointment** here, while 3/9/10 write **T_Encounter** — two tables, same codes, different rows (`_discovery/agent-06/findings.md` R1)
- **Tables**: T_Appointment, T_Encounter, Solr `encounter_risl_qa`
- **APIs**: `POST /api/rest/addPayment/` (addBillPaymentNew)
- **UI dependency**: kiosk/queue display of "billed" state
- **Migration requirement**: **unify the visit lifecycle** — VisitStatus 11/12 split-brain is Risk #1 in the 06 catalog (C.1). One state machine per encounter/appointment pair.
- **Test cases**:
  - + settled → VisitStatus 11
  - − VisitStatus already 12 → unchanged

## BR-IPD-001 — Admission preconditions
- **Module**: IPD Admission
- **Trigger**: `admitPatient` → `admitPatientInt`
- **Conditions**: all four checks
- **Input**: doctorId, registration, admission type, ward (for babies)
- **Rule**: (1) admitting doctor required → `BERA022`; (2) if `allowDirectAdmission` off → existing registration required → `BERB009`; (3) no other active admission → `BERB013`; (4) baby admission requires a cradle ward in the unit → `BERB012`; attending doctor defaults to admitting doctor.
- **Output**: admission proceeds or error
- **Error**: `BERA022`, `BERB009`, `BERB013`, `BERB012`
- **Source Evidence**: SVC `custom/PatientRegAdmissionServiceImpl.java:249-281` (`:249-251`, `:256-260`, `:261-266`, `:267-275`)
- **Tables**: T_Admission, T_Registration
- **APIs**: `POST /api/rest/patientRegAddmission/adddata`
- **UI dependency**: admission form (`addpatientAdmissiontdto_schema.json`)
- **Migration requirement**: preconditions must be explicit in ADT service API; note RISK R4 — no DB-level guard on concurrent duplicate admissions (Java `APP015` check is dead; see BR-IPD-004)
- **Test cases**:
  - + all valid → admitted
  - − each violation → respective code (BERA022 / BERB009 / BERB013 / BERB012)

## BR-IPD-002 — Bed allocation algorithm (auto-allocation)
- **Module**: IPD Admission / Bed
- **Trigger**: `admitPatientInt`
- **Conditions**: `allowAutoAllocation` on; `fromBedId=0`
- **Input**: unitId, ward/building/category of requested class
- **Rule**: Free bed = `OCCUPIED=false` AND `status=true` AND `isnoncensus/isot/ispreop/ispostop=false`. Priority chain: (1) same ward+building+category, (2) waiting-bed, (3) floating-bed, (4) waiting+floating. Claim via atomic `UPDATE M_BEDMASTER SET OCCUPIED=true WHERE ID=? AND OCCUPIED=false`; failure → `BEXB001`. Auto-allocation off + no bed → interim ward (unbedded) admission.
- **Output**: bedId assigned or interim admission or `BEXB001`
- **Error**: `BEXB001` (no bed found)
- **Source Evidence**:
  - SVC: `PatientRegAdmissionServiceImpl.java:283-358` (allocation `:283-359`, fallback chain `:311-342`, CAS `:344`, BEXB001 `:357`, interim `:290-293`, `:1400-1402`)
  - DB: proc `CIMS_UPDATEBEDOCCUPIEDFLAG` (`:1303`)
- **Tables**: M_BEDMASTER, T_ADMISSIONDETAILS
- **APIs**: `POST /api/rest/patientRegAddmission/adddata`
- **UI dependency**: bed picker / interim admission list (`interimAdmissions.json`, `ip:showInterimAdmissions`)
- **Migration requirement**: CAS claim must stay atomic (row-level lock / conditional UPDATE) in PG; bed availability cache in ADT service
- **Test cases**:
  - + first-fit (same ward+building+category)
  - + waiting-bed fallback
  - + floating-bed fallback
  - + none → `BEXB001`
  - boundary: concurrent claims (CAS) — only one winner

## BR-IPD-003 — Bed status release gate
- **Module**: IPD Admission / Bed
- **Trigger**: bed allocation validation
- **Conditions**: bed found
- **Input**: `M_BedMaster.BedStatusId`, `M_BedStatusMaster.isrelease`
- **Rule**: Bed's `M_BedStatusMaster.isrelease` must be true at allocation → else `BERB005`.
- **Output**: allocation allowed or error
- **Error**: `BERB005`
- **Source Evidence**: SVC `custom/PatientRegAdmissionServiceImpl.java:1289-1301`
- **Tables**: M_BEDMASTER, M_BEDSTATUSMASTER
- **APIs**: `POST /api/rest/patientRegAddmission/adddata`
- **UI dependency**: bed screen status colors
- **Migration requirement**: master-driven release semantics — do NOT hard-code; see RISK R5 (BR-DIS-003)
- **Test cases**:
  - + release-status bed → allocated
  - − non-release status → `BERB005`

## BR-IPD-004 — IPD number template
- **Module**: IPD Admission
- **Trigger**: `admitPatientInt`
- **Conditions**: always
- **Input**: unitprefixformrno, year (YYYY−2000), per-unit sequence
- **Rule**: `ipdNo = String.format(ipdNoTemplate, unitprefixformrno, YEAR-2000, %05d seq)` → template `A/%s/%d/%s`; `CIMS_ADDADMISSIONINFORMATION` inserts T_Admission.
- **Output**: `A/<PREFIX>/<YY>/<00001>`
- **Error**: `APP015` — **dead path** (RISK R4): Java `resultStatus==-1 → APP015` can never fire because `CIMS_ADDADMISSIONINFORMATION` always ends with 0; admission duplicate/uniqueness relies on pre-checks (BERB013) instead of the proc
- **Source Evidence**:
  - SVC: `PatientRegAdmissionServiceImpl.java:1236-1252`, `:1265-1268`
  - CFG: `application.properties` — `ipd.no.template=A/%s/%d/%s`
  - DB: `generated-procs/CIMS_ADDADMISSIONINFORMATION.sql` (also copies ambulance details — see BR-IPD-006)
- **Tables**: T_Admission
- **APIs**: `POST /api/rest/patientRegAddmission/adddata`
- **UI dependency**: IPD number on admission confirmation / BHT barcode
- **Migration requirement**: R4 — concurrency-safe uniqueness at DB level (unique constraint on patient active admission) must be added in PG
- **Test cases**:
  - + normal → `A/PRE/26/00001`
  - boundary: seq > 99999 (6-digit overflow)
  - − (target) concurrent same-patient admission → must fail exactly once

## BR-IPD-005 — Interim-admission auto-abscond
- **Module**: IPD Admission (job)
- **Trigger**: `${interimAdmission.abscond.schedule}` scheduled job (`updateAbscondInterimAdmissionStatusJob`)
- **Conditions**: `BEDID=0/null` (interim/unbedded), `ISCANCEL=0`, `ADDEDDATETIME ≤ now − 3 days`
- **Input**: T_Admission rows
- **Rule**: mark `ISCANCEL=1` + discharge type "auto absconded" + `ABSCONDDATETIME` set.
- **Output**: admission cancelled as absconded
- **Error**: none
- **Source Evidence**: SVC `jobs/ApplicationJobs.java:991-1019` (predicate `:1008`, update `:1007-1016`; `:1015` sets ISCANCEL)
- **Tables**: T_Admission
- **APIs**: none (scheduled job)
- **UI dependency**: interim admission list shows absconded state
- **Migration requirement**: port job to scheduled task in ADT service (event-driven timeout)
- **Test cases**:
  - + >3 days unbedded → absconded
  - + bedded → untouched
  - boundary: exactly 3 days (`<=`) → absconded

## BR-DIS-001 — Discharge preconditions
- **Module**: IPD Discharge
- **Trigger**: `patientTransferDischarge/discharge`
- **Conditions**: advised discharge exists; dates; pending amounts; document set
- **Input**: dischargeTypeId, dates, photo/doc/feedback files, patient amount
- **Rule**:
  - Active advised discharge required → `BERB023`.
  - Discharge/death/handover dates ≥ admission date; discharge date ≥ last charge `addedDateTime`.
  - Pending patient amount > 0 (excluding gate-pass service) → `BERB015`.
  - Cradle babies with non-finalized bills → `BERB011`.
  - Photo + discharge doc + feedback doc must come as a set → `BERB023/024/25`.
  - Transfer-to-other-hospital type triggers inter-hospital admission request + COVID referral.
  - `validatePatientDischarge` → `BERB014`.
  - Duplicate active advised discharge → `BERB018` (`addAdvisedDischargeList`; re-advise updates Status=true; babies advised too).
- **Output**: discharge allowed or error
- **Error**: `BERB023`, `BERB024`, `BERB025`, `BERB014`, `BERB015`, `BERB011`, `BERB018`
- **Source Evidence**:
  - SVC: `custom/PatientTransferDischargeServiceImpl.java:389-509` (`:392-400` dates, `:402-405` BERB023, `:406-416` triple set, `:423-425` BERB014, `:426-438` BERB015, `:450-453` BERB011, `:454-460` last-charge date, `:463-508` transfer-to-hospital); `:907-946` (advised discharge, BERB018), `:949+` (babies advised)
- **Tables**: T_AdviseDischarge, T_Discharge
- **APIs**: `POST /api/rest/patientTransferDischarge/addAdvisedDischargeList`, `POST /api/rest/patientTransferDischarge/discharge`, `POST /api/rest/patientTransferDischarge/cancelAdvisedDischarge`
- **UI dependency**: discharge clearance tabs (clearanceTab.html); document upload
- **Migration requirement**: clearance state machine must be modeled in ADT service; keep gate-pass exclusion rule
- **Test cases**:
  - + clean discharge passes
  - − each violation → respective code (BERB023/024/025/014/015/011/018)

## BR-DIS-002 — Discharge execution & death handling
- **Module**: IPD Discharge
- **Trigger**: `patientTransferDischarge/discharge` (execution)
- **Conditions**: preconditions met (BR-DIS-001); babies discharged first (`helper.dischargeBabies` `:671-704`); zero-bill path builds a final zero bill (`:511-668`)
- **Input**: discharge type, dates, patient
- **Rule**: `CIMS_ADDBEDDISCHARGE` inserts T_Discharge; if discharge type `IsExpired=1` (death) → `T_Registration_Details.isdeath=1` (proc), `T_Discharge.IsDeath/DeathDate/HandOvrDate` updated, `PatientDeathDetails` saved, patient Solr `death=true`. `CIMS_UPDATEIPDISDISCHARGE` sets `ISDischarged=1, IsClosed='1'` (+ babies, + T_TransferRequest closed). Solr encounters cleared unless death. ABDM discharge summary queue entry.
- **Output**: discharge record; admission closed; bed released (BR-DIS-003)
- **Error**: none at execution
- **Source Evidence**:
  - SVC: `custom/PatientTransferDischargeServiceImpl.java:720-831` (`:752-769` death details, `:771-777` closure, `:782-793` bed release, `:796-803` bed remap, `:808-812` death Solr, `:820-825` clearClosedEncounters, `:881` ABDM queue)
  - DB: `generated-procs/CIMS_ADDBEDDISCHARGE.sql:24-41` (death → isdeath=1); `generated-procs/CIMS_UPDATEIPDISDISCHARGE.sql` (ISDischarged=1, IsClosed='1', T_TransferRequest)
  - INT: Solr clear/update (`solrUtil.clearClosedEncounters`)
- **Tables**: T_Discharge, T_AdmissionDetails, T_Registration_Details, PatientDeathDetails, T_TransferRequest
- **APIs**: `POST /api/rest/patientTransferDischarge/discharge`, `POST /api/rest/patientTransferDischarge/printPatientDeathForm`
- **UI dependency**: discharge summary templates (`Common_Dis_Form`, death reports `ip:cdrForm4A/4B`)
- **Migration requirement**: discharge must emit PatientDischarged event (10-target-architecture.md:30); death flag propagation to patient registry + Solr via event
- **Test cases**:
  - + normal discharge → ISDischarged=1, IsClosed='1'
  - + death → isdeath=1, PatientDeathDetails saved, Solr death=true
  - + baby delink/decouple

## BR-DIS-003 — Bed release on discharge
- **Module**: IPD Discharge / Bed
- **Trigger**: discharge execution
- **Conditions**: bed assigned
- **Input**: bedId
- **Rule**: Bed `OCCUPIED=false` + `BedStatusId=6` (via `CIMS_UPDATEBEDOCCUPIEDFLAG`); universal (amenities) bed restored to origin; cradle (non-census) bed re-mapped to cradle ward.
- **Output**: bed available
- **Error**: none
- **Source Evidence**:
  - SVC: `custom/PatientTransferDischargeServiceImpl.java:779-803` (`:790` BedStatusId=6, `:796-798` universal restore, `:800-803` cradle re-map)
  - DB: proc `CIMS_UPDATEBEDOCCUPIEDFLAG` (occupied=false)
  - RISK R5: `BedStatusId=6` is a hard-coded literal in Java SQL while allocation validates against master `isrelease` — master-data change breaks release semantics; contract must be pinned (`_discovery/agent-06/findings.md` R5; 06-catalog C.4)
- **Tables**: M_BEDMASTER, M_BEDSTATUSMASTER
- **APIs**: `POST /api/rest/patientTransferDischarge/discharge`, `PUT /api/rest/patientTransferDischarge/releaseBed`
- **UI dependency**: bed board color changes
- **Migration requirement**: pin the released-status contract (either enforce via master or make constant config) — Risk #4 in 06 catalog
- **Test cases**:
  - + normal bed → released
  - + universal bed → restored to origin
  - + cradle bed → re-mapped to cradle ward

## BR-BILL-001 — Bill creation & numbering
- **Module**: Billing
- **Trigger**: `addservicecharge` / bill finalization
- **Conditions**: charge addition or finalize
- **Input**: services, unitId, encounter/admission
- **Rule**: Header created with `Status=true, IsSettled=false, BillNo='000000000000000'` placeholder, `CashCounterId=1`; final bill number from `getLatestAutocodeSeq(unitId, "T_OPDBill", opBillNoTemplate)` (IP variant separately); net amount recomputed at finalize as `TotalBill − TotalTax − TotalConcession`; `CIMS_ADDBILL` inserts (defaults `IsCancelled=0`, `IsPharmacy=0`); OPD `T_ITEMSALE` ISBILLED=true + BILLID set; per-charge `CIMS_ADDBILLDETAILS_1`.
- **Output**: bill header + details; final BillNo
- **Error**: none
- **Source Evidence**:
  - SVC: `custom/AddServiceOrderServiceHelper.java:217-341` (`:224-227` defaults, `:333` net, `:335` bill no, `:338` InterORFinal, `:344-345` item sale billed, `:370` details, `:636` charge IsBilled, `:637-644` package charges zeroed)
  - DB: `generated-procs/CIMS_ADDBILL.sql` (`:13` IsPharmacy=0, `:87,129` IsCancelled=0)
  - CFG: `application.properties` — `op.bill.no.template=%s/I/%d/%s`, `ip.bill.no.template=%s/I/%d/%s`
- **Tables**: T_Bill, T_BillDetails, T_ITEMSALE, T_Charges
- **APIs**: `POST /api/rest/serviceOrder/addServicechargesNew`, `POST /api/rest/serviceOrder/addServiceOrder`, `POST /api/rest/serviceOrder/saveServices`
- **UI dependency**: billing wizard (`billing:billingFlow`), client-side `calculateTotalBillAmount` (billing-module.js:2050)
- **Migration requirement**: mega-transaction Order→Bill→Lab→Rad→DoctorShare→EMR→HL7 must split into saga (10-target-architecture.md:13, §4 row 1); number templates per unit
- **Test cases**:
  - + draft bill (no number)
  - + final bill (numbered)
  - boundary: sequence formatting / rollover

## BR-BILL-002 — Doctor share split
- **Module**: Billing / Doctor share
- **Trigger**: `addservicecharge` per charge
- **Conditions**: tariff-service NOT `otProcedure=true`; share% > 0
- **Input**: totalAmount, sharePercent
- **Rule**: `doctorShare = totalAmount × %/100`; `hospitalShare = totalAmount − doctorShare`; recorded via `CIMS_ADDDOCTORSHARE` with `IsBilled=false`; DB uses CEILING rounding, DTO uses HALF_UP.
- **Output**: doctor-share row
- **Error**: none
- **Source Evidence**: SVC `custom/AddServiceOrderServiceHelper.java:383-419` (OT skip `:383-386`); DB proc `CIMS_ADDDOCTORSHARE`
- **Tables**: T_DoctorShare (T_DOCTORSHARE)
- **APIs**: `POST /api/rest/serviceOrder/addServicechargesNew`; doctor-share bill screens (`billing:doctorShare`)
- **UI dependency**: doctor share configuration master; share screens
- **Migration requirement**: rounding semantics (CEILING vs HALF_UP) must be pinned — one canonical calculation
- **Test cases**:
  - + share% > 0 → split correct
  - + share% = 0 → no row
  - − OT procedure tariff → no doctor share

## BR-BILL-003 — Patho/Rad order auto-creation at billing
- **Module**: Billing / Lab / Radiology
- **Trigger**: `addservicecharge` for patho/rad charges
- **Conditions**: charge specialization = patho (1) / radio (2)
- **Input**: charge, M_PathoTestSample definitions
- **Rule**: `CIMS_BILLFORINVESTIGATIONNEW` result 0 → create order booking + one detail per sample defined in `M_PathoTestSample` (`SampleCollectionNO=1..n`, `SampleNo=0`, `IsOutSourced=false`, `PathologistId=0`, testType=1); rad analog with `IsImageCaptured=false`.
- **Output**: T_PATHORDERBOOKING(+DETAILS) / T_RADIOLOGYORDERBOOKING(+DETAILS) rows (status=true; `istemp` mirrors charge temp-ness)
- **Error**: none
- **Source Evidence**:
  - SVC: `custom/AddServiceOrderServiceHelper.java:421-519` (patho), `:551-601` (rad)
  - DB: `CIMS_BILLFORINVESTIGATIONNEW`, `CIMS_GETPATHOTESTLISTFORBILL`, `CIMS_ADDPATHORDERBOOKING`, `CIMS_ADDPATHORDERBOOKINGDETAIL`, `CIMS_ADDRADORDERBOOKING`(+details)
- **Tables**: T_PATHORDERBOOKING, T_PATHORDERBOOKINGDETAILS, T_RADIOLOGYORDERBOOKING, T_RADIOLOGYORDERBOOKINGDETAILS, M_PathoTestSample
- **APIs**: `POST /api/rest/serviceOrder/addServicechargesNew`; `pathoOrder/*` booking list
- **UI dependency**: investigation list after billing (`billing:viewOpdChargesonInvestigation`)
- **Migration requirement**: Lab/Rad order creation is coupled to billing in one transaction — must become LabOrderCreated/RadOrderCreated events (10-target-architecture.md §4)
- **Test cases**:
  - + patho bill → orders created with samples
  - + rad bill → orders created
  - + non-patho charge → none
  - boundary: 0 samples defined → order without details

## BR-BILL-004 — Tax calculation
- **Module**: Billing
- **Trigger**: `addservicecharge` amount computation
- **Conditions**: `Taxmaster` rows; `allowNumberOfTaxCount` (2 in use: SGST/CGST/IGST labels)
- **Input**: rate, taxPct, conPct, taxId
- **Rule**: up to 3 taxes; `tax = rate × pct/100`; if `Taxmaster.applyafterconcession` → `(rate − rate×conPct/100) × pct/100`. Tax is added to SelfAmount; `TaxAmount = Σ taxN`; taxes computed only when taxAmount==0; `netAmount = selfAmount + nonSelfAmount`.
- **Output**: TaxAmount on bill detail
- **Error**: none (no taxId in master → 0)
- **Source Evidence**:
  - SVC: `custom/AddServiceOrderServiceHelper.java:2767-2798` (getTaxAmount `:2775-2790`), `:2838-2849` (setChargesAmountsBySponsors)
  - CFG: `application.properties` — `allowNumberOfTaxCount=2` (06 catalog), `discountBeforeTax=true` (findings §2)
- **Tables**: T_BillDetails (tax cols), M_TAXMASTER
- **APIs**: `POST /api/rest/serviceOrder/addServicechargesNew`
- **UI dependency**: 3 tax buckets in `#servicesTbl` (billing-module.js)
- **Migration requirement**: tax engine must be a pure function in Billing service; GST flag + date threshold (see STATUS-CODE-DICTIONARY §GST)
- **Test cases**:
  - + tax before concession
  - + tax after concession (`applyafterconcession`)
  - + no taxId in master → 0

## BR-BILL-005 — Co-pay / concession split
- **Module**: Billing
- **Trigger**: `addservicecharge` pricing
- **Conditions**: sponsor present; company == selfcompanyid or not
- **Input**: price, coPayPerc, concession (plan%/bill%), deductable
- **Rule**: `selfRate = price × coPayPerc/100`; `nonSelfRate = price − selfRate`. Self-company: concession applies to selfRate (plan% then bill% **added**). Non-self-company: concession applies to nonSelfRate (plan% then bill% **replaces** — asymmetric; RISK R2). Deductable null/0 → 100 (full self-pay). Net = self + nonSelf.
- **Output**: SelfAmount / NonSelfAmount / net
- **Error**: none
- **Source Evidence**:
  - SVC: `custom/AddServiceOrderServiceHelper.java:2800-2851` (getConcessionDetails `:2800-2830`; self-branch adds `:2815`; non-self overwrites `:2822` — RISK R2; deductable `:2833-2835`; net `:2849`)
  - RISK R2: `_discovery/agent-06/findings.md` R2 — inconsistent multi-tier concession; 06-catalog C.2 (Risk #2)
- **Tables**: T_BillDetails, T_PATIENTSPONSOR, concession templates
- **APIs**: `POST /api/rest/serviceOrder/addServicechargesNew`
- **UI dependency**: concession/co-pay columns client-side (`calculateTotalBillAmount`)
- **Migration requirement**: **golden-master pricing test** for the self vs non-self concession divergence; fix asymmetry consciously
- **Test cases**:
  - + self company (plan% then bill% added)
  - + non-self company (plan% then bill% replaces) — **divergence test**
  - + both concession tiers
  - boundary: deductable 0 → full self-pay

## BR-BILL-006 — Bill settlement & OPD checkout
- **Module**: Billing / Payment
- **Trigger**: `addBillPayment`
- **Conditions**: payment modes applied
- **Input**: payment modes, balances, cash amount
- **Rule**: Settled ⇔ self & non-self balances ≤ 0 (`CIMS_UPDATEBILLFORCONCESSION` sets IsSettled=true). OPD + not admission-appointment → `T_Encounter.isCheckout=true` + Solr + `T_Appointment.VisitStatus=11`. Cash change (`changeAmount`) recorded on T_Bill when `invstopdpostbilling`. Payment record: `transactionType=2`, cashCounterId=1, receipt no via `getLatestAutocodeSeq("Payments", receipts.no.template)`; `CIMS_ADDPAYMENT` → T_Payment; `CIMS_ADDPAYMENTBILLDETAILS` billType=1 with new balances; cash-mode rule: paymentModeId > 1 needs bankId/number (else 0/null); payment modes capped at remaining amount; MIS entry `helper.addUserWiseCollection`.
- **Output**: IsSettled=true; checkout; payment row(s)
- **Error**: none
- **Source Evidence**:
  - SVC: `custom/AddPaymentServiceHelper.java:62-521` (`:178-203` advance FIFO, `:215-253` partial ordering nonSelf-first, `:255-265` finalTrxAmount, `:267-282` payment+receipt, `:270` transactionType=2, `:293-309` bill details, `:313-349` advance modes, `:367-384` cash-mode/caps, `:390-403` MIS, `:438-463` settlement, `:440-447` checkout+Solr, `:477-478` VisitStatus 11, `:480-506` changeAmount)
  - DB: proc `CIMS_UPDATEBILLFORCONCESSION`, `CIMS_ADDPAYMENT`, `CIMS_ADDPAYMENTBILLDETAILS`, `CIMS_ADDPAYMENTADVANCEDETAILS`
- **Tables**: T_Bill, T_Payment, T_PaymentModeDetails, T_Appointment, T_Encounter
- **APIs**: `POST /api/rest/addPayment/` (addBillPaymentNew), `POST /api/rest/addPayment/itemSalePayment`
- **UI dependency**: payment modes modal (`showPaymentModes`), receipt print
- **Migration requirement**: settlement = billing↔payments interop (InvoiceGenerated + REST update, 10-target-architecture.md §4 row 2); changeAmount flag `invstopdpostbilling`
- **Test cases**:
  - + full payment → settled + checkout + VisitStatus 11
  - + partial → unsettled, balances updated

## BR-BILL-007 — Advance adjustment (FIFO)
- **Module**: Billing / Payment
- **Trigger**: `addBillPayment` with advance adjust
- **Conditions**: advances with balance > 0
- **Input**: advance list, self balance
- **Rule**: advances consumed against self balance in id order (FIFO); partial advance split; consumed advances recorded as payment-mode 7 + `CIMS_ADDPAYMENTADVANCEDETAILS`.
- **Output**: advance balance reduced; payment-mode-7 rows
- **Error**: none
- **Source Evidence**: SVC `custom/AddPaymentServiceHelper.java:178-203`, `:313-349`; DB `CIMS_ADDPAYMENTADVANCEDETAILS`
- **Tables**: T_Advance, T_Payment, T_PAYMENTADVANCEDETAILS
- **APIs**: `POST /api/rest/addPayment/`; advance screens (`billing:addPayment`, `addAdvance`)
- **UI dependency**: advance-adjust toggle (`setDefaultAdvanceAdjCheck`)
- **Migration requirement**: FIFO allocation must be idempotent; advance ledger in Payments service
- **Test cases**:
  - + full advance covers bill
  - + partial advance → split
  - + multiple advances → consumed in id order

## BR-BILL-008 — Refund/cancellation restore
- **Module**: Billing / Payment / Refund
- **Trigger**: `addRefund`; bill cancellation
- **Conditions**: advance balance > 0 for advance refund; settled bill for cancellation
- **Input**: refund modes, bill id
- **Rule**: Advance refund: `transactionType=3`, requires advance balance>0 (`BEXAR002`). Bill refund: `transactionType=4`. Settled-bill cancellation: `T_Advance Used−=n, Balance+=n`; charges restored from `*BeforeFinalization` + `IsCancelled=true`; `T_Itemsale BillID=0, IsBilled=false`; doctor-share details status=false; `cancelbill` marker set.
- **Output**: refund row; restored bill
- **Error**: `BEXAR002` (zero advance balance)
- **Source Evidence**:
  - SVC: `custom/AddRefundServiceImpl.java:82-155` (`:89` transactionType=3, `:134-155` refund modes, BEXAR002); `custom/BillCancellationServiceImpl.java:231-295` (restores), `:347` (transactionType=4), `:455-467` (cancelbill), `:470-509` (BeforeFinalization restore `:488-509`, charges `:489`)
  - RISK R6: `dischargeOnZeroBill` rebuild path breaks cancellation restore if any charge lacks before-finalization snapshot (findings R6)
- **Tables**: T_Refund, T_Advance, T_Charges, T_ITEMSALE, T_DoctorShare, T_Bill
- **APIs**: `POST /api/rest/addRefund/refund`; `DELETE /api/rest/serviceOrder/removeCharge`
- **UI dependency**: refund modal, credit-note print
- **Migration requirement**: cancellation is the compensation leg of the Order→Bill saga (10-target-architecture.md:135 — CIMS_SETTLEDBILLCANCELLATION / CIMS_UNSETTLEDBILLCANCELLATION)
- **Test cases**:
  - + advance refund
  - + bill cancel restores all amounts
  - − zero advance balance → `BEXAR002`

## BR-PHAR-001 — Dispensing completes visit (VisitStatus 12)
- **Module**: Pharmacy / OPD visit state
- **Trigger**: `medication/dispenseItemSale`
- **Conditions**: item sale with undispensed details
- **Input**: itemSaleId
- **Rule**: Each item-sale detail `DISPENSED=true`; when all details of an item sale dispensed → `T_ITEMSALE.DISPENSED=true`; when no undispensed item sales remain for the visit → `T_Appointment.VisitStatus=12`.
- **Output**: VisitStatus 12 (pharmacy fully dispensed)
- **Error**: none
- **Source Evidence**:
  - SVC: `custom/MedicationServiceImpl.java:799-821` (`:812` VisitStatus 12)
  - API: `PUT /api/rest/medication/dispenseItemSale` (04-api-catalog.md:1173)
  - JS: `dispenseItems` (item-category-module.js:2715)
  - RISK R1: 12 written to T_Appointment; split-brain with T_Encounter statuses
- **Tables**: T_ITEMSALE, T_ITEMSALEDETAILS, T_Appointment
- **APIs**: `PUT /api/rest/medication/dispenseItemSale`
- **UI dependency**: pharmacy dispense screens (`pharmacyOrderDispense`, `opPharmacyFlow`)
- **Migration requirement**: part of the VisitStatus 11/12 split-brain fix (06 catalog C.1); DispensingCompleted event (10-target-architecture.md:36)
- **Test cases**:
  - + all dispensed → VisitStatus 12
  - + partial (detail level) → stays < 12
  - boundary: last item sale dispensed → 12

## BR-PHAR-002 — Prescription closure on sale
- **Module**: Pharmacy / Prescription
- **Trigger**: item-sale detail creation (`CIMS_ADDITEMSALESDETAILS_NEW`)
- **Conditions**: prescription-linked sale
- **Input**: prescription detail qty, batchId
- **Rule**: proc inserts sale detail (with BatchId) and closes prescription: `IsClosed='1', PendingQty=0` (or `PendingQty−=qty`); master `IsClosed='1'`; compound details closed. Dispense flags `isverified/isdispensed/ispartialdispense` set on T_PRESCRIPTIONDETAILS.
- **Output**: closed prescription; sale detail with batch
- **Error**: none
- **Source Evidence**:
  - DB: `generated-procs/CIMS_ADDITEMSALESDETAILS_NEW.sql:103-121`
  - SVC: `custom/PharmacyServiceImpl.java:735-746` (flags `:536,574,624,768`); batch expiry via proc `cims_getitemanditembatchlistbyvisitid_new3` (`DTOQueries_4.java:762`), surfaced as `ibm.expirydate` (`PharmacyServiceImpl.java:2622`)
- **Tables**: T_PRESCRIPTIONDETAILS, T_PRESCRIPTIONMASTER, T_ITEMSALEDETAILS, T_ITEMCOMPOUNDDETAILS
- **APIs**: `POST /api/rest/itemsales/addItemSalesDetails`, `POST /api/rest/itemsales/addItemSales`
- **UI dependency**: verify/dispense screens (`pharmacyOrderVerify`, `pharmacyOrderDispense`)
- **Migration requirement**: prescription lifecycle must be event-driven (PrescriptionCreated → DispensingCompleted, 10-target-architecture.md:36)
- **Test cases**:
  - + full qty → closed (PendingQty=0)
  - + partial → PendingQty reduced
  - boundary: qty == remaining

## BR-PATH-001 — Sample collection locks charge removal
- **Module**: Pathology / Radiology / Billing
- **Trigger**: `serviceOrder/removeCharge`
- **Conditions**: patho/rad charge with `isaccepted=true` on latest booking detail
- **Input**: chargeId, booking detail
- **Rule**: Sample accepted → charge cannot be removed unless `autoChargeCancellationOnRefund` (else `LEX001`); removable path → booking `ISCANCELLED=true`; nursing → `T_PATIENTNURSINGORDERS.orderstatus=DISCONTINUE`; procedure → EMR service/procedure/schedule status=false; charge row `ISCANCELLED=true`.
- **Output**: charge removed or error
- **Error**: `LEX001` (sample collected, cancellation not permitted)
- **Source Evidence**:
  - SVC: `custom/AddServiceOrderServiceHelper.java:3693-3719` (getSampleStatus), `:3751-3775` (booking ISCANCELLED), `:3781-3812` (EMR status=false), `:3819` (nursing DISCONTINUE), `:3862` (charge ISCANCELLED)
  - API: `DELETE /api/rest/serviceOrder/removeCharge` (04-api-catalog.md:329)
- **Tables**: T_PATHORDERBOOKING(+DETAILS), T_PATIENTNURSINGORDERS, T_EMR_SERVICES, T_Charges
- **APIs**: `DELETE /api/rest/serviceOrder/removeCharge`, `POST /api/rest/pathoOrder/sampleAccepted`
- **UI dependency**: sample accept/reject screens; credit-note checks (`checkSampleStatusForCreditNote`)
- **Migration requirement**: cancellation saga must handle lab-sampled charges (compensation with LEX001 guard)
- **Test cases**:
  - + no sample → cancel OK
  - + collected → `LEX001`
  - + refund-config on → cancel OK (booking cancelled)

## BR-OT-001 — OT scheduling prerequisites
- **Module**: OT & Surgery
- **Trigger**: `otscheduler/addPatientProcdure`
- **Conditions**: active admission or visit
- **Input**: patient, procedures, schedule
- **Rule**: Patient needs active admission (else latest visit); none → `BEOT004`; schedule/OT-details/procedure rows created (`CIMS_ADDUPDATEPATIENTPROCEDURESCHEDULEMASTER`, `CIMS_ADDUPDATEOTDETAILS`, `CIMS_ADDUPDATEPATIENTPROCDURE`); dependent-surgery discount% applied; procedure rate from tariff (`getServicePriceDetails`, 0 if none); doctor rows `CIMS_ADDUPDATEOTDETAILSDOCDETAILS`.
- **Output**: schedule + procedure + rate rows
- **Error**: `BEOT004` (no admission/visit)
- **Source Evidence**: SVC `pas/OTSchedulerServiceImpl.java:596-800` (`:616-631` BEOT004, `:731-736` rate, `:759-768` dependent discount); API `POST /api/rest/otscheduler/addPatientProcdure` (04-api-catalog.md:2708)
- **Tables**: T_OTSCHEDULE, T_OTDETAILS, T_PATIENTPROCEDURE(+RATEDETAILS), M_PROCEDUREMASTER
- **APIs**: `POST /api/rest/otscheduler/addPatientProcdure`; `otconfig/*`, `otdetails/*`
- **UI dependency**: OT schedule screens (`ot:otSchedule/:otId/:tableId`, surgeryScheduleSchema.json)
- **Migration requirement**: ProcedureScheduled event; OT service reads admission via event/API (10-target-architecture.md:38)
- **Test cases**:
  - + admitted patient → scheduled
  - + OPD visit → scheduled (latest visit)
  - − neither → `BEOT004`

## BR-OT-002 — OT pre-billing
- **Module**: OT / Billing
- **Trigger**: procedure ordering / billing
- **Conditions**: `otprocedure.pre.billing=true` (config, in use)
- **Input**: procedure charge
- **Rule**: procedure-specialization charges created `consumed=true` (direct charge `CIMS_ADDCHARGE`, NOT temp) so they bill upfront; doctor-share skipped for OT tariff services; ordered-procedures query filters `isOtProcessDone=false`.
- **Output**: upfront OT charge
- **Error**: none
- **Source Evidence**:
  - SVC: `custom/AddServiceOrderServiceImpl.java:2202-2204`; `custom/AddServiceOrderServiceHelper.java:383-386`; `pas/OTSchedulerServiceImpl.java:4069-4077` (`:4069` T_EMR_Services ordering record + `tempchargeid` link, `:4069-4077` filter)
  - CFG: `application.properties` — `otprocedure.pre.billing=true` (findings §2)
- **Tables**: T_Charges, T_EMR_SERVICES, T_PATIENTPROCEDURE
- **APIs**: `POST /api/rest/otscheduler/addPatientProcdure`, `POST /api/rest/serviceOrder/addServicechargesNew`
- **UI dependency**: OT billing flow (`billing:otBillFlow`)
- **Migration requirement**: pre-billing flag must survive into Billing/OT service config; charge temp vs consumed semantics
- **Test cases**:
  - + pre-bill on → consumed direct charge
  - + pre-bill off → temp charge path

## BR-BLD-001 — Blood requisition status machine
- **Module**: Blood Bank
- **Trigger**: `updateRequisitionStatus` (after cross-match / issue / cancel)
- **Conditions**: cumulative volumes
- **Input**: requisition, cross-matched volume, bag volume, ordered volume
- **Rule**: 1=new; after cross-match → 2 if cumulative cross-matched volume ≥ ordered volume else stays 1; after issue → 3 if cumulative bag volume ≥ ordered volume; cancel/expire → 10.
- **Output**: requisition status 1/2/3/10
- **Error**: none
- **Source Evidence**:
  - SVC: `pas/BloodBankServiceImpl.java:6290-6317` (`:6302` status 2 decision, `:6312` status 3)
  - DB: `constants/BloodBankConstants.java:31-33` (REQ_CANCELLATION=10)
  - RISK R7: job sets *details* status 10 but header constant used at creation/cancel only — header/details asymmetry (findings R7)
- **Tables**: T_PATIENTREQUISITION(+DETAILS), T_BLOODCROSSMATCHINGRESULT
- **APIs**: `POST /api/rest/patientrequisition/addPatientBloodRequisition`, `POST /api/rest/bloodtestmaster/saveBloodCrossMatchingTestResult`, `POST /api/rest/bloodtestmaster/issueBloodBag`
- **UI dependency**: crossMatchingList, issueBloodBag screens
- **Migration requirement**: state machine in Blood Bank service + state diagram (see STATUS-CODE-DICTIONARY); resolve header/details asymmetry (R7)
- **Test cases**:
  - + partial cross-match → stays 1
  - + full cross-match → 2
  - + issue → 3
  - + cancel/expire → 10

## BR-BLD-002 — Auto decross after 48h
- **Module**: Blood Bank (job)
- **Trigger**: `${bloodbag.auto.decross}` daily 00:00 (`0 0 * ? * *`)
- **Conditions**: crossmatched (status=1), not decrossed, `crossMatchedTestDate` age ≥ 2880 min (AUTO_DECROSS_PERIED_MINUTES)
- **Input**: blood bags
- **Rule**: decross bag (`crosSMatched=false`), requisition status → 1, cross-match count decremented, `BloodBagDecross` row with reason 0.
- **Output**: bag uncrossed; requisition re-opened
- **Error**: none
- **Source Evidence**:
  - SVC: `jobs/ApplicationJobs.java:1045-1061`; `pas/BloodBankServiceImpl.java:7657-7701` (`:7666-7667` crosSMatched=false)
  - DB: `constants/BloodBankConstants.java:38` (period constant)
- **Tables**: T_BLOODBAG, T_BLOODCROSSMATCHINGRESULT, T_BLOODBAGDECROSS, T_PATIENTREQUISITIONDETAILS
- **APIs**: none (scheduled job)
- **UI dependency**: crossMatchedBags list
- **Migration requirement**: scheduled decross → event-driven timeout in Blood Bank service
- **Test cases**:
  - + <48h → untouched
  - + ≥48h → decrossed, req → 1
  - boundary: exactly 2880 min → decrossed

## BR-BLD-003 — Bag expiry handling (daily 23:00)
- **Module**: Blood Bank (job)
- **Trigger**: `${bloodbag.expiry.schedule}` (`0 1 * * * *` per findings §2)
- **Conditions**: `expirydate = today`; not already issued
- **Input**: blood bags
- **Rule**: bags expiring today → `isbagexpired=true` (issued bags skipped); if crossmatched & unissued → decross + requisition status back to 1.
- **Output**: expired flag; decross
- **Error**: none
- **Source Evidence**: SVC `jobs/ApplicationJobs.java:769-815` (`:785` isbagexpired=true); CFG `bloodbag.expiry.schedule`
- **Tables**: T_BLOODBAG, T_PATIENTREQUISITIONDETAILS
- **APIs**: none (scheduled job)
- **UI dependency**: bloodBagTrail, expired list
- **Migration requirement**: scheduled job → Blood Bank service
- **Test cases**:
  - + expiring today → isbagexpired=true
  - + already issued → skipped
  - + crossmatched & unissued → decross + req status 1

## BR-BLD-004 — Requisition expiry
- **Module**: Blood Bank (job)
- **Trigger**: `${bloodorder.cancellation.schedule}`
- **Conditions**: `requesteddate + orderValidationPeriod(h) ≤ sysdate`; sample not collected; status=1
- **Input**: requisition details
- **Rule**: details `requisitionstatus=10`, `isorderexpired=true`, reason "Order is expired".
- **Output**: expired requisition details
- **Error**: none
- **Source Evidence**: SVC `jobs/ApplicationJobs.java:734-766` (`:743` status 10); RISK R7 (details vs header asymmetry)
- **Tables**: T_PATIENTREQUISITIONDETAILS
- **APIs**: none (scheduled job)
- **UI dependency**: patientRequisitionList shows expired
- **Migration requirement**: timeout handling in Blood Bank service
- **Test cases**:
  - + expired → details 10
  - + collected → untouched

## BR-BLD-005 — Donor auto-cancellation
- **Module**: Blood Bank (job)
- **Trigger**: `${donor.cancellation.schedule}`
- **Conditions**: donor in DNR_NEW(1)/DNR_FIT(2); no action within 72h
- **Input**: donor registrations
- **Rule**: → donationStatus=7 (DNR_CANCELLATION).
- **Output**: cancelled donor
- **Error**: none
- **Source Evidence**: SVC `jobs/ApplicationJobs.java:832-848`; DB `constants/BloodBankConstants.java:8-17`
- **Tables**: T_BLOODDONORREGISTRATION(+DETAILS)
- **APIs**: none (scheduled job)
- **UI dependency**: donorList status column
- **Migration requirement**: timeout event in Blood Bank service
- **Test cases**:
  - + 72h boundary → cancelled
  - + post-med-exam statuses (3,4,5,10,11) untouched

---

## Additional rules (ADDED — evidenced only in `_discovery/agent-06/findings.md`, NOT in the 06 catalog)

## BR-OPD-008 — MLC (medico-legal) registration on visit — **(ADDED)**
- **Module**: OPD/Emergency Visit
- **Trigger**: `addwalkinVisitNew` encounter creation
- **Conditions**: MLC flag set in DTO
- **Input**: `mlcDetailsDto` (broughtBy*, broughtByName, broughtByRelationId, broughtByMobileNo, isBroughtDead, formalityRemark)
- **Rule**: MLC flag + `MLCDetails` row created with encounter; audit event `MLCDETAILS.ADD`. Brought-dead (`isBroughtDead`) propagates `isDeath` to T_Registration_Details_int at registration.
- **Output**: MLCDetails row + audit
- **Error**: none
- **Source Evidence**:
  - SVC: `EmergencyAndWalkinVisitServiceImpl.java:1210-1214` (MLC flag + row), `:1438-1481` (audit MLCDETAILS.ADD); `PatientServiceHelper.java:825-829` (isDeath=broughtDead)
  - JS: `walkinVisit-schema.json` mlcDetailsDto block (03-legacy-frontend-inventory.md:81); MLC screens `op:mlcDetails`
- **Tables**: T_MLCDETAILS, T_Encounter, T_Registration_Details
- **APIs**: `POST /api/rest/walkinVisit/addwalkinVisit`, `PUT /api/rest/walkinVisit/updateMlcDetailsbyVisitId`, `POST /api/rest/patientRegAddmission/addUpdateMLCDetails`
- **UI dependency**: MLC form section; MLC clearance lists (`ip:MlcclearanceListFlow`)
- **Migration requirement**: MLC is part of ADT context (10-target-architecture.md:30 owns T_MLCDETAILS)
- **Test cases**:
  - + MLC visit → row + audit
  - + brought-dead → isdeath propagated
  - − non-MLC visit → no row

## BR-OPD-009 — Walk-in validation gates — **(ADDED)**
- **Module**: OPD Walk-in
- **Trigger**: `addwalkinVisitNew` input validation
- **Conditions**: various
- **Input**: text fields, address/mobile, PCTS number, patient admission status
- **Rule**:
  1. Script-injection text check → `BEXA002`.
  2. ext-LMIS mode → address + mobile required.
  3. Duplicate PCTS number → `BEXA034`.
  4. Patient with an active (non-leave) admission cannot be walked in → `FWAD001`.
- **Output**: validation pass or error
- **Error**: `BEXA002`, `BEXA034`, `FWAD001`
- **Source Evidence**: SVC `EmergencyAndWalkinVisitServiceImpl.java:315-319` (BEXA002), `:325-332` (ext-LMIS), `:345-350` (BEXA034), `:371-378` (FWAD001)
- **Tables**: T_Encounter, T_Admission (read)
- **APIs**: `POST /api/rest/walkinVisit/addwalkinVisit`
- **UI dependency**: schema validators (`validatePatientDetails` validation.js:1; `validateMobileNoByAPI`)
- **Migration requirement**: keep error codes as contract (04-backend-architecture.md §9.6 — codes must be retained)
- **Test cases**:
  - + valid input → passes
  - − script-injection text → `BEXA002`
  - − ext-LMIS missing address/mobile → validation error
  - − duplicate PCTS → `BEXA034`
  - − active admission → `FWAD001`

## BR-IPD-006 — Ambulance service carry-over on admission — **(ADDED)**
- **Module**: IPD Admission
- **Trigger**: `CIMS_ADDADMISSIONINFORMATION` proc during admission
- **Conditions**: prior visit ≤ 6h from admission AND `IsAmbulanceServiceAvailed`
- **Input**: visit/encounter, admission
- **Rule**: proc copies `T_AmbulanceServiceDetails` rows onto the new admission.
- **Output**: ambulance details carried to admission
- **Error**: none
- **Source Evidence**: DB `generated-procs/CIMS_ADDADMISSIONINFORMATION.sql:105-187` (findings 1.3 step 9)
- **Tables**: T_AmbulanceServiceDetails, T_Admission
- **APIs**: `POST /api/rest/patientRegAddmission/adddata`
- **UI dependency**: ambulance section on admission schema
- **Migration requirement**: carry-over logic must port to ADT service (data move with the admission row)
- **Test cases**:
  - + visit ≤6h + ambulance flag → copied
  - + >6h → not copied
  - − no ambulance → no copy

---

## Cross-layer rule map (which rules span JS + Java + SQL + scheduled jobs)

Legend: **JS** = client logic/contract in FAUG schemas/controllers · **Java** = service impl logic · **SQL/Proc** = stored proc / native SQL / DB constant · **Job** = scheduled job (`ApplicationJobs`) · **Solr** = search index side effect · **INT** = external integration (HL7/ABDM/WhatsApp/MinIO)

| Rule | JS | Java | SQL/Proc | Job | Solr | INT | Notes |
|---|---|---|---|---|---|---|---|
| BR-PAT-001 | ✓ | ✓ | ✓ (`cims_getlatestautocodeandincremetitbyone`, CIMS_ADDPATIENTREGISTRATION*) | – | – | ✓ (MinIO docs) | 3 MRN modes + upgrade |
| BR-PAT-002 | ✓ | ✓ | – | – | ✓ | – | Solr duplicate check is the *core* of the rule |
| BR-PAT-003 | – | ✓ | – | – | – | – | pure validation |
| BR-OPD-001 | – | ✓ | – | – | – | – | config-driven tariff + literal fallback (R9) |
| BR-OPD-002 | – | ✓ | – | – | – | – | collision predicates |
| BR-OPD-003 | – | ✓ | ✓ (unit sequence) | – | – | – | number template |
| BR-OPD-004 | – | ✓ | – | – | – | – | visit-type state machine |
| BR-OPD-005 | – | ✓ | ✓ (CIMS_ADDPATIENTSPONSOR_NEW_DOC) | – | – | – | sponsor defaults |
| BR-OPD-006 | – | ✓ | – | – | – | – | doctor required |
| BR-OPD-007 | ✓ (kiosk `getLastVisitStatus`) | ✓ | – | – | ✓ | – | **cross-layer split-brain (R1)** |
| BR-OPD-008 | ✓ (mlcDetailsDto) | ✓ | – | – | – | ✓ (audit) | ADDED |
| BR-OPD-009 | ✓ (schema validators) | ✓ | – | – | – | – | ADDED |
| BR-IPD-001 | – | ✓ | – | – | – | – | preconditions |
| BR-IPD-002 | – | ✓ | ✓ (CIMS_UPDATEBEDOCCUPIEDFLAG) | – | – | – | atomic CAS claim |
| BR-IPD-003 | – | ✓ | – | – | – | – | master-driven gate |
| BR-IPD-004 | – | ✓ | ✓ (CIMS_ADDADMISSIONINFORMATION) | – | – | – | **dead APP015 path (R4)** |
| BR-IPD-005 | – | ✓ | – | ✓ | – | – | auto-abscond job |
| BR-IPD-006 | – | – | ✓ (proc carry-over) | – | – | – | ADDED, proc-only |
| BR-DIS-001 | – | ✓ | – | – | – | – | precondition bundle |
| BR-DIS-002 | – | ✓ | ✓ (CIMS_ADDBEDDISCHARGE, CIMS_UPDATEIPDISDISCHARGE) | – | ✓ | ✓ (ABDM) | death + closure + Solr |
| BR-DIS-003 | – | ✓ | ✓ (CIMS_UPDATEBEDOCCUPIEDFLAG) | – | – | – | **hard-coded BedStatusId=6 (R5)** |
| BR-BILL-001 | ✓ (calculateTotalBillAmount) | ✓ | ✓ (CIMS_ADDBILL*, autocode) | – | – | – | number templates |
| BR-BILL-002 | – | ✓ | ✓ (CIMS_ADDDOCTORSHARE) | – | – | – | rounding split |
| BR-BILL-003 | – | ✓ | ✓ (CIMS_BILLFORINVESTIGATIONNEW, CIMS_ADDPATHORDERBOOKING*, CIMS_ADDRADORDERBOOKING*) | – | – | – | **bill→lab/rad same txn** |
| BR-BILL-004 | ✓ (3 tax buckets) | ✓ | – | – | – | – | tax engine |
| BR-BILL-005 | ✓ (concession cols) | ✓ | – | – | – | – | **asymmetry (R2)** |
| BR-BILL-006 | ✓ (payment modal) | ✓ | ✓ (CIMS_ADDPAYMENT*, CIMS_UPDATEBILLFORCONCESSION) | – | ✓ | ✓ (MIS) | settlement + checkout |
| BR-BILL-007 | ✓ (advance toggle) | ✓ | ✓ (CIMS_ADDPAYMENTADVANCEDETAILS) | – | – | – | FIFO |
| BR-BILL-008 | – | ✓ | ✓ (CIMS_SETTLED/UNSETTLEDBILLCANCELLATION) | – | – | – | compensation flow (R6) |
| BR-PHAR-001 | ✓ (dispenseItems) | ✓ | – | – | – | – | **VisitStatus 12 (R1)** |
| BR-PHAR-002 | – | ✓ | ✓ (CIMS_ADDITEMSALESDETAILS_NEW, batch query proc) | – | – | – | closure + batch |
| BR-PATH-001 | ✓ (sample screens) | ✓ | – | – | – | – | charge-removal guard |
| BR-OT-001 | – | ✓ | ✓ (CIMS_ADDUPDATE*) | – | – | – | scheduling |
| BR-OT-002 | – | ✓ | ✓ (CIMS_ADDCHARGE) | – | – | – | pre-billing flag |
| BR-BLD-001 | ✓ (cross-match screens) | ✓ | ✓ (constants) | ✓ | – | – | **header/details asymmetry (R7)** |
| BR-BLD-002 | – | ✓ | ✓ (constants) | ✓ | – | – | 48h decross |
| BR-BLD-003 | – | ✓ | – | ✓ | – | – | bag expiry |
| BR-BLD-004 | – | ✓ | – | ✓ | – | – | requisition expiry |
| BR-BLD-005 | – | ✓ | ✓ (constants) | ✓ | – | – | donor auto-cancel |

**Key cross-layer risk chains** (from 06 catalog §C):
1. VisitStatus 11/12 split-brain — BR-OPD-007 + BR-PHAR-001 (billing writes T_Appointment; clinical writes T_Encounter).
2. Non-self concession overwrite — BR-BILL-005 (R2).
3. DNR_REJECTED == DNR_DONATED == 5 — donor enums (STATUS-CODE-DICTIONARY §Donor).
4. Bed release hard-coded BedStatusId=6 — BR-DIS-003 (R5).
5. Dead admission failure path — BR-IPD-004 (R4).
6. Requisition header/details status asymmetry — BR-BLD-001/BR-BLD-004 (R7).
7. DB triggers: **zero triggers exist** (05-database-catalog.md:128) — all DB-layer enforcement is in procs; no trigger-layer rules exist.
