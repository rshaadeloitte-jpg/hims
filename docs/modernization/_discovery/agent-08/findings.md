# AGENT-08 Findings — Security / RBAC / Audit (evidence dump)

Source: `D:\ihms` (iHMS/OHUM PAS NG, Java + CXF REST + custom JS frontend). Research only; no code modified.
All paths relative to `D:\ihms\` unless absolute.

---

## 1. Authentication flow

### 1.1 Entry points (JAX-RS endpoints)

| Endpoint | Class:line | Method | Notes |
|---|---|---|---|
| POST `/auth/loginf` | `src/main/java/com/ohum/services/AuthenticationServiceImpl.java:70-88` | `loginFormParams` | Main form login; requires unitId>0, roleId>0, non-blank username/password |
| POST `/auth/loginh` | `AuthenticationServiceImpl.java:62-68` | `loginHeaderParams` | **Always throws 401** (stub) |
| POST `/auth/loginb` | `AuthenticationServiceImpl.java:192-199` | `loginBasicAuth` | **Always throws 401** (stub; Basic auth parser exists in `AuthenticationInterface.java:23-44` but unused) |
| POST `/auth/loginotp` | `AuthenticationServiceImpl.java:90-120` | `loginotp` | OTP login; validates via `validateOTPForLoginWithoutSSO("isSSOLoginOTP",...)` then `attempAuthenticationSSO` |
| POST `/auth/loginextf` | `AuthenticationServiceImpl.java:122-171` | `loginFormParamsExt` | External login; strips hospital/user PII from response |
| POST `/auth/loginPatient` | `AuthenticationServiceImpl.java:173-190` | `loginPatientFormParams` | Patient portal login → `attempPatientAuthentication` |
| POST `/auth/loginJ` | `AuthenticationServiceImpl.java:282-287` | `externalLogin` | API-key login (`apiToken`/`apiKey` headers) → `authenticateExternalLogin` |
| POST `/auth/loginExt` | `AuthenticationServiceImpl.java:289-312` | `loginFormForKDL` | Returns only token (LoginResponseView) |
| PUT `/auth/logout` | `AuthenticationServiceImpl.java:201-208` | `logout` | `@Secure`; calls `attemptUnAuthentication` |
| POST `/auth/getuserrole`, `/getuserroleOtp`, `/changeUserRole` | `AuthenticationServiceImpl.java:215-280` | — | Role enumeration for unit; `changeUserRole` switches role w/o re-auth |
| POST `/auth/changeUserRole` | `AuthenticationServiceImpl.java:273-280` | — | `@Secure` role/unit switch (new token issued by `loginViewUtilFunction`) |
| POST `/usermanagement/loginsso` | `services/pas/UserManagementServiceImpl.java:345-410` | `loginsso` | Mobile SSO against `risl.sso.mobile.url` (`risl.sso.mobile.encryptkey=R@j$S0@02{09}19#`, application.properties:1270-1272); AES/CBC/PKCS5 encrypt (`doSSOEncrypt` :327-343) |
| GET `/usermanagement/getSessionDetails` | `UserManagementServiceImpl.java:302-319` | — | SSO unit/role switch on existing token; no re-auth |

Class-level `@NotSecure` on `AuthenticationServiceImpl` (`:43`). Login endpoints also carry `@Login` + `@AuditEvent({LOGIN})`.

### 1.2 Frontend login

- `public/js/common-module.js:979-1030` `doLogin()`: `param1 = btoa(username)`, `param2 = sjcl sha256(password)` (:1002-1006); POST `api/rest/auth/loginf` (:1007).
- OTP variant: `api/rest/auth/loginotp` (:1008-1010).
- SSO success path: `__token` cookie (set by server) → `api/rest/usermanagement/getSessionDetails?unitId&roleId` (:982-987).
- Biometric (cordova): stored btoa'd creds replayed to `/loginf` (:853-857).
- `auth-init-module.js:126-127` stores `login_user_role` / `login_user_role_l` globals; `:137-140` maps "FrontDesk" → "nhncFrontDesk"; `:184-188` derives Billing.OPBilling/IPBilling from Billing.OPIPBilling permission; `:274` admin gets `masterNav.html`, others `roleBasedNav.html` (:301-315).

