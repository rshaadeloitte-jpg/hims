# AGENT-07 — Integrations / Solr / Redis / External Systems — Evidence Dump

Legacy HMIS "iHMS/OHUM PAS NG" at `D:\ihms`. Research-only. Every claim has file:line evidence.
Path convention: `src/main/java/com/ohum/...` unless stated.

---

## 1. SOLR — Evidence

### 1.1 Technology & topology

| Item | Evidence |
|---|---|
| Client | Raw SolrJ `SolrClient` (CloudSolrClient) — **not** Spring `SolrTemplate`. Bean: `App.java:249-253` `new CloudSolrClient.Builder(solrCloudNodes.split(","), Optional.empty())` |
| spring-data-solr usage | Only `@SolrDocument`/`@Indexed` annotations on the 5 view DTOs (`dto/solr/*.java`). No `SolrTemplate` anywhere in `src/`. `spring.data.solr.repositories.enabled=false` (application.properties:1283) |
| ZK ensemble | `spring.data.solr.zk-host=10.10.5.151:2181,10.10.5.152:2182,10.10.5.153:2183` (application.properties:166); staging snapshot `artifacts/risl/application.properties:57` = `10.68.180.49:2181-2183` |
| Prod topology (from ops scripts) | Solr 7.5.0 + ZooKeeper 3.4.13 on 3 nodes `10.70.241.198/199/200`; collections 2-shard: `patient`, `encounter`, `healthfactor`, `icd10cm`, `item` — **no `service` collection in sync script** (`artifacts/risl/solr-scripts/sync/scripts_node1_78_198/_sync.sh:31-48`) |
| Collection names | QA: `*_risl_qa` (application.properties:174-190); PROD: `patient`, `encounter`, `item`, `service`, `icd10cm` (commented defaults application.properties:198-210; staging artifacts/risl/application.properties:59-63) |
| Ops scripts | `solr-scripts/status/check.sh` (per-minute process check + mail); `solr-scripts/restart-cron/zkslr_ctl.sh` (daily midnight solr+zk restart); `solr-scripts/sync/{node}/sync.sh+_sync.sh` (rsync index dirs from primary node to DR nodes, ionice) |
| Schema artifact | `artifacts/solr.patient.schema` — field list for patient/encounter docs (types text_general/plong/pdate/boolean) |
| Config flag | `spring.data.solr.enabled=true` (application.properties:158); gated in every SolrUtil method via `if(!solrEnabled)return;` |
| Wildcards | `solr.search.wildcardqueries=false`, `mrno.reverse.search` (application.properties:182,120; SolrUtil.java:123-124) |

### 1.2 Document models (collections → classes → fields)

| Collection (cfg key) | View class | Key fields (`@Field`) | Doc ID scheme |
|---|---|---|---|
| `patient_risl_qa` (solr.patient.collection.name) | `dto/solr/PatientSolrView.java` (`@SolrDocument` at :24) | mrno_s_lower, firstName_s_lower, middleName_s_lower, lastname_s_lower, fullName_s_lower, genderId_l/gender_s_lower, prefixId_l/prefix_s_lower, maritalstatusId_l/maritalstatus_s_lower, registraionDate_dt, mobileCountryCode_s_lower, mobileNo_s_lower, alternateMobileNo_s_lower, email_s_lower, civilid_s_lower, passportno_s_lower, drivinglicno_s_lower, parentPatientId_l, isVip_b, familyIdNo_s_lower, memberId_s_lower, bplNumber_s_lower, resAddress/resArea/resPinCode_s_lower, resCityId_l, resDistrictId_l, resStateId_l, resCountryId_l, isDeath_b, dateOfBirth_dt, unitId_l, raceId_l, resiNo_l, religionId_l, nationalityId_l, languagePreferedId_l, isRural_b, fatherName_s_lower, motherName_s_lower, kinName_s_lower, kinLastname_s_lower, kinRelationId_l, kinMobileNo_l, relation_s_lower, occupationId_l, occupation_s_lower, husbandName_s_lower, patEctsNo_s_lower, bloodGroupId_l, bloodGroupName_s_lower, pctsNo_s_lower, rationCardNo_s_lower, janaadharId_s_lower, janaadharMemberId_s_lower, reverseMrno_s_lower, identityNumber_s_lower, identityTypeId_l, insuranceSchemeName_s_lower, plan_s_lower, insuranceSchemeId_l, insPlanId_l, insuranceSchemeNo_s_lower, enrId_s_lower, healthIdNumber_s, healthId_s, defaultHealthId_s | `id` = patientId (PatientSolrView.java:28-32) |
| `encounter_risl_qa` (solr.encounter.collection.name) | `dto/solr/EncounterSolrView.java` | isAdmission_b, encounterDate_dt, patientId_l, tariffId_l, companyId_l, associatedCompanyId_l, planId_l, fromTime_dt, toTime_dt, visitMark_b, appointmentReasonId_s, doctorId_l, departmentId_l, appointmentTypeId_l, isChargeable_b, visitTypeId_s, refEntityId_l, token_s, type_l, opdNo_s, isRequireReschedule_b, isCheckout_b, clinicId_l, isDoctorCheckout_b, referralDepartmentIds_s, isEncounterClosed_b, isSkipFromQueue_b, isTelemedicineAppt_b, isArrived_b, availableForVideoCall_b, status_b, isBloodBankVisit_b, telemedEncounterId_l, isFpv_b, isPctsRegistration_b, isMlc_b, isMlcCompleted_b, isBroughtDead_b, transportrequired_b, transporttype_s, transporttype_dt, tid_s, buildingId_l (EncounterSolrView.java:32-201) + all PatientSolrView fields | `id` = `<encId>_E` or `<admId>_A` (SolrUtil.java:289,333,1358) |
| `item_risl_qa` (solr.item.collection.name) | `dto/solr/ItemSolrView.java` | ~75 fields: itemCode_s, brandname_s_lower, strength_s, itemName_s_lower, moleculename_l, itemGroup_l, itemCategory_l, dispencingtype_l, mfgByString_s, mrkByString_s, purchaseRate_s, mRP_s, vatper_s, inventorycode_s_lower, isasset_b, otc_b, nursingOrderable_b, doctorOrderable_b, isKit_b, status_b, isPharmacy_b, isMedicalGases_b, isLinen_b, expirydate_dt, ... (ItemSolrView.java:31-311) | `id` = itemmaster.id |
| `service_risl_qa` (solr.service.collection.name) | **NO view class, NO query code** — declared `SolrUtil.java:114` + `ApplicationJobs.java:144-145` but never read/written. ORPHANED/LEGACY. `service` collection absent from prod sync script too. | — | — |
| `icd10cm_risl_qa` (solr.icd10cm.collection.name) | `dto/solr/Icd10cmSolrView.java` | icdcode_s, shortdescription_s_lower, longdescription_s_lower, pnlocallang_s_lower, status_b, alias_s, parent_s, category_s, codingsystem_s (Icd10cmSolrView.java:30-62) | `id` = icd master id |
| `healthfactor_risl_qa` (solr.healthfactor.collection.name) | `dto/solr/HealthFactorSolrView.java` | code_s_lower, description_s_lower, category_s, hfLocalLang_s, snomedDisplay_s (HealthFactorSolrView.java:24-40) | `id` = hf id |

