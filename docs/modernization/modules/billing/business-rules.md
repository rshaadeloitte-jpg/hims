# M04 — Business Rules (06 §B)

| ID | Rule | Evidence | Tests |
|---|---|---|---|
| BR-BILL-001 | Draft header (BillNo='000000000000000', CashCounterId=1); final number from `getLatestAutocodeSeq("T_OPDBill", op.bill.no.template)`; net = Total−Tax−Concession; CIMS_ADDBILL (IsCancelled=0) | AddServiceOrderServiceHelper:217-341; CIMS_ADDBILL.sql | + draft (no number); + final; boundary: seq formatting |
| BR-BILL-002 | Doctor share: skip if tariff-service `otProcedure=true`; `share = total×%/100`, hospital = total−share; CIMS_ADDDOCTORSHARE (IsBilled=false); CEILING in DB / HALF_UP on DTO | :383-419 | + share>0; + share=0 → none; − OT → none |
| BR-BILL-003 | Patho (spec 1)/rad (spec 2) charges → CIMS_BILLFORINVESTIGATIONNEW → order booking + details per `M_PathoTestSample` (SampleCollectionNO=1..n); rad analog IsImageCaptured=false | :421-633 | + patho bill → orders; + rad; + non-patho → none; boundary: 0 samples |
| BR-BILL-004 | Tax ≤3 buckets; applyafterconcession variant; TaxAmount=Σ taxN | :2767-2798 | + before; + after; + no taxId → 0 |
| BR-BILL-005 | Co-pay/concession split; self adds %, non-self replaces % (asymmetric — RISK R2) | :2800-2851 | + self company; + non-self; + both tiers (divergence test) |
| BR-BILL-006 | Settled ⇔ self & non-self balances ≤ 0 (CIMS_UPDATEBILLFORCONCESSION); OPD checkout → isCheckout + VisitStatus=11; changeAmount recorded when `invstopdpostbilling` | AddPaymentServiceHelper:438-506 | + full → settled+checkout; + partial → unsettled |
| BR-BILL-007 | Advance FIFO; payment-mode 7; CIMS_ADDPAYMENTADVANCEDETAILS | :178-203, :313-349 | + full; + partial; + multiple advances order |
| BR-BILL-008 | Refund types 3/4; settled-cancel restores T_Advance + charges from *BeforeFinalization + T_ITEMSALE reset + doctor-share false | AddRefundServiceImpl:82-155; BillCancellationServiceImpl:231-295,347,470-509 | + advance refund; + bill cancel restore; − zero balance → BEXAR002 |

## Status dictionary (06 §A2/A3/A6)
- T_Bill: Status=1 active; IsSettled false→true; IsCancelled=0; InterORFinal; IsPharmacy=0.
- T_Charges: IsBilled; IsCancelled; IsTemp (T_CHARGES_TEMP); *BeforeFinalization snapshots.
- T_Payment.TransactionType: 2=bill payment, 3=advance refund, 4=bill refund; payment-mode 7=advance adjustment.
- GST: `isGSTApplicable=true` AND transDate ≥ 2017-07-01 (BaseServiceImpl:521-533).
