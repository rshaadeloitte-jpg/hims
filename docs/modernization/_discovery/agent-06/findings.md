# AGENT 06 — Findings (Evidence Dump)
## CROSS-MODULE BUSINESS LOGIC TRACER — iHMS/OHUM PAS NG (D:\ihms)
Date: 2026-08-14 | Mode: RESEARCH-ONLY (no code modified)

---

## 1. WORKFLOW TRACES (numbered steps, file:line evidence)

### 1.1 PATIENT REGISTRATION
UI: schema-driven FAUG form `WalkinVisitSchema` (`js/appointment-module.js:156`, schema `public/modules/op/schemas/walkinVisit-schema.json:4` dynamicAspects — `gvar@isExtLMISEnabled` forces mobileNo/resAddress required; `gvar@app_type_w` s/k variants force field visibility).

1. JS posts to `api/rest/patient/registerPatient` (batch) — `pas/PatientServiceImpl.java:3519` (`@POST /registerPatient`, `@Transactional(rollbackOn = Exception.class)`).
2. Delegates per-dto to `PatientServiceHelper.registerPatient` — `custom/PatientServiceHelper.java:232-987`.
3. Name normalization: name → firstName (first 3 tokens) + lastName (4th token onward) — `PatientServiceHelper.java:288-296`.
4. Identity duplicate check via Solr: `findPatientByIdentity(0L, body)`; non-null → throw `BERA011` — `PatientServiceHelper.java:661-669`.
5. MRN generation (three modes) — `PatientServiceHelper.java:373-425`:
   - `mrnoFromSeries=true` (property) → `helper.validateOrGenerateMrno(...)`; blank result → `IsTempMrno=true`.
   - `autoGenerateTempMrnNo=true` && `isAppt` → temp MRN via SQL fn `cims_getlatestautocodeandincremetitbyone('T AppoinmentTempMrn', ...)`; format `<tempMrnPrefix>-000N` (padded 3) — `PatientServiceHelper.java:379-407`.
   - Offline unit (`unit.getIsOffline()`) → client MRN used as-is — `PatientServiceHelper.java:413-415`.
   - Permanent MRN upgrade later: `setPermanentMrno` = `ddMMYY + leftPad(patientId,9,'0')` on T_Registration_Int, `IsTempMrno=0` — `BaseServiceImpl.java:1007-1017`. Hex variant `generateHexMrno` `BaseServiceImpl.java:987-989`.
6. DB: `CIMS_ADDPATIENTREGISTRATION` proc → T_Registration (patient id, MRNo) — `PatientServiceHelper.java:726-729`; `CIMS_ADDPATIENTREGISTRATIONDETAILS` → T_Registration_Details — `PatientServiceHelper.java:802-804`.
7. Side effects: audit `CREATE.REG` (`:734-738`); ID documents to MinIO (`:730-749`); photo + thumbnail (`:750-762`); `IsTempMrno` for blank series MRN (`:378`).
8. Sponsor defaults: tariffId from sponsor[0], insuranceSchemeId/planId fallback — `PatientServiceHelper.java:281-291`.
9. Emergency: `isDeath=broughtDead` on T_Registration_Details_int (`:825-829`).
10. Solr index: `solrUtil.indexPatientInSolr(dto, prdto)` — `PatientServiceHelper.java:849`; impl `util/SolrUtil.java:198-203` (`solrEnabled` flag, `patient_risl_qa` collection — `application.properties`).
11. HealthId duplicate check: Solr search by healthIdNumber → `BEXA040` — `PatientServiceHelper.java:783-790`.
12. Update path (patientId>0): delta-update of T_REGISTRATION_DETAILS/T_REGISTRATION (insurance scheme, plan, address, name, mobile, DOB) then `indexUpdPatientInSolrMinimal` — `PatientServiceHelper.java:817-933`.

