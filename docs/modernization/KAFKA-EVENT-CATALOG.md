# iHMS / OHUM PAS NG — Kafka Event Catalog (PRELIMINARY)

**Status: PRELIMINARY — synthesized from discovery (no re-exploration).**
**Sources:** `10-target-architecture.md` §7.1, `_discovery/agent-10/findings.md` §2/§3/§4, `07-integrations-and-search.md`, `06-business-rules-catalog.md`, `05-database-catalog.md`.
**Selection rule:** a candidate becomes an **accepted event only when legacy evidence shows ≥2 independent consumers/side-effects**, or it **replaces a proven DB-poll async pipeline**. Everything else stays synchronous REST until a saga/outbox exists.

---

## 1. Selection method & evidence summary

| Evidence class | Count | Examples (file:line) |
|---|---|---|
| In-transaction side-effect chains (same Spring tx) | 5 chains | `EmergencyAndWalkinVisitServiceImpl.java:1379-1937` (encounter→queue→MLC→FPV→HL7 A45→corona→diagnosis→Solr→ABDM queue); `HelperServiceImpl.java:221,252,307` (HL7); `AddServiceOrderServiceHelper.java:341-516` (mega-txn) |
| Existing DB-poll async pipelines | 3 | `T_WHATSAPPMESSAGEQUEUE` → `ApplicationJobs.java:1611`; `T_ABDMREQUESTQUEUE` → `:1661-1688`; `T_OUTBOUNDHIS` (outbox w/ RetryCount) → EHRSyncJobs |
| Solr index writers (prospective event consumers) | 15+ call sites | `PatientServiceImpl.java:5610`, `SolrUtil.java:174-333`, `EmergencyAndWalkinVisitServiceImpl.java:1796-1931`, `ApplicationJobs.java:867-977,2122` |
| Async integrations already queue-backed | 2 | WhatsApp (`07 §3.1 #15`), ABDM (`#1`) |
| Scheduled jobs that replaceable by events | 30+ | `ApplicationJobs.java` (auto-charges :283, WhatsApp :1611, ABDM :1661-1688, Solr backfill :2122, eRaktkosh :1205,1383, EHR files :1096, ASHA incentives :1035) |

---

## 2. ACCEPTED Event Candidates

### E-01 PatientRegistered
| Field | Value |
|---|---|
| Producer | Patient service — `PatientServiceImpl` save (`PatientServiceImpl.java:5610` Solr index), `PatientDetailRegistrationServiceImpl` (save :1989), `PatientServiceHelper:373-425` (MRN gen) |
| Consumers (evidence) | **Search** (Solr patient index `PatientServiceImpl.java:5610`, `SolrUtil.java:174-188`); **Interop/HL7** (registration msg `HelperServiceImpl.java:221`, `BedReservationServiceImpl.java:180`); **ABDM** (consent flow `EmergencyAndWalkinVisitServiceImpl.java:652` T_PATIENT_ABDM token check; queue entry `:1937`); **Notifications** (WhatsApp consent `PatientDetailRegistrationServiceImpl.java:892`) |
| Payload | Patient id, MRN, name, DOB, gender, mobile, identity numbers (civilid/passport/dlic/ration/janaadhar/pcts/ects), ABHA healthId/healthIdNumber, blood group, unitId, sponsor, address (mirrors `PatientSolrView` ~100 fields — `07 §1.2`) |
| Schema | JSON v1 (`patient-registered.v1`) — FHIR R4 `Patient` for ABDM-facing consumers is an adapter in Interop, not the topic schema |
| Partition key | `patientId` (order per patient) |
| Ordering | Required: registration precedes any encounter/admission event for the same patient |
| Retry / DLQ | Retry ×5 (mirrors WhatsApp max trigger count 5); DLQ `dlt.patient-registered` |
| Idempotency | Consumer-side: Solr doc id = patientId (`id` in doc); HL7/ABDM senders dedupe on eventId + existing control ID; single-session constraints apply at patient level |
| Version | v1 |
| Justification | **4+ independent consumers today, executed synchronously in the DB tx** — the strongest event case; removes Solr/HL7/ABDM latency from registration (`07 §1.6` risk 3) |

