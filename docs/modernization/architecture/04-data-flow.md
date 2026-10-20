# C4 — Key Data Flows: iHMS / OHUM PAS NG

> **Document type:** C4 Level 4 (sequence-level data flows) — synthesized from discovery docs; no new code exploration.
> **Inputs:** `07-integrations-and-search.md` (§1.3, §3.2), `_discovery/agent-10/findings.md` (§2 transaction boundaries), `10-target-architecture.md`, `04-backend-architecture.md`.
> **Status:** AS-IS flows evidence-based (file:line cited); TO-BE adaptations marked **PRELIMINARY**.

---

## 1. Flow F1 — Patient Registration → Solr Index → ABDM/HL7 side effects

**AS-IS:** registration saves to Oracle, synchronously indexes Solr, mirrors to Redis, enqueues ABDM/HL7 — all inside the same Spring transaction; on rollback the aspect compensates by deleting Solr doc + Redis keys.

```mermaid
sequenceDiagram
    autonumber
    actor Clerk as Registration Clerk
    participant FE as FAUG SPA (fjs-config_patientManagement)
    participant SVC as PatientServiceImpl / PatientRegAdmissionServiceImpl
    participant DB as Oracle (CIMS_ADDPATIENTREGISTRATION*)
    participant SLR as SolrCloud (patient_risl_qa)
    participant RDS as Redis (current_pat_*)
    participant ABDM as ABDM queue (T_ABDMREQUESTQUEUE)

    Clerk->>FE: registerPatient / demographic search
    FE->>SVC: POST /api/rest/patient/registerPatient
    SVC->>SVC: searchPatientInSolr (dup check, ≤15 hits → DB re-verify)
    SVC->>SLR: searchPatientInSolr (edismax boosts mrno^18, mobile^16, janaadhar^17)
    SLR-->>SVC: hits
    SVC->>DB: CIMS_ADDPATIENTREGISTRATION (tx start)
    DB-->>SVC: MRN / patientId
    SVC->>SLR: SolrUtil.indexPatientInSolr (addBean, no commit — autoSoftCommit)
    SVC->>RDS: cacheImpl.add(current_pat_<id>, view, TTL 100s)
    SVC->>DB: HL7 outbound row (T_OUTBOUNDHIS) + ABDM queue entry
    SVC->>ABDM: addEntryToABDMQueue (5s poller later)
    Note over SVC: tx rollback → ServiceLayerAspect:1164-1182 deletes Solr doc + Redis keys
    SVC-->>FE: JSON response
    FE->>Clerk: registration saved
```

**Evidence:** `PatientServiceImpl.java:5610` (solr index on save), `SolrUtil.java:174-333`, `07` §1.3; `HelperServiceImpl.java:221` (HL7); `EmergencyAndWalkinVisitServiceImpl.java:1937` (ABDM queue); aspect compensation `ServiceLayerAspect.java:1164-1182`.

**TO-BE (PRELIMINARY):** register → local outbox (PG) → Kafka `PatientRegistered` → Search(27) indexes Solr, Interop(25) sends HL7/ABDM. No in-txn Solr writes.

---

## 2. Flow F2 — Service Order → Bill → Payment → Receipt (the mega-transaction)

**AS-IS:** one `@Transactional(rollbackOn=Exception.class)` method in `AddServiceOrderServiceHelper.java` chains 12+ stored-proc calls spanning four future bounded contexts (Ordering, Billing, Lab, Radiology) plus EMR, doctor share, package subscription and HL7 ORM messages.

