# M05/M39 — Workflows

## Master CRUD + cache flow (04 §4, 07 §2.1)
1. CRUD through generic filter: `POST /api/rest/<entity>/crudadd|crudupdate|cruddelete|crudaddMultiple` → GenericServiceImpl validates `MBase.validate()` → save → cache invalidation.
2. Cache: `@CrudCached` masters (~375 keys) read via MasterCachedServiceImpl (264 endpoints); invalidation on CRUD through filter (`updateCachedObject`, incl. delete path); default TTL 3600/86400; TTL resets via `NoAuthCacheTTLReset`.
3. `crudaddMultiple` uses `REQUIRES_NEW` per item (one bad item doesn't roll back a batch — GenericServiceImpl.java:59-66).

## Tariff application flow (M04 consumers)
- Billing charges look up tariff/service/package rates from cache (`tariffMaster`, `classMasterMap_*`); RGHS packages via getRghsPackageList; NHCX plans via insurance_plan_on_request.

## Report dispatcher flow (M39, recon §1)
- `/IHMS/api/rest/reports/<reportName>` → Nashorn `reports.js` + report configs (loaded at startup) → dispatch to ReportServiceImpl → Velocity `generic_reports.vm`.

## Generic CRUD routing (recon §1)
- Filter pattern `{context}/api/rest/([^/]+)(.*)` — explains 1,109 frontend URLs (e.g. `accidenttype/all`, `bedmaster/upload`, `citymaster/{id}`) — a data-driven API layer, NOT dead links.
