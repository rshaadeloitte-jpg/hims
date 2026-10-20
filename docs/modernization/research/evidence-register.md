# Evidence Register — iHMS Modernization Research

> Every major conclusion must point here. Format: ID | Claim | Evidence (path:line) | Agents | Confidence.
> Primary source: `D:\ihms`. Validation agents V1–V6 re-verified load-bearing claims 2026-08-14.

## E-01 System identity
- Artifact `com:ohumpasng:3.4` (JAR); main class `com.ohum.App`; context `/IHMS`; port 8080.
- Evidence: `D:\ihms\pom.xml:6-9,21`; `D:\ihms\application.properties:3-4`.
- Confirmed by: Phase-0; V1 (App.java:150 CXFServlet `/api/*`). Confidence: CONFIRMED.

## E-02 Stack: Java 1.8, Spring Boot 2.7.5, CXF, EclipseLink 2.6.4, Oracle
- Evidence: `pom.xml:14` (java 1.8), `pom.xml:32-35` (SB 2.7.5), `pom.xml:315-316` + services.xml (CXF JAX-RS), `pom.xml:376-379` (EclipseLink, Hibernate excluded), `application.properties:63-101` (Oracle DUAL, OracleDriver, jazz url).
- Confirmed by: V1, V3. Confidence: CONFIRMED (EclipseLink PG fallback branch exists at `App.java:180-185` but inert).

## E-03 Repository scale
- 3,001 Java files; 8,748 frontend files; 719 proc SQL files; 1,308 schemas; 2,943 templates; 42 fjs-config files (+root); 71 module JS files.
- Evidence: direct counts Phase-0 and V3/V4 (identical numbers). Confidence: CONFIRMED.

## E-04 Four routing layers (now five surfaces)
1. CXF JAX-RS bus: 137 bean refs at `/rest` (`services.xml:93,96-227`); 155 service files / 231,791 LOC / 132 `@Path` classes.
2. Generic CRUD filter: `CrudOperationsFilter.java:123` pattern, 659 `@CrudApi` entities (654 registered via MBase check at `:127`); methods `GenericServiceImpl.java:36,54,71,107,153,259,342`.
3. Report dispatcher: `IntegrationOperationsFilter.java:261-281` Nashorn reports.js + reportMap; serves `/IHMS/api/rest/reports/<name>` (`:465-495`); Velocity `templates/generic_reports.vm` (`:571`).
4. /ext/api gateway: `IntegrationOperationsFilter.java:235-246,445-457` ApiIntegrationMapping + allowedUsers gating (`:442-447`).
5. Additional: HAPI FHIR `FhirServer extends RestfulServer` (`xcg/genserv/FhirServer.java:41`, bean `App.java:261`); Spring MVC `ErrorPathHandler` `/error` (`ErrorPathHandler.java:11`); dual-annotated `InventoryEAushadiIntegrationImpl` (`:111`).
- Confirmed by: V1 (0.9–0.98 confidence). Correction: prior "3,060 endpoints" not reproducible as a single sum — raw `@Path(` 3,167; method-level verbs 3,041. MasterCachedServiceImpl = 270 endpoints (not 264).

## E-05 Orphaned/unpublished services
- 17 @Component service classes absent from services.xml; only one JAX-RS-annotated orphan: `custom/ActionNotificationServiceImpl` (`@Path("/actionNotif")`, 3 endpoints) — unpublished dead surface.
- Evidence: V1 (count 89 @Component across services; 17 unmatched). Correction to prior "11".

## E-06 Security: no backend authorization
- `SpringSecurityConfig.java:117` `antMatchers("/**").permitAll()`; CSRF disabled `:110-111`; stateless `:113`.
- Enforcement commented out: `ServiceLayerAspect.java:421-426` (`// throw new DummyHTTPStatusException...`); role check commented `:316-322,374-379`; `IntegrationOperationsFilter.java:733-738` token check commented.
- RBAC is frontend-only: `public/js/common-module.js:3389-3396` (checkPermissionForSplash), `queue/templates/roleBasedNav.html:9-17` (data-roleM), route gating `fjs-config.json:606` (`secure:true`) + faug-min.js onBeforeRouteChange.
- Confirmed by: V2. Confidence: CONFIRMED.

