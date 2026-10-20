# M04 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| BillServiceImpl | `/bill` | getAllBills*, finalize, cancellation helpers (CIMS_SETTLED/UNSETTLEDBILLCANCELLATION :70-91) |
| BillReceiptServiceImpl | `/billReceipt` | receipts, printCreditNoteForOP/IP |
| BillSummaryServiceImpl | `/billSummary` | summaries (draft/final) |
| BillDetServiceImpl | `/billDet` | bill details |
| BillCancellationServiceImpl | `/cancelBill` | settled/unsettled cancellation; refundReceiptByIds; BR-BILL-008 |
| AddPaymentServiceImpl | `/addPayment` | addBillPaymentNew, itemSalePayment, updateCharges; +AddPaymentServiceHelper (BR-BILL-006/007) |
| AddAdvanceServiceImpl | `/addAdvance` | getAdvanceAgainstList, getPatientAdvanceByBillEstimation, addAdvance |
| AddRefundServiceImpl | `/addRefund` | addRefund (transactionType=3) |
| AdvanceBillReceiptServiceImpl | `/advancebillReceipt` | advance receipts |
| AllReceiptsServiceImpl | `/allReceipts` | all receipt list |
| RefundBillReceiptServiceImpl | `/refundBillReceipt` | refund receipts |
| PatientBillEstimationServiceImpl | `/patientbillestimation` | estimation |
| ChargeServiceImpl | `/charge` | charges |
| PaymentServiceImpl | `/payment` | payment |
| RefundServiceImpl | `/refund` | refund |
| ConcessionTemplateServiceImpl | `/concessiontemplate` | concession templates |
| CompanyApprovedAmountServiceImpl | `/companyapprovedamount` | company approved amounts |
| AddServiceOrderServiceImpl | `/serviceOrder` | addServicechargesNew, includeCharge/excludeCharge/removeCharge, saveCreditNote, addPackageRefund, moveOPChargesToIP, getChargesForCreditNote, checkSampleStatusForCreditNote |
| MasterCachedServiceImpl | `/master` | getDoctorFrequentServices, filter* lookups |

**Endpoint scale (03 §5):** billing module 34 routes; serviceOrder 55 endpoints.
