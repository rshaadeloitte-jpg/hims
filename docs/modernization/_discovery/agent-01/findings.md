# AGENT-01 Findings — Repository & Architecture Cartography

Source repo: `D:\ihms` (git branch: `master`). Research-only pass; no code modified.

---

## 1. Maven Build

### 1.1 `pom.xml` (root, D:\ihms\pom.xml) — 999 lines
- GAV: `com:ohumpasng:3.4`, **`<packaging>jar</packaging>`** (pom.xml:6-9)
- Parent: `spring-boot-starter-parent 2.7.5` (pom.xml:31-35)
- Java target: `1.8` (pom.xml:14, compiler plugin source/target 1.8 at :117-123)
- `start-class`: `com.ohum.App` (pom.xml:21)
- Repos: Maven Central + private Artifactory `http://10.10.5.94:8082/artifactory/ohum-mvn-repo` (pom.xml:43-51)
- Sonar: host `http://10.10.5.70`, sources include `artifacts/risl/src/main/java` and `src/main/resources/public/js` (pom.xml:22-26)

### 1.2 Plugins (pom.xml)
| Plugin | Version | Phase | Line |
|---|---|---|---|
| sonar-maven-plugin | 3.4.0.905 | — | pom.xml:112-115 |
| maven-compiler-plugin | (default) | — | pom.xml:116-123 |
| maven-surefire-plugin | 2.19.1 (+junit-platform-surefire-provider 1.1.0, junit-jupiter-engine 5.0.1) | test (skipped by default `junit.skipTests=true`, :19) | pom.xml:124-144 |
| spring-boot-maven-plugin | (managed 2.7.5) | repackage | pom.xml:145-148 |
| maven-resources-plugin | (default) — non-filtered ext: js, otf, eot, svg, ttf, woff, ico, csv, json, xls, xlsx, jrxml, png | — | pom.xml:149-170 |
| **staticweave-maven-plugin** (EclipseLink) | 1.0.0, weave goal bound to `process-classes`, uses org.eclipse.persistence.jpa 2.6.4 | process-classes | pom.xml:171-190 |
| asciidoctor-maven-plugin | 1.5.6, backend html, doc `index.adoc` → `target/generated-docs`; dep spring-restdocs-asciidoctor 2.0.1.RELEASE | prepare-package | pom.xml:191-217 |
| gatf-plugin (com.test:gatf-plugin 1.8, REST→Postman generator) | **commented out** | — | pom.xml:78-110 |

### 1.3 Dependencies of note (pom.xml)
- **JPA**: `spring-data-jpa` **excludes hibernate-entitymanager** (pom.xml:366-374); `spring-boot-starter-data-jpa` **excludes hibernate-entitymanager AND hibernate-core** (pom.xml:532-545); explicit `org.eclipse.persistence:org.eclipse.persistence.jpa:2.6.4` (pom.xml:376-379) → **EclipseLink is the JPA provider; Hibernate is excluded**.
- **CXF 3.1.0**: cxf-rt-rs-service-description, cxf-rt-frontend-jaxws, cxf-rt-frontend-jaxrs, cxf-rt-transports-http (pom.xml:296-322)
- **Spring Boot**: starter, starter-tomcat (embedded), starter-web, starter-mail (pom.xml:520-531, 753-756)
- **Spring Data**: redis (+ jedis), solr 4.3.15, spring-data-commons; repos disabled at runtime (application.properties:1282-1286)
- **Databases**: `com.oracle:ojdbc7:12.1.0` (pom.xml:819-823) and `mysql:mysql-connector-java` (pom.xml:904-907); c3p0 0.9.5-pre5 (pom.xml:385-389)
- **Reporting/Print**: jasperreports 6.0.3, commons-digester 2.1, itext 1.3.1, openhtmltopdf 1.1.24, barcode4j, zxing, qrgen (pom.xml:583-620, 758-814)
- **HL7/FHIR**: hapi-base 2.2, hapi-structures-v25 2.2 (pom.xml:567-576); HAPI-FHIR 5.7.0 (jaxrsserver-base, structures DSTU2/DSTU3/R4, validation, client, server, base; org.hl7.fhir.core 5.6.971) (pom.xml:909-974)
- **ABDM**: `com.ohum:abdm-client:3.0.0-SNAPSHOT` from ohumrepo (pom.xml:935-939); bouncycastle bcprov-jdk18on 1.72 (pom.xml:941-944); nimbus-jose-jwt 9.37.3 (pom.xml:993-997)
- **Cache**: ehcache-core 2.6.8 + ehcache-web, hazelcast + hazelcast-spring, spymemcached 2.10.3, kryo 5.1.1 (pom.xml:497-518, 825-834)
- **Template**: Velocity 1.7 + velocity-tools 1.4/2.0 (pom.xml:391-395, 772-781), Thymeleaf + thymeleaf-spring5 (pom.xml:975-982)
- **Excel/CSV**: POI 5.0.0/poi-ooxml-schemas 4.1.2/xmlbeans 5.0.1, opencsv 2.0 & 5.5 (pom.xml:436-461, 930-934)
- **Other**: minio 5.0.2, tika-core 1.22, okhttp3, jedis, unboundid-ldapsdk, libphonenumber suite, icu4j 57.1, zip4j, joda-time 2.12.1, json-simple, jsoup, reflections 0.9.9, xstream 1.4.5, thumbnailator, tomcat-jasper-el 8.0.15, jasper-jdt 6.0.29 (pom.xml passim)
- **Test**: spring-boot-starter-test (junit excluded), junit-jupiter api/engine, junit-platform-launcher, spring-restdocs-mockmvc, mockito-core (pom.xml:868-898)