### E-02 EncounterCreated
| Field | Value |
|---|---|
| Producer | OPD/Encounter — `EmergencyAndWalkinVisitServiceImpl.java:1379` (encounter save), `AddUpdateAppointmentServiceImpl.java:70` |
| Consumers (evidence) | **Search** (Solr encounter `:1796-1931`, `SolrUtil.java:245-333`); **Interop/HL7** (A45 visit msg `HelperServiceImpl.java:252`, `:1614`); **Queue/Kiosk** (queue update `:1411`); **ADT** (admission precondition); **ABDM** (queue `:1937`) |
| Payload | encounterId, patientId, unitId, doctorId, deptId, visitMark, appointmentType, opdNo/token, visitstatus2, clinicId |
| Schema | JSON v1 `encounter-created.v1` |
| Partition key | `patientId` (all encounters of a patient ordered) |
| Ordering | Per patient: after PatientRegistered; encounter 1..n ordered |
| Retry / DLQ | Retry ×5; DLQ |
| Idempotency | Solr doc id = `<encId>_E`; queue consumers keyed on encounterId |
| Version | v1 |
| Justification | 4+ side effects in one method (`agent-10 §2.3`); Solr encounter doc is the unit-scoped search key (`07 §1.4`) |

### E-03 PatientAdmitted
| Field | Value |
|---|---|
| Producer | ADT — `IPDAdmissionServiceImpl.java:130` (CIMS_ADDADMMLCDETAILS), `:313` (CIMS_UPDATEBEDOCCUPIEDFLAG_1), `PatientRegAdmissionServiceImpl` (admission) |
| Consumers (evidence) | **Interop/HL7** (A01 `HelperServiceImpl.java:307`); **Search** (Solr `indexAmissionInSolr` `SolrUtil.java:245-333`); **Billing** (auto bed charges via `CIMS_JOBSCHEDULING` job `ApplicationJobs.java:283`); **ABDM** (`addAbhaRecordsNode` `:1661`); **Clinical/Nursing/BloodBank/Diet** (admission-relative flows) |
| Payload | admissionId, patientId, encounterId, bedId, wardId, unitId, admissionType, ipdNo, doctorId, admittedAt |
| Schema | JSON v1 `patient-admitted.v1` |
| Partition key | `patientId` |
| Ordering | After EncounterCreated; before PatientDischarged |
| Retry / DLQ | Retry ×5; DLQ |
| Idempotency | Solr doc id = `<admId>_A`; bed-charge consumers keyed on (admissionId, bedId) to avoid double-charge |
| Version | v1 |
| Justification | HL7+Solr+bed state+ABDM in one tx (`agent-10 §2.4`); **bed auto-charging is currently a scheduled poll** — event makes it real-time |

### E-04 PatientDischarged
| Field | Value |
|---|---|
| Producer | ADT — `PatientTransferDischargeServiceImpl.java:720-831` (CIMS_ADDBEDDISCHARGE, CIMS_UPDATEIPDISDISCHARGE; death handling :789) |
| Consumers (evidence) | **Billing** (final settlement, `T_PATIENTDISCHARGEDATA`); **EHR** (discharge files `ApplicationJobs.java:1096` creteEHRFiles); **Search** (Solr encounter clear unless death, `PatientTransferDischargeServiceImpl.java:698-3282`); **Grievance** (satisfaction trigger); **Reporting** (discharge summaries) |
| Payload | dischargeId, patientId, admissionId, encounterId, dischargeType, isExpired/isDeath, date, balanceDue |
| Schema | JSON v1 `patient-discharged.v1` |
| Partition key | `patientId` |
| Ordering | After PatientAdmitted |
| Retry / DLQ | Retry ×5; DLQ |
| Idempotency | death+discharge idempotent by dischargeId; Solr clear idempotent by doc id |
| Version | v1 |
| Justification | 4+ consumers incl. a currently **scheduled** EHR file job → becomes event-driven |

