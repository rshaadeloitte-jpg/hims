# Module: ot-surgery (M12 — Operation Theatre & Surgery)

> Master reference: `../../MODULE-INVENTORY.md` §M12; catalog: `../../02-business-module-catalog.md` §M12; rules: `../../06-business-rules-catalog.md` §B (BR-OT-*); frontend: `../../03-legacy-frontend-inventory.md` §2.11.

**Target service (10 §2.1):** 11. OT & Surgery (emits `ProcedureScheduled`, `SurgeryCompleted`; consumes OrderCreated/PatientAdmitted; reads patient via Search; emits Inventory consumption).

**One-line purpose:** Surgical case planning, scheduling, resource allocation and charge capture.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_ot.json` (14 keys); ~80 OT templates |
| Services | `/otscheduler` (79), `/otconfig` (35), `/otdetails`, `/surgerydetails` |
| Business rules | BR-OT-001 (scheduling prerequisites — BEOT004), BR-OT-002 (OT pre-billing) |
| Complexity | 3/5 |

**Dependencies:** M03 (IPD patient), M08 (orders), M04 (billing), M11 (CSSD kits), M13 (OT items/equipment), M07 (procedure prescriptions).

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md · tests.md