```mermaid
sequenceDiagram
    autonumber
    actor Doc as Doctor / Ordering user
    participant FE as FAUG SPA (addOpIpBillingSchema)
    participant H as AddServiceOrderServiceHelper (tx:341-516)
    participant DB as Oracle procs
    participant WQ as T_WHATSAPPMESSAGEQUEUE

    Doc->>FE: select services / place order
    FE->>H: /serviceOrder or /patient addServiceOrderDetails
    Note over H: @Transactional(rollbackOn=Exception.class)
    H->>DB: CIMS_ADDCHARGE (:265)
    H->>DB: CIMS_ADDBILL — bill header, seq from DUAL (:341)
    H->>DB: CIMS_ADDBILLDETAILS_1 (:370)
    H->>DB: CIMS_ADDDOCTORSHARE (:417)
    alt lab tests in order
        H->>DB: CIMS_ADDPATHORDERBOOKING (:473) + DETAIL (:516)
    end
    alt radiology in order
        H->>DB: CIMS_ADDRADORDERBOOKING (:579) + DETAILS (:599)
    end
    opt concession
        H->>DB: CIMS_ADDCONCESSION / DETAIL (:722-737)
    end
    H->>DB: CIMS_UPDATECHARGES / UPDATECHARGESTEMP (:908-991)
    H->>DB: EMR write (EmrDetails/EmrServices :2351/:2367)
    H->>DB: HL7 ORM outbox (:2299-2399)
    H->>WQ: WhatsApp bill PDF queue row (:3924-3926)
    DB-->>H: OK
    H-->>FE: bill summary
    FE->>FE: Payment screen (billReceiptSchema)
    FE->>AddPaymentServiceHelper: CIMS_ADDPAYMENT (:291) + BILLDETAILS (:309) + MODEDETAILS (:333) + ADVANCEDETAILS (:348)
    AddPaymentServiceHelper->>DB: CIMS_UPDATEBILLFORCONCESSION (:463)
    AddPaymentServiceHelper-->>FE: receipt (bill_receipt.vm / ipbillreceipt.vm)
```

**Transaction-boundary hotspot table** (from agent-10 §2.1):

| Line span | Proc calls | Crosses future service boundary |
|---|---|---|
| `:265` | CIMS_ADDCHARGE | Billing (5) |
| `:341`, `:370` | CIMS_ADDBILL(+DETAILS) | Billing (5) |
| `:417` | CIMS_ADDDOCTORSHARE | Billing/Doctor share (5) |
| `:473`, `:516` | CIMS_ADDPATHORDERBOOKING(+DETAIL) | **Lab (7)** |
| `:579`, `:599` | CIMS_ADDRADORDERBOOKING(+DETAILS) | **Radiology (8)** |
| `:722-737` | CIMS_ADDCONCESSION(+DETAIL) | Billing (5) |
| `:908-991` | CIMS_UPDATECHARGES(+TEMP) | Billing (5) |
| `:2351/:2367` | EMR write | **Ordering/Clinical (4)** |
| `:2299-2399` | HL7 ORM outbox | **Interop (25)** |
| `:3924-3926` | WhatsApp queue row | **Interop (25)** |

**TO-BE (PRELIMINARY):** saga choreography — OrderCreated (4) → Billing(5) computes charges → InvoiceGenerated → Payments(6) → PaymentReceived; Lab/Rad order creation becomes LabOrderCreated/RadOrderCreated events consumed by Billing for bill lines. Compensations already exist as procs (CIMS_SETTLEDBILLCANCELLATION / CIMS_UNSETTLEDBILLCANCELLATION, `BillServiceImpl.java:70-91`).

---

## 3. Flow F3 — Lab Order → Result → WhatsApp Report

**AS-IS:** lab order is booked inside the billing mega-txn; results flow through review/release; released reports are queued to `T_WHATSAPPMESSAGEQUEUE` and drained by a 15-minute poller (window 14:00–07:00), which generates PDF and sends via eSanchar.

