# Agent 07 — Integrations, Solr & Redis Discovery (iHMS / OHUM PAS NG)

> Legacy system: `D:\ihms` (Java, CXF/JAX-RS, EclipseLink/Oracle, Spring Boot-ish XML+annotation hybrid).
> Source of truth for all paths: `src/main/java/com/ohum/...`. Evidence dump: `_discovery/agent-07/findings.md`.

---

## 1. SOLR — the search backbone

> ## ⛔ SOLR MUST REMAIN
> Solr is **non-negotiable in the target architecture**. It is not a convenience cache — it is the
> **primary query path for patient identity search** (registration, OPD/IPD check-in, ABDM consent
> linking, duplicate detection), the **master lookup for ICD-10, health factors and pharmacy items**,
> and it is embedded in transactional flows with explicit rollback compensation
> (`ServiceLayerAspect` APO layer). ~15 search methods in `SolrUtil.java` are called from patient
> registration, admission, appointment, blood bank, requisition, EJS outreach, ABDM and MDM services.
> Any modernization that replaces or drops Solr breaks the registration/search UX at every hospital
> unit and breaks ABDM consent discovery (which queries Solr for `healthIdNumber`/`mobile`+DOB matches).
> It also runs as a **3-node SolrCloud (7.5.0) with 2-shard collections and DR rsync scripts**
> (`artifacts/risl/solr-scripts/`) — the operational pattern (per-minute health checks, midnight
> restart cron, rsync-based DR index sync) must be preserved or improved, not removed.

### 1.1 Architecture overview

```
┌────────────┐   REST (CXF /api)   ┌────────────────────────────┐
│   Client   │ ──────────────────▶ │  Services (PatientReg*,    │
│   (JS/UI)  │                     │  EmergencyAndWalkin, ABDM, │
└────────────┘                     │  EJS, BloodBank, ...)      │
                                   └───────────┬────────────────┘
                                               │ addBean/query (SolrJ)
                                   ┌───────────▼────────────────┐
                                   │  SolrUtil (util/SolrUtil.java)│
                                   │  • index* (write, per-trx)  │
                                   │  • search* (15 methods)     │
                                   │  • sync* (batch drainers)   │
                                   │  • Redis mirror keys        │
                                   └───────────┬────────────────┘
                                               │ SolrClient (CloudSolrClient, App.java:249-253)
                                   ┌───────────▼────────────────┐
                                   │  SolrCloud 7.5.0, 3 nodes  │
                                   │  ZK 10.10.5.151-153:2181   │
                                   │  6 collections (see 1.2)   │
                                   └────────────────────────────┘
```

- **Client**: raw SolrJ `CloudSolrClient` (not `SolrTemplate`). spring-data-solr is used only for the
  `@SolrDocument`/`@Indexed` annotations on the 5 view DTOs; repositories disabled
  (`application.properties:1283`).
- **Environments**: QA collections carry `_risl_qa` suffix (`patient_risl_qa` etc.,
  `application.properties:174-190`); production/staging use bare names `patient`, `encounter`, `item`,
  `icd10cm`, `healthfactor` (`artifacts/risl/application.properties:59-63` + DR sync script shard names).
- **Commit strategy**: per-document `addBean` with commit commented out (`SolrUtil.java:175,188`);
  batch syncs commit explicitly (`:956`). Relies on Solr autoSoftCommit.

### 1.2 Collections & document models

