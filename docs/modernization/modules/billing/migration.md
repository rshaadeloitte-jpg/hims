# M04 — Migration

**Target services:** 5. Billing & Settlement + 6. Payments & Refunds — **Wave 2 (Revenue)** (10 §6, complexity **highest/very high**).

**Complexity: 5/5** — mega-transaction owner; 13+ CIMS_* procs; asymmetric concession; VisitStatus split-brain; client-side billing math.

**Steps:**
1. **Split the mega-transaction** (10 §4 row 1): Order→Bill→Lab→Rad→DoctorShare→EMR→HL7 today is atomic (`AddServiceOrderServiceHelper.java:341-516`). Target: REST (phase 1) → Kafka saga (phase 2) with `InvoiceGenerated`, `ChargeAdded`, `BillCancelled` events; compensation via existing settled/unsettled cancellation flows.
2. **Lab/Rad order auto-creation** (BR-BILL-003) moves out of billing tx → LabOrderCreated/RadOrderCreated events post-split (10 §4).
3. **Payments:** InvoiceGenerated event + REST settlement update (legacy payment procs update bill rows — AddPaymentServiceHelper:463).
4. **Golden-master the billing math**: client-side `calculateTotalBillAmount` + tax/concession/co-pay rules → contract tests; include the non-self concession overwrite divergence (RISK R2).
5. **Procs → PL/pgSQL:** CIMS_ADDBILL/ADDCHARGE/ADDCONCESSION/ADDPAYMENT*/ADDADVANCE/SETTLED+UNSETTLEDBILLCANCELLATION on extraction path (10 §5).
6. **Sequences:** preserve bill-number templates (`getLatestAutocodeSeq("T_OPDBill", op.bill.no.template)`, IP variant) — external references (10 §3.2).
7. **Cutover:** T_BILL, T_CHARGES, T_ITEMSTOCK are critical shared-table cutover list (10 §5).
8. **Bed auto-charges:** BedChanged event → Billing (CIMS_JOBSCHEDULING job today, ApplicationJobs:283).

**Exit criteria (Wave 2):** mega-txn split into saga; InvoiceGenerated/PaymentReceived streams live.
