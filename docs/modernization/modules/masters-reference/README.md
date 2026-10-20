# Module: masters-reference (M05 — Tariff/Packages/Plans + M39 — Commons/Reference Platform)

> Master reference: `../../MODULE-INVENTORY.md` §M05 & §M39; catalog: `../../02-business-module-catalog.md` §M05/§M39; backend: `../../04-backend-architecture.md` §4 (caching); security: `../../08-security-rbac-audit.md` §3.3.

**Target service (10 §2.1):** 21. Reference Master Data — owns all M_* masters (609 entities), cache keys, tariffs/packages/plans/services; emits `MasterDataChanged`, `TariffChanged`, `PackageChanged`.

**One-line purpose:** Pricing & eligibility backbone (M05) + platform-level shared services: masters CRUD, audits, config, shifts, MRN series, generic CRUD filter, report dispatcher (M39).

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_masters.json` (330 keys, 24 subdomains); `fjs-config_commons.json` (67 keys) |
| Services | `/tariff`, `/servicemaster`, `/planmaster`, `/packageservice`, `/concessiontemplate`, `/companyapprovedamount`, `/addServiceMaster`, `/master` (264 endpoints), `/masterConfigService`, generic CRUD filter (659 @CrudApi entities) |
| Cache | **Owner of ~375 `@CrudCached` master keys** (MemoryCacheImpl active; Redis impls commented out — deployment risk, 07 §2.2) |
| Complexity | M05: 3/5 · M39: 3/5 |

**Dependencies:** M04 (billing), M01 (patient categories), M07 (pharmacy pricing), M32 (RGHS), all modules (platform).

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md
