# Module: pathology (M15 — Pathology / Laboratory)

> Master reference: `../../MODULE-INVENTORY.md` §M15; catalog: `../../02-business-module-catalog.md` §M15; rules: `../../06-business-rules-catalog.md` §B (BR-PATH-001); frontend: `../../03-legacy-frontend-inventory.md` §2.6.

**Target service (10 §2.1):** 7. Lab (Pathology) — emits `LabOrderCreated`, `LabResultFinalized`; consumes OrderCreated/PatientRegistered/EncounterCreated.

**One-line purpose:** End-to-end laboratory workflow from order to released report.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_pathology.json` (57 keys); ~100 templates |
| Services | `/pathoOrder` (114), `/investigation`, `/machineConfig`, `/kdl` |
| God class | PathoOrderServiceImpl: 133 endpoints / 14,280 LOC (04 §9.1) |
| Business rules | BR-PATH-001 (sample collection locks charge removal — LEX001) |
| Complexity | 4/5 |

**Dependencies:** M08 (orders), M01 (patient), M04 (billing), M13 (consumables), M17 (blood bank barcode), M38 (KDL).

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md · tests.md