### E-05 LabResultFinalized
| Field | Value |
|---|---|
| Producer | Lab — `PathoOrderServiceImpl.java:2851-3183`, `PathoOrderServiceHelper.java:1130-1208` (sendCategoryWiseReportOnWhatsApp) |
| Consumers (evidence) | **Interop/Notifications** (WhatsApp report — **replaces T_WHATSAPPMESSAGEQUEUE poller** `ApplicationJobs.java:1611`); **EHR** (result persistence); **Search** (patient doc update); **Blood Bank** (serology for requisition) |
| Payload | bookingId, detailId(s), patientId, encounterId, test results (parameters), status, finalizedAt |
| Schema | JSON v1 `lab-result-finalized.v1` |
| Partition key | `encounterId` (all results of a visit ordered) |
| Ordering | Per encounter: results finalized in order; per-parameter finalization order must be preserved |
| Retry / DLQ | Retry ×5 (matches legacy max trigger 5); DLQ |
| Idempotency | WhatsApp consumer dedupes on queue-entry id (legacy `whatsappMsgQueId` ref in `savePdfToTable` `CommonUtilServiceImpl.java:6067-6082`); PDF generation idempotent per result |
| Version | v1 |
| Justification | **Direct replacement of a proven DB-poll pipeline** — strongest "migrate poller→event" case |

### E-06 PrescriptionCreated
| Field | Value |
|---|---|
| Producer | Clinical/Pharmacy — `PrescriptionMasterServiceImpl`, `MedicationServiceImpl.java:799-821` |
| Consumers (evidence) | **Pharmacy** (ward dispense list `PharmacyServiceImpl.java:482-550`); **Billing** (medication charges); **Nursing** (drug administration) |
| Payload | prescriptionId, patientId, encounterId/admissionId, item lines (itemId, qty, dose, frequency), prescriberId, wardId |
| Schema | JSON v1 `prescription-created.v1` |
| Partition key | `encounterId` |
| Ordering | Per encounter |
| Retry / DLQ | Retry ×3; DLQ |
| Idempotency | line-level upsert on (prescriptionId, lineNo) |
| Version | v1 |
| Justification | 3 consumers; **Pharmacy's dispense list is currently a DB query against the same tables** — event removes cross-service table coupling |

### E-07 InvoiceGenerated
| Field | Value |
|---|---|
| Producer | Billing — `AddServiceOrderServiceHelper.java:341` (CIMS_ADDBILL), `:370` (CIMS_ADDBILLDETAILS_1), `:417` (CIMS_ADDDOCTORSHARE), `:2526` (package subscription), `:2351/:2367` (EMR), `:2299-2399` (HL7 ORM) |
| Consumers (evidence) | **Payments** (settlement); **DoctorShare** accrual (`:417`); **RCM** (claims stage); **Interop** (HL7 ORM `:2299`, WhatsApp bill PDF); **EMR** (`:2351`); **Search** (bill search) |
| Payload | billId, billNo, patientId, encounterId/admissionId, header (total/tax/concession/self/nonSelf), lines, charges, doctorShare, isInterimOrFinal |
| Schema | JSON v1 `invoice-generated.v1` |
| Partition key | `billId` |
| Ordering | Per bill: charge add → bill create → bill finalize must be ordered |
| Retry / DLQ | Retry ×5; DLQ |
| Idempotency | payment consumers: billId + eventId; WhatsApp PDF: billId |
| Version | v1 |
| Justification | **Internal multi-consumer** (6+), though today it is one atomic tx — **phased**: internal consumers first via outbox, external integration consumers after saga (see §4 caveat) |

### E-08 PaymentReceived
| Field | Value |
|---|---|
| Producer | Payments — `AddPaymentServiceHelper.java:291` (CIMS_ADDPAYMENT), `:309` (CIMS_ADDPAYMENTBILLDETAILS), `:333` (CIMS_ADDPAYMENTMODEDETAILS), `:348` (CIMS_ADDPAYMENTADVANCEDETAILS), `:463` (CIMS_UPDATEBILLFORCONCESSION) |
| Consumers (evidence) | **Billing** (settlement state update `:463`); **RMRS/treasury** (`RMRSServiceImpl.java:1361-1366` reads collection); **RCM** (claim stage); **Notifications** (receipt) |
| Payload | paymentId, billId, patientId, amount, mode(s), advance details, transactionType (2/3/4), changeAmount |
| Schema | JSON v1 `payment-received.v1` |
| Partition key | `billId` |
| Ordering | Per bill: payment events ordered; settlement depends on last payment |
| Retry / DLQ | Retry ×5; DLQ |
| Idempotency | CIMS_UPDATEBILLFORCONCESSION must be idempotent (settled ⇔ balances ≤ 0, `BR-BILL-006`) — replay-safe by recomputation |
| Version | v1 |
| Justification | 4+ consumers incl. treasury (scheduled ROWNUM=1 reads today) |

