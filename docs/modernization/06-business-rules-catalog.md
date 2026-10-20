# 06 — Business Rules Catalog (iHMS/OHUM PAS NG)
## Agent 06: Cross-Module Business Logic Tracer | Evidence-first, RESEARCH-ONLY
Source: `D:\ihms` | Generated: 2026-08-14
All numeric status meanings are backed by code (constants / setStatus / SQL literals). Unknowns are marked `[UNKNOWN]`.

---

## A. STATUS CODE DICTIONARIES (entity | code | meaning | set-by | evidence)

### A1. Visit/Encounter state — `T_Encounter.VisitStatus` / `T_Appointment.VisitStatus`
| Entity | Code | Meaning | Set by | Evidence |
|---|---|---|---|---|
| T_Encounter | 3 | Doctor called patient (consult started) | `updateCallPatientData` / `updateCallPatientDataNew` | `EmergencyAndWalkinVisitServiceImpl.java:2017,2363` |
| T_Appointment | 4 | Patient-assessment questionnaire saved | `QuetionnaireServiceImpl` | `QuetionnaireServiceImpl.java:107` |
| T_Appointment | 6 | Other questionnaire saved | `QuetionnaireServiceImpl` | `QuetionnaireServiceImpl.java:109` |
| T_Encounter | 9 | Doctor checkout / compound-med pre-visit RX | `updateDoctorCheckout` | `EmergencyAndWalkinVisitServiceImpl.java:2270,2351` |
| T_Encounter | 10 | Cabin released (no pending temp charges) | `releaseCabin` | `EmergencyAndWalkinVisitServiceImpl.java:2146` |
| T_Appointment | 11 | Bill paid / encounter checked out | `addBillPayment` | `AddPaymentServiceHelper.java:477,820` |
| T_Appointment | 12 | Pharmacy fully dispensed for visit | `dispenseItemSale` | `MedicationServiceImpl.java:812` |
| T_Encounter | (str) `visitstatus2="2"` | Default on walk-in visit | `addwalkinVisitNew` | `EmergencyAndWalkinVisitServiceImpl.java:1039` |
| T_Encounter | (str) `visitstatus2="9"` | Compound-medication visit | `addwalkinVisitNew` | `EmergencyAndWalkinVisitServiceImpl.java:1141` |

### A2. Bill — `T_Bill`
| Code | Meaning | Set by | Evidence |
|---|---|---|---|
| `Status=1` | Active bill | `addservicecharge` | `AddServiceOrderServiceHelper.java:224` |
| `IsSettled=false→true` | Settled only when Self & NonSelf balances ≤ 0 | `addBillPayment` via `CIMS_UPDATEBILLFORCONCESSION` | `AddPaymentServiceHelper.java:438-463` |
| `IsCancelled=0` | Insert default | `CIMS_ADDBILL` | `generated-procs/CIMS_ADDBILL.sql:87,129` |
| `cancelbill` | Bill cancellation marker | `BillCancellationServiceImpl` | `BillCancellationServiceImpl.java:455-467` |
| `InterORFinal` | Interim vs final | `addservicecharge` | `AddServiceOrderServiceHelper.java:338` |
| `IsPharmacy=0` | Default non-pharmacy | proc default | `generated-procs/CIMS_ADDBILL.sql:13` |

### A3. Charge — `T_Charges` / `T_CHARGES_TEMP`
| Code | Meaning | Evidence |
|---|---|---|
| `IsBilled` | Billed=true at finalize; T_ITEMSALE.BillID set | `AddServiceOrderServiceHelper.java:344-345,636` |
| `IsCancelled` | Charge cancelled (removeCharge, bill cancel) | `AddServiceOrderServiceHelper.java:3862`; `BillCancellationServiceImpl.java:489` |
| `IsTemp` / table split | Un-consumed charges live in `T_CHARGES_TEMP` | `AddServiceOrderServiceImpl.java:2233-2241` |
| `*BeforeFinalization` cols | Snapshot used to restore amounts on bill cancellation | `BillCancellationServiceImpl.java:488-509` |

