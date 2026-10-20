# UNKNOWN-BEHAVIOR.md — Register of Unresolved Behaviors

> Every unresolved behavior observed during discovery, with evidence. Template: `## UNKNOWN-XXX / Evidence / Observed behavior / Missing information / Potential impact / How to verify`. Research-only; verification methods must not modify code.

---

## UNKNOWN-001 — VisitStatus initial value
- **Evidence**: T_Encounter/T_Appointment VisitStatus written only via guarded updates (3/4/6/9/10/11/12, all `< N` guards, e.g. `EmergencyAndWalkinVisitServiceImpl.java:2017,2270,2146`; `QuetionnaireServiceImpl.java:107,109`; `AddPaymentServiceHelper.java:477,820`; `MedicationServiceImpl.java:812`). No `setVisitStatus(` call on entities in services (verified 2026-08-14 — only DTO setters). Only `visitstatus2` string col is defaulted (`"2"`/`"9"` — `EmergencyAndWalkinVisitServiceImpl.java:1039,1141`).
- **Observed behavior**: initial value of `T_Encounter.VisitStatus` and `T_Appointment.VisitStatus` comes from the DB column default; never set by Java at creation.
- **Missing information**: the actual DB column default in Oracle (both tables), and whether any path writes 0/1/2.
- **Potential impact**: state-machine init wrong in target → kiosk/queue misreads; monotonic guards assume a baseline `< 3`.
- **How to verify**: `SELECT data_default FROM all_tab_cols WHERE table_name IN ('T_ENCOUNTER','T_APPOINTMENT') AND column_name='VISITSTATUS';` plus insert-without-status probe.

---

## UNKNOWN-002 — Visit lifecycle split-brain semantics
- **Evidence**: `AddPaymentServiceHelper.java:477` updates `T_Appointment`, `EmergencyAndWalkinVisitServiceImpl.java:2017` updates `T_Encounter` — same codes, two tables; `getLastVisitStatus` reads T_Appointment only (`EmergencyAndWalkinVisitServiceImpl.java:2275-2279`); agent-06 R1.
- **Observed behavior**: codes 3/9/10 written to T_Encounter; 4/6/11/12 written to T_Appointment; kiosk reads appointment side.
- **Missing information**: which code sequence a single visit actually experiences across both tables; whether T_Encounter.VisitStatus is read anywhere important (e.g. BHT, queue), and whether any UI relies on encounter-side values.
- **Potential impact**: target unifying into one table must preserve both read paths; wrong merge breaks kiosk queues or checkout logic.
- **How to verify**: query a real QA visit's rows in both tables across its lifecycle; grep all reads of `VisitStatus` per table; browser-automate a full visit (walk-in→bill→dispense) recording both columns at each step.

---

## UNKNOWN-003 — DNR_REJECTED == DNR_DONATED == 5 collision
- **Evidence**: `BloodBankConstants.java:8-17` — DNR_REJECTED=5 and DNR_DONATED=5 (verified in 06-catalog A8); agent-06 R3.
- **Observed behavior**: single literal 5L used for two meanings in donor workflow (also DNR_UNFIT==DNR_VOLUNTORY==3).
- **Missing information**: which code paths treat 5 as rejected vs donated; how reports/audit disambiguate; whether any DB row has meaning 5 donated that would be misread as rejected.
- **Potential impact**: donor status misreporting; cancelled donors vs successful donations confused in ERaktkosh sync and reports.
- **How to verify**: enumerate callers of both constants; correlate with T_DONOR rows (donation records vs rejection records); audit report data.

---

## UNKNOWN-004 — orderValidationPeriod=72 meaning
- **Evidence**: `ApplicationJobs.java:147-148` `@Value("${ordervalidationperiod}")`; `application.properties:732` = 72; `artifacts/risl/application.properties:343` = 24 and `:457` = 72 (duplicate key!). Used in `ApplicationJobs.java:741,743` as `requesteddate + (1/24*N) <= sysdate`.
- **Observed behavior**: requisition expiry compares `requesteddate + N hours ≤ sysdate`; QA config 72h; a second risl config value 24h exists in the same file (later key wins).
- **Missing information**: intended value per environment; duplicate-key resolution; whether 72h matches clinical policy for blood orders.
- **Potential impact**: wrong expiry window expires valid requisitions (status→10) or keeps stale ones live.
- **How to verify**: confirm effective property per deployment (`getProperty` load order); BR-BLD-004 boundary test with deployed value; clinical review.

