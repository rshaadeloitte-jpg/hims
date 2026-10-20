# C3 — Component Diagram: iHMS / OHUM PAS NG

> **Document type:** C4 Level 3 (Components inside the monolith / per-service for TO-BE).
> **Inputs:** `01-system-landscape.md`, `_discovery/agent-01/findings.md` (§2-§3), `04-backend-architecture.md`, `_discovery/reconciliation-notes.md` (§1), `10-target-architecture.md`, `08-security-rbac-audit.md`.
> **Status:** AS-IS evidence-based; TO-BE sections marked **PRELIMINARY**.

---

## 1. AS-IS Component Diagram

```mermaid
flowchart TB
    subgraph FE["FAUG Frontend (classpath:/public/ — 8,748 files)"]
        F1["index.html shell + fjs-config.json (126 KB)
            41 × fjs-config_<module>.json (schemas/routes)"]
        F2["faug-min.js framework + ext.js + main.js"]
        F3["71 flat JS controllers
            admission-module.js · billing-module.js · nursing-module.js …"]
        F4["40 module dirs
            schemas/*.json + templates/*.html (4,255 files)"]
        F5["Business config JSONs
            tiuxml_mapping.json · mastersConfigration.json · vitals*.json"]
        F6["jQuery / Bootstrap3 / highstock / fullcalendar /
            tinymce / qz-tray / aadhaarauth (59 script tags)"]
    end

    subgraph BUS["CXF JAX-RS Bus (services.xml — address=/rest, @ /api/*)"]
        B1["137 serviceBean refs
            65 XML <bean> + 96 @Component (65 REST)"]
        B2["inInterceptor: PreProtocolInterceptor (_type=json)"]
        B3["providers: RequestResponseInterceptor (Jackson)
            ServiceJAXRSExceptionMapper · JAXBElementProvider
            NullableLongParamConverterProvider"]
        B4["extensionMappings: json|xml|* → application/json"]
    end

    subgraph ROUTE["4 Routing Layers"]
        R1["① Explicit @Path services — 132 classes / 3,003 endpoints"]
        R2["② CrudOperationsFilter — {context}/api/rest/<entity>/…
            → GenericServiceImpl.crudadd/update/get/getAll
            registry: 659 @CrudApi entities (1,109 FE URLs)"]
        R3["③ IntegrationOperationsFilter — /api/rest/reports/<name>
            reportMap from reports.js + *.json (Nashorn)
            → ReportServiceImpl + generic_reports.vm"]
        R4["④ /ext/api/<mapped> → internal URL
            ApiIntegrationMapping + allowedUsers ROLE_INTEGRATION"]
    end

    subgraph SVC2["Service Tier (com.ohum.services — 155 files)"]
        S1["BaseServiceImpl (1,044 LOC) — GenericDAO + cache +
            error helpers (BEXA*) + Velocity/PDF + OkHttp"]
        S2["GenericServiceImpl (370 LOC) — CRUD api,
            @Transactional(rollbackOn=Exception)"]
        S3["pas/ ~66 · custom/ ~30 · nurse/ cssd/ ejs/ aasmaan/
            abdm/ patp/ payment/ ehr/"]
        S4["Helpers: PatientServiceHelper · IPDHelper ·
            MasterCachedServiceHelper · AddPaymentServiceHelper ·
            AddServiceOrderServiceHelper · PathoOrderServiceHelper ·
            IntegrationHelper · EHRServiceHelper"]
    end

    subgraph DATA["Data Access & Persistence"]
        D1["GenericDAO — EntityManager wrapper
            save/update/delete/get/getAll/getQ"]
        D2["GenericQuery — fluent: namedQ() · nativeQ() · criteria()
            storedProcQ() · paginate() · list() · get()"]
        D3["EclipseLink EMF (static-weaved, shared-cache NONE)
            models (950) + dto (1,583) packages"]
        D4["719 Oracle stored procs (CIMS_*)
            = de-facto DB business layer"]
    end

    subgraph CROSS["Cross-cutting components"]
        C1["ServiceLayerAspect (@Around *ServiceImpl @Path)
            auth (401) · @NotSecure bypass · audit flush ·
            FAUG Nashorn render (integration/scripts/api.js) ·
            Solr write + rollback compensation (:1164-1182)"]
        C2["Cache: MemoryCacheImpl (active, expiry 86400)
            @Cached masters (~375 keys) · sessions · Solr mirrors
            RedisCacheImpl / RedisClusterCacheImpl (commented)"]
        C3["Jobs (5 classes / ~55 @Scheduled): ApplicationJobs 50+,
            InventoryJobSchedule, HL7InboundMessageJob (disabled),
            EHRSyncJobs (disabled), AutoNextDayAllocation"]
        C4["Security: SpringSecurityConfig (permitAll + CSP)
            DefaultAuthenticationImpl · CustomLDAPAuthenticationImpl
            SSOAuthFilter · EmitraRedirectFilter · IdEncryptionUtil (PHI)"]
        C5["Audit: AuditHandler/AuditHelper — 59 files / 568 call sites
            T_AUDIT · T_TRX_HISTORY · T_LOGIN_AUDIT · T_ABDM_AUDIT
            MBase beforeCreateUpdateCallback identity stamping"]
        C6["Async: taskExecutor(20)/taskScheduler(10)
            @Async + @BackgroundQueue contract"]
    end

    F1 --> F2
    F2 --> F3
    F3 --> F4
    F4 --> F5
    F5 --> F6
    F3 -->|"REST /IHMS/api/rest/**"| B1
    B1 --> B2
    B2 --> B3
    B3 --> B4
    B1 --> R1
    B1 --> R2
    B1 --> R3
    B1 --> R4
    R1 --> SVC2
    R2 --> S2
    R3 --> S3
    R4 --> S3
    SVC2 --> D1
    D1 --> D2
    D2 --> D3
    D2 --> D4
    C1 -.-> SVC2
    C2 -.-> SVC2
    C3 -.-> D4
    C4 -.-> SVC2
    C5 -.-> SVC2
    C6 -.-> SVC2
```