### 1.3 Indexing flows (DB → service → SolrUtil → collection)

**Write path core:** `SolrUtil.indexPatientOrEncounterInSolrInternal()` SolrUtil.java:150-196 — enrich from cached masters (gender/prefix/etc. via `getGenderPrefixMaritalstatusmap()` :1313-1353), writes Redis cache `current_pat_<id>` / `current_pat_<mrno>` / `current_enc_<id>` (TTL 100s) (:172,181-182), registers rollback compensation props `solr:<collection>=<id>`, `redis:id`, `redis:mrno` on `UserTrxContextHolder` (:173,184-186), then `solrClient.addBean(<collection>, view)` (:174,187). **Commits are commented out** (:175,188) — autoSoftCommit relies.

| Trigger | Where indexed | Callers |
|---|---|---|
| New patient registration | `indexPatientInSolr(AddPatientRegistrationDto,AddPatientRegistrationDetailsDto)` :198-203 | PatientRegAdmissionServiceImpl:875,1662; PatientRegApptServiceImpl:1299; PatientServiceHelper:761; BedReservationServiceImpl:165; EjsDetailsServiceImpl:2105 (outreach) |
| Patient update | `indexUpdPatientInSolr(PatientRegistrationDto,PatientRegistrationDetailsDto)` :205-210 | PatientRegAdmissionServiceImpl:1180,1665; PatientServiceHelper:4363; PatientTransferDischargeServiceImpl (via view) |
| Patient minimal update (middle/last name cleared) | `indexUpdPatientInSolrMinimal` :1007-1013; `indexPatientInSolr(PatientSolrView)` :1893-1899 | PatientServiceImpl:5609,5688,6829; PatientServiceHelper:4481 |
| New encounter | `indexEncounterInSolr(dto1,dto2,sponsor,enc)` :212-219 | PatientRegApptServiceImpl:986; BloodBankServiceImpl:1623; PatientRequisitionServiceImpl:746,849 |
| Encounter from existing patient doc (uses `current_pat_` cache or Solr getById) | `indexEncounterInSolr(Encounter,sponsor)` :239-258 | EmergencyAndWalkinVisitServiceImpl:1798; MarkAndAddVisitServiceImpl:402; BloodBankServiceImpl:1429,1475,1625; PatientRequisitionServiceImpl:749,851,854; SolrUtil.indexOneEncounter :1731 |
| New admission | `indexAmissionInSolr(...)` :230-237 | PatientRegAdmissionServiceImpl:1663 |
| Admission update | `indexAmissionUpdPatInSolr(...)` :221-228 | PatientRegAdmissionServiceImpl:1665 path |
| Admission from DB row | `indexAdmissionInSolr(Object[],sponsor)` :260-279 | via `indexOneAdmission` :1735-1788 (uses SP `CIMS_GETSPONSORBYADMISSIONID_NEW_DOC` :1744) |
| Sponsor/tariff change | `updateSponsorInSolr` :1130-1145; partial field update `updateFieldsInSolr` :2082-2148 (atomic `set` via SolrInputDocument) | AddUpdateAppointmentServiceImpl:191 (encounter); PatientTransferDischargeServiceImpl:4400,4413 (patient); CommonABDMUtil:1391 (patient healthId) |
| Encounters of updated patient refreshed | `indexUpdEncounterInSolr` :1113-1127 | PatientServiceHelper:4200? (search `getEncountersByPatientId`) |
| Close/delete encounter | `clearClosedEncounters` (@Async :281-296), `clearClosedEncountersSync` :298-308, `clearEncounterInSolr` :330-338 | PatientTransferDischargeServiceImpl:698,823,3198,3282; daily close job ApplicationJobs:563 |
| Delete patient + all encounters | `clearPatientSync` :310-316 | ServiceLayerAspect:576 (rollback), EmergencyAndWalkinVisitServiceImpl |
| Delete patient | `deletePatientFromSolr` :1901-1908 | searchPatientInSolr consistency check :625 |
| HealthFactor delete / ICD delete | `removeHfIndexFromSolr` :2060-2066; `removeIcd10CmIndexFromSolr` :2068-2074 | master delete flows |
| Reindex single encounter/admission | `indexOneEncounter` :1680-1733; `indexOneAdmission` :1735-1788 | manual/API repair |
| Missing patient/encounter repair | `indexMissingPatient(hid,pid)` :1790-1812 (big native SQL join of T_REGISTRATION_INT+T_REGISTRATION_DETAILS_INT); `indexMissingEncounter` :1814-1891 | EmergencyAndWalkinVisitServiceImpl:3953,3965,3978,4037,4107,4145 (REST repair endpoints) |

**Master-data collections (flag-based incremental sync, no realtime indexing):**

