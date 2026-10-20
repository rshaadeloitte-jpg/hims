# Repository Snapshot — iHMS / OHUM PAS NG (Phase 0)

> Generated: 2026-08-14 · Source: `D:\ihms` · Analysis: Phase 0 of HMIS modernization discovery
> All evidence paths are relative to `D:\ihms`.

---

## 1. Project Identity

| Attribute | Value | Evidence |
|-----------|-------|----------|
| Artifact | `com:ohumpasng:3.4` (JAR packaging) | `pom.xml:6-9` |
| App name | iHMS (integrated Hospital Management System) | `src/main/resources/public/index.html` title; `application.properties:233` `hospitalName = Ohum HealthCare` |
| Main class | `com.ohum.App` | `pom.xml:21`, `src/main/java/com/ohum/App.java` |
| Java version | 1.8 | `pom.xml:14` |
| Spring Boot | 2.7.5 | `pom.xml:32-35` |
| JPA provider | EclipseLink 2.6.4 (NOT Hibernate; Hibernate excluded) | `pom.xml:376-379, 534-545`; static weaving plugin `pom.xml:171-190` |
| Context path | `/IHMS` | `application.properties:4` |
| Server port | 8080 | `application.properties:3` |

## 2. Source Size

- Java files: **3,001** under `src/main/java` (+ additional under `artifacts/risl/src/main/java` referenced by sonar config `pom.xml:24`)
- Frontend static files: **8,748** under `src/main/resources/public`
- Templates (Velocity/HTML/print): 209 files in `templates/` (root), plus `src/main/resources/public/modules/**/templates`
- Integration fixtures: 1,562 files in `integration/` (tiuxml mapping, HL7 templates, reports, scripts)
- Artifacts: 1,071 files in `artifacts/` (incl. `risl` legacy Java source, Solr/Redis/server scripts, migration-scripts, Equipment Interface docs)
- Generated stored-procedure sources: 719 files in `generated-procs/`
- Locales (CLDR): 175 files in `locales/`

## 3. Top-Level Repository Layout (annotated)