### 1.3 Server-side authentication (`DefaultAuthenticationImpl`, ACTIVE)

- Bean wiring: `src/main/resources/services.xml:27-30` — `defAuthImpl` = `DefaultAuthenticationImpl` wired into `AuthenticationServiceImpl`; `customLDAPAuthImpl` = `CustomLDAPAuthenticationImpl` **exists but is NOT wired**. `application.properties:1204` `auth.method=` (empty); `:428` `ldap.auth.enabled=false`.
- `DefaultAuthenticationImpl.attempAuthentication` (`util/auth/DefaultAuthenticationImpl.java:193-319`):
  - Query: `t_userdetails` JOIN `t_userunitdetails` JOIN `t_userroles` JOIN `t_userrolemaster` WHERE `lower(loginname)=?` AND `status='1'` AND `roleid=?` AND unitids (**:225-230**).
  - Locked flag `details[1]="1"` → BEL002 (:240-241).
  - **Hardcoded test backdoor**: `username.equals("dGVzdC51c2VyLmludGVybmFsLjAwMQ==")` (= base64 of `test.user.internal.001`) with daily-rotating password = sha256 concatenation of 3 parts incl. `DigestUtils.sha256Hex(new DateTime().toString("ddMMyyyy"))` (:199-223).
  - Password check: `passwordManager.attempt(...)` (:257) → see §7 (effectively no-op).
  - Locked-count increment on failure: `userManagementServiceImpl.updateUserLockedCount(details)` (:259-263) — sets lockout after threshold (T_USERDETAILS.ACCOUNTLOCKTHRESHOLD).
  - Login audit: `LOGIN.FAIL` (:284-293) / `LOGIN` (:307-315).
- Token: `getNewEncodedToken()` = `Base64(UUID.randomUUID())` (:404-408) — **opaque random token, NOT JWT**. Stored in cache: `cacheImpl.add(view.getToken(), view, sessionTimeout*60)` (:881) + `_BI` raw variant (:882-883).
- `loginViewUtilFunction` (:436-892) builds the session object:
  - Role: `masterCachedServiceImpl.findRoleById(...)` → `user.setRole(roleById.getDescription())` (:446-447), `roleLevelId` (:448).
  - **Permissions**: reads ALL `ModulePermissionMaster` rows (:740), then `t_role_enabledpermission where roleId=?` (:743-744) → builds `RoleBasedPermissionDTO` list with `path`, `apiMethod`, `apiPath`, `moduleName`, `parentId` (:749-767); derives parent paths by splitting on "." (:769-787); `view.setRolePermissions(new ArrayList<String>(setOfPath))` (:788).
  - Single-session: `allowSingleSessionPerUser` — removes previous token via `UNQ_SESS_USER_<userId>` key, with `ALLOWED_MULTI_LOGIN_USERNAMES` app_config exception (:843-855).
  - District/block authority: `SELECT DISTRICTID, BLOCKID FROM T_USER_HOSPITAL_AUTHORITY WHERE USERID=?` (:857-876).
- `attempAuthenticationSSO` (:321-342): **no password check** — user identified by `sAMAccountName` only.
- `attempPatientAuthentication` (:345-402): patient login via stored proc; **hardcodes `patuser.setRole("Administrator")`** (:358).
- `authenticateExternalLogin` (:933-965): unit `apiToken`/`apiKey` match → picks first user with role `%HOSPITAL ADMIN%` (:948-950) → token.

### 1.4 LDAP variants (present but inactive)

