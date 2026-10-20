# DEAD-CODE-CANDIDATES.md — Classification with Evidence (iHMS/OHUM PAS NG)

> Synthesized from: 04-backend-architecture.md + _discovery/agent-04/findings.md, 05-database-catalog.md + _discovery/agent-05/findings.md, 07-integrations-and-search.md + _discovery/agent-07/findings.md, _discovery/reconciliation-notes.md, 08-security-rbac-audit.md.
> Classification: **Confirmed active / Probably active / Unknown / Probably dead / Confirmed dead**.
> RULE: never delete during discovery. Every candidate carries evidence + a verification method. Direct verification performed 2026-08-14 on `D:\ihms`.

---

## 1. Summary counts

| Class | Count | Notes |
|---|---|---|
| Confirmed active | 30 stored procs via namedStoredProcQ + 3,000 reachable service endpoints (3,003 − 3 unwired) + 57 ABDM callbacks + 5 Solr collections + MemoryCacheImpl | verified live callers |
| Probably active | ~65 endpoint classes wired via XML/component scan; ~32 more procs evidenced in agent-06 traces | behavior depends on config |
| Unknown | **~657 stored procs** (719 − 30 − 32 evidenced); 24 `@Component` beans not referenced in services.xml; FhirServer; eSign; Backman OLIS; 1,583 DTOs; patportal dir | must be verified per candidate |
| Probably dead | commented jobs (3 classes), commented endpoints (2 confirmed + litter), Base.java hierarchy, kundera/MongoDB props, old report entries, `service_risl_qa` collection, RbacSecurityInterceptor | strong static evidence |
| Confirmed dead | `Base.java` unused by any entity; `APP015` check (proc always returns 0); serviceSolrName config | code-internal proof |

---

## 2. Stored procedures — 719 artifacts, only ~30 live

**Evidence:** agent-05 §6.1 (`generated-procs/`: 692 procedures + 17 functions + 11 packages, all `CIMS_*`); 30 called via `namedStoredProcQ()` from `BillingCommonUtilServiceImpl.java:110,121,133,147,155,179,240,264`, `CommonUtilServiceImpl.java:649,662,674,1117,1120,2972-2978`.

### 2.1 CONFIRMED ACTIVE — the 30 live-called procs (agent-05 §6.1)
```
CIMS_ADDCHARGE, CIMS_ADDITEMSTOCK_UPDATE, CIMS_GETCHARGES, CIMS_GETCHARGESTEMP,
CIMS_GETPATIENTADVANCE, CIMS_ITEMSALE_VISIT, CIMS_GETITEMSALESDETAILS,
CIMS_GETITEMSALESRETURN_VISIT, CIMS_GETITEMSALESRETURNDETAILS,
CIMS_GETSPONSORBYADMISSIONID_NEW_DOC, CIMS_GETITEMCATEGORY,
CIMS_GETALLSERVICEMASTERDETAILSBYDESCRIPTION, CIMS_GETITEMTAXLISTFORUNIT,
CIMS_GETPATIENTGENERALDETAILSLISTFORSEARCHNEW, CIMS_GETPATIENTGENERALDETAILSLISTFORSEARCHSTRICT,
CIMS_GETPATHOTESTPARAMETERANDSUBTESTDETAILS_UPDATED_NEW2, ..._UNITWISE, ..._NEW3,
CIMS_GETINVSTOCKJOBSCHEDULE, CIMS_GETITEMSBYSTOCKFORLINENREQUEST, CIMS_JOBSCHEDULING,
CIMS_UPDATECHARGES, CIMS_UPDATECHARGETEMP, CIMS_CONFIGUREUNITPARAMETER,
CIMS_CheckPackageSubscriptionByAdmId, cims_getappointmentfornotification,
CIMS_rpt_PathoResultEntry_new1, CIMS_rpt_PathoTemplateReport_Final1,
CIMS_rptPathResultEntryFroCultureTest_Final1
```