### A4. Admission — `T_Admission` / `T_AdmissionDetails`
| Code | Meaning | Set by | Evidence |
|---|---|---|---|
| `ISDischarged=0→1`, `IsClosed='0'→'1'` | Discharge completed (also closes T_TransferRequest) | `CIMS_UPDATEIPDISDISCHARGE` | `generated-procs/CIMS_UPDATEIPDISDISCHARGE.sql` |
| `ISCANCEL=1` + `DISCHARGETYPEID` + `ABSCONDDATETIME` | Auto-abscond (interim admission, no bed > 3 days) | `updateAbscondInterimAdmissionStatusJob` | `ApplicationJobs.java:1007-1016` |
| `BEDID=0/null` | Interim (un-bedded) admission marker | query predicate | `ApplicationJobs.java:1008` |

### A5. Bed — `M_BedMaster`
| Code | Meaning | Set by | Evidence |
|---|---|---|---|
| `OCCUPIED=false/true` | Occupancy flag, atomic CAS update | admission / discharge / transfer | `PatientRegAdmissionServiceImpl.java:344`; `PatientTransferDischargeServiceImpl.java:789` |
| `BedStatusId=6` | Released (free after discharge/transfer) | discharge, transfer | `PatientTransferDischargeServiceImpl.java:790,3811,4162,5082,5186` |
| `BedStatusId=0` | Reset to available | cancel/transfer ops | `PatientTransferDischargeServiceImpl.java:1074,1096,1102,3831` |
| `BedStatusId=5` | Occupied/retained predicate | read query | `BedManagementServiceImpl.java:1859` |
| `BedStatusId=1 or 3` | Free-bed predicates | read queries | `BedManagementServiceImpl.java:1864,2763` |
| `M_BedStatusMaster.isrelease` | Governs whether bed status allows allocation | gate at admission → `BERB005` | `PatientRegAdmissionServiceImpl.java:1293-1300` |

### A6. Payment — `T_Payment.TransactionType`
| Code | Meaning | Evidence |
|---|---|---|
| 2 | Bill payment | `AddPaymentServiceHelper.java:270` |
| 3 | Advance refund | `AddRefundServiceImpl.java:89` |
| 4 | Bill refund | `BillCancellationServiceImpl.java:347` |
| (payment mode) 7 | Advance adjustment as payment mode | `AddPaymentServiceHelper.java:317` |

### A7. Blood requisition & bag
| Code | Meaning | Set by | Evidence |
|---|---|---|---|
| 1 | NEW / cross-match pending (or partial) | creation; re-opened by decross/expiry | `BloodBankServiceImpl.java:6302`; `ApplicationJobs.java:7675,804` |
| 2 | Fully cross-matched (vol ≥ ordered vol) | `updateRequisitionStatus` | `BloodBankServiceImpl.java:6302` |
| 3 | Fully issued (bag vol ≥ ordered vol) | `updateRequisitionStatus` | `BloodBankServiceImpl.java:6312` |
| 10 | Cancelled / expired | cancel; `updateExpiredBloodRequisitionStatusJob` | `BloodBankConstants.java:33`; `ApplicationJobs.java:743` |
| Bag `isbagexpired=true` | Expired on expiry date | `updateExpiryStatusOfTheBloodBagsJob` (23:00 daily) | `ApplicationJobs.java:785` |
| Bag `isdecrossed / crosSMatched=false` | Auto-decrosed after 2880 min | `decrossBloodBag` (daily 00:00) | `ApplicationJobs.java:1055`; `BloodBankServiceImpl.java:7666-7667` |
| Bag issue filters: `isreserved=false, isbagextracted=false, isdiscard=false, isdiscarded=false, isissued=false, istested=true, iscrossmatched=false` | Issuable bag predicate | read query | `BloodBankServiceImpl.java:2273-2277` |

