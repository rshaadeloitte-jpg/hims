# 04 — Backend Architecture: iHMS / OHUM PAS NG

Reverse-engineered from `D:\ihms` (research only, no modifications). Stack: Spring Boot 2.7.5, Java 8,
EclipseLink JPA 2.x, Apache CXF 3.x JAX-RS (NOT Spring MVC), Oracle DB, Solr Cloud, Redis-cacheable
in-memory cache, HAPI FHIR (vendored subset), Nimbus JOSE (HCX JWE), Velocity/JasperReports for printing.

---

## 1. Runtime topology & REST bus wiring

```
Browser / EMR / Mobile
        │
        ▼
Servlet container (Spring Boot, server.port=8080, context-path=/IHMS)
   ├─ servlet filters: GzipFilter → SpringSecurityConfig (CSP headers) → IntegrationOperationsFilter /
   │                   CrudOperationsFilter / SSOAuthFilter / EmitraRedirectFilter
   ├─ CXFServlet  @  /api/*                     (App.java:149-150)
   │      └─ <jaxrs:server id="restServices" address="/rest">   (services.xml:93)
   │             ├─ 134 <jaxrs:serviceBeans>     (services.xml:95-227)
   │             ├─ inInterceptor: PreProtocolInterceptor        (forces _type=json)
   │             ├─ providers: RequestResponseInterceptor (Jackson), ServiceJAXRSExceptionMapper,
   │             │             JAXBElementProvider, NullableLongParamConverterProvider
   │             └─ extensionMappings: json|xml|* → application/json
   ├─ FhirServer @Bean (HAPI RestfulServer, FHIR R4)  — NO servlet registration found (dormant flag)
   └─ @ComponentScan("com.ohum") beans — 96 @Component beans incl. 65 REST service beans
```

Key facts:
- **Every endpoint lives under `http://<host>:8080/IHMS/api/rest/<ClassPath>/<MethodPath>`** — there is no Spring
  MVC `@RestController` anywhere; all routing is CXF JAX-RS annotations resolved at bus startup.
- `services.xml` is the single source of truth for the REST bus; bean ids in `<jaxrs:serviceBeans>` must
  match either an XML `<bean>` in services.xml (65 definitions) or a `@Component("beanId")` class (96 definitions).
- `applicationContext.xml` is thin: declares `cacheImpl` (MemoryCacheImpl active; RedisClusterCacheImpl /
  RedisCacheImpl / Hazelcast / Ignite variants commented out) and imports `services.xml`.
- `persistenceContext.xml` references persistence unit `ZDocit-HIS-SQL-Server-PU` (commented out Hibernate path),
  but the **active** unit is `ORCL` (persistence.xml: `jta-data-source jdbc/orcl`, EclipseLink provider,
  `shared-cache-mode NONE`). DB: Oracle via `spring.datasource.url=jdbc:oracle:thin:@//10.10.7.63:1521/orcl`
  (application.properties:63-77).

## 2. Layered structure (service → DAO → entity)

```
com.ohum
├── services/            JAX-RS service classes (132 with endpoints) — the "controller+service" god layer
│   ├── BaseServiceImpl  (1,044 LOC) — @Autowired GenericDAO + CacheInterface + sessionCache;
│   │                    exposes getGenericDAO()/getCacheImpl()/getSessionCache(), error helpers (BEXA*
│   │                    message codes), Velocity/PDF helpers, locale, OkHttp client
│   ├── GenericServiceImpl (370 LOC) — generic CRUD api: crudadd/crudupdate/cruddelete/crudaddMultiple,
│   │                    validates MBase.validate() + pre/post ops, @Transactional(rollbackOn=Exception)
│   ├── pas/             ~66 services (Patient, Bill, Master, IPD, OT, BloodBank, Radiology…)
│   ├── custom/          ~30 services (registration/admission/payment/discharge workflows + helpers)
│   ├── nurse/, cssd/, ejs/, aasmaan/, abdm/, patp/, payment/, ehr/   — domain modules
│   └── helpers:         PatientServiceHelper, IPDHelper, MasterCachedServiceHelper, AddPaymentServiceHelper,
│                        AddServiceOrderServiceHelper, PathoOrderServiceHelper, IntegrationHelper, EHRServiceHelper
├── dao/                 GenericDAO (JPA EntityManager wrapper: save/update/delete/get/getAll/getQ) +
│                        GenericQuery (fluent builder: namedQ()/nativeQ()/typed()/criteria()/storedProcQ(),
│                        nameValue()/values()/paginate()/list()/get()/update())  ← THE query interface
├── models/              JPA entities (masters/, transactionals/, etc.)
├── views/               (155 files) response DTOs — e.g. UserView, BillChargesView, PatientAdmissionView
├── dto/                 (1,583 files) request/result DTOs incl. *ResultDto generated from stored procs
├── mappers/             JAX-RS exception mapper + NullableLong ParamConverter
├── cache/               CacheInterface: MemoryCacheImpl (active), RedisCacheImpl, RedisClusterCacheImpl,
│                        AbstractCacheImpl; @Cached annotation + TTL management
├── aspects/             ServiceLayerAspect (the real "framework"), IntegrationOperationsFilter, UserTrxContextHolder
├── annotations/         26 annotations (BodyParam, NotSecure, Cached, CrudApi, StoredProc*, AuditEvent…)
├── interceptors/        PreProtocolInterceptor, RequestResponseInterceptor, CrudOperationsFilter,
│                        IdEncryptionUtil (PHI), emitra/SSO filters
├── audit/               AuditHandler + AuditHelper (template-driven audit persistence)
├── jobs/                ApplicationJobs (50+), InventoryJobSchedule, HL7/EHR/AutoNextDay (mostly commented)
├── util/                SolrUtil, HL7Helper, EHRUtil, FileStoreUtil, PdfGenerator, BillInvoiceGenerator,
│                        auth/ (DefaultAuthenticationImpl, CustomLDAPAuthenticationImpl, PasswordValidator),
│                        hl7/ (HL7InboundHelperUtil, ProcessHL7InboundMessagesUtil)
├── xcg/                 ABDM (ABDMController, HFRController, client/*), HCX (JWE), genserv (FHIR server)
└── services.xml         CXF bus definition (see §1)
```

