# M07 — Workflows

## Dispensing flow (02 §M07 Workflow)
1. Prescription (doctor / nurse / uploaded) → pharmacist verify (`pharmacyOrderVerify`).
2. Dispense (batch/expiry check from stock — getItemAndItemBatchListByStoreId).
3. Bill/payment (OP/IP; `opIpPharmacyBillSchema`, `ipPharmacyBillingSchema`) → issue labels (printMedLabel, compoundingLable).
4. Returns (IP/OP: itemSaleReturnList, ipReturnFlow) → interventions.

## Visit-completion rule (BR-PHAR-001)
- Each item-sale detail `DISPENSED=true`; all details of an item sale dispensed → `T_ITEMSALE.DISPENSED=true`; no undispensed item sales remain for the visit → `T_Appointment.VisitStatus=12`.
- Evidence: MedicationServiceImpl:799-821.

## Prescription closure (BR-PHAR-002)
- `CIMS_ADDITEMSALESDETAILS_NEW` inserts sale detail (BatchId) and closes prescription: `IsClosed='1', PendingQty=0` (or `PendingQty−=qty`); master `IsClosed='1'`; compound details closed; dispense flags `isverified/isdispensed/ispartialdispense` on T_PRESCRIPTIONDETAILS.
- Evidence: proc :103-121; PharmacyServiceImpl:735-746.

## BCMA handoff (M30)
- Dispensed meds → bedside scan (unitDose, scanWristband/:patientMrn); missing-dose requests back to pharmacy.
