# iHMS / OHUM PAS NG — Strangler Fig Migration Strategy

**Status: PRELIMINARY — synthesized from discovery (agents 01, 04, 05, 07, 09, 10 + reconciliation). No new exploration.**
**Standing decision: strangler fig is the DEFAULT.** Big-bang is only justified if a wave proves it necessary (see §7). The monolith keeps running end-to-end at every step; the oracle is never unplugged until Wave 6.

---

## 1. Why strangler, with THIS evidence

- **3,060 backend endpoints + 1,109 generic-CRUD URLs + 222 unmatched URLs** (reconciliation §1) — the API surface is enormous, partly data-driven and partly *unknown*; a big-bang rewrite would require perfect understanding of 100% of it up front.
- **719 PL/SQL artifacts, only ~30 confirmed live-called** (agent-05 §6) — a big-bang port would drag ~689 possibly-dead procs through conversion; strangler lets each module port only what is on its extraction path (agent-10 §7.3: hybrid proc strategy).
- **Master data god-node (514 edges) + mega-transaction + in-txn side effects** (Solr/HL7/ABDM/WhatsApp) — the three top decomposition risks (agent-10 §9) are *transformation* risks, not *rewrite* risks; they are best retired incrementally where each cut is observable and reversible.
- **FAUG is server-rendered** (`ServiceLayerAspect` injects `integration/scripts/api.js` into Nashorn; `ext.js` is backend-injected — reconciliation §6) — but the **REST contract is the stable interface**: the React SPA (agent-09) can consume the same `/api/rest/**` contract unchanged (agent-09 §5: "keep server contract unchanged"). Frontend-first is therefore cheap and safe; this is what makes the strangler viable at all.

---

## 2. Coexistence mechanisms (analysis per subsystem)

### 2.1 API gateway routing (TO-BE component)
- New gateway (API-first, e.g. Kong/Envoy/APISIX or Spring Cloud Gateway — **TO-BE decision**) fronts both legacy and new services at `/IHMS/api/rest/**`.
- **Routing principle: default-legacy, flip-per-module.** All traffic starts routed to the monolith; a module's slice flips to the new service only when its exit criteria pass (MIGRATION-WAVES §5).
- **Contract preservation is mandatory:**
  - extension mappings (`json|xml|* → application/json`, `PreProtocolInterceptor` forces `_type=json`) — gateway must emulate the `*`→json default and the `?ext=` behavior.
  - CXF path semantics: `/<ClassPath>/<MethodPath>` under `/api/rest` — no Spring MVC routing exists to re-learn (agent-04 §1).
  - **PHI encrypted params** (`IdEncryptionUtil.getEncryptedValue`, aspect decrypt of QueryParam/FormParam) must pass through untouched — gateway must not parse/rewrite bodies or params it cannot decrypt.
  - `X-Request-ID` idempotency header (w-api-call.js) propagated (also becomes the correlation id — OBSERVABILITY §2).
  - Context path `/IHMS`, port 8080 and `orig.api.path.prefix=/IHMS/api/rest` semantics preserved for the transition (application.properties:3-4,371).

### 2.2 Anti-corruption layer — the FOUR routing layers must be replicated
The reconciliation discovered the API surface is **four layers**, not one (reconciliation §1). The gateway/ACL must reproduce all four before any flip:

1. **Explicit CXF JAX-RS endpoints** (~3,060 from agent-04; 2,322 FE URLs matched): per-service `@Path` methods. Replicated by generating gateway route configs from the endpoint catalog + `services.xml` bean list (137 refs).
2. **Generic CRUD filter semantics** (`CrudOperationsFilter.java:76,123` → `GenericServiceImpl.crudadd/crudupdate/crudget/crudgetAll`, `@CrudApi` registry of **659 entities**, 1,109 FE URLs): this is a *data-driven API layer*, not dead links. The ACL must preserve: entity-name route pattern `{context}/api/rest/<entity>/(all|{id}|upload|…)`, `MBase.validate()` before writes, `crudaddMultiple` REQUIRES_NEW per-item batch semantics (`GenericServiceImpl.java:59-66`), cache invalidation on CRUD write (`updateCachedObject`, `CrudOperationsFilter.java:139-330`). New Reference service (Wave 1) absorbs this for master entities.
3. **Report dispatcher** (`IntegrationOperationsFilter`, Nashorn `reports.js` loaded at startup + `*.json` configs → `/api/rest/reports/<reportName>` → `ReportServiceImpl` by name → `templates/generic_reports.vm`): must be emulated (gateway route → Reporting service dispatcher) or reports break wholesale.
4. **`/ext/api` mappings** (`ApiIntegrationMapping` rewrite + `allowedUsers` ROLE_INTEGRATION gating, `IntegrationOperationsFilter.java:254,435-450`): the backend-controlled integration surface — the ACL keeps this as an explicit contract, with role gating enforced (one of the only backend authz points, reconciliation §5).