```
D:\ihms
├── pom.xml                     Maven build (Spring Boot 2.7.5, Java 8, EclipseLink, CXF, HAPI FHIR/HL7)
├── application.properties      1506-line runtime config: DB, Redis, Solr, integrations, schedules, templates
├── persistence.xml             JPA persistence config (root copy; see also src/main/resources)
├── src/
│   └── main/
│       ├── java/
│       │   ├── com/ohum/       MAIN APPLICATION PACKAGE (~2,600 files)
│       │   │   ├── App.java            Spring Boot entry point
│       │   │   ├── SpringSecurityConfig.java
│       │   │   ├── GzipFilter.java
│       │   │   ├── annotations/  aspects/  audit/  cache/  constants/  dao/
│       │   │   ├── dto/              ~164 response DTOs (aadhar, aasmaan, bloodBank, cssd, custom, ehr,
│       │   │   │                    ejs, emitra, file, integration, kiosk, nurse, pas, patp, payment,
│       │   │   │                    queries, solr, transport)
│       │   │   ├── exceptions/  interceptors/ (emitra, sso)  jobs/  mappers/
│       │   │   ├── models/          JPA entities (~800+):
│       │   │   │   ├── masters/         ~620 master entities (Item, Bed, Doctor, Tariff, Tax, Patho,
│       │   │   │   │                    Rad, Blood, OT, IVF, Accounting, Staff, Unit, Ward…)
│       │   │   │   ├── transactionals/  ~140 transactional entities (Bills, Prescription, IPD, CSSD,
│       │   │   │   │                    BioMedicalWaste, Equipment, Transport, Linen, Diet, OT…)
│       │   │   │   ├── trx/  statistical/  rcm/  txt/  hcx/  ejs/  chiranjeevi/  aasmaan/
│       │   │   ├── services/
│       │   │   │   ├── pas/            ~66 JAX-RS services (Patient, Bill, IPD, Appointment, Bed…)
│       │   │   │   ├── custom/         ~30 JAX-RS services (inventory, pharmacy, billing helpers…)
│       │   │   │   ├── masters/ ejs/ ehr/ nurse/ pas/ patp/ payment/ aadhar/ abdm/ cssd/
│       │   │   │   └── *.java          AuthServiceImpl, CWFServiceImpl, RcmServiceImpl,
│       │   │   │                       QueueManagmentServiceImpl, ClinicalServiceImpl, IntegrationServiceImpl…
│       │   │   ├── util/               auth (LDAP), hl7, tiuxml, generic query logger
│       │   │   ├── views/              ~155 view DTOs (AddPrescriptionView, BillReceiptView…)
│       │   │   └── xcg/                abdm (ABDM/HFR/NHCX servers), genserv, hcx
│       │   ├── com/fasterxml/          vendored Jackson customizations
│       │   ├── org/eclipse/...         vendored EclipseLink platform code
│       │   ├── org/abdm/...            vendored ABDM openapi client
│       │   └── ca/uhn/fhir/...         vendored HAPI FHIR JAX-RS server
│       └── resources/
│           ├── applicationContext.xml  (4.4 KB)
│           ├── persistenceContext.xml (3.3 KB)
│           ├── services.xml           (15 KB) — CXF JAX-RS <jaxrs:server address="/rest"> bean wiring (~all services)
│           ├── logback-spring.xml
│           ├── configStatus.json
│           ├── jasperreports_extension.properties
│           ├── public/                STATIC FRONTEND (FAUG)
│           │   ├── index.html         FAUG bootstrap page (jQuery, Bootstrap, faug-min.js, ext.js)
│           │   ├── faug-min.js  faug-ext.js  faug-ext-globalize.js  faug.css
│           │   ├── fjs-config_*.json  40 module configs → map SchemaName → schema JSON paths
│           │   │                      (op, ip, pharmacy, billing, pathology, radiology, inventory,
│           │   │                       nursing, ot, cssd, bloodBank, masters, reports, rcm, pmrd,
│           │   │                       transport, diet, queue, kiosk, chemotherapy, bcma, nhcx,
│           │   │                       biomedicalwaste, careplan, charting, commons, enquiry,
│           │   │                       equipments, fileManagement, grievance, linenlaundry,
│           │   │                       nursingWorklist, paymentTransfer, schemas, shifthandover,
│           │   │                       templates, tiuxmls, worklist, biReport, patientManagement, rmrs)
│           │   ├── modules/           40 module dirs, each with schemas/*.json + templates/*.html
│           │   ├── js/                ~71 module JS controllers (billing-module.js, admission-module.js,
│           │   │                       pharmacy-module.js, nursing-module.js, operation-theatre.js…)
│           │   ├── jsonFile/  data/  charts/  layer/  queue/  testdata/  tiuxd/
│           │   ├── ohumlite/          Cordova mobile app (patient app) — excluded from WAR build
│           │   ├── patportal/         patient portal app (also Cordova) — excluded from WAR
│           │   ├── resources/         jQuery plugins, Bootstrap, TinyMCE, charts, timers, i18n
│           │   └── master*Config.json vitalsConfiguration.json ODPartoGraphConfig.json etc. (client-level config)
│           ├── lang/  fhir/  fonts/  compress/  META-INF/
├── templates/                  209 files: Velocity/HTML templates (nhnc, risl) + jasper jrxml?
├── integration/                1,562 files: tiuxml schemas/templates/mappings, HL7, reports, scripts
├── artifacts/                  1,071 files: risl legacy src, solr-scripts, redis-scripts, server-scripts,
│                               migration-scripts, Equipment Interface docs
├── generated-procs/            719 files: generated stored procedures
├── locales/cldr                CLDR i18n data
├── scripts/                    Node helpers (compress, route update, JSON validation, tiuxml template prep)
├── logs/  ruler/  graphify-out/  target/  .metadata/ (Eclipse)  .github/java-upgrade
├── build_war.sh  clean.sh  create.sh   build scripts
├── *.py                        ast_extract.py, build_graph.py, trace.py, summary.py, detect.py,
│                               finalize.py, generate_html.py, label_communities.py (analysis tooling)
└── labels.csv / labels.json / All_Labels_*.csv   label inventories
```