## E-07 Security: password verification is a no-op + bypasses
- `PasswordValidator.java:13-24` — `return true;//suppliedPassword.equals(actualPassword);`.
- Test backdoor: `DefaultAuthenticationImpl.java:199-223` user `dGVzdC51c2VyLmludGVybmFsLjAwMQ==` (=test.user.internal.001) with date-derived password; `:244` skips attempt().
- Static-token bypass: `secretTokenForOnlineAppointment` in application.properties → `setGlobalUser(1L)` with no credential check (`IntegrationOperationsFilter.java:659-660,871-872`).
- `@NotSecure/@Login/@IgnoreApiForAuth` → `ServiceLayerAspect.java:364-367` assigns global user id=1: ~50+ anonymous endpoints.
- Frontend hardcoded token: `public/js/common-module.js:1136`.
- Passwords near-plaintext: `DefaultAuthenticationImpl.java:410-416` encryptDecryptUserKey (char*2-5), `:922-931` `SELECT ... WHERE LOGINNAME=? AND PASSWORD=?`.
- Secrets: 34 plaintext secret keys in `application.properties` (values <REDACTED>) + duplicate set in `artifacts/risl/application.properties` + `src/main/resources/fhir/abdm-constants.properties`. Private key material committed: `my.key`, `qzPrivateKey.pem`, `digital-certificate.txt`.
- New: patient portal login grants role "Administrator" (`DefaultAuthenticationImpl.java:358`); eMitra decrypted PII logged at INFO (`EmitraRedirectFilter.java:102,138`); SSO `userdetails` in query string (`SSOAuthFilter.java:110`); `/ext/api` empty-allowlist bypass (`IntegrationOperationsFilter.java:442-447`); __token cookie no HttpOnly/Secure (`SSOAuthFilter.java:179`).
- Confirmed by: V2. Confidence: CONFIRMED.

## E-08 Stored-procedure usage is pervasive (major correction)
- 719 .sql files = 702 procedures + 16 functions + 1 mixed; 646 `CIMS_*`; ZERO packages (prior "packages CIMS_*" claim wrong).
- Java references **918 unique procedure names** (933 active occurrences) via `procedureName=` in `dto/queries/DTOQueries_1..5.java` (204/202/208/208/110) — not ~30. Direct call: `CIMS_GETcounter` at `AddServiceOrderServiceHelper.java:3627,3666`.
- 1,343 `namedStoredProcQ()` call sites across services (top: InventoryServiceImpl 209, PathoOrderServiceImpl 74, OTSchedulerServiceImpl 56).
- Evidence: V3 (exact counts), V6 (call sites). Prior RISK #3 mitigation ("30 live procs") must be revised: golden tests needed for all 918 via runtime capture.

## E-09 Database surface
- 940 @Entity classes in `com.ohum.models` (not ~898); +109 in dto → 1,049–1,050 total; 894 distinct `@Table` names. "843 tables" UNVERIFIABLE from source (no ALL_TABLES dump).
- T_REGISTRATION / T_REGISTRATION_DETAILS / T_REGISTRATION_INT have NO JPA mapping (DB-only; ~120 native-SQL reference sites).
- Oracle constructs in Java: ROWNUM 489, SYSDATE 247, DUAL 47, NVL 21, MERGE INTO 4, CONNECT BY 0. Sequences: 3 active @SequenceGenerator, 50 hardcoded names, 155 distinct in procs.
- Evidence: V3. Confidence: CONFIRMED.

## E-10 Transaction: mega-transaction chain
- `AddServiceOrderServiceImpl.java:2037` `@Transactional(rollbackOn=Exception.class)`; chain writes per step: bill via `CIMS_ADDBILL` (`:2206`), charges via `CIMS_ADDCHARGETEMP`/`CIMS_ADDCHARGE` (`:2271-2276`), lab `T_PATHORDERBOOKING` (`:2273`), radiology `T_RADIOLOGYORDERBOOKINGDETAILS` (`:2274`), EMR `T_EMR_DETAILS`+`T_EMR_SERVICES` (`:2351,2367`), doctor share `T_DOCTORSHARECHARGESDETAIL` (`:2503,2508`), item sale `T_ITEMSALE` (`:2568`). HL7 leg DISABLED (`:2318,2399` commented; `hl7.billingOrder.messages.enabled=false` `application.properties:824`).
- 1,309 @Transactional occurrences (1,265 rollbackOn=Exception); zero class-level; zero em.flush(); zero @Version.
- Evidence: V6. Confidence: CONFIRMED.