```mermaid
sequenceDiagram
    autonumber
    actor Tech as Lab Technician
    participant P as PathoOrderServiceImpl / PathoOrderServiceHelper
    participant DB as Oracle (T_PATHORDERBOOKING, T_PATHOPATIENTPARAMETERDETAILS)
    participant WQ as T_WHATSAPPMESSAGEQUEUE
    participant J as ApplicationJobs.triggerWhatsappReportsInQueue (15-min poll)
    participant PDF as Jasper/Velocity render
    participant ES as eSanchar WhatsApp API

    Tech->>P: pathoOrder / sample collect (barcode) / accept
    P->>DB: result entry per parameter (resulting)
    P->>P: result review → report release (reportReleaseSchema)
    P->>WQ: insert queue row (T_WHATSAPPMESSAGEQUEUE)
    loop every 15 min (14:00-07:00)
        J->>WQ: SELECT pending (SYNCHRONIZED=false, ≤5 attempts)
        J->>PDF: generate report PDF (PathoOrderServiceHelper:1130-1208)
        J->>ES: POST WhatsApp message
        ES-->>J: status
        J->>WQ: update T_WHATSAPPMESSAGESTATUS
    end
```

**Evidence:** `PathoOrderServiceHelper.java:1130-1208` (sendCategoryWiseReportOnWhatsApp), `ApplicationJobs.java:1598-1643` (poller, max trigger 5), `07` §3.1#15, agent-10 §4.3.

**TO-BE (PRELIMINARY):** `LabResultFinalized` event → Interop(25) consumes and sends; `T_WHATSAPPMESSAGEQUEUE` replaced by Kafka topic (proven DB-poll → topic pattern).

---

## 4. Flow F4 — Admission → Bed → Discharge → Bill

**AS-IS:** admission procs lock bed occupancy and write MLC/advised-discharge; discharge clears the encounter (Solr) and settles bills; bed auto-charges run by scheduled job.

```mermaid
sequenceDiagram
    autonumber
    actor Clerk2 as Admission Clerk
    participant A as IPDAdmissionServiceImpl / BedReservationServiceImpl
    participant DB as Oracle (CIMS_ADDADMISSIONINFORMATION, CIMS_UPDATEBEDOCCUPIEDFLAG_1)
    participant SLR as Solr (encounter index, id <admId>_A)
    participant B as PatientTransferDischargeServiceImpl
    participant J as ApplicationJobs (auto charges, Solr jobs)

    Clerk2->>A: ipdadmission (bed allocation)
    A->>DB: CIMS_ADDADMMLCDETAILS (:130) + CIMS_ADDADVISEDDISCHARGELIST (:196)
    A->>DB: CIMS_UPDATEBEDOCCUPIEDFLAG_1 (:313) [@Transactional]
    A->>SLR: indexAmissionInSolr (encounter doc, 3-day retention)
    Note over A: Bed reservation also sends HL7 registration message (BedReservationServiceImpl:166-180)
    loop daily
        J->>DB: CIMS_JOBSCHEDULING — bed auto-charges
        J->>SLR: closeDailyEncountersJob — close + delete 3-day-old docs (:542-570)
    end
    Clerk2->>B: discharge clearance (patientTransferDischarge)
    B->>B: bill settlement check (billing clearance)
    B->>SLR: clear encounter / partial patient update
    B->>DB: T_PATIENTDISCHARGEDATA + final bill (discharge summary)
    B->>DB: bed release → CIMS_UPDATEBEDOCCUPIEDFLAG_1
```

**Evidence:** `IPDAdmissionServiceImpl.java:130,196,313`, `PatientTransferDischargeServiceImpl.java:698-3282,4400-4413`, `ApplicationJobs.java:283` (CIMS_JOBSCHEDULING), `:542-570` (encounter close/delete), agent-10 §2.4.

**TO-BE (PRELIMINARY):** PatientAdmitted/BedChanged events drive bed state (3) and billing auto-charges (5); discharge events feed Billing settlement and EHR; encounter archive collection replaces 3-day deletion.

---

## 5. Flow F5 — Inventory: GRN → Stock → Dispense

**AS-IS:** GRN posting adds stock via procs; pharmacy dispensing decrements stock — **including a dual-write** where Pharmacy calls `CIMS_ADDITEMSTOCK` directly (the classic Pharmacy/Inventory boundary problem).