## 4. Runtime Stack (from `application.properties`)

- **Database**: Oracle (`spring.jpa.database=ORACLE`, `ojdbc7`, `jdbc:oracle:thin:@//10.10.7.63:1521/orcl`, user `IHMSDOCITDEVMODS_PROD_V1`). PostgreSQL driver ALSO on classpath (`pom.xml:381-383`) — target DB for modernization.
- **Redis**: standalone `oracle.sms:6379`, prefix `risl_2233`, Jedis pool 200/200/100 (`application.properties:109-153`)
- **Solr**: Cloud via ZooKeeper `10.10.5.151-153:2181-2183`; collections: `patient_risl_qa`, `encounter_risl_qa`, `item_risl_qa`, `service_risl_qa`, `icd10cm_risl_qa`, `healthfactor_risl_qa` (`application.properties:157-190`)
- **FHIR**: HAPI FHIR 5.7.0 (DSTU2/DSTU3/R4), `fhir.packages=ndhm.tgz`, namespaces `genserv ndhm abdm`
- **HL7**: HAPI 2.2 (v2.5), inbound pickup schedule `0 0 1 * * ?`
- **ABDM** (India digital health): gateway/CM/HIE-CM/HFR/HPR/ABHA/PHR v3, milestones M1,M2
- **HCX**: `abdm.hcx.url=https://apisbx.abdm.gov.in/hcx`
- **eSign**: esign integration (esign.com)
- **MinIO**: object storage (disabled by default `minio.enabled=false`)
- **Print**: WebSocket print server `ws://localhost:8182`, qz-tray JS
- **SMS/WhatsApp**: eSanchar gateway (Rajasthan govt)
- **SSO**: Rajasthan govt SSO (`ssotest.rajasthan.gov.in`), mobile SSO variant
- **LDAP/AD**: Samba/AD auth (disabled `ldap.auth.enabled=false`), restrictive handler
- **Aadhaar**: face auth/OTP/biometric via sewadwaar APIs
- **Government schemes**: Chiranjeevi (RJ), RGHS, Bhamashah, JanAadhaar, e-Mitra, e-Aushadhi (drug stock/indent), PCTS, NMC, Jeevanvahini ambulance, Mera Aspataal, Kaysons
- **EHR**: `ehr.sync.schedule`, EWD integration `emr.url=https://ohumdevewd.ohumhealthcare.com/rislmodsewd/openMDWS/get.ewd`
- **Files**: `int.files.path=D:/RISL/ihms/integration`, `document.files.path=/tmp`, `tiuxml.file.path`
- **Misc**: JWT (nimbus), QR/barcode (zxing/barcode4j), openhtmltopdf + JasperReports 6.0.3 + iText, Velocity templates, Excel (POI), Tika, hazelcast, ehcache, spymemcached, kryo, XStream

## 5. HTTP Surface

- CXF JAX-RS services mounted at address `/rest` (`services.xml`) → `GET/POST /IHMS/api/rest/<service-path>` (see `application.properties:371` `orig.api.path.prefix = /IHMS/api/rest`)
- Internal API prefix: `/ext/api` (`application.properties:367`)
- Reports prefix: `/IHMS/api/rest/reports` (`application.properties:369`)
- Frontend is a single-page FAUG app; routes defined in JS configs (`fjs-config_*.json`) and per-module JS controllers; UI calls back-end via AJAX to `/IHMS/api/rest/...` and `/ext/api/...`