### 1.2 OPD — WALK-IN VISIT / APPOINTMENT
1. JS: `api/rest/walkinVisit/addwalkinVisit` — `js/appointment-list-module.js:1986`, `js/nhncModule.js:7180`.
2. `addwalkinVisit` → `addwalkinVisitNew` — `custom/EmergencyAndWalkinVisitServiceImpl.java:302-309`.
3. Input validation: script-injection text check → `BEXA002` (`:315-319`); ext-LMIS requires address+mobile (`:325-332`); duplicate PCTS No → `BEXA034` (`:345-350`).
4. Patient with active (non-leave) admission cannot be walked in → `FWAD001` (`:371-378`).
5. Non-residential tariff: resState != config.state → tariff `config.nonresidentialtariffId` else hard-coded `2L` (`:410-419`).
6. Appointment-slot rules (`:477-626`):
   - Same day + dept + patient active encounter → `BEXA029` (`:537-546`).
   - Same-hour collision in dept → `BERA028` (`:582-589`); appointments spaced ≥30 min (`:563`); default slot 15 min from `config.appointmentslot` (`:423-426`).
   - Future/same-day → `createAppointment` (telemedicine variant too) (`:606-624`).
7. Walk-in (no appointment): doctor required → `BERA022` (`:628-630`); patient registration via `registerPatient` (`:649`).
8. Temp-MRN → permanent upgrade at visit (`:678-684`).
9. Encounter creation (`:947-1156`): type=2 emergency / 1 normal (`:998-1002`); `visitstatus2="2"` default (`:1039`), `"9"` when compound-medication visit type (`:1140-1142`); visitMark=true (`:1194`); token via `CIMS_GETAUTOTOKENNO` when queueTokenBaseWorkFlow (`:1176-1186`); appointmentTypeId 1 online / 0 walk-in (`:1204-1208`); MLC flag + `MLCDetails` row + audit `MLCDETAILS.ADD` (`:1210-1214`, `:1438-1481`).
10. OPD no: `String.format(opdNoTemplate, unitprefixformrno, YEAR-2000, %05d)` — template `V/%s/%d/%s`; emergency uses `emerNoTemplate` `EV/%s/%d/%s` (`:1291-1308`); sequence from `Unitmaster.getOPDSequenceName(unitId)`.
11. Visit-type state machine: revisit same doctor within `Visittypemaster.encountervalidity` advances sequence; no visits → sequence=1 visit type (`:1063-1107`).
12. Encounter saved (`:1379`); sponsor row `CIMS_ADDPATIENTSPONSOR_NEW_DOC` with defaults companyId=selfcompanyid, tariffId=selftariffid, priorityId=1 (`:1729-1794`); Solr `indexEncounterInSolr` (`:1796-1800`).
13. Consultation charges auto-fetched into Solr cache `current_enc_<id>_E` (`:1838-1847`).
14. Last visit status read from T_Appointment (`:1861-1866`).

### 1.3 IPD ADMISSION + BED ALLOCATION
1. `patientRegAddmission/admitPatient` → `admitPatientInt` — `custom/PatientRegAdmissionServiceImpl.java:237-241`.
2. Validation: admitting doctor required → `BERA022` (`:249-251`); direct admission config gate → `BERB009` (`:256-260`); active admission exists → `BERB013` (`:261-266`); baby needs cradle ward → `BERB012` (`:267-275`).
3. Bed allocation (`:283-359`): bed free when `OCCUPIED=0`; exclusion `isnoncensus/isot/ispreop/ispostop=false`; fallback chain: same-building+category → waiting bed → floating bed → waiting+floating (`:311-342`); atomic `UPDATE M_BEDMASTER SET OCCUPIED=true WHERE ID=? AND OCCUPIED=false` (`:344`); none free → `BEXB001` (`:357`). Auto-allocation disabled + fromBedId=0 → interim ward admission (`:290-293`).
4. Bed status gate: `M_BedStatusMaster.isrelease=true` required → else `BERB005` (`:1289-1301`).
5. IPD no: `String.format(ipdNoTemplate, unitprefixformrno, YEAR-2000, %05d)` — `A/%s/%d/%s` (`:1236-1252`); `CIMS_ADDADMISSIONINFORMATION` → T_Admission; Java expects resultStatus -1 → `APP015` (but proc always ends 0 — dead check, see 4.5) (`:1265-1268`).
6. Bed occupied flag proc `CIMS_UPDATEBEDOCCUPIEDFLAG` (`:1303`); baby re-ward to cradle ward (`:1305`).
7. `CIMS_ADDADMISSIONDETAILS` → T_AdmissionDetails (bedId, status=true) (`:1481`); mother-child linking (`:1518-1541`); OP charges moved to IP `billingHelper.moveOPChargesToIP` (`:1546`); advance payment possible (`:1879`).
8. Emergency contact proc `CIMS_ADDADMISSIONADDEMERGENCYCONTACT` (`:1571`).
9. Ambulance carry-over: proc copies T_AmbulanceServiceDetails (visit ≤ 6h from admission + IsAmbulanceServiceAvailed) — `generated-procs/CIMS_ADDADMISSIONINFORMATION.sql:105-187`.
10. T_AdmissionDetails.IsClosed default 0; discharge sets IsClosed=1.

