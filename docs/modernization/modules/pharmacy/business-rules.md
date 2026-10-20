# M07 — Business Rules (06 §B)

| ID | Rule | Evidence | Tests |
|---|---|---|---|
| BR-PHAR-001 | Dispensing completes visit: detail DISPENSED → item sale DISPENSED → no undispensed sales → T_Appointment.VisitStatus=12 | MedicationServiceImpl:799-821 | + full; + partial (detail level); boundary: last item sale |
| BR-PHAR-002 | Prescription closure on sale: CIMS_ADDITEMSALESDETAILS_NEW inserts detail (BatchId), closes prescription (IsClosed='1', PendingQty=0 or −=qty), compound details closed; dispense flags isverified/isdispensed/ispartialdispense | proc :103-121; PharmacyServiceImpl:735-746 | + full qty → closed; + partial → pending reduced |

## Status codes (06 §A9, GenericConstants)
- Medication: 1=ACTIVE, 2=DISCONTINUE, 3=EXPIRED.
- Patient med: 1=ORDERED, 2=TAKEN, 3=MISSED, 4=SKIPPED.
- Prescription/dispensing/return status code lists: UNKNOWN-14 (02 §M07).