- `util/auth/CustomLDAPAuthenticationImpl.java:44-73`: LDAP bind via `LDAPAuthenticationUtil.authenticateToLDAPServer` (:48); SID cross-check `checkSidNumber` compares stored `SID` with AD `objectSid` (:50-61). Then `attempAuthenticationByUserName` (:75-87) → same `loginViewUtilFunction`.
- `util/LDAPAuthenticationUtil.java` (unboundid SDK): bind patterns, `TrustAllTrustManager` (:117), plaintext password comparison branch `ldapPassword.equals(password)` (:144-149), env-lookup stubs (:344-365, mostly commented out).
- `application.properties:402-518`: `ldap.auth.enabled=false` (:428); server `10.10.5.71:636` SSL (:462-464); `ldap.new.user.creation.Ldap.type=3` (Samba, :410); role defaults "Physician" (:418).
- `LDAPBasedRestrictivePasswordValidator` (`util/auth/LDAPBasedRestrictivePasswordValidator.java`): external password-manager delegate (`ldap.restrictive.handler.url` = `http://10.10.7.70:8082`, application.properties:1006) — only activated when `auth.pwd.manager` contains "restrictive" (App.java:141-145).

### 1.5 Rajasthan SSO (risl.sso.*)

- `interceptors/sso/SSOAuthFilter.java:100-227`: intercepts `POST {contextPath}/` with `userdetails` param (:106-107) → GET `risl.sso.gettoken.url` + encoded userdetails (:110-112) → parses `sAMAccountName` from SOAP `<string xmlns="SSO">` (:115-132) → DB lookup of units/roles by `UPPER(SAMACCOUNTNAME)` (:135-155) → `attempAuthenticationSSO` (no password!) (:159) → 301 to `risl.sso.ihmsmapping.url` with `__token` cookie (:177-179).
- Failures → redirect `ihms-no-user.html` (:174, :184, :194).
- `application.properties:646-648`: `risl.sso.gettoken.url=http://ssotest.rajasthan.gov.in:8888/ssows.asmx/GetTokenDetail?token=`; `risl.sso.ihmsmapping.url=http://localhost:8090/IHMS?token=`; `risl.sso.url=https://ssotest.rajasthan.gov.in` (:702).
- `public/ihms-sso.html` (SSO instructions) + `public/ihms-no-user.html` ("Permission denied... contact Hospital Administrator").
- Mobile: `risl.sso.mobile.url` + AES key (application.properties:1270-1272).

### 1.6 Token → request pipeline (the auth gate)

- `aspects/IntegrationOperationsFilter.java` (`@Order(HIGHEST_PRECEDENCE-2)`, :102):
  - Referer allowlist check in `setFingerprint` (:406-422): missing/disallowed Referer → 401.
  - For `/api/rest/*`: `token` from header or query param (:658, :870); **if `token == secretTokenForOnlineAppointment` → `ServiceLayerAspect.setGlobalUser(1L)`** (:659-660, :871-872) — a shared static token impersonates user id=1; else `view = cacheImpl.get(token)` → `UserTrxContextHolder.setSecurityEntity(view)` (:662-665, :874-877). If token unknown → view null → aspect rejects.
  - `fjs-config.json` endpoint requires valid token (:698-706); the `/online|ohumlite/fjs-config.json` token check is **commented out** (:729-738).
  - `/fetchToken` (:827-859) uses token for OPD queue machine registration.
  - Integration `/int/` mappings: requires token + `mappingMap` entry + optional `allowedUsers` incl. `"ROLE_INTEGRATION"` (:435-468) — **only real role-name check in backend**.
  - Reports `/rep/`: token required, POST only (:469-482).
- `aspects/ServiceLayerAspect.java` `handleWorkflow` (:250-555) — around-advice on all `com.ohum.services.*ServiceImpl` `@Path`/`@WebMethod` methods (:223-225):
  - `view = getUserView()` (:362); `isOpenApi = @NotSecure || @Login || @IgnoreApiForAuth` (:364); if isOpenApi && view==null → `setGlobalUser(1L)` (fake user id=1, unitId=1) (:365-367, :992-1001).
  - `isSecure = view != null` (:370-372); else → `DummyHTTPStatusException(UNAUTHORIZED)` (:442).
  - Sliding expiry: `generatedTime + sessionTimeout minutes`, renews `generatedTime` + re-caches (:391-416).
  - **Path-level permission check exists but is COMMENTED OUT**: lines :418-426 — `if (!view.getPermissionsMap().containsKey(fpath.toLowerCase()) || !view.getPermissionsMap().get(fpath.toLowerCase())) { // throw new DummyHTTPStatusException(Status.UNAUTHORIZED); }`.
  - Audit flush in `finally` when audits queued (:542-544).
