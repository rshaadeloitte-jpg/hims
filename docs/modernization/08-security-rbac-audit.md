# iHMS / OHUM PAS NG — Security, RBAC & Audit (AS-IS)

> Produced by Agent 08 (Security / Roles / Audit Analyst). Evidence dump: `_discovery/agent-08/findings.md`.
> Repo: `D:\ihms`. Research-only; no source modified. All `file:line` references are evidence.

---

## 1. Executive Summary

iHMS authenticates users with **opaque bearer tokens** (Base64 of a UUID) stored in the in-process/Redis cache, issued by a custom CXF filter/interceptor stack (`IntegrationOperationsFilter` + `ServiceLayerAspect`) — **not** by Spring Security (which is configured but inert: CSRF disabled, `permitAll`, `SessionCreationPolicy.NEVER`) and **not** by JWT (nimbus-jose-jwt is a transitive dependency; zero usage in `com.ohum`).

**Critical finding: RBAC is enforced in the FRONTEND, not the backend.** The backend only verifies that a token is valid and unexpired. The only server-side per-API permission check was written and then **commented out** (`ServiceLayerAspect.java:421-426`). Business APIs can be invoked directly by any authenticated user regardless of role. UI visibility is gated by a permission-path list (`data-roleM` attributes, 2,078 unique keys) shipped to the browser at login.

A second critical finding: with the checked-in configuration, **the password comparison is disabled** — the default `PasswordValidator.attempt()` returns `true` unconditionally (`PasswordValidator.java:24`, comparison commented out), and `auth.pwd.manager=password` selects that no-op validator. Only user-exists + not-locked + role/unit-mapping checks run.

Role inventory is **data-driven** (rows in `T_USERROLEMASTER`); the code references ~30 role names. Audit is a **manual, service-authored** event queue (59 service files, 568 call sites) flushed to `T_AUDIT` per request, with rich downstream tables (`T_TRX_HISTORY`, `T_LOGIN_AUDIT`, `T_ABDM_AUDIT`).

---

## 2. Authentication

### 2.1 Login flow (form login — primary path)

```
Browser (common-module.js:979-1030)
  doLogin(): param1=btoa(username), param2=sjcl.sha256(password)   (client-side hash)
  POST /api/rest/auth/loginf   (form-encoded)
        │
        ▼
AuthenticationServiceImpl.loginFormParams (@Path("/auth") @NotSecure)  :70-88
  validates unitId>0, roleId>0, username/password non-blank
        │
        ▼
DefaultAuthenticationImpl.attempAuthentication  (wired via services.xml:27-30)  :193-319
  SQL: t_userdetails ⋈ t_userunitdetails ⋈ t_userroles ⋈ t_userrolemaster
       WHERE lower(loginname)=? AND status='1' AND roleid=? AND unitid=?   :225-230
  locked? → BEL002                                                          :240-241
  passwordManager.attempt(...)   → ALWAYS TRUE (see §7)                     :257, PasswordValidator.java:24
  failed attempts → updateUserLockedCount (ACCOUNTLOCKTHRESHOLD lockout)    :259-263
  audit LOGIN / LOGIN.FAIL                                                  :284-315
        │
        ▼
loginViewUtilFunction  :436-892
  role  = T_USERROLEMASTER.DESCRIPTION                                     :446-447
  perms = t_role_enabledpermission → M_ModulePermissionMaster paths
          ("Billing.OPIPBilling", parents auto-derived)                    :739-788
  token = Base64(UUID) ; cache.add(token, view, sessionTimeout*60)         :881
  single-session enforcement (UNQ_SESS_USER_<id>)                          :843-855
        │
        ▼
Client receives UserView { token, user{role, roleId, ssoName, doctorId, wards, storeIds, ...},
                           permissions[], ... }   →  Fg.g("permissions"), login_user_role globals
```

### 2.2 Authentication variants