## 6. Business Modules (preliminary, from `fjs-config_*.json` + `modules/` dirs — to be confirmed by discovery)

op, ip, patientManagement, appointment (in js/), pharmacy, billing, rcm, pathology, radiology, nursing, nursingWorklist, ot, cssd, bloodBank, diet, inventory, procurement, transport, linenlaundry, biomedicalwaste, equipment, fileManagement, grievance, queue, kiosk, chemotherapy, bcma, careplan, charting, enquiry, masters, reports, pmrd, shifthandover, tiuxmls, worklist, nhcx, biReport, allocation, paymentTransfer, rmrs + integrations (abdm, hcx, ejs/emitra, aadhar, aasmaan, chiranjeevi, rghs, eaushadhi, ehr, hl7)

## 7. Scheduled Jobs (cron evidence in application.properties)

auto.charges, dailyencounter, nurse.patient.allocation, inventory External Stock, bloodorder.cancellation, bloodbag.expiry, bloodbag.auto.decross, donor.cancellation, rmrs.authorityPayment, equipmentmaintenancedue, interimAdmission.abscond, ehr.sync, ejs.asha.incentive, ihms.summary, ihms.allcount, rghsSims, whatsApp.retrigger, eAushadiItemStockUpdate, unitWise.autoIndent, unitWise.mainToEAushadhi.autoIndent, cancel.cross.service.visit, checkoutDoctor.QueueRooms, sendRecord.ABDM, hl7inbound.pickup, eRaktkoshDonation, operator.ehr.sync

## 8. Build & Tooling

- `build_war.sh` — WAR packaging script
- Maven plugins: sonar, compiler 1.8, surefire (JUnit5), spring-boot, staticweave (EclipseLink), asciidoctor (restdocs)
- Java upgrade hooks under `.github/java-upgrade` (Java 8 → newer planned?)
- `scripts/` Node.js helpers for schema/route management (compress.js, updateRoute.js, validateJons.js)

## 9. Skill Installation Attempt (Phase 0)

| Skill | Command | Result |
|-------|---------|--------|
| ui-ux-pro-max | `npx skills add https://github.com/nextlevelbuilder/ui-ux-pro-max-skill` | Reported success but no files materialized (verified `~/.agents/skills`, `~/.claude/skills`, `~/.config/opencode`) — **FAILED to materialize; not available** |
| business-analyst (bmad) | `npx skills add https://github.com/aj-geddes/claude-code-bmad-skills` | Same — **FAILED to materialize** |
| documentation-writer | `npx skills add https://github.com/github/awesome-copilot` | Same — **FAILED to materialize** |
| PostgreSQL Database Administration | `npx skills add https://github.com/manutej/luxor-claude-marketplace` | Same — **FAILED to materialize** |
| java-springboot | pre-installed | **AVAILABLE** (`C:\Users\Admin\.agents\skills\java-springboot`) — loaded |

Per project instructions, analysis continues with equivalent expertise; failures recorded here.

## 10. Key Risks Already Visible (Phase 0)

1. Massive FAUG frontend: 40 module configs, ~71 JS controllers, thousands of schema JSONs + HTML templates — business logic lives in JS.
2. 3,001 Java files, 800+ entities, one CXF REST bus (`/rest`) — monolith.
3. Oracle → PostgreSQL migration required; Oracle-specific SQL (NVL, DUAL, ROWNUM, SEQUENCE) and generated stored procedures (719 files in `generated-procs/`).
4. Heavy external integration surface (ABDM, HCX, govt schemes, HL7, FHIR, LDAP, SSO, SMS/WhatsApp, eSign, print servers, equipment interfaces).
5. Secrets present in `application.properties` (DB/Redis/Aadhaar/eSign/LDAP passwords) — must not be committed to the new repo.
6. Status codes are numeric; meaning must be reverse-engineered (never guessed).