### 1.4 DISCHARGE & BED RELEASE
1. Advised discharge first: `addAdvisedDischargeList` — duplicate active advised → `BERB018`; re-advise updates Status=true (`custom/PatientTransferDischargeServiceImpl.java:907-946`); babies advised too (`:949+`).
2. `patientTransferDischarge/discharge` (`:389-902`): dates must be ≥ admission date (`:392-400`); advised discharge required → `BERB023` (`:402-405`); photo/doc/feedback triple required together (`:406-416` → `BERB023/024/025`); `validatePatientDischarge` → `BERB014` (`:423-425`); pending charges (gate-pass service excluded) > 0 → `BERB015` (`:426-438`); cradle babies with unfinalized bill → `BERB011` (`:450-453`); discharge date must be ≥ last charge date (`:454-460`).
3. Type "TRANSFER TO OTHER HOSPITAL" → admission request to other hospital + COVID referral update (`:463-508`).
4. Zero-bill discharge: creates final zero bill via `addServiceOrderServiceImpl.saveOrUpdateServices` (`:511-668`).
5. Babies discharged first (`helper.dischargeBabies`) (`:671-704`).
6. `CIMS_ADDBEDDISCHARGE` → T_Discharge insert; death type (IsExpired=1) sets `t_registration_details.isdeath=1` (`generated-procs/CIMS_ADDBEDDISCHARGE.sql:24-41`); Java then updates deathDate/handOvr on T_Discharge + saves `PatientDeathDetails` (`:752-769`).
7. `CIMS_UPDATEIPDISDISCHARGE` → T_AdmissionDetails ISDischarged=true + IsClosed='1'; T_TransferRequest IsClosed='1' (`:771-777`; proc `generated-procs/CIMS_UPDATEIPDISDISCHARGE.sql`).
8. Bed release: `CIMS_UPDATEBEDOCCUPIEDFLAG` occupied=false + `M_BedMaster BedStatusId=6` (`:782-793`); universal bed restore origin (`:796-798`); cradle bed re-mapped to cradle ward (`:800-803`).
9. Solr: `solrUtil.clearClosedEncounters` (unless death) (`:820-825`); death → patient Solr `death=true` (`:808-812`).
10. ABDM discharge summary queue entry (`:881`).

### 1.5 BILLING (order → bill → receipt/refund/credit note)
Order/charge:
1. `addservicecharge` (`custom/AddServiceOrderServiceHelper.java:195-843`): each charge → `CIMS_ADDCHARGE` (consumed/`createCharge`) or temp (`CIMS_ADDCHARGETEMP` when not consumed) — `:259-275`, `:2235-2239` (AddServiceOrderServiceImpl).
2. Bill header defaults: `Status=true`, `IsSettled=false`, `BillNo='000000000000000'` placeholder, `CashCounterId=1` (`:224-227`).
3. Bill no (final only): `getLatestAutocodeSeq(unitId, "T_OPDBill", opBillNoTemplate)` (`:335`); template `op.bill.no.template=%s/I/%d/%s`, `ip.bill.no.template=%s/I/%d/%s`.
4. Net amount recomputed at finalization: `TotalBill − TotalTax − TotalConcession` (`:333`).
5. `CIMS_ADDBILL` → T_Bill (proc `generated-procs/CIMS_ADDBILL.sql` — IsCancelled=0, IsPharmacy=0 defaults).
6. OPD: `T_ITEMSALE` ISBILLED=true, BILLID set (`:344-345`).
7. Per-charge `CIMS_ADDBILLDETAILS_1` (`:370`).
8. Doctor share (skip if tariff-service `otProcedure=true`): share%>0 → `DoctorShare = TotalAmount × share% / 100`, `HospitalShare = TotalAmount − DoctorShare`; proc `CIMS_ADDDOCTORSHARE`, `IsBilled=false` (`:383-419`).
9. Patho/Rad order auto-creation on bill: group = pathospecializationid → `CIMS_BILLFORINVESTIGATIONNEW` result 0 → `CIMS_GETPATHOTESTLISTFORBILL` → `CIMS_ADDPATHORDERBOOKING` (testType=1) + one `CIMS_ADDPATHORDERBOOKINGDETAIL` per sample (seq = M_PathoTestSample count) with `SampleCollectionNO=k+1`, `SampleNo=0`, `IsOutSourced=false`, `PathologistId=0` (`:421-519`). Rad analog `CIMS_ADDRADORDERBOOKING` + details (`:551-601`).
10. Package charges zeroed on bill (`:637-644`); charge marked IsBilled (`:636`).
11. Concession (bill-level): `CIMS_ADDCONCESSION` + `CIMS_ADDCONCESSIONDETAIL` per charge with concession>0 (`:704-744`).
12. Sponsor-based re-pricing after bill: priority-1 sponsor + class (IP class from admission, OPD config.opdclassid) → `findAllServicesForBill`; per service: `concession=false → concessionPercent=0`; `deductablePercent = 100 − deductable` when company == selfcompanyid (`:764-836`).