- `@Secure` annotation (`annotations/Secure.java`) has `String[] value()` (roles) but **no code reads its values** — old role-based logic commented out at ServiceLayerAspect :316-322, :374-379.
- Spring Security (`SpringSecurityConfig.java`): `csrf().disable()` (:110-111); `SessionCreationPolicy.NEVER` + `enableSessionUrlRewriting(false)` (:112-114); `antMatchers("/**").permitAll()` (:117); `formLogin()` configured but with no user-details service; `httpBasic().disable()` (:121); CSP header writer (:105-109). **Spring Security is effectively inert** — the CXF/filter stack is the real gate.
- CXF interceptors: `PreProtocolInterceptor` (json default), `RequestResponseInterceptor` (response filtering `__crfls/__crfli/__crflm`), registered in `services.xml:234-242`.
- `CrudOperationsFilter` (`@Order(HIGHEST_PRECEDENCE-1)`, interceptors/CrudOperationsFilter.java:74-76) — CRUD cache refresh, not security.
- `GzipFilter.java` — response compression only (no security function).
- `IdEncryptionUtil` (`interceptors/IdEncryptionUtil.java`) — optional PHI field encryption on serialization (`phi.encryption.enabled`), wired into fjs-config `<<ENCCRYPTED_FIELDS>>` (IntegrationOperationsFilter:342).

---

## 2. User model & roles (entities)

| Entity | Table | Key fields | Evidence |
|---|---|---|---|
| `User.java` (DTO, not @Entity) | — | `password` (@JsonIgnore :35-37), `role` :42, `roleId` :90, `roleLevelId` :143, `ssoName` :104, `doctorId` :78, `wards` :84, `storeIds` :76, `employeeId` :92, `defaultRoute` :94, `vistaPassword` :82, `isNurse` :98, `isHod/isResident` :131-134 | models/User.java |
| `TUserdetails` | T_USERDETAILS | `LOGINNAME`, `PASSWORD` :97-98, `LOCKED` :86, `SAMACCOUNTNAME` :183-184, `ROLEID` :107, password policy cols :109-140 (`MINPASSWORDLENGTH`, `ATLEASTONEDIGIT`, `NOOFPASSWORDSTOREMEMBER`, `MAXPASSWORDAGE`, `ACCOUNTLOCKTHRESHOLD`, `ACCOUNTLOCKDURATION`…), `PASSWORDVISTA` @EncodeAsBase64 :168 | models/masters/TUserdetails.java |
| `TUserrolemaster` | T_USERROLEMASTER | `CODE`, `DESCRIPTION` (role name), `ISNURSEROLE`, `ISCLINICALROLE`, `ISSUPPLIER`, `LEVELID`, `SHOWMASTERNAV`, `BI_ROLE_NAME`; cached `@CrudCached(name="roleMaster")` :34 | models/masters/TUserrolemaster.java |
| `TUserunitdetails` | T_USERUNITDETAILS | userid, isdefault | models/masters/TUserunitdetails.java |
| `TUserWardDetails` | T_UserWardDetails | userId, wardId | models/masters/TUserwarddetails.java |
| `TUserHospitalAuthority` | T_USER_HOSPITAL_AUTHORITY | **no Java entity** — raw SQL only (DISTRICTID, BLOCKID) | DefaultAuthenticationImpl:857 |
| `ModulePermissionMaster` | M_ModulePermissionMaster | `path`, `apiMethod`, `apiPath`, `moduleName`, `action`, `parentId` | models/masters/ModulePermissionMaster.java |
| `RoleBasedPermission` | t_role_enabledpermission | `roleId`, `userId` (nullable), `enabledModulePermissionIdsJson` | models/RoleBasedPermission.java |
| `AuthorityMaster` | M_AUTHORITYMASTER | `authority`, amount, TDS, GST, contract… — **NOT RBAC; vendor/contract master** | models/masters/AuthorityMaster.java |