### 1.4 Shell scripts (repo root)
- `build_war.sh` (20 lines): **swaps in a RSL/WAR build variant** — copies `artifacts/risl/pom.xml`, `artifacts/risl/src/main/java/com/ohum/App.java`, `artifacts/risl/persistence.xml`, `artifacts/risl/application.properties` and `artifacts/risl/src/main/resources/public/*` over the root files, runs `mvn package -DskipTests`, then restores the originals and deletes `src/main/resources/webapp` (build_war.sh:1-19). I.e., the repo is a JAR app by default; WAR packaging is produced by temporarily overlaying the `artifacts/risl` variant.
- `clean.sh` (7 lines): deletes stale local git branches older than 1 month (local + `git push origin --delete`).
- `create.sh` (29 lines): dev utility — assembles a `ohumpas-pentaho-int.jar` from selected compiled classes (cache, views, dto.pas, models…) for a Pentaho integration jar (hardcoded macOS path `/Users/sumeetc/Projects/...`; see also `target/` reference).

### 1.5 `artifacts/risl/pom.xml` (1176 lines) — the WAR variant
- Same GAV but **`<packaging>war</packaging>`** (artifacts/risl/pom.xml:9)
- Adds jakarta/javax bridge deps for external Tomcat: jakarta.annotation-api, tomcat-el-api, javax.ws.rs-api, jakarta.persistence-api 2.2.3, jakarta.transaction, javax.inject (artifacts/risl/pom.xml:151-262)
- Its `src/main/java/com/ohum/App.java` (262 lines vs 287 in root) replaces datasource with `JndiDataSourceLookup` + `PropertySourcesPlaceholderConfigurer` (@Primary) — i.e., **WAR deployment expects a JNDI datasource (`jdbc/orcl`, per risl persistence.xml) from external Tomcat**.
- `artifacts/risl/persistence.xml` + `application.properties` (root of artifacts/risl) define the JNDI persistence unit.
- The risl `public/` overlay is sparse: data/, images/, resources/, tiuxml_mapping.json (2 files + App.java total).

---

## 2. Spring Contexts (src/main/resources)

### 2.1 `applicationContext.xml` (70 lines)
- Only active bean: **`cacheImpl` = `com.ohum.cache.MemoryCacheImpl`** (expiry 86400s) (applicationContext.xml:43-45). Alternatives commented out: `RedisClusterCacheImpl`, `RedisCacheImpl`, Hazelcast, Ignite (:34-66).
- Declares `<cxf:bus/>` (:30-32) and imports `services.xml` (:68).
- Schema namespaces hint at legacy content (mongo, security, hazelcast) that is no longer used.

### 2.2 `persistenceContext.xml` (62 lines)
- Defines `entityManagerFactory` = `LocalContainerEntityManagerFactoryBean`, unit name `ZDocit-HIS-SQL-Server-PU` (persistenceContext.xml:26-33). Legacy commented alternatives: HibernateJpaVendorAdapter for MySQL (:35-43), JpaTransactionManager + AOP tx advice on `com.ohum.dao.GenericDAO` (:45-60).
- **NOTE**: the runtime `entityManagerFactory` bean is actually overridden by the `@Bean(name="entityManagerFactory")` in App.java:173-193 (EclipseLinkJpaVendorAdapter, packages `com.ohum.models`, `com.ohum.dto`). Whether persistenceContext.xml is even loaded is UNKNOWN (it is not imported by applicationContext.xml; see open questions).

