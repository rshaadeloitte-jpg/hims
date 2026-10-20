# iHMS / OHUM PAS NG — System Landscape (AS-IS)

> Produced by Agent 01 (Repository & Architecture Cartography). Evidence dump: `_discovery/agent-01/findings.md`.
> Repo: `D:\ihms`, branch `master`, ~23,882 files / ~1.78 GB. Research-only; no source modified.

---

## 1. Executive Summary

iHMS ("ohumpasng" v3.4) is a hospital management information system (HMIS) — an evolution of the DOCIT product line — deployed for the Government of Rajasthan ("RISL" overlay: e-Mitra, SSO, Jan-Aadhaar, Bhamashah, ABDM/NHCX, e-Aushadi integrations). It is a **single-module, Spring Boot 2.7.5 / Java 8** monolith that runs by default as a **self-executing JAR with embedded Tomcat**, with a build script variant producing a **WAR for external Tomcat (JNDI datasource)** used by the RISL production overlay.

The application blends three architecture generations:
1. **Legacy XML-Spring + CXF JAX-RS** — `applicationContext.xml` / `services.xml` wire ~137 REST service beans into a single CXF `jaxrs:server` at `/rest` (mounted under `/api/*` via CXFServlet), with EclipseLink JPA (static-weaved) over Oracle.
2. **Spring Boot conveniences** — programmatic `@Bean` config in `App.java`, `SpringBootServletInitializer`, `application.properties`, logback-spring, scheduled/async executors.
3. **FAUG "framework"-driven frontend** — a ~8,700-file static SPA (schema/template JSON + 71 flat JS controllers) served from `classpath:/public/`, talking REST to the CXF tier.

Notable oddities: a `docit.ctrl` file-based **watchdog kill-switch** in `main()`, a WAR-build variant overlay (`build_war.sh` + `artifacts/risl/`), an in-repo Node.js "rules engine" (`ruler/`), and committed runtime logs (up to 118 MB/day).

---

## 2. Technology Stack