### 1.1 Component inventory (AS-IS) — with evidence

| Layer | Component | Role | Evidence |
|---|---|---|---|
| Frontend | FAUG engine (`faug-min.js` + `fjs-config.json` 126 KB + 41 module configs) | Schema-driven SPA framework (routes/schemas in JSON) | 01 §5; agent-01 §3.4 |
| Frontend | 71 flat JS controllers | UI logic per domain (admission-, billing-, pharmacy-module.js…) | 01 §5 |
| Frontend | 40 module dirs (4,255 schemas/templates) | Form/template definitions rendered client-side | agent-01 §3.4 |
| Frontend | Business config JSONs | tiuxml mapping, vitals config, masters config | agent-01 §3.4 |
| Bus | CXF `restServices` @ `/rest` under CXFServlet `/api/*` | Single REST bus; 137 refs; extension mapping | agent-01 §2.3; 04 §1 |
| Routing ① | Explicit `@Path` service classes (132, 3,003 endpoints) | Conventional REST | 04 §10 |
| Routing ② | `CrudOperationsFilter` → `GenericServiceImpl` | Data-driven CRUD over 659 `@CrudApi` entities | reconciliation §1 |
| Routing ③ | `IntegrationOperationsFilter` report dispatcher | `/reports/<name>` → ReportServiceImpl + Velocity | reconciliation §1 |
| Routing ④ | `/ext/api` mapping (`ApiIntegrationMapping`, `ROLE_INTEGRATION`) | Backend-gated integration surface | reconciliation §1, §5 |
| Service | `BaseServiceImpl` (1,044 LOC) | Shared service plumbing (DAO, cache, errors, print helpers, OkHttp) | 04 §2 |
| Service | `GenericServiceImpl` (370 LOC) | Generic CRUD ops; `crudaddMultiple` REQUIRES_NEW per item | 04 §2, §4 |
| Service | Domain services (pas/custom/nurse/cssd/ejs/aasmaan/abdm/patp/payment/ehr) | Business logic containers (no controller/service separation) | 04 §2 |
| Service | Helpers (8 named) | Cross-cutting workflow logic (e.g., AddServiceOrderServiceHelper) | 04 §2 |
| Data | `GenericDAO` + `GenericQuery` | The only query interface (6 kinds) | 04 §4 |
| Data | EclipseLink EMF | JPA provider (static-weaved; Hibernate excluded); packages models+dto | agent-01 §5 |
| Data | 719 stored procs (`generated-procs/`) | DB-side business layer; ~30 called from Java; rest UNKNOWN usage | reconciliation §4 |
| Cross | `ServiceLayerAspect` | The real "framework": auth, audit, FAUG render, Solr compensation, background queue | 04 §5,§8; agent-10 §1.1 |
| Cross | Cache impls | MemoryCacheImpl active; Redis variants commented | 07 §2.2 |
| Cross | Jobs | 5 classes, ~55 schedules (Solr drainers, ABDM, WhatsApp, eRaktkosh) | 04 §7 |
| Cross | Security stack | Spring Security inert (permitAll); custom token auth; frontend RBAC | 08 §2-§3 |
| Cross | Audit | Manual service-authored events flushed per request | 08 §5 |