### 2.3 `services.xml` (271 lines) — CXF JAX-RS wiring
- `jaxrs:server id="restServices" address="/rest"` (services.xml:93)
- **67 `<bean id=...>` definitions** (services.xml:27-92) + 2 later (requestResponseInterceptor :257, jaxbProviderSoap :259, jacksonProvider :261, jaxrs-data-binding :267 — actually 71 bean defs total; ids counted 67 in first block + 4 later)
- **137 `<ref bean=...>` entries inside `<jaxrs:serviceBeans>`** (services.xml:94-228) — many refs point to beans NOT defined in this file (doctorServiceImpl, tariffServiceImpl, nursingServiceImpl, pharmacyServiceImpl, patientPortalServiceImpl, inventoryServices, errorPathHandler, mdmServiceImpl, aasmaan*, ejs*, etc.) → supplied by @Component/@Service scanning (`@ComponentScan(basePackages="com.ohum")` App.java:71) — the XML list and the component-scanned list are complementary.
- Service bean ids (first block): defAuthImpl, customLDAPAuthImpl, authenticationServiceImpl, equipmentServiceImpl, organismMasterServiceImpl, patientServiceImpl, quotationServiceImpl, refundServiceImpl, billServiceImpl, itemSalesServiceImpl, prescriptionMasterServiceImpl, departmentScheduleServiceImpl, appointmentServiceImpl, userManagementServiceImpl, patientRequisitionServiceImpl, iPDConfigServiceImpl, helperServiceImpl, drugServiceImpl, bedReservationServiceImpl, companyApprovedAmountServiceImpl, patientBillEstimationServiceImpl, surgeryDetailsServiceImpl, purchaseOrderServiceImpl, paymentServiceImpl, oTDetailsServiceImpl, visitServiceImpl, complaintDetailsServiceImpl, otConfigServiceImpl, itemStockServiceImpl, itemCategoryServiceImpl, planMasterServiceImpl, iPDServiceImpl, iPDAdmissionServiceImpl, masterServiceImpl, chargeServiceImpl, serviceMasterServiceImpl, bedServiceImpl, oTSchedulerServiceImpl, concessionTemplateServiceImpl, preventiveMaintenanceServiceImpl, materialConsumptionServiceImpl, packageServiceServiceImpl, refEntityServiceImpl, expiryItemServiceImpl, bloodTestMasterServiceImpl, radiologyServiceImpl, itemMasterServiceImpl, uploadFileServiceImpl, openingBalanceServiceImpl, cwfServiceImpl, paymentTransferServiceImpl, ejsIncentiveServiceImpl, integrationServiceImpl, rcmserviceimpl, queueManagmentServiceImpl, clinicalServiceImpl, uploadHelperServiceImpl, machineConfigServiceImpl, eHRUtilServiceImpl, abhaServiceImpl, abdmController (ABDMController), hfrController (HFRController), nhcxServiceImpl (services.xml:27-92)
- Providers/interceptors: `PreProtocolInterceptor` (in, :234-236), `RequestResponseInterceptor` (:257), `ServiceJAXRSExceptionMapper` (:239), JAXBElementProvider (:259), JacksonJsonProvider w/ customJacksonMapper (:261-265), NullableLongParamConverterProvider (:242). WadlGenerator commented out (:243-253).
- Extension mappings: json→application/json, xml→application/xml, *→application/json (:229-233).

### 2.4 `logback-spring.xml` (53 lines)
- 3 appenders: CONSOLE, FILE (rolls, denies ERROR), ERROR_FILE (ThresholdFilter ERROR). Rolling: SizeAndTimeBased, 200MB/file, 30 history, 4GB cap. Pattern with MDC keys meth/uri/corr/rip. Root INFO (logback-spring.xml:8-52). Overridable via `app.logging.*` properties.