```mermaid
sequenceDiagram
    autonumber
    actor Store as Store / Purchase officer
    participant I as InventoryServiceImpl / PurchaseOrderServiceImpl
    participant P as PharmacyServiceImpl
    participant DB as Oracle (T_GRN, T_ITEMSTOCK, T_ITEMBATCHMASTER)
    participant SLR as Solr (item collection)
    participant EA as e-Aushadhi

    Store->>I: showGRNSchema / GRN posting
    I->>DB: CIMS_ADDGRN + CIMS_ADDGRNITEMS_IMPROVED1 / PROVISIONAL
    I->>DB: CIMS_ADDITEMSTOCK (stock + batch, ROWCOUNT/LOCK semantics)
    I->>SLR: flag SOLR_STATUS=0 → syncItemsToSolrJob (5s) drains → addBean
    Note over I: e-Aushadhi stock reconciliation nightly (eAushadiItemStockUpdate 04:00)
    I->>EA: indent issue / QC status / returns (reTriggerEAushadhiIndent AOP hook)
    Store->>P: pharmacy dispensing (dispensingtype, batch/expiry check)
    P->>DB: CIMS_ADDITEMSTOCK (PharmacyServiceImpl:2140) — DUAL-WRITE hotspot
    P->>DB: CIMS_ADDWARDPICKLIST (:2383) / CIMS_SETEMRNO (:886)
    P->>DB: T_ITEMSALE + bill (pharmacyReceipt.vm)
```

**Evidence:** `CIMS_ADDITEMSTOCK.sql` (updates T_ReceivedItemAgainstIssue/ReturnDetails + inserts T_ItemStock), `PharmacyServiceImpl.java:2140` (CIMS_ADDITEMSTOCK), agent-10 §3 row 4, `07` §3.1#8.

**TO-BE (PRELIMINARY):** Inventory(10) owns stock; Pharmacy(9) calls stock API or consumes `InventoryUpdated`; **open decision** — single stock service vs per-store ownership (`10` §7.1). GRN→stock stays a local Inventory transaction; e-Aushadhi sync via Interop/Inventory events.

---

## 6. Flow F6 — HL7 Inbound / Outbound

**AS-IS:** outbound messages are created synchronously inside domain transactions into `T_OUTBOUNDHIS` (no active sender found); inbound pickup job is disabled in checked-in config. Registration/visit/admission/orders/lab results are the emitting events (ADT/ORM/ORU/DFT/REF/OMP).

```mermaid
sequenceDiagram
    autonumber
    participant SVC as Domain services (registration/visit/admission/orders/lab)
    participant DB as Oracle (T_OUTBOUNDHIS / T_INBOUNDHIS)
    participant JOB as Hl7InboundMessageJob (pickup — DISABLED)
    participant EQ as Lab/CPOE/Pyxis equipment (HAPI v2.5)

    SVC->>SVC: HelperServiceImpl:221/252/307 — registration/visit/admission
    SVC->>DB: create*HL7Message (HL7Helper) → T_OUTBOUNDHIS row (same tx)
    Note over DB: Outbound review only — no active sender found (07 §3.2 HL7)
    alt inbound (legacy, disabled)
        EQ->>DB: (messages queued externally)
        JOB->>DB: pickup (hl7inbound.pickup.freq) — DISABLED
    end
    Note over SVC: AddServiceOrderServiceImpl:2299-2399 emits ORM for procedure/nursing orders in bill txn
```

**Evidence:** `HL7Helper.java:217-1091`, `HelperServiceImpl.java:221,252,307`, `Hl7InboundMessageJob.java:39-205`, `07` §3.1#10.

**TO-BE (PRELIMINARY):** domain events → Interop(25) outbox → HL7 sender (idempotent); inbound re-enabled deliberately (decision needed — dormant today). Keep outbox on PG with idempotent senders (`10` §7.2).

---

