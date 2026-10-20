# Contradiction Register — iHMS Modernization Research

> Format: CONTR-### | Agent A finding vs Agent B finding | Evidence | Resolution | Confidence | Migration impact.
> Entries CONTR-001…010 seeded from prior cross-agent reconciliation (reconciliation-notes.md). CONTR-011+ are the V1–V6 independent-validation corrections (2026-08-14).

## CONTR-001 — Stored-proc usage: ~30 live vs pervasive
- Prior research: "719 procs, only ~30 confirmed live" (RISK #3, agent-05).
- Validation (V3/V6): 918 unique proc names referenced from Java (933 occurrences in DTOQueries_1..5); 1,343 namedStoredProcQ() call sites; direct call `CIMS_GETcounter` (AddServiceOrderServiceHelper.java:3627).
- Resolution: prior claim derived from namedStoredProcQ annotation scan only; procedureName-string references (EntityManager metadata registry) were missed. **Superseded**: proc usage is pervasive.
- Confidence: CONFIRMED. Impact: PG migration effort and golden-test scope grow substantially; DEAD-CODE-CANDIDATES must be rebuilt from the DTOQueries lists; port order should follow call-site density (InventoryServiceImpl 209 → PathoOrderServiceImpl 74 → OTSchedulerServiceImpl 56 → …).

## CONTR-002 — Generated procs contain packages
- Prior: "packages CIMS_*" among the 719.
- V3: 702 CREATE OR REPLACE PROCEDURE + 16 FUNCTION + 1 mixed; ZERO PACKAGE statements.
- Resolution: corrected — no packages.
- Confidence: CONFIRMED. Impact: plpgsql conversion is per-proc/function, no package wrapping.

## CONTR-003 — Entity/table counts
- Prior: ~898 entities / 843 tables.
- V3: 940 @Entity in models (+109 in dto ⇒ 1,049); 894 distinct @Table names; 843 UNVERIFIABLE (no DDL dump in repo).
- Resolution: use 940/1,049 entities and 894 distinct @Table names; 843 retired as an unverifiable figure.
- Confidence: CONFIRMED. Impact: data-migration work-package sizing uses 894 tables (contract at W0).

## CONTR-004 — Frontend route count
- Prior: 533 routes.
- V4: 636 routes (635 unique) in fjs-config.json router.routes; route parsing methodology in prior round under-counted (aliases/duplicates).
- Resolution: corrected to 636.
- Confidence: CONFIRMED. Impact: React route inventory + permission map must cover 636 routes; the duplicate case-variant pair must be resolved in the router design.

## CONTR-005 — Endpoint count 3,060
- Prior: 3,060 REST endpoints.
- V1: raw `@Path(` = 3,167; method-level verb annotations = 3,041; frontend-matched = 2,322 explicit + 1,109 CRUD-filter; 222 unmatched (triage pending, UNKNOWN-005).
- Resolution: no single method reproduces 3,060; adopt "≈3,000 explicit verbs + 1,109 generic CRUD routes + report-dispatcher dynamic set". MasterCachedServiceImpl endpoints 270 (not 264); NursingServiceImpl 17,350 LOC (not 8k+); UploadFileServiceImpl 19,941 LOC is the largest service (not MasterCached).
- Confidence: CONFIRMED. Impact: API contract inventory is layered; generic CRUD layer must be captured as a contract family (659 entities) rather than per-endpoint.

## CONTR-006 — CXF-only REST (no Spring MVC)
- Prior: no MVC controllers.
- V1: 2 exceptions — ErrorPathHandler `/error` (returns 401) and InventoryEAushadiIntegrationImpl (dual @Path+@RestController). FHIR RestfulServer adds a 4th REST surface; ABDM/HFR controllers ride the CXF bus.
- Resolution: "CXF dominates" holds; must replicate `/error` 401 behavior + FHIR R4 surface (UNKNOWN-009 reachability) + ABDM callback routes.
- Confidence: CONFIRMED. Impact: target must keep an error-path contract and FHIR endpoints.

## CONTR-007 — Unwired service beans
- Prior: 11 @Component beans not in services.xml.
- V1: 17 measured; only 1 is JAX-RS-annotated (ActionNotificationServiceImpl, orphaned). Counting rule explained the delta.
- Resolution: 17; only ActionNotificationServiceImpl is a dead REST surface.
- Confidence: CONFIRMED. Impact: negligible for migration; dead-surface list updated in DEAD-CODE-CANDIDATES.

## CONTR-008 — Active cache implementation
- Prior (UNKNOWN-008): Memory vs Redis impl ambiguous.
- V5: applicationContext.xml:43 activates MemoryCacheImpl; Redis variants commented out; jedis pool 200/200/100 only relevant if Redis enabled.
- Resolution: repo default = in-memory; production deployment still to verify (keep UNKNOWN-008 open for live env).
- Confidence: HIGH (repo), UNKNOWN (deployed). Impact: target Redis session/token cache design must not assume current Redis usage.

## CONTR-009 — HL7 active vs dormant
- Prior: HL7 inbound pickup configured.
- V5/V6: cron defined but @Scheduled commented out; hl7.billingOrder.messages.enabled=false; HL7 legs in mega-transaction commented out.
- Resolution: dormant/disabled by configuration. Verify production state at W0 (UNKNOWN-010).
- Confidence: HIGH. Impact: HL7 EMR bridge must be designed (target has HL7 anyway) but is not on the critical path.

## CONTR-010 — VisitStatus constant location
- Prior: status dictionaries from constants.
- V6: no VisitStatus constants class; magic numbers in native SQL across T_APPOINTMENT (11/12/4/6) and T_ENCOUNTER (3/10/9); T_APPOINTMENT entity maps no VisitStatus column.
- Resolution: status meanings remain UNKNOWN for several values (UNKNOWN-001/002/003); dictionary locked from STATUS-CODE-DICTIONARY + live-DB defaults at W0.
- Confidence: CONFIRMED (absence of constants), UNKNOWN (meanings). Impact: contracts must carry status enums; never port magic numbers blindly.

## CONTR-011 — "Solr writes inside DB transaction" nuance
- Prior: indexing in transactional flows.
- V5: confirmed for patient/admission/appointment/icd10cm/healthfactor/item syncs (SolrUtil 899/952/965/1579/2032/1790); BUT `getEncounterPatientSearch` (PatientServiceImpl:458) itself is not @Transactional, and jobs commit outside tx (ApplicationJobs:2083-2093).
- Resolution: refinement, not contradiction.
- Confidence: CONFIRMED. Impact: outbox design must distinguish write-path (tx) vs search-path (read) vs job-path (bulk).

## CONTR-012 — Redis pool values
- Prior: "pool 200/200/100".
- V5: matches `spring.redis.jedis.pool.*`; legacy `spring.redis.pool.*` block says 200/100/0. Two config generations coexist.
- Resolution: use jedis block (200/200/100); legacy block is dead config.
- Confidence: HIGH. Impact: minor; target Redis sizing uses 200/200/100 as baseline.

## CONTR-013 — ext.js / faug-ext.js
- Prior: ext.js backend-injected; faug-ext.js referenced by index.html.
- V4: ext.js injection CONFIRMED (IntegrationOperationsFilter:760-782); faug-ext.js exists but is referenced NOWHERE (dead file). Globalize extension is the configured one.
- Resolution: corrected.
- Confidence: CONFIRMED. Impact: React migration must reproduce EXT_DATA injection contract (global vars like gvar@maltaApplication in dynamicAspects conditions depend on it).

## CONTR-014 — FAUG "lookup" construct
- Prior: schemas contain "lookup".
- V4: no `"lookup":` schema key; concept = crudop `lookup_type` params (`walkinVisit-schema.json:6245`).
- Resolution: terminology corrected.
- Confidence: CONFIRMED. Impact: schema-engine mapping table in React design must use correct construct names.

## CONTR-015 — Template engine
- Prior: mustache/underscore assumption.
- V4: custom `<%...%>` + `#var/#for/#if` engine in faug-min.js templatize; no mustache/handlebars markers.
- Resolution: corrected.
- Confidence: CONFIRMED. Impact: 2,943 templates must be converted by a custom transpiler — do not assume off-the-shelf template compatibility.

## CONTR-016 — "29 candidate services" basis
- Prior: MICROSERVICE-CATALOG 29 services.
- V6: "29" has no source-code definition; 155 service files / 135 *ServiceImpl.
- Resolution: 29 is a design choice (consolidation of 40 modules), not a code-derived number. Keep as PRELIMINARY; validate boundaries with V6 writer maps.
- Confidence: MEDIUM. Impact: service catalog stays a candidate list until W2 ownership validation.

## CONTR-017 — MasterCachedServiceImpl god-class ranking
- Prior: largest service, 12.7k LOC, 264 endpoints.
- V6/V1: 13,331 LOC — 4th largest; 270 endpoints.
- Resolution: corrected ranking (UploadFileServiceImpl 19,941 > NursingServiceImpl 18,119 > PathoOrderServiceImpl 14,629 > MasterCachedServiceImpl 13,331 > InventoryServiceImpl 12,941).
- Confidence: CONFIRMED. Impact: refactor priority ordering shifts; UploadFileServiceImpl deserves investigation (mostly file/report handling?).

## CONTR-018 — Solr collection count "6, 5 live"
- Prior: 6 collections, 5 live.
- V5: 6 keys defined; service collection key has no @Value binding in SolrUtil → 5 bound in code.
- Resolution: refined (6 defined, 5 code-bound).
- Confidence: HIGH. Impact: keep all 6 collections; verify service collection usage in live env (UNKNOWN-034).

## CONTR-019 — @Transactional variant usage
- Prior: "~1,300 @Transactional; rollbackFor/REQUIRES_NEW notes".
- V6: 1,309 occurrences; 1,265 `rollbackOn=Exception.class` (EclipseLink semantics, NOT Spring rollbackFor); 7 REQUIRES_NEW; 2 rollbackFor; 1 rollbackFor Throwable; 1 dontRollbackOn.
- Resolution: corrected semantics — rollbackOn covers CHECKED exceptions too; target Spring must use rollbackFor=Exception.class equivalents.
- Confidence: CONFIRMED. Impact: transaction porting must translate rollbackOn→rollbackFor; 7 REQUIRES_NEW sites (GenericServiceImpl:35,53; IntegrationServiceImpl:1780,1913; InventoryServiceImpl:12261; IPDHelper:241; MasterCachedServiceHelper:189,195; HelperServiceImpl:1170; UserManagementServiceImpl:464) get standalone-tx treatment.

## CONTR-020 — Patient portal role escalation
- Prior: security register listed portal as low-risk.
- V2: `DefaultAuthenticationImpl.java:358` grants role "Administrator" to portal logins — contradiction with least-privilege assumption.
- Resolution: recorded as new CRITICAL finding (Security §8). Frontend-only enforcement makes this exploitable.
- Confidence: CONFIRMED. Impact: target IAM must define patient context separately from staff RBAC; never share role space.