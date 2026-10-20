# Module: billing (M04 — Billing, Payments, Receipts & Refunds)

> Master reference: `../../MODULE-INVENTORY.md` §M04; catalog: `../../02-business-module-catalog.md` §M04; rules: `../../06-business-rules-catalog.md` §B (BR-BILL-*) + §A2/A3/A6; frontend: `../../03-legacy-frontend-inventory.md` §2.2.

**Target services (10 §2.1):** 5. Billing & Settlement + 6. Payments & Refunds.

**One-line purpose:** Charge capture → estimation → bill generation → payment collection → receipts → refunds; doctor share settlement.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_billing.json` (77 keys) |
| Services | `/bill`, `/billReceipt`, `/billSummary`, `/billDet`, `/cancelBill`, `/addPayment`, `/addAdvance`, `/addRefund`, `/advancebillReceipt`, `/allReceipts`, `/refundBillReceipt`, `/patientbillestimation`, `/charge`, `/payment`, `/refund`, `/companyapprovedamount`, `/concessiontemplate` |
| Business rules | BR-BILL-001..008 |
| Procs | CIMS_ADDBILL, CIMS_ADDCHARGE, CIMS_ADDCONCESSION, CIMS_UPDATEBILLFORCONCESSION, CIMS_ADDPAYMENT*, CIMS_ADDADVANCE, CIMS_ADDDOCTORSHARE, CIMS_BILLFORINVESTIGATIONNEW, CIMS_SETTLED/UNSETTLEDBILLCANCELLATION (20 CIMS_ calls in BillingCommonUtilServiceImpl) |
| **Complexity** | **5/5 — revenue core; mega-transaction owner** |

**Critical risk:** Order→Bill→Lab→Rad→DoctorShare→EMR→HL7 mega-transaction (AddServiceOrderServiceHelper.java:341-516); concession asymmetry (BR-BILL-005 R2); VisitStatus 11/12 split-brain.

**Dependencies:** M01–M03, M05 (tariff/packages), M07 (pharmacy bill), M06 (doctor share), M32 (RCM), M33 (payment transfer), M08 (charges), M17 (blood bank billing).

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md · tests.md
