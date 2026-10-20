# Agent 03 — FAUG/jQuery Frontend Reverse-Engineering: Findings (Evidence Dump)

**Scope:** `D:\ihms\src\main\resources\public` (FAUG framework HMIS frontend)
**Mode:** Research only — no files in D:\ihms were modified.
**Companion artifacts (same folder):**
- `api-map-per-file.md` — **the API map**: every `api/rest` endpoint found in `js/*.js` with `file → endpoint → line → calling function`
- `api-map-js-with-functions.txt` — raw `file|line|function|endpoint` table (3,218 rows, all 71 controllers)
- `api-map-raw-js-json.txt` — raw `file:line\tURL-fragment` for js + all schema JSONs + sub-apps (7,404 rows)
- `api-endpoints-normalized.txt` — 3,867 unique normalized endpoints with every occurrence `file:line`
- `route-inventory.md` — 533 routes from `fjs-config.json` grouped by module

---

## 1. STATS (evidence-backed)

| Metric | Value | Evidence |
|---|---|---|
| Routes (screens) in `fjs-config.json` | **533** `module:screen` routes | fjs-config.json `router.routes` (regex scan, module-prefixed only) |
| Route helper keys (load/crudop/varAssign/draw) | ~412 additional keys | same scan |
| Module configs `fjs-config_*.json` | **40** | `public/` dir listing |
| Schema JSON files | **1,308** | `modules/*/schemas/*.json` count |
| HTML templates | **2,942** | `modules/*/templates/*.html` count |
| JS controllers (`public/js`) | **~71** (≈14 MB) | dir listing |
| API references in `js/*.js` with function context | **3,218** | api-map-js-with-functions.txt |
| Unique normalized endpoints (all sources) | **3,867** | api-endpoints-normalized.txt |
| Endpoints under `api/rest/` unique | **3,857** | same |
| Raw API references (js + schemas + sub-apps) | **7,404** | api-map-raw-js-json.txt |
| Nursing charting format files | 29 (`charts/ODCWF_*.json`) | charts/ dir |
| Master-data import files | 60+ CSV/XLS | data/ dir |
| Sub-apps embedded in public/ | `queue/`, `ohumlite/` (each has own index.html + faug-min.js + fjs-config.json) | dir listing |
| FAUG engine | faug-min.js 360 KB, faug-ext.js 18 KB, faug-ext-globalize.js 37 KB | dir listing |

## 2. BOOTSTRAP & CONFIG LOADING

- `index.html` is a near-empty shell: `<body m_tenant_id="1" defaultTenant="">` + spinner + logo; loads ~100 vendor scripts then `tiuxml.js`, `js/components.js`, `js/common-module.js`, `js/qz-tray.js`, `ext.js` (from `resources/ext.js`?? — **no: `ext.js` is referenced but not present in `public/` root — resolved at runtime by the backend (likely injected)** — UNKNOWN, flagged).
- FAUG loads `fjs-config.json` via `$.ajax({dataType:"json", url: Fg.getConfigPath()})` where `getConfigPath()` returns `"fjs-config.json"`; if `settings.aTokenWhere == "queryparam"` token is appended `?<aTokenName>=<aTokenValue>`, else sent as header (faug-min.js `__loadfaugconfig`).
- `fjs-config.json` contains: `settings`, `router.routes` (hash routes), `router.fragments`, `router.firstroute`, `schemas` (key→file list), `templates`.
- Route modules are **lazy-loaded**: on `hashchange`, FAUG parses the hash `#<module>:<screen>/<params>`, and if the module isn't registered yet loads `fjs-config_<module>.json` (faug-min.js: `"fjs-config_"+t+".json"`), registers schemas/templates, then calls the route handler (process function).
- Setting `settings.enableRouteModules = true`; `settings.numApiWebWorkers = 3` — API calls run in **Web Workers** (`w-api-call.js` uses `XMLHttpRequest` + `postMessage`; main thread posts `[meth, url, contType, content, vheaders, cachedTime, blockUi, strace, ckey, addIdempotentRequestId]`; responses cached in worker `cachedApiData[ckey]`).
- `settings.layerFile: "layer/default.json"` — default UI rendering config (form element markup: `form.element.container.class="form-group"`, radio/checkbox/array layouts, table chrome: search box, items-per-page, pagination, download button, modal template, freeze loader). Overridable per schema via `details.form.custom.*`.
- `settings.idEncryptFields = "<<ENCCRYPTED_FIELDS>>"`; `settings.catchApiUncaughtExceptions=true`.
- `common-module.js:12660` — `$.get('v_check?param='+$('html').attr('v'), ...)` version check endpoint (non-REST).

## 3. ROUTING (screen = `#module:screen`)