### A8. Donor — `BloodDonorRegistration(details).donationStatus` (BloodBankConstants.java:8-17)
| Code | Meaning |
|---|---|
| 1 (DNR_NEW) | New registration |
| 2 (DNR_FIT) | Fit (med exam ok) |
| 3 (DNR_UNFIT / DNR_VOLUNTORY — same literal!) | Unfit; also voluntary-donor type |
| 4 (DNR_ACCEPTED) | Accepted |
| 5 (DNR_REJECTED **and** DNR_DONATED — collision) | Rejected / Donated |
| 7 (DNR_CANCELLATION) | Cancelled (incl. auto-cancel of inactive donors) |
| 10 (DNR_PERM_UNFIT) | Permanently unfit |
| 11 (DNR_TEMP_UNFIT) | Temporarily unfit |

### A9. Other enums (GenericConstants.java:47-188)
- Authorization: 0=ACCEPTED, 1=APPROVED, 2=PARTIALAPPROVED, 3=REJECTED, 4=FAILED, 5=NEW, 6=CANCEL, 7=SUBMITTED, 8=QUERIED.
- Medication: 1=ACTIVE, 2=DISCONTINUE, 3=EXPIRED. Patient med: 1=ORDERED, 2=TAKEN, 3=MISSED, 4=SKIPPED.
- NHCX request: 0=INSURANCEPLAN..4=COMMUNICATION; status 0=ACCEPTED..3=FAILED.
- GST applicability: `isGSTApplicable=true` AND `transDate ≥ 2017-07-01 00:00:00` (`BaseServiceImpl.java:521-533`).

---

## B. BUSINESS RULES

### BR-PAT-001 — Patient MRN generation strategy
- **Module**: Patient Registration | **Trigger**: registerPatient (new patient)
- **Conditions**: `mrnoFromSeries` / `autoGenerateTempMrnNo` / offline unit flags
- **Input**: `PatientAppointmentandRegDto.mRNO`, placeOfBirthId, unitId
- **Rule**: (1) `mrnoFromSeries=true` → validate/generate from series; blank → `IsTempMrno=true`. (2) else `autoGenerateTempMrnNo=true` and appointment → temp MRN `<tempMrnPrefix>-<3-digit-seq>` from `cims_getlatestautocodeandincremetitbyone('T AppoinmentTempMrn')`. (3) offline unit → client-supplied MRN preserved. Permanent MRN (upgrade path) = `ddMMYY + patientId padded to 9` (`BaseServiceImpl.generateMrno`).
- **Output/Error**: MRN assigned on T_Registration; `IsTempMrno` flag
- **Source evidence**: `custom/PatientServiceHelper.java:373-425`; `BaseServiceImpl.java:987-1017`; `application.properties` (`mrnoFromSeries=false`, `autoGenerateTempMrnNo=false`)
- **Tables**: T_Registration, T_Registration_Int, M_MRNSeries
- **API**: `POST api/rest/patient/registerPatient`
- **UI dependency**: MRN field optional; temp-MRN badge
- **Migration**: sequence/auto-code infra (`cims_getlatestautocodeandincremetitbyone`), temp→permanent promotion on visit/admission (`EmergencyAndWalkinVisitServiceImpl.java:678-684`, `PatientRegAdmissionServiceImpl.java:378-386`)
- **Tests**: + series off/on; + appointment temp MRN padding (1/2/3 digit); + offline reuse; − invalid; boundary: patientId → 9-digit pad; MRN colliding (BERA011)

### BR-PAT-002 — Duplicate patient identity detection
- **Rule**: Before insert, Solr lookup `findPatientByIdentity`; hit → error `BERA011`. Health ID (ABDM) duplicate → `BEXA040` with existing MRN.
- **Evidence**: `PatientServiceHelper.java:661-669, 783-790`
- **API**: registerPatient; **Tables**: Solr `patient_risl_qa`
- **Migration**: duplicate-identity heuristics and healthId uniqueness must survive Solr retirement
- **Tests**: + unique passes; − same name/mobile/dob → BERA011; − same healthIdNumber → BEXA040

### BR-PAT-003 — Family-Id / member coupling
- **Rule**: `familyIdNo` provided without `memberId` → `BERP007`.
- **Evidence**: `PatientServiceHelper.java:704-706`
- **Tests**: + both; − familyId only → BERP007