**Unmatched surface:** the 222 unresolved URLs must be dispositioned (real endpoint → route; typo → fix; dead → retire) *before* the modules they touch flip (MIGRATION-WAVES §6.11).

### 2.3 Dual-read
- On-demand reports / Jasper / Velocity / PDF-in-DB (`CommonUtilServiceImpl.savePdfToTable :6067`) read **Oracle** until the Reporting read-models exist (Wave 5); new services serve their own PG-backed read APIs.
- Master/tariff/patient lookups in new services read via the Reference REST API + Redis cache (single owner), *never* by touching Oracle directly (no cross-service DB access rule, agent-10 §2.2).
- Reconciliation: row-count + checksum comparisons per moved table run daily through the transition window (Wave exit criteria).

### 2.4 Dual-write avoidance — single-writer + event sync (recommended)
- **Rule: no table has two writers in the new world.** Every moved table gets exactly one owner; the other side reads via API/event/CDC.
- The one legacy "dual-write" that exists today — Pharmacy writing `T_ITEMSTOCK` (`PharmacyServiceImpl.java:2140`) — is resolved by *design*, not by replicating it: after Wave 5, Inventory owns stock and Pharmacy calls the stock API (decision needed: API vs per-store split, agent-10 §6.3).
- Bill settlement: `CIMS_UPDATEBILLFORCONCESSION` (payment writes bill) becomes a Billing-service API/event after Wave 4 — single writer preserved.
- Redis stays a cache/read-through layer only (never source of truth — findings §4.2); **cache invalidation is a single-owner contract** (Reference service publishes `MasterDataChanged`).
- Solr is the sanctioned exception — see §2.6.

### 2.5 CDC (Oracle → PostgreSQL) (TO-BE)
- **Initial bulk load**: per moved table, idempotent backfill preserving IDs (preserve `<table>_seq` sequences; reseed `START WITH max(id)+1`, agent-05 §8); quoted case-preserved table names (`"M_ADMISSIONTYPE"`); `TIMESTAMP` (no tz) since app writes GMT via Java (agent-05 §8).
- **Incremental**: Debezium (Oracle LogMiner) or vendor CDC (TO-BE choice) → Kafka → PG apply, with exactly-once/idempotent upsert (`INSERT … ON CONFLICT`, agent-10 §5 row 13).
- **Cutover protocol per table** (agent-10 §5): (a) freeze writes from the old side, (b) event-forward new writes (outbox), (c) backfill + CDC drain, (d) cut reads, (e) verify, (f) rollback switch armed. Critical list: `T_ENCOUNTER`, `T_BILL`, `T_ITEMSTOCK` (agent-10 §5; MIGRATION-WAVES §4.12).
- **Legacy ID continuity**: `SELECT <seq>.NEXTVAL FROM DUAL` (aspect `ServiceLayerAspect.java:725`, procs) vs PG `NEXTVAL('<seq>')` — the code already branches on dialect (agent-05 §1: 9 dual-dialect sites); external refs (MRNO, IPDNO, bill no, HL7 control IDs) must stay identical.

