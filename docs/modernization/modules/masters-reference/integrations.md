# M05/M39 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| RGHS packages | Out | getRghsPackageList (TariffServiceImpl / patientRghsPackageDetails) |
| NHCX plans | Out | insurance_plan_on_request (nhcx templates, M38) |
| MDM (Janta) | Both | MdmServiceImpl `/mdm` — master data sync (Janta central) |
| Integration broker | Both | IntegrationServiceImpl `/integration`; `/ext/api` mapping (ROLE_INTEGRATION) |

## Solr
- service collection **orphaned** (`serviceSolrName` declared, never used — 07 §1.2 #6); item/icd10cm/healthfactor collections served from masters.

## Redis (07 §2.1) — **the cache hub**
- ~375 `@CrudCached` keys: genderMaster, prefixMaster, maritalStatusMaster, companyMaster, planMaster, itemcategory, bedMasterMap, auditEventMaster…; `<masterName>_<key>` variants; gzip/nogzip response caches; `USR_BUILD_<userId>`.
- **Deployment risk (07 §2.2):** checked-in bean = MemoryCacheImpl (in-process, per-node divergence); RedisCacheImpl/RedisClusterCacheImpl commented out; cache.database.prefix=risl_2233.

## Scheduled jobs
- NoAuthCacheTTLReset (TTL resets); syncItemMaster/syncIcd10cm/syncHealthFactor Solr drainers (5s).
