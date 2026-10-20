# Cross-Agent Reconciliation — Phase 2

> Generated: 2026-08-14. Reconciliation between Agent 03 (frontend URL map), Agent 04 (backend endpoint catalog), Agent 05 (database), and routing infrastructure.

## 1. Frontend ↔ Backend URL Reconciliation (systematic)

Method: All 3,867 unique frontend-called URLs (agent-03 `api-endpoints-normalized.txt`) were normalized (prefix-stripped, lowercased, path-params wildcarded) and matched against all 3,060 backend endpoints from agent-04's catalog (same normalization). Results:

| Stage | Count |
|---|---|
| FE unique URLs (raw) | 3,867 |
| After noise removal (external URLs, schema artifacts) | 3,651 |
| Matched to explicit backend @Path endpoints | 2,322 |
| **Matched to generic CRUD filter** (entity-name routes) | **1,109** |
| **Unresolved — require human verification** | **222** |

**Critical discovery — THREE hidden routing layers beyond CXF JAX-RS:**

1. **Generic CRUD Filter** — `src/main/java/com/ohum/interceptors/CrudOperationsFilter.java:76` (`Filter`, pattern `{context}/api/rest/([^/]+)(.*)` at line 123). Intercepts every `/api/rest/<entity>/...` request and routes to `GenericServiceImpl.crudadd / crudupdate / crudget / crudgetAll` (lines 372-558). Entity registry: classes annotated `@CrudApi` — **659 entities** in `com.ohum.models` (init at lines 125-136). This explains 1,109 frontend URLs like `accidenttype/all`, `bedmaster/upload`, `citymaster/{id}` — all master CRUD screens render through ONE generic endpoint family. **These are not dead links; they are a data-driven API layer.**
2. **Report Dispatcher** — `src/main/java/com/ohum/aspects/IntegrationOperationsFilter.java` (Filter, `@Order(HIGHEST_PRECEDENCE-2)`). Loads `int.files.path/reports/reports.js` (Nashorn JS) + `*.json` report configs at startup (lines 269-278) into `reportMap`, and serves `/IHMS/api/rest/reports/<reportName>` dynamically (lines 469-571), dispatching to `ReportServiceImpl` methods by name and rendering via Velocity templates (`templates/generic_reports.vm`). Explains all `reports/getXxxReport` frontend URLs.
3. **Integration API Mapping** — same filter rewrites `/ext/api/<mapped-path>` → internal `/api/rest/...` URL via `ApiIntegrationMapping` config (`mappingMap`, lines 254, 435-450) with `allowedUsers` (ROLE_INTEGRATION) gating.

**Of the 222 unresolved URLs:** spot-checks show the backend source DOES contain some of them (e.g. `NursingServiceImpl.java:8150 getUserWardList` — missed by agent-04's parser), so most are parser gaps or variant spellings; a small number are frontend typos (e.g. `diagosismaster`, `referraltypemastermaster`, `proceduretypemaster//{}`, `expiryitem//getgrnnoforrettosupplier`, `billReceiptdownloadFileOpen`) that may still resolve through the CRUD filter or may be dead. Full list preserved at `_discovery/recon-unmatched-urls.txt`. **These 222 must be individually triaged before migration; they are logged in UNKNOWN-BEHAVIOR.md and RISK-REGISTER.md.**

## 2. Module Inventory Reconciliation (Agent 02 vs 09 vs 10)

All three agents independently derived 40 frontend module configs (fjs-config_*.json) — consistent. Agent 02's 40-module catalog (M01-M40) aligns with agent 09's screen mapping and agent 10's service candidates. Discrepancies: agent 10 proposed 29 services (correctly consolidating), agent 02 lists 40 modules (frontend granularity). Final MODULE-INVENTORY uses the 40-module granularity with service ownership mapping per agent 10.

## 3. Status Code Reconciliation (Agent 06)

Status dictionary built from constants + setter evidence: VisitStatus, T_Bill, Charges, Admission, Bed, Payment, Blood requisition, Donor, GST. **Unresolved numeric meanings flagged** (e.g. DNR_REJECTED==DNR_DONATED==5; VisitStatus initial value) — recorded in UNKNOWN-BEHAVIOR.md.

## 4. Database Reconciliation (Agent 05 + 04)

- 843 tables (808 explicit @Table) / ~898 entities; 659 @CrudApi entities = the CRUD-API surface.
- 719 PL/SQL artifacts in generated-procs (692 procs + 17 functions + 11 packages, `CIMS_*`), only ~30 actively called from Java (namedStoredProcQ) — the rest are UNKNOWN usage status (candidate dead code — see DEAD-CODE-CANDIDATES.md).
- `T_REGISTRATION`/`T_REGISTRATION_DETAILS`/`T_REGISTRATION_INT` have NO entity mapping (schema only in DB) — critical migration risk.
- Core patient/encounter/bill tables written by many services (T_ENCOUNTER, T_ADMISSION, T_BILL/T_BILLDETAILS, M_ITEMMASTER+T_ITEMSTOCK+batch) — shared-table hotspots for service-boundary design.

## 5. Security Reconciliation (Agent 08 cross-check)

- Agent 08's "frontend-only RBAC" finding cross-checks with agent 04's "auth is aspect-based" (ServiceLayerAspect) — consistent. The `IntegrationOperationsFilter`'s allowedUsers gating (ROLE_INTEGRATION) is one of the only backend authorization points.
- `/ext/api` mapping is the backend-controlled integration surface (vs frontend-gated business APIs).

## 6. Cross-cutting facts confirmed by ≥2 agents

| Fact | Agents |
|---|---|
| Solr: 6 collections, 5 live, sync indexing inside DB transactions | 07, 10 |
| Redis: risl_2233 prefix; memory vs redis impl ambiguity | 07, 04 |
| CXF single REST bus `/rest` with 132-137 beans; no MVC controllers | 01, 04 |
| EclipseLink (not Hibernate); EMF programmatic; PG dialect branch exists but ~1% coverage | 01, 05 |
| Master god-node MasterCachedServiceImpl (~12.7k LOC, 264 endpoints) | 04, 10 |
| Order→Bill→Lab→Rad→DoctorShare→EMR→HL7 mega-transaction | 06, 10 |
| Encounter Solr 3-day rolling window | 07, 06 |
| ext.js referenced by index.html but ABSENT from repo (backend-injected) | 03, 01 |
| services.xml has 134 bean refs; 11 @Component service beans NOT wired into CXF | 04, 01 |

## 7. High-severity unresolved items (→ DISCOVERY-GAPS.md)

1. 222 unmatched frontend URLs (triage list saved)
2. FhirServer (HAPI R4) bean reachability — no servlet registration found (agent 04)
3. 719 generated procs: which are used vs legacy (only 30 confirmed live)
4. `T_REGISTRATION*` schema truth (no entities)
5. Cache implementation: MemoryCacheImpl vs RedisCacheImpl active (applicationContext.xml comment state)
6. `persistenceContext.xml` runtime role (not imported by App.java per agent 01)
7. ext.js injection mechanism
8. VisitStatus initial value + visit lifecycle split-brain (T_Appointment vs T_Encounter)