| Layer | Technology | Version | Evidence |
|---|---|---|---|
| Language | Java (javax.* namespace; Jakarta bridge added for WAR) | 1.8 | pom.xml:14 |
| Framework | Spring Boot (starter-parent) | 2.7.5 | pom.xml:31-35 |
| Web/REST | Apache CXF JAX-RS (+ JAX-WS dep present) | 3.1.0 | pom.xml:296-322 |
| MVC (static) | Spring WebMvc (ResourceHandler only) + CXFServlet `/api/*` | — | App.java:130-151 |
| Persistence | **EclipseLink JPA** (Hibernate excluded) | 2.6.4 | pom.xml:376-379, 532-545 |
| Weaving | EclipseLink staticweave-maven-plugin | 1.0.0 | pom.xml:171-190 |
| Database | Oracle (ojdbc7 12.1.0); MySQL driver also on classpath; PostgreSQL dialect switch exists in code | 12.1.0 | pom.xml:819-823; App.java:180-184 |
| DB logic | 719 generated Oracle stored procs (`generated-procs/`) | — | dir listing |
| Cache | In-app `MemoryCacheImpl` (default; Redis/RedisCluster/Hazelcast/Ignite variants commented) | — | applicationContext.xml:34-66 |
| External cache | Redis (jedis; standalone or cluster) + spymemcached, ehcache, hazelcast deps | — | application.properties:109-153 |
| Search | Solr Cloud (spring-data-solr 4.3.15, `CloudSolrClient`) | — | pom.xml:842-847; App.java:249-253 |
| Object storage | MinIO (disabled by default) | 5.0.2 | pom.xml:852-856; application.properties:750 |
| HL7 | HAPI base + structures v25 | 2.2 | pom.xml:567-576 |
| FHIR | HAPI-FHIR (DSTU2/DSTU3/R4) + custom `xcg.genserv.FhirServer` | 5.7.0 | pom.xml:909-974 |
| ABDM | `com.ohum:abdm-client` (private Artifactory), bouncycastle, nimbus-jose-jwt | 3.0.0-SNAPSHOT | pom.xml:935-997 |
| Templates/Print | Apache Velocity (.vm) + JasperReports + openhtmltopdf + qz-tray client signing | 1.7 / 6.0.3 | pom.xml:391-395, 583-600; templates/ |
| Auth | Spring Security config (permit-all + CSP) + LDAP/AD (UnboundID) + SSO (Rajasthan) + custom auth impls | — | SpringSecurityConfig.java; application.properties:402-518 |
| Scheduling | Spring `@EnableScheduling`/`@EnableAsync`; cron-driven jobs (HL7 inbound, EHR sync, e-Aushadi, WhatsApp retrigger…) | — | App.java:77-78; application.properties:307-1503 |
| Frontend | FAUG framework (faug-min.js) + jQuery 3.x, Bootstrap, highstock, fullcalendar, tinymce, fabric.js… | — | index.html inventory |
| Mobile | ohumlite (in-repo FAUG app); patportal + apps/* (Cordova-era scaffolds, gitignored) | — | .gitignore |
| Build | Maven + scripts (`build_war.sh`, `clean.sh`, `create.sh`); Sonar scanner; RESTdocs/asciidoctor | — | pom.xml |
| Ops extras | Node rule engine (`ruler/`, HTTP+ioredis); Python graph/label tooling (scratch) | — | ruler/, graphify-out/ |

---

## 3. Build & Deployment Model (AS-IS)

### 3.1 Two packaging modes
1. **JAR (default, dev/QA)** — `com:ohumpasng:3.4`, `<packaging>jar</packaging>`, spring-boot-maven-plugin repackage, embedded Tomcat via `spring-boot-starter-tomcat`. `App` extends `SpringBootServletInitializer` so it is WAR-compatible without code changes.
2. **WAR (RISL production variant)** — produced by `build_war.sh`: temporarily overlays `artifacts/risl/{pom.xml, App.java, persistence.xml, application.properties, public/*}` onto the root tree, runs `mvn package -DskipTests`, restores originals. The risl `App.java` uses **JNDI datasource lookup** (`jdbc/orcl`) + `PropertySourcesPlaceholderConfigurer`; its pom adds `jakarta.persistence-api 2.2.3`, `tomcat-el-api`, `javax.ws.rs-api` for external Tomcat 8/9.

### 3.2 Runtime shape
- Port **8080**, context path **`/IHMS`** (application.properties:3-4).
- **CXFServlet at `/api/*`** (App.java:149-151) hosts the CXF `jaxrs:server` (`address="/rest"`) → all REST: `/IHMS/api/rest/**` (matches `orig.api.path.prefix`, application.properties:371).
- Static SPA served from `/**` handler → external `static.resources.path` + `classpath:/public/` (App.java:130-137).
- Datasource: QA default `jdbc:oracle:thin:@//10.10.7.63:1521/orcl` (application.properties:77); JPA unit `ORCL` (JTA) in META-INF/persistence.xml for JNDI mode.
- **Watchdog**: `main()` writes `docit.ctrl`; polls every 5 s; deleting the file triggers clean shutdown (App.java:273-286).
- Logging: logback-spring with 3 appenders; rolling 200 MB/day; MDC fields meth/uri/corr/rip.

### 3.3 Infrastructure dependencies (from application.properties)
Oracle DB · Redis (standalone `oracle.sms:6379` or 6-node cluster) · Solr Cloud (zk 10.10.5.151-153; collections patient/encounter/item/service/icd10cm/healthfactor `*_risl_qa`) · LDAP/AD (10.10.5.71:636) · MinIO (optional) · External APIs: e-Mitra, Rajasthan SSO, Jan-Aadhaar, Bhamashah, eSanchar SMS/OTP/WhatsApp, e-Aushadi/RMSC, RGHS, Chiranjeevi, eRaktkosh, PCTS, EMR/EHR (openMDWS), ABDM sandbox (gateway/CM/HIE-CM/HFR/HPR/PHr/ABHA), score engine, print server (ws://localhost:8182), MDM server, offline sync server (localhost:8090).

### 3.4 Build pipeline & tooling
- `clean.sh`: prunes git branches older than 1 month.
- `create.sh`: assembles `ohumpas-pentaho-int.jar` from compiled classes (Pentaho integration; macOS dev path).
- `scripts/` + `src/main/resources/compress/`: Node tooling to compress/validate FAUG schemas & templates; `ruler/`: Node rule-engine server.
- `target/` holds only `classes/` + `test-classes/`; tests default to skip (`junit.skipTests=true`). `*.war`/`target` gitignored.

---

## 4. Spring Context Architecture (AS-IS)

```
com.ohum.App (Spring Boot main, @SpringBootApplication)
 ├─ @ComponentScan("com.ohum")                     → @Service/@Component beans (services, util, interceptors…)
 ├─ @ImportResource:
 │    ├─ classpath:META-INF/cxf/cxf.xml            → CXF default bus (from cxf-core JAR, not in repo)
 │    └─ classpath:applicationContext.xml          → <cxf:bus/> + bean cacheImpl (MemoryCacheImpl)
 │          └─ <import resource="services.xml"/>   → jaxrs:server "restServices" @ /rest
 │                ├─ 137 serviceBean refs (67 defined in XML; remainder component-scanned)
 │                ├─ inInterceptors: PreProtocolInterceptor
 │                └─ providers: RequestResponseInterceptor, ServiceJAXRSExceptionMapper,
 │                             JAXBElementProvider, JacksonJsonProvider(customJacksonMapper),
 │                             NullableLongParamConverterProvider
 ├─ programmatic @Beans (override/add XML):
 │    entityManagerFactory  — EclipseLinkJpaVendorAdapter, packages com.ohum.models/.dto, shared-cache NONE
 │    transactionManager    — JpaTransactionManager
 │    taskExecutor(20)/taskScheduler(10) — @Async/@Scheduled support
 │    velocityEngine        — file loader → templates/ (.vm)
 │    corsFilter            — /rest/** origins
 │    solrClient            — CloudSolrClient (cond. spring.data.solr.enabled)
 │    genFhirServer         — HAPI FHIR server (cond. abdm.enabled)
 │    restrictive           — LDAP-based password validator (cond. auth.pwd.manager=restrictive)
 ├─ CXFServlet /api/*  →  REST endpoints at /api/rest/**
 └─ SpringSecurityConfig (SecurityFilterChain):
      permitAll + CSP/HSTS headers, CSRF off, session NEVER, formLogin
```
Notes:
- `persistenceContext.xml` (unit `ZDocit-HIS-SQL-Server-PU`) is **not imported** by applicationContext.xml; its role at runtime is UNKNOWN — the EMF comes from `App.java`.
- The XML-first service layer (67 beans) and component-scanned services (~155 files in `services/`) are merged in the same CXF server; dedup is by bean id convention `*ServiceImpl`.
- Security is layered outside Spring Security: custom auth (DefaultAuthenticationImpl / CustomLDAPAuthenticationImpl), SSO filter (`interceptors/sso/SSOAuthFilter`), FHIR RBAC/token interceptors (`xcg/genserv/interceptors/`), `security.allow.html.apis`. Agent 08 owns the deep dive.

---

## 5. Frontend Asset Map

| Area | Content |
|---|---|
| Entry pages | index.html (SPA shell, 120 lines), ihms-sso.html, ihms-no-user.html |
| Framework | faug-min.js + faug-ext.js + faug-ext-globalize.js + ext.js + main.js; ~59 scripts / 35 css in index.html (jQuery, Bootstrap 3, fullcalendar, highstock, tinymce, select2, jstree, fabric.js, pdf.js, qz-tray, BrowserPrint, aadhaarauth) |
| UI config | `fjs-config.json` (126 KB master) + 40 `fjs-config_<module>.json` (42 files on disk inc. `fjs-config_schemas.json`/`templates.json` 29-byte stubs → 40 real modules) |
| Controllers | `public/js/` — 71 flat JS files (admission-, billing-, pharmacy-, blood-bank-module.js…) |
| Modules | `public/modules/` — 40 dirs × {schemas/*.json, templates/*.html} = 4,255 files (billing, inventory, pharmacy, op, ip, ot, pathology, radiology, bloodBank, nursing, masters, rmrs, rcm, nhcx…) |
| Business config | tiuxml_mapping.json (+chemo/op/nhnc variants), tiuxml.json, mastersConfigration.json, vitals*.json, notesTitle.json, ODPartoGraphConfig.json |
| Mobile | `ohumlite/` — self-contained offline FAUG app ("iHMS"; 13 js controllers incl. abdm-, appointment-token-flow-modules); `patportal/` — **empty (gitignored)**; `apps/` — mobile scaffolds, gitignored (only `apps/ohumlite_resources/` incl. `JanSwasthya.keystore` signing key present) |
| i18n | `resources/lang/` ICU bundles (en_IN, en_Mt, pl) + `locales/cldr/` (CLDR en/en-IN/en-MT/hi) |
| Other | charts/, data/, fonts/, images/, jsonFile/, layer/, queue/, resources/ (vendor), testdata/ (excluded from jar), tiuxd/ |

REST contract: frontend calls `/IHMS/api/rest/**` (CXF), JSON via Jackson (custom mapper), extension-mapped json/xml.

---

## 6. Non-Java Source Areas

| Area | Size | Purpose |
|---|---|---|
| generated-procs/ | 719 .sql | Oracle stored procs (CIMS_*, HL7_*, RPT*, util funcs) — de-facto DB business layer |
| templates/ | ~180 .vm + nhcx/, risl/, templates/ | Velocity print/report templates (receipts, forms, certificates, labels) |
| integration/ | tiuxml (schemas/templates/mapping/PrescriptionFiles), scripts/api.js | Instrument & HL7-style XML (TIUXML) integration payloads |
| artifacts/risl/{solr,redis,server}-scripts | shell | Solr per-node sync (3 node groups), status/restart, redis/server health checks |
| ruler/ | Node.js | Rules engine server (HTTP + ioredis) at rules.engine.url |
| scripts/ + resources/compress/ | Node.js | FAUG schema/template compression & validation tooling |
| locales/cldr | JSON | CLDR locale data |
| .github/java-upgrade | PS1/sh hooks | Java-upgrade tool-call recording harness |

---

## 7. Open Questions & Risks

1. `persistenceContext.xml` runtime role — UNKNOWN (see §4).
2. `patportal/` mobile app absent from repo (gitignored) — UNKNOWN contents.
3. Plaintext secrets across application.properties and pom.xml (Sonar token, DB/AD/mail/SMS/ABDM creds) — high-risk for any shared/enhanced environment.
4. Committed runtime logs in `logs/` (~500 MB of error logs, up to 118 MB/day) and scratch artifacts (graphify-out/, labels.*, python scripts, keys `my.key`/`qzPrivateKey.pem`/`digital-certificate.txt`) — housekeeping + secret-hygiene concern.
5. DTO model is enormous (1,583 files) vs entities (950) — likely a large JAXB/JSON serialization surface; agent 04 territory.
6. CXF 3.1.0 is very old (2015) for Spring Boot 2.7.5; jakarta bridge deps only in WAR variant.
7. `fjs-config_schemas.json` / `fjs-config_templates.json` stubs (29 bytes) suggest a tooling step (compress.js) regenerates them at build time — verify before touching frontend assets.

---

## 8. Suggested Next Actions (for downstream agents)
- **Agent 02/03 (build & CI):** replicate JAR + WAR (build_war.sh) builds; capture exact dependency tree (`mvn dependency:tree`); note Artifactory at 10.10.5.94 as non-public repo.
- **Agent 04 (Java deep dive):** start with `services/` (155) + `dao/` (GenericDAO) + `models/` (950) + EMF config (App.java:173-193); beware static-weave requirement.
- **Agent 08 (security):** SpringSecurityConfig.java:105-122 (permitAll), SSOAuthFilter, TokenBasedSecurityInterceptor/RbacSecurityInterceptor, LDAP auth, plaintext secrets.
- **Frontend/UX agents:** FAUG framework (faug-min.js) + fjs-config module graph; 71 controllers; 40 module dirs.