## E-11 No JPA entities for core financial tables
- T_BILL, T_BILLDETAILS, T_ADMISSION, T_VISIT, T_CHARGES: no entity — written via procs/native SQL only. Writer maps (top 5): T_ENCOUNTER → NursingServiceImpl 46, AppointmentServiceImpl 42, PatientServiceImpl 38, EmergencyAndWalkinVisitServiceImpl 24(+2), PathoOrderServiceImpl 22; T_ADMISSION → PatientTransferDischargeServiceImpl 27, BedManagementServiceImpl 8, PatientRegAdmissionServiceImpl 6, NursingServiceImpl 5, IntegrationHelper 4; T_BILL → BillReceiptServiceImpl 52, BillSummaryServiceImpl 42, PatientServiceImpl 22, PathoOrderServiceImpl 19, AddServiceOrderServiceImpl 9.
- Evidence: V6. Confidence: CONFIRMED. Migration impact: these objects must be modeled in target PG explicitly (contract-first).

## E-12 Visit status magic numbers (no constants)
- No VisitStatus constants class; literals in native SQL: T_APPOINTMENT VisitStatus 11/12/4/6; T_ENCOUNTER VisitStatus 3/10/9. T_Encounter entity maps visitstatus1/2/currentvisitstatus (`Encounter.java:194,196,202`); T_Appointment entity has NO VisitStatus column mapped (`Appointment.java:29`).
- Evidence: V6. Confidence: CONFIRMED. Impact: status dictionary must be locked into contracts before port.

## E-13 God classes
- Top by LOC in com.ohum.services: UploadFileServiceImpl 19,941; NursingServiceImpl 18,119; PathoOrderServiceImpl 14,629; MasterCachedServiceImpl 13,331; InventoryServiceImpl 12,941; BloodBankServiceImpl 7,836; PatientServiceImpl 7,487; CommonUtilServiceImpl 6,884; PatientTransferDischargeServiceImpl 5,241; MdmServiceImpl 5,105; EmergencyAndWalkinVisitServiceImpl 4,990; OTSchedulerServiceImpl 4,954. 16 service files >2,000 lines. MasterCachedServiceImpl is 4th largest (correction to "largest").
- Evidence: V6, V1. Confidence: CONFIRMED.

## E-14 DAO layer
- Exactly 2 DAO classes: `GenericDAO` (EntityManager facade, audit advice via ServiceLayerAspect `:697-778`) and `GenericQuery` (fluent native/stored-proc builder). No manual tx management in services; no em.flush().
- Evidence: V6. Confidence: CONFIRMED.

## E-15 FAUG frontend specifics
- Engine `faug-min.js` (360,613 B): custom templatize (`<%expr%>`, `#var`, `#for`, `#if`), crudop, hash routing (`i+="#"+t`).
- **636 routes** (not 533) in `fjs-config.json` `router.routes` (`:587-607`); 635 unique; one case-variant duplicate (`pharmacy:ipReturnFlow`/`pharmacy:iPReturnFlow`) breaks strict JSON parsers. 3 aliases. `secure:true` + onBeforeRouteChange permission gate.
- `ext.js` absent from repo; served from cache by `IntegrationOperationsFilter.java:760-782` (default `var EXT_DATA=undefined;`). `faug-ext.js` exists but is unreferenced (dead).
- Billing arithmetic client-side: `billing-module.js:2094,2101-2103,2111,1628,792,1383-1396,2040`.
- QZ Tray `js/qz-tray.js` insecure ports 8182/8283/8384/8485, `ws://localhost:8182` (`application.properties:726`); barcode client `jquery-barcode.min.js` + server `BarCodeGenerator.java`/`QRCodeGenerator.java`; 4 ZPL templates.
- dynamicAspects: 40 schema files, faug-min.js 83 refs. "lookup" is not a schema key (correction).
- Evidence: V4. Confidence: CONFIRMED.