| Collection | Sync method | Drainer job | Source query | Flag column updated |
|---|---|---|---|---|
| item | `syncItemMaster(List<Itemmaster>)` SolrUtil.java:965-997 (`addBeans` :990) | `syncItemsToSolrJob` @Scheduled(fixedDelay=5000) ApplicationJobs.java:867-882 | `Itemmaster where status=true and solrStatus=false` :873 | `M_ITEMMASTER.SOLR_STATUS` :992 |
| icd10cm | `syncIcd10cmMaster` :1579-1602 (`addBeans` :1596) | `syncIcd10cmToSolrJob` fixedDelay=5000 :976-989 | `IcdCmMaster where status=true and solrStatus=false` :981 | `M_ICD10_CM.SOLR_STATUS` :1597 |
| healthfactor | `syncHealthFactorToSolr` :2032-2058 (`addBeans` :2052) | `syncHealthFactorToSolrJob` fixedDelay=5000 :884-899 | `Healthfactormaster where status=true and solrStatus=false` :888 | `M_HEALTHFACTORMASTER.SOLRSTATUS` :2054 |
| patient | `syncSolrPatients(List<PatientSolrView>)` :899-963 (`addBeans` :952) | `indexMissingPatientsInSolr` fixedDelay=5000 ApplicationJobs.java:2122-2171 (only node in `solrsync.servers` :2131, window 18:00-07:00 :2127, batch 1000, `SOLR_STATUS=0` since 2023-01-01 :2158) | T_REGISTRATION_INT+_DETAILS_INT | `T_REGISTRATION_INT.SOLR_STATUS` :955,958 |
| patient (legacy drainer) | same | `syncPatientsToSolrJob` — **commented out** :520-540 | — | — |

**Encounter retention:** `closeDailyEncountersJob` @Scheduled(cron=`dailyencounter.schedule`) ApplicationJobs.java:542-570 — closes encounters between `solr.encounter.retain.days`±1 (application.properties:795 `solr.encounter.retain.days=3`) then `clearClosedEncountersSync` :563. **Encounter index = rolling 3-5 day window by design.**

**ABHA id backfill to Solr:** `updateHealthIds` (disabled; @Scheduled commented :2039) — batch atomic `set` of `healthId_s`/`healthIdNumber_s` via `SolrInputDocument` on patient collection (ApplicationJobs.java:2067-2093).

### 1.4 Search queries (all in SolrUtil.java — the sole search API)

| Method (line) | Collection | Purpose | Notable query mechanics |
|---|---|---|---|
| `searchPatientInSolr` :340-642 | patient OR encounter (`allPatients` flag :603) | The main patient search (registration/OPD screens) | `defType=edismax`, `bq` boosts (mrno^18, mobile^16, firstName^10, middle^8, last^8, fullName^12, civilId^16, janaadhar^17 …), trailing `*` wildcards, `reverseMrno_s_lower` reverse-search :444-451, filter `*:*`+fq, sort registraionDate desc, rows≤100 :582-601; first checks Redis `current_pat_` :343-371; DB consistency re-check (`query.isRegSearch()`, ≤15 ids) :618-628, deletes stale docs :625 |
| `searchPatientInEncouter` :644-897 | encounter | Find patient within unit (OPD/IPD/ward) | fq `isAdmission_b`+`unitId_l`(+`opdNo_s:[* TO *]`) :689-696, same boost set, sort encounterDate desc, dedupe by patient :882-890 |
| `searchPatientInEncounterForCheckinCheckout` :1516-1577 | encounter | Check-in/checkout queue by unit/cabin/department/doctor/dates | fq AND of unitId/clinicId/departmentId/doctorId/isDoctorCheckout/isEncounterClosed/visitMark/date-range :1525-1556 |
| `searchExactPatientInSolr` :1015-1059 | patient | Duplicate registration guard (mobile+DOB or janaadhar pair) | exact AND fq, sort id desc :1039 |
| `searchPatientInSolrByIdentity` :1061-1094 | patient | civilId/passport/drivingLic lookup (used by `BaseServiceImpl.findPatientByAadhaar` :259-267, `findPatientByIdentity` :289-304, merge flows) | OR fq |
| `getPatientByMrno` :1147-1171 / `getPatientByMrnoAndUnitId` :2345-2378 | patient | MR no lookup | fq `mrno_s_lower` (Redis check first) |
| `searchPatientInSolrByIds` :1273-1291 | patient | bulk getById |
| `searchABHAPatientHIDInSolr` :2150-2216 / `searchABHAPatientHIDInSolrList` :2218-2268 | patient/encounter | ABDM patient matching (mobile+DOB±1yr) | Levenshtein name match ≤3 :2187-2188 |
| `searchJanadharPatientsInSolr` :2270-2296 | patient | Bulk ABHA candidates (janaadhar w/o healthIdNumber) |
| `searchPatientByProperty` :1910-1960 / `searchPatientByPropertyNew` :2298-2343 | patient | by id/passport/civil/dlic/healthId/mobile |
| `searchAllItemsInSolr` :1173-1271 | item | Item search (pharmacy) | fq isPharmacy/isMedicalGases/isLinen/isasset :1188, name/brand with `*` prefix-suffix, rows 10000 :1250 |
| `searchAllIcd10cmInSolr` :1604-1678 | icd10cm | ICD search | shortdesc/icdcode/alias OR :1628, complaint R*/OHC.* filter :1630-1642 |
| `searchHealthfactor` :1962-2001 / `searchMultipleHealthfactors` :2003-2030 | healthfactor | HF search | description_s_lower wildcard, rows 10000 |
| `getAppListSearchFilters` :1394-1453 / `getAppListTypeFilter` :1455-1494 | (builder) | Token/appointment list filters | maps appointment type→fq string (e.g. rghs → `insuranceSchemeName_s_lower:RGHS` :1488) |

Client-side: patient search is server-side REST only; JS calls the API (no direct Solr hits found; confirmed by absence of solr refs in static JS — search flows go through the `SearchPatientGeneralDetailsResultDto` REST DTOs).

### 1.5 Failure handling & consistency

- `handleSolrException` SolrUtil.java:1508-1514 — swallows RemoteSolrException with "SyntaxError" (returns empty list), else logs. Search methods return empty list rather than failing the request when solr down (e.g. :636-641).
- Transaction rollback compensation "APO layer": ServiceLayerAspect.java:1164-1182 — on failure of `savePatient`/`addwalkinVisit`/`createAppointment`/`registerPatientFromOutreach`/`addBedReservation`, deletes Solr doc by `solr:<coll>` prop and Redis keys by `redis:*` prop.
- `handleTrxTimeouts` ServiceLayerAspect.java:557-604 — on trx timeout, reverses Solr admission/encounter/patient writes (:564,576,583,596).
- Reads fall back Redis `current_pat_`/`current_enc_` (100s TTL) before hitting Solr (SolrUtil:343,357,1155,1359,1376,242-249).
- `syncSolrPatients` marks `SOLR_STATUS` in same transaction as `addBeans` (SolrUtil.java:951-962) — at-least-once flag semantics; job drains `SOLR_STATUS=0`.
- No Solr circuit breaker; no bulk API for realtime writes (per-trx addBean).