### 2.6 Solr continuity — index ownership + dual-write during transition
- Solr **must remain** (agent-07 §1, non-negotiable): primary patient-identity search, ICD-10/health-factor/item lookup, ABDM consent discovery (queries Solr for healthIdNumber/mobile+DOB).
- **Index ownership**: Search service (27) owns all 6 collections. Until Wave 3, legacy services continue their realtime `addBean` + flag-drained batch syncs (`SOLR_STATUS` at-least-once); new services **never** call Solr directly — they emit events consumed by Search.
- **During transition, Solr is dual-written by design** (legacy in-txn writes + event pipeline), with the rollback-compensation semantics preserved (on tx failure delete Solr doc + Redis keys — `ServiceLayerAspect.java:1164-1182`). This is the one sanctioned dual-write; it is already how the system works today (realtime + drainers can double-write; agent-07 §1.6.6).
- **Decisions before Patient moves**: encounter 3-day rolling retention (`solr.encounter.retain.days=3`, `closeDailyEncountersJob`) → archive collection instead of delete (legal retention, agent-07 §1.7); full reindex + reconciliation script; Redis `current_pat_/current_enc_` mirrors (100s TTL hiding soft-commit lag) kept or replaced by a documented read-through (agent-07 §2.3).
- Ops pattern to carry over: 3-node SolrCloud health checks; replace midnight-restart cron + rsync DR with proper replication (agent-07 §1.7) — improved, not removed.

### 2.7 Event synchronization
- **Outbox pattern on PG** (`pg_logical`/Debezium → Kafka, agent-10 §7.3) for new services; legacy side keeps its existing queue tables (`T_WHATSAPPMESSAGEQUEUE`, `T_ABDMREQUESTQUEUE`, `T_OUTBOUNDHIS`).
- **Interop runs as the integration broker early** (Wave 0 relay): forwards events between old and new sides; queue tables stay in Oracle and their pollers keep running until Wave 5 (in-flight continuity is a Wave-5 exit criterion).
- Choreographed sagas for Order→Bill→Payment (Wave 4) with compensation reusing existing cancellation procs (`CIMS_SETTLEDBILLCANCELLATION/CIMS_UNSETTLEDBILLCANCELLATION`, `BillServiceImpl.java:70-91`).

### 2.8 Shared authentication — token format must survive
- Token today: opaque bearer in cache (`session.timeout` TTL), issued by login (LDAP + Rajasthan SSO via `SSOAuthFilter`), validated per-request by `ServiceLayerAspect` against the session cache; `UNQ_SESS_USER_<userId>` ↔ token; logout clears (`DefaultAuthenticationImpl.java:847-852`).
- **Constraint: the token format is the shared contract.** New services accept the same opaque bearer; the gateway introspects (or forwards to IAM) without changing its shape, TTL, or invalidation semantics. No JWT migration mid-strangler — an opaque token survives cache-topology changes only if the cache layer is unified (resolve MemoryCacheImpl-vs-Redis divergence first — agent-07 §2.2).
- PHI-decrypt keys and `@NotSecure` bypass semantics must be re-implemented faithfully at the ACL.

---

## 3. Strangler sequence: hybrid (frontend-first globally, backend-first inside high-risk waves)

**Recommendation: hybrid.**

1. **Phase A — frontend-first, all modules:** React SPA replaces FAUG screen-by-screen, calling **legacy APIs unchanged through the gateway** (agent-09: 1:1 route mapping, 1,842-endpoint client generation, keep server contract). Evidence this is safe: the REST contract is the stable interface; the four routing layers live server-side and are preserved; `X-Request-ID`/error-envelope semantics are replicated in the client (agent-09 §5). Frontend migration is thus decoupled from backend extraction and can proceed on agent-09 §12's own sequence (platform → OP pilot → IP+billing → pharmacy+nursing → patho/rad → inventory → rest → mobile).
2. **Phase B — backend-first, per wave:** when a wave's module flips, its new service goes live **behind the (already React) UI**: gateway routes that module's slice to the new service; legacy UI for that module (if any screens remain on FAUG) also calls the new service through the gateway (new-backend-behind-old-UI). Both directions of strangler operate.
3. **Phase C — high-risk exceptions (backend-first within wave):** within Wave 4, the saga decomposition of Order→Bill→Payment is developed and shadow-tested against the legacy mega-txn *before* any billing screen flips; billing traffic stays on the monolith until saga parity is proven, then flips atomically with a rollback window.

**Justification from agent-09 (290 routes, 1:1 mapping, "map legacy screens 1:1 first, never invent features")** and **agent-04 (3,060 endpoints; generic CRUD; no controller/service separation)** — the API surface, not the code structure, is the contract to strangler around. FAUG decoupling (JSON-first, agent-10 §5) is the enabling step of Phase A.