### BR-OPD-001 — Non-residential tariff selection
- **Rule**: If patient `resStateId != config.state` → sponsor tariff = `config.nonresidentialtariffId`, else hard-coded `2L`.
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:410-419`; `PatientRegAdmissionServiceImpl.java:366-375`
- **Tables**: T_PATIENTSPONSOR | **Migration**: config-driven tariff, fallback literal 2L
- **Tests**: + resident; + non-resident with config; − non-resident without config (falls to 2L)

### BR-OPD-002 — Appointment slot collision rules
- **Rule**: (a) Active (status=true, iscancelled=0) same-day same-dept encounter for patient → `BEXA029`. (b) Same-day hour window (HH:00:00–HH:59:59) same dept+unit appointment → `BERA028`. (c) Appointments ≥ 30 min apart; slot duration = `config.appointmentslot` (default 15).
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:537-546, 547-593, 423-426`
- **Tables**: T_Encounter | **Tests**: + slot free; − same-hour → BERA028; − duplicate dept/date → BEXA029

### BR-OPD-003 — OPD/emergency visit number template
- **Rule**: `opdNo = format(opdNoTemplate, unitprefixformrno, year-2000, %05d seq)` → `V/<PREFIX>/<YY>/<00001>`; emergency uses `EV/<...>`. Sequence per unit (`Unitmaster.getOPDSequenceName`).
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:1291-1308`; `application.properties`
- **Tests**: + normal; + emergency; boundary: seq > 99999 (5-digit overflow → 6 digits)

### BR-OPD-004 — Visit-type sequence state machine
- **Rule**: Revisit by same doctor within `Visittypemaster.encountervalidity` (validity+1 days) advances to next sequence visit type; no prior visit → sequence=1 type; emergency → visit type with `isEmergency=true`.
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:1052-1107`
- **Tables**: M_VISITTYPEMASTER, T_Visit, T_Encounter | **Tests**: + new patient seq1; + revisit within validity → next seq; + revisit after validity → restart

### BR-OPD-005 — Sponsor defaulting on OPD visit
- **Rule**: Company default `config.selfcompanyid`, tariff `config.selftariffid`, `priorityId=1`; plan required per sponsor else `BERP008`. Stored via `CIMS_ADDPATIENTSPONSOR_NEW_DOC`.
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:1359-1365, 1729-1794`
- **Tests**: + defaults applied; − missing plan → BERP008

### BR-OPD-006 — Walk-in requires doctor
- **Rule**: Non-appointment walk-in with no doctorId → `BERA022` (unless appointmentAgainstUnitHead resolves unit-head doctor, else `BERA029`).
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:380-388, 628-630`
- **Tests**: + doctor; − none → BERA022

### BR-OPD-007 — Appointment payment advances VisitStatus
- **Rule**: After OPD bill settlement, `T_Appointment.VisitStatus=11` (only if < 11) and, when checkout enabled, `T_Encounter.isCheckout=true` + Solr update.
- **Evidence**: `AddPaymentServiceHelper.java:438-447, 477-478, 820`
- **Migration**: VisitStatus 11 is the kiosk/queue "billed" signal (JS `getLastVisitStatus`)
- **Tests**: + settled → 11; − VisitStatus already 12 → unchanged

### BR-OPD-008 — MLC (medico-legal) registration on visit
- **Rule**: MLC flag + `mlcDetailsDto` (broughtBy*, broughtByName, broughtByRelationId, broughtByMobileNo, isBroughtDead, formalityRemark) on walk-in visit creation → `MLCDetails` row created with the encounter + audit event `MLCDETAILS.ADD`; brought-dead (`isBroughtDead`) propagates `isdeath` to `T_Registration_Details_int` at registration.
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:1210-1214, 1438-1481`; `PatientServiceHelper.java:825-829`; `walkinVisit-schema.json` mlcDetailsDto block
- **Tables**: T_MLCDETAILS, T_Encounter, T_Registration_Details
- **Migration**: MLC is part of ADT context (`10-target-architecture.md` §2.1 owns T_MLCDETAILS)
- **Tests**: + MLC visit → row + audit; + brought-dead → isdeath propagated; − non-MLC visit → no row

### BR-OPD-009 — Walk-in validation gates
- **Rule**: Walk-in visit input gates: script-injection text check → `BEXA002`; ext-LMIS mode → address + mobile required; duplicate PCTS number → `BEXA034`; patient with an active (non-leave) admission cannot be walked in → `FWAD001`. Error codes are contract (`04-backend-architecture.md` §9.6).
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:315-319, 325-332, 345-350, 371-378`
- **Tests**: − injection text → BEXA002; − ext-LMIS missing address/mobile → error; − duplicate PCTS → BEXA034; − active admission → FWAD001