---

## 2. REDIS — Evidence

### 2.1 Cache abstraction (com/ohum/cache — 6 files)

| File | Role |
|---|---|
| `CacheInterface.java` | Contract: get/add/replace/setIfAbsent/touch, cache groups, counters (`initCounter`/`incCounter`/`decCounter`), sets, queues (`pushToQ`, `prependToQ`, `removeFromQ`, list ops), raw strings, multi-get, local cache invalidation. `DEFAULT_CACHE_EXP_TIME=3600` |
| `AbstractCacheImpl.java` | Key prefixing: `getKey()` prepends `${cache.database.prefix}` (`risl_2233` application.properties:133) — so Redis keys = `risl_2233<key>`; local in-JVM map `mlocalCache` for `@CrudCached(isLocalCached=true)` masters (ALL_GLB_KEYS built by reflection :42-58) |
| `MemoryCacheImpl.java` | In-memory impl ("only for testing, not clustered" :22-23). **CURRENTLY THE ACTIVE BEAN** — `src/main/resources/applicationContext.xml`: `cacheImpl = MemoryCacheImpl`, expiry 86400s. Redis impls are commented out in the same file (:28-42). |
| `RedisCacheImpl.java` | Standalone Redis impl via `JedisConnectionFactory`; Kryo serialization (`ser`/`unser` :57-87); `setIfAbsent` = `SET key val NX EX` :111-120 |
| `RedisClusterCacheImpl.java` | Redis Cluster impl via `JedisClusterConnection` (JedisConnectionFactory :38) — **bean commented out** in applicationContext.xml |
| `CacheQuery.java` | Query DTO for searchCache |

- `sessionCache` (`BaseServiceImpl.java:110-111`) autowires `CacheInterface` — same bean as `cacheImpl` (no second bean defined anywhere; applicationContext.xml defines only `cacheImpl`). So session/token/queue/counter data lives in the SAME cache store as master data.
- Redis connection props: `spring.redis.host=oracle.sms`, port 6379 (application.properties:110-112); jedis pool max-active 200 etc. (:144-152); `spring.redis.timeout=5000` (:152); cluster nodes commented (:116-128). Staging snapshot: cluster `10.68.180.49:7002-7004`, prefix `risl_stag_new` (artifacts/risl/application.properties:37-48).
- **Deployment reality (root properties + XML): checked-in config runs MemoryCacheImpl; Redis intended via commented beans/prefix `risl_2233`; risk: master-data cache and tokens per-JVM in multi-node prod if XML not switched.**

### 2.2 Key patterns inventory (file:line)

**Master data (owners: `@CrudCached` on ~375 models + `MasterCachedServiceImpl`):**
- `@CrudCached(name=...)` per master table (e.g. `genderMaster`, `prefixMaster`, `maritalStatusMaster`, `companyMaster`, `planMaster` — consumed SolrUtil.java:1316-1323; `itemcategory` SolrUtil.java:968). Write path: `CrudOperationsFilter.updateCachedObject` (CrudOperationsFilter.java:139-330) — on CRUD through the filter, list/map caches refreshed (`cname`, `cname_<key>`) ; delete path :567. Cache keys = annotation names; local-cached masters also in JVM map (AbstractCacheImpl:36-57).

**Solr-adjacent (owner: SolrUtil):**
- `current_pat_<patientId>`, `current_pat_<mrno>` — PatientSolrView, TTL 100s (SolrUtil.java:181-183); read :242-249,263-269,343-371,1155,1376, getPatientByMrno :1155
- `current_enc_<encId_E|_A>` — EncounterSolrView, TTL 100s (SolrUtil.java:172,1359,1361; EmergencyAndWalkinVisitServiceImpl:1844)
- `gender_prefix_maritalstatus_map` — master lookup map (SolrUtil.java:1314,1350)

**Session/authentication (owner: DefaultAuthenticationImpl, filters, interceptors):**
- `<token>` → UserView — token→user session (SSOAuthFilter:170 `cacheImpl.add(view.getToken(), view, sessionTimeout*60)`; TokenBasedSecurityInterceptor:32 `sessionCache.get(token)`)
- `UNQ_SESS_USER_<userId>` → token (DefaultAuthenticationImpl.java:847-852, with `_BI` suffix token removal)

**Locks / guards / flags (owner: ServiceLayerAspect, ApplicationJobs, CommonUtilServiceImpl):**
- `CDD_<opdipdid>` discharge-in-progress lock, TTL 60s (ServiceLayerAspect:1147-1156)
- `INV_RI_<indentId>` receive-in-progress lock, TTL 60s (ServiceLayerAspect:1192-1201)
- `DOCHKOUT_yyyyMMdd` once-a-day job guard (ApplicationJobs:591-608, TTL to midnight)
- `disableAutoDemoAuth` (ApplicationJobs:1690-1701), `disablePatientIndexing` (ApplicationJobs:2123-2125), `disableCreatePackageFromGlobal` (ApplicationJobs:2178)
- `USR_BUILD_<userId>` building mapping (BaseServiceImpl:1037)
- `perf.current.db.locks` DB lock stats (ApplicationJobs:925-973)

**ABDM (owner: CommonABDMUtil/AbhaServiceImpl/HIPUtil/HIUUtil):**
- `ABDM_REQ_<reqId>`, `ABDM_RES_<reqId>` request/response correlation store (CommonABDMUtil:350-394)
- `ABDM_PAT_AUTH_<txId>` patient auth token (CommonABDMUtil:444-499; ApplicationJobs:1998)
- `ABDM_HPR_AUTH_<userId>` HPR token (CommonABDMUtil:515-518)
- `ABDM_SES_TKN` ABDM session token (CommonABDMUtil:569-591, TTL=expiresIn)
- `HIP_ABDM_KEYS` / `HIU_ABDM_KEYS` — DH key material, TTL 365d (CommonABDMUtil:301-303; used HIUUtil:298,350,389)
- `ABHANUM_<healthIdNumber>` → patientId (PatientServiceImpl:6743; AbhaServiceImpl:518-520)

