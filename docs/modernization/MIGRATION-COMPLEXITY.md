# iHMS / OHUM PAS NG — Module Complexity Assessment

**Status: PRELIMINARY — synthesized from discovery (agents 04, 05, 06, 07, 09, 10 + reconciliation). No new exploration.**
**Purpose:** per-module migration complexity to calibrate waves, staffing and risk budgets. Scores are 1 (low) – 5 (high); totals out of 40. **TO-BE** items (gateway, observability platform) are scored only where they migrate legacy surface.

---

## 1. Scoring table (29 candidate services, agent-10 §6)

| Rank | # | Module (candidate service) | Bus | Tech | DB | Int | FE | Coup | DataMig | Risk | **Total** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 5 | Billing & Settlement | 5 | 5 | 5 | 4 | 5 | 5 | 5 | 5 | **39** |
| 2 | 4 | Ordering & Prescription | 5 | 5 | 5 | 4 | 3 | 5 | 5 | 5 | **37** |
| 3 | 10 | Inventory & Procurement | 4 | 5 | 4 | 4 | 5 | 5 | 4 | 4 | **35** |
| 4 | 1 | Patient Identity & Registration | 4 | 5 | 4 | 4 | 3 | 5 | 5 | 4 | **34** |
| 5 | 6 | Payments & Refunds | 4 | 4 | 4 | 3 | 4 | 4 | 4 | 5 | **33** |
| 5 | 9 | Pharmacy & Dispensing | 4 | 4 | 5 | 3 | 4 | 5 | 4 | 4 | **33** |
| 7 | 7 | Lab (Pathology) | 4 | 4 | 4 | 4 | 4 | 4 | 4 | 4 | **32** |
| 8 | 2 | Appointment & Encounter (OPD) | 4 | 4 | 4 | 3 | 4 | 5 | 4 | 3 | **31** |
| 9 | 3 | ADT & Bed | 4 | 4 | 4 | 3 | 4 | 4 | 3 | 4 | **30** |
| 9 | 21 | Reference Master Data | 3 | 5 | 4 | 3 | 4 | 5 | 4 | 2 | **30** |
| 9 | 25 | Interop & Notifications | 4 | 5 | 4 | 5 | 1 | 4 | 4 | 3 | **30** |
| 12 | 11 | OT & Surgery | 4 | 4 | 3 | 3 | 4 | 3 | 3 | 5 | **29** |
| 12 | 12 | Nursing & Inpatient Care | 4 | 5 | 3 | 2 | 5 | 3 | 3 | 4 | **29** |
| 12 | 22 | Security & IAM | 3 | 4 | 3 | 4 | 2 | 5 | 3 | 5 | **29** |
| 15 | 13 | Blood Bank | 4 | 3 | 3 | 3 | 3 | 3 | 3 | 5 | **27** |
| 16 | 26 | Reporting & Analytics | 3 | 4 | 3 | 3 | 3 | 4 | 3 | 3 | **26** |
| 17 | 8 | Radiology | 3 | 3 | 3 | 2 | 3 | 4 | 3 | 3 | **24** |
| 18 | 23 | RCM & Claims | 3 | 3 | 2 | 4 | 2 | 3 | 2 | 3 | **22** |
| 18 | 27 | Search & Index (Solr) | 2 | 4 | 1 | 3 | 1 | 5 | 4 | 2 | **22** |
| 18 | 28 | Govt Programs (EJS/Aasmaan/Chiranjeevi) | 3 | 3 | 3 | 3 | 2 | 2 | 3 | 3 | **22** |
| 21 | 24 | RMRS & PMRD (Treasury) | 3 | 2 | 2 | 2 | 3 | 3 | 2 | 4 | **21** |
| 22 | 29 | Queue & Kiosk | 3 | 3 | 2 | 2 | 3 | 3 | 2 | 2 | **20** |
| 23 | 15 | CSSD | 3 | 2 | 2 | 2 | 2 | 3 | 2 | 3 | **19** |
| 24 | 18 | Equipment Management | 3 | 2 | 3 | 2 | 2 | 2 | 2 | 2 | **18** |
| 25 | 14 | Diet | 2 | 2 | 2 | 1 | 2 | 2 | 2 | 2 | **15** |
| 25 | 19 | Bio-Medical Waste | 2 | 2 | 2 | 1 | 2 | 2 | 2 | 2 | **15** |
| 27 | 17 | Transport | 2 | 2 | 2 | 1 | 2 | 2 | 2 | 2 | **14** |
| 27 | 20 | Grievance & Feedback | 2 | 2 | 2 | 1 | 2 | 2 | 2 | 1 | **14** |
| 29 | 16 | Linen & Laundry | 2 | 1 | 2 | 1 | 2 | 2 | 2 | 1 | **13** |