### 2.2 PROBABLY ACTIVE — procs evidenced in agent-06 workflow traces (32 total, not in the 30-list, called via native SQL or helper)
```
CIMS_ADDPATIENTREGISTRATION, CIMS_ADDPATIENTREGISTRATIONDETAILS,
CIMS_ADDPATIENTSPONSOR_NEW_DOC, CIMS_ADDADMISSIONINFORMATION,
CIMS_ADDADMISSIONDETAILS, CIMS_ADDADMISSIONADDEMERGENCYCONTACT, CIMS_UPDATEBEDOCCUPIEDFLAG,
CIMS_ADDBEDDISCHARGE, CIMS_UPDATEIPDISDISCHARGE, CIMS_ADDBILL, CIMS_ADDBILLDETAILS_1,
CIMS_ADDDOCTORSHARE, CIMS_BILLFORINVESTIGATIONNEW, CIMS_GETPATHOTESTLISTFORBILL,
CIMS_ADDPATHORDERBOOKING, CIMS_ADDPATHORDERBOOKINGDETAIL, CIMS_ADDRADORDERBOOKING,
CIMS_ADDCONCESSION, CIMS_ADDCONCESSIONDETAIL, CIMS_GETBILLLISTFORSEARCH_NEW_one,
CIMS_ADDPAYMENT, CIMS_ADDPAYMENTBILLDETAILS, CIMS_ADDPAYMENTADVANCEDETAILS,
CIMS_UPDATEBILLFORCONCESSION, CIMS_ADDITEMSALESDETAILS_NEW,
CIMS_ADDUPDATEPATIENTPROCEDURESCHEDULEMASTER, CIMS_ADDUPDATEOTDETAILS,
CIMS_ADDUPDATEPATIENTPROCDURE, CIMS_ADDUPDATEOTDETAILSDOCDETAILS, CIMS_GETAUTOTOKENNO,
cims_getlatestautocodeandincremetitbyone (function), cims_getitemanditembatchlistbyvisitid_new3
```
Evidence: agent-06 findings §1.1-1.9 (file:line). **Verification:** grep each name in `src/main/java` (currently confirmed at trace sites); count ≥1 call site.

### 2.3 UNKNOWN — the remaining ~659 (719 − ~60 evidenced)
- **Evidence:** agent-05 §6.1 — "30 actively called... the rest are UNKNOWN usage status".
- **Verification method (no code change):** (1) `rg -n "<PROCNAME>" src/main/java` per proc (case-insensitive); (2) query Oracle `ALL_DEPENDENCIES` for reverse dependencies from live procs (a legacy proc may be called *by* a live proc); (3) grep frontend for report names that map to proc-driven reports (integration/reports/reports.json); (4) DB audit/usage tables if available. Classify each as live (≥1 Java call), transitively live (depended-on by a live proc), or legacy-only (nothing references; candidate for archival, NOT deletion).

---

## 3. Service beans — the "11 unwired" claim vs verified reality

- **Claim:** reconciliation-notes.md:57 — "services.xml has 134 bean refs; 11 @Component service beans NOT wired into CXF" (attributed to agents 04+01).
- **Direct verification (2026-08-14, this synthesis):** services.xml `<jaxrs:serviceBeans>` holds **133 refs**; **67 XML bean definitions**; **96 @Component beans** (85 referenced in services.xml refs). Cross-reference of `@Component("...")` with the serviceBeans ref list yields **exactly 1 REST `@Path` class not on the CXF bus**:
  - `actionNotificationServiceImpl` (`services/custom/ActionNotificationServiceImpl.java`, class path `/actionNotif`, 3 endpoints) — `@Component("actionNotificationServiceImpl")` present, **absent from `<jaxrs:serviceBeans>`** → its 3 endpoints are NOT reachable via CXF. **Unknown** whether a second registration mechanism exists (CXF scans none other).