### E-09 InventoryUpdated
| Field | Value |
|---|---|
| Producer | Inventory (and Pharmacy stock ops via API) — `CIMS_ADDITEMSTOCK.sql` (+`_UPDATE`), `InventoryServiceImpl`, `PharmacyServiceImpl.java:2140` |
| Consumers (evidence) | **Procurement** (reorder/auto-indent jobs `ApplicationJobs.java:2197,2376`); **Search** (Solr item index `SolrUtil.java:990`, `syncItemsToSolrJob` 5s drainer `07 §1.2`); **EAushadhi** (stock sync `ApplicationJobs.java:2961-2967`); **Pharmacy/OT/CSSD** (stock availability) |
| Payload | itemId, storeId, batchId, delta (+/-), reason (GRN/issue/consumption), availableStock, updatedAt |
| Schema | JSON v1 `inventory-updated.v1` |
| Partition key | `itemId` (or `storeId` if per-store ordering needed — decision) |
| Ordering | **Required per (item, store, batch)** — stock decrements must not reorder |
| Retry / DLQ | Retry ×5; DLQ; **reconciliation job needed** (stock is money) |
| Idempotency | delta application must be idempotent — carry `stockEntryId` (source tx id); stock corrections keyed on entry id |
| Version | v1 |
| Justification | 4+ consumers; item Solr is currently a **5s flag drainer** (`SOLR_STATUS=0`) → becomes event-driven |

### E-10 MasterDataChanged (incl. TariffChanged, PackageChanged)
| Field | Value |
|---|---|
| Producer | Reference Master — `MasterCachedServiceImpl`, `AddServiceMasterImpl.java:207-244` (CIMS_ADDSERVICEMASTER2/CIMS_ADDTARIFFSERVICE2), CRUD through `CrudOperationsFilter.java:139-330` (~375 `@CrudCached` masters) |
| Consumers (evidence) | **All services** (Redis cache invalidation — cache keys owned by Reference, `10 §2.2`); **Search** (item/icd10cm/healthfactor Solr drainers `ApplicationJobs.java:867,884,976`); **Billing** (tariff lookup `BillServiceImpl.java:104`) |
| Payload | masterType, masterId, op (create/update/delete), version |
| Schema | JSON v1 `master-data-changed.v1` (thin envelope; data fetched via API) |
| Partition key | `masterType` |
| Ordering | Not required across masters; required per master id (update-after-update) |
| Retry / DLQ | Retry ×3; DLQ |
| Idempotency | per-master version stamps |
| Version | v1 |
| Justification | Replaces per-node cache write-through + 5s Solr drainers with one authoritative invalidation stream (`07 §2.3`) |

### E-11 BedChanged (PRELIMINARY)
| Field | Value |
|---|---|
| Producer | ADT — `CIMS_UPDATEBEDOCCUPIEDFLAG_1` (`IPDAdmissionServiceImpl.java:313`), transfer/discharge (`PatientTransferDischargeServiceImpl.java:789-790`) |
| Consumers (evidence) | **Billing** (bed auto-charges — currently scheduled `CIMS_JOBSCHEDULING` `ApplicationJobs.java:283`); **Search** (Solr encounter fields) |
| Payload | bedId, admissionId, oldStatus, newStatus (BedStatusId 0/1/3/5/6, OCCUPIED), changedAt |
| Schema | JSON v1 `bed-changed.v1` |
| Partition key | `bedId` |
| Ordering | Required per bed (occupancy CAS semantics, `BR-IPD-002`) |
| Retry / DLQ | Retry ×3; DLQ |
| Idempotency | keyed on (bedId, changedAt) |
| Version | v1 |
| Justification | **Weakest of the accepted set** — auto-charge today is a job, not a listener; event makes it real-time. Keep job as fallback during strangler |

