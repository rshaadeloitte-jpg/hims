# M04 — Workflows

## Main flow (02 §M04 Workflow)
1. Services charged (orders/consumables/bed) → charges land in T_CHARGES_TEMP until finalized (06 §A3).
2. Bill summary (draft; BillNo='000000000000000', Status=true, IsSettled=false) → finalize → final bill number from `getLatestAutocodeSeq("T_OPDBill", op.bill.no.template)` / IP variant (BR-BILL-001).
3. Payment (cash/card/cheque/advance/company) → receipt print → settlement ⇔ self & non-self balances ≤ 0 (BR-BILL-006).
4. OPD + not admission-appointment → T_Encounter.isCheckout=true + Solr + VisitStatus=11 (BR-OPD-007).
5. Refund on cancellation: unsettled vs settled paths (BR-BILL-008) — charges restored from *BeforeFinalization snapshots, IsCancelled=true, T_ITEMSALE.BillID=0.

## Order → bill coupling (mega-transaction, 10 §1)
- `AddServiceOrderServiceHelper.java:341-516`: charge add → CIMS_ADDBILL/ADDCHARGE → patho/rad order auto-creation (BR-BILL-003, CIMS_BILLFORINVESTIGATIONNEW) → doctor share (BR-BILL-002) → EMR/HL7 outbound — one transaction.

## Tax (BR-BILL-004)
- ≤3 taxes (SGST/CGST/IGST; `allowNumberOfTaxCount=2` in use); `tax = rate × pct/100`; if `Taxmaster.applyafterconcession` → on (rate − rate×conPct/100); tax added to SelfAmount.

## Co-pay/concession (BR-BILL-005 — RISK R2)
- `selfRate = price × coPayPerc/100`; `nonSelfRate = price − selfRate`.
- Self-company: concession **adds** plan% then bill%. Non-self-company: concession **replaces** (plan% then bill%). Deductable 0 → 100 (full self-pay).

## Advance adjustment (BR-BILL-007)
- Advances with balance>0 consumed FIFO against self balance; partial split; recorded payment-mode 7 + CIMS_ADDPAYMENTADVANCEDETAILS.

## Cancellation/refund (BR-BILL-008)
- Advance refund: transactionType=3, advance balance>0 else BEXAR002. Bill refund: transactionType=4. Settled-bill cancellation: T_Advance Used−=n / Balance+=n; doctor-share status=false.