| # | Collection (QA name) | Indexer (writer) | Doc model (fields count) | Sync trigger | Retention |
|---|---|---|---|---|---|
| 1 | **patient** (`patient_risl_qa`) | `PatientSolrView` (dto/solr/PatientSolrView.java) — **~100 fields** (names, all identity numbers: civilid/passport/dlic/ration/janaadhar/pcts/ects/enrId, ABHA healthId+healthIdNumber+defaultHealthId, kin, occupation, blood group, insurance scheme/plan, unitId, address) | Realtime on register/update (`indexPatientInSolr*`); nightly batch backfill `indexMissingPatientsInSolr` (SOLR_STATUS=0) | Permanent |
| 2 | **encounter** (`encounter_risl_qa`) | `EncounterSolrView` (dto/solr/EncounterSolrView.java) — patient fields + ~45 encounter fields (visitMark, appointmentTypeId, doctorId, departmentId, clinicId, opdNo, token, isCheckout, isEncounterClosed, telemedicine flags, MLD flags, transport, buildingId) | Realtime on encounter/admission create/update (`indexEncounterInSolr`, `indexAmissionInSolr`, `updateFieldsInSolr` partials); `id = <encId>_E / <admId>_A` | **Rolling window: `solr.encounter.retain.days=3`** — `closeDailyEncountersJob` closes + deletes (ApplicationJobs.java:542-570) |
| 3 | **item** (`item_risl_qa`) | `ItemSolrView` — ~75 fields (item code/name/brand, molecule, category, purchase rate/MRP/VAT, inventory code, pharmacy/linen/medical-gas/asset flags, kit/compound, batch/expiry) | **Not realtime** — flag-based drainer `syncItemsToSolrJob` (5s fixedDelay, SOLR_STATUS=0, batches of 500, 90s cap) + manual REST `startItemSyncingToSolr` (MasterCachedServiceImpl:12592) | Permanent |
| 4 | **icd10cm** (`icd10cm_risl_qa`) | `Icd10cmSolrView` — icdcode, short/long description, local language, alias, parent, category, coding system | Flag-based drainer `syncIcd10cmToSolrJob` (5s) | Permanent |
| 5 | **healthfactor** (`healthfactor_risl_qa`) | `HealthFactorSolrView` — code, description, category, local lang, SNOMED display | Flag-based drainer `syncHealthFactorToSolrJob` (5s) | Permanent |
| 6 | **service** (`service_risl_qa`) | **NO indexer, NO reader.** `serviceSolrName` declared (SolrUtil.java:114, ApplicationJobs.java:144) but never used. Collection also absent from prod DR sync script. | — | — | Orphaned |

### 1.3 Indexing flow (DB → Solr)

```
DB (Oracle) ──save──▶ ServiceImpl ──call──▶ SolrUtil.indexXxx() ──▶ cacheImpl.add(current_pat_/current_enc_, TTL 100s)
                  (transaction)                    │                     │
                                                   │ addBean(collection, view)  ◀─── enrich from cached masters
                                                   ▼                     ▼
                                              SolrCloud doc          Redis mirror (read-through)
                                                   ▲
                        rollback compensation: ServiceLayerAspect.java:1164-1182 (APO layer) —
                        on tx failure deletes Solr doc + Redis keys (props "solr:<coll>" / "redis:id|mrno")
```

- **Realtime writers** (each: DB save → `SolrUtil` → collection, all gated `if(!solrEnabled)return`):
  - `PatientRegAdmissionServiceImpl:875,1180,1662-1665` — register/update/admission
  - `PatientRegApptServiceImpl:986,1299` — appointment → encounter
  - `EmergencyAndWalkinVisitServiceImpl:1795-1798,3953-4145` — walk-in + repair endpoints (`indexMissingPatient/Encounter`)
  - `PatientServiceHelper:761,4363,4481`; `MarkAndAddVisitServiceImpl:402`; `BedReservationServiceImpl:165`;
    `BloodBankServiceImpl:1429-1625`; `PatientRequisitionServiceImpl:746-854`; `PatientTransferDischargeServiceImpl:698-3282,4400-4413`
    (discharge → clear encounter; death → partial patient update); `EjsDetailsServiceImpl:2105` (outreach registration);
    `AddUpdateAppointmentServiceImpl:191` (encounter field partial update); `CommonABDMUtil:1391` (healthId partial update)
- **Batch writers** (flag-drained): `syncSolrPatients` (SolrUtil.java:899), `syncItemMaster` (:965),
  `syncIcd10cmMaster` (:1579), `syncHealthFactorToSolr` (:2032) — each updates its `SOLR_STATUS`/`SOLRSTATUS`
  flag in the same tx, so failures are retried (at-least-once).
- **Consistency checks**: on registration search with ≤15 hits, patient ids are re-verified against DB and
  stale Solr docs deleted (SolrUtil.java:618-628).

### 1.4 Search APIs (consumers)

