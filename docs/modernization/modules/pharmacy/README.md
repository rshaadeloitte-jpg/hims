# Module: pharmacy (M07 — Pharmacy & Dispensing)

> Master reference: `../../MODULE-INVENTORY.md` §M07; catalog: `../../02-business-module-catalog.md` §M07; rules: `../../06-business-rules-catalog.md` §B (BR-PHAR-*); frontend: `../../03-legacy-frontend-inventory.md` §2.5.

**Target service (10 §2.1):** 9. Pharmacy & Dispensing.

**One-line purpose:** Medication ordering → verification → dispensing → administration → return; OTC/counter sales; pharmacy billing.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_pharmacy.json` (39 keys) |
| Services | `/pharmacy`, `/prescriptionmaster`, `/drug`, `/medication`, `/prescription`, `/itemsales` |
| Business rules | BR-PHAR-001 (dispensing completes visit, VisitStatus=12), BR-PHAR-002 (prescription closure on sale) |
| Procs | CIMS_ADDITEMSALESDETAILS_NEW, CIMS_ADDITEMSTOCK, CIMS_GETPRESCRIPTIONWARDDETAILS* |
| Complexity | 4/5 |

**Key risk:** dual-write of stock with Inventory (`PharmacyServiceImpl.java:2140`); stock ownership boundary unresolved (10 §7 Q1).

**Dependencies:** M13 (stock), M04 (billing), M09 (nursing orders), M08 (services), M30 (BCMA), M01.

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md · tests.md