- User↔role↔unit mapping is **raw SQL only** (no entity): `t_userroles` referenced in DefaultAuthenticationImpl:228-231, SSOAuthFilter:136, UserManagementServiceImpl:281.
- Role CRUD UI: `public/js/user-module.js` (tUserrolemaster listing :622, role tabs :647-653) + `modules/commons/templates/tuserrolemaster.html`.
- User creation: `services/custom/AddUserServiceImpl.java` `saveUser` :673, `updateUser` :1931 — password stored `encryptDecryptUserKey(password)` (:885, :1180, :1504) — reversible obfuscation (`char*2-5`), see §7.
- Permission editor: `AddUserServiceImpl.getPermissions` :166-236 (jstree from ModulePermissionMaster + t_role_enabledpermission), `updatePermissions` :241-296 (ADD/REMOVE.MODULEPERMISSION audit); UI `public/js/masters-ext.js:112-162`.

---

## 3. Authorization enforcement — where it actually happens

### 3.1 Backend

| Mechanism | Location | Status |
|---|---|---|
| Per-API path permission check | `ServiceLayerAspect.java:421-426` | **COMMENTED OUT** (dead code) |
| `@Secure(roles)` / role-value enforcement | annotations/Secure.java; ServiceLayerAspect :316-322, :374-379 | **Never implemented** (commented out) |
| `MasterCachedServiceImpl.checkPermission(String)` | `services/custom/MasterCachedServiceImpl.java:10356-10362` (`view.getPermissions().contains(...)`) | Used in exactly **1 call site**: `InventoryServiceImpl.java:11588` (`"Inventory.AllApproval"`) |
| Integration API allowedUsers | `IntegrationOperationsFilter.java:442-447` (`"ROLE_INTEGRATION"` check :444) | Active for `/int/*` only |
| Token auth gate (any valid token = any API) | ServiceLayerAspect :370-372, :442 | Active |
| `hasRole`/`getCurrentUser`/method security | — | **No matches** in `com.ohum.services` (grep: only checkPermission hits above) |

### 3.2 Frontend (the REAL enforcement)

- `public/js/common-module.js:3389-3400` `checkPermissionForSplash()`: hides any `.menuRolesCls/.commonSubLinksCls/.mstConfigCls/.genericRolesClz/...` element whose `data-roleM` is not in `Fg.g("permissions")`.
- `common-module.js:3423-3427` `checkPermission(key)` — boolean helper used in JS conditions.
- `common-module.js:3377-3387` `setPermissionForSplash(clz, hideClz)`.
- Called from ~50 module files (billing-module.js:32, appointment-list-module.js:1383, bed-management-module.js:488, patho-booking-module.js:49, etc.).
- Sidebar menus carry `data-roleM` in `modules/commons/templates/masterNav.html` (:12, :18, :22, :28, :31, :34, :37...) and `roleBasedNav.html`.
- **2078 unique `data-roleM` keys** across all `modules/**/*.html` templates (counted). Top-level distribution: Configuration 1424, BIReport 263, Inventory 70, Admission 45, IHMSDashboard 41, Billing 34, Appointment 27, Nursing 26, Equipment 20, Pharmacy 18, Investigation 16, QueueManagement 16, Login 9, Report 7, + ~50 modules with 1-4 keys.
- Role-NAME string checks in JS (not permission keys): auth-init-module.js:87-92 (`role == "Administrator" || "FrontDesk"`, `"Doctor"`), :225-231 (nursing role list), :274 (`"administrator"/"Admin"`), common-module.js:6010-6026 (`"nhncFrontDesk"`, `"Nurse"`+`isClinicalRole()`), item-category-module.js:578 (`"Cashier"`), patho-modules.js:3-10 (Nurse/Ward-Incharge/...), nusing-module.js:11070-11072, masters-ext.js:1657 (`"DOCTOR"`+isHod), masters-modules.js:11268.
- `isClinicalRole()` — clinical flag from role master (TUserrolemaster.isClinicalRole) surfaced to JS.

**Conclusion: Authorization = token authenticity (backend) + permission-list gating of UI elements (frontend). There is no server-side RBAC check on business APIs.**

---

## 4. Session handling