### 1.2 God-components (decomposition pressure — evidence from 04 §9, agent-10)

| Component | Size | Coupling |
|---|---|---|
| `NursingServiceImpl` | 300 endpoints / 17,350 LOC / 349 graph edges | Encounter/Admission reads, orders, vitals |
| `PathoOrderServiceImpl` | 133 endpoints / 14,280 LOC | Billing txn, WhatsApp, KDL machines |
| `MasterCachedServiceImpl` | 264 endpoints / 12,710 LOC / **514 edges** | ~30 services consume its cache keys |
| `InventoryServiceImpl` | 235 endpoints / 12,070 LOC / 250 edges | Stock ops, Pharmacy dual-write |
| `UploadFileServiceImpl` | 294 edges | All modules' document upload |
| `AddServiceOrderServiceHelper` | one method chains 12+ procs | Order→Bill→Lab→Rad→DoctorShare→EMR→HL7 |

---

## 2. TO-BE Component Diagram (C3) — PRELIMINARY

Per-service component summary for the candidate 29-service landscape (`10-target-architecture.md` §2). Components shown at summary level; numbering matches the candidate service list.

```mermaid
flowchart LR
    subgraph PT["Platform services (Wave 0)"]
        P1["22 IAM — login/OTP/SSO/LDAP · JWT · user/role/grants
            components: authN facade · token issuer · role store"]
        P2["21 Reference Master — M_* masters + cache contract
            components: master CRUD · rate/tariff API ·
            Redis cache-key owner · MasterDataChanged publisher"]
        P3["27 Search & Index — Solr pipeline
            components: event consumer · indexer · search API ·
            (owns patient/encounter/item/icd10cm/healthfactor)"]
        P4["25 Interop & Notifications — HL7/ABDM/WhatsApp/EHR
            components: event consumer · outbox · external API clients ·
            webhook receiver"]
    end

    subgraph DOM["Domain services (waves 1-5)"]
        D01["1 Patient — register/merge/ABHA
            components: REST API · registry service · event publisher"]
        D02["2 Appointment & Encounter
            components: booking · queues · encounter lifecycle"]
        D03["3 ADT & Bed — admission/bed/discharge
            components: ADT API · bed state · discharge workflow"]
        D04["4 Ordering & Prescription
            components: orders API · prescription service · notes"]
        D05["5 Billing & Settlement
            components: bill service · charges · doctor share ·
            concession · estimation (saga participant)"]
        D06["6 Payments & Refunds
            components: payment capture · advances · refunds ·
            receipts · settlement updates"]
        D07["7 Lab — order/result/machine
            components: booking · specimen · resulting · machine API"]
        D08["8 Radiology — booking/report/template"]
        D09["9 Pharmacy — dispense · picklist · interventions"]
        D10["10 Inventory — items/stock/batch · PO/GRN · indent · eAushadhi
            (stock ops API decision: Pharmacy borrows via API)"]
        D11["11 OT · D12 Nursing · D13 Blood Bank · D14 Diet · D15 CSSD
            D16 Linen · D17 Transport · D18 Equipment · D19 BMW
            D20 Grievance · D23 RCM · D24 RMRS/PMRD · D28 Govt Programs
            D29 Queue & Kiosk"]
        D15R["26 Reporting — on-demand reports · dashboards ·
            read-model consumers (Jasper/Velocity replaced by
            report engine + PDF/MinIO)"]
    end

    subgraph PLATFORM["Shared platform components"]
        X1["API Gateway — JWT authN · rate limit · route · authZ policy"]
        X2["Kafka — topics: PatientRegistered · EncounterCreated ·
            PatientAdmitted/Discharged · OrderCreated · PrescriptionCreated ·
            InvoiceGenerated · PaymentReceived · LabOrderCreated/Finalized ·
            InventoryUpdated · MasterDataChanged …"]
        X3["PostgreSQL schema-per-service + outbox tables"]
        X4["Redis — cache/lookup (owner: Reference 21)"]
        X5["Observability — logs/metrics/traces"]
        X6["Config & secrets vault"]
    end

    D01 --> P1
    D02 --> P1
    D03 --> P1
    D04 --> P1
    D05 --> P1
    D06 --> P1
    D07 --> P1
    D08 --> P1
    D09 --> P1
    D10 --> P1
    D11 --> P1
    D12 --> P1
    D13 --> P1
    D14 --> P1
    D15 --> P1
    D15R --> P1
    D01 --> P2
    D02 --> P2
    D03 --> P2
    D04 --> P2
    D05 --> P2
    D06 --> P2
    D07 --> P2
    D08 --> P2
    D09 --> P2
    D10 --> P2
    D11 --> P2
    D12 --> P2
    D13 --> P2
    D14 --> P2
    D15 --> P2
    D15R --> P2
    D01 --> P3
    D02 --> P3
    D03 --> P3
    D04 --> P3
    D05 --> P3
    D07 --> P3
    D10 --> P3
    D04 --> P4
    D05 --> P4
    D06 --> P4
    D07 --> P4
    D03 --> P4
    D05 --> X2
    D06 --> X2
    D04 --> X2
    D07 --> X2
    D09 --> X2
    D10 --> X2
    P2 --> X2
    P3 --> X2
    P4 --> X2
    X1 --> D01
    X1 --> D02
    X1 --> D03
    X1 --> D04
    X1 --> D05
    X1 --> D06
    X1 --> D07
    X1 --> D08
    X1 --> D09
    X1 --> D10
    X1 --> D11
    X1 --> D12
    X1 --> D13
    X1 --> D14
    X1 --> D15
    X1 --> D15R
    P1 --> X1
    X4 -.-> D01
    X4 -.-> D02
    X4 -.-> D05
    X4 -.-> D10
    X4 -.-> D17
    X2 --> X5
    X2 --> X6
```