| Variant | Endpoint | Mechanism | Password check? |
|---|---|---|---|
| Form login | POST `/auth/loginf` | DB users, client-sha256 pwd | No (validator no-op) |
| OTP login | POST `/auth/loginotp` | SMS OTP (patientRegAppt.validateOTPForLoginWithoutSSO) then `attempAuthenticationSSO` | N/A |
| Rajasthan SSO | POST `{contextPath}/` + `userdetails` | `SSOAuthFilter` → `risl.sso.gettoken.url` SOAP token lookup → `attempAuthenticationSSO` (by sAMAccountName) | **No password at all** (SSOAuthFilter.java:159) |
| Mobile SSO | POST `/usermanagement/loginsso` | AES-encrypted creds → `risl.sso.mobile.url` (REST JSON) | External SSO |
| LDAP/AD/Samba | (unwired bean `customLDAPAuthImpl`) | `CustomLDAPAuthenticationImpl` → unboundid LDAP bind + objectSid vs stored `SID` | Yes (LDAP), but **not active**: `ldap.auth.enabled=false`, `auth.method=` empty |
| Patient portal | POST `/auth/loginPatient` | stored proc `patientLoginNameExists` | DB-side (stored proc) — role hardcoded "Administrator" |
| External API | POST `/auth/loginJ` | Unit `apiToken`/`apiKey` match → first `%HOSPITAL ADMIN%` user | N/A (key-based) |
| API integration | `/int/*` | token + `ROLE_INTEGRATION` role check (only real backend role check) | N/A |

### 2.3 Request authentication (how a token gates an API)

1. `IntegrationOperationsFilter` (highest-precedence servlet filter) — for `/api/rest/*`:
   - Referer must be in `allowed.referer.urls` (else 401) — `:417-420`.
   - `token` header (or `?token=` param) → `cacheImpl.get(token)` → `UserTrxContextHolder.setSecurityEntity(view)` — `:656-666`, `:867-878`.
   - **Bypass**: `token == secretTokenForOnlineAppointment` → runs as global user id=1 — `:659-660`, `:871-872`.
2. `ServiceLayerAspect.handleWorkflow` (AOP around every `*ServiceImpl` `@Path` method):
   - `view == null` → `DummyHTTPStatusException(401)` — `:442`.
   - `@NotSecure` / `@Login` / `@IgnoreApiForAuth` methods run as **fake user id=1** when no token — `:364-367`.
   - Token expiry = `generatedTime + session.timeout` (sliding, renewed per request) — `:391-416`.
   - **Per-path permission check: commented out** — `:418-426`.

### 2.4 Session vs token

- **Token-based, cache-backed sessions. No `HttpSession` usage** (only one unused import, `PathoOrderServiceImpl.java:43`). `SessionCreationPolicy.NEVER`, URL rewriting disabled (SpringSecurityConfig:112-114).
- TTL: `session.timeout = 30` min (application.properties:642), applied as `cache.add(token, view, 30*60)`.
- `allowSingleSessionPerUser = true` (:1477) — new login evicts the previous token via `UNQ_SESS_USER_<userId>` (exceptions: `ALLOWED_MULTI_LOGIN_USERNAMES` app_config).
- Logout (`PUT /auth/logout`, `@Secure`) removes token + `_BI` cache entries, audits LOGOUT.
- JWT: **not used**. `nimbus-jose-jwt` in pom.xml:994-995 is for the ABDM client only.

---

## 3. Authorization model — where is it enforced? (CRITICAL)

### 3.1 Backend: AUTHENTICATION only, not AUTHORIZATION

| Check | Where | Status |
|---|---|---|
| Valid non-expired token | ServiceLayerAspect :362-442 | ✅ Active |
| Referer allowlist | IntegrationOperationsFilter :417-420 | ✅ Active |
| API-path permission lookup | ServiceLayerAspect :421-426 (`view.getPermissionsMap().containsKey(fpath)`) | ❌ **Commented out** (dead) |
| `@Secure(roles)` method-level roles | annotations/Secure.java; aspect :316-322, :374-379 | ❌ Never implemented |
| `checkPermission("...")` helper | MasterCachedServiceImpl :10356-10362 | ⚠️ 1 call site only: `InventoryServiceImpl:11588` (`Inventory.AllApproval`) |
| `ROLE_INTEGRATION` gating | IntegrationOperationsFilter :442-447 (`/int/*` mapping allowedUsers) | ✅ Active (integration APIs only) |