Route object shapes (fjs-config.json):
- `{"func": "startAppointmentFlow"}` — pure JS entry point (controller function, defined in module JS files loaded globally).
- `{"crudop": {schemaName, op, vars}, "func": ...}` — runs a schema CRUD op (viewer/table) then calls func.
- `{"draw": ...}` / `{"load": ...}` — load schema/template and draw.
- `{"crudop": ...}` under `masters:<%arg1%>/view|add|edit/:id` — generic master CRUD screens parameterized by arg1 (schema name from route param). **This is a generic "schema designer" driven screen** (`masters:formConfiguration/:fileType`, `masters:formdesigner`, `commons:editSchema/:schemaName`, `commons:addForm`, `tiuxmls:designer`, `tiuxmls:tiuxmls/:txml`).
- Route params: `op:addAppointment/:doctor_id/:doctor_name/:department_id/:app_date/:from/:totime` — variables injected into schema via `vars` (`"unit_id": "route@unitId"` etc.) or `fromRoute`.

Route security: `Faug.execFunc("onBeforeRouteChange", ...)` hook (defined in common-module.js) can veto navigation; `disableBackForwardButton: true`.

## 4. SCHEMA FORMAT (schema-JSON-driven forms) — evidence: modules/op/schemas/walkinVisit-schema.json (6,621 lines)