### BR-IPD-001 — Admission preconditions
- **Rule**: admitting doctor required (`BERA022`); if `allowDirectAdmission` off → existing registration required (`BERB009`); no other active admission (`BERB013`); baby admission requires a cradle ward in unit (`BERB012`); attending doctor defaults to admitting doctor.
- **Evidence**: `PatientRegAdmissionServiceImpl.java:249-281`
- **Tests**: + ok; − each violation → respective code

### BR-IPD-002 — Bed allocation algorithm (auto-allocation)
- **Rule**: Free bed = `OCCUPIED=false` AND `status=true` AND `isnoncensus/isot/ispreop/ispostop=false`. Priority chain: (1) same ward+building+category, (2) waiting-bed, (3) floating-bed, (4) waiting+floating. Claim via atomic `UPDATE ... SET OCCUPIED=true WHERE OCCUPIED=false`; failure → `BEXB001`. Auto-allocation off + no bed → interim ward (unbedded) admission.
- **Evidence**: `PatientRegAdmissionServiceImpl.java:283-358, 1400-1402`
- **Tables**: M_BEDMASTER, T_ADMISSIONDETAILS | **Tests**: + first-fit; + waiting fallback; + none → BEXB001; boundary: concurrent claims (CAS)

### BR-IPD-003 — Bed status release gate
- **Rule**: Bed's `M_BedStatusMaster.isrelease` must be true at allocation → else `BERB005`.
- **Evidence**: `PatientRegAdmissionServiceImpl.java:1289-1301`
- **Tests**: + release status ok; − non-release → BERB005

### BR-IPD-004 — IPD number template
- **Rule**: `ipdNo = format(ipdNoTemplate, unitprefixformrno, year-2000, %05d seq)` → `A/<PREFIX>/<YY>/<00001>`; `CIMS_ADDADMISSIONINFORMATION` inserts T_Admission.
- **Evidence**: `PatientRegAdmissionServiceImpl.java:1236-1252, 1265-1268`; `application.properties`
- **Note**: Java's `resultStatus==-1 → APP015` check is effectively dead (proc always ends 0) — RISK R4.

### BR-IPD-005 — Interim-admission auto-abscond
- **Rule**: Admission with `BEDID=0/null`, `ISCANCEL=0`, and `ADDEDDATETIME ≤ now − 3 days` → marked `ISCANCEL=1` + discharge-type "auto absconded" + `ABSCONDDATETIME` (job `${interimAdmission.abscond.schedule}`).
- **Evidence**: `ApplicationJobs.java:991-1019`
- **Tests**: + 3-day threshold; boundary: exactly 3 days (`<=`)

### BR-IPD-006 — Ambulance service carry-over on admission
- **Rule**: `CIMS_ADDADMISSIONINFORMATION` copies `T_AmbulanceServiceDetails` rows onto the new admission when a prior visit ≤ 6h before admission availed ambulance service; carry-over logic must port to the ADT service (data moves with the admission row).
- **Evidence**: `generated-procs/CIMS_ADDADMISSIONINFORMATION.sql:105-187`
- **Tables**: T_AmbulanceServiceDetails, T_Admission
- **Tests**: + visit ≤6h + ambulance flag → copied; + >6h → not copied; − no ambulance → no copy

### BR-DIS-001 — Discharge preconditions
- **Rule**: Active advised discharge required (`BERB023`); discharge/ death/handover dates ≥ admission date; discharge date ≥ last charge `addedDateTime`; pending patient amount > 0 (excluding gate-pass service) → `BERB015`; cradle babies with non-finalized bills → `BERB011`; photo + discharge doc + feedback doc must come as a set (`BERB023/24/25`); transfer-to-other-hospital triggers inter-hospital admission request + COVID referral.
- **Evidence**: `PatientTransferDischargeServiceImpl.java:389-509`
- **Tables**: T_AdviseDischarge, T_Discharge | **Tests**: + clean discharge; − each violation