Legend: **Bus**=business complexity · **Tech**=technical complexity · **DB**=DB complexity · **Int**=integration complexity · **FE**=frontend complexity · **Coup**=coupling · **DataMig**=data migration risk · **Risk**=clinical/financial risk.

---

## 2. Complexity ranking — top 5 (the risk budget is here)

1. **Billing & Settlement (39/40)** — the settlement spine. Every dimension is maxed: 10 transactional methods on `BillServiceImpl` alone; `CIMS_ADDBILL` family; bill-number templates (`BR-BILL-001`); tax/concession/co-pay arithmetic (`BR-BILL-004/005`); doctor-share CEILING rounding (`BR-BILL-002`); 77 billing schemas + 93 templates; `T_BILL` shared with payments/pharmacy/RCM/RMRS/reports; GST retrospective rule (2017-07-01) on `BaseServiceImpl.java:521-533`. One of the three critical-cutover tables (`T_BILL`).
2. **Ordering & Prescription (37/40)** — owner of the mega-transaction (`AddServiceOrderServiceHelper.java:341-516`): bill + lab + rad + doctor-share + EMR + HL7 in one ACID unit; charge lifecycle with `T_CHARGES_TEMP`; concession asymmetry (`BR-BILL-005`); sample-collection locks charge removal (`BR-PATH-001`); HL7 ORM emission in txn (`:2299-2399`). Cannot be decomposed without saga + outbox + compensation design (agent-10 §9 risk 1).
3. **Inventory & Procurement (35/40)** — god class (12,070 LOC, 235 endpoints) + `UploadFileServiceImpl` (294 edges); `T_ITEMSTOCK` is the highest-contention unit-of-work (pharmacy dual-write at `PharmacyServiceImpl.java:2140`); 2-level approval workflows; eAushadhi/Kaysons/RMSC; 70 schemas/196 templates (indent→freeze→approve→PO→GRN state machine).
4. **Patient Identity & Registration (34/40)** — `T_REGISTRATION*` has **no entity mapping** (reconciliation §4 — schema truth only in Oracle; highest data-migration risk in the system); realtime Solr index in txn (`PatientServiceImpl.java:5610`); MRN generation (`BR-PAT-001`) and duplicate detection via Solr (`BR-PAT-002`, BERA011/BEXA040); ABDM linkage (`T_PATIENT_ABDM`); HL7 on registration (`HelperServiceImpl.java:221`); 30 `T_PATIENT*` tables read by every module.
5. **Payments & Refunds (33/40) / Pharmacy & Dispensing (33/40) (tie)**
   - Payments: money movement — proc chain `CIMS_ADDPAYMENT→…→CIMS_UPDATEBILLFORCONCESSION` (`AddPaymentServiceHelper.java:291-348,463`); advance FIFO (`BR-BILL-007`); refund/cancellation restore (`BR-BILL-008`); treasury/RMRS downstream; financial risk 5.
   - Pharmacy: stock dual-write hotspot; `VisitStatus=12` dispensing completion (`BR-PHAR-001`); prescription closure (`BR-PHAR-002`); BCMA/chemotherapy surface; drug-dispensing clinical risk.

---

## 3. Evidence notes per module

**1. Patient Identity & Registration (34)** — Bus: MRN strategies, temp→permanent promotion, offline units (`BR-PAT-001`). Tech: Solr realtime-in-txn, ABDM queue entry, HL7 in txn. DB: unmapped T_REGISTRATION core (agent-05 §3.6). Int: ABDM/ABHA, Bhamashah, JanAadhaar, e-Mitra lookups. FE: 60-field walk-in form (agent-09 §11.1). Coup: shared tenant record (agent-10 §3). DataMig: unmapped schema + identity continuity. Risk: identity errors propagate everywhere.

**2. Appointment & Encounter (31)** — Bus: slot collisions BERA028/BEXA029, visit-type state machine, sponsor defaulting (`BR-OPD-001..007`). Tech: one method bundles encounter+queue+MLC+FPV+HL7+corona-stats+Solr+ABDM (`EmergencyAndWalkinVisitServiceImpl.java:1379-1937`). DB: T_ENCOUNTER highest-shared (52 refs, agent-05 §7). FE: 43 schemas/73 templates. Coup: 5 — shared tenant record. Risk: VisitStatus 11/12 split-brain (agent-06 §C.1).