### E-12 RefundIssued (PRELIMINARY)
| Field | Value |
|---|---|
| Producer | Payments — `AddRefundServiceImpl.java:82-155` (transactionType 3/4), `BillCancellationServiceImpl.java:231-295,347` |
| Consumers (evidence) | **Billing** (charge restore, `*BeforeFinalization` snapshot logic `BillCancellationServiceImpl.java:488-509`); **Notifications** (refund receipt); **Reporting** |
| Payload | refundId, billId, advanceId, patientId, amount, reason, restoredCharges[] |
| Schema | JSON v1 `refund-issued.v1` |
| Partition key | `billId` |
| Ordering | After InvoiceGenerated/PaymentReceived for the bill |
| Retry / DLQ | Retry ×5; DLQ |
| Idempotency | refundId; cancellation restore is the compensating action — must be replay-safe |
| Version | v1 |
| Justification | Multi-consumer, but billing restore is today same-tx; acceptable after InvoiceGenerated/PaymentReceived land |

### E-13 OrderCreated (ADDED WP-S1 — see §3 resolution)
| Field | Value |
|---|---|
| Producer | Ordering — `services/ordering-service` `OrderService.create()` (clean target of the `AddServiceOrderServiceHelper.java:341-516` mega-txn fragment), written to the transactional outbox in the SAME tx as t_order/t_order_line |
| Consumers (evidence) | **Billing** (bill DRAFT + one charge per line — the saga consumer side; internal ingress contract-stub for the future Kafka listener); post-saga: Lab/Rad order auto-create, Nursing |
| Payload | orderId, patientId, encounterId\|null, admissionId\|null, lines[{itemId, serviceName, rate NUMERIC(19,2), qty int}], prescriberId, wardId\|null — unsupplied context slots travel as explicit null |
| Schema | JSON v1 `order-created.v1` |
| Partition key | `encounterId`; documented fallbacks `<admissionId>` when encounterId is NULL (E-06 pattern), `no-context:<orderId>` when both are NULL (column NOT NULL) |
| Topic | ihms.ordering.events |
| Ordering | Per partition key |
| Retry / DLQ | Retry ×5; DLQ topic + DLQ-status outbox row (replay job) |
| Idempotency | producer idempotencyKey `order-created:<orderId>`; consumer keyed on the envelope eventId (t_consumed_event UNIQUE + partial UNIQUE on t_bill.saga_event_id); compensation on bill-draft rejection: ordering PLACED -> BILLING_FAILED(4) with persisted reason |
| Version | v1 |
| Justification | RESOLVES the §3 mega-txn gating item for the ordering->billing hop (choreographed saga slice, WP-S1); EMR/Lab-Rad hops remain deferred |

---

## 3. REJECTED / DEFERRED Event Candidates (with rationale)