### BR-DIS-002 — Discharge execution & death handling
- **Rule**: `CIMS_ADDBEDDISCHARGE` inserts T_Discharge; if discharge type `IsExpired=1` → `T_Registration_Details.isdeath=1`, T_Discharge.IsDeath/DeathDate/HandOvrDate updated, PatientDeathDetails saved, patient Solr `death=true`; `CIMS_UPDATEIPDISDISCHARGE` sets `ISDischarged=1, IsClosed='1'` (+ babies, + T_TransferRequest closed); Solr encounters cleared unless death.
- **Evidence**: `PatientTransferDischargeServiceImpl.java:720-831`; `generated-procs/CIMS_ADDBEDDISCHARGE.sql:24-41`; `CIMS_UPDATEIPDISDISCHARGE.sql`
- **Tests**: + normal; + death; + baby delink/decouple

### BR-DIS-003 — Bed release on discharge
- **Rule**: Bed `OCCUPIED=false` + `BedStatusId=6`; universal (amenities) bed restored to origin; cradle (non-census) bed re-mapped to cradle ward.
- **Evidence**: `PatientTransferDischargeServiceImpl.java:779-803`
- **Tests**: + normal; + universal bed; + cradle bed

### BR-BILL-001 — Bill creation & numbering
- **Rule**: Header created with `Status=true, IsSettled=false, BillNo='000000000000000'`, `CashCounterId=1`; final bill number from `getLatestAutocodeSeq("T_OPDBill", op.bill.no.template)` / IP variant; net amount recomputed `Total − Tax − Concession` at finalize; `CIMS_ADDBILL` inserts (IsCancelled=0).
- **Evidence**: `AddServiceOrderServiceHelper.java:217-341`; `generated-procs/CIMS_ADDBILL.sql`
- **Tests**: + draft (no number); + final (number); boundary: sequence formatting

### BR-BILL-002 — Doctor share split
- **Rule**: Skip if tariff-service `otProcedure=true`. If share% > 0: `doctorShare = totalAmount × %/100`; `hospitalShare = total − doctorShare`; recorded via `CIMS_ADDDOCTORSHARE` (IsBilled=false) with CEILING/HALF_UP rounding (CEILING for shares in DB, HALF_UP on DTO).
- **Evidence**: `AddServiceOrderServiceHelper.java:383-419`
- **Tests**: + share>0; + share=0 → none; − OT service → none

### BR-BILL-003 — Patho/Rad order auto-creation at billing
- **Rule**: For charges whose specialization = patho (1) / radio (2): `CIMS_BILLFORINVESTIGATIONNEW` result 0 → create order booking + one detail per sample defined in `M_PathoTestSample` (`SampleCollectionNO=1..n`, `SampleNo=0`, `IsOutSourced=false`, `PathologistId=0`); rad analog with `IsImageCaptured=false`.
- **Evidence**: `AddServiceOrderServiceHelper.java:421-633`
- **Tables**: T_PATHORDERBOOKING, T_PATHORDERBOOKINGDETAILS, T_RADIOLOGYORDERBOOKING(DETAILS)
- **Tests**: + patho bill → orders; + rad bill → orders; + non-patho → none; boundary: 0 samples

### BR-BILL-004 — Tax calculation
- **Rule**: Up to 3 taxes (SGST/CGST/IGST labels, `allowNumberOfTaxCount=2` in use): `tax = rate × pct/100`; if `Taxmaster.applyafterconcession` → `(rate − rate×conPct/100) × pct/100`. Tax is added to SelfAmount; `TaxAmount = Σ taxN`.
- **Evidence**: `AddServiceOrderServiceHelper.java:2767-2798`; `application.properties`
- **Tests**: + tax before concession; + tax after concession; + no taxId in master → 0