**Consequence:** any user with a valid token can call any business endpoint (patient registration, billing, discharge, master changes) by crafting the HTTP call directly — the UI simply hides buttons.

### 3.2 Frontend: the real RBAC

- At login the server ships `permissions[]` (dotted paths, e.g. `Billing.OPIPBilling`, `Inventory.Store.Add`).
- `checkPermissionForSplash()` (common-module.js:3389-3400) hides any element with a `data-roleM` key not in the list — applied on ~50 module screens.
- Sidebar menus (`masterNav.html`, `roleBasedNav.html`) are permission-gated links.
- **2,078 unique `data-roleM` permission keys** across templates (counted): Configuration 1,424 · BIReport 263 · Inventory 70 · Admission 45 · IHMSDashboard 41 · Billing 34 · Appointment 27 · Nursing 26 · Equipment 20 · Pharmacy 18 · Investigation 16 · QueueManagement 16 · Login 9 · Report 7 · others 1-4.
- Role-NAME string checks also exist in JS (not permission-based): `"Administrator"`, `"Doctor"`, `"Cashier"`, `"Nurse"`, `"Ward-Incharge"`, nursing-supervisor variants, `"doctorscan"`, `"nhncFrontDesk"` (auth-init-module.js:87-92,225-231,274; common-module.js:6010-6026; item-category-module.js:578; patho-modules.js:3-10; nusing-module.js:11070-11072; masters-ext.js:1657).

### 3.3 Permission data model (as designed)

```
T_USERROLEMASTER (role: code, description, isNurseRole, isClinicalRole, issupplier, levelId, showmasternav)
      │ 1─n  t_userroles (user×role×unit, raw-SQL only)
      ▼
t_role_enabledpermission (roleId, userId?, enabledModulePermissionIdsJson)
      ▼
M_ModulePermissionMaster (path "Module.Screen.Action", apiMethod, apiPath, moduleName, action, parentId)
      ▼ (built at login, DefaultAuthenticationImpl:739-788)
view.permissions[]  →  frontend data-roleM gating
```

The permission **tree editor** exists (`AddUserServiceImpl.getPermissions/updatePermissions`, UI `masters-ext.js:112-162` — jstree) and writes `ADD.MODULEPERMISSION`/`REMOVE.MODULEPERMISSION` audit events — the plumbing is complete; only the backend *enforcement* was turned off.

---

## 4. Role / permission inventory

### 4.1 Roles

Roles are **DB rows** (T_USERROLEMASTER.DESCRIPTION) — no fixed list in code; count unknown statically. Role names evidenced in code:

| Role name | Evidence |
|---|---|
| Administrator / Admin | DefaultAuthenticationImpl:358 (patient login!); auth-init-module.js:87,274 |
| FrontDesk (→ `nhncFrontDesk` alias) | auth-init-module.js:87,137-140; common-module.js:6010-6018 |
| Doctor / DOCTOR / doctorscan | auth-init-module.js:90,168; masters-ext.js:1657 |
| Cashier | item-category-module.js:578 |
| Nurse, staff nurse, nurse-staff, student nurse, Ward-Incharge, nusing-incharge, nursing supervisor, nursing superintendent (and misspelled "Nurseing Superwiser/Superintendent") | patho-modules.js:3-10; nusing-module.js:11070-11072; auth-init-module.js:225-229 |
| Senior allied health therapist, allied therapist, community health worker, ot technician | auth-init-module.js:228-229 |
| sync (machine/sync user — sha256 pwd path) | DefaultAuthenticationImpl:253,543 |
| ROLE_INTEGRATION | IntegrationOperationsFilter:444 |
| HOSPITAL ADMIN (external login) | DefaultAuthenticationImpl:948 |
| Physician (LDAP default role) | LDAPAuthenticationUtil:67, application.properties:418 |

