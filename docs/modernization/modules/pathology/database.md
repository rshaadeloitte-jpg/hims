# M15 — Database (05 §3, 10 §2.1 row 7)

## Tables
| Table | Notes |
|---|---|
| T_PATHORDERBOOKING (+DETAILS) | Orders; ISCANCELLED flag |
| T_PATHOPATIENTPARAMETERDETAILS | Result parameters |
| T_MACHINECONFIGURATION | Machine config |
| PathoPatientDocs / PathOrderBookingDetailDocuments | Lab documents (M34) |
| IhmsCounts | Counters |
| M_PATHOTESTMASTER (12 refs) | Test master |
| masters/pathology (~35) | pathotestmaster, pathoparametermaster, pathoprofilemaster, pathotemplatemaster, pathosamplemaster, machinemaster, organismmachinmap, pathantibioticmaster, pathoremarksmaster, pathozonemaster, pathocategory, discardreasonmaster, antibioticmachinemap, containermaster, cultureNegativeMaster, errorflagmaster, gendercategory, labconfigmaster, machinetestmaster, organmaster, parameterresultingmethod, pathoCabinMaster, patholocationmaster, pathoparameterunits, pathoparammachinemap, pathorganismmaster, pathosubspecialization, pathsensitivityresultmaster |

## Stored procedures
- `CIMS_ADDPATHORDERBOOKING` (order booking).

## Migration notes (10 §2.1 row 7)
- Target owns T_PATHORDERBOOKING(+DETAIL), T_PATHOPATIENTPARAMETERDETAILS, T_MACHINECONFIGURATION, patho masters.
- Emits `LabOrderCreated`, `LabResultFinalized`; consumes OrderCreated/PatientRegistered/EncounterCreated; WhatsApp/EHR via Interop.
- M_PathoTestSample defines auto-created sample details (BR-BILL-003).