### BR-BILL-005 — Co-pay / concession split
- **Rule**: `selfRate = price × coPayPerc/100`; `nonSelfRate = price − selfRate`. Self-company: concession applies to selfRate (plan% then bill% **added**). Non-self-company: concession applies to nonSelfRate (plan% then bill% **replaces** — asymmetric, RISK R2). Deductable 0 → 100 (i.e., full self-pay). Net = self + nonSelf.
- **Evidence**: `AddServiceOrderServiceHelper.java:2800-2851`
- **Tests**: + self company; + non-self company; + both concession tiers (self adds, non-self replaces — divergence test!)

### BR-BILL-006 — Bill settlement & OPD checkout
- **Rule**: Settled ⇔ self & non-self balances ≤ 0 (`CIMS_UPDATEBILLFORCONCESSION`). OPD + not admission-appointment → `T_Encounter.isCheckout=true` + Solr + `VisitStatus=11`. Cash change (`changeAmount`) recorded on T_Bill when `invstopdpostbilling`.
- **Evidence**: `AddPaymentServiceHelper.java:438-506`
- **Tests**: + full payment → settled+checkout; + partial → unsettled

### BR-BILL-007 — Advance adjustment (FIFO)
- **Rule**: Advances with balance>0 consumed against self balance in id order; partial advance split; consumed advances recorded as payment-mode 7 + `CIMS_ADDPAYMENTADVANCEDETAILS`.
- **Evidence**: `AddPaymentServiceHelper.java:178-203, 313-349`
- **Tests**: + full advance; + partial; + multiple advances order

### BR-BILL-008 — Refund/cancellation restore
- **Rule**: Advance refund: transactionType=3, requires advance balance>0 (`BEXAR002`). Bill refund: transactionType=4. Settled-bill cancellation: `T_Advance Used−=n, Balance+=n`; charges restored from `*BeforeFinalization` + `IsCancelled=true`; `T_Itemsale BillID=0, IsBilled=false`; doctor-share status=false.
- **Evidence**: `AddRefundServiceImpl.java:82-155`; `BillCancellationServiceImpl.java:231-295, 347, 470-509`
- **Tests**: + advance refund; + bill cancel restores; − zero balance → BEXAR002

### BR-PHAR-001 — Dispensing completes visit (VisitStatus 12)
- **Rule**: Each item-sale detail `DISPENSED=true`; when all details of an item sale dispensed → `T_ITEMSALE.DISPENSED=true`; when no undispensed item sales remain for the visit → `T_Appointment.VisitStatus=12`.
- **Evidence**: `MedicationServiceImpl.java:799-821`
- **Tests**: + full; + partial (detail level); boundary: last item sale

### BR-PHAR-002 — Prescription closure on sale
- **Rule**: `CIMS_ADDITEMSALESDETAILS_NEW` inserts sale detail (BatchId) and closes prescription: `IsClosed='1', PendingQty=0` (or `PendingQty−=qty`), master `IsClosed='1'`; compound details closed. Dispense flags `isverified/isdispensed/ispartialdispense` on T_PRESCRIPTIONDETAILS.
- **Evidence**: `generated-procs/CIMS_ADDITEMSALESDETAILS_NEW.sql:103-121`; `PharmacyServiceImpl.java:735-746`
- **Tests**: + full qty → closed; + partial → pending reduced

### BR-PATH-001 — Sample collection locks charge removal
- **Rule**: Patho/rad charge with sample accepted (`isaccepted=true` on latest booking detail) cannot be removed unless `autoChargeCancellationOnRefund` → else `LEX001`; removable → booking `ISCANCELLED=true`; nursing → `T_PATIENTNURSINGORDERS.orderstatus=DISCONTINUE`; procedure → EMR service/procedure/schedule status=false.
- **Evidence**: `AddServiceOrderServiceHelper.java:3693-3719, 3751-3862`
- **Tests**: + no sample → cancel ok; + collected → LEX001; + refund-config → cancel ok

### BR-OT-001 — OT scheduling prerequisites
- **Rule**: Patient needs active admission (else latest visit); none → `BEOT004`; schedule/OT-details/procedure rows created; dependent-surgery discount% applied; procedure rate from tariff (0 if none).
- **Evidence**: `OTSchedulerServiceImpl.java:616-800`
- **Tests**: + admitted; + OPD visit; − neither → BEOT004