- **24 other `@Component` beans not referenced anywhere in services.xml** — these are non-REST infrastructure (auditHandler, auditHelper, ServiceLayerAspect, InventoryJobSchedule, hl7/*, util/*, reportServiceImpl, mergePatientUtils, billingCommonUtilServiceImpl, commonUtilServiceImpl, etc.) that are DI-injected by type, not CXF endpoints. Not dead; normal Spring wiring. `errorPathHandler` resolves via Spring default naming (class `ErrorPathHandler`, `@RestController`) — the ref `services.xml:184` is valid.
- **Conclusion:** the "11 unwired" figure could not be reproduced; verified unwired REST surface = **1 bean / 3 endpoints**. Flag the reconciliation figure as stale/incorrect; re-run the bean-diff script during migration on the *deployed* services.xml (deployment may differ from repo).
- **Verification method:** script comparing `@Component("id")` across `src/main/java` vs `<ref bean>` inside `<jaxrs:serviceBeans>`; then for unwired candidates, assert `@Path` presence and test reachability (HTTP 404 vs 200).

---

## 4. Dormant integrations & jobs (07-integrations-and-search.md §3)

| Candidate | Class | Evidence | Verification |
|---|---|---|---|
| HL7 inbound pickup job | **Probably dead (config)** | `jobs/Hl7InboundMessageJob.java` @Scheduled commented (`hl7inbound.pickup.freq`); agent-07 §3.10: "Inbound pickup job disabled"; HL7Helper util still live (outbound) | check deployed properties/job registration; decide inbound revival vs archival |
| EHR sync job | **Probably dead (config)** | `EHRSyncJobs.java` @Scheduled commented; `ehr.sync.schedule` duplicated 3× (`application.properties:820,974,1060`) — conflicting/disabled; agent-07 §3.9 | confirm no enabled schedule; decide whether EHR/EWD export (`creteEHRFiles` ApplicationJobs:1094) remains |
| AutoNextDayAllocation | **Probably dead** | `AutoNextDayAllocation.java` @Scheduled commented (`0 10 1 * * ?`); agent-04 §4 | confirm no external trigger; nurse-allocation logic moved to InventoryJobSchedule.nursePatientAllocation (live) |
| eSign (C-DAC) | **Unknown** | `util/CAeSignService.java` — no Java caller found (agent-07 §3.14, :294); config `esign.*` present | grep callers; check UI/scripts/frontend for eSign invocation; verify at deployment |
| EXT LIMS (state lab) | **Probably dead (config)** | `ApplicationJobs.addStats_Archived` — gated by DB `ISEXTLMISENABLED` (agent-07 §3.1 #28); disabled in checked-in config | check DB config value at deployment |
| `service_risl_qa` Solr collection | **Confirmed dead** | `serviceSolrName` declared (SolrUtil.java:114, ApplicationJobs.java:144) never used; no indexer, no reader; absent from DR script (agent-07 §1.2 #6) | confirm no consumer; drop from target Solr |
| FhirServer (HAPI R4) | **Unknown** | `@Bean genFhirServer()` `@ConditionalOnProperty(abdm.enabled=true)` (App.java:261); **NO ServletRegistrationBean** — only CXFServlet at `/api/*` (App.java:149-150); agent-04 §7 | check abdm.enabled at deployment; test FHIR base URL reachability (UNKNOWN-009) |
| RbacSecurityInterceptor (FHIR) | **Probably dead** | `genserv/interceptors/RbacSecurityInterceptor` commented out; TokenBasedSecurityInterceptor active (agent-04 §7) | — |
| RedisClusterCacheImpl / RedisCacheImpl / Hazelcast / Ignite | **Probably dead (config)** | commented out in `applicationContext.xml:34-66`; MemoryCacheImpl active (:43-45) — but `risl_2233` prefix + staging cluster config suggest prod intends Redis (agent-07 §2.2) | **deployment config ambiguity** — verify which bean ships in prod (UNKNOWN-010/RISK-REGISTER) |
| `updateHealthIds` ABHA backfill | **Probably dead** | @Scheduled commented (ApplicationJobs.java:2039); agent-07 §1.5 | — |
| `syncPatientsToSolrJob` / `readInvenStockExternal` / `doctorCheckoutJobOld` / whatsapp cron | **Probably dead** | ApplicationJobs.java:520,585-614 (old variant), 1608; InventoryJobSchedule.java:503 — commented variants (agent-04 §4) | verify per-job |

---

## 5. Commented-out endpoints & dead code in services

| Candidate | Class | Evidence | Verification |
|---|---|---|---|
| `updateDonorBloodTestStatus` | **Probably dead** | commented `@Path` in `pas/BloodBankServiceImpl.java` (agent-04 parser skipped) | confirm no JS caller; legacy path removal |
| `departmentFilterForRad` | **Probably dead** | commented `@Path` in `custom/MasterCachedServiceImpl.java` | confirm no JS caller |
| "commented-out endpoints litter code" | **Probably dead (volume)** | agent-04 §9 #10: "commented-out endpoints litter code (parser skipped them) — dead code volume is high" | systematic scan of `// @Path`/`/* @Path */` blocks; map to JS callers |
| Duplicate/overloaded endpoints | **Probably active** | `uploadHospitalLocation` x2, `addToQueue` GET+POST (agent-04 §9 #10) | keep both until JS callers triaged |
| `APP015` dead admission check | **Confirmed dead** | `CIMS_ADDADMISSIONINFORMATION` always ends 0; Java `resultStatus==-1 → APP015` cannot fire (06-catalog BR-IPD-004 note; agent-06 R4) | remove from target; admission uniqueness relies on BERB013 pre-check |
| Old report entries | **Probably dead** | `integration/reports/reports.json` contains `getServicewiseCollectionReportOld`, `getUserwiseCollectionReportOld`, `getUserwiseSummaryReportOld` (verified 2026-08-14); frontend schema `getServicewiseCollectionReportSchemaOld.json` alongside `...Schema.json` | check fjs-config_reports.json route list + JS callers; keep or archive per report-config source of truth (UNKNOWN-013) |

---

## 6. Unused hierarchy & config

| Candidate | Class | Evidence | Verification |
|---|---|---|---|
| `models/Base.java` | **Confirmed dead** | `Base extends LBase` (createdAt/updatedAt/createdBy/updatedBy/status enum) — **no entity extends Base** (agent-05 §2.1: masters/transactionals/txt/trx/aasmaan/ejs all use MBase/LBase/EjsBase; "Nobody extends Base (dead hierarchy)") | remove in target; 05-database-catalog.md:35 "Base — unused by any entity" |
| kundera/MongoDB props | **Probably dead (config)** | `application.properties:23-27` kundera.nodes/keyspace=zdocit (agent-05 §9: "hint at MongoDB usage — not confirmed in code") | grep kundera/mongo imports; no usage found to date |
| `persistenceContext.xml` PU `ZDocit-HIS-SQL-Server-PU` | **Probably dead** | legacy PU referenced; active unit `ORCL` (persistence.xml:5); persistenceContext.xml not imported by applicationContext.xml (agent-01 §2.2 UNKNOWN); Hibernate/MySQL blocks commented | verify runtime import; target = single PG PU |
| patportal directory | **Confirmed empty/absent** | `pom.xml:67-73` excludes `public/patportal/{app,bin,hooks,node_modules,platforms,plugins,res}` (a Cordova app) — **directory does not exist in repo** (verified); backend `patp/PatientPortalServiceImpl.java` (22 endpoints) has no checked-in frontend caller besides ohumlite schemas | the backend service IS wired (patp in services.xml) — verify deployed patportal app separately; decision: revive or archive |
| 1,583 DTO files | **Unknown (volume)** | agent-04 §9 #11: enormous DTO surface, much stored-proc-result-coupled; 21 DTOs are @Entity (agent-05 §2.2 #8) | categorize by caller; proc-result DTOs die with their procs; keep wire-format DTOs for contract tests |
| `views/` 155 files | **Probably active** | response DTOs = wire format for contract tests (TEST-STRATEGY §3.1) | keep as contract fixtures |

---

## 7. Audit/caching dead-weight

| Candidate | Class | Evidence |
|---|---|---|
| `@AuditEvent` annotation-driven audit | **Probably dead** | 10 annotations total; handler never invoked from aspect (agent-08 §5.1); manual audit is the live path (59 files, 568 call sites) |
| Per-path permission check | **Confirmed dead (commented)** | `ServiceLayerAspect.java:421-426` commented out (agent-08 §3.1) — MUST be revived in target (security risk, RISK-REGISTER #1) |
| `PasswordValidator.attempt` | **Confirmed dead (no-op)** | `PasswordValidator.java:24` returns true unconditionally; active bean via `auth.pwd.manager=password` (agent-08 §7) — MUST be replaced in target |

---

## 8. Do-not-delete rule

All above are candidates only. Before any removal in the target: (1) confirm zero callers (Java + JS + reports.json + other procs via ALL_DEPENDENCIES); (2) archive artifacts (procs, DTOs, report configs, schemas) to a `legacy/` reference area for the migration's golden tests; (3) record the removal in RISK-REGISTER + UNKNOWN-BEHAVIOR with the verification performed. Dead code with *deployment* ambiguity (cache impl, HL7 inbound, EHR, FhirServer, eSign, patportal) must be verified against the deployed environment, not the repo alone.