| Candidate | Legacy evidence | Verdict / rationale |
|---|---|---|
| **OrderCreated → Bill → LabOrder → RadOrder → DoctorShare → EMR → HL7 (the "mega-txn")** | `AddServiceOrderServiceHelper.java:341-516, 722-737, 908-991`; `AddServiceOrderServiceImpl.java:2206-2399` — ONE `@Transactional` | **REJECTED for now. Must remain synchronous REST.** Splitting an ACID unit into events without a designed saga introduces partial-bill/lost-charge states. Prerequisite: saga choreography + outbox (bill cancellation compensation already exists: `BillServiceImpl.java:70-91`). **This is the gating item for all revenue events (E-07/E-08)** — **RESOLVED (WP-S1): the ordering→billing hop of this ACID unit is now choreographed saga + outbox exactly as this row demanded:** E-13 OrderCreated (`order-created.v1`, see §2) is written atomically with the order rows via the ihms_clinical.outbox; billing consumes it into a DRAFT bill idempotent per envelope eventId; rejection compensation flips the order PLACED → BILLING_FAILED(4) with the bill-side reason persisted (no silent loss, no retry storm). The remaining hops (LabOrder/RadOrder/DoctorShare/EMR/HL7) stay DEFERRED as below. Original verdict text preserved above unchanged. |
| LabOrderCreated / RadOrderCreated | created *inside* the mega-txn (`:473-516`, `:579-599`) | **DEFERRED** — same-tx with billing today; become events only after saga split (`10 §7.1` verdict) |
| ChargeAdded | `CIMS_ADDCHARGE` (:265), temp-charge split (`AddServiceOrderServiceImpl.java:2233-2241`) | **REJECTED** — single-consumer state transition inside mega-txn; patho/rad order auto-creation (`BR-BILL-003`) depends on it synchronously |
| AppointmentBooked | e-Mitra kiosk redirect (sync, `07 #4`), queue room, WhatsApp reminders (weak evidence) | **REJECTED (phase 1)** — one external side-effect, synchronous today; revisit when queue/token flows split. E-02 EncounterCreated covers the multi-consumer case |
| Solr `addBean` calls as events | `SolrUtil.java:174-333` etc. | **REJECTED as topics** — they become **consumers** of E-01..E-09, not events (Search owns the pipeline, `10 §4` row) |
| HL7 outbound messages (A01/A45/ORM) | created synchronously in tx (`HL7Helper`); **no active outbound sender found** (`07 §3.2`) | **REJECTED as topics** — Interop consumes domain events and produces HL7; keep outbox table in PG with idempotent senders (`10 §7.2`) |
| BloodRequisitionCreated | eRaktkosh scheduled push (`ApplicationJobs.java:1205,1383`) | **DEFERRED** — single external consumer today (scheduled); promote when eRaktkosh push is event-triggered |
| ClaimSubmitted (RCM→NHCX) | `NHCXServiceImpl.java:30`, JWE via ABDM gateway (`07 #2`) | **DEFERRED** — external-only consumer; needs outbox for guaranteed delivery; joins after revenue events |
| IncentiveComputed (EJS) | `ApplicationJobs.java:1035` (asha.incentive.schedule) | **DEFERRED** — batch job, single finance consumer; low urgency |
| KitIssued / SterilizationCompleted / TransportRequested / EquipmentComplaintRaised / BMWRecorded / GrievanceRaised | single-domain flows, no cross-service consumer evidence | **REJECTED (phase 1)** — domain-internal; keep REST until a second consumer appears |
| ReportGenerated | on-demand reports + scheduled jobs (`10 §4` row) | **REJECTED** — request-response (REST) + scheduled ELT into read-model tables; not a domain event |

---

## 4. Outbox Pattern Recommendation

1. **Transactional outbox per PG schema**: each producer service writes domain rows + `outbox` rows in the **same PG transaction** (guaranteed atomicity — solves the current "Solr/HL7 in Spring tx" inconsistency, `agent-10 §2`).
2. **Relay**: `pg_logical`/Debezium-class CDC on the outbox table → Kafka topics (`10 §5`). Kafka ≥2 consumers per topic; DLQ topics `dlt.*`; retention policy per event class (revenue events 90 days, master events 7 days — `[UNKNOWN]` actual compliance requirement, see §6 Q6).
3. **Event envelope (all events)**: `{eventId (UUID), eventType, version, occurredAt, producerService, entityId, partitionKey, payload, idempotencyKey}`. Schema registry (JSON Schema or Avro) — Avro recommended for revenue events.
4. **Exactly-once vs at-least-once**: Kafka offers at-least-once; **all consumers must be idempotent** (see Idempotency column per event). Legacy already operates at-least-once (`SOLR_STATUS=0` flag drainers, `SYNCHRONIZED` queue flags) — no regression.
5. **Saga scaffolding** for revenue flows (`10 §5`): choreographed Order→Bill→Payment with compensation (bill cancellation procs `CIMS_SETTLEDBILLCANCELLATION/CIMS_UNSETTLEDBILLCANCELLATION` map to compensation handlers).
6. **Ordering enforcement**: partition keys defined per event (§2); for cross-event ordering (PatientRegistered → EncounterCreated → PatientAdmitted), a single `patientId`-keyed topic stream is recommended during strangler to preserve the legacy single-DB ordering guarantee.

---

## 5. Kafka vs Existing Async Queues (poller→event migration)

### 5.1 The three proven poller pipelines