### BR-OT-002 — OT pre-billing
- **Rule**: `otprocedure.pre.billing=true` → procedure-specialization charges are created `consumed=true` (direct charge, not temp) so they bill upfront; doctor-share skipped for OT tariff services; ordered procedures query filters `isOtProcessDone=false`.
- **Evidence**: `AddServiceOrderServiceImpl.java:2202-2204`; `AddServiceOrderServiceHelper.java:383-386`; `OTSchedulerServiceImpl.java:4069-4077`
- **Tests**: + pre-bill on; + pre-bill off (temp charge path)

### BR-BLD-001 — Blood requisition status machine
- **Rule**: 1=new; after cross-match → 2 if cumulative cross-matched volume ≥ ordered volume else stays 1; after issue → 3 if cumulative bag volume ≥ ordered volume; cancel/expire → 10.
- **Evidence**: `BloodBankServiceImpl.java:6290-6317`; `BloodBankConstants.java:31-33`
- **Tests**: + partial cross-match; + full; + issue; + expire → 10 (details-level; header asymmetry — RISK R7)

### BR-BLD-002 — Auto decross after 48h
- **Rule**: Daily 00:00 job: crossmatched (status=1), not decrossed, `crossMatchedTestDate` ≥ 2880 min old → decross bag (`crosSMatched=false`), requisition status → 1, count decremented, `BloodBagDecross` reason=0.
- **Evidence**: `ApplicationJobs.java:1045-1061`; `BloodBankServiceImpl.java:7657-7701`; `BloodBankConstants.java:38`
- **Tests**: + <48h untouched; + ≥48h decrossed; boundary: exactly 2880

### BR-BLD-003 — Bag expiry handling (daily 23:00)
- **Rule**: Bags with `expirydate = today` → `isbagexpired=true` (issued bags skipped); if crossmatched & unissued → decross + requisition status back to 1.
- **Evidence**: `ApplicationJobs.java:769-815`
- **Tests**: + expiring today; + already issued; + crossmatched → decross

### BR-BLD-004 — Requisition expiry
- **Rule**: `requesteddate + orderValidationPeriod(h) ≤ sysdate` and sample not collected and status=1 → details `requisitionstatus=10`, `isorderexpired=true`, reason "Order is expired".
- **Evidence**: `ApplicationJobs.java:734-766`
- **Tests**: + expired → 10; + collected → untouched

### BR-BLD-005 — Donor auto-cancellation
- **Rule**: Donor in DNR_NEW(1)/DNR_FIT(2) with no action within 72h → donationStatus=7 (DNR_CANCELLATION).
- **Evidence**: `ApplicationJobs.java:832-848`; `BloodBankConstants.java:8-17`
- **Tests**: + 72h boundary; + post-med-exam statuses untouched

---

## C. FIVE RISKIEST CROSS-LAYER RULES
1. **VisitStatus 11/12 split-brain** (`BR-OPD-007`, `BR-PHAR-001`): billing writes `T_Appointment`, clinical writes `T_Encounter`, same numeric codes, two tables; kiosk/queue (`getLastVisitStatus`) reads one side. Migration must unify the visit lifecycle.
2. **Non-self concession overwrite** (`BR-BILL-005` R2): `getConcessionDetails` adds bill% to plan% for self company but replaces for non-self — silent pricing drift for corporate patients; needs a golden-master test.
3. **DNR_REJECTED == DNR_DONATED == 5** (`A8`): single literal, two meanings in donor workflow — statuses are ambiguous in reports/audit.
4. **Bed release hard-coded `BedStatusId=6`** (`BR-DIS-003` R5): discharge SQL writes literal 6 while allocation validates against master `isrelease` — master data change breaks release semantics; contract must be pinned.
5. **Dead admission failure path** (R4): Java `resultStatus==-1 → APP015` cannot fire (`CIMS_ADDADMISSIONINFORMATION` ends with 0) — admission duplicates/uniqueness rely on pre-checks (`BERB013`) instead of the proc; race conditions at concurrent admission for same patient are unguarded at DB level.