**Queue management (QMS) (owner: QueueManagmentServiceImpl + QMSUtil):**
- Room queues `pushToQ(<qName>, tokenNo)` — token queue per room (QueueManagmentServiceImpl:490,504; QMSUtil:48-126)
- Counters `qName+"_S"` sequence counter (QMSUtil:28-32; incCounter QueueManagmentServiceImpl:317,582,738-756,1345-1346); counter keys `*_time` for wait-time sums
- `ABDM` per-node counters `<nodename>_ABDM_S` / `<nodename>_ABDM_F` success/fail counters (ApplicationJobs:2033-2034)

**Gzip masterdata response cache (owner: RequestResponseInterceptor):**
- `nogzip_masterdata_for_` / `gzip_masterdata_for_` compressed DTO byte-caches (RequestResponseInterceptor.java:85,91)

**TTL policy:** defaults 3600 (interface), 86400 (active MemoryCacheImpl bean), explicit TTLs: 60 (locks), 100 (Solr views), 300s+ (tokens = `session.timeout` min), ABDM keys 365d, ABDM session TTL from provider.

**Invalidation:** master-data on CRUD via CrudOperationsFilter:139-330; tokens on re-login (DefaultAuthenticationImpl:847-852); Solr-view keys are TTL-only (no explicit invalidate on update — replaced by re-add during same tx). `invalidateLocalCache()` exists but no caller found except cache-group flush paths.

### 2.3 Redis vs others
- ehcache: only `GzipFilter` uses `net.sf.ehcache.constructs.web.GenericResponseWrapper` (gzip) — GzipFilter.java:39-40
- hazelcast (dep pom.xml:512-517), spymemcached (pom:399), ignite — commented/absent in code; applicationContext.xml has commented HazelCastCacheImpl + IgniteCacheImpl beans
- Redis cluster impl exists (`RedisClusterCacheImpl`) — bean commented out

---

## 3. EXTERNAL INTEGRATIONS — Evidence inventory

### 3.1 ABDM (Ayushman Bharat Digital Mission) — HIP/HIU/CM/HFR/HPR/PHR/ABHA

| Aspect | Evidence |
|---|---|
| Config | `abdm.enabled=true`, client `SBXID_001996`, gw `dev.abdm.gov.in/gateway`, healthid, cm, hiecm, hfr (facilitysbx), hpr, phr, abha, idp, session urls, `abdm.version=v3`, `abdm.milestones=M1,M2`, `abdm.bridge.cb.url` (application.properties:1156-1220); PROD block commented :1374-1428; `abdm.servers=server234..server241` (8 app nodes :1465); `abdm.facility.link.url` :1484; `abdm.hcx.*` :1492-1493 |
| Inbound callbacks | `xcg/abdm/server/ABDMController.java` @Path("/abdm") :160 — ~40 callback endpoints: patients/profile/share, on-find, links/link/init+confirm, care-contexts/discover, consent-requests/on-init, consents/hip/notify+hiu/notify, health-information/hip/request+hiu/on-request, subscriptions, heartbeat, token on-generate (list :244-1340). `HFRController` @Path("/abdm-hfr") :60 — facility registration endpoints (:92-382). `CallbackAPI.java` |
| Outbound | `CommonABDMUtil` (session mgmt, keys, JWE), `HealthIdUtil`/`AbhaUtil` (ABHA create/link via abdm-client dep, pom:937), `HIPUtil` (consent/health-info transfer), `HIUUtil` (HIU flows), `PHRUtil`, `HPRUtil` (professional reg), `NHCXUtil`; services `abdm/AbhaServiceImpl.java`, `HPRServiceImpl.java`, `NHCXServiceImpl.java` |
| Async queue | `ABDMRequestQueue` model; `sendPatientRecordToAbha` job (ApplicationJobs:1688-1728, node-assigned via `addAbhaRecordsNode` :1661-1683); `abhaDemoAuthProcessor` bulk demo-auth (ApplicationJobs:1899-2037, 25 threads, TBL_JAN_MEMBER_AADHAAR_REF_NO); `sendRecord.ABDM` cron :1262; `jobForPushingAbhaIdToJanAadhar` (disabled :2539) |
| Persistence | models/trx/abdm: PatientAbdm, PatientAbdmToken, PatientAbdmCR, PatientAbdmCC, PatientAbdmCRData, UserFaceAuth; `ABDMRequestQueue` |
| FHIR bridge | embedded HAPI FHIR R4 `FhirServer` (xcg/genserv/FhirServer.java:41-134) with `PatientResourceProvider`, TokenBasedSecurityInterceptor, ConsentInterceptor, NPM validation package `fhir.packages=ndhm.tgz` (application.properties:1140); `FhirDataUtil`, `FhirUtil`, `CommonFhirUtilHelper`; bean `genFhirServer` gated on abdm.enabled (App.java:259-264) |
| Failure handling | Redis store `ABDM_REQ_/ABDM_RES_`; queue table with ISSEND/SYNCHRONIZED/NODE flags; retry via 5s jobs; failure counters in Redis; `abdm.http.logs=true` |

### 3.2 HCX (Health Claim Exchange) — insurance
- `xcg/hcx` + `xcg/abdm/client/hcx/*`: NHCXServiceHelper, HCXFhirRequestDataUtil, HCXFhirResponseDataUtil, HCXJWEPayloadParser, HCXPayloadEncryptor/Decryptor, KeyLoader, InsurancePlanRequest, HcxBundleHtmlRenderer
- Config: `abdm.hcx.participant.url=https://apisbx.abdm.gov.in/pmjay/sbxhcx/participanthcxservice`, `abdm.hcx.url=https://apisbx.abdm.gov.in/hcx`, `abdm.keys.file.path=/tmp` (application.properties:1492-1493,1505; NHCXUtil.java:59-77)
- Service: `abdm/NHCXServiceImpl.java`; models `models/hcx/NHCXRequest.java`, `NHCXRequestDetails.java`; coverage-eligibility + pre-auth bundles (`xcg/hcx/dto/*.java`)

