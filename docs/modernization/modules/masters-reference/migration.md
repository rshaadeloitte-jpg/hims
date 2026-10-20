# M05/M39 — Migration

**Target service:** 21. Reference Master Data — **Wave 0 (Foundation)** (10 §6); M39 commons distributed across Reference/IAM/cross-cutting.

**Complexity:** M05 3/5 · M39 3/5 — huge but mechanical CRUD; god-node cache decomposition; 659 @CrudApi entities through generic filter.

**Steps:**
1. **Wave 0 priority:** master/cache contract live; ~375 cache keys get a single owner (Reference) with `MasterDataChanged`/`TariffChanged`/`PackageChanged` events for invalidation (10 §2.2).
2. **Cache topology decision (07 §2.3):** MemoryCacheImpl vs Redis cluster ambiguity must be resolved; replace ~375 reflective `@CrudCached` with declared cache (Caffeine local + Redis pub/sub invalidation); Kryo serialization → JSON typed structures.
3. **Generic CRUD filter → generated CRUD service:** 659 `@CrudApi` entities + 1,109 frontend URLs depend on it (recon §1); rebuild as data-driven CRUD API with schema metadata.
4. **Report dispatcher:** Nashorn `reports.js` removed in JDK 15+ — rebuild as declarative report config + rendering service (M25).
5. **Oracle→PG:** 582 M_* tables 1:1, quoted case preserved, `<t>_seq` sequences; `MERGE INTO` (T_FREQ_ITEMS) → INSERT…ON CONFLICT (05 §8).
6. **Orphan cleanup:** Solr `service` collection (no indexer) — drop or wire.
7. **Sequences/auto-code** (`auto_code_seq`, CIMS_ADDSERVICEMASTER2/CIMS_ADDTARIFFSERVICE2) preserved for external references.
8. **Dual-dialect branches** already exist (9 sites) — extend PG coverage (05 §1).

**Exit criteria (Wave 0):** master/cache contract live; JWT auth; event backbone; FAUG→JSON for first screens.