---

## 4. Gateway routing table design (TO-BE)

Priority order (first match wins):

| Priority | Pattern (under `/IHMS/api/rest`) | Target | Notes |
|---|---|---|---|
| 1 | Exact legacy endpoint (from 3,060-catalog) for flipped module | New service | Route only after module exit criteria pass |
| 2 | `reports/<reportName>` | Report dispatcher emulation → Reporting service (Wave 5+) or legacy dispatcher | Nashorn config semantics preserved |
| 3 | `ext/api/<mapped-path>` | ApiIntegrationMapping emulation (ROLE_INTEGRATION gate) | Allowed-users check at gateway |
| 4 | `<entity>/(all|{id}|crudadd\|crudupdate\|cruddelete|…)` where entity ∈ @CrudApi registry | New Reference service (post-Wave 1) or legacy GenericServiceImpl | Keep MBase.validate + REQUIRES_NEW batch + cache-invalidation semantics on the new side |
| 5 | Module-prefix slice (e.g. `diet/*`, `linenLaundry/*`) | New service | Post-flip default for that module |
| 6 | Everything else | **Legacy monolith** (default) | Until Wave 6 |

Additional rules: token passthrough unchanged · `X-Request-ID` → trace propagation · PHI params not parsed · extension default `*`→json · 222 unmatched URLs triaged into priority 1 (real) or retired · routing changes are configuration, gated by feature flags, with per-route rollback one command away.

---

## 5. Data migration phases (per wave, per module)

| Phase | Activity | Evidence/anchor |
|---|---|---|
| P0 Baseline | Schema dump from Oracle (incl. unmapped tables: `T_REGISTRATION*`, aasmaan names), row counts, sequence `LAST_NUMBER`s, column case audit | agent-05 §3.6, §4, §8 |
| P1 Initial bulk | Idempotent backfill to PG; quoted case-preserved tables; preserve `<table>_seq` + reseed; CLOB→TEXT; NUMBER(19)→BIGINT; boolean/char flag audit | agent-05 §8; agent-10 §5 |
| P2 Incremental | CDC Oracle→PG (Debezium/LogMiner); idempotent ON CONFLICT apply; lag monitoring | agent-10 §5 |
| P3 Cutover | Freeze writes → drain CDC → flip writes (outbox) → cut reads → verify (row counts, checksums, spot business transactions) | agent-10 §5; MIGRATION-WAVES §4.12 |
| P4 Post-cutover | Dual-read comparison window (5 business days per Wave 2 precedent); drift reports; sequence continuity check; arm/disarm rollback | Wave exit criteria |
| Global | Migration runs per module inside its wave; `T_ENCOUNTER/T_BILL/T_ITEMSTOCK` get the strictest P3 execution (only tables where a lost update reaches patients) | agent-10 §5 |

---

## 6. Rollback strategy

1. **Per-module rollback (default):** gateway flip-back to legacy (route config change, seconds); new-service writes to its PG schema paused/stopped; event consumers paused; PG tables re-synced from Oracle where needed. Because each module owns its schema, rollback scope is one schema, not the system.
2. **Rollback triggers:** data divergence beyond threshold in dual-read comparison · module SLO breach (e.g., patient search P95, registration latency) · clinical incident attributable to the migration · saga compensation failure in Wave 4 · integration message loss in Wave 5.
3. **Rehearsal:** every wave runs a scripted rollback rehearsal before the next wave begins (Wave-2 exit criterion).
4. **Global rollback** exists only until Wave 2 completes; afterwards the system is *decomposable*, not *revertible-as-a-whole* — irreversible steps (sequence reseed, `T_BILL` cutover, Oracle read-only in Wave 6) are each gated by long observation windows.
5. **Rollback hygiene:** no dual-writers at any moment (a flip is atomic); rollback never creates a window where both old and new write the same table.

---

## 7. Risks of strangler for THIS monolith