| Legacy pipeline | Poll cadence / mechanics (evidence) | Consumers today | Kafka replacement |
|---|---|---|---|
| `T_WHATSAPPMESSAGEQUEUE/STATUS` | 15-min poll `triggerWhatsappReportsInQueue` (`ApplicationJobs.java:1611`), window 14:00–07:00, **max trigger count 5**, PDF gen in `PathoOrderServiceHelper.java:1130-1208`, status callback `IntegrationServiceImpl.java:1462` | WhatsApp reports (lab), bill PDFs | Topic `lab-result-finalized` + `invoice-generated`; Interop consumer generates PDF + calls WhatsApp API; max-trigger 5 → retry policy + DLQ; window constraint (14:00–07:00) becomes a **configurable quiet-hours filter in the consumer**, not a scheduler |
| `T_ABDMREQUESTQUEUE` | 5s poll (`ApplicationJobs.java:1661-1688`), **node-sharded** (8 servers, per-node Redis counters `<nodename>_ABDM_S/F` `:2033-2034`), Redis correlation store `ABDM_REQ_/ABDM_RES_` | ABDM consent, patient-record push | Topic `patient-registered`/`patient-admitted` + domain events; **node sharding → Kafka consumer groups** (partitions replace server sharding); Redis correlation keys stay in Interop as state store |
| `T_OUTBOUNDHIS` (HL7 outbox) | written synchronously (`HL7Helper`), `RetryCount` column, `EHRSyncJobs` file gen (`ApplicationJobs.java:1096`); **no active outbound sender** (`07 §3.2`) | dormant HL7, EHR files | Keep outbox table in PG (idempotent senders per `10 §7.2`); Interop consumes events → writes outbox → sends; `RetryCount` column semantics move to Kafka retry config |

### 5.2 Why Kafka over keeping pollers
- **Latency**: WhatsApp 15-min poll → sub-second delivery; ABDM 5s poll → near-real-time.
- **Removes scheduler complexity**: 30+ jobs in `ApplicationJobs.java` (node guards `DOCHKOUT_yyyyMMdd`, kill-switches `disablePatientIndexing`…) shrink to consumer configs.
- **Ordering & replay**: Kafka partitions preserve per-key order; pollers have none (`ROWNUM <= n` picks arbitrary rows, `agent-05 §5.2`).
- **Backpressure**: legacy pollers re-read the same rows on failure (double-send risk); Kafka offsets + idempotent consumers eliminate it.
- **Counter-evidence (why not Kafka-first everywhere)**: the legacy single DB gives cross-table ordering + atomic side effects for free; premature eventing before outbox+saga (E-07/E-08 gate) would **lose ACID without compensation** — hence §3 REJECTED list.

### 5.3 Migration steps (per pipeline)
1. Freeze new poller work; dual-run: poller keeps processing rows older than T0, Kafka consumes new events.
2. Interop consumer group drains remaining queue-table rows (idempotent by queue-entry id).
3. Delete queue tables + poller jobs after queue depth = 0 for 7 days; archive tables to `ihms_interop` read-model.
4. Reconcile: `T_WHATSAPPMESSAGESTATUS` vs WhatsApp API delivery callbacks; ABDM per-node counters vs topic lag.

---

## 6. Open Questions

1. Revenue event gating: saga design for the mega-txn (`AddServiceOrderServiceHelper.java:341-516`) — the single dependency for E-07/E-08/E-12 and deferred Lab/Rad events.
2. Per-store vs single-stock topic partitioning for E-09 (`10 §7 Q2`).
3. Topic retention: regulatory (GSTR/NHM/RGHS) vs operational retention (`10 §7 Q3`).
4. Audit stream: dedicated `audit-*` topic for `T_AUDIT`/`T_TRX_HISTORY` events (59 services, 568 call sites — `08 §1`) — **not designed yet**; candidate (next free id — E-13 was taken by the WP-S1 OrderCreated event).
5. Multi-unit tenancy in partition keys (unitId on nearly every table, `05 §7`) — cross-unit fan-out design.
6. ABDM/HCX JWE payloads: encrypt in producer (Interop) or consumer-side adapter? (Legacy encrypts at the edge, `07 #1/#2`.)