## E-16 Solr
- 6 collection keys; service collection unbound in code → effectively 5 live (`SolrUtil.java:105-129`); ZK hosts `10.10.5.151-153:2181-2183`.
- Indexing inside transactions: `SolrUtil.java:899,952,956` (`@Transactional(rollbackOn=Exception.class)` → addBeans+commit + `UPDATE t_registration_int SET solr_status=1`); `:965-990,1579-1580,2032-2033,1790-1791`; delete via `ServiceLayerAspect.java:1172`.
- Encounter 3-day window: `solr.encounter.retain.days=3` (`application.properties:796`); `ApplicationJobs.java:542-563` fence + clearClosedEncountersSync.
- Patient search embedded in transactional flows: `PatientRegAdmissionServiceImpl.java:236,1663-1664`; `PatientRegApptServiceImpl.java:279,1300`; search `PatientServiceImpl.java:501-509` (not itself @Transactional — gated by solrEnabled).
- Evidence: V5. Confidence: CONFIRMED.

## E-17 Redis & cache
- `application.properties:109-153`: standalone `oracle.sms:6379`, prefix `risl_2233`, jedis pool max-active 200 / max-idle 200 / min-idle 100 (legacy block differs 200/100/0).
- **Active cache impl = MemoryCacheImpl** (`applicationContext.xml:43`); RedisCacheImpl (`:38`) and RedisClusterCacheImpl (`:34`) commented out.
- Evidence: V5. Confidence: CONFIRMED (resolves content of prior UNKNOWN-008 directionally; deployment may differ — keep open).

## E-18 Integrations (all 22+ have config AND code)
- ABDM (xcg.abdm + services.abdm), HCX (xcg.abdm.client.hcx + els.hcx), eSanchar SMS/WhatsApp (esanchar.wabot.rajasthan.gov.in), e-Aushadhi (InventoryEAushadiIntegrationImpl, 4 endpoints + 2 schedules), RGHS (rghs.* + els.RGHSSims), Chiranjeevi (models.chiranjeevi + els.chiranjeevi + tokens), Bhamashah, JanAadhaar, e-Mitra (EmitraRedirectFilter + util.Emitra*), SSO (SSOAuthFilter + mobile), LDAP (disabled `ldap.auth.enabled=false`), Aadhaar (sewadwaar 4 APIs), HL7 (HAPI v2.5 util.hl7 + Hl7InboundMessageJob — @Scheduled COMMENTED OUT), FHIR (HAPI 5.7.0, dstu2/3/R4), eSign (CAeSignService), MinIO (code present, `minio.enabled=false`), EWD/EHR (emr.url + EHRSyncJobs), PCTS, Mera Aspataal, Jeevanvahini, Kaysons, e-Raktkosh (eRaktkoshDonation cron).
- 28 cron/schedule keys + 10 hardcoded crons (ApplicationJobs.java 2968 lines, 46 @Scheduled; InventoryJobSchedule 5; EHRSyncJobs none active).
- Evidence: V5. Confidence: CONFIRMED.

## E-19 Jobs/async
- 68 @Async (SendNotificationServiceImpl 41); taskExecutor pool 20/40 queue 4500 (`App.java:229`); ContextAwareExecutorDecorator; ApplicationJobs ad-hoc fixed pool 25 (`:1914`).
- Evidence: V6. Confidence: CONFIRMED.

## E-20 Business modules & features (prior discovery, unchanged)
- 40 modules M01–M40 from fjs-config/module dirs; 289 features; 39 documented business rules; A1–A9 status dictionaries; 533→636 routes; 28 integrations; 29 candidate services (PRELIMINARY). Evidence: `02-business-module-catalog.md`, FEATURE-MATRIX, BUSINESS-RULES, STATUS-CODE-DICTIONARY. Sources: module dirs + fjs-config + agent findings. Confidence: HIGH (feature/rule counts from prior discovery round; route count corrected to 636).