---

## UNKNOWN-005 — 222 unmatched frontend URLs
- **Evidence**: reconciliation-notes.md:23; full list `_discovery/recon-unmatched-urls.txt` (7,160 bytes, dated 2026-08-14); examples `diagosismaster`, `referraltypemastermaster`, `proceduretypemaster//{}`, `expiryitem//getgrnnoforrettosupplier`, `billReceiptdownloadFileOpen`.
- **Observed behavior**: 222 of 3,867 FE URLs match neither CXF endpoints nor the generic CRUD filter; spot-checks show some exist in source but were missed by the parser (e.g. `NursingServiceImpl.java:8150 getUserWardList`); some are typos that may still resolve via CRUD filter or may 404.
- **Missing information**: per-URL truth (live-but-unparsed / CRUD-resolved / typo-dead / genuinely missing).
- **Potential impact**: dropping a live URL breaks a screen silently; keeping a dead URL preserves a wrong contract.
- **How to verify**: triage file item-by-item (grep source, check CRUD filter entity resolution, runtime probe each URL against QA).

---

## UNKNOWN-006 — ext.js injection mechanism
- **Evidence**: `index.html:101` references `ext.js`; no file in `public/` (verified). `IntegrationOperationsFilter.java` serves `/ext.js` from cache key `EXT_JS_CONTENT`, defaulting to `var EXT_DATA=undefined;` when unset (verified). `MasterCachedServiceImpl.java:10236-10243` `POST /master/addExtJs` sets it — **only when user == `test.user.internal.001`** (the daily backdoor account).
- **Observed behavior**: JS is injected at runtime into every page via cache; default is inert.
- **Missing information**: who calls `/master/addExtJs` in production, with what content (per-hospital overrides?); what EXT_DATA does in the UI.
- **Potential impact**: hidden per-deployment behavior; the injection depends on the test-user backdoor (security risk); React rebuild must replicate a dynamic extension mechanism or confirm none used.
- **How to verify**: capture live `/ext.js` responses per environment; audit calls to addExtJs; compare EXT_DATA usage in faug-min.js.

---

## UNKNOWN-007 — persistenceContext.xml runtime role
- **Evidence**: agent-01 §2.2 — defines `entityManagerFactory` for PU `ZDocit-HIS-SQL-Server-PU`; runtime EMF bean overridden by `App.java:173-193` (EclipseLink, ORCL); persistenceContext.xml is NOT imported by applicationContext.xml (agent-01 open question).
- **Observed behavior**: app runs against ORCL; the XML file appears unused but is shipped.
- **Missing information**: whether Spring Boot picks it up via default classpath scan (META-INF naming), and whether any env loads the SQL-Server PU.
- **Potential impact**: a second EMF could hijack datasource in some packaging; or it's pure dead weight (see DEAD-CODE §6).
- **How to verify**: startup log check for EMF count; remove-from-classpath experiment in a clone; compare packaged jar contents.

---

## UNKNOWN-008 — MemoryCacheImpl vs RedisCacheImpl active impl
- **Evidence**: `applicationContext.xml:43-45` MemoryCacheImpl active, Redis commented (:34-66); Redis keys prefixed `risl_2233` (application.properties:133); staging cluster config `10.68.180.49:7001-7006` in `artifacts/risl/application.properties`; agent-07 §2.2.
- **Observed behavior**: checked-in repo config = in-memory cache; deployment evidence implies Redis was/is intended (cluster, prefix, Kryo serialization, Jedis pool 200).
- **Missing information**: what the deployed applicationContext.xml actually contains in QA/prod.
- **Potential impact**: multi-node prod on MemoryCacheImpl = per-node divergent master data, sessions, Solr mirrors; tokens invisible across nodes.
- **How to verify**: inspect deployed `applicationContext.xml` on each node; runtime cache key probe across nodes.

---