Top-level keys:
- `templatize: true` — schema is itself a template
- `type: "object"`
- **`dynamicAspects`**: array of `{cond: ["|op|expected:source@var"], valueset: {path: value}}` — conditional schema mutations. Condition sources: `gvar@<globalvar>`, `object@<prop>`, `this@value`, `form@<field>`, `route@<param>`, `class@...`, `id@...`, `data@...`; operators seen: `|=|` (equals), `|!=|`, `len|>=|n`, `|!tb|` (not blank), `|!u|` (not undefined). valueset paths like `properties.mobileNo.required`, `properties.departmentId.custom.container.custom.class`, `crud.c.url`, `crud.c.callbacks.onValidateOp`, `details.form.submit.label`, `propertyOrder` (full reorder of fields). Example: `"|=|true:gvar@isExtLMISEnabled" → properties.mobileNo.required=true`.
- `properties`: field definitions. Field keys seen:
  - `type`: string|number|date|boolean|html (static HTML block), `transient: true` (not serialized)
  - `display` (label, may embed `%%Fg.g(gvarName)%%` i18n/global injection, e.g. `"%%Fg.g(mrn_label)%%"`), `showlabel`, `showicon`, `showhelptext`, `show`, `required` (also `"%%Fg.g(IsPatientMobileNumberMandatory)%%"` — dynamic required!)
  - `enum`: literal options `[[1,"Appointments"],[2,"Emergency"]]` OR `[]` + `events.default: [{type:"func", fromVar:"gender_master_full_new"}]` → options populated from a cached global master list (gvar arrays loaded at login)
  - `genericOptionTemplateFile`/`genericTemplateFile`/`optionTemplateFile` — option rendering template (e.g. `generic-radio-list.html`, `religion-master-list.html`)
  - `custom.class`, `custom.icon.custom.class`, `custom.container.custom.class/attrs`, `custom.customTemplate` (HTML with `<%cc%>` container class, `<%cs%>` style, `<%ca%>` attrs, `<%label%>`, `<%icon%>`, `<%element%>`, `<%helptext%>`)
  - `elementHtml` — full custom element HTML (e.g. Age Y/M/D composite; mobileNo with country-code input pair)
  - `validations.cond`: array of `[condition, errorMsg]`; conditions: `func:validateJanaadharId`, `regex|=|^[A-Za-z][A-Za-z .]*$:this@value`, `|!tb|:this@value`; `validations.errorselector: "helpText"`
  - `events.default` / `events.change`: `{type: "ajax", meth: "GET", url: "api/rest/...", done: [...]}` (chained), `{type: "full", func: "jsFunction", done:[...]}`, `{type: "func", fromVar: "masterListVar"}`; supports `cachedTime` (ms cache), `freezeUi:false`, `target: "otherFieldId"` (write response into another field's options)
  - `valueAssign.fromVar/fromData` (auto-fill from gvar/response), `serializeValueFunction` (e.g. `convertToJacksonDateTimeFmtforBabyOf`), `serializeValuePosition` (`a:data-value`), `faug-serialize-function` (`encodeAdharToBase64`)
  - `dateFormat: "%%Fg.g(GenericUIDateFormat)%%"`, `dateConfig: {maxDate: 0}`
  - `defaultIndex` (preselect dropdown option), `addId: true`
- `details`: `{name, label, labelEleId, form: {id, create, parentEleId, position, showReset, isModal, noHtml5Validation, validateHiddenElements, custom: {class, style, properties.customTemplate, attrs}, submit: {label, custom:{el, class, attrs}}, callbacks: {afterDraw: {func, controlEvents: [...]}}}, callbacks: {afterLoad: {func}}}`. **controlEvents**: declarative jQuery binding — `{jqselector, event, cond, varAssign, crudop: {schemaName, op, vars}, func}` — e.g. `.pat-mrnsearch-box` keyup ≥8 chars → set `pat_mrn_no` → run `crudop AddPatientRegistrationDto/patientMrnSearch` → call `resetPreviousVisitData` (walkinVisit-schema.json:6157-6175).
- `crud`: operation map. Ops: `c` (create), `r`/`ra` (read/read-all), `u`, `d`, custom names. Each op: `{meth, url, contentType, content (JSON template with <%var%>), exResCd, template (result list template), showdropdownmenu, dropdownmenuItem: {click: {func, varAssign, attrAssign, valueAssign}}, vars, callbacks: {onValidateOp, beforeOp, afterOp: {func}, failure: {func}}}`. Evidence walkinVisit-schema.json:6555-6620 (`crud.c.url = "api/rest/walkinVisit/addwalkinVisit?pat_token_no=<%token%>&app_type=<%app_type_w%>"`, `onValidateOp: "validatePatientDetails"`, `beforeOp: "setWalkinObject"`, `afterOp.func: "setViewOpdChargesRoute"`, `failure.func: "showErrorMsgForAppt"`).
- List/table schemas: `details.viewer: [{id, parentEleId, position, isDataTable, serverPaginate, callbacks}]`, `defaultOp: "ra"`, `crud.ra: {template, forTemplate:true, callbacks.afterDraw.func}` (e.g. showNurseWorklistSchema.json → template `roleNurse.html`, afterDraw `loadWorkList`).
- Nested objects: `properties.mlcDetailsDto.properties.formalityRemark` — sub-object forms; arrays via `properties.items.properties.*` (e.g. `patientSponsor.items.properties.planId`).

## 5. TEMPLATE ENGINE (faug-ext.js)

- `FaugExtensionFactory.templateEngine` compiles HTML to JS:
  - `<% expr %>` — inline expression/JS output
  - `#code` lines — JS statements (compiled; `#` lines inside `##...##` blocks for multi-line)
  - `##var x = 1; if(...) {...}##` — multi-line JS block
  - `!!templateName arg1 arg2!!` — include sub-template via `Faug.includeTemplate(name, args)`
  - output escaped with `Fg.eh()` unless line prefixed `!`
  - `isVarFromOptions` mode: vars from `arg` object
- Templates get `Faug`/`Fg` globals; common calls: `Faug.getRoute()`, `Faug.getVar("x")`, `Fg.g("gvar")`, `Faug.getDateUtil(date).format(...)`, `rows` array for list templates (see patient-search-list.html: uses `rows[i].mRNo`, `.mobileNo`, `Faug.getDateUtil(r.dateofBirth)`).
- Table/listing output uses FAUG "table" chrome from layer/default.json (`_t_*` classes: search box `_t_sb`, download `_t_dw`, pagination `_t_f*`) with object flags `page_enabled`, `search_enabled`, `dwnld_enabled`, `invoke_apionchange`.

## 6. GLOBALS / gvar MECHANISM

- `Faug.addGlobalVar(name, val)` / `Faug.getGlobalVar(name)` / `Faug.addVar` / `Fg.g(name)` / `Fg.ag("login_data", data)` / `Fg.setVar`.
- Schema `%%Fg.g(Name)%%` injects gvar values at render; schema `gvar@name` in cond/valueset; `fromVar:"name"` fetches option lists.
- Login response seeds the globals (auth-init-module.js:10-110): `login_data` (whole payload), `user`, `unit_id`, `hospitalName`, `SSOUrl`, `MDMUrl`, `scanPrintApiUrl`, `userstoreIds`, `crr_Default_Store_id`, `doctorId`, `login_user_id`, `login_user_role`, `login_user_role_l`, `tkn`, `isNhnc`, `draftIpBill`, `otProcedurePreBilling`, `isMLCRole`, `isSuspension`, plus all `isXxx` feature flags (isExtLMISEnabled, isNhncEmer, isFPV, isPctsRegistration, isTelemedicine, isCheckinFlow, isInsuranceCategoryMap, appointmentAgainstUnitHead, isPctsRegistration…) consumed by `dynamicAspects`.
- Feature-flag gvars drive entire screen variants (walkinVisit has 9 dynamicAspects branches for kiosk/nhnc/FPV/PCTS/checkin/telemedicine/LMIS).
- `Faug.getVar()` vs `getGlobalVar()`: route/flow-scoped vars (addVar) vs session-wide (addGlobalVar). Both read via `Fg.g()`.
- Master-data caches as gvars: `gender_master_full_new`, `prefix_master_full`, `religion_masters`, `occupation_masters`, `qualificationMaster`, `Plan_master_full`, `config_app`, `staffmasters`.

## 7. AUTH & SESSION

- Login: `api/rest/auth/loginf` (POST, `application/x-www-form-urlencoded`) → `onLoginSuccess` (common-module.js:1007); OTP variant `api/rest/auth/loginotp`.
- Role fetch: `api/rest/auth/getuserrole` (+ `getuserroleOtp`); SSO: `api/rest/usermanagement/loginsso`; SSO→MDM: `api/rest/usermanagement/getMDMTokenWithSSOToken?userdetails=...` (auth-init-module.js:334).
- Role switch: `api/rest/auth/changeUserRole?userId=&unitId=&roleId=&setAsDefaultRole=` (auth-init-module.js:5271).
- Logout: `api/rest/auth/logout` (PUT).
- Token: gvar `tkn`, appended to API calls; `auth-init-module.js:198` `$.ajaxSetup({error:...})` global error handler; token refresh interval for biReport (`refreshTokenForBiInterval`).
- Session expiry template `modules/commons/templates/session_expire.html`; roles UI: `roleBasedNav.html`, `selectDefaultRole.html`, `unit-role-change.html`.
- localStorage: `ls_locale_language`, `ls_locale` (i18n), `DefaultSystemPrinter`, `BarcodePrinter`, `NoOfCopies`, `isADotMatrixPrinter`, `box`, `fixed`, `CSS`, `CSStxt`.
- Hidden legacy token headers hardcoded: `{token: "ZDgxOTM2ZTQtNGMwNi00MjI4LWIzMGQtOWEzNzgzYWZiNTA4"}` in getuserrole calls (common-module.js:1136).

## 8. API CALL PATTERNS (wrapper taxonomy)

Main-thread wrappers (faug-min.js): `Faug.ajaxGet(blockUi, url, contentType, content, headers, success, error, addIdempotentRequestId)`, `Faug.ajaxPost(...)`, `Faug.ajaxPut`, `Faug.ajaxDelete`, `Faug.ajaxCall(meth,...)`, promise style `Faug.get(url).done()`, `Faug.post`, `Faug.put`, `Faug.api(true, "GET", url, ...)`, `Fg.api(...)`.
- First param `true` = freeze UI (blocking spinner).
- `X-Request-ID` idempotency header added for POST/PUT/DELETE when requested (w-api-call.js:7).
- Worker path (default): settings.numApiWebWorkers=3; window `message` handler in w-api-call.js; cache via `cachedTime` (e.g. schema events `cachedTime: 120000`).
- Schema-declared calls: `crud.*.url`, `properties.*.events.*.url` — FAUG executes with its own XHR; response handling via `done` chains, `dropdownmenuItem.click`, `valueAssign`, `varAssign`.
- Raw jQuery usage is rare in controllers (16 occurrences, mostly template `$.get` of HTML and `$.getJSON` of config JSONs: `vitalsConfiguration.json`, `mastersConfigration.json`, `charts/<file>.json`, `tiuxml_mapping.json`, `chemo_tiuxml_mapping.json`).
- Version check: `$.get('v_check?param='+$('html').attr('v'))` (common-module.js:12660).
- File downloads: `$.fileDownload` (jquery.fileDownload.js) + `api/rest/billReceipt/downloadFile` etc.; PDFs via `pdf.min.js`; images via `markerjs2`, `painterro` (annotation), `signature_pad`, `html5-qrcode`, `webcam.min.js`.

## 9. API MAP — top endpoints per module (full map in api-map-per-file.md)

Counts = unique normalized endpoints per backend controller (first path segment after `api/rest/`):

```
nursingList 293 | inventoryStockList 226 | master 195 | bloodtestmaster 115 | pathoOrder 114 |
pharmacy 98 | patient 83 | otscheduler 79 | masterConfigService 67 | addUserService 62 |
serviceOrder 55 | mdm 52 | walkinVisit 51 | equipment 50 | reports 47 | expiryitem 47 |
cssd 45 | abha 40 | RMRS 36 | otconfig 35 | bedmanagement 34 | transport 31 |
patientRegAppt 31 | appointment 30 | doctor 30 | patientTransferDischarge 29 | diet 29 |
packageservice 28 | medication 26 | billReceipt 26 | appointmentschedule 23 |
patientRegAddmission 22 | fileService 21 | queueManagment 21 | preventivemaintenance 20 |
itemmaster 20 | companyapprovedamount 20 | bill 18 | ipdadmission 17 | usermanagement 17 |
cwf 17 | ejs-details 16 | linenLaundry 14 | patientDetailReg 14 |
bioMedicalWasteManagement 13 | integration 12 | radiology 12 | complaintdetails 11 |
patientrequisition 11 | visit 10 | eAushadiIndent 10 | auth 10 | hpr 9 | itemsales 9 |
billSummary 8 | planmaster 8 | sendEmail 8 | servicemaster 8 | notedetailsdata 8 | rcm 7 | ...
```

### Most-referenced single endpoints (all js/):
```
api/rest/patient/getPatientGeneralDetailsListForSearch        15 refs (multiple modules)
api/rest/patientRegAppt/sendOTP                               12
api/rest/patient/getPatient                                    8
api/rest/walkinVisit/document                                  8
api/rest/master/getMastersForAppointment                       8
api/rest/serviceOrder/addServicechargesNew                     7
api/rest/patientRegAppt/validateOTP                            7
api/rest/billReceipt/downloadFile                              3
api/rest/patientRegAppt/pdfAppointmentFaceSheet(FromAppList|Html) 11 combined
```

### Hotspots by controller (function-level; see api-map-per-file.md for full lists):
- **billing-module.js** (14,307 lines, 366 functions): `api/rest/serviceOrder/*` (addServicechargesNew x9, addServiceOrder x3, getAllService x3, getOutsourceServiceStatus x3), `api/rest/patient/getPatient`, `api/rest/patientRegAppt/pdfAppointmentFaceSheet*`, `api/rest/walkinVisit/printBHTBarcode`, `printAttendantPass`, `api/rest/billReceipt/printCreditNoteForOP|IP`, `api/rest/addPayment`, `api/rest/cancelBill/refundReceiptByIds`, `api/rest/master/getDoctorFrequentServices`, `api/rest/bill/checkToPrintNewPass`, `submitPass`, `api/rest/ipdadmission/getSponsorByAdmissionID`, `api/rest/otscheduler/addPatientProcdure`.
- **inventory-module.js** (1.08 MB): `api/rest/inventoryStockList/*` (~226 endpoints), `api/rest/expiryitem/*`, `api/rest/eAushadiIndent/*`, `api/rest/companyapprovedamount/*`, `api/rest/planmaster/*`, `api/rest/masterConfigService/*`.
- **nursing-module.js / nursing-module-ext.js / nursing-charting-module.js / nursing-note-module.js**: `api/rest/nursingList/*` (~293 endpoints incl. getAllRoleNursePatientList, getPatientAllocationReport, getAllocatedNursesByPatientId, addAquityByPatientDetails, nurseDoctorDeAllocation, deleteNursePatientAllocationOnShiftHandOver, getPatientEDOD, doctorAvailableSlots, getLabAndRadOrderDetails, getPatient), `api/rest/clinical/getProviderList`, `api/rest/serviceOrder/allServiceFastByGet`, `api/rest/pharmacy/getMedications`.
- **pharmacy-module.js / pharmacy-module-new.js / pharmacy-module-op.js / opPharmacy-new.js**: `api/rest/pharmacy/*` (getMedications, printPatientDispenseDetails, getItemCurrentStock, printCompoundLabel, addItemPriceNew), `api/rest/medication/addItemPriceNew`, `api/rest/master/filterPharmDiagnosis`.
- **auth-init-module.js**: `api/rest/auth/*` (loginf, getuserrole, logout, changeUserRole), `api/rest/usermanagement/*` (loginsso, getMDMTokenWithSSOToken, getSupplierUserDetails), `api/rest/patientRegAppt/*`.
- **common-module.js** (15,542 lines): login flows, `api/rest/patient/*` search endpoints, `api/rest/master/getMastersForAppointment`, `api/rest/addServiceMaster/getServiceListByUnitID`, `v_check`.
- **patho-modules.js / patho-booking-module.js**: `api/rest/pathoOrder/*` (114 incl. downloadPathoResultApi), `api/rest/pathotestmaster`, `api/rest/pathoparameterunits`, `api/rest/abha/*`.
- **blood-bank-module.js**: `api/rest/bloodtestmaster/*` (115), `api/rest/bloodbag*, api/rest/requisition...`, `getBagListForIssue`.
- **bed-management-module.js**: `api/rest/bedmanagement/*` (34), `api/rest/patientTransferDischarge/*` (29), `api/rest/ipdadmission/*`.
- **operation-theatre.js**: `api/rest/otscheduler/*` (79), `api/rest/otconfig/*` (35), `api/rest/anesthesianotes`, `surgerynotes`.
- **cssd-module.js**: `api/rest/cssd/*` (45), `api/rest/linenLaundry/*`.
- **abdm-module.js**: `api/rest/abha/*` (40), `api/rest/hpr/*` (9), `api/rest/abdm-hfr/v1.5/facility/*`, `api/rest/patient/getPatient`.
- **rmrs-module.js**: `api/rest/RMRS/*` (36), `api/rest/companyapprovedamount/*`.
- **masters-modules.js / masters-ext.js**: `api/rest/masterConfigService/*` (67), `api/rest/addUserService/*` (62), `api/rest/doctormaster`, `admissiontype`, `anesthesiatypemaster`, `ejs-details/getEjsSubcenter`.
- **equipment-modules.js**: `api/rest/equipment/*` (50), `api/rest/mdm/*` (52), `api/rest/preventivemaintenance/*`, `api/rest/asset*`.
- **scwf-module.js / cwf-module.js**: `api/rest/master/filterHealthfactor|filterDiagnosis|filterComplaints`, `api/rest/cwf/consolidatedLookup`, `api/rest/nursingList/searchIcdCM|searchAdvice`.
- **chemotherapy-module.js**: `api/rest/chemotherapy...` (chemotherapy planner, regimen, notes), `chemo_tiuxml_mapping.json` chart fetch.
- **transport-module.js**: `api/rest/transport/*` (31).
- **queue-module** (queue/ sub-app has its own endpoints; `api/rest/queueManagment/*` (21) referenced from main app).

### Notable external/integration endpoints
- `aadhaarauthtest.rajasthan.gov.in/GenericFaceAuthApi/api` (face auth, see templates/userFaceauth.html)
- ABDM HFR: `api/rest/abdm-hfr/v1.5/facility/search|get-master-data`
- e-Raktkosh: `api/rest/eraktkosh*` (bloodBank:eraktkoshRequest route)
- FHIR bundles loaded client-side: `modules/commons/schemas/<docType>_FHIRBundle.json` (common-module.js:13836)

## 10. SCREEN INVENTORY (533 routes) — see route-inventory.md; module breakdown:

```
masters 63 | inventory 57 | commons 50 | reports 44 | billing 34 | op 33 | bloodBank 31 |
pharmacy 27 | ip 23 | nursing 22 | equipments 20 | cssd 14 | pathology 14 | allocation 11 |
rmrs 11 | queue 11 | ot 10 | transport 10 | enquiry 9 | worklist 6 | chemotherapy 4 |
radiology 4 | diet 4 | kiosk 3 | paymentTransfer 2 | charting 2 | grievance 2 | biReport 2 |
patientManagement 1 | tiuxmls 1 | rcm 1 | record 1 | pmrd 1 | shifthandover 1 | bcma 1 |
linenlaundry 1 | biomedicalwaste 1 | fileManagement 1
```

Named helpers also registered as keys: `load`, `crudop`, `varAssign`, `draw` (route sub-configs).

### 12-point deep-dive examples (representative screens)

**A. OP Walk-in Visit / Appointment (op:appointmentFlow → walkinVisit-schema.json)**
- Route: `#op:appointmentFlow` → `func: startAppointmentFlow` (appointment-module.js). Checkin route: `#op:appointmentNewForm`.
- Schema: `modules/op/schemas/walkinVisit-schema.json`; Template: property custom templates + `generic-radio-list.html`.
- Key fields: visitTypeId (radio, AJAX default event → getvisitTypes), switchh (Appointments/Emergency), janaadharIdNo/familyIdNo/civilId/abhaSearch/mRNO (search-as-you-type with crudop patientSearchby*), token, firstName, genderid (radio from gvar gender_master_full_new), age (Y/M/D composite), dateOfBirth, mobileNo (country-code pair), patientSponsor (plan dropdown), address block (country/state/district/city cascades via events), departmentId/buildingId/clinicId (cascade via appointmentschedule/getDepartmentsByDate), appointmentDate/from (slot picker DoctorAppointmentSlots viewer), MLC block (broughtBy*, caseTypeId, isLegal, mlcDetailsDto), vehicleDetails, referralDetails (NHNC), fpvDetailsDto/ancVisitDetails (FPV/PCTS), telemedicine fields (isTelemedicine).
- Validation: schema regex + `func:validatePatientDetails` (onValidateOp, validation.js:1), validators validateJanaadharId, validateBhamashahId, validateAdharNumber, validateHIDNO, validateAge, validateMobileNoByAPI (calls API), validateCountryCode.
- Callbacks: beforeOp `setWalkinObject`, afterOp `setViewOpdChargesRoute` (navigates to `#billing:viewOpdCharges`), failure `showErrorMsgForAppt`.
- CRUD: POST `api/rest/walkinVisit/addwalkinVisit?pat_token_no=<%token%>&app_type=<%app_type_w%>`; GET `api/rest/walkinVisit/searchToken`; POST `api/rest/patient/getPatientGeneralDetailsListForSearch` (ect/pcts search); events GET `api/rest/appointmentschedule/getDepartmentsByDate`, `getCabinWiseSlotForTelemedicine`, `getCabinWiseApointmentSlotCount`, `getDayWiseBuildingMasterByDepartmentId`, `getDeptsForTsu`, `getTelemedicineSpecialityUnit`, `api/rest/master/getPoliceStationDistrict|ByDistrictId`, `getDriverNumberById`.
- Age/DOB mutual calc: `.page` blur → `getDobFromAge`; `.pdob` blur → `getAgeFromDob` (common-module.js:1380/1653).

**B. OP Billing (billing:billingFlow)**
- Controller billing-module.js `startBillingFlow` (line 707); schema `modules/billing/schemas/addBillingSchema.json` (crud ops patientAdmSearch/patientAdmMrnSearch/patientAdmMobSearch → POST `api/rest/patientDetailReg/getPatientGeneralDetailsSearch`; serviceSearch → POST `api/rest/servicemaster/getServiceListForBill` with `dropdownmenuItem.click → addToServiceTable`).
- Table-driven: `#servicesTbl` rows rendered by template `modules/billing/templates/service-search-list.html` + `itemDetailRow.html`; pharmacy order section `#pharmacyOrderTbl`.
- Client math: `calculateTotalBillAmount` (billing-module.js:2050) — amount = rate×qty summed per row; tax amounts from `data-taxamt1..3`; concession `concessionAmt`; self/non-self balances; package-covered amounts; max concession via `getMaxConcessionAmount(New)`; corporate approval amount; VAT.
- Submit: POST `api/rest/serviceOrder/addServiceOrder` → `generateBillSucess` → routes to receipt. Pharmacy charges POST `api/rest/serviceOrder/addServicechargesNew`.
- Bills list: `billing:billingListFlow` → `startBillingListFlow`; GET `api/rest/bill/getAllBills...` via schema getAllBills-schema.json; receipt viewers: `opBillPaymentSchema`, `ipBillPaymentSchema`, `billSummarySchema`, `ipBillSummarySchema`, `advanceReceiptByReceiptNoSchema`.
- Payments: `api/rest/addPayment` POST; advance `api/rest/addAdvance`; refund `api/rest/cancelBill/refundReceiptByIds`; credit note `api/rest/billReceipt/printCreditNoteForOP|IP`; package billing: `packageBilling.js` → `api/rest/serviceOrder/allPackagesFastByGet`, `addPackageSubscriptionSchema`.

**C. Admission (ip:admissionOrder / ip:admissionList)**
- Controller admission-module.js (132 api refs); routes `ip:admissionOrder`, `ip:admissionOrderEmer`, `ip:admissionList`, `ip:admissionReqList`, `ip:transferReqList`, `ip:admissionConfirm`.
- Schema: `modules/ip/schemas/addpatientAdmissiontdto_schema.json` (+ _emergency variant, addBabyAdmissiondto_schema.json); templates `modules/ip/templates/admission-icons.html`, `emr-admission-icon.html`.
- Endpoints: `api/rest/patientRegAddmission/*` (22), `api/rest/patientDetailReg/*`, `api/rest/ipdadmission/*` (getSponsorByAdmissionID, getActiveAdmissionOfRegisteredPatient, addAdmMLCDetails), `api/rest/bedmanagement/*`, `api/rest/serviceOrder/allServiceFastByGet`, `api/rest/patient/getPatient`, `api/rest/patientTransferDischarge/*`.
- Bed management: `ip:bedManagement/:class_id/:ward_id/:bed_id/:building_id/:filterId` → bed-management-module.js (bed_management_schema.json, bed_screen_schema.json, templates bed-managment-building.html/ward.html) — building/ward/class/bed drill-down with occupancy color coding.

**D. Nursing coversheet (nursing:ipCoversheet/:patientId/:admId/:patientIen/:visitIen/:opdipd)**
- nursing-module.js; schema showPatientCoversheetSchema.json; coversheet composes tiles (`modules/nursing/templates/tilesHF.html`, `csNoteTiles.html`, `noteSummary.html`, `ipCoversheetNotes.html`); `api/rest/nursingList/*` patient/note endpoints; notes rendered from tiuxml schemas (`modules/tiuxmls/schemas/*.json`).
- TIU/XML note engine: `tiuxml.js`, `tiuxml.json` (219 KB mapping), `tiuxml_mapping.json`, per-module mappings (op_/nhnc_/chemo_tiuxml_mapping.json), `masters:formConfiguration/:fileType`, `tiuxmls:designer` (visual form designer).

## 11. PRINTING

- Document print: build content into `#popupSlotWrap`, hide chrome, `window.print()` on `#printable` (common-module.js:119-161 `printContent`, `printContentBarcode`, `printContent2`); templates like `modules/billing/templates/ipBillSummeryDetail.html`, `billSummeryDetail.html`, `modules/ip/templates/dischargeSummary/*_dis_form.html` + `*_dis_ticket.html`.
- Label printing: `js/qz-tray.js` (QZ Tray websocket; `qz.websocket.isActive()` guards) — ZPL templates `modules/commons/templates/zebraLabels/gatepass.zpl`, `bhtpass.zpl`, `bagIssuedLabel.zpl`; wristband barcode `wristNoBarcode.html`, token sheet `token_sheet.html`.
- BrowserPrint (Zebra) for lab reports: patho-modules.js:8587 `BrowserPrint.getDefaultDevice('printer', ...)`.
- Printer selection UI: `printerSetting-ext.js` + `modules/commons/templates/printerSettings.html`; localStorage keys `DefaultSystemPrinter`, `BarcodePrinter`, `NoOfCopies`, `isADotMatrixPrinter`; route `commons:showMDM` (MDM = device management).
- Server-side print/PDF: `api/rest/billReceipt/*`, `api/rest/walkinVisit/printBHTBarcode`, `printAttendantPass`, `api/rest/patientRegAppt/pdfAppointmentFaceSheet*` (returns PDF URL/blob; rendered with pdf.min.js), `api/rest/reports/*` print endpoints.
- Dot-matrix: `isADotMatrixPrinter` localStorage flag changes print pipeline.

## 12. BUSINESS LOGIC HOTSPOTS (JS, must replicate)

| Function | File:Line | Behavior |
|---|---|---|
| `calculateTotalBillAmount` | billing-module.js:2050 | bill totals: services + pharmacy + taxes + concession + package coverage |
| `getMaxConcessionAmount` / `New` | billing-module.js | concession cap rules (OP vs IP) |
| `generateBill` | billing-module.js:2879 | POST addServiceOrder, routes on success |
| `confirmServices` / `confirmServicesStep2/3` | billing-module.js | service order workflow steps (OPD/IPD/package) |
| `getDobFromAge` / `getAgeFromDob` | common-module.js:1380/1653 | age↔DOB conversion; age limits (≤115), NHNC baby variants (`getDobFromAgeBabyOf` 7736) |
| `calculateAgeFromDob` | common-module.js:1818 | age string "Xy Ym Zd" generation |
| `setWalkinObject` | appointment-module.js:3119 | builds walkin DTO before POST |
| `validatePatientDetails` | validation.js:1 | pre-submit validation (multi-field) |
| `validateMLCDetails` | validation.js | MLC block validation |
| `switchAppointmentsTabRisl` | appointment-module.js | Appointments↔Emergency flow switch |
| `fetchLatestToken` / `fetchAbsentToken` | appointment-module.js | queue token handling |
| `routeToAppointmentList` | appointment-list-module.js:2768 | post-checkin navigation |
| `showPoliciesFunction` / RGHS verification | appointment-list-module.js | eligibility check (`getPolicies` button) |
| `onRouteChange` | common-module.js:163 | global route-change handler: resets gvars, menu visibility by role, calendar/trip dates |
| `startAppointmentFlow` etc. | per module | screen entry points |

Status transitions happen mostly server-side; client initiates via `serviceOrder/addServicechargesNew` (billing), `walkinVisit/addwalkinVisit` (registration), `nursingList/*DeAllocation`, `bedmanagement/*` etc. Client "steps" (e.g., billing 3-step, admission flow) are JS wizard state machines.

## 13. ROLE-BASED UI

- gvars `login_user_role`, `login_user_role_l` (list); route guard in common-module.js:190-198 (Nurse/Nurse Assistant/Cashier/kiosk/Customer Service/Pharmacist/Queue roles get logout button etc.).
- Menu: role-specific nav templates `roleBasedNav.html` (commons + queue sub-app), `selectDefaultRole.html`, `unit-role-change.html`; `onRouteChange` shows/hides `#logoutLink`, `.menuRolesCls`.
- `changeUserRole` API switches role/unit/store; `crr_Default_Store_id`, `userstoreIds` drive store-scoped screens (inventory/pharmacy).

## 14. UNKNOWNS / FLAGS

- `ext.js` is referenced by index.html but absent from `public/` (backend-injected?); `resources/ext.css` also missing. Possibly generated by the WAR build — **UNKNOWN**.
- `apps/` and `ohumlite/` contents not fully inventoried (ohumlite = lighter HMIS variant sharing modules; queue/ = standalone queue app).
- Exact evaluation order of `dynamicAspects` vs propertyOrder collisions not verified line-by-line in minified code.
- The `templates/` and `testdata/` top-level dirs not reviewed.
- `mastersConfigration.json` (57 KB) drives masters UI (masters-ext.js loads it).
- Backend auth contract (`api/rest/auth/*`) — confirm with backend agent (note hardcoded header token `ZDgxOTM2ZTQt...`).
- `v_check` endpoint contract unknown.
- i18n: `faug-ext-globalize.js` (moment locale + i18next); language switch writes `ls_locale_language`; UI text uses `data-translate` attrs + `<%Fg.t('KEY')%>`; labels via `%%Fg.g(lbl)%%`.

## 15. KEY FILES INDEX (for React team)

| File | Role |
|---|---|
| `fjs-config.json` | routes, initial schemas/templates, settings |
| `fjs-config_*.json` (40) | per-module schema/template registries |
| `layer/default.json` | default rendering chrome (forms, tables, modals) |
| `faug-min.js` | engine: routing, schema interpreter, dynamicAspects, gvars, ajax, table widget |
| `faug-ext.js` | template engine + date/dialog lib adapters |
| `faug-ext-globalize.js` | i18n/date/number adapters |
| `w-api-call.js` | Web Worker XHR bridge with cache |
| `w-fetch-file.js` | file fetching worker |
| `js/*.js` (71) | per-module controllers (business logic + direct API calls) |
| `modules/*/schemas/*.json` (1,308) | declarative forms/lists + embedded API URLs + events |
| `modules/*/templates/*.html` (2,942) | row/item/list/page templates (compiled at runtime) |
| `tiuxml.js` + `tiuxml*.json` | TIU note-form rendering engine + mappings |
| `charts/ODCWF_*.json` (29) | nursing charting formats |
| `data/*.csv|xls` (60+) | master data seed/import files |
| `jsonFile/moduleRoute.json` | legacy module→route map (superseded by fjs-config router) |