Tax/discount:
13. `getTaxAmount`: `tax = rate × perc/100`; if `Taxmaster.applyafterconcession` → `(rate − rate×conPer/100) × perc/100` (`:2775-2790`).
14. `getConcessionDetails` (co-pay split): `selfRate = orgPrice × coPayPerc/100`; `nonSelfRate = orgPrice − selfRate`; self-company → concession on selfRate (plan% then ADD bill% — note: added); non-self-company → concession on nonSelfRate (plan% then bill% REPLACES — asymmetry; see risk R5) (`:2800-2830`).
15. `setChargesAmountsBySponsors`: deductablePercent null/0 → 100 (`:2833-2835`); taxes computed only if taxAmount==0; tax added to SelfAmount; `netAmount = selfAmount + nonSelfAmount` (`:2838-2849`).

Payment:
16. `addBillPayment` (`custom/AddPaymentServiceHelper.java:62-521`): bill lookup (`CIMS_GETBILLLISTFORSEARCH_NEW_one`); advance adjust consumes advances FIFO against self balance (`:178-203`); partial payment ordering: compayPayment → nonSelf first, then self (`:215-253`); `finalTrxAmount = (balSelf − newBalSelf) + (balNonSelf − newBalNonSelf)` (`:255-265`).
17. `PaymentDto.transactionType=2` (bill payment), cashCounterId=1, receipt no via `getLatestAutocodeSeq("Payments", receipts.no.template)` (`:267-282`); `CIMS_ADDPAYMENT` → T_Payment; `CIMS_ADDPAYMENTBILLDETAILS` billType=1 with new balances (`:293-309`).
18. Advance-used payment modes recorded with `paymentModeId=7` + `CIMS_ADDPAYMENTADVANCEDETAILS` (`:313-349`).
19. Cash-mode rule: paymentModeId > 1 needs bankId/number; else 0/null (`:367-374`); payment modes capped at remaining amount (`:376-384`); MIS entry `helper.addUserWiseCollection` (`:390-403`).
20. Bill settled ⇔ self & non-self balances ≤ 0 → `CIMS_UPDATEBILLFORCONCESSION` IsSettled=true (`:438-463`); OPD: T_Encounter isCheckout=true + Solr (`:440-447`); `T_Appointment VisitStatus=11` (`:477-478`); `invstopdpostbilling` → item-sale payment + changeAmount on T_Bill (`:480-506`).
21. Refund at settlement: `addRefundServiceImpl.addRefund` (transactionType=3 advance refund; comment: 4 = bill refund) (`:465-472`; `custom/AddRefundServiceImpl.java:89`).
22. Payment refund (bill): `AddRefundServiceImpl.addRefund` — advance balance must be >0 → `BEXAR002`; refund modes posted (`:134-155`).

Cancellation:
23. `BillCancellationServiceImpl` (settled bill): restores T_Advance (`Used=Used−?, Balance=Balance+?`), reverts charge amounts to `*BeforeFinalization` + `ISCANCELLED=true`, `T_Itemsale BillID=0, IsBilled=false`, doctor-share details status=false, refund payment transactionType=4 (`:231-295`, `:347`, `:470-509`).

