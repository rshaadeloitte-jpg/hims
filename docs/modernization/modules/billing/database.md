# M04 — Database (05 §3, 10 §2.1 rows 5-6)

## Tables
| Table | Notes |
|---|---|
| T_BILL / T_BILLDETAILS | Bill header/lines; Status/IsSettled/IsCancelled/InterORFinal/IsPharmacy; changeAmount |
| T_CHARGES / T_CHARGES_TEMP | IsBilled/IsCancelled/IsTemp; *BeforeFinalization snapshots |
| T_PAYMENT / T_PAYMENTMODEDETAILS | TransactionType 2/3/4; payment-mode 7 |
| T_ADVANCE / T_REFUND / T_RECEIPT | Advances FIFO (Used/Balance), refunds, receipts |
| T_ITEMSALE (+DETAILS) | BillID linkage; DISPENSED flags (M07) |
| T_DOCTORSHARE / T_DOCTORBILL | Doctor share settlement (M06) |
| T_SPONSOR / T_PATIENTSPONSOR | Sponsors |
| T_TARIFFSERVICEMASTER / T_SURGCHARGES | Tariff linkage (M05) |
| CreditNote(+Details) | Credit notes |
| PackageChargeSubscription / PatientBillEstimationSurgeryHead | Package/OT estimation |
| Doctorvisitcharge / Doctorwaiver | Visit charges/waivers (M06) |
| masters/billing | companymaster, companyplandetails, tariffmaster, packagemaster, planmaster, concessiontemplatemaster, paymentmodetype, cashcountermaster, cancellationreason, advanceagainst, companyassociatemaster, companytypemaster, concessioncategorymaster, billingDepartmentMaster |
| masters/hospital | accountheadmaster, bankmaster |
| s_servicewisecollection / s_userwisecollection | Statistical collection aggregates (reports) |

## Stored procedures (CIMS_*) — 20 live calls in BillingCommonUtilServiceImpl (05 §5)
CIMS_ADDBILL · CIMS_ADDCHARGE · CIMS_ADDCONCESSION · CIMS_UPDATEBILLFORCONCESSION · CIMS_ADDPAYMENT* · CIMS_ADDADVANCE · CIMS_ADDDOCTORSHARE · CIMS_BILLFORINVESTIGATIONNEW · CIMS_ADDITEMSALESDETAILS_NEW · CIMS_ADDPAYMENTADVANCEDETAILS · CIMS_SETTLEDBILLCANCELLATION · CIMS_UNSETTLEDBILLCANCELLATION · CIMS_ADDPATIENTSPONSOR_NEW_DOC.

Sequences: `T_PaymentModeDetails_seq`, `T_Refunddetail_seq` (procs).

## Migration notes
- 10 §2.1: Billing owns T_BILL/T_BILLDETAILS/T_CHARGE/T_DOCTORBILL/T_DOCTORSHARE/T_CONCESSION/T_CREDITNOTE; Payments owns T_PAYMENT/T_PAYMENTMODEDETAILS/T_ADVANCE/T_REFUND/T_RECEIPT.
- Partitioning consideration for T_BILL by year (10 §3.4).
- Shared-table cutover list includes T_BILL (10 §5).