### 3.3 Aadhaar / Janaadhaar / biometric (Rajasthan)
- Config: `risl.janaadhaar.api.url` (fetchJayFamily), `.aadhar.api.url`, `.api.photo.url`, `.family.api.url`, `.mobile.janaadhar.api.url` (+ direct/new API variants :1485-1488), encryption `risl.janaadhaar.encrptyApi.url/key/Id` :1224-1228, push key/Id :1436-1437; `aadhar.api.biometric.url` (sewadwaar kyc bio) :934; `aadhaar.auth.api` (GenericFaceAuthApi) :1266; `risl.bhamashah.aadhar.api.url` (PatientServiceImpl:271); `local.biometric.scan.url=http://localhost:9263/scan` :942 (PatientServiceImpl:307 — local RD scan server); `risl.bhamashah.api.url` (IntegrationOperationsFilter:136)
- Code: `dto/aadhar/AadharObjectDto.java`; `services/aadhar/*`; CommonUtilServiceImpl Janaadhaar fetch + verification (:299 aadhar.api.biometric.url); aadhaarAuthenticate + generateEncryptedData/decryptResponse endpoints IntegrationServiceImpl:1578-1625; OTP flows :1922-2068; FamilyMemberDetails model (biometric JSON)
- Trigger: patient registration / face-auth ABHA enrolment / KYC lookup

### 3.4 e-Mitra (Rajasthan citizen services gateway — appointment/registration payments)
- Config: endpoints kiosk-details, back2back (serviceTransaction), update-app, verify-transaction; `emitra.sso.getTokenDetails`, merchant-code RISLTEST, encryption-key, service.id.appt=3649, service.id.report=3789, office.code, revenue.head, checksum.key, comm.type (application.properties:1447-1464)
- Code: `util/EmitraServiceHelper.java` (getKioskDetails :128, updateApplicationId :187, back2back tx), `util/EmitraAESUtil.java` (AES encryption), `interceptors/emitra/EmitraRedirectFilter.java` (:108-162 — decrypt SSOTOKEN, validate via e-Mitra SSO API, store in Redis under token, cookie `__token`)
- Trigger: online appointment booking via e-Mitra kiosk

### 3.5 EJS (ASHA worker outreach — mother & child data)
- `services/ejs/*`: EjsDetailsServiceImpl (@Path "/ejs-details" :109; search/data endpoints per domain: members/diseases/mc/school/death/work/ecs/familyplanning/cbac/mchn/help/refer/abortion/familyawc/family/media/photos ~60 endpoints :135-1524), EjsAshaServiceImpl, EjsIncentiveServiceImpl (incentive calc job `ejs.asha.incentive.schedule` application.properties:940, ApplicationJobs:1035), EjsChecklistsServiceImpl, EjsAncPncChildServiceImpl, EjsPopulationServiceImpl, EjsMasterLocationServiceImpl, EjsSupervisorServiceImpl
- `registerPatientFromOutreach` (EjsDetailsServiceImpl:1906) → patient + `indexPatientInSolr` (:2105)
- DTOs: ~80 `dto/ejs/*`; models `ejs/EjsAshaWork` (IncentiveStatus), MediaTypes/Categories; user auth for outreach users in DefaultAuthenticationImpl

### 3.6 Chiranjeevi Yojana (maternity insurance — RGHS)
- Config: `chiranjivi.auth.token`, `chiranjivi.send.request.url` (addIpdDetail), `chiranjivi.check.status.url` (eidstatus), `chiranjivi.fetch.janBasedTidlist.url`, `chiranjivi.fetch.documentlist.tid.url` (application.properties:890-894,1236-1238); used in PatientServiceImpl:298-320
- Code: models/chiranjeevi (RefrenceTid), dto ChiranjiviYojnaDto/Response/DataDto/ChiranjeeviJanadharBasedDto; flows in PatientRegAdmissionServiceImpl/PatientTransferDischargeServiceImpl (INSURANCESCHEMEID=2 per ApplicationJobs:1127); emergency/bed flows
- Trigger: admission of chiranjeevi beneficiary; status polling by eid

### 3.7 RGHS (Rajasthan Government Health Scheme)
- Config: `rghsapp.api.url=http://10.68.180.47/RGHS/service/` :962, `rghs.ihmsTid.api.url` (gettidforIHMS) :1246, `rghs.ihmsDocs.api.url` (getDocsforIHMS) :1248; `rghsSimsData.api.url=http://10.70.230.199:8080/medical/api/AllApi/IHMS_GetData` :980, `simsRGHS.AuthKey` :982, `rghsSims.schedule` :978; photo/prescription dirs :1310-1318; `rghsapp` checkRGHSEligiblity (IntegrationServiceImpl:643); package upload :1646,1790
- Code: dto RGHSSimsRequestDto, RGHSOpdMetadata (dto/integration), `rghsDataScheduler` (ApplicationJobs:1063-1092 — pushes "R" status for sponsors with REFERENCENO, INSURANCESCHEMEID=3, 10-day old), integrationHelper.addSimsRGHSData; RGHS package master creation job :2177
- Trigger: scheduled nightly + admission-time eligibility; claim/SIMS reconciliation

### 3.8 e-Aushadhi (Rajasthan drug supply chain — RMS)
- Config: `eAushadiIndentRequestUrl`, `eAushadiReturnRequestUrl`, `eAushadiItemStockUrl`, `eAushadiAuthorization` (Basic), `eAushadiRequestServiceId=101`, `eAushadi.api.timeout=10`, `eAushadiFinalReturnRequestUrl`, `eAushadhi.currentStockDtl.api`, `eAushadhiItemMaster.Details`, cookie, `eAushadiItemStockUpdate.schedule` (application.properties:862-870,1306,1430,1489-1491,1050)
- Code: `services/custom/InventoryEAushadiIntegrationImpl.java` @Path("/eAushadiIndent") :110 — indent :245, issueIndent :259, updateQCStatus :842, returnIndent :980/1004, consumption :1356, stock by store :1371/1537, Kaysons stock :1807-1923, item master update :1963; `IntegrationHelper` (updateEAushadhiItemMaster), jobs `createAutoIndentForMainToEAushadhi` (ApplicationJobs:2376), `updateItemMasterFromEaushadhi` (0 0 18, :2961-2967); ack DTOs IndentEAushadiAcknowledge etc.; main-store auto-indent job :2197
- Trigger: indent approval (AOP hook ServiceLayerAspect:1204-1213), scheduled stock sync nightly, return approvals