### 1.6 PHARMACY (prescription → dispensing → stock)
1. Prescription details flags: `isverified`, `isdispensed`, `ispartialdispense`, `pendingqty`, `flagged/flagreason`, `isacknowledge` (`custom/PharmacyServiceImpl.java:536,574,624,735-746,768`).
2. `medication/dispenseItemSale` (OP): per-detail `DISPENSED=true`; when no undispensed details remain → T_ITEMSALE DISPENSED=true; when no undispensed item sales remain for the visit → `T_Appointment VisitStatus=12` (`custom/MedicationServiceImpl.java:799-821`).
3. `CIMS_ADDITEMSALESDETAILS_NEW` (proc): item-sale detail with BatchId; closes prescription: `T_PrescriptionDetails IsClosed='1', PendingQty=0` (or `PendingQty = PendingQty − Quantity`); `T_PrescriptionMaster IsClosed='1'`; compound details closed (`generated-procs/CIMS_ADDITEMSALESDETAILS_NEW.sql:103-121`).
4. Batch availability/expiry computed in DB proc `cims_getitemanditembatchlistbyvisitid_new3` (`DTOQueries_4.java:762`); batch expiry surfaced as `ibm.expirydate` (`PharmacyServiceImpl.java:2622`).
5. `medication.amount.show` (property false) → session flag in UserView (`util/auth/DefaultAuthenticationImpl.java:140`).
6. Pharmacy bill: `CIMS_RPT_PHARMACYBILL_NEW`; GST display via `getGstApplicable` (`MedicationServiceImpl.java:1080`).

### 1.7 PATHOLOGY (order → sample → result)
1. Order auto-created at bill time (see 1.5 step 9) — patho booking rows `status=true`, `istemp` mirror charge temp-ness (`AddServiceOrderServiceHelper.java:421-519`).
2. Sample collection: `T_pathorderbookingdetails.isaccepted=true` gates removal (`getSampleStatus` `AddServiceOrderHelper.java:3693-3719`).
3. Charge removal: patho/rad with `isaccepted=true` (sample collected) and NOT `autoChargeCancellationOnRefund` → `LEX001`; otherwise `T_PathorderBooking.ISCANCELLED=true` (`:3751-3775`); procedure charges → `T_EMR_Services status=false` + procedure/schedule status=false (`:3781-3812`); nursing → `T_PATIENTNURSINGORDERS orderstatus = TaskConstants.DISCONTINUE` (`:3819`); charge row `ISCANCELLED=true` (`:3862`).
4. Result entry: `PathoOrderServiceImpl` (`/pathoOrder/getNormalResult`, `getTemplateResult`, `updatePathoResultEntryTemplate`, `addResultReviewHoldRecord/Notes`) (`:380-1092`).

### 1.8 OT (schedule → procedure → billing)
1. `otscheduler/addPatientProcdure` (`pas/OTSchedulerServiceImpl.java:596-800`): patient must have active admission or visit → `BEOT004` (`:616-631`); schedule via `CIMS_ADDUPDATEPATIENTPROCEDURESCHEDULEMASTER`; `CIMS_ADDUPDATEOTDETAILS`; per procedure `CIMS_ADDUPDATEPATIENTPROCDURE` + `PatientProcedureRateDetails` (rate from `getServicePriceDetails`, 0 if none); dependent-surgery discount% (`:731-736`, `:759-768`); doctor rows `CIMS_ADDUPDATEOTDETAILSDOCDETAILS`.
2. Pre-billing: `otprocedure.pre.billing=true` → procedure charge `consumed=true` → `CIMS_ADDCHARGE` direct (not temp) (`AddServiceOrderServiceImpl.java:2202-2204`); doctor-share skipped for OT tariff services (`AddServiceOrderServiceHelper.java:383-386`); ordered-procedure list query filters `isOtProcessDone=false`, `sm.otprocedure=true` (`OTSchedulerServiceImpl.java:4069-4077`).
3. `T_EMR_Services` is the ordering record; `tempchargeid` links to charge (`OTSchedulerServiceImpl.java:4069`).