| API (SolrUtil method) | Consumers | Purpose |
|---|---|---|
| `searchPatientInSolr` :340 | Registration, patient search screens, ABDM flows | edismax, boosted fq (mrno^18, mobile^16, name parts^8-12, civilId^16, janaadhar^17…), wildcards, reverse-MR search, sort by registration date desc |
| `searchPatientInEncouter` :644 | Unit-scoped patient find (OPD/IPD) | same boosts + `unitId_l` + `isAdmission_b` + `opdNo_s` |
| `searchPatientInEncounterForCheckinCheckout` :1516 | Check-in/check-out queues | cabin/department/doctor/date-range filters |
| `searchExactPatientInSolr` :1015 | Duplicate registration guard | mobile+DOB exact |
| `searchPatientInSolrByIdentity` :1061 | Aadhaar/passport/licence lookup (BaseServiceImpl:259-304, merge) | identity OR search |
| `getPatientByMrno(AndUnitId)` :1147,:2345 | MR-no lookup everywhere | Redis-first |
| `searchABHAPatientHIDInSolr(List)` :2150,:2218 | ABDM patient matching | mobile + DOB ±1yr + Levenshtein name ≤3 |
| `searchAllItemsInSolr` :1173 | Pharmacy item search | pharmacy/linen/gas/asset flags, name/brand wildcards, rows 10000 |
| `searchAllIcd10cmInSolr` :1604 | ICD diagnosis search | shortdesc/code/alias, complaint (R*/OHC.*) filter |
| `searchHealthfactor(s)` :1962,:2003 | Health factor autocomplete | description wildcard |
| `getAppListSearchFilters/TypeFilter` :1394,:1455 | Appointment token lists | builds fq per appointment type (rghs, maay, teleconsult…) |

- Search DTOs: `SearchSolrItemDto`, `SearchSolrIcd10cmDto`, `SearchPatientGeneralDetailsResultDto`, `dto/solr/*`.
- JS side: all patient search goes through REST endpoints; no direct Solr calls from the front-end.

### 1.5 Re-index & repair
- Nightly backfill: `indexMissingPatientsInSolr` (ApplicationJobs.java:2122-2171) — runs only on nodes
  listed in `solrsync.servers` (application.properties:1475), window 18:00–07:00, `SOLR_STATUS=0` rows,
  batch 1000.
- Manual REST repair: `EmergencyAndWalkinVisitServiceImpl:3953-4145` (`indexMissingPatient`,
  `indexMissingEncounter`), `MasterCachedServiceImpl:12592` (`startItemSyncingToSolr`).
- ABHA backfill: `updateHealthIds` (commented-out job, ApplicationJobs:2039) writes `healthId_s`/`healthIdNumber_s`
  atomically in batches of 10k.

### 1.6 Failure modes & consistency risks
1. **No commits in realtime path** — visibility depends on Solr autoSoftCommit; search-after-save can miss docs
   (mitigated by Redis `current_pat_` 100s mirror).
2. **Encounter TTL by design**: docs deleted after 3-5 days; patient search scoped to a unit relies on the
   encounter index — old visits are unreachable via search.
3. **Synchronous writes inside DB transactions** — Solr latency directly blocks registration; rollback uses the
   APO-layer compensation (ServiceLayerAspect:1164-1182, :557-604).
4. Solr outage degrades to empty search results (methods return `[]`, `handleSolrException` :1508), and item/ICD/HF
   searches silently return nothing; DB fallback only exists for the registration consistency check (≤15 hits).
5. `service` collection orphaned; `serviceSolrName` dead config.
6. Index drift: realtime writes + flag drains can double-write; `SOLR_STATUS` flag semantics differ per table
   (int boolean vs Oracle 0/1 via `getBooleanValueForQueryWQ`).

