# Module: nursing (M09 — Nursing: Worklist, Notes, Allocation, Care Plans)

> Master reference: `../../MODULE-INVENTORY.md` §M09; catalog: `../../02-business-module-catalog.md` §M09; frontend: `../../03-legacy-frontend-inventory.md` §2.4; security: `../../08-security-rbac-audit.md` §4.1.

**Target service (10 §2.1):** 12. Nursing & Inpatient Care (+ 26. Worklist ownership — boundary per 02 open Q5).

**One-line purpose:** Ward nursing operations: task management, documentation, patient assignment, handover.

| Aspect | Summary (evidence) |
|---|---|
| Configs | `fjs-config_nursing.json` (28 keys) + template-only: nursingWorklist, careplan, charting, shifthandover (UNKNOWN-04 ownership) |
| Service | `/nursingList` — **NursingServiceImpl: 300 endpoints, 17,350 LOC — largest god class** (04 §9.1) |
| Clients | nursing-module.js (795 KB), nursing-module-ext.js (674 KB), nursing-note-module.js (244 KB), nursing-charting-module.js, incident-reporting-module.js |
| Complexity | 4/5 |

**Key risk:** single-file tx boundary; tiuxml forms engine; vitals/charting configs; worklist ownership ambiguity.

**Dependencies:** M03 (IPD), M07 (med orders), M08 (service orders), M15/M16 (results), M26 (worklist), M27 (queue nurse desk), M40 (tiuxml forms), M04 (billing).

**Per-module files:** features.md · workflows.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md (business-rules.md: no formal BR-* IDs assigned in 06; statuses listed in README of master doc)
