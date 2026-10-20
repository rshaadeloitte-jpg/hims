# Assumption Register — iHMS Modernization Research

> Assumptions in active use, with status. Rule: an assumption may become a fact only with two-source evidence; otherwise it stays flagged and gated.

## A01 — Prior discovery docs are accurate
- **Statement**: The 24-document prior research set (blueprint, catalogs, matrices) accurately reflects `D:\ihms`.
- **Status**: PARTIALLY VALIDATED. V1–V6 confirmed the load-bearing claims with the corrections recorded in contradiction-register. Remaining sections (feature matrix rows, per-module docs) not re-audited line-by-line — treat as HIGH CONFIDENCE, not CONFIRMED.
- **Risk if wrong**: wrong module/service boundaries or missing features.

## A02 — 40-module catalog == complete business surface
- **Statement**: fjs-config/module-dir enumeration yields the complete module set.
- **Status**: HIGH CONFIDENCE (2 sources: 42 configs + 40 module dirs; root fjs-config.json lists 40 — nhcx, rcm missing from the configs array though present on disk — minor inconsistency).
- **Risk**: any module not mapped (e.g. dashboard-only screens) could be missed.

## A03 — "843 tables" from prior research
- **Status**: UNVERIFIABLE from repo (no ALL_TABLES dump). Best proxy: 894 distinct `@Table` names. Do not use 843 as a contractual number; reconcile at W0 data-extraction time (UNKNOWN-031).

## A04 — ~30 live stored procedures (prior claim)
- **Status**: DISPROVEN. 918 unique proc names referenced from Java (933 occurrences); 1,343 namedStoredProcQ call sites. Now superseded by E-08.
- **Impact**: PG port scope much larger; per-proc classification needed before port (DEAD-CODE-CANDIDATES must be re-derived from DTOQueries files).

## A05 — 3,060 REST endpoints
- **Status**: CORRECTED. Not reproducible as a single sum; closest readings: 3,167 `@Path(`, 3,041 method-level verb annotations, 2,322 explicit + 1,109 CRUD-filter URL matches. Use "≈3,000 explicit-verb endpoints + 1,109 generic-CRUD routes" going forward.

## A06 — 533 frontend routes
- **Status**: CORRECTED to 636 (635 unique) in `fjs-config.json` router.routes. Case-variant duplicate `pharmacy:ipReturnFlow`/`pharmacy:iPReturnFlow` is a real data defect to fix in target router.

## A07 — Frontend is a single-page schema-driven jQuery app
- **Status**: CONFIRMED, with deltas (E-15): custom template language (not mustache), two schema dialects, backend-injected ext.js, dead faug-ext.js, QZ Tray printing, client-side billing arithmetic.

## A08 — Cache implementation = Redis in production
- **Status**: UNVERIFIED for production. Repo defaults to MemoryCacheImpl (`applicationContext.xml:43`). Deployed config may differ — must verify at W0 (UNKNOWN-008).

## A09 — HL7 inbound is live
- **Status**: LIKELY INACTIVE — `hl7inbound.pickup.freq` defined but `@Scheduled` commented out (`Hl7InboundMessageJob.java:51`); `hl7.billingOrder.messages.enabled=false`. Treat as dormant until verified (UNKNOWN-010).

## A10 — 29 candidate services are the target boundary set
- **Status**: PRELIMINARY (design). Boundary soundness re-checked against V6 writer maps: hotspot tables (T_ENCOUNTER, T_BILL family, M_ITEMMASTER/T_ITEMSTOCK) confirmed as cross-cutting; owner assignments still provisional. Do not commit until W2 validation (see HMIS-MODERNIZATION-MASTER-PLAN §18).

## A11 — PostgreSQL per-service schemas with no shared tables
- **Status**: PRELIMINARY (design). Works only if ownership maps resolve every hotspot; V6 writer maps now available as the working input.

## A12 — Kafka needed for the modernization
- **Status**: CONDITIONAL. KAFKA-EVENT-CATALOG lists 12 accepted events; use only for outbox-fed synchronization (patient identity, encounters, bills, stock, notification). No Kafka for request/response. Validate each event against E-08/E-10 transaction evidence at design time.

## A13 — Redis needed in target
- **Status**: CONDITIONAL. Use cases: session/token cache (replacing MemoryCacheImpl), Solr-independent quick lookups (e.g. masters, unit config), idempotency keys for /ext/api. NOT primary storage. Re-validate at W0 (current cache semantics are implementation-dependent per A08).

## A14 — Solr remains
- **Status**: CONFIRMED (E-16). Non-negotiable per program mandate and evidence (indexing inside DB transactions, search embedded in registration/admission flows).

## A15 — React schema-driven engine can cover most FAUG screens
- **Status**: PRELIMINARY (design; 09-react-migration-design). Supported by V4 (two schema dialects are structurally uniform) but requires the 636-route + 1,308-schema archetype proof in W1 (see EXECUTION-BACKLOG W1-02).

## A16 — Migrated services keep legacy API contracts (gateway-fronted strangler)
- **Status**: PRELIMINARY design; endorsed by V1 (5 routing surfaces identified to replicate) and V4 (frontend calls 3,867 URLs).

## A17 — No DB triggers exist
- **Status**: CONFIRMED (V3: 0 CREATE TRIGGER/INDEX/VIEW in repo SQL). BUT: live Oracle DB may hold DDL not present in repo — verify against ALL_TRIGGERS/ALL_INDEXES at W0 (UNKNOWN-032).

## A18 — Test user / seeded data in DB
- **Status**: CONFIRMED (test.user.internal.001 backdoor per E-07). Target must not replicate backdoors.

## A19 — "~1% PostgreSQL dialect coverage"
- **Status**: QUALITATIVE. Only real branch: `App.java:180-185` else→POSTGRESQL. No PG-specific SQL found in queries. Treat as near-zero, not a feature.

## A20 — Timezone/locale semantics
- **Status**: NOT ANALYZED. `D:\ihms\timezones` (9,549 B) and CLDR locales (175 files) exist; server timezone handling undocumented. Add to W0 data-extraction checklist (UNKNOWN-033).