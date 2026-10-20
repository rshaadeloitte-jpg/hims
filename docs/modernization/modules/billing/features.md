# M04 — Features (02 §M04)

| Feature | Evidence |
|---|---|
| OP/IP billing | addOpIpBilling, addBillingSchema, addIpBillingSchema; billingFlow / ipBillingFlow / opIpBillingFlow routes |
| Partial payment | partialPayBillSchema; `addPayment/addBillPaymentNew` |
| Advance against bills | addAdvanceSchema; BR-BILL-007 (FIFO); payment-mode 7 |
| Bill estimation | billEstimation.vm; patientbillestimation; addOTBillEstimationSchema; getAllEstimationBillSchema |
| IP bill with pharmacy separate | ipBillSummeryWithoutPharmcy.vm; ipPharmacyBilling (M07) |
| Credit notes | creditNoteSchema, getAllCreditNoteListSchema; `serviceOrder/saveCreditNote` |
| Doctor share add/deduct & bill | doctorShareSchema, doctorShareAddDeductSchema, doctorShareBillSchema; BR-BILL-002 |
| Sponsor change | changeSponsorSchema(+List), showSponsorSchema |
| Company approved amounts | addApprovalAmountSchema, approveAmountListSchema; `/companyapprovedamount` |
| Package subscription | addPackageSubscriptionSchema; packageSubscriptionFlow; addPackageRefund |
| Tariff copay details | tariffcopaydetails, tariffConfigPackageDetails |
| Receipts (bill/advance/refund/combined) | billReceiptSchema, advanceReceiptByReceiptNoSchema, combineBillReceiptSchema, refund_recieptSchema |
| Bill cancellation (settled/unsettled) | cancelOpBillSummarySchema, cancelIpBillSummarySchema; BillCancellationServiceImpl |
| Health-plan consumption | healthplanConsumptionDetailSchema; checkoutForHealthConsumption |
| Change/refund return flows | return_advance, return_bill_advance-schema |

## Client-side billing math (03 §3 — must port faithfully)
- `calculateTotalBillAmount` (billing-module.js:2050): services+pharmacy totals, 3 tax buckets, concession, self/non-self split, package coverage, corporate approval, max-concession caps, VAT.
- `confirmServices`/`confirmServicesStep2/3`/`finalServiceSummary` wizard with per-step POSTs.