### 1.7 Modernization notes (Solr)
- **Keep Solr** (see preservation note). Target improvements:
  - Introduce explicit commit/softCommit tuning + a dedicated indexing queue (Kafka/RMQ) to decouple tx path.
  - Promote Redis mirror to a full read-through cache or replace with DB-backed consistent reads; remove dual-write
    to Redis for `current_pat_` (or keep as strict TTL cache).
  - Add circuit breaker + explicit degrade-to-DB fallback for patient search.
  - Migrate collections to managed schema / schema-less design; document field dictionary (currently only
    `artifacts/solr.patient.schema` partial artifact exists).
  - Reconsider 3-day encounter retention — data-loss risk for post-discharge lookups (per legal retention of
    records, encounters should move to an archive collection rather than be deleted).
  - Replace rsync-based DR sync and midnight restart cron with SolrCloud proper replication/balancing.
  - Version-bump: Solr 7.5.0 → 9.x (ZK removal option), keep edismax boost semantics (bq weights are load-bearing).

---

## 2. REDIS usage

### 2.1 What is cached (key pattern → owner → TTL → invalidation)

| Key pattern | Owner (file:line) | TTL | Invalidation |
|---|---|---|---|
| `current_pat_<patientId>`, `current_pat_<mrno>` (PatientSolrView) | SolrUtil.java:181-183 | 100s | TTL only; re-added on each index write |
| `current_enc_<encId_E/A>` (EncounterSolrView) | SolrUtil.java:172,1359; EmergencyAndWalkinVisitServiceImpl:1844 | 100s | TTL only |
| `<masterName>` (≈375 `@CrudCached` masters: genderMaster, prefixMaster, maritalStatusMaster, companyMaster, planMaster, itemcategory, bedMasterMap, auditEventMaster…) | CrudOperationsFilter.java:139-330 (write), MasterCachedServiceImpl (read) | default 3600/86400 | On CRUD through filter (`updateCachedObject`), incl. delete path :567 |
| `<masterName>_<key>` map/list variants | CrudOperationsFilter.java:193-237,244-296 | default | On CRUD |
| `gender_prefix_maritalstatus_map` | SolrUtil.java:1314,1350 | default | TTL |
| `<token>` → UserView (session) | SSOAuthFilter:170; TokenBasedSecurityInterceptor:32 | `session.timeout` min | Logout/re-login (DefaultAuthenticationImpl:847-852) |
| `UNQ_SESS_USER_<userId>` → token (+`_BI`) | DefaultAuthenticationImpl:847-852 | — | Re-login |
| `ABDM_REQ_<reqId>` / `ABDM_RES_<reqId>` | CommonABDMUtil:350-394 | — | Explicit remove after consume |
| `ABDM_PAT_AUTH_<txId>` (JWT) | CommonABDMUtil:444-499 | — | remove on completion (ApplicationJobs:1998) |
| `ABDM_HPR_AUTH_<userId>` | CommonABDMUtil:515-518 | — | TTL |
| `ABDM_SES_TKN` (ABDM session) | CommonABDMUtil:569-591 | expiresIn from provider | Re-fetch when expired |
| `HIP_ABDM_KEYS` / `HIU_ABDM_KEYS` (DH key material) | CommonABDMUtil:301-303 | 365d | TTL |
| `ABHANUM_<healthIdNumber>` → patientId | PatientServiceImpl:6743; AbhaServiceImpl:518-520 | — | remove after lookup |
| QMS room token queues (`pushToQ/removeFromQ`, `<qName>_S` counters, `*_time` sums) | QMSUtil:28-126; QueueManagmentServiceImpl:317-1346 | touch to day-end | Queue ops |
| `CDD_<opdipdid>` discharge lock | ServiceLayerAspect:1147-1156 | 60s | On completion |
| `INV_RI_<indentId>` receive lock | ServiceLayerAspect:1192-1201 | 60s | On completion |
| `DOCHKOUT_yyyyMMdd` job guard | ApplicationJobs:591-608 | to midnight | TTL |
| `disableAutoDemoAuth`, `disablePatientIndexing`, `disableCreatePackageFromGlobal` (kill-switches) | ApplicationJobs:1690-1701,2123-2125,2178 | — | manual |
| `USR_BUILD_<userId>` | BaseServiceImpl:1037 | — | — |
| `perf.current.db.locks` | ApplicationJobs:925-973 | — | overwritten each run |
| `<nodename>_ABDM_S` / `<nodename>_ABDM_F` counters | ApplicationJobs:2033-2034 | — | — |
| `nogzip_masterdata_for_` / `gzip_masterdata_for_` (gzip response cache) | RequestResponseInterceptor:85,91 | — | — |