### 3.9 EHR / EWD (Electronic Health Record exchange)
- Config: `ehr.dir.path=C://EHR` :972, `ehr.sync.schedule` (cron 0 0 2 :820 AND fixedDelay 200000000000ms :1060 — duplicate, effectively disabled), `emr.url`, `emr.public.url`, `emr.manusername/password`, `emr.integration.*`; `operator.ehr.sync.cron=0 * * ? * *` :1280
- Code: `services/ehr/EHRUtilServiceImpl.java` @Path("/ehr") :91 — handleSyncReq :905, getAllPlatformUnits :965, findSSOUserDetails :1033, fetchDepartments :1106-1112, fetchAppointmentSchedule :1118, createVisit :1126, counts :1205; `creteEHRFiles` job (ApplicationJobs:1094-1106, unit.ehrCode grouping → EWD files to ehr.dir.path); TempPrescriptionNote → patient note job :2102-2117

### 3.10 HL7 v2.x (HAPI — laboratory/CPOE/ADT integration)
- Config: `hl7inbound.pickup.freq=0 0 1 * * ?` :339 (job **disabled**, @Scheduled commented Hl7InboundMessageJob.java:51), `hl7standard.validation=false` :343, `hl7.billingOrder.messages.enabled=false` :824
- Inbound: `jobs/Hl7InboundMessageJob.java` (ADT_A01/A02/A03/A04/A08/A45, ORM_O01, ORU_R01, DFT_P03(Pyxis), REF_I12, OMP_O09 — :58-69) drains `Inboundhis` (T_InBoundHIS) via `ProcessHL7InboundMessagesUtil.getOrders()` :144 (statuses: 1=picked, 3=failed :389,565...); validation `Hl7InboundMessagesValidationHelperUtil`; `HL7InboundHelperUtil` writes statuses
- Outbound: `util/HL7Helper.java` — createRegistrationHL7Message :217, createRegistrationUpdateHL7Message :491, createVisitA45 :748, createAdmissionA01 :1087, plus createPathOrderORM/RadOrderORM/ProcOrderORM/NursingOrderORM/CancelOrder (AddServiceOrderServiceHelper:3921+, PathoOrderServiceImpl) — persisted as `Outboundhis`; `hl7.billingOrder.messages.enabled` gates billing DFT
- UI: getListOfInboundmsgs/getListOfOutboundmsgs (MasterServiceImpl:676-740, status=3 failures)
- **Status: inbound pickup job disabled in checked-in config; outbound persisted, no active drainer found**

### 3.11 FHIR server (embedded)
- `xcg/genserv/FhirServer.java` — HAPI RestfulServer R4 (:75), PatientResourceProvider (:79), TokenBasedSecurityInterceptor (:85), ConsentInterceptor (:87), NPM validation `ndhm.tgz` (:108), remote terminology server `${fhir.terminology.server}=abdm` (:48,95-100); `fhir.auth.token` :1148, `fhir.server.egypt=true` :1150 (Egypt deployment flag), `fhir.server.gen=false` :1152, namespaces genserv ndhm abdm, clients ndhm abdm :1142-1146, `fhir.bundle.save.dir=/data/tiuxml/fhirBundles` :1468
- Registered via App.java:259-264 when abdm.enabled

### 3.12 TIUXML (Tele-ICU / clinical document XML exchange)
- Config: `tiuxml.file.path=D:/RISL/ihms/integration/tiuxml/tiuxmlmapping` :986; doc dirs: saved.dischargeSummary.filepath :1254, rghs photo/prescription :1310-1312, kdl.prescription.directory :1442, pharmacyUploadPrescription :1435, stockAdjustmentDocument/deptConsumptionDocument :1440-1441, postMortomReportDoc/mortuaryAmendmentDoc/patientDeathFormDoc :1012-1040, RCUpdateDocument :1466, BatchExpiryUpdatedDoc/AssetGrnUploadedDoc :1478-1479, KaysonsCurrentStock :1474
- Code: `util/tiuxml/TEMPLATE.java`, `FIELD.java`, `CPRS_TEMPLATE.java` (XML template model), `util/XMLToJsonUntil.java` (CPRS_TEMPLATE usage), consumed by MasterCachedServiceImpl (template file IO), NursingServiceImpl, AllMasterDataView, PatientTransferDischargeServiceImpl (discharge summary generation)
- Purpose: structured XML document exchange (CPRS = Computerized Patient Record System, tele-ICU/EMR documents)

### 3.13 MinIO (S3 object storage)
- Config: `minio.enabled=false` :750, `minio.url=http://10.10.5.147:9000/`, access/secret keys :754-756, buckets: staffPhotodoc.files.coll.name, policydoc.files.coll.name, mlcdoc.coll.name, radimg.coll.name, document.files.coll.name=docit-documentfiles (:758-766)
- Code: `services/custom/UploadFileServiceImpl.java` (document upload/download — ~100 cache/mime ops), FhirDataUtil (ABDM FHIR bundles), PatientTransferDischargeServiceImpl, PathoOrderServiceImpl, PharmacyServiceImpl, PatientRegApptServiceImpl, PatientRegAdmissionServiceImpl, PatientServiceHelper, IPDAdmissionServiceImpl, PatientPortalServiceImpl, DefaultAuthenticationImpl (profile photos)
- Note: `minio.enabled=false` — document storage falls back to filesystem (`document.files.path`)

### 3.14 eSign (C-DAC Aadhaar eSign)
- Config: esign.base-url/sso-id/secret-key/application-code/asp-id/response-url/esp-url/timeouts (application.properties:1495-1503)
- Code: `util/CAeSignService.java` — token :110, sign XML :162/208, sign PDF :292, ESP redirect; **no caller found in src** (standalone util, likely invoked from UI/scripts) — mark UNKNOWN wiring

### 3.15 SMS / WhatsApp (eSanchar gateway)
- Config: `send.sms.url` (sewadwaar eSanchar OBD CreateSMS) :592, `send.sms.url.otp` :624, whatsApp.api.sendmessage.url (esanchar.wabot.rajasthan.gov.in) :1108, apikey/username/password :1110-1114 (+ ihms variant :1120-1124), `whatsApp.max.trigger.count=5` :1130, `whatsApp.retrigger.schedule` :1134 (job commented :1578), `whatsApp.message.queue=18000` :1242; SMS creds send.sms.username/password/uniqueid/servicename :146-170 (SendNotificationServiceImpl)
- Code: `SendNotificationServiceImpl` (SMS/OTP, @Value block :125-174; MOIC daily SMS `dailySMSToMOIC`), `models/WhatsappMessageQueue.java`, `triggerWhatappLabReportOne` job (ApplicationJobs:1598-1605) + `triggerWhatsappReportsInQueue` (:1611-1643, drains T_WHATSAPPMESSAGEQUEUE → sendCategoryWiseReportOnWhatsApp), status table T_WHATSAPPMESSAGESTATUS (ApplicationJobs:1134)
- Trigger: lab report generation (category-wise), OTP, appointment notifications