Role flags that alter behavior: `isClinicalRole()` (JS), `isNurseRole`, `levelId`, `showmasternav` (admin gets full `masterNav`).

### 4.2 Permission keys (actions) — 2,078 unique `data-roleM` values

Convention: `Module.Screen.Action` where Action ∈ {Add, Edit, Delete, Upload, Download, View, Approve, Freeze, Reorder, RaiseNewIndent, …}. Sample top-level modules: Configuration (1,424 — master screens incl. `Configuration.Master.Unit.TUserrole.*`, `Configuration.Master.Inventory.Store.Add`, `Configuration.Master.Pathology.TestMaster.*`), BIReport, Inventory (70 — `Inventory.Indent.StoreIndent.RaiseNewIndent`, `Inventory.Procure.POLIST.ApprovePO`, `Inventory.AllApproval`), Admission, IHMSDashboard (incl. ME/MH district/state R1-R4), Billing (`Billing.OPIPBilling`, `Billing.DeathPatientBilling`), Appointment, Nursing, Pharmacy, Investigation (`Investigation.ResultFinalize`, `Investigation.ResultReviewConfirm`, `Investigation.SampleCollection.Collect`), QueueManagement, Login (`Login.UnitRole`, `Login.Clinician`…).

### 4.3 Module → screen → API mapping

| Layer | Artefact | Location |
|---|---|---|
| Modules | 48 `fjs-config_<module>.json` (billing, pharmacy, pathology, nursing, inventory, ot, op, ip, cssd, bloodBank, diet, linenlaundry, rcm, rmrs, transport, pmrd, …) | `public/fjs-config_*.json` |
| Screens | `schemas` within fjs-configs; HTML templates `modules/<module>/templates/*.html` | `public/modules/**/templates/` |
| Actions | `data-roleM` keys (above) | templates |
| APIs | service `@Path` methods (e.g. `AuthenticationServiceImpl` `/auth/*`); `ModulePermissionMaster.apiMethod/apiPath` (DB data, not in repo) | `src/main/java/com/ohum/services/**` |

The M_ModulePermissionMaster rows that would complete Role→API mapping are **database data** — not in the repository; the schema is `M_ModulePermissionMaster (path, apiMethod, apiPath, moduleName, action, parentId)`.

---

## 5. Audit architecture

### 5.1 Pipeline

```
Service method (any *ServiceImpl)
   │  manual: audit.getAudits().add(...)      ← 59 files / 568 call sites
   ▼
UserTrxContextHolder.getInternalAudit()  (ThreadLocal, per request; UserTrxContextHolder.java:101-105)
   ▼
ServiceLayerAspect finally block (ServiceLayerAspect.java:542-544) — once per request:
   auditHelper.saveAuditEvent(internalAudit, remoteAddr)  (AuditHelper.java:67-147)
   │
   ├─ T_AUDIT            (Audit model)  eventId/eventDescription/patientId/…/remoteAddress/token/addedBy/unitId
   ├─ T_TRX_HISTORY      (TrxHistory)   ~60 eventIds: CREATE.VISIT, TRANSFER.BED, ADD.DISCHARGE, MLC*,
   │                                    MORTUARY*, SAMPLE*.CONFIRM/RELEASE, BLOODBAG.*, ERAKTKOSH*, grievance → T_GRIEVANCE_TICKET_HISTORY
   ├─ T_LOGIN_AUDIT      (on LOGIN)     user, unit, remote address, isOtp          (AuditHelper:913-932)
   ├─ T_ABDM_AUDIT       (ABHA creation)                                          (:934-955)
   └─ T_APPOINTMENT_CHANNEL                                                       (:957-973)
JobAuditEvent → saveJobEvent (@JobEvent jobs, start/end/status/failureDetails)    (AuditHelper:906-911; ServiceLayerAspect:816-851)
```