- `application.properties:642` `session.timeout=30` (minutes); `:1477` `allowSingleSessionPerUser=true`.
- Token TTL = `sessionTimeout*60` seconds in cache (`DefaultAuthenticationImpl:881`, SSOAuthFilter:171, IntegrationOperationsFilter:171 in SSO path).
- Sliding renewal per request (`ServiceLayerAspect:402-416`) with re-cache `cacheImpl.add(token, view, sessionTimeout*60)`.
- No `HttpSession` usage anywhere in `com.ohum` (grep: 1 stray import in `PathoOrderServiceImpl.java:43`, unused). Spring `SessionCreationPolicy.NEVER` (SpringSecurityConfig:113).
- Single-session kill: `UNQ_SESS_USER_<id>` session-cache key (DefaultAuthenticationImpl:848-853), exception list `ALLOWED_MULTI_LOGIN_USERNAMES` app_config (:844-847).
- SSO cookie `__token` (SSOAuthFilter:179); frontend reads cookie in `startAction` (common-module.js:909-911) and clears on session-detail fetch (:986).
- `session_expire.html` template exists (`modules/commons/templates/session_expire.html`).
- Frontend 401 handling: `$.ajaxSetup` error → `versionCheckAndReloadIfNecessary()` / logout (auth-init-module.js:198-206).

---

## 5. Audit implementation

### 5.1 Components

- `com/ohum/audit/AuditHandler.java` — Velocity-based audit text templating; `AuditTemplateDefinition` enum with only **LOGIN("Logged In") and LOGOUT("Logged Out")** (:267). Writes `T_AUDIT` via `Audit` model (`genericDAO.save(audit)` :109).
- `com/ohum/audit/AuditHelper.java` — `saveAuditEvent` (:67-147): enriches from `AuditEventMaster` template cache (`auditEventMasterMap`), sets token/unitId/addedBy from `UserView`, saves to `T_AUDIT`, then `handleAuditPostActions` (:149-666) inserts `T_TRX_HISTORY` rows for ~60 event IDs (CREATE.VISIT, TRANSFER.BED, ADD.DISCHARGE, MLC*, MORTUARY*, pathology SAMPLE*/RESULT*, BLOODBANK*, ERAKTKOSH*, grievance T_GRIEVANCE_TICKET_HISTORY, etc.). Post-login → `T_LOGIN_AUDIT` (:913-932); ABHA creation → `T_ABDM_AUDIT` (:934-955); appointment/lab channel → `T_APPOINTMENT_CHANNEL` (:957-973).
- `saveJobEvent(JobAuditEvent)` (:906-911) — job audit rows; created in `ServiceLayerAspect.handleJobExecution` (:816-851) for `@JobEvent` methods, saved with start/end time + failure details.
- `JobAuditEvent` model (`models/JobAuditEvent.java`) — name, startTime, endTime, status, failureDetails (no @Table → default table name `JobAuditEvent`).
- `AuditEventMaster` (models/masters/AuditEventMaster.java) — eventId → templateString, cached as `auditEventMasterMap` (AuditHelper:73).

### 5.2 How events get queued

- Manual pattern (dominant): services add `Audit` records to `UserTrxContextHolder.getInternalAudit().getAudits()` — **59 service files, 568 occurrences**; flushed once per request in `ServiceLayerAspect` finally (:542-544) via `auditHelper.saveAuditEvent(..., remoteAddr)` when `view != null`, `doAudit`, and audits queued.
- Annotation pattern (vestigial): `@AuditEvent({AuditTemplateDefinition.LOGIN})` on 9 methods in `AuthenticationServiceImpl` + 1 in `AddUserServiceImpl` — `AuditHandler.auditEvents` is never called from `ServiceLayerAspect` (template handling commented out; only `setAuditParameters` runs, :519-541 → unused vars). Total `@AuditEvent` occurrences: 10.
- Audit identity: `handlePrePersistCallbackHooks` (`ServiceLayerAspect:697-750`) sets sequence PKs; `MBase.beforeCreateUpdateCallback` (`models/MBase.java:72-90`) stamps `ADDEDBY/ADDEDDATETIME/UPDATEDBY/UPDATEDDATETIME` from `view.getUser()` — applied on `GenericDAO.save/update` @Before (:697-775).

### 5.3 Models