### 3.16 SSO (Rajasthan state SSO)
- Config: `risl.sso.gettoken.url` (ssotest.rajasthan.gov.in:8888 GetTokenDetail) :646, `risl.sso.ihmsmapping.url` :648, `risl.sso.url` :702, `sso.rest.url` + user/pass :994-998, `risl.sso.mobile.url` + encryptkey :1270-1272; `ssobasedAuthentication` :634
- Code: `interceptors/sso/SSOAuthFilter.java` (gettokenUrl call :109, creates token → UserView in cache TTL session.timeout :170, cookie __token :178); `util/auth/DefaultAuthenticationImpl` (token mgmt), `CustomLDAPAuthenticationImpl`; LDAP config block :492

### 3.17 Print server
- Config: `print.server.url=ws://localhost:8182/` :726 (websocket)
- Code: IntegrationOperationsFilter:151-152 (`printServerUrl`, base URL whitelist :1046), SpringSecurityConfig:41-42 (CSRF/whitelist), is_print param handling :504-599 (dot-matrix print splitting)

### 3.18 Equipment interface (Backman DxH 500 hematology analyzer)
- Artifact: `artifacts/Equipment Interface/Backman_Access_DxH 500/OLIS_1.0.jar` — vendor OLIS driver jar
- **No source references to OLIS/Backman/DxH in src/main/java** — integration is via external driver/jar (lab instrument → LIMS/HL7 path); verify runtime wiring — UNKNOWN in code

### 3.19 Jeevanvahini (108 ambulance)
- Config: `risl.jeevanvahini.api.fetch.url=http://jeevanvahini-rj.emri.in/IHMS/fetchCase.php`, `.push.url=.../updateCase.php` (application.properties:722-724)
- Code: CommonUtilServiceImpl:224-227, PatientServiceImpl:277-281 (ambulanceApiFetchUrl/pushUrl); `pushEmergencyCaseDetails` endpoint (IntegrationServiceImpl:2178)

### 3.20 Mera Aspataal (hospital feedback/rating)
- Config: batch size 10000, valid fromTime/toTime 0-23, valid.days=5 (application.properties:810-816; PatientServiceImpl:286-296)

### 3.21 eRaktkosh (blood bank)
- Config: `eRaktkosht.url=https://eraktkosh.in/Blood_Bank/service` :1022, `eRaktkoshDonation.schedule` :1020
- Code: addeRaktkoshDonation (IntegrationServiceImpl:984), getEraktkoshRequest (:1207), `eRaktkoshSaveDonation` (ApplicationJobs:1205), `updateERaktkoshDonation` (:1383), BloodBankServiceImpl (donor/requisition), dto/integration/ERaktkosh*RequestDto; retrigger :1174-1201 area

### 3.22 NMC (National Medical Commission / hospital statistics)
- Config: `nmc.client.id`, `nmc.client.secret`, `nmc.sms.opdipd.url` (10.68.102.7:8090 SMSAPI) (application.properties:946-950); `updateNMCData` job (ApplicationJobs:1733-1800, cron 0 0 3, inserts into NMC_NEW_2023 OPD/IPD/lab/charge aggregates for 11 units :1752); tokenEndpointforDataSyncing + getOpdIpdData endpoints (IntegrationServiceImpl:238-259)

### 3.23 PCTS (Pregnancy Child Tracking System)
- Config: `pcts.pushdata.url=https://pctsrajmedical.rajasthan.gov.in/public/ihms/ihms.asmx/SubmitIPDOPDData`, userid/password/salt (application.properties:1480-1483)
- Code: IntegrationServiceImpl:196-202 (creds), `pushDataToPCTS` job (ApplicationJobs:2953-2959, cron 0 0 0 L (monthly last day)); isPctsRegistration flag in encounter Solr view (EncounterSolrView:169)

### 3.24 Kaysons (pharmacy wholesaler)
- Config: `kaysons.currentStock.Document.path`, `kaysons.eAushadhi.currentStockDtl.api` (application.properties:1474-1475)
- Code: InventoryEAushadiIntegrationImpl:179-182 + endpoints getKaysonsCurrentStockDetails :1807, OTP :1886-1904, opening balance :1923 — stock sync via e-Aushadhi RMSC SOAP

### 3.25 Other flags/keys
- `scan.print.api.url=https://localhost:4443` (DefaultAuthenticationImpl:185 — scan/print app); `risl.connectToTelemediceRoom.api.url` (PatientServiceImpl:283); `risl.bhamashah.*`; `score.url`, `mdm.url` (IntegrationOperationsFilter:142-145); `ohumdoc.integration.enabled` + `ohumDoc.authToken` (docs); `abdm.facility.link.url`; `allowed.urls`/`allowed.referer.urls` (CORS whitelists incl. aadhaarauth.rajasthan.gov.in :730,858); `risl.bhamashah.api.url`; `eUpkaranItemStockDetails` endpoint (IntegrationServiceImpl:1537 — eUpkaran = lab equipment portal)

---

## 4. UNKNOWNS / GAPS
- `service` Solr collection: no code path reads/writes it — orphaned (both QA and prod naming).
- CAeSignService (eSign): defined, no Java caller found — wiring may be UI/script side.
- Backman OLIS jar: no code reference — runtime/wiring unknown.
- Active cache impl: checked-in XML = MemoryCacheImpl; Redis keys (risl_2233 prefix) and Redis cluster configs imply prod intends Redis — deploy-time XML not in repo (applicationContext.xml in src/main/resources is the deployed artifact — flag).
- `ehr.sync.schedule` duplicated with contradictory values (2 AM cron vs 200000000000ms fixedDelay) — last wins = effectively disabled.
- Inbound HL7 pickup job disabled in checked-in source.
- Redis standalone `spring.redis.host=oracle.sms` vs cluster config both present — only standalone active per comment block.