All keys are prefixed with `cache.database.prefix=risl_2233` (application.properties:133; AbstractCacheImpl.getKey).

### 2.2 Important caveats (evidence-first)
- **The checked-in cache bean is `MemoryCacheImpl`** (`src/main/resources/applicationContext.xml`), expiry 86400.
  `RedisCacheImpl` (standalone, `spring.redis.host=oracle.sms:6379`) and `RedisClusterCacheImpl`
  (cluster nodes commented) exist and are **commented out** in the XML. `cache.database.prefix=risl_2233` and
  the staging snapshot (`artifacts/risl/application.properties`, cluster `10.68.180.49:7001-7006`, prefix
  `risl_stag_new`) prove Redis was/ is the intended store in deployment. **This is a deployment-config risk:
  multi-node prod running MemoryCacheImpl = per-node divergent master data, tokens, and Solr mirrors.**
- `sessionCache` (BaseServiceImpl:110) is the same bean as `cacheImpl` (no separate bean exists).
- Serialization: Kryo (RedisCacheImpl ser/unser :57-87). Jedis connection factory; pool max-active 200.
- Redis vs others: ehcache → only GzipFilter gzip wrapper; hazelcast/spymemcached deps present but unused
  (commented HazelCastCacheImpl in XML); no `@Cacheable`/`@CacheEvict` usage found (Spring cache annotations absent).

### 2.3 Modernization notes (Redis)
- Decide one topology: standalone vs cluster; make it explicit and enforced (config currently half-commented).
- Promote `current_pat_/current_enc_` to proper read-through or drop (it exists only to hide Solr soft-commit lag).
- Move session/token, ABDM correlation, and queue/counter data to typed structures (JSON) — Kryo binary across
  versions is fragile.
- Replace ~375 reflective `@CrudCached` master cache with a declared cache (e.g., Caffeine local + Redis pub/sub
  invalidation) — current invalidation is per-node and write-through from every node.

---

## 3. EXTERNAL INTEGRATIONS

### 3.1 Inventory (summary table)