### 2.5 Bootstrap / config chain
- `App.java` (src/main/java/com/ohum/App.java, 287 lines) — the Spring Boot entry:
  - Annotations: @ServletComponentScan, @EnableAspectJAutoProxy, @SpringBootApplication, @ComponentScan("com.ohum"), @EnableTransactionManagement, @EnableAutoConfiguration, @Configuration, **@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:applicationContext.xml"})** (:75), @PropertySource("file:application.properties") (:76), @EnableAsync, @EnableScheduling (:77-78)
  - `extends SpringBootServletInitializer implements WebMvcConfigurer` (:79) → runnable both as embedded-Tomcat JAR and as WAR in external Tomcat
  - CXFServlet registered at **`/api/*`** (:149-151) → REST reachable at `/api/rest/**` (server address `/rest`); confirmed by `orig.api.path.prefix = /IHMS/api/rest` (application.properties:371)
  - Static resources: `addResourceHandlers("/**")` → `staticResourcesPath` (file:) + `classpath:/public/` (:130-137)
  - Beans: PasswordValidator (LDAP-based "restrictive" vs no-op, :139-145), entityManagerFactory (EclipseLinkJpaVendorAdapter; DB dialect Oracle vs PostgreSQL chosen by driver name; shared cache NONE; packages com.ohum.models, com.ohum.dto; :173-193), transactionManager (JpaTransactionManager, :195-201), velocityEngine (file loader → `vm.template.path`=D:/RISL/ihms, :203-211), corsFilter (`/rest/**`, from `cors.allowed.endpoints`, :213-227), taskExecutor (pool 20 / queue 4500, wrapped in UserTrxContextHolder.ContextAwareExecutorDecorator, :229-238), taskScheduler (10 threads, :240-247), solrClient (CloudSolrClient from `spring.data.solr.zk-host`, conditional, :249-253), genFhirServer (HAPI FhirServer, conditional on `abdm.enabled`, :259-264)
  - `main()`: verifies KeyAgreement (ECDH), runs app, then **creates a `docit.ctrl` file and polls its existence every 5 s; when deleted, closes context and exits** — a watchdog kill-switch mechanism (:266-286). `docit.ctrl` is gitignored.
  - `classpath:META-INF/cxf/cxf.xml` — NOT present in repo (`Test-Path` false). Supplied by the CXF jar itself (default CXF bus config in cxf-core). The app's own `<cxf:bus>` in applicationContext.xml additionally customizes it.
- `application.properties` (repo root, 1506 lines) — loaded as external file: `@PropertySource(value = "file:application.properties")` (App.java:76). Contains QA DB creds (10.10.7.63/orcl, user IHMSDOCITDEVMODS_PROD_V1), Redis standalone (`oracle.sms:6379`), Solr zk 10.10.5.151-153, LDAP/AD (10.10.5.71:636), ABDM sandbox creds, e-Mitra/SSO/Jan-Aadhaar/eSanchar/e-Aushadi/RGHS/Chiranjeevi/eRaktkosh/PCTS endpoints, minio, schedules (cron), print/template paths. **Contains many secrets in clear text** (mail, LDAP, ABDM, SMS, minio keys) — flag for security review (application.properties:31-33, 448, 754-756, 1108-1124, 1162-1172, 1482).
  - Notable flags: `spring.autoconfigure.exclude=...UserDetailsServiceAutoConfiguration` (:1290); `spring.jpa.open-in-view=false` (:5); FHIR: `fhir.packages=ndhm.tgz`, `fhir.namespaces=genserv ndhm abdm`, `fhir.clients=ndhm abdm`, `fhir.server.egypt=true` (:1140-1150); `abdm.enabled=true`, version v3 (:1156, 1216)
- `src/main/resources/META-INF/persistence.xml` (11 lines): single unit `ORCL`, EclipseLink provider, JTA `jdbc/orcl`, exclude-unlisted=false, shared-cache NONE — used for external-Tomcat JNDI mode; in embedded mode App.java builds the EMF programmatically.
- `SpringSecurityConfig.java` (src/main/java/com/ohum/SpringSecurityConfig.java, 124 lines): @EnableWebSecurity, SecurityFilterChain; permits all `/**` with `anyRequest().authenticated()` effectively unreachable; **CSRF disabled, sessions NEVER**, CSP headers, HSTS, formLogin only (:105-122). NOTE only existence/role here — deep dive owned by agent 08 (see also interceptors/sso/SSOAuthFilter.java, xcg/genserv/interceptors/{TokenBasedSecurityInterceptor,RbacSecurityInterceptor}.java).

---

## 3. Directory Trees with Responsibilities

