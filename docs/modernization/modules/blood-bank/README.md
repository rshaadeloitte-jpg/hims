# Module: blood-bank (M17 — Blood Bank)

> Master reference: `../../MODULE-INVENTORY.md` §M17; catalog: `../../02-business-module-catalog.md` §M17; rules: `../../06-business-rules-catalog.md` §B (BR-BLD-*) + §A7/A8; frontend: `../../03-legacy-frontend-inventory.md` §2.7.

**Target service (10 §2.1):** 13. Blood Bank — emits `BloodRequisitionCreated`, `BloodIssued`; consumes PatientAdmitted, LabResultFinalized (serology).

**One-line purpose:** Whole blood & component management: donor to transfusion.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_bloodBank.json` (40 keys); 21 tables |
| Services | `/bloodtestmaster` (115), `/patientrequisition` |
| Business rules | BR-BLD-001..005 (requisition machine, decross 48h, expiry 23:00, requisition expiry, donor auto-cancel) |
| Jobs | 4 scheduled jobs (decross, expiry, requisition expiry, donor cancel) + eRaktkosh nightly |
| Complexity | 4/5 |

**Critical risks:** DNR_REJECTED==DNR_DONATED==5 literal collision (06 §C.3); requisition header/detail asymmetry (R7); serology placement ambiguity (M15 vs M17).

**Dependencies:** M01 (patient/donor), M08 (requisition orders), M13 (consumables), M04 (billing), M15 (serology).

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md · tests.md