- **Event description templating**: `AuditEventMaster.templateString` per eventId (cached `auditEventMasterMap`); Velocity rendering; `AuditHandler.AuditTemplateDefinition` only defines LOGIN/LOGOUT (annotation-driven auditing is vestigial — 10 `@AuditEvent` annotations total, handler never invoked from the aspect).
- **Identity stamping**: `MBase.beforeCreateUpdateCallback` sets `ADDEDBY/ADDEDDATETIME/UPDATEDBY/UPDATEDDATETIME/CREATEDUNITID/UPDATEDUNITID` from the session user on every entity save/update (MBase.java:72-90; applied via ServiceLayerAspect:697-775).
- **Coverage**: good for clinical/financial transactions (the T_TRX_HISTORY switch), but inconsistent — audit is authored per-service; no automatic request logging of all APIs (only M_B_API/M_A_API memory metrics per REST call, IntegrationOperationsFilter:673).

### 5.2 Audit entities

`Audit` (T_AUDIT), `AuditEventMaster` (M_AUDITEVENTMASTER, cached), `JobAuditEvent`, `TrxHistory` (T_TRX_HISTORY). `Audit.auditObjects/auditPropMap` are transient carriers flattened into the row.

---

## 6. Session management summary

| Aspect | Value | Evidence |
|---|---|---|
| Session mechanism | Opaque token → cache entry (UserView); sliding expiry | DefaultAuthenticationImpl:881; ServiceLayerAspect:391-416 |
| Timeout | 30 min | application.properties:642 |
| Single session | `allowSingleSessionPerUser=true`, `UNQ_SESS_USER_<id>` eviction | application.properties:1477; DefaultAuthenticationImpl:843-855 |
| SSO cookie | `__token` (non-HttpOnly/secure flags not set) | SSOAuthFilter:179 |
| HttpSession | Unused | grep (1 unused import) |
| Frontend expiry | `session_expire.html`, 401 → reload/logout | auth-init-module.js:198-206 |

---

## 7. Password policy & credential handling

- **Verification disabled**: `PasswordValidator.attempt` returns `true` unconditionally (`PasswordValidator.java:24` — `//suppliedPassword.equals(actualPassword);`). Active bean = plain validator because `auth.pwd.manager=password` (application.properties:1028) doesn't contain "restrictive" (App.java:141-145).
- Restrictive mode (optional): delegates to external password manager `ldap.restrictive.handler.url` (`LDAPBasedRestrictivePasswordValidator.java:40-59`), which verifies `linkedToken + token(password)`; client IP hardcoded `0.0.0.0`.
- Storage: `T_USERDETAILS.PASSWORD` = `encryptDecryptUserKey(pwd)` — reversible char-transform `c*2-5` (obfuscation, not a hash); `PASSWORDVISTA` base64. "sync" role path sha256-wraps it.
- Policy columns exist on T_USERDETAILS (min/max length, digit/lower/upper/special, history count, age min/max, lock threshold/duration) — **no enforcement found in the auth path** (unknown if enforced in user-creation UI; `AddUserServiceImpl.saveUser` validation not evidenced).
- Client: password never sent in clear (sha256 in browser). This makes the missing server-side compare the single biggest authentication weakness — combined with `test.user.internal.001` daily-password backdoor (DefaultAuthenticationImpl:199-223).

---

## 8. CSRF / CORS / transport hardening

- **CSRF disabled** (SpringSecurityConfig:110-111); token also accepted as query parameter → token leakage via logs/referrers (ServiceLayerAspect:872).
- CORS: custom bean only for `/rest/**` (prefix mismatch with real `/api/rest`), credentials not allowed, origins = `cors.allowed.endpoints` (App.java:213-227).
- Referer allowlist enforced (IntegrationOperationsFilter:417-420) with `allowed.referer.urls`.
- CSP present but weak: `script-src 'unsafe-inline' 'unsafe-eval'` (SpringSecurityConfig:72).
- HSTS/X-XSS-Protection/X-Content-Type-Options set on HTML/config responses.
- GzipFilter = compression only. IdEncryptionUtil = optional PHI field encryption (disabled by default; `phi.encryption.enabled`).