### 3.1 Repo root (52 entries) — non-Java code
```
D:\ihms\
├── pom.xml / application.properties / persistence.xml     # root config (dev/JAR mode)
├── build_war.sh / clean.sh / create.sh                    # build & hygiene scripts
├── src\main\java\com\ohum\...                             # 3001 Java files (see 3.2)
├── src\main\resources\                                     # see 3.3
├── artifacts\
│   ├── risl\                  # WAR variant: pom.xml, App.java, persistence.xml,
│   │   │                        application.properties, public overlay, ihms_online\
│   │   │                        (config.xml, config_docit.xml, network_security_config.xml),
│   │   │                        wstx\, solr-scripts\ (sync\scripts_node{1,2,3}\ sync.sh,
│   │   │                        status\check.sh, restart-cron\zkslr_ctl.sh),
│   │   │                        redis-scripts\check.sh, server-scripts\check.sh
│   ├── migration-scripts\      # MigratePatientAdmissionUtil.java, MigratePatientRegistrationUtil.java, UpdateLdapID.java
│   ├── Equipment Interface\    # Backman_Access_DxH 500\OLIS_1.0.jar (lab instrument jar)
│   ├── eclipse-java-google-style.xml, google-style-guide-javascript-eclipse.xml  # code style
├── generated-procs\             # 719 .sql files (Oracle stored procs, CIMS_* naming)
├── templates\                   # 180+ Apache Velocity (.vm) print/report templates + subdirs nhcx\, risl\, templates\
├── integration\
│   ├── tiuxml\{PrescriptionFiles, schemas, templates, testdata, tiuxmlmapping}
│   ├── scripts\api.js
│   ├── reports\  (empty)        # int.files.path=D:/RISL/ihms/integration (application.properties:375)
├── locales\cldr\                # ICU/CLDR locale data (main\{en,en-IN,en-MT,hi}, segments, supplemental)
├── scripts\                     # Node.js tooling: compress.js, prepare_tiuxml_templates.js,
│   │                              validate_artifacts.js, balanced_braces_check.js, html2text.js,
│   │                              master-repl.js, updateRoute.js, find_duplicates.js, package.json …
├── ruler\                       # Node "Rules Engine" (Ruler.js, run.sh, package.json) — HTTP server + ioredis
│   │                              (PROD: node ./Ruler.js localhost 8000 q@1@preuat <redis-cluster>) 
├── graphify-out\                # pre-existing graph analysis output (graph.json/html, GRAPH_REPORT.md,
│   │                              functional_flow_appointment_opd_cwf.md, cache\, converted\, .graphify_python)
├── logs\                        # Runtime log files committed/left in repo: IHMS.log + IHMS-error.log
│   │                              + rotated daily archives (Mar 31 2026 – Apr 29 2026); error logs up to ~118 MB
├── .github\
│   ├── copilot-instructions.md
│   └── java-upgrade\{.gitignore, hooks\scripts\{recordToolUse.ps1, recordToolUse.sh}}  # upgrade harness hooks
├── apps\ohumlite_resources\     # mobile app resources: res\, icon\, screen\, JanSwasthya.keystore, README.md
│                                  (other apps/* dirs are gitignored: docit, ihmsOnline, ihms, ohumlite, ohumlife, diabye, patportal, ostat, ohumg…)
├── .metadata\, .settings\, .vscode\, .serena\, .agent\, .gitnexus\  # IDE/agent tooling (Eclipse workspace, vscode, serena, gitnexus)
├── labels.* / All_Labels_*.csv / ExistingHiLabels.ser      # label/i18n extraction artifacts (Python tooling)
├── ast_extract.py, build_graph.py, detect.py, finalize.py, generate_html.py,
│   label_communities.py, summary.py, trace.py, update_report.py   # Python analysis/scratch tooling (with graphify-out/)
├── docit.ctrl                  # watchdog control file (created by App.main at runtime)
├── my.key, qzPrivateKey.pem, digital-certificate.txt       # keys/certs (qz tray print signing)
└── target\                     # compiled output only: classes\, test-classes\ (no jar/war present — gitignored)
```
Repo stats: ~23,882 files, ~1.78 GB (incl. logs & .git). src/main/resources/public alone: 8,748 files.