- `models/Audit.java` → `T_AUDIT`: eventId, eventDescription, patientId…employeeId (30+ FK-style ids), remoteAddress, result, token (:225-226), addedBy/unitId inherited; transient: auditObjects, auditPropMap, audits, values, isOtp, extraDetails.
- `models/TrxHistory.java` → `T_TRX_HISTORY`: eventId, eventDescription, trxId, unitId, userId, patId, opd_ipd, resultValues, pathoOrderBookingId(Details)Id, opd_ipd_id — patient transaction trail derived from audit events.

---

## 6. CSRF / CORS / headers

- CSRF: **disabled** (SpringSecurityConfig.java:110-111); `UserView.csrfToken` field exists but never populated (views/UserView.java:87-89).
- CORS bean (App.java:213-227): registered only for `/rest/**` (**mismatch**: real prefix is `/api/rest`), `allowCredentials(false)`, origins from `cors.allowed.endpoints` (application.properties:329 = `https://ohumcrd.com`), methods GET/PUT/POST/DELETE/OPTIONS.
- Referer allowlist: `allowed.referer.urls` (application.properties:858: localhost:8080, health.rajasthan.gov.in, ssotest.rajasthan.gov.in, fhir.ohumhealthcare.com, ohumcrd.com, localhost:11100, aadhaarauth.rajasthan.gov.in) — enforced in `IntegrationOperationsFilter.setFingerprint` (:417-420); also CSP `frame-ancestors` (SpringSecurityConfig:84).
- `allowed.urls` (application.properties:730) feeds CSP script-src/style-src/connect-src (SpringSecurityConfig:70-96, IntegrationOperationsFilter:1048-1088) — includes `'unsafe-inline' 'unsafe-eval'` (script-src) — weak CSP.
- Security headers (HSTS, X-XSS-Protection, X-Content-Type-Options, CSP) on served pages (IntegrationOperationsFilter:690-693, SpringSecurityConfig:105-109).

---

## 7. Password handling — CRITICAL

- **Default `PasswordValidator.attempt(...)` always returns `true`** — the compare is commented out: `util/auth/PasswordValidator.java:24` — `return true;//suppliedPassword.equals(actualPassword);`. It sha256-hashes the stored value but never compares it to the input.
- Active bean: `App.java:139-145` — `auth.pwd.manager=password` (application.properties:1028) → `authMgr.toLowerCase().contains("restrictive")` is false → plain `new PasswordValidator() {}` (no-op). **=> With the checked-in config, any password (or none) succeeds for any unlocked user whose loginname+role+unit match.** The only credential-adjacent checks: user exists, status='1', not locked (BEL002), and role/unit mapping.
- Restrictive mode (not active): `LDAPBasedRestrictivePasswordValidator` forwards to external password manager `ldap.restrictive.handler.url` (`/api/login`, `/api/logout`) with `linkedToken`, supplied password as `token`, client IP hardcoded `0.0.0.0` (:47).
- Storage: `encryptDecryptUserKey` = `(char*2 - 5)` reversible transform (`DefaultAuthenticationImpl.java:410-416`, `AddUserServiceImpl.java:136-153`) — **obfuscation, not hashing**; stored in `T_USERDETAILS.PASSWORD`.
- "sync" role: `inppwd = sha256(decryptEncryptUserKey(password))` (DefaultAuthenticationImpl:253-255) — still never compared.
- `checkSidNumber` compares stored `SID` column with LDAP objectSid (DefaultAuthenticationImpl:922-931).
- Client-side hashing: browser sends `sha256(password)` (common-module.js:1002-1005) — so plaintext never travels (good), but server does not verify.
- `TUserdetails` password-policy columns exist (length, complexity, age, lock threshold) — enforcement code **not found** in auth path (mark unknown).
- Secrets in clear in application.properties: DB password (:97), emr.manpassword (:227), LDAP bind password (:448), sso REST creds (:996-998), `secretTokenForOnlineAppointment` (:598/636 — **also shipped to browser** in `public/fjs-config.json:42` globals.onlineToken), ohumDoc.authToken (:333), fhir.auth.token (:1148), minio.secret.key (:756), several API keys.

---

## 8. Known role names found in code (data-driven roles in T_USERROLEMASTER)

