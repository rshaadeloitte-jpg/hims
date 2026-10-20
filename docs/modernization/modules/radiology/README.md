# Module: radiology (M16 — Radiology)

> Master reference: `../../MODULE-INVENTORY.md` §M16; catalog: `../../02-business-module-catalog.md` §M16; frontend: `../../03-legacy-frontend-inventory.md` §2.6.

**Target service (10 §2.1):** 8. Radiology — emits `RadOrderCreated`, `RadResultFinalized`; consumes OrderCreated.

**One-line purpose:** Imaging orders, acquisition queue, reporting and report release.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_radiology.json` (9 keys) |
| Services | `/radiology` (12), `/reports` (getRadiologyReports) |
| Business rules | cross-ref BR-BILL-003 (rad order auto-creation), BR-PATH-001 (charge removal lock) |
| Complexity | 2/5 |

**Dependencies:** M08 (orders), M04 (billing), M01 (patient).

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md