## UNKNOWN-009 — FhirServer reachability
- **Evidence**: `App.java:259-264` `@Bean genFhirServer()` `@ConditionalOnProperty(abdm.enabled=true)` (HAPI RestfulServer R4, registers PatientResourceProvider + TokenBasedSecurityInterceptor); **no ServletRegistrationBean** found; only CXFServlet at `/api/*` (App.java:149-150); agent-04 §7, §9 #7.
- **Observed behavior**: bean created but no registered servlet mapping → FHIR surface may be unreachable, or wired externally at runtime.
- **Missing information**: `abdm.enabled` value in deployment; any external servlet container config; whether ABDM care-context FHIR bundles depend on this server.
- **Potential impact**: if live and dropped → NDHM/ABDM FHIR consumers break; if dead, extra maintenance weight.
- **How to verify**: probe `/fhir`-style base URLs on QA; check deployed servlet registrations; grep for external wiring in tomcat/context config.

---

## UNKNOWN-010 — eSign & Backman OLIS runtime wiring
- **Evidence**: eSign: `util/CAeSignService.java` (sign XML/PDF, ESP redirect) — no Java caller found (agent-07 §3.14, :294). Backman: `artifacts/Equipment Interface/Backman_Access_DxH 500/OLIS_1.0.jar` — no src references (agent-07 §3.1 #18).
- **Observed behavior**: both classes exist and are configured but have no in-repo caller.
- **Missing information**: invocation path (UI script? external system? scheduling?).
- **Potential impact**: false expectation of integration; or silently broken signing/lab-equipment flows.
- **How to verify**: deploy-time inspection (scheduled tasks, UI buttons, log grep); vendor documentation for OLIS jar wiring.

---

## UNKNOWN-011 — VisitStatus on T_Appointment column-type drift
- **Evidence**: two different guard styles in the same codebase: `VisitStatus < 11` (`AddPaymentServiceHelper.java:820`) vs `to_number(VisitStatus,'999999') < 11` (`AddPaymentServiceHelper.java:477`) — implies the column may be VARCHAR in some deployments.
- **Observed behavior**: one query defensively casts, the other does not; both succeed against the QA schema (agent-06 traces show both used).
- **Missing information**: actual column type of T_APPOINTMENT.VISITSTATUS (NUMBER vs VARCHAR2) in each environment.
- **Potential impact**: PG `to_number` on varchar works, plain comparison on varchar is lexical (`'11' < '9'` false!) — type mismatch silently changes state-machine guards after migration.
- **How to verify**: `SELECT data_type FROM all_tab_cols WHERE table_name='T_APPOINTMENT' AND column_name='VISITSTATUS';` in every environment.

---

## UNKNOWN-012 — Batch/expiry ordering inside CIMS procs — ✅ RESOLVED (2026-08-20, MIG-012b)
- **Evidence**: `cims_getitemanditembatchlistbyvisitid_new3` computes batch availability/expiry DB-side (`DTOQueries_4.java:762`; `PharmacyServiceImpl.java:2622` surfaces `ibm.expirydate`).
- **Observed behavior**: batch selection order (FEFO? nearest expiry? insertion order?) is decided inside the proc, invisible to Java.
- **Missing information**: the sort/selection algorithm and tie-breaking inside the proc; whether config (e.g. `pharmacy.batch.selection`) exists.
- **Potential impact**: wrong expiry-priority after port → dispensing older stock first; drug-safety issue.
- **How to verify**: read the proc body (it is in generated-procs); golden test batch lists before/after; compare with actual expiry dates.
- **RESOLUTION (read from `generated-procs/CIMS_GETITEMBATCHLISTFORSEARCH_NEWONE_OUTER_IMPROVED.sql` + `_FORRAISEPO.sql`)**:
  - **Algorithm = FEFO**: `Order By ExpiryDate, BatchId` (earliest expiry first; tie-break by batch id ascending). No config flag exists — the order is hard-coded in every batch-search proc.
  - **Filters**: `AvailableStock > 0` (only positive available rows), `(B.ExpiryDate IS NULL OR B.ExpiryDate > cast(sysdate as date))` (a batch expiring TODAY is already unavailable), `COALESCE(B.IsFreeze_Recall,0)=0` (recalled batches excluded), balance row = `MAX(ID)` per (store,item,batch,unit) — the append-only ledger pattern.
  - **Null expiry sorts last** in practice (Oracle default ASC puts NULLs last; preserved as `NULLS LAST` in the target).
  - **Target implementation**: `GET /api/v1/stock/available-batches?storeId&itemId` (item-service, MIG-012b) — HQL projection `findAvailableBatchesFefo` with identical filters + `ORDER BY expiryDate ASC NULLS LAST, batchId ASC`; L2 integration test pins NEAR→FAR→NOEXP ordering and expired/recalled/zero-stock exclusion; live-verified on Podman stack 2026-08-20 (IMP-DEC-029).

---

## UNKNOWN-013 — Report config source of truth (int.files.path/reports vs packaged)
- **Evidence**: `IntegrationOperationsFilter.java:118,269-278` — loads `int.files.path/reports/reports.js` (Nashorn) + all `*.json` from the same dir at startup; `application.properties:375` `int.files.path = D:/RISL/ihms/integration`; a repo copy exists at `D:\ihms\integration\reports\` (reports.js + reports.json + ~60 html, verified 2026-08-14).
- **Observed behavior**: runtime uses the external path; repo copy may drift from deployment copy; report definitions include `getServicewiseCollectionReportOld` etc.
- **Missing information**: whether the deployed `D:/RISL/ihms/integration` matches the repo `integration/` dir; how report configs are versioned.
- **Potential impact**: report changes applied to deployment are lost/unversioned; golden tests would run against the wrong config.
- **How to verify**: diff repo `integration/` vs deployment path; add a startup config-hash check.

---

## UNKNOWN-014 — Permission semantics of data-roleM (2,078 keys)
- **Evidence**: 2,078 unique `data-roleM` values (agent-08 §3.2; 08-catalog §4.2 — Configuration 1,424, BIReport 263, Inventory 70, Admission 45, ...); backend path check commented (`ServiceLayerAspect.java:421-426`); `M_ModulePermissionMaster` rows (apiMethod/apiPath) are DB data, not in repo (agent-08 §4.3).
- **Observed behavior**: keys gate UI elements only; the DB table that maps role→permission→API is absent from the repo.
- **Missing information**: the actual M_ModulePermissionMaster contents + t_role_enabledpermission rows in QA (count, apiPath coverage); whether any key lacks a corresponding ModulePermissionMaster row.
- **Potential impact**: target IAM must reproduce 2,078 keys + parent derivation; missing rows = hidden UI features; silently enforcing a partially-loaded permission table could lock out screens.
- **How to verify**: dump `M_ModulePermissionMaster` + `t_role_enabledpermission` from QA; diff keys vs paths; runtime probe of a sample of APIs against a restricted role.

---

## UNKNOWN-015 — proc usage of the ~659 unconfirmed procs
- **Evidence**: 719 artifacts, only 30 namedStoredProcQ-live + ~30 trace-evidenced (agent-05 §6.1; DEAD-CODE §2); reconciliation-notes §4.
- **Observed behavior**: the rest may be (a) called by live procs (transitive), (b) called by jobs/native SQL, (c) legacy-only.
- **Missing information**: per-proc dependency graph and usage.
- **Potential impact**: porting 659 unnecessary PL/SQL blocks inflates effort; skipping live ones breaks flows.
- **How to verify**: ALL_DEPENDENCIES on Oracle + `rg` per name + runtime proc-call monitoring (e.g. `v$sql` for `BEGIN CIMS_`).

---

## UNKNOWN-016 — T_REGISTRATION* schema & insertion semantics
- **Evidence**: no entities; native SQL only (agent-05 §5.4; reconciliation-notes §4); `CIMS_ADDPATIENTREGISTRATION` + `...DETAILS` write them (`PatientServiceHelper.java:726-729,802-804`); Solr backfill reads `T_REGISTRATION_INT.SOLR_STATUS` (agent-07 §1.2).
- **Observed behavior**: patient master lives in T_REGISTRATION family; schema known only from procs + native queries.
- **Missing information**: full DDL, constraints, the T_REGISTRATION vs T_REGISTRATION_INT split (permanent vs temp MRN?), indexes.
- **Potential impact**: wrong schema migration corrupts patient identity — the system's core.
- **How to verify**: DBMS_METADATA dump from Oracle; reverse from proc bodies + native SQL.

---

## UNKNOWN-017 — GST applicability property effect
- **Evidence**: `BaseServiceImpl.java:521-533` — `isGSTApplicable=true` AND transDate ≥ 2017-07-01; property set (`agent-06 §2`).
- **Observed behavior**: tax applied conditionally; logic inside helper used by billing.
- **Missing information**: whether any tax is truly GST-labeled vs old VAT paths (Taxmaster allowNumberOfTaxCount=2, labels SGST/CGST/IGST), and whether the 2017 boundary matters for any data.
- **Potential impact**: mislabeled tax buckets in target reports.
- **How to verify**: golden tax tests around the boundary date; report comparison.

---

## UNKNOWN-018 — "sync" role & sha256 password path
- **Evidence**: `DefaultAuthenticationImpl.java:253-255,543` — "sync" role hashes password via sha256 before the (no-op) compare; `test.user.internal.001` backdoor (agent-08 §2.4).
- **Observed behavior**: machine/sync accounts and the backdoor share special handling.
- **Missing information**: what consumes sync accounts (integration jobs?); whether the backdoor is exercised in prod.
- **Potential impact**: backdoor = full-auth bypass; sync accounts may be needed by integrations post-migration.
- **How to verify**: log analysis for `test.user.internal.001` logins; grep integration configs for sync users.

---

## UNKNOWN-019 — `Eclipselink.jpa.naming_into_indexed` effect
- **Evidence**: `App.java:188-191` sets `eclipselink.jpa.naming_into_indexed=true` (custom property); agent-05 §9 ("effect on generated SQL unknown").
- **Observed behavior**: EclipseLink generates indexed (column-position) SQL parameter binding.
- **Missing information**: whether this changes behavior vs named binding under PG or with stored-proc DTO mapping.
- **Potential impact**: subtle bind-order bugs after dialect switch.
- **How to verify**: SQL log capture pre/post; golden stored-proc result tests.

---

## UNKNOWN-020 — Multi-tier concession rounding (BR-BILL-005 CEILING vs HALF_UP)
- **Evidence**: `AddServiceOrderServiceHelper.java:383-419` — CEILING on DB-side doctor share; HALF_UP on DTO; agent-06 §1.5 #14.
- **Observed behavior**: rounding convention differs between stored-proc result and Java-side computation.
- **Missing information**: which rounding the receipt/ledger actually shows for fractional amounts; cumulative drift.
- **Potential impact**: paise-level billing mismatches between bill, receipt and doctor-share records.
- **How to verify**: golden test with 3-decimal rates; compare bill/receipt/doctor-share for the same charge set.

---

## UNKNOWN-021 — ABDM catch-all routing collisions
- **Evidence**: `ABDMController.java:1340` `POST /{versionNumber}/{endpoint}/on_{action}` catch-all coexists with ~40 explicit v2/v3 paths (agent-04 §7); dual-version paths.
- **Observed behavior**: explicit paths match first; unknown combos hit catch-all and write raw response, swallowing errors.
- **Missing information**: whether any real ABDM message has matched the catch-all in production (would indicate a route typo).
- **Potential impact**: silent accept of malformed callbacks; ambiguous behavior for target routing.
- **How to verify**: ABDM sandbox test suite replay; log scan for catch-all hits.

---

## UNKNOWN-022 — Whitelist vs dynamicAspects merge semantics
- **Evidence**: schemas use `dynamicAspects` with 9 branches (walkinVisit-schema.json — `isExtLMISEnabled`, `app_type_w`, `isNhncEmer`, ...); agent-03 §6.7: "multiple branches applying simultaneously, `propertyOrder` wins" — unverified.
- **Observed behavior**: multiple conditional branches may mutate the same field properties.
- **Missing information**: the merge algorithm in faug-min.js (last-write? priority?).
- **Potential impact**: React port must reproduce exactly which fields become required/visible per hospital config.
- **How to verify**: scripted browser tests flipping gvars; read faug-min.js merge code.

---

## UNKNOWN-023 — `saveAppErrors` / app error telemetry usage
- **Evidence**: `ClinicalServiceImpl.java` POST `/clinical/saveAppErrors` (`AppErrorDetails`) — appears in endpoint catalog (agent-04 §2).
- **Observed behavior**: client errors are POSTable to the server.
- **Missing information**: where these rows are stored/consumed; whether any ops dashboard reads them.
- **Potential impact**: dropping it loses error telemetry; keeping it unmodeled leaks an untyped sink.
- **How to verify**: grep table/entity for AppErrorDetails; DB check for the target table.

---

## UNKNOWN-024 — `configStatus.json` / `v_check` version semantics
- **Evidence**: `src/main/resources/configStatus.json` exists; frontend calls `v_check` (agent-03 §5); agent-03 §6.2 open question.
- **Observed behavior**: version-check endpoint exists; config status file shipped.
- **Missing information**: what v_check returns and how the UI uses it (cache-busting? feature gating?).
- **Potential impact**: static-asset cache invalidation in target deployment.
- **How to verify**: read v_check implementation; network capture of its usage.

---

## UNKNOWN-025 — `getUserWardList` parser gap family
- **Evidence**: `NursingServiceImpl.java:8150 getUserWardList` exists but was missed by the endpoint parser; part of the 222 unmatched URL set (reconciliation-notes.md:23).
- **Observed behavior**: at least some unmatched URLs are real endpoints missed by parsing (regex/annotation variance).
- **Missing information**: how many of the 222 are of this class (vs true typos).
- **Potential impact**: contract-test coverage gap for those endpoints.
- **How to verify**: complete UNKNOWN-005 triage; re-run parser with relaxed rules.

---

## UNKNOWN-026 — `getLastVisitStatus` semantics for queue/kiosk
- **Evidence**: `EmergencyAndWalkinVisitServiceImpl.java:2275-2279` reads latest T_Appointment.VisitStatus; agent-06 §1.2 #14.
- **Observed behavior**: queue/kiosk uses appointment-side status; values 11/12 appear as "billed"/"dispensed" signals.
- **Missing information**: how the JS interprets each numeric value (mapping table); behavior for encounters with no T_Appointment row.
- **Potential impact**: kiosk flow correctness post-unification.
- **How to verify**: browser-automate kiosk; capture JS branching on lastVisitStatus values.

---

## UNKNOWN-027 — ERaktkosh donation sync direction/semantics
- **Evidence**: `ApplicationJobs.java:1203,1381` eRaktkoshSaveDonation/updateERaktkoshDonation (weekly 23:59 Sunday, `0 59 11 * * 0`); agent-04 §4.
- **Observed behavior**: donations pushed to national blood-bank network.
- **Missing information**: conflict resolution when local data changed since last push; the DNR_REJECTED/DONATED collision's effect on pushed status (UNKNOWN-003 link).
- **Potential impact**: national registry pollution with wrong donor statuses.
- **How to verify**: golden push payload diff; eRaktkosh sandbox.

---

## UNKNOWN-028 — `allowNumberOfTaxCount` / 3-tax-bucket behavior
- **Evidence**: BR-BILL-004 — up to 3 taxes (SGST/CGST/IGST), `allowNumberOfTaxCount=2` in use; `AddServiceOrderServiceHelper.java:2767-2798`.
- **Observed behavior**: exactly 2 tax buckets active in current config though 3 exist.
- **Missing information**: which bucket mapping is used per unit; behavior when config changes to 3.
- **Potential impact**: tax display/calc drift after migration if the limit is hardcoded in the target.
- **How to verify**: BR-BILL-004 golden tests at count=2 and count=3; per-unit config dump.

---

## UNKNOWN-029 — docit.ctrl semantics
- **Evidence**: `App.java:273-286` (verified 2026-08-14): main() creates `docit.ctrl` in CWD, sleeps 5s while it exists; deleting the file triggers `context.close()` then `System.exit(0)` after 10s.
- **Observed behavior**: the file acts as a process watchman — presence = run, absence = shutdown.
- **Missing information**: who deletes it (ops script? watchdog? deploy tooling?) and whether it exists in prod.
- **Potential impact**: killing the file accidentally stops the hospital system; target must replace with standard lifecycle.
- **How to verify**: ops interview; check for deletion scripts in deploy tooling.

---

## UNKNOWN-030 — `getServicewiseCollectionReportOld` family usage
- **Evidence**: `integration/reports/reports.json` contains `getServicewiseCollectionReportOld`, `getUserwiseCollectionReportOld`, `getUserwiseSummaryReportOld` (verified); frontend has both `getServicewiseCollectionReportSchemaOld.json` and `...Schema.json`; agent-03 §2.9.
- **Observed behavior**: old and new report variants coexist; dispatcher serves whichever the config names.
- **Missing information**: which screens call the Old variants and whether outputs differ.
- **Potential impact**: report parity tests must cover both; dead variants bloat the report map.
- **How to verify**: grep fjs-config_reports.json + report-module.js for both names; diff outputs.

---

*Register maintained by the migration synthesis agent. Items are filed from all discovery agents; new unknowns should be appended with the same template.*