---

## 9. Security gaps & risks (ranked)

| # | Risk | Evidence | Severity |
|---|---|---|---|
| 1 | **No backend authorization** — permission check commented out; any token holder can call any API | ServiceLayerAspect:421-426 | Critical |
| 2 | **Password verification disabled** by default config | PasswordValidator.java:24; application.properties:1028 | Critical |
| 3 | **Static shared token bypass** (`secretTokenForOnlineAppointment`) authenticates as user id=1 and is embedded in browser-delivered `fjs-config.json` | IntegrationOperationsFilter:659,871; fjs-config.json:42; application.properties:598 | Critical |
| 4 | Hardcoded **test-user backdoor** with date-derived password | DefaultAuthenticationImpl:199-223 | High |
| 5 | `@NotSecure`/`@Login`/`@IgnoreApiForAuth` run as **fake user id=1** (audit/ownership contamination) | ServiceLayerAspect:364-367 | High |
| 6 | Secrets in repo (DB, LDAP, SSO, MinIO, API keys) + clear-text password in application.properties | application.properties:97,227,448,756,996-998 | High |
| 7 | Reversible password obfuscation instead of hashing | DefaultAuthenticationImpl:410-416 | High |
| 8 | CSRF disabled; token in query string | SpringSecurityConfig:110-111 | High |
| 9 | Weak CSP (`unsafe-inline`, `unsafe-eval`) | SpringSecurityConfig:72 | Medium |
| 10 | LDAP util trusts all certs; inactive but shipped | LDAPAuthenticationUtil.java:117 | Medium |
| 11 | SSO login performs **no server-side password/OTP check** (trusts external token endpoint URL param) | SSOAuthFilter:110-159 | Medium |
| 12 | Patient portal login assigns role **"Administrator"** | DefaultAuthenticationImpl:358 | Medium |
| 13 | External `/loginJ` grants first HOSPITAL ADMIN user's identity via unit API key | DefaultAuthenticationImpl:948-950 | Medium |
| 14 | Audit is service-authored/manual — coverage uneven; annotation-driven audit unused | 59 files / 10 annotations | Medium |
| 15 | Frontend-only role checks mean JS devtools can reveal hidden features; role-name string comparisons are brittle | common-module.js:3389-3400 | Info/Design |

---

## 10. Modernization notes

1. **Server-side authorization first**: re-enable the path-permission check (module permission master has `apiMethod`+`apiPath` designed for it) or introduce Spring Security method security / a gateway policy layer keyed on `Role→ModulePermissionMaster`; never trust the browser `permissions[]` list.
2. **Authentication**: replace the shared-secret bypass with per-integration credentials; remove the hardcoded test-user path; make `PasswordValidator` a real comparator; adopt a standard token (JWT with signature+expiry or server-side sessions) with HttpOnly+Secure cookies instead of header/query tokens.
3. **Credential hygiene**: hash passwords (bcrypt/PBKDF2), purge secrets from `application.properties`/fjs-config into a vault, and enforce the existing T_USERDETAILS password-policy columns.
4. **Audit**: standardize on annotation-driven auditing (extend `AuditTemplateDefinition` beyond LOGIN/LOGOUT) or a filter-level request logger with role, IP, token, and outcome; keep T_TRX_HISTORY semantics for clinical trail.
5. **Session**: honor `SessionCreationPolicy` properly; add explicit revocation API; mark the SSO `__token` cookie HttpOnly/Secure/SameSite.
6. **Web hardening**: fix CORS registration prefix (`/api/rest`), tighten CSP, re-enable CSRF protection or use the anti-CSRF pattern (token in body header only), add rate limiting on `/auth/*`.
7. **Data model**: promote `t_userroles` and `T_USER_HOSPITAL_AUTHORITY` to first-class entities; role/permission rows should become versioned reference data in the migration to a new IAM.
