# Module: ipd (M03 — IPD / Admission, Bed & Discharge)

> Master reference: `../../MODULE-INVENTORY.md` §M03; catalog: `../../02-business-module-catalog.md` §M03; rules: `../../06-business-rules-catalog.md` §B (BR-IPD-*, BR-DIS-*); frontend: `../../03-legacy-frontend-inventory.md` §2.3.

**Target service (10 §2.1):** 3. ADT & Bed (owns T_ADMISSION, T_BEDRESERVATION, T_TRANSFERREQUEST, T_PATIENTDISCHARGEDATA, T_MLCDETAILS, T_ATTENDANTPASS, bed/ward/room/cabin masters; emits PatientAdmitted, PatientDischarged, BedChanged, TransferRequested).

**One-line purpose:** Inpatient stay lifecycle: admission → bed → stay services → discharge → bed release.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_ip.json` (65 keys) |
| Services | `/ipdadmission`, `/ipd`, `/ipdconfig`, `/bed`, `/bedreservation`, `/bedmanagement`, `/canceladmission`, `/patientTransferDischarge`, `/patientRegAddmission` |
| Business rules | BR-IPD-001..005, BR-DIS-001..003 |
| Procs | CIMS_ADDADMISSIONDETAILS, CIMS_ADDADMISSIONINFORMATION, CIMS_UPDATEBEDOCCUPIEDFLAG_1, CIMS_ADDBEDDISCHARGE, CIMS_UPDATEIPDISDISCHARGE |
| Complexity | 4/5 |

**Key statuses:** M_BEDMASTER.OCCUPIED (atomic CAS) + BedStatusId (0/1/3/5/6); T_Admission ISDischarged/IsClosed/ISCANCEL+ABSCONDDATETIME.

**Dependencies:** M01, M02 (emergency), M04 (billing/clearance), M10 (diet), M09 (nursing), M08 (orders), M28 (mortuary), M11 (CSSD), M15 (blood requisition), M18 (BMW clearance).

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md · tests.md