There is **no controller/service separation**: the JAX-RS classes are both routers and business-logic
containers. Helpers exist but business logic still lives mostly inside the service impls.

## 3. DI patterns

- **XML beans** (old style) in `services.xml` — 65 service beans, plus auth impls
  (`defAuthImpl` → DefaultAuthenticationImpl, `customLDAPAuthImpl` → CustomLDAPAuthenticationImpl).
- **@Component("beanId")** (newer style) — 96 components, e.g. `@Component("nursingServiceImpl")`.
  Services.xml `<ref bean="...">` resolves against both.
- **Field @Autowired** everywhere, including into `BaseServiceImpl` (genericDAO, cacheImpl, sessionCache).
- Cross-service `@Autowired @Lazy` for cyclic dependencies (e.g. AuthenticationServiceImpl →
  PatientRegApptServiceImpl).
- Constructor injection: essentially absent.

## 4. Data access & transaction strategy

- One `GenericDAO` per service (field-injected, shared) backed by `@PersistenceContext EntityManager`
  (thread-safe per-request proxy). `getQ("JPQL or SQL")` returns a `GenericQuery`; `.nativeQ()` for SQL,
  `.storedProcQ()` for stored procedures; `.list()/.get()/.update()` execute.
- **Query surface is 6 kinds**: named JPQL, native SQL, typed criteria, named stored proc, dynamic stored
  proc (`storedProcQ` + `@StoredProc*` annotations), and direct `EntityManager`.
- **Transactions**: `javax.transaction.Transactional` (JTA annotations) with `rollbackOn = Exception.class`
  on ~1,056 endpoint methods; JpaTransactionManager (App.java:195). Some methods are class-level tx.
  `@EnableTransactionManagement` + `@EnableAspectJAutoProxy` (App.java:73-74).
- **Notable**: `GenericServiceImpl.crudaddMultiple` uses `REQUIRES_NEW` per item so one bad item doesn't
  roll back a batch (GenericServiceImpl.java:59-66).
- **Caching**: `@Cached`-driven master-data cache (MemoryCacheImpl, expiry 86400s, in-process).
  Cached services e.g. MasterCachedServiceImpl (264 endpoints) return cached collections; TTL resets
  are controlled by `NoAuthCacheTTLReset` and the aspect's cache advice (ServiceLayerAspect.java:797).
- **Solr**: patient/encounter/item/healthfactor/ICD-10 index sync — both in-request (updateEncounterSolr
  advice, ServiceLayerAspect.java:853) and via scheduled jobs (fixedDelay 5s jobs) + missing-index backfill.

## 5. Async tasks

- `@EnableAsync` + `taskExecutor`/`taskScheduler` beans (App.java:229-248, thread pool + queue from props).
- `@Async` + `@Path` + `@BackgroundQueue` annotation contract enforced by the aspect
  (ServiceLayerAspect.java:1045-1074) for background queue services (must NOT be @NotSecure).
- WhatsApp report queue (triggerWhatsappReportsInQueue), ABHA record push jobs use fixed-delay loops.

## 6. Exception handling

- `GenericApplicationException` (with HTTP status + message code), `GenericDAOException`, `DummyHTTPStatusException`
  (carries the desired status + optional body for `@RestSuccessResponseStatusCode`).