Administrator, Admin, FrontDesk (→nhncFrontDesk alias), Doctor, doctorscan, Cashier, Nurse, student nurse, staff nurse, nurse-staff, Ward-Incharge, nusing-incharge, nursing supervisor, nursing superintendent, Nurseing Superwiser, Nurseing Superintendent, senior allied health therapist, allied therapist, community health worker, ot technician, sync, ROLE_INTEGRATION, HOSPITAL ADMIN (external login), Physician (LDAP default). Source: JS role-name checks (auth-init-module.js:87-92,225-231,274; common-module.js:6010-6026; item-category-module.js:578; patho-modules.js:3-10; nusing-module.js:11070-11072; masters-ext.js:1657), DefaultAuthenticationImpl:253,948, IntegrationOperationsFilter:444, LDAPAuthenticationUtil:67.

**Role count is DB-driven and cannot be determined from code.**

---

## 9. Module → screen → action → API mapping evidence

- Modules: `src/main/resources/public/fjs-config*.json` — 48 per-module config files (billing, pharmacy, pathology, nursing, inventory, ot, op, ip, masters, commons, reports, rcm, …) + main `fjs-config.json` (settings/globals/schemas/router; duplicate-key error on parse — `pharmacy:ipReturnFlow` vs `pharmacy:iPReturnFlow`).
- Screens: `schemas` in fjs-configs; templates under `modules/<module>/templates/*.html`.
- Actions: `data-roleM="Module.Screen.Action"` (`.Add/.Edit/.Delete/.Upload/.Download/.View…`) — 2078 unique keys (see §3.2).
- API linkage: `ModulePermissionMaster.apiMethod/apiPath` columns (DB data, not in repo); service `@Path` annotations map REST routes.
- Sample permission paths (evidence): `Billing.OPIPBilling` (auth-init-module.js:184), `Inventory.AllApproval` (InventoryServiceImpl:11588), `Billing.DeathPatientBilling` (DefaultAuthenticationImpl:790), plus template keys above.

---

## 10. Top security risks (evidence-backed)

1. **No server-side authorization** — per-API permission check commented out (ServiceLayerAspect:421-426); only 1 active `checkPermission` call (InventoryServiceImpl:11588). Any authenticated user can call any business API directly.
2. **Password verification disabled by default** — PasswordValidator returns true (PasswordValidator.java:24); active config `auth.pwd.manager=password`.
3. **Shared static auth bypass token** — `secretTokenForOnlineAppointment` grants user id=1 (IntegrationOperationsFilter:659, 871); value embedded in `fjs-config.json` (browser-downloadable) and application.properties.
4. **Hardcoded daily test-user backdoor** (`test.user.internal.001`, DefaultAuthenticationImpl:199-223).
5. **@NotSecure/@Login/@IgnoreApiForAuth → setGlobalUser(1L)** — open APIs run as fake user id=1 (ServiceLayerAspect:364-367).
6. **Plaintext-adjacent secrets in repo** (application.properties, fjs-config.json) — DB, LDAP, SSO, MinIO, API keys.
7. **Reversible password obfuscation** (`encryptDecryptUserKey`, char*2-5) instead of hashing.
8. **CSRF disabled** + token in URL param accepted (`request.getParameter("token")`) → leakage via logs/referrer.
9. **Weak CSP** — `script-src 'unsafe-inline' 'unsafe-eval'`.
10. **LDAP util uses TrustAllTrustManager** (LDAPAuthenticationUtil.java:117).
11. SSO flow authenticates with **no password** server-side (SSOAuthFilter:159 → attempAuthenticationSSO) — relies entirely on external SSO token validation via `risl.sso.gettoken.url`.
12. Patient login hardcodes role "Administrator" (DefaultAuthenticationImpl:358).
13. External login (`/loginJ`) maps API key → first `%HOSPITAL ADMIN%` user (DefaultAuthenticationImpl:948-950).
14. `getuserroleOtp` uses `LOWER(SAMACCOUNTNAME)=?` without status check on unit (minor).
15. Audit gaps: `@AuditEvent` framework largely unused (10 annotations); audit is manual via 59 services — coverage uneven by construction.
