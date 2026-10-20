# M07 — Database (05 §3, 10 §2.1 row 9)

## Tables
| Table | Notes |
|---|---|
| T_PRESCRIPTIONMASTER / T_PRESCRIPTIONDETAILS / T_PRESCRIPTIONDISPENSEDETAILS | Prescription + dispense; IsClosed/PendingQty; isverified/isdispensed/ispartialdispense |
| T_PATIENTMEDICATION / T_PATIENTPRESCPTIONNOTE / T_PATIENTNONMEDORDERDETAILS | Medication orders/notes |
| T_ITEMSALE (+DETAILS) | Item sales; DISPENSED; BillID (billing linkage) |
| T_ITEMCOMPOUNDDETAILS | Compounding |
| T_INFUSIONMIXING (+DETAILS) | IV admixture (M30) |
| IHR_MEDICATION_TMP | BCMA temp table (M30) |
| M_ITEMMASTER (18 refs) / T_ITEMSTOCK (15) / T_ITEMCLINIC (17) / T_ITEMBATCHMASTER | **Stock shared with Inventory (M13)** — dual-write hotspot |
| masters/Pharmacy | AdministrationSchedule, CreditAccountMaster, Drugingredients, Druginteractions, Medicationroute |
| masters/meds | instructionmaster, route, scheduleListMaster |

## Stored procedures (CIMS_*)
- `CIMS_ADDITEMSALESDETAILS_NEW` (sale detail + prescription closure, BR-PHAR-002)
- `CIMS_ADDITEMSTOCK` (stock movement)
- `CIMS_GETPRESCRIPTIONWARDDETAILS*` (ward pick lists)
- Sequences: `T_PrescriptionDetails_seq` (procs)

## Migration notes
- **Pharmacy/Inventory stock boundary is the #1 open question (10 §7 Q1)**: legacy Pharmacy writes stock directly (PharmacyServiceImpl.java:2140); target: Pharmacy borrows stock ops via Inventory REST API + InventoryUpdated event (10 §2.2).
- Stock decrement must use row-level locking like legacy procs (10 §3.4).