- `ServiceJAXRSExceptionMapper` (ExceptionMapper<Exception>) converts every thrown exception into a JSON
  error response; Jackson parse errors, invalid format, unknown property all mapped.
- The aspect catches exceptions around service methods, audits them, and rethrows as
  GenericApplicationException/DummyHTTPStatusException (ServiceLayerAspect.java:473-551).
- Errors are NOT centralized: each service calls `getApplicationExceptionErrorMessage("BEXAxxx")` codes.

## 7. Job scheduling

- `@EnableScheduling` (App.java:78); 5 job classes, ~55 @Scheduled (many commented out).
- Heavy users: ApplicationJobs (50+), InventoryJobSchedule (5). Crons come from application.properties
  (e.g. auto.charges.schedule=0 59 11 * * 0; dailyencounter.schedule=0 0 0 * * *; ejs.asha.incentive.schedule
  =0 0 23 * * ?; eAushadiItemStockUpdate.schedule=0 0 4 * * ?).
- Solr index jobs run every 5s (continuous reconciliation), ABHA push jobs every 5s, eRaktKosh nightly,
  RGHS SIMS push 23:30 nightly, PCTS monthly (last day midnight).
- Full table with cron values: see findings.md §4.

## 8. Security & auth model

- Endpoint auth is enforced in `ServiceLayerAspect.handleWorkflow` (@Around on all *ServiceImpl @Path/@WebMethod
  methods, ServiceLayerAspect.java:223-232): resolves UserView from session cache, 401 via DummyHTTPStatusException;
  `@NotSecure` (class or method) bypasses.
- Login endpoints: /auth/* (11 POST + 1 PUT logout), Login annotation + AuditEvent(LOGIN) audit.
- Spring Security (SpringSecurityConfig) only sets CSP/headers — no URL security rules.
- PHI: IdEncryptionUtil (phi.encryption.*) + aspect decrypt of QueryParam/FormParam (getEncryptedValue).
- SSO via interceptors/sso/SSOAuthFilter; e-Mitra redirect filter; LDAP (CustomLDAPAuthenticationImpl,
  LDAPAuthenticationUtil, ldap.server.port=636).
- Detail owned by agent 08.

## 9. Code-quality hotspots & technical debt

1. **God classes**: NursingServiceImpl (300 endpoints, 17,350 LOC), PathoOrderServiceImpl (133, 14,280), MasterCachedServiceImpl (264, 12,710), InventoryServiceImpl (235, 12,070) — single file = single tx boundary = contention and unmaintainability.
2. **No controller/service separation** — JAX-RS classes are routers + business logic; helpers partially
   mitigate but many services still call genericDAO directly inside endpoint methods.
3. **Native SQL + stored procedures mixed with JPQL** — GenericQuery hides the dialect; stored-proc-backed
   *ResultDto endpoints are DB-coupling hotspots (BloodBank, Radiology, Patient services).
4. **Commented-out endpoints & dead code** — significant (commented @Path blocks, disabled jobs, alternate
   cache implementations, commented EHRSyncJobs/Hl7InboundMessageJob/AutoNextDayAllocation).
5. **String-concatenated native SQL** with boolean formatting helper (getBooleanValueForQuery) — SQL injection
   surface if any param is unsanitized; values() params are bound elsewhere.
6. **Error codes as opaque strings** ("BEXA003") resolved from resource bundles — new system must retain codes.
7. **Print/report endpoints return raw HTML String / byte[]** — rendering (Velocity templates + jasperreports)
   is embedded in the API tier; ~60+ such endpoints.
8. **Long transactions in batch jobs** (ApplicationJobs 14 @Transactional methods) — lock contention risk.
9. **FhirServer bean created without servlet registration** — dormant FHIR R4 surface or externally wired.
10. **Dual-version ABDM paths + catch-all routing** (ABDMController) — fragile callback routing.
11. **Repository scale**: dto/ has 1,583 files and views/ 155 — enormous DTO surface, much of it
    stored-proc-result-coupled, will dominate migration effort.

## 10. Migration-relevant numbers

- 3,003 REST endpoints, 132 service classes, 65 XML beans + 96 @Component beans (65 REST).
- 1,056 endpoints @Transactional; 1,445 @Transactional annotations total.
- 5 job classes / ~55 schedules; 2 DBs in config history (Oracle active; SQL Server unit commented).
- 26 custom annotations; 3 aspects/filters in aspects/; 5+ servlet filters; 2 CXF interceptors.
- Non-REST: 1 FHIR server (dormant flag), ~50 ABDM callback endpoints (v2+v3), 18 HFR endpoints, 10 HCX endpoints.