### 2.1 TO-BE per-service component shape (PRELIMINARY)

Every candidate service follows the same internal skeleton (all names candidates):

| Component | Responsibility | Replaces (AS-IS) |
|---|---|---|
| **REST API layer** | Exposes `/…` endpoints to the gateway; JSON only | JAX-RS `@Path` methods (FAUG HTML rendering removed) |
| **Domain service core** | Business logic currently inside `*ServiceImpl` + procs on the extraction path | `services/` god classes + `CIMS_*` procs |
| **DB adapter** | PostgreSQL schema owned by this service only; no cross-service DB access | GenericDAO/GenericQuery + EclipseLink EMF (Oracle) |
| **Outbox writer** | Transactional write to local outbox for each emitted event | In-txn side-effect calls (Solr/HL7/WhatsApp) |
| **Event publisher/consumer** | Kafka producers/consumers per `10-target-architecture.md` §4 | DB-queue pollers (`ApplicationJobs`) |
| **Cache client** | Read-through Redis lookups (keys owned by Reference 21) | `cacheImpl` reflective lookups |
| **External client** | HTTP/SDK clients (ABDM, eSanchar, e-Aushadhi, print…) | `IntegrationServiceImpl`, `HttpUtil` |
| **Read-model** | Local copies of borrowed data (encounterId, tariff snapshot) fed by events | Direct table joins (`T_ENCOUNTER` joins from every module) |

### 2.2 Cross-cutting component changes (TO-BE, PRELIMINARY)

| Concern | AS-IS | TO-BE |
|---|---|---|
| Authorization | Frontend `data-roleM` + commented-out aspect check (`08` §3) | Gateway/API policy layer keyed on `Role → ModulePermissionMaster`; re-enabled server-side check |
| Authentication | Opaque cache token, 30-min sliding, password check disabled | JWT (HttpOnly+Secure cookie or signed token) + real password hashing |
| Audit | Manual `audit.getAudits().add()` (568 sites) → T_AUDIT per request | Standardized event-driven audit stream (annotation/filter-level) |
| Rendering | FAUG Nashorn inside `ServiceLayerAspect` | JSON APIs + React SPA |
| Caching | ~375 reflective `@CrudCached` keys, per-node invalidation | Declared cache (Caffeine + Redis pub/sub) owned by Reference |
| Search indexing | Realtime in-txn `addBean` + flag drainers | Event-driven indexing pipeline with explicit commits |