### 1.9 BLOOD BANK (requisition → cross-match → issue)
1. Requisition statuses (`pas/BloodBankServiceImpl.java:6290-6317`): `1` = NEW/cross-match pending; after cross-match `requisitionStatus = (totalCrossMatchedVolume ≥ quantity×volume) ? 2 : 1`; after issue `= 3` when `bagsCumulativeVolume ≥ totalOrderedVolume`.
2. Cancel requisition: `REQ_CANCELLATION=10` (`constants/BloodBankConstants.java:31-33`).
3. Cross-match: `saveBloodCrossMatchingTestResult` — `CIMS_ADDPAYMENT`…no, `BloodCrossmatchingResult` save, `CrossMatchingTestNo` from autocode `t_bloodcrossmatchingresult` (`:2615-2714`); notification SMS; cross-matching count incremented (`:2700-2703`).
4. Issue: `issueBloodBag` marks `BloodCrossMatchResultDetails.bagIssued=true` (`:3593-3595`).
5. Schedules (`jobs/ApplicationJobs.java`):
   - Auto-decross `${bloodbag.auto.decross}` (0 0 * ? * *) — crossmatched, not decrossed, `minutesDiff ≥ 2880` (AUTO_DECROSS_PERIED_MINUTES) → decross, bag `crosSMatched=false`, requisition back to 1, `BloodBagDecross` with reason 0 (`:1045-1061`; `BloodBankServiceImpl.java:7657-7701`).
   - Bag expiry daily 23:00 — bags expiring today → `isbagexpired=true`; if crossmatched & not issued → decross + req status 1 (`:769-815`).
   - Requisition expiry `${bloodorder.cancellation.schedule}` — `requesteddate + orderValidationPeriod h ≤ sysdate`, sample not collected, status=1 → details status 10, `isorderexpired=true`, reason "Order is expired" (`:734-766`).
   - Donor cancellation `${donor.cancellation.schedule}` — DNR_NEW(1)/DNR_FIT(2) older than 72h → DNR_CANCELLATION(7) (`:832+`).
   - Donor statuses: NEW=1, FIT=2, UNFIT=3, ACCEPTED=4, REJECTED=5, DONATED=5(!), CANCELLATION=7, PERM_UNFIT=10, TEMP_UNFIT=11 (`constants/BloodBankConstants.java:8-17`).
6. Blood-bag extraction validity: hours < `bloodBagExtractionValidityPeriod` (8) gate at `:2255`.

### 1.10 STATUS / STATE MACHINES (evidence)
- **T_Encounter / T_Appointment VisitStatus** (Java sets, ascending only):
  - `3` = doctor called patient (`updateCallPatientData`, `EmergencyAndWalkinVisitServiceImpl.java:2017,2363`)
  - `4` = patient-assessment questionnaire saved (`QuetionnaireServiceImpl.java:107`)
  - `6` = questionnaire (non-assessment) saved (`QuetionnaireServiceImpl.java:109`)
  - `9` = doctor checkout (`updateDoctorCheckout`, `EmergencyAndWalkinVisitServiceImpl.java:2270`); also compound-medication pre-visit prescription (`:2351`)
  - `10` = cabin released (`releaseCabin`, `:2146`)
  - `11` = billed/paid (checkout) (`AddPaymentServiceHelper.java:477,820`)
  - `12` = pharmacy fully dispensed (`MedicationServiceImpl.java:812`)
  - Initial value set by UI/DB default; `visitstatus2` separate string col: `"2"` default, `"9"` compound-med visit (`EmergencyAndWalkinVisitServiceImpl.java:1039,1141`).