**3. ADT & Bed (30)** — Bus: admission preconditions (BERB005/009/012/013), bed CAS allocation (BEXB001), auto-abscond, discharge/death/MLC (`BR-IPD-001..005`, `BR-DIS-001..003`). Tech: `CIMS_ADDADMISSIONINFORMATION` multi-table proc; bed OCCUPIED flag via proc (`CIMS_UPDATEBEDOCCUPIEDFLAG_1` `IPDAdmissionServiceImpl.java:313`). DB: T_ADMISSION shared; M_BEDMASTER statuses (BedStatusId 5/6/0/1/3 — hard-coded 6 risk, agent-06 §C.4). Int: HL7 A01 in txn; bed auto-charge job → billing. FE: 65 schemas/120 templates (bed board, transfer, discharge, clearance, mortuary).

**4. Ordering & Prescription (37)** — see ranking §2. Tech 5 additionally: EMR write + package subscription + credit note in same txn family (`AddServiceOrderServiceImpl.java:2351,2526,2680`). Coup 5: bill+lab+rad+share+emr+hl7. DataMig 5: charge/bill continuity and *BeforeFinalization* snapshot restore semantics (`BillCancellationServiceImpl.java:488-509`).

**5. Billing & Settlement (39)** — see ranking §2. FE 5: billingFlow/estimation/advance/refund/receipts/credit-note/doctor-share/sponsor/package/IP-OP summaries (agent-09 §2.3).

**6. Payments & Refunds (33)** — Bus: payment modes, advances, refunds, cancellations, receipts; Tech: 4-proc chain + Redis upload-dedup counters (`BillReceiptServiceImpl.java:3137-3145`); DB: T_PAYMENT/T_ADVANCE/T_REFUND + bill updates; Int: RMRS, RCM claim stage, receipts; FE: billing payment screens; Coup 4: settlement state; DataMig 4: balance continuity; Risk 5: cash handling.

**7. Lab (Pathology) (32)** — Bus: specimen→parameter→result→review→release double-verify→critical alert state machine; Tech: god class 14,629 LOC / 133 endpoints (agent-04 §9); machine interface (KDL/Backman OLIS jar); DB: booking in mega-txn + `T_PATHOPATIENTPARAMETERDETAILS` volume; Int: WhatsApp report delivery (`PathoOrderServiceHelper.java:1130`), EHR, eUpkaran/EXT LIMS; FE: 57 schemas (stage-parameterized worklist — agent-09 §11.6); Coup 4: LabOrderCreated→Billing, LabResultFinalized→Interop; Risk 4: result correctness.

**8. Radiology (24)** — Bus: booking/modality/templates/results (small); Tech: booking inside `AddServiceOrderServiceHelper.java:579-599`; DB: T_RADORDERBOOKING self-contained; FE: 9 schemas/6 templates; Coup 4: RadOrderCreated→Billing; Risk 3: diagnostic reports.

**9. Pharmacy & Dispensing (33)** — Bus: dispensing, compound, ward picklist, interventions, VisitStatus 12; Tech: 3,930 LOC + CIMS_ADDITEMSTOCK dual-write (`:2140`) + `CIMS_SETEMRNO` (`:886`); DB 5: T_ITEMSTOCK highest contention; Int: Reference item master, Inventory stock API, BCMA; FE: 39 schemas/143 templates; Coup 5: stock + billing + clinical; Risk 4: dispensing safety.

**10. Inventory & Procurement (35)** — see ranking §2. DB 4: M_ITEMMASTER/T_ITEMSTOCK/batch shared with pharmacy/lab/OT/CSSD/blood bank/diet; Int 4: eAushadhi + Kaysons + auto-indent jobs (`ApplicationJobs.java:2197,2376`); FE 5: 70 schemas/196 templates.

**11. OT & Surgery (29)** — Bus: scheduling preconditions BEOT004, dependent-surgery discount, pre-billing (`BR-OT-001/002`); Tech: `OTSchedulerServiceImpl` + Solr patient search (`:4879`); FE: 14 schemas/48 templates (schedule board, procedure performance); Risk 5: surgical/anesthesia safety, procedure audit trail.

**12. Nursing (29)** — Tech 5: 17,350 LOC / 300 endpoints (agent-04 §9); ROWNUM queries (`:7124`); FE 5: 371 templates — largest; Bus 4: notes/vitals/tasks/rostering/TIUXML/handover; Risk 4: legal documentation, vitals safety.

**13. Blood Bank (27)** — Bus: donor/bag/requisition state machines with 4 daily jobs (`BR-BLD-001..005`); Risk 5: transfusion safety; Int: eRaktkosh (`ApplicationJobs.java:1205,1383`), Solr encounter links, lab serology.

**14. Diet (15)** — simplest transactional domain: `T_DIETORDER/T_DIETITEMSCHEDULEDETAILS/T_TODAYDIETITEMMENU`; only `SYSDATE/to_char` constructs (`DietServiceImpl.java:154`).

**15. CSSD (19)** — kit formation/sterilization/issue; consumes `ProcedureScheduled` + `InventoryUpdated` (event-only); sterilization compliance risk 3.