## 7. Flow F7 — ABDM: ABHA Linking / Consent / HIE Exchange

**AS-IS:** dual-version ABDM paths (v2+v3), Redis correlation store, DB queue with 5s node-sharded pollers, and catch-all callback controller.

```mermaid
sequenceDiagram
    autonumber
    participant SVC as Patient/ABDM services (AbhaServiceImpl, CommonABDMUtil)
    participant RDS as Redis (ABDM_REQ_<id>, ABDM_PAT_AUTH_<txId>, ABDM_SES_TKN)
    participant Q as T_ABDMREQUESTQUEUE
    participant J as ApplicationJobs addAbhaRecordsNode / sendPatientRecordToAbha (5s)
    participant G as ABDM Gateway (HIE-CM/HIP/HIU/HFR/HPR)

    SVC->>RDS: store ABDM session token (ABDM_SES_TKN, expiresIn)
    SVC->>G: create/verify ABHA (healthId, mobile+DOB, ±1yr, Levenshtein ≤3)
    SVC->>SLR: searchABHAPatientHIDInSolr — consent discovery
    SVC->>Q: addEntryToABDMQueue (EmergencyAndWalkinVisitServiceImpl:1937)
    loop every 5s (per node, sharded)
        J->>Q: pick node-shard rows (ISSEND/SYNCHRONIZED flags)
        J->>RDS: correlation keys (ABDM_REQ_/ABDM_RES_)
        J->>G: push records / consent artefacts (JWE)
        G-->>SVC: callbacks → ABDMController catch-all
        SVC->>RDS: remove correlation key on completion
    end
    Note over J: per-node success/fail counters <nodename>_ABDM_S / _ABDM_F
```

**Evidence:** `CommonABDMUtil.java:350-394,444-499,569-591`, `ApplicationJobs.java:1661-1688,2033-2034`, `07` §3.2 ABDM.

**TO-BE (PRELIMINARY):** ABDM flows owned by Interop(25) consuming domain events; Kafka replaces T_ABDMREQUESTQUEUE; single ABDM API version (v3) retained; Redis stays as short-lived correlation store only.

---

## 8. Transaction-boundary hotspot register (for decomposition)

Every row below is a place where one Spring transaction crosses what will become multiple service boundaries (`_discovery/agent-10/findings.md` §2):

| # | Hotspot | Single txn spans | Evidence |
|---|---|---|---|
| H1 | **Mega-txn** AddServiceOrderServiceHelper | Order, Bill, Lab, Rad, DoctorShare, EMR, HL7, WhatsApp | `:265-991`, `:2299-2399` |
| H2 | Payment txn AddPaymentServiceHelper | Bill settlement, payment, modes, advances, concession | `:291-463` |
| H3 | Visit/encounter txn EmergencyAndWalkinVisitServiceImpl | Encounter, queue, MLC, FPV, HL7 A45, corona, diagnosis, Solr, ABDM | `:1379-1937` |
| H4 | Admission txn IPDAdmissionServiceImpl | Admission, MLC, advised-discharge, bed flag | `:130-461` |
| H5 | Registration side-effect chain | Registration, HL7, Solr | `HelperServiceImpl:221,252,307`; `BedReservationServiceImpl:166-180` |
| H6 | Stock dual-write | Inventory + Pharmacy stock decrement | `PharmacyServiceImpl.java:2140` |
| H7 | Billing↔encounter status coupling | Bill creation updates visit/bill state; BillSummary reads Solr | `AddPaymentServiceHelper:463`; `BillSummaryServiceImpl:811` |
| H8 | Job-driven cross-module writes | Auto-charges (bed→bill), Solr drainers, WhatsApp poller | `ApplicationJobs` (14 @Transactional methods) |

**Guiding rule (TO-BE, PRELIMINARY):** every row above becomes either (a) a local transaction + outbox event, or (b) a saga participant with compensation. In-txn remote calls (Solr, HL7, ABDM) must move behind the outbox.