| Risk | Evidence | Mitigation |
|---|---|---|
| **Shared tables** (`T_ENCOUNTER`, `T_ADMISSION`, `T_BILL`, `T_ITEMSTOCK`, `T_PATIENT*`, `M_SERVICEMASTER/TARIFF`) written/read across boundaries | agent-10 §3 (written-by/read-by matrix) | Single-owner + API/event reads; strict P3 protocol; read-model copies for consumers |
| **719 procs, mega-procs span future boundaries** (`CIMS_ADDBILL`, `CIMS_ADDPATHORDERBOOKING` reach across 4+ future services) | agent-10 §2.6; `AddServiceOrderServiceHelper.java:341-516` | Convert only extraction-path procs to service logic with same tx boundaries, then PG (agent-10 §5); keep others on Oracle until their module moves; triage ~689 unused as dead-code candidates |
| **Solr sync inside transactions** — rollback compensation exists today (`ServiceLayerAspect.java:1164-1182`); a new event pipeline must replicate delete/compensate semantics or index drift is permanent | agent-07 §1.6, §1.3 | Search owns ingestion; event consumers replay + reconciliation jobs (existing `indexMissingPatientsInSolr` pattern extended to PG) |
| **Session/cache semantics**: per-node MemoryCacheImpl divergence, token TTLs, QMS queue state, 60s tx locks (`CDD_`, `INV_RI_`), job kill-switches in Redis | agent-07 §2.1, §2.2 | Unify cache topology before split; never split Redis-backed live state across two services (Queue & Kiosk rule); move kill-switches to config/feature flags |
| **Unmapped schema**: `T_REGISTRATION*` truth only in Oracle; aasmaan convention-named tables | reconciliation §4; agent-05 §3.4 | P0 baseline includes DB-side schema dump; add explicit `@Table`/DDL before backfill |
| **In-flight async messages** (WhatsApp 15-min poller, ABDM 5-s poller, HL7 outbox, eAushadhi ack) must not lose messages across cutover | agent-07 §3.2 | Queue tables stay legacy until Wave 5; relay forwards; idempotent senders; max-trigger-5 counters preserved |
| **Encounter index 3-day retention** = record-loss window during transition | agent-07 §1.6.2 | Archive collection decision in Wave 1 |
| **VisitStatus split-brain** (`T_Appointment` vs `T_Encounter`, codes 11/12) — a strangler split could freeze the mismatch in place | agent-06 §C.1 | Unify visit lifecycle in Wave 3a before downstream consumers move |
| **Hidden routing layers + 222 unmatched URLs** — silent breakage if ACL incomplete | reconciliation §1 | ACL must replicate all four layers; triage list resolved per module flip |
| **PHI encryption + auth woven into aspect** — any gateway rewrite breaks decryption or authz | agent-04 §8 | Pass-through of encrypted params; token format frozen (§2.8) |
| **Sequence continuity** (bill no, MRNO, IPDNO, HL7 control IDs) broken by split ID assignment | agent-05 §4; `HL7Helper.java:246` | Preserve sequences; reseed to max+1; dual-dialect NEXTVAL already in code |
| **FAUG server-render dependency** (`ext.js` injected, Nashorn api.js) — a frontend-first phase must not assume pure-static frontend | reconciliation §6; agent-10 §1.1 | Phase A keeps backend contract byte-identical; FAUG HTML rendering retires per screen (agent-10 §5) |
| **Big-bang temptation**: if a wave's parity harness fails repeatedly, the reflex is "rewrite everything" — the evidence (only 30 live procs, 3,060 endpoints, 222 unknowns) says that multiplies risk | agent-05 §6; reconciliation | Big-bang only after a written justification showing the strangler variant is unworkable for that module (§1) |

---

## 8. Open decisions that shape the strangler

1. Stock ownership (API vs per-store split) — gates Pharmacy/Inventory flip (agent-10 §6.3).
2. Cache topology (Redis standalone vs cluster vs per-node memory) — gates token/queue continuity (agent-07 §2.3).
3. Encounter/BILL partitioning + retention on PG — gates P0/P3 for the critical cutover list (agent-10 §7.2).
4. Oracle coexistence duration + hybrid-read reporting (state keeps Oracle warehouse) — shapes Wave 6 (agent-10 §7.5).
5. Outbox plumbing choice (pg_logical vs Debezium) and CDC vendor — Wave 0 (TO-BE).
6. Audit stream (T_AUDITEVENT) placement — rides Wave 0 (OBSERVABILITY §5).