**16. Linen (13)** — lowest total: single service (`LinenLaundryServiceImpl`), self-contained tables, no integration.

**17. Transport (14)** — self-contained (`T_TRANSPORTREQUEST/T_VEHICLEMAINTENANCE/M_AMBULANCEMASTER`); reads admission.

**18. Equipment (18)** — registry/complaints/PM/warranty/AMC + CLOB docs (agent-05 §3.1); spares from Inventory; AMC cost to Finance.

**19. Bio-Medical Waste (15)** — `T_BIOMEDICALWASTE(+DETAILS)` self-contained; reads ward.

**20. Grievance & Feedback (14)** — `T_PATIENTCOMPLAINTS/T_PATIENTGRIEVANCE*/T_FEEDBACKTEMPLATE`; reads patient; Mera Aspataal export lives in Patient service.

**21. Reference Master Data (30)** — Tech 5: god node 514 edges, 264 endpoints, cache contract (~375 keys), bulk upload; DB 4: 582 M_* tables; Coup 5: consumed by ~30 services; FE 4: 300 config-driven master screens (`mastersConfigration.json`, agent-09 §2.11); Risk 2: low direct, platform-wide breakage potential.

**22. Security & IAM (29)** — Bus: LDAP + SSO + per-unit/store/ward grants; Tech: auth woven into `ServiceLayerAspect` (per-request), session cache; Int 4: LDAP/AD, Rajasthan SSO, e-Mitra, OTP; Coup 5: every request; Risk 5: token compromise.

**23. RCM & Claims (22)** — eligibility/pre-auth/claims via NHCX/HCX with JWE (`NHCXServiceImpl.java:45-124`); reads bill/admission; consumes `PaymentReceived`.

**24. RMRS & PMRD (21)** — treasury collections/receipts; `ROWNUM=1` (`RMRSServiceImpl.java:1361-1366`); govt money accountability → Risk 4.

**25. Interop & Notifications (30)** — Int 5: 28 external integrations (agent-07 §3); Tech 5: `HL7Helper` 118 SELECTs/19 NEXTVAL, `IntegrationServiceImpl.java:1258-1410`, ABDM 50 endpoints + 8 jobs, 4 queue tables + pollers; DataMig 4: in-flight queue continuity.

**26. Reporting & Analytics (26)** — Tech 4: Jasper (`BillInvoiceGenerator.java:19-25`), Velocity, PDF-in-DB (`savePdfToTable :6067`), report dispatcher (Nashorn `reports.js`, reconciliation §1); Coup 4: read-model over all domains; Risk 3: regulatory SLAs (GSTR/NHM/RGHS).

**27. Search & Index (22)** — Coup 5: search embedded in registration/admission/appointment/blood-bank/requisition/EJS/ABDM/MDM (agent-07 §1.4); Tech 4: realtime+batch dual paths, SOLR_STATUS flags, rollback compensation (`ServiceLayerAspect.java:1164-1182`); DataMig 4: full reindex + 3-day encounter retention gap.

**28. Govt Programs (22)** — DB 3: 61 EJS (BigInteger IDs → numeric(38)) + 35 aasmaan (convention-mapped names — must verify, agent-05 §3.4); Int: EJS portal data, Chiranjeevi API, JanAadhaar; incentives → finance.

**29. Queue & Kiosk (20)** — Tech: QMS Redis queues (`QMSUtil:28-126`), TRUNC/to_date (`QueueManagmentServiceImpl.java:1477-1759`); UserV/DTol: token counters are high-volume front-desk; Coup: reads encounter; VisitStatus 11 signal consumer.

---

## 4. Cross-cutting observations (all modules)

- **The DTO/views surface dominates effort, not logic**: 1,583 DTO files + 155 views, much of it stored-proc-result-coupled (agent-04 §9.11) — every module carries a hidden tax when its *ResultDto-generating endpoints move.
- **No controller/service separation** (agent-04 §2): extraction is per-service-class, not per-layer; helpers (`AddServiceOrderServiceHelper`, `MasterCachedServiceHelper`, …) span boundaries and must be split with their modules.
- **Error-code contract**: opaque `BEXA*` codes resolved from bundles (agent-04 §9.6) must be preserved in new services (frontend `printErrorMessageByErrorMsg` depends on them).
- **PHI encryption**: `IdEncryptionUtil` + aspect decrypt of params (agent-04 §8) — the gateway must not break encrypted-param flows during routing splits.
- **Module complexity does not equal wave order**: Wave placement (MIGRATION-WAVES) weighs *dependencies* and *risk appetite*; e.g. Billing is #1 complexity but Wave 4, while Diet (#25) is Wave 2. The pairing of a complexity-ranked module with its dependencies is deliberate.