| # | Integration | Direction | Protocol | Trigger | Config keys (application.properties) | Key classes (file:line) |
|---|---|---|---|---|---|---|
| 1 | **ABDM (HIP/HIU/CM/HFR/HPR/PHR/ABHA)** | Both | HTTPS REST + JWE/JWS, FHIR R4, callbacks | API (user), scheduled jobs, inbound callbacks | abdm.* (:1156-1220, :1484-1493), abdm.servers (:1465) | xcg/abdm/server/ABDMController.java:160 (40 endpoints), HFRController:60, CallbackAPI, CommonABDMUtil, HealthIdUtil, HIPUtil, HIUUtil, PHRUtil, HPRUtil, AbhaServiceImpl, HPRServiceImpl; models/trx/abdm/*; ABDMRequestQueue |
| 2 | **HCX (claims/pre-auth)** | Out (also callbacks) | HTTPS REST, JWE | API + ABDM gateway | abdm.hcx.url, abdm.hcx.participant.url, abdm.keys.file.path (:1492-1493,1505) | xcg/hcx/*, xcg/abdm/client/hcx/* (NHCXServiceHelper, HCXPayloadEncryptor/Decryptor, KeyLoader, InsurancePlanRequest), services/abdm/NHCXServiceImpl, models/hcx/* |
| 3 | **Aadhaar / Janaadhaar / biometric** | Out (pull) | HTTPS REST (+ local RD device HTTP) | User action (registration/ABHA) | risl.janaadhaar.* (:662-688,1224-1228,1436-1437,1485-1488), aadhar.api.biometric.url (:934), aadhaar.auth.api (:1266), local.biometric.scan.url (:942) | dto/aadhar/AadharObjectDto, services/aadhar/*, CommonUtilServiceImpl:299, IntegrationServiceImpl:1578-1625,1922-2068, PatientServiceImpl:307 |
| 4 | **e-Mitra** | Out (initiate) / In (redirect) | HTTPS REST + AES | Online appointment (kiosk) | emitra.* (:1447-1464) | util/EmitraServiceHelper:128-212, util/EmitraAESUtil, interceptors/emitra/EmitraRedirectFilter:108-178 |
| 5 | **EJS (ASHA/outreach MCH)** | Both | REST (internal + portal data) | User + scheduled incentives | ejs.asha.incentive.schedule (:940) | services/ejs/* (~10 services), EjsDetailsServiceImpl:109-2105, EjsIncentiveServiceImpl, ~80 dto/ejs |
| 6 | **Chiranjeevi Yojana** | Out | HTTPS REST | Admission/user | chiranjivi.* (:890-894,1236-1238) | models/chiranjeevi/RefrenceTid, PatientServiceImpl:298-320, admission/discharge flows (INSURANCESCHEMEID=2) |
| 7 | **RGHS + SIMS** | Both | HTTPS REST | Scheduled + eligibility check | rghsapp.api.url, rghs.ihmsTid.api.url, rghs.ihmsDocs.api.url (:962,1246-1248), rghsSimsData.api.url, simsRGHS.AuthKey, rghsSims.schedule (:978-982), dirs (:1310-1318) | ApplicationJobs.rghsDataScheduler:1063-1092, IntegrationHelper.addSimsRGHSData, IntegrationServiceImpl:643,1646,1790, dto RGHSSimsRequestDto, RGHSOpdMetadata |
| 8 | **e-Aushadhi (RMS)** | Both | HTTPS REST (SOAP-style) + Basic auth | Indent approval (AOP), scheduled stock sync | eAushadi* (:862-870,1050,1306,1430,1489-1491) | InventoryEAushadiIntegrationImpl:110-1963, IntegrationHelper, ApplicationJobs:2197,2376,2961-2967, ServiceLayerAspect:1204-1213 |
| 9 | **EHR/EWD** | Out | File-based (dir) + REST | Scheduled (currently disabled) | ehr.dir.path, ehr.sync.schedule (dup :820/:1060), emr.* | services/ehr/EHRUtilServiceImpl:91-1205, ApplicationJobs.creteEHRFiles:1094-1106 |
| 10 | **HL7 v2.x (HAPI)** | Both | HL7 v2 (pipe) via DB queue | Scheduled pickup (currently disabled) | hl7inbound.pickup.freq (:339), hl7standard.validation (:343), hl7.billingOrder.messages.enabled (:824) | jobs/Hl7InboundMessageJob:39-205, util/hl7/* (ProcessHL7InboundMessagesUtil:108-1341, HL7InboundHelperUtil, Hl7InboundMessagesValidationHelperUtil), util/HL7Helper:217-1091, models/transactionals/Inboundhis, Outboundhis, MasterServiceImpl:676-740 |
| 11 | **FHIR server (embedded)** | In/out | FHIR R4 (HAPI) | Any FHIR client (NDHM) | fhir.* (:1140-1152), abdm.enabled | xcg/genserv/FhirServer:41-134, PatientResourceProvider, TokenBasedSecurityInterceptor, CommonFhirUtilHelper, FhirDataUtil |
| 12 | **TIUXML (tele-ICU/CPRS documents)** | Out (file exchange) | XML templates → files | Discharge/clinical events | tiuxml.file.path (:986), saved.dischargeSummary.filepath (:1254), rghsPhotoDir/rghsPrescriptionDir (:1310-1312), kdl/prescription/stock dirs (:1435-1478) | util/tiuxml/TEMPLATE.java, FIELD.java, CPRS_TEMPLATE.java, util/XMLToJsonUntil, MasterCachedServiceImpl, NursingServiceImpl, PatientTransferDischargeServiceImpl |
| 13 | **MinIO (S3)** | Out | S3 (MinIO SDK) | File upload/download | minio.* (:750-766) | UploadFileServiceImpl (+8 other services), FhirDataUtil; **disabled by default (`minio.enabled=false`)** → filesystem fallback |
| 14 | **eSign (C-DAC)** | Out | HTTPS REST (esign) | Manual (document signing) | esign.* (:1495-1503) | util/CAeSignService:40-614 — **no Java caller found (UNKNOWN wiring)** |
| 15 | **SMS/WhatsApp (eSanchar)** | Out | HTTPS REST + queue table | Notifications, lab reports, scheduled retrigger | send.sms.url (:592), send.sms.url.otp (:624), whatsApp.* (:1064-1134,1242) | SendNotificationServiceImpl:125-174, models/WhatsappMessageQueue, ApplicationJobs:1598-1643, PathoOrderServiceHelper.sendCategoryWiseReportOnWhatsApp |
| 16 | **SSO (Rajasthan)** | In | HTTPS REST | Any authenticated access | risl.sso.* (:646-648,702,994-998,1270-1272), ssobasedAuthentication (:634) | interceptors/sso/SSOAuthFilter:69-178, util/auth/DefaultAuthenticationImpl, CustomLDAPAuthenticationImpl |
| 17 | **Print server** | Out | WebSocket (ws://) | Print jobs | print.server.url=ws://localhost:8182/ (:726) | IntegrationOperationsFilter:151,1046, SpringSecurityConfig:41-57 |
| 18 | **Equipment (Backman DxH 500)** | In | Vendor OLIS jar | Instrument polling | — (jar only) | artifacts/Equipment Interface/Backman_Access_DxH 500/OLIS_1.0.jar — **no src reference (UNKNOWN runtime wiring)** |
| 19 | **Jeevanvahini (108 ambulance)** | Both | HTTPS REST | Emergency case push/fetch | risl.jeevanvahini.api.fetch.url/.push.url (:722-724) | CommonUtilServiceImpl:224-227, PatientServiceImpl:277-281, IntegrationServiceImpl:2178 |
| 20 | **Mera Aspataal** | Out | (batch export) | Feedback window | mera.aspataal.* (:810-816) | PatientServiceImpl:286-296 |
| 21 | **eRaktkosh (blood bank)** | Out | HTTPS REST | Scheduled + user | eRaktkosht.url (:1022), eRaktkoshDonation.schedule (:1020) | ApplicationJobs:1205,1383, IntegrationServiceImpl:984,1207, BloodBankServiceImpl, dto/integration/ERaktkosh* |
| 22 | **NMC (statistics)** | Out | REST + DB staging | Scheduled nightly | nmc.client.id/secret (:946-950), nmc.sms.opdipd.url | ApplicationJobs.updateNMCData:1733-1800, IntegrationServiceImpl:238-259 |
| 23 | **PCTS (maternal tracking)** | Out | SOAP (asmx) | Scheduled (monthly) | pcts.pushdata.* (:1480-1483) | ApplicationJobs.pushDataToPCTS:2953-2959, IntegrationServiceImpl:196-202 |
| 24 | **Kaysons (wholesaler)** | Out | REST via e-Aushadhi RMSC | User + scheduled | kaysons.* (:1474-1475) | InventoryEAushadiIntegrationImpl:179-1923 |
| 25 | **Scan/print app** | Out | HTTPS local | Document scan | scan.print.api.url=https://localhost:4443 (:1439) | DefaultAuthenticationImpl:185 |
| 26 | **Bhamashah (Rajasthan scheme DB)** | Out | HTTPS REST | Patient lookup | risl.bhamashah.aadhar.api.url, risl.bhamashah.api.url | PatientServiceImpl:271, IntegrationOperationsFilter:136 |
| 27 | **eUpkaran (lab equipment)** | Out | REST | User | — | IntegrationServiceImpl:1537 |
| 28 | **EXT LIMS (state lab)** | Out | DB insert (staging) | Scheduled (disabled) | — (config ISEXTLMISENABLED in DB) | ApplicationJobs.addStats_Archived:2556-2943 |

### 3.2 Detail notes per high-risk integration

**ABDM (1)** — Biggest integration surface (~50 endpoints, 8 async jobs). Failure handling: Redis correlation store
(`ABDM_REQ_/ABDM_RES_`), DB queue (`ABDMRequestQueue` with ISSEND/SYNCHRONIZED/NODE), 5s retry jobs, node-sharded
processing (8 servers), per-node success/fail counters in Redis. Data exchanged: ABHA creation/linking, consent
artefacts, care-contexts, HIUs, FHIR bundles, HFR facility registration, HPR professional registration.
Milestones: M1/M2 gated (`abdm.milestones`). Sandbox vs prod config blocks coexist (comment sets).

**HCX (2)** — claims: coverage-eligibility + pre-auth bundles; JWE encryption/decryption with local key files
(`abdm.keys.file.path`). Depends on ABDM gateway SBX.

**e-Aushadhi (8)** — indent lifecycle: create → issue → QC status → return (final), stock reconciliation,
item-master sync (scheduled 18:00), auto-indent from main store (unit-wise cron). Basic-auth header hard-coded
in properties (Base64). Timeout 10s. Failure: ack DTOs + `reTriggerEAushadhiIndent` AOP hook (ServiceLayerAspect:1204).

**HL7 (10)** — Outbound messages created synchronously in transaction (registration/visit/admission/orders/lab results),
stored in `Outboundhis` for review; **no active outbound sender found**. Inbound pickup job disabled; historical
integration to lab/CPOE/Pyxis (ADT/ORM/ORU/DFT/REF/OMP) — dormant in checked-in config. `hl7.billingOrder.messages.enabled=false`.

**WhatsApp (15)** — write-behind queue `T_WHATSAPPMESSAGEQUEUE` drained every 15 min (window 14:00-07:00) by
`triggerWhatsappReportsInQueue`; retrigger job commented; status table `T_WHATSAPPMESSAGESTATUS`; max trigger count 5.

**SSO (16)** — external SSO token validation (GetTokenDetail), then local token issued into cache with
`session.timeout` TTL; mobile variant with fixed encrypt key `R@j$S0@02{09}19#` (hard-coded — secret risk).

### 3.3 Failure-modes summary
- **Silent degradation**: Solr down → empty search results; MinIO disabled → filesystem; e-Sign unwired;
  HL7 inbound disabled; EHR sync effectively disabled (duplicate conflicting property).
- **Hard-coded secrets in properties**: eAushadi Basic auth, WhatsApp keys, SIMS AuthKey, NMC tokens,
  SSO mobile encrypt key, chiranjivi auth token, esign placeholders (test values present).
- **Synchronous remote calls inside DB transactions**: Solr writes, HL7 outbound persistence, e-Mitra/Aadhaar
  lookups — latency/outage blocks the primary flow.
- **Config ambiguity**: dual cache impl (memory vs redis), dual ehr.sync.schedule, dual solr collection naming
  (QA suffix vs prod), sandbox vs prod ABDM blocks — deploy-time mistakes are likely.

---

## 4. Final summary

- **Solr collections (6 configured, 5 live):** patient, encounter, item, icd10cm, healthfactor (live);
  service (orphaned). Realtime indexers: patient/encounter from registration/admission/appointment/blood-bank/
  requisition/EJS services via `SolrUtil`; batch drainers: item/icd10cm/healthfactor/patient-backfill.
  Encounter index = 3-day rolling window.
- **Redis key patterns:** `risl_2233`-prefixed; Solr mirrors (`current_pat_*`/`current_enc_*`, TTL 100s),
  ~375 master caches (`@CrudCached`), session tokens, ABDM correlation/keys, QMS queues+counters, tx locks
  (60s), job kill-switches. Caveat: checked-in bean = MemoryCacheImpl; Redis impls present but commented.
- **Integrations counted: 28** (ABDM, HCX, Aadhaar/Janaadhaar/biometric, e-Mitra, EJS, Chiranjeevi, RGHS/SIMS,
  e-Aushadhi, EHR/EWD, HL7, embedded FHIR, TIUXML, MinIO, eSign, SMS/WhatsApp, SSO, print server, Backman
  equipment, Jeevanvahini, Mera Aspataal, eRaktkosh, NMC, PCTS, Kaysons, scan/print, Bhamashah, eUpkaran, EXT LIMS).
- **Top risks:** (1) Solr availability is load-bearing and coupled to DB transactions with weak failure
  semantics — must be preserved but hardened; (2) ambiguous cache implementation (Memory vs Redis cluster) and
  hard-coded credentials; (3) dormant integrations (HL7 inbound, EHR sync, eSign, EXT LIMS) create false
  expectations; (4) encounter index 3-day retention = potential record-retention/compliance gap; (5) 3-node
  Solr 7.5 + rsync DR + midnight restart cron — fragile ops pattern to carry into target architecture.