### 3.2 `src/main/java/com/ohum` — 3,001 Java files
| Package | Files | Responsibility |
|---|---|---|
| annotations | 27 | custom annotations |
| aspects | 3 | AOP aspects (UserTrxContextHolder etc.) |
| audit | 2 | audit handler/helper |
| cache | 6 | MemoryCacheImpl / RedisCacheImpl / RedisClusterCacheImpl + interfaces |
| constants | 5 | constant classes |
| dao | 2 | GenericDAO-based data access |
| dto | 1583 | DTOs (incl. com.ohum.dto.pas, dto.custom) — bulk of code |
| exceptions | 3 | exception types |
| interceptors | 6 | CXF interceptors + filters (PreProtocol, RequestResponse, CrudOperationsFilter, GzipFilter, sso/SSOAuthFilter, emitra/EmitraRedirectFilter, IdEncryptionUtil) |
| jobs | 5 | scheduled jobs (ApplicationJobs, InventoryJobSchedule, Hl7InboundMessageJob, EHRSyncJobs, AutoNextDayAllocation) |
| mappers | 2 | JAX-RS exception mappers |
| models | 950 | JPA entities (models.masters, models.transactionals) |
| services | 155 | service impls (services.pas, services.ejs, services.abdm, services.ehr, services.payment, services.custom) |
| util | 46 | utilities incl. util.auth (DefaultAuthenticationImpl, CustomLDAPAuthenticationImpl, LDAP-based password validators) |
| views | 155 | view/report DTOs |
| xcg | 31 | FHIR/ABDM (xcg.genserv FhirServer, xcg.abdm.server {ABDMController, HFRController, Aspect}, xcg.abdm.client {HIP/HIU/HPR/PHR/HealthId/NHCX util, hcx subpackage}, xcg.hcx.dto) |

### 3.3 `src/main/resources`
| Dir/File | Contents |
|---|---|
| applicationContext.xml, persistenceContext.xml, services.xml, logback-spring.xml | Spring/CXF/logback wiring (§2) |
| META-INF/persistence.xml | JPA unit ORCL (JTA jdbc/orcl) |
| lang\ | icu.properties, icu_en_in, icu_en_mt, icu_pl, icu_ui_en_in, icu_ui_en_mt (ICU MessageFormat bundles) |
| fhir\ | abdm-constants.properties, abdm.tgz (FHIR package archive, fhir.packages=ndhm.tgz loads from here) |
| fonts\ | DejaVuSans.ttf + fonts.xml |
| compress\ | node JS compression tooling (compress.js, prepare_tiuxml_templates.js, validate_artifacts.js, find_duplicates.js, balanced_braces_check.js) — excluded from packaged jar |
| public\ | 8,748 files — the FAUG single-page frontend (§4) |