- **T_Bill**: `Status=1` active (`AddServiceOrderServiceHelper.java:224`); `IsSettled` false→true on full payment (`AddPaymentServiceHelper.java:438-463`); `IsCancelled` (proc default 0; `IsCancelled=true` on removeCharge path uses T_Charges); `cancelbill` set on bill cancellation (`BillCancellationServiceImpl.java:455-467`); `InterORFinal` (interim flag) (`AddServiceOrderServiceHelper.java:338`).
- **T_Charges**: `IsBilled`, `IsCancelled`, `IsTemp`(T_CHARGES_TEMP), `Consumed`; restore-on-cancel uses `*BeforeFinalization` columns (`BillCancellationServiceImpl.java:488-499`).
- **T_AdmissionDetails**: `ISDischarged` 0→1, `IsClosed` '0'→'1' (`generated-procs/CIMS_UPDATEIPDISDISCHARGE.sql`); `ISCANCEL` for abscond (`ApplicationJobs.java:1015`); interim admission `BEDID=0`.
- **T_Discharge**: `IsDeath` set when discharge type IsExpired=1 (`PatientTransferDischargeServiceImpl.java:752-758`); `T_Registration_Details.isdeath=1` (proc).
- **M_BedMaster**: `OCCUPIED` 0/1; `BedStatusId` 6 = released (set on discharge/transfer `PatientTransferDischargeServiceImpl.java:790,3811,4162`); `0` = reset/available (`:1074,1096,3831`); `5` = retained+occupied (query predicate `:1859`); `1,3` = free-bed predicates (`:1864,2763`). Labels live in `M_BedStatusMaster.isrelease` (master data; consumed at `PatientRegAdmissionServiceImpl.java:1293-1300`).
- **T_Payment**: `transactionType` 2=payment, 3=advance refund, 4=bill refund (`AddPaymentServiceHelper.java:270`; `AddRefundServiceImpl.java:89`; `BillCancellationServiceImpl.java:347`).
- **Blood requisition**: 1=new/partial crossmatch, 2=fully crossmatched, 3=fully issued, 10=cancelled (`BloodBankServiceImpl.java:6302-6312`; `ApplicationJobs.java:743`).
- **Prescription**: `IsClosed` 0/1, `IsDispensed`, `IsVerified`, `IsPartialDispense`, `PendingQty` (`PharmacyServiceImpl.java`, `CIMS_ADDITEMSALESDETAILS_NEW.sql`).
- **Authorization (payor)**: enum ACCEPTED=0..QUERIED=8 (`GenericConstants.java:47-60`).
- **Medication order**: ACTIVE=1, DISCONTINUE=2, EXPIRED=3; patient med: ORDERED=1, TAKEN=2, MISSED=3, SKIPPED=4 (`GenericConstants.java:167-188`; not referenced in services — consumed by integrations/EHR side).
- **Donor**: DNR_NEW=1, DNR_FIT=2, DNR_UNFIT=3, DNR_ACCEPTED=4, DNR_REJECTED=5, DNR_DONATED=5 (collision!), DNR_CANCELLATION=7, DNR_PERM_UNFIT=10, DNR_TEMP_UNFIT=11 (`BloodBankConstants.java`).
- **GST applicability**: `isGSTApplicable=true` (property) AND transDate ≥ 2017-07-01 (`BaseServiceImpl.java:521-533`).
- **NHCX**: request types 0-4; statuses ACCEPTED=0..FAILED=3 (`GenericConstants.java`).

---

## 2. KEY CONFIG (application.properties)
```
mrnoFromSeries=false; autoGenerateTempMrnNo=false; medication.amount.show=false;
otprocedure.pre.billing=true; isGSTApplicable=true; discountBeforeTax=true;
opd.no.template=V/%s/%d/%s; emer.no.template=EV/%s/%d/%s; ipd.no.template=A/%s/%d/%s;
op.bill.no.template=%s/I/%d/%s; ip.bill.no.template=%s/I/%d/%s; receipts.no.template=%s/R/%d/%s; creditnote.no.template=%s/C/%d/%s;
bloodBagExtractionValidityPeriod=8; bloodbag.expiry.schedule=0 1 * * * *; bloodbag.auto.decross=0 0 * ? * *;
solr.patient.collection.name=patient_risl_qa; solr.encounter.collection.name=encounter_risl_qa
```

## 3. RISK NOTES (raw)
R1. VisitStatus 11 is on **T_Appointment** in AddPaymentServiceHelper (`:477`) but statuses 3/9/10 write **T_Encounter** — two tables, same codes, different rows; appointment row's VisitStatus used for kiosk/queue.
R2. `getConcessionDetails` non-self branch overwrites `concObj[1]` instead of adding (line 2822) vs self branch adds (line 2815) — inconsistent multi-tier concession.
R3. DNR_REJECTED == DNR_DONATED == 5L collision in BloodBankConstants.
R4. `CIMS_ADDADMISSIONINFORMATION` resultStatus: Java throws APP015 on -1 but proc resets to 0 after insert — check effectively dead.
R5. BedStatusId=6 hard-coded as "released" in Java SQL while master table defines semantics — drift risk if master changed.
R6. dischargeOnZeroBill path rebuilds full bill with serviceOrderDto.final=true + all charges — any charge without before-finalization snapshot breaks cancellation restore.
R7. `t_patientrequisitiondetails.requisitionStatus=10` set by job, but requisition *header* status 10 = REQ_CANCELLATION constant only used at creation/cancel; job updates details, not header (asymmetry).
R8. `releaseCabin` sets VisitStatus=10 only if NO temp charges remain (`releaseCabin` gating).
R9. Hard-coded tariff `2L` fallback when config.nonresidentialtariffId null (OPD + IPD) (`EmergencyAndWalkinVisitServiceImpl.java:415`, `PatientRegAdmissionServiceImpl.java:371`).