### 3.4 `src/main/resources/public` — frontend map
- Entry: `index.html` (120 lines) + `ihms-sso.html`, `ihms-no-user.html`
- Root JS: faug-min.js (framework), faug-ext.js, faug-ext-globalize.js, ext.js, main.js, tiuxml.js, scheduler.js, tables.js, tiles.js, w-api-call.js, w-fetch-file.js, markerjs2.min.js, painterro-1.2.87.min.js, signature_pad.umd.min.js, polyfill.min.js, qz-tray.js (in js/), aadhaarauth.js
- `fjs-config.json` (126 KB) + 41 per-module `fjs-config_<module>.json` files: UI schema/route config consumed by FAUG; master file holds settings (debugEnabled, numApiWebWorkers=3, defaultLanguage=en, defCurrency=INR, layerFile, enableRouteModules…), globals, dependencies. `fjs-config_schemas.json`/`templates.json` are 29-byte stubs.
- Master config JSONs: tiuxml_mapping.json, chemo/op/nhnc_tiuxml_mapping.json, tiuxml.json, mastersConfigration.json, vitalsConfiguration.json, vitalsConfigurationClinic.json, vitalsMasterConfig.json, notesTitle.json, ODPartoGraphConfig.json
- `js\`: **71 flat JS "module" files** (e.g., admission-module.js, billing-module.js, blood-bank-module.js, ChartingApplication.js, pharmacy-module.js…) — the FAUG controllers
- `modules\`: **40 dirs**, each with `schemas\*.json` (input/form schemas) + `templates\*.html` (UI templates) — 4,255 files total. Module list: allocation, bcma, billing, biomedicalwaste, biReport, bloodBank, careplan, charting, chemotherapy, commons, cssd, diet, enquiry, equipments, fileManagement, grievance, inventory, ip, kiosk, linenlaundry, masters, nhcx, nursing, nursingWorklist, op, ot, pathology, patientManagement, paymentTransfer, pharmacy, pmrd, queue, radiology, rcm, reports, rmrs, shifthandover, tiuxmls, transport, worklist
- `ohumlite\`: self-contained mobile/offline FAUG app ("iHMS", own index.html, own fjs-config.json, js\ with 13 modules incl. abdm-module.js, appointment-token-flow.js, billing-module.js; data\, layer\, schemas\, templates\, queue.html, faqs.html). Cordova-era layout (hooks/plugins/platforms dirs excluded from jar & gitignored).
- `patportal\`: **directory is empty/gitignored** (`src/main/resources/public/patportal` in .gitignore; pom excludes app/bin/hooks/node_modules/platforms/plugins/res) — a patient-portal Cordova app scaffold, contents UNKNOWN (not in repo).
- `apps\`: public/apps\ contains offlinedashboard (splash/templates); root `apps\` dir (separate gitignored mobile apps incl. ohumlite_resources with JanSwasthya.keystore signing key) — mostly gitignored.
- `data\`, `jsonFile\`, `layer\`, `queue\`, `images\`, `charts\`, `resources\` (vendor libs incl. bootstrap, jquery, fullcalendar, highstock, select2, tinymce, jstree…), `fonts\`, `tiuxd\`, `testdata\` (excluded from jar), `templates\` (public templates).

### 3.5 index.html script inventory (59 script tags, in load order — top items)
jquery.min.js → bootstrap.min.js → jquery.webui-popover → promise → ua-parser → bxslider → bootstrap-editable → bootstrap-typeahead → **faug-min.js** → numb2words → marker2 → jquery.fileDownload → tinymce → jquery-barcode → webcam → supercal → moment → fullcalendar → timelineScheduler → jstree → bootstrap-select → touchspin → ion.rangeSlider → multiselect → Chart.js → highstock → enscroll → timeago → bootstrap-tagsinput → qrcode → select2 → bootstrap-toggle → fabric → rsvp → sha-256 → qz-tray → sjcl → iframeResizer → tableHeadFixer → BrowserPrint → pdf.js → jquery-ui → html5-qrcode → aadhaarauth → **main.js → ext.js** → html5shiv → respond.
Links: bootstrap.min.css, font-awesome, dataTables.bootstrap, bootstrap-editable, datetimepicker, **faug.css**, app.css, icons.css, calendar styles, navigation.css, jstree.css, select2, main.css, ext.css.

### 3.6 Non-Java code areas
- `generated-procs\`: **719 Oracle stored procedures/functions** (CIMS_ADD*, CIMS_GET*, CIMS_UPDATE*, HL7_*, RPT*, utility functions FNCONVERTDATE…, TO_BASE64, SEQ_CREATE/DROP…) — the DB business layer backing the JPA entity services.
- `templates\`: ~180 `.vm` Velocity print templates (receipts, discharge forms, prescription, labels, certificates, reports) + subdirs nhcx\, risl\, templates\.
- `integration\tiuxml`: TIUXML mapping (schemas\, templates\, tiuxmlmapping\, PrescriptionFiles\, testdata\) — instrument/HL7-style XML integration artifacts.
- `artifacts\risl\solr-scripts`: Solr Cloud per-node sync scripts (scripts_node1_78_198, node2_160_199, node3_161_200), status/check.sh, restart-cron/zkslr_ctl.sh.
- `artifacts\risl\redis-scripts\check.sh`, `artifacts\risl\server-scripts\check.sh`.
- `ruler\`: Node.js rule-engine server (HTTP + Redis) — `rules.engine.url=http://localhost:8000` (application.properties:914).
- `locales\cldr\`: CLDR data (en, en-IN, en-MT, hi).
- `scripts\` + `src/main/resources/compress\`: Node tooling for FAUG schema/template compression & validation.

---

## 4. WAR vs JAR / Packaging / Deployment Model

- Default build: **fat/executable JAR** (`packaging jar` + spring-boot-maven-plugin + spring-boot-starter-tomcat → embedded Tomcat). `App` extends `SpringBootServletInitializer` (App.java:79, 153-156), so the same class also works as a WAR.
- `build_war.sh` overlays the `artifacts/risl` variant to produce a **WAR for external Tomcat 8/9** with **JNDI datasource** (`jdbc/orcl`), `jakarta.persistence-api 2.2.3` bridge, and a `webapp` staging dir (deleted afterwards, build_war.sh:18). `clean.sh` deletes stale git branches. `create.sh` builds a Pentaho integration jar from compiled classes.
- Deployment target evidence: QA properties point at `jdbc:oracle:thin:@//10.10.7.63:1521/orcl` (application.properties:77), Redis standalone `oracle.sms:6379`, Solr Cloud 10.10.5.151-153, EMR web services `https://ohumdevewd.ohumhealthcare.com/rislmodsewd/openMDWS/get.ewd?` (:217), `app.public.url=http://192.168.1.76:8080/IHMS` (:325), server.port=8080, `context-path=/IHMS` (:3-4).
- `target\` contains only `classes\` and `test-classes\`; no jar/war (gitignored: `target`, `*.war`).
- Runtime watchdog: `docit.ctrl` file created at startup; app polls every 5 s and **shuts down when the file is deleted** (App.java:273-286) — ops kill-switch.
- Logging: `logs\IHMS.log` + `IHMS-error.log` roll daily at 200 MB (observed: up to 118 MB single-day error files; ~500 MB total error logs in repo — large but gitignored; presence in working tree suggests a live dev server ran from this checkout).

---

## 5. EclipseLink vs Hibernate — Evidence

1. `pom.xml:366-374` — spring-data-jpa excludes hibernate-entitymanager.
2. `pom.xml:532-545` — spring-boot-starter-data-jpa excludes hibernate-entitymanager AND hibernate-core.
3. `pom.xml:376-379` — explicit EclipseLink `org.eclipse.persistence.jpa 2.6.4`.
4. `pom.xml:171-190` — staticweave-maven-plugin (de.empulse.eclipselink 1.0.0) weaving at process-classes with EclipseLink 2.6.4 → **static weaving** of entities.
5. `src/main/resources/META-INF/persistence.xml:6` — provider `org.eclipse.persistence.jpa.PersistenceProvider`.
6. `App.java:179-192` — `EclipseLinkJpaVendorAdapter`; eclipselink.* JPA property map (logging, `eclipselink.cache.shared.default=false`, `eclipselink.jpa.naming_into_indexed=true`).
7. `application.properties:390-396` — `eclipselink.logging.level.sql=INFO`, `eclipselink.jpa.naming_into_indexed=true`.
8. persistenceContext.xml:38-40 — Hibernate vendor adapter only in a commented-out legacy block.

---

## 6. Open Questions / UNKNOWNs

1. Whether `persistenceContext.xml` is loaded at runtime at all (no import in applicationContext.xml; App.java builds EMF programmatically). UNKNOWN.
2. `patportal/` content — empty in working tree (gitignored). UNKNOWN.
3. `classpath:META-INF/cxf/cxf.xml` — not in repo; assumed from CXF jar (verified via App.java:75 import). The `<cxf:bus>` in applicationContext.xml is the app-level override.
4. Number of service classes discovered purely via component scan (beyond the 137 refs) — count of `@Component/@Service` in services\ (~155 files) not cross-checked against XML. Partial.
5. `ruler/`, `graphify-out/`, `scripts/`, python tooling — dev/analysis tools, not part of runtime artifact. Purpose of each as described from headers; not deep-read.
6. `.gitnexus/`, `.serena/`, `.agent/` — agent/tool state directories (not runtime).
7. Sonar credentials in pom.xml (10.10.5.70 + token) and many plaintext secrets in application.properties — flagged, not inventoried exhaustively.

---

## 7. Key File:Line References
- pom packaging/Java/start-class: pom.xml:9,14,21
- Hibernate exclusions: pom.xml:366-374, 532-545; EclipseLink: pom.xml:171-190, 376-379
- staticweave: pom.xml:171-190; asciidoctor: pom.xml:191-217
- build_war.sh overlay: build_war.sh:3-19
- App.java annotations/import: App.java:68-79; CXF servlet: App.java:149-151; EMF: App.java:173-193; static resources: App.java:130-137; watchdog: App.java:273-286
- cacheImpl: applicationContext.xml:43-45; services import: applicationContext.xml:68
- jaxrs server address + beans: services.xml:93-228; providers: services.xml:234-254
- logback appenders: logback-spring.xml:8-52
- persistence unit: src/main/resources/META-INF/persistence.xml:5-10
- SpringSecurityConfig chain: SpringSecurityConfig.java:105-122
- risl WAR packaging: artifacts/risl/pom.xml:9; risl JNDI App.java (JndiDataSourceLookup)
- context path/port: application.properties:3-4; orig API prefix: application.properties:371
