# AGT-05 — IAM & Security Groundwork (Wave 0 · MIG-004 dependency-neutral prep)

**Agent**: AGT-05 (IAM & Security Engineer) · **Status**: DRAFT — IdP recommendation below is a **DRAFT ADR awaiting Agent 00 ratification** (§5.4).
**Scope**: Read-only review of legacy authz evidence (`/mnt/d/ihms`) + current reference-service security posture (`/mnt/d/hims_new_deloitte`) + written design. **No legacy or target code/docs modified.**
**Blocker posture**: MIG-004 (IAM extraction) remains BLOCKED (BLK-001/002: no Oracle live env, no legacy runtime). This report is the dependency-neutral groundwork (policy: never stop the swarm for one blocker). Report path: `outputs/iam/AGT-05-IAM-groundwork.md`.

---

## 1. Executive summary

- The legacy monolith's **authentication is a cache-backed opaque-token session system** (`Base64(UUID)` token ↔ `UserView` in Redis), implemented on the CXF filter/AOP stack (`IntegrationOperationsFilter` + `ServiceLayerAspect`). Spring Security exists but is inert (`/**`.permitAll). JWT is **not** used.
- Authorization is **data-driven and mostly frontend-enforced**: a per-role permission-path list (`M_ModulePermissionMaster` paths via `t_role_enabledpermission`) is shipped to the browser at login; the only general server-side per-API permission check in `ServiceLayerAspect` is **commented out**. Documented as legacy finding **E-07** — the target must *not* replicate it (IMP-DEC-006: default-deny in new services).
- Scope granularity is **role × unit** (`T_USERROLES(userid, roleid, unitid)` grant tuples), plus **store** (`T_USERSTOREDETAILS`), **ward** (`T_USERWARDDETAILS`, `T_NurseWardDetails`), department/doctor (`M_DOCTORMASTER`), and cash-counter binds. Every entity carries tenancy/audit columns via `MBase` (`UnitID, ADDEDBY/UPDATEDBY, ADDEDDATETIME/UPDATEDDATETIME, CREATEDUNITID/UPDATEDUNITID, STATUS, SYNCHRONIZED`).
- Password handling is **not production-safe**: default `PasswordValidator.attempt()` returns `true` unconditionally (comparison commented out); a Rajasthan-state "Password Manager" REST service is the only real credential check when `ldap.restrictive` mode is configured; DB stored procedures compare `tu.password = p_password` directly; a **hardcoded test-user bypass** exists in `DefaultAuthenticationImpl` (no password check for `test.user.internal.001`).
- **Recommendation (DRAFT ADR)**: adopt **OIDC/OAuth2 with a self-hosted Keycloak** as the IdP (realms per environment, role+unit grouped claims, LDAP/AD federation to `ohum.healthcare` AD), expose new services as Spring Security 6 **resource servers validating JWT (RS256)**, keep **default-deny**, and map legacy RBAC/scope tables to **claims** per §6 (no token-format compatibility sandbox needed — the gateway MIG-005 maps the legacy `token` header to OIDC bearer at the edge).
- reference-service current posture is verified good: **default-deny + stateless + 401**; tests pin `401` for unauthenticated requests. The MIG-004 delta is a concrete, small file set (§7).

---

## 2. Legacy authorization model (evidence-backed)

Evidence base: JPA entities + raw SQL/stored procedures + auth filters/aspects in `/mnt/d/ihms` (read-only). Cross-check: `docs/modernization/08-security-rbac-audit.md` (Agent 08 AS-IS dump) agrees with the source-level findings below.

### 2.1 Entity/table inventory

| Table | Entity / Evidence | Columns (beyond `MBase`/`LBase` base) | Role in authz |
|---|---|---|---|
| `T_USERDETAILS` | `com/ohum/models/masters/TUserdetails.java` (`@Table("T_USERDETAILS")`) | `LOGINNAME, ISEMPLOYEE, ISDOCTOR, ISPATIENT, EMPLOYEEID, DOCTORID, PATIENTID, SECRETEQTN, SECRETEANS, LOCKED, UPDATEDWINDOWSLOGINNAME, ENABLEPASSWORDEXPIRATION, PASSWORDEXPIRATIONINTERVAL, PASSWORD, FIRSTPASSWORDCHANGED, USERTYPE, ROLEID, MINPASSWORDLENGTH, MAXPASSWORDLENGTH, ATLEASTONEDIGIT, ATLEASTONELOWERCASECHAR, ATLEASTONEUPPERCASECHAR, ATLEASTONESPECIALCHAR, NOOFPASSWORDSTOREMEMBER, MINPASSWORDAGE, MAXPASSWORDAGE, ACCOUNTLOCKTHRESHOLD, ACCOUNTLOCKDURATION, CASHCOUNTERID, ISLOGIN, ISCHARGECANCELLATION, ISALLOWEDCASHTRANSACTION, ISCHARGECONCESSION, CONCESSIONCATEGORYID, SAMACCOUNTNAME (SSO), JANAADHARID/MEMBERID, MRNO, OUTREACHUSERID, HOSPITALTYPEIDSJSON, `UnitID`/`ROLEID` (default unit + default role on the user row)` | Principal identity; credential store; per-user password policy; lockout; default unit+role; SSO link (`sAMAccountName`) |
| `T_USERROLEMASTER` | `TUserrolemaster.java` (`@Table("T_USERROLEMASTER")`, `@CrudCached("roleMaster")`) | `CODE, DESCRIPTION, UPDATEDWINDOWSLOGINNAME, ISNURSEROLE, ISCLINICALROLE, ISSUPPLIER, LEVELID, SHOWMASTERNAV, ISBLOODBANKLABTECHNICIAN, BI_ROLE_NAME` (+ DB-only `DEFAULTROUTE` referenced by stored procs/queries, e.g. `CIMS_LOGINNAMEEXISTSDOCIT_IMPROVED_ROLE_NEW.sql`) | Role catalog (data-driven; ~30 role names referenced in code per Agent 08) |
| `T_USERROLES` | **No JPA entity** — raw SQL only; inserts in `AddUserServiceImpl`: `Insert into t_userroles (userid, roleid, unitid) values (?,?,?)` | `USERID, ROLEID, UNITID` (+ MBase-style audit/status cols in procs) | **Grant tuple: user gets role *within* a unit** (role × unit scoping) |
| `T_USERUNITDETAILS` | `TUserunitdetails.java` (`@Table("T_USERUNITDETAILS")`) | `USERID, ISDEFAULT` (+ `UnitID` from `MBase`) | Which units a user may operate in; `ISDEFAULT` marks the default unit |
| `T_USERSTOREDETAILS` | `TUserStoreDetails.java` (`@Table("T_UserStoreDetails")`) | `USERID, STOREID` (+ `UnitID`, `STATUS`) | Store-level scope (pharmacy/inventory stores) |
| `T_USERWARDDETAILS` | `TUserWardDetails.java` (`@Table("T_UserWardDetails")`) | `USERID, WARDID` (+ `UnitID`) | Ward-level scope (nursing) |
| `M_MENUMASTER` | `Menumaster.java` (`@Table("M_MENUMASTER")`) | `TITLE, IMAGEPATH, PARENT, MODULE, ACTION, HEADER, CONFIGURATION, MODE, ACTIVE, PARENTID, MODULEID, MENUORDER` | **Menu catalog only — no role column.** Menus are shown per role via frontend permission-path gating, not DB joins |
| `M_ModulePermissionMaster` | `ModulePermissionMaster.java` (`@Table("M_ModulePermissionMaster")`) | `path, apiMethod, apiPath, moduleName, action, parentId` (+ MBase) | API-level permission definitions (dotted paths like `Billing.OPIPBilling`) |
| `t_role_enabledpermission` | `RoleBasedPermission.java` (`@Table("t_role_enabledpermission")`) | `roleId, userId, enabledModulePermissionIdsJson` (JSON array of `M_ModulePermissionMaster.ID`) | Per-role/user enabled permission-ID set (read at login to build the permission path list) |
| `t_rolebasedpermission` | no entity (raw SQL in `BaseServiceImpl.checkRolePremission`) | `roleId, userId, modulepermissionid` (+ MBase) | Row-per-permission mirror used by `checkRolePremission(path, roleId)` |
| `T_AUDIT` | `Audit.java` (`@Table("T_AUDIT")`) | `EVENTID, EVENTDESCRIPTION, PATIENTID, DOCTORID, DEPARTMENTID, VISITID, ADMISSIONID, NURSEID, STOREID, WARDID, BILLID, CHARGEID, ... ITEMS, TOKEN, REMOTEADDRESS` + MBase audit columns + transient `role` | Audit event log with who/what/unit attribution |
| `MBase` / `LBase` | `models/MBase.java`, `models/LBase.java` | `ID` (PK, sequence), `UNITID, STATUS, CREATEDUNITID, UPDATEDUNITID, ADDEDBY, ADDEDDATETIME, UPDATEDBY, UPDATEDDATETIME, SYNCHRONIZED`; `beforeCreateUpdateCallback(userId, unitId)` stamps actor+unit | Tenancy + audit attribution injected into **every** table |

### 2.2 Roles

- Roles are **master data**, not enums: `T_USERROLEMASTER` rows (`CODE`/`DESCRIPTION` flags `ISNURSEROLE`, `ISCLINICALROLE`, `ISSUPPLIER`, `LEVELID`, `SHOWMASTERNAV`, `BI_ROLE_NAME`). Role descriptions are used as the runtime role string (`user.setRole(roleById.getDescription())` — `DefaultAuthenticationImpl.loginViewUtilFunction`).
- A user's roles are the `T_USERROLES` tuples for their `USERID`; **same user may hold different roles in different units**; one tuple is the default (`T_USERDETAILS.UNITID`/`ROLEID` or `T_UserUnitDetails.ISDEFAULT` + `T_UserRoles` default flags; SSO flow reads default unit/role from `T_UserDetails`).
- Login is **role+unit specific**: `attempAuthentication(username, password, unitId, roleId, ...)` — the caller selects which (unit, role) to authenticate into; `/auth/getuserrole` lists `(roleId, description)` per unit for a loginname.

### 2.3 Unit / store / ward scope

- **Unit (facility)**: `T_USERUNITDETAILS` membership (+ `ISDEFAULT`); every record in the DB is unit-scoped via `MBase.UnitID`; most domain queries filter `unitId` from the session (`UserView.getUser().getUnitId()`; MDC `unit`).
- **Store**: `T_USERSTOREDETAILS(userid, storeid)`; login loads `storeIds` from `t_userstoredetails WHERE userid=? AND status=1 AND unitid=?` (`CIMS_GETUSERDETAILSANDPERMISSIONS.sql` r2) and only active stores survive (`user.setStoreIds`).
- **Ward**: `T_USERWARDDETAILS(userid, wardid)` entity exists; the primary login path derives wards from staff mapping: `SELECT wardid FROM T_NurseWardDetails JOIN T_userdetails ON T_NurseWardDetails.staffid = T_userdetails.employeeid WHERE T_userdetails.id = ?` (`DefaultAuthenticationImpl.loginViewUtilFunction`) → `user.setWards(...)`.
- Department/room: derived at login from doctor/staff mappings (`M_DOCTORDEPARTMENTDETAILS`, department/room mappings; partly commented out in code, set from queue/room lookup when token workflow on). `M_DOCTORMASTER` flags (`ISNURSE, ISRESIDENTDOCTOR, ISHOD`) and `M_STAFFMASTER` identity complete the profile.
- Cash counter: `M_CASHCOUNTERMASTER WHERE userid=?` → `cashCounterId/opdStationId` in session.

### 2.4 Menu-permission model

- `M_MENUMASTER` = menu tree (title/parent/module/action/order). **No role linkage at DB level.**
- Runtime permission surface = **dotted path list** built at login (`DefaultAuthenticationImpl.loginViewUtilFunction`):
  1. Load all `M_ModulePermissionMaster`.
  2. Load `t_role_enabledpermission WHERE roleId = ?` (optionally plus userId).
  3. Resolve `enabledModulePermissionIdsJson` → `ModulePermissionMaster.path` set; **auto-derive parent prefixes** (e.g. `Billing.OPIPBilling` also adds `Billing`).
  4. Store as `UserView.permissions` (List<String>) and `permissionsMap` (Map<String,Boolean>); shipped to the frontend, which gates menu/button visibility with `data-roleM` attributes (~2,078 unique keys — Agent 08).
- Server-side enforcement was intended: `ServiceLayerAspect` per-path lookup `view.getPermissionsMap().containsKey(fpath)` — **commented out** (dead code; Agent 08 `ServiceLayerAspect.java:418-426`). The only live server-side role checks are: `BaseServiceImpl.checkRolePremission(path, roleId)` (`m_modulepermissionmaster ⋈ t_rolebasedpermission ... WHERE roleId=? AND path=?`), used opportunistically in a few services (e.g. `AddUserServiceImpl`), `@Secure(roles)` (never implemented), and the `/int/*` integration path `ROLE_INTEGRATION` check.
- **Consequence (E-07)**: any authenticated user (valid token) can call any business API — RBAC is *de facto* frontend-only. New services must enforce authorization server-side.

### 2.5 Password handling (security-critical findings)

1. Client hashes password (`sjcl.sha256`) then Base64 in some variants; server-side path: `DefaultAuthenticationImpl.attempAuthentication` → `passwordManager.attempt(token, username, inppwd, pwd, "sha256", p -> MdmServiceImpl.decryptEncryptUserKey(p), null, referer)`.
2. **Default `PasswordValidator.attempt()` returns `true` unconditionally** — the comparison line is commented out (`util/auth/PasswordValidator.java`). With `auth.pwd.manager=password` (application.properties:1028) the check is a **no-op**; only user-exists + not-locked + role/unit mapping are checked. **Effective authentication bypass for any known loginname at its (unit, role).**
3. Real credential checking exists only in restrictive mode: `LDAPBasedRestrictivePasswordValidator` POSTs `linkedToken/token/address/principal/envUrl` to `ldap.restrictive.handler.url` (`http://10.10.7.70:8082`)/api/login — an external "Password Manager" service (down → login blocked with BEL001).
4. Stored procedures compare hashes directly: `CIMS_LOGINNAMEEXISTSDOCIT_IMPROVED_ROLE_NEW.sql` — `WHERE tu.loginname = p_loginname AND tu.password = p_password` (hash stored; compare happens DB-side; 3-strike lockout: `lockedcount < 3 → +1`, `>= 3 → locked='1'`).
5. Per-user password policy columns exist on `T_USERDETAILS` (min/max length, digit/lower/upper/special, history `NOOFPASSWORDSTOREMEMBER`, age, `ACCOUNTLOCKTHRESHOLD/DURATION`, `FIRSTPASSWORDCHANGED`, `ENABLEPASSWORDEXPIRATION`).
6. **Hardcoded test-user backdoor**: `username.equals("dGVzdC51c2VyLmludGVybmFsLjAwMQ==")` (`test.user.internal.001`) — password rotated daily by a string of sha256 hex concatenations; the user **skips `passwordManager.attempt` entirely** after this block (query still requires row existence). Should be removed during IAM cutover (breaker: any legacy-retained auth path inherits this).
7. Reversible obfuscation in `MdmServiceImpl.decryptEncryptUserKey` (`c*2-5`) applied to passwords for `sync`-role users and MDM flow — plaintext-equivalent storage for those accounts.
8. `T_USERDETAILS` also stores `USERNAMEVISTA/PASSWORDVISTA` (Base64-encoded, `@EncodeAsBase64`) — external EMR credentials in the user table.
9. Security questions (`SECRETEQTN/SECRETEANS`) for self-service reset; OTP login (`/auth/loginotp`) supported; password expiry flags.

### 2.6 Token mechanism

- Token = **`Base64(UUID.randomUUID())`** (`AuthenticationInterface.getNewEncodedToken()`) — opaque, no JWT. Sent as `token` **header or `?token=` query param** (`IntegrationOperationsFilter`); SSO redirect sets a `__token` cookie.
- Server-side session object `UserView` stored in the cache (`cacheImpl.add(token, view, sessionTimeout*60)`, `session.timeout = 30` min, **sliding renewal** per request; Redis backend per `spring.redis.*`; in-memory `MemoryCacheImpl` fallback).
- Request flow: `IntegrationOperationsFilter` → `cacheImpl.get(token)` → `UserTrxContextHolder.setSecurityEntity(view)` (ThreadLocal); `ServiceLayerAspect` → `view == null` → **401** (`DummyHTTPStatusException(Status.UNAUTHORIZED)`); `@Login`/`@NotSecure`/`@IgnoreApiForAuth` endpoints run as **fake user id=1** when no token.
- **Bypass**: `secretTokenForOnlineAppointment` header value runs as global user id=1 (application.properties:598 — a committed static secret).
- Referer allowlist (`allowed.referer.urls`) enforced for `/api/rest/*`; CSRF only for online-appointment form page; single-session enforcement `allowSingleSessionPerUser=true` (evicts prior token via `UNQ_SESS_USER_<userId>`); logout removes token + `_BI` cache keys.
- **SSO (Rajasthan state)**: `SSOAuthFilter` exchanges a `userdetails` POST → RISL `GetTokenDetail` SOAP (`risl.sso.gettoken.url`) → `sAMAccountName` → DB lookup (`T_UserDetails ⋈ T_UserUnitDetails ⋈ T_UserRoles ⋈ T_UserRoleMaster ⋈ M_StaffMaster`) → unit-wise role map → `attempAuthenticationSSO` (no password). Mobile SSO: AES-encrypted creds → `risl.sso.mobile.url`. LDAP/AD beans exist (`CustomLDAPAuthenticationImpl`) but **not active** (`ldap.auth.enabled=false`).

### 2.7 Legacy model — condensed summary (for the target mapping in §6)

| Concept | Legacy representation |
|---|---|
| Identity | `T_USERDETAILS.ID` (numeric), `LOGINNAME`, `SAMACCOUNTNAME` (SSO), staff/doctor/patient link ids |
| Roles | `T_USERROLEMASTER` catalog; `T_USERROLES(userid,roleid,unitid)` grants; default via `T_USERDETAILS.UNITID/ROLEID` |
| Facility scope | `T_USERUNITDETAILS(userid, unitid, isdefault)`; `MBase.UnitID` on every entity |
| Department scope | Derived from staff/doctor dept mappings (`M_DOCTORDEPARTMENTDETAILS`) at login |
| Store scope | `T_USERSTOREDETAILS(userid, storeid)` |
| Ward scope | `T_USERWARDDETAILS(userid, wardid)` + `T_NurseWardDetails` via employee |
| Permissions | `M_ModulePermissionMaster` (paths) ← `t_role_enabledpermission` (JSON ids) / `t_rolebasedpermission` (rows); shipped to UI; server check mostly disabled |
| Menus | `M_MENUMASTER` catalog; UI gating by permission path |
| Credentials | Weak: no-op validator by default; external PW manager in restrictive mode; DB-side hash compare; test-user bypass; obfuscated MDM/Vista creds |
| Session/token | Opaque Base64-UUID token w/ server-side cache TTL 30 min; sliding renewal; single-session option; static bypass secret |
| Audit | `T_AUDIT` + MBase actor/unit stamps; service-authored manual events |

---

## 3. Reference-service current posture (verified)

**File**: `services/reference-service/src/main/java/com/hims/reference/configuration/SecurityConfig.java` (Spring Boot **3.5.3** / Spring Security 6, Jakarta).

```java
http
  .csrf(AbstractHttpConfigurer::disable)
  .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .exceptionHandling(eh -> eh.authenticationEntryPoint(
        (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
  .authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health/**", "/v3/api-docs/**",
                         "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
        .anyRequest().authenticated())
  .httpBasic(AbstractHttpConfigurer::disable)
  .formLogin(AbstractHttpConfigurer::disable);
```

**Verified properties** (IMP-DEC-006):
- ✅ **Default-deny**: only actuator health + OpenAPI docs + `/error` are `permitAll`; every other request requires an authenticated principal. No legacy-style `/**` permitAll (contrast `D:\ihms\...\SpringSecurityConfig.java` which does `.antMatchers("/**").permitAll()`).
- ✅ **401 behavior**: custom `AuthenticationEntryPoint` → `sendError(SC_UNAUTHORIZED)` (empty body; the JSON error envelope is the MIG-004 delta).
- ✅ Stateless (no JSESSIONID); CSRF disabled (stateless bearer APIs — acceptable per OAuth2 resource-server norms; CSRF protection for the future UI/BFF remains a gateway/UI concern).
- ⚠️ **Currently any authenticated principal passes** (there is no authorization layer beyond "authenticated"); bearer-token *validation* is explicitly deferred to MIG-004 (comment in source). This is the documented interim posture, not a legacy replication.

**Tests** (`src/test/java/com/hims/reference/api/MasterReadControllerTest.java`, `@WebMvcTest` + real `SecurityConfig`):
- `byId_withoutAuth_isRejected` → expects `status().isUnauthorized()` — **401 pinned for unauthenticated access**.
- Authenticated cases use `@WithMockUser` / `.with(user("tester"))`; they assert legacy-shape JSON, 400/404 envelopes, `/entities` list. (Recorded green in `docs/modernization/implementation/MIGRATION-TRACKER.md` — "Security: default-deny + 401 ✅".)

**What already anticipates MIG-004**: controller `unitId` params are documented "legacy derives from session token; IAM supplies it from MIG-004 onward" (`MasterReadController.java`); `CorrelationIdFilter` (`X-Request-ID`, MDC `corr`) is auth-agnostic; `application.yml` externalizes DB/Kafka via env vars.

---

## 4. Target IAM design

### 4.1 Principles (contract-first, no legacy replication)

1. **Authorization server-side only** — never ship permission lists to the browser as the enforcement mechanism (legacy E-07 anti-pattern). UI may still receive them for rendering, but the API enforces.
2. **Default-deny everywhere** — every new service starts from `authenticated()` + explicit grants (IMP-DEC-006 already encodes this).
3. **OIDC/OAuth2 as the single protocol** for interactive login (authorization-code + PKCE through the UI/BFF) and machine access (client-credentials). Legacy opaque-token and RISL-SSO exchanges terminate at the gateway (MIG-005) or a transitional auth shim — services only ever see an OIDC-validated principal.
4. **JWTs (JWS RS256/ES256) as the token format for services**, validated against the IdP JWKS; short expiry (15 min) + revocation via IdP session/refresh; no server-side session store in services (stateless).
5. **Claims, not lookups** — legacy grant model maps to claims (roles, units, stores, wards, permissions) so services do not query IAM per request; high-cardinality data (permissions) can be pruned to what services enforce.
6. **Audit attribution from the token** — every request carries actor (`sub`/userId), facility (`hims:defaultUnitId`), and scope lists so services can stamp `T_AUDIT`/outbox events without a DB session lookup.
7. **Secrets externalized** — no committed secrets (legacy committed `secretTokenForOnlineAppointment`, LDAP bind password, SSO REST password, MDM keys — all findings above) must never appear in target repos/cfg.

### 4.2 Architecture

```
Browser (React, W3+)                  API Gateway (MIG-005)              reference-service (S21)          IAM service (MIG-004)
   │  login@IdP (PKCE)                      │  validate JWT (JWKS)             │  Spring Security 6 RS            Keycloak cluster
   ▼                                       ▼                                 ▼                                (LDAP/AD federation →
IdP (Keycloak)  ── tokens ──────────►  passes bearer JWT  ────────────►  JwtAuthenticationConverter    ohum.healthcare AD 10.10.5.71)
   │  (Authorization Code + PKCE;          │  legacy `token`/RISL-SSO          │  claims → Authorities/
   │   client-credentials for M2M)         │  shim (transitional)              │  HimsPrincipal
   │                                       ▼                                 ▼
   └── realm: hims (dev/stg/prd)      UserCreated/RoleChanged events ──►  outbox → Kafka (UserCreated,
                                        (IdP ↔ IAM provisioning sync)     RoleChanged per MIG-WAVES §2.2)
```

- **Interactive**: UI → IdP (Keycloak) via authorization-code + PKCE through a BFF/gateway; services receive `Authorization: Bearer <JWT>`.
- **M2M**: service/client-credentials with audience `hims-services`, scopes per domain (`masters:read`, …).
- **Transitional compat (gateway, MIG-005)**: legacy clients sending `token=<opaque>` or RISL-SSO cookies are shimmed: gateway exchanges/forwards to IAM, converting to a service-bound JWT; reference-service never learns the legacy token format.
- **Provisioning**: user/role/unit/store/ward grants remain governed by the IAM service's own tables (migrated from `T_USER*` at MIG-004); IdP groups mirror `(unit × role)` composites; `UserCreated/RoleChanged` events flow on Kafka for local caches (per MIG-WAVES §2.2 "Users/grants: JWT + /users API; UserCreated event").

### 4.3 Spring Security 6 resource-server config (target shape)

```java
http
  .csrf(AbstractHttpConfigurer::disable)
  .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .exceptionHandling(eh -> eh
        .authenticationEntryPoint(json401("AUTH_REQUIRED"))     // 401: missing/invalid token
        .accessDeniedHandler(json403("ACCESS_DENIED")))         // 403: authenticated, insufficient scope
  .oauth2ResourceServer(rs -> rs
        .jwt(jwt -> jwt.jwtAuthenticationConverter(himsJwtConverter())))   // claims → authorities/principal
  .authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health/**", "/v3/api-docs/**",
                         "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/v1/masters/**").hasAuthority("PERM_masters.read")
        .anyRequest().authenticated());
```

- `JwtAuthenticationConverter` → `HimsPrincipal` + `GrantedAuthorities`: `realm roles` (→ `ROLE_<code>` for coarse gates) and `hims:permissions` (→ `PERM_<path>` — dotted paths normalized, e.g. `Billing.OPIPBilling` → `PERM_Billing.OPIPBilling`) for fine-grained gates. `@EnableMethodSecurity` + `@PreAuthorize("hasAuthority('PERM_masters.read')")` where useful.
- Issuer/audience/JWKS from config (§7 file list), **not** hardcoded; `jwt` decoder `NimbusJwtDecoder.withIssuerLocation` + audience validator.

### 4.4 RBAC + facility/department/store scope claims

| Claim | Meaning | Source (legacy) | Enforcement use |
|---|---|---|---|
| `sub` | IdP subject (= `T_USERDETAILS.ID` mapped at provisioning; or stable staff id if preferred) | T_USERDETAILS.ID | identity, audit `ADDEDBY/UPDATEDBY` |
| `preferred_username` | login name | LOGINNAME / SAMACCOUNTNAME | display, support |
| `hims:userId` | legacy numeric user id (kept for back-compat joins/audit) | T_USERDETAILS.ID | audit stamps, MDC `u` |
| `roles` (realm) | role codes | T_USERROLEMASTER.CODE via T_USERROLES | `ROLE_*` authorities |
| `hims:roleDetails` | `[{roleId, roleCode, unitId, isDefault, flags(clinical/nurse/supplier), levelId}]` | T_USERROLES × T_USERROLEMASTER | per-unit role resolution, role-switch UI |
| `hims:units` | `[{unitId, isDefault}]` | T_USERUNITDETAILS | facility scope; `MBase.UnitID` stamping |
| `hims:defaultUnitId` | default facility | ISDEFAULT | default scope when caller omits unitId |
| `hims:stores` | `[storeId]` | T_USERSTOREDETAILS | inventory/pharmacy scope checks |
| `hims:wards` | `[wardId]` | T_USERWARDDETAILS + T_NurseWardDetails | nursing scope checks |
| `hims:departmentId` | primary dept | M_DOCTORDEPARTMENTDETAILS (derived) | department scoping |
| `hims:employeeId` / `hims:doctorId` / `hims:staffNo` | staff identifiers | T_USERDETAILS / M_STAFFMASTER / M_DOCTORMASTER | audit attribution |
| `hims:permissions` | enabled permission paths | t_role_enabledpermission → M_ModulePermissionMaster | `PERM_*` authorities |

- Token size guard: only *enforceable* permissions are emitted; the full UI menu list stays out of the service token (frontend can call a `/me/ui-permissions` endpoint if it still needs menu gating during W1–W3).
- Role-flags (`isNurseRole`, `isClinicalRole`, `issupplier`, `levelId`, `BI_ROLE_NAME`) become claims so services can do attribute-based checks without IAM round-trips.

### 4.5 Audit attribution (who / what / unit)

- Target keeps the legacy `T_AUDIT`-style envelope (`EVENTID, EVENTDESCRIPTION, PATIENTID/DOCTORID/VISITID/... , STOREID, WARDID, ITEMS, TOKEN, REMOTEADDRESS` + actor/unit columns) — see `Audit.java` and `MBase` stamps.
- **Who**: JWT `hims:userId` + `preferred_username` → `ADDEDBY/UPDATEDBY`; **What**: event code + domain refs; **Unit**: `hims:defaultUnitId` and effective unit per request; store/ward from claims.
- Implement an `AuditPrincipal` filter/interceptor that seeds an `AuditContext` from the token (mirrors `UserTrxContextHolder` + `AuditHandler` pattern) so services stamp events without DB lookups; `CorrelationIdFilter` already gives a per-request correlation id to join audit rows end-to-end.

### 4.6 Secret management direction

- Everything secret is environment-injected (pattern already used for `DB_PASSWORD`, `KAFKA_BOOTSTRAP`): `ISSUER_URI`, `JWKS_URI`, `HIMS_AUDIENCE`, IdP admin/service-account creds.
- `platform/.env.example` documents keys; no committed values (audit today for legacy-committed secrets listed in §2.5/2.6 — treat as **compromised on cutover**: rotate `secretTokenForOnlineAppointment`, LDAP bind password, RISL SSO REST password, unit `apiToken`s).
- Direction: dev = `.env`; prod = platform secret store/k8s secrets or HashiCorp Vault; IdP admin sealed by realm owner. HS256 symmetric keys rejected for inter-service tokens (RS256/ES256 with per-env keysets).

---

## 5. IdP evaluation shortlist — DRAFT ADR

### 5.1 Criteria (weights: must-have vs nice-to-have)

| # | Criterion | Why it matters here |
|---|---|---|
| M1 | OIDC/OAuth2 conformance (Authorization Code + PKCE, client credentials, JWKS, refresh) | Non-negotiable protocol target (§4) |
| M2 | Token claim customization (custom claims per client, multi-valued claims) | `hims:units/stores/wards/permissions` mapping (§4.4) |
| M3 | User federation to LDAP/AD | Existing `ohum.healthcare` AD (10.10.5.71, ldap config present but inactive in legacy) |
| M4 | RBAC + groups with hierarchy (per-facility roles) | `unit × role` grants |
| M5 | Self-host/air-gap feasibility + HA story (active/active, external DB) | Government hospital network; offline-tolerant ops |
| M6 | Admin/self-service UX (user lifecycle, role assignment per unit) | IAM service staff operations; legacy had full user-mgmt UI |
| M7 | Data residency/sovereignty and procurement (OSS vs SaaS, Indian gov health data) | State health data; public-cloud data-residency posture |
| M8 | Effort/ops cost for a small platform team | Bootstrap + ongoing maintenance |
| N1 | SSO/SAML federation to RISL (Rajasthan SSO) | Whatever replaces RISL SSO integration must still interoperate |
| N2 | Audit event export (IdP login events) | Complement `T_AUDIT` with authentication events |
| N3 | Password policy + lockout + reset out-of-the-box | Replace legacy per-user policy columns |
| N4 | Standards support for future (SAML for other gov portals, SCIM for provisioning) | Long-term |

### 5.2 Shortlist

| Option | Type | Meets M1–M8 | Strengths | Risks / gaps |
|---|---|---|---|---|
| **Keycloak** (Red Hat build) | OSS, self-hosted | **All** | Full OIDC+SAML; realms; custom claim mappers; LDAP/AD federation; groups/roles hierarchy; self-host with external PG; huge community; RHSSO commercial support path; SCIM via extensions; IdP brokering (can federate RISL SSO later) | Requires ops care (2+ replicas, PG, backup); upgrade cadence; UI older | 
| **Dex** (CNCF) | OSS, lightweight connector hub | M1,M3,partial M2,M5; **fails M6** | Tiny footprint; connectors (LDAP, OIDC, SAML) | No admin console/user self-service; no built-in user store; claims mapping limited; not an IAM for hospital staff lifecycle — needs companion directory |
| **Azure AD/Entra ID** | Cloud SaaS | M1–M4,M6–M8 (if licensed); M5 conditional | Zero self-host ops; mature policies/conditional access; SCIM; claim mapping rich | Data residency for state health data; network dependency at remote sites; per-user licensing; vendor lock-in; gov procurement complexity |
| **Okta / Auth0** | Cloud SaaS | M1–M4,M6–M8 (if licensed) | Best-in-class UX/APIs; breach-history strong; SCIM | Same cloud-dependency/cost/residency concerns as Entra; no local air-gap |
| **AWS Cognito** | Cloud SaaS (AWS) | M1,M2,M4 partial,M6 partial; M3 LDAP weak | Cheap entry; good if already on AWS | Weaker AD federation; claim mapping simpler; data residency; AWS-only |

### 5.3 RECOMMENDATION

> **Adopt Keycloak (self-hosted, high-availability pair + external PostgreSQL) as the iHMS IdP**, with OIDC for all new services (§4) and LDAP federation to the existing `ohum.healthcare` AD for staff authentication. If/when the organization holds an approved public-cloud identity tenant (Entra ID/Okta) and connectivity to all facility sites is guaranteed, re-evaluate cost/residency trade-offs — the claim contract (§4.4/§6) is IdP-agnostic and this ADR's mapping survives a swap.

Rationale (vs shortlist): only Keycloak satisfies the must-haves including self-host/air-gap (M5), admin/self-service (M6) and OSS procurement for state health data (M7) with in-scope ops cost (M8); Dex lacks the user-lifecycle/admin half of IAM; cloud IdPs are architecturally clean but introduce data-residency, network, licensing, and procurement constraints that a state hospital network cannot take on for MIG-004 without an explicit infrastructure decision. Keycloak brokering also preserves the RISL SSO path (N1) during transition.

### 5.4 DRAFT ADR (for Agent 00 ratification)

```
ADR-XXXX (DRAFT) — iHMS IAM: OIDC/OAuth2 with self-hosted Keycloak
Status: DRAFT (proposed by AGT-05; awaiting ratification by Agent 00 / W0 gate)
Context:
  - Legacy auth: opaque Base64-UUID tokens in Redis; RBAC frontend-only (E-07);
    no-op password validator; RISL SSO + LDAP/AD exist but LDAP inactive; MIG-004 BLOCKED.
  - Reference-service already default-deny (IMP-DEC-006); bearer validation intentionally deferred.
Decision:
  1. All iHMS target services authenticate via OIDC/OAuth2 (JWT RS256, 15-min TTL,
     JWKS validation, audience hims-services). Interactive = auth-code + PKCE via BFF/gateway;
     M2M = client-credentials.
  2. Deploy Keycloak (2+ replicas, external PostgreSQL, per-env realms dev/stg/prd) as the IdP;
     federate ohum.healthcare AD for staff; keep RISL SSO behind an IdP broker during transition.
  3. Authorize server-side: roles → ROLE_*, permissions (M_ModulePermissionMaster paths)
     → PERM_* authorities; facility/dept/store/ward claims per §4.4; default-deny preserved.
  4. IAM service owns migrated T_USER* grant tables; IdP groups mirror (unit × role);
     UserCreated/RoleChanged events on Kafka. Legacy opaque-token/RISL flows terminate at
     gateway (MIG-005) which issues service-bound JWTs.
  5. Secrets: externalized only; rotate all legacy-committed secrets at cutover.
Consequences:
  + Stateless services, standardized protocol, server-side authz, audit attribution from claims.
  + IdP-agnostic claim contract → cloud IdP remains a drop-in if procurement/connectivity change.
  − Keycloak ops burden (HA, backups, upgrades) owned by platform team; realm/claim changes
    need IAM-service governance; token-size discipline needed (emit only enforceable perms).
Ratification required: Agent 00 (architecture owner) + W0 gate; file as IMP-DEC-<next> in
docs/modernization/implementation/DECISIONS.md once ratified (this report does not modify it).
```

---

## 6. Token compatibility design (legacy → claims → Spring Security context)

Goals: (a) services never change semantics because the token is now a JWT; (b) legacy `T_USERROLES`-style per-unit roles survive as claims; (c) the security context in reference-service exposes the same fields the legacy `UserView`/`User` provided to business code.

| Legacy concept (evidence: §2) | JWT claim(s) | Spring Security context | reference-service usage |
|---|---|---|---|
| `T_USERDETAILS.ID`, `LOGINNAME` | `sub` (=mapped IdP subject), `preferred_username`, `hims:userId` | `HimsPrincipal.userId/username` | audit `ADDEDBY/UPDATEDBY`, MDC `u` |
| `T_USERROLES` grant × `T_USERROLEMASTER.DESCRIPTION` (runtime role string), `CODE`, `LEVELID`, flags | `realm_access.roles` (`ROLE_<CODE>`), `hims:roleDetails[]` incl. `roleId/unitId/description/flags/levelId` | `ROLE_*` authorities + principal role model | coarse API gates, role-scoped behavior, reporting |
| `T_USERUNITDETAILS` membership + `ISDEFAULT`; `T_USERDETAILS.UNITID` | `hims:units[]`, `hims:defaultUnitId` | `HimsPrincipal.units/defaultUnitId` | `unitId` resolution for `MasterReadController` when param absent; `MBase.UnitID` scope stamps |
| `T_USERSTOREDETAILS` | `hims:stores[]` | `HimsPrincipal.storeIds` | inventory/pharmacy scope checks (future services) |
| `T_USERWARDDETAILS`, `T_NurseWardDetails` (derived at login) | `hims:wards[]` | `HimsPrincipal.wardIds` | nursing workflows (future services) |
| department/room derived at login | `hims:departmentId`, `hims:roomId` (where token workflow) | `HimsPrincipal.departmentId` | department-scoped queries |
| `t_role_enabledpermission` → `M_ModulePermissionMaster.path` (dotted, parents auto-derived) | `hims:permissions[]` | `PERM_*` authorities (`JwtGrantedAuthoritiesConverter`) | `@PreAuthorize` / `requestMatchers` enforcement — **server-side, unlike legacy** |
| `T_USERDETAILS.EMPLOYEEID/DOCTORID`, `M_STAFFMASTER.STAFFNO`, `M_DOCTORMASTER` flags | `hims:employeeId`, `hims:doctorId`, `hims:staffNo`, `hims:isNurse/isResident/isHod/...` | principal attributes | audit, doctor/nurse-specific flows |
| token TTL/single-session (legacy cache) | JWT `exp` (15 min) + IdP session/refresh; revocation via IdP | n/a (stateless) | IdP logout → 401 automatically on expiry; gateway may keep short-lived opaque shim during transition |
| legacy `__token` cookie / `token` header | replaced by `Authorization: Bearer` at gateway | n/a | gateway (MIG-005) maps old headers to bearer for legacy clients |

**Security-context adapter** (target, in reference-service): `JwtAuthenticationConverter` parses the above claims into `HimsPrincipal` (implements `@AuthenticationPrincipal` contract). Code that today would read `UserView.getUser().getUnitId()` reads `HimsPrincipal.defaultUnitId()` — one seam, MIG-004-scoped. Permissions are converted to authorities **before** filter-chain authorization so `requestMatchers` and `@PreAuthorize` work natively.

**Password/session migration note**: legacy password hashes are **not reusable** (no-op validator, DB-side raw compares, MDM obfuscation). MIG-004 must force password reset (or adopt LDAP/AD as authoritative for staff). Single-session policy moves to IdP (offline sessions / revocation) — `ALLOWED_MULTI_LOGIN_USERNAMES` exceptions become per-user or per-role IdP policy.

---

## 7. What must change in reference-service at MIG-004 (concrete file-level list)

All paths under `services/reference-service/`. No changes now — this is the MIG-004 execution list (dependency-neutral, ready to run once IdP/legacy environment exists).

| # | File | Change | Why |
|---|---|---|---|
| 1 | `pom.xml` | Add `spring-boot-starter-oauth2-resource-server` (brings `spring-security-oauth2-jose`) | JWT RS256 validation via Auto-config | 
| 2 | `src/main/java/com/hims/reference/configuration/SecurityConfig.java` | Replace "any authenticated principal passes" interim with `oauth2ResourceServer(jwt(...))` (issuer/JWKS config), add `accessDeniedHandler` → JSON 403, entry point → JSON 401 envelope; add `requestMatchers` grants (`PERM_masters.read` etc.); keep `permitAll` list unchanged | Enforce §4.3; preserve default-deny |
| 3 | `src/main/java/com/hims/reference/configuration/JwtGrantedAuthoritiesConverter.java` (new) | Claims → `ROLE_*`/`PERM_*` authorities incl. permission-path normalization; wire into `JwtAuthenticationConverter` | §4.4/§6 mapping |
| 4 | `src/main/java/com/hims/reference/security/HimsPrincipal.java` (new) | Immutable principal: userId, username, defaultUnitId, units[], storeIds[], wardIds[], dept, permissions | Business-facing seam replacing `UserView`-style reads |
| 5 | `src/main/java/com/hims/reference/security/AuditPrincipalFilter.java` (new) | Extract principal + correlation id into request-scoped `AuditContext` (who/unit/store/ward) for audit stamping | §4.5 attribution |
| 6 | `src/main/java/com/hims/reference/api/MasterReadController.java` | Resolve `unitId` from `HimsPrincipal.defaultUnitId()` when the `@RequestParam unitId` is absent (legacy derived from session); validate requested unit ∈ principal units (else 403/400) | Scope containment at the API seam (params already documented as IAM-supplied) |
| 7 | `src/main/java/com/hims/reference/application/MasterReadUseCase.java` + `infrastructure/persistence/JpaMasterReadRepository.java` | Thread the effective unit scope from principal (already unit-scoped; make it principal-forced, not caller-optional, for scoped endpoints except `all`) | Prevent cross-facility reads |
| 8 | `src/main/java/com/hims/reference/api/GlobalExceptionHandler.java` | Add `AccessDeniedException` handler → 403 `ApiError` envelope; align 401 body shape (envelope vs today's empty `sendError`); keep existing 400/404/500 handlers | Contract consistency for gateway/UI |
| 9 | `src/main/resources/application.yml` | Add `spring.security.oauth2.resourceserver.jwt.issuer-uri`, `jwk-set-uri`, `audiences` via env (`HIMS_ISSUER_URI`, `HIMS_JWKS_URI`, `HIMS_AUDIENCE`) | IdP-agnostic config, secret-free |
| 10 | `src/test/java/com/hims/reference/api/MasterReadControllerTest.java` | Keep no-token→401 test; add `@WithMockJwtAuthentication`/mock-converter cases: valid token w/o unit claim → default-unit scoping; token unit={1} requesting unit=2 → 403/400; wrong `PERM` → 403 | Pin new behavior; legacy-shape tests unchanged |
| 11 | (unchanged) `CorrelationIdFilter.java`, `ReferenceServiceApplication.java`, `Dockerfile` | — | Corr-id is auth-agnostic; app may add `@EnableMethodSecurity` when `@PreAuthorize` is used |

Dependencies on other agents: MIG-005 (gateway) maps legacy `token` header/RISL-SSO → OIDC bearer; MIG-001 (BLK-001) provides the live DDL for `T_USERROLES` backfill validation; platform (`AGT-08`) stands up Keycloak in compose for dev; Agent 00 ratifies the IdP ADR (§5.4).

---

## 8. Open questions / risks (for MIG-004 unblocking)

1. **IdP ratification**: self-hosted Keycloak vs cloud tenant — needs Agent 00 + infrastructure owner (§5). Claim contract is swap-safe.
2. **RISL SSO (Rajasthan)**: keep as identity source (IdP broker) or replace entirely? Affects `sAMAccountName` linkage and password-reset scope.
3. **Password estate**: legacy hashes unusable → forced reset or AD-authoritative; confirm staff AD coverage before cutover.
4. **`T_USERROLES`/`T_USERROLEMASTER` live DDL** (BLK-001) to validate composite keys, status columns, `DEFAULTROUTE` — affects provisioning mapping and backfill.
5. **Provisioning ownership**: IAM service consumes `T_USER*`; per-unit role assignment UX parity with legacy `AddUserServiceImpl` flows (incl. `t_role_enabledpermission` tree editor).
6. **Permission-path inventory hygiene**: >2,000 dotted UI paths vs ~enforceable API set — define the authorized "server-enforced" subset to keep tokens small.
7. **Legacy bypasses to retire**: `secretTokenForOnlineAppointment`, `test.user.internal.001` backdoor, `/int/*` ROLE_INTEGRATION shortcut, fake user id=1 for `@Login` endpoints — each needs a target equivalent or documented removal.
8. **Audit continuity**: reconcile `T_AUDIT` service-authored events with IdP-delivered auth events (login/logout/failed login) in one log model.

---

## 9. Evidence index (read-only, `/mnt/d/ihms` + target repo)

**Legacy entities**
- `src/main/java/com/ohum/models/masters/TUserdetails.java` (`T_USERDETAILS`)
- `src/main/java/com/ohum/models/masters/TUserrolemaster.java` (`T_USERROLEMASTER`)
- `src/main/java/com/ohum/models/masters/TUserunitdetails.java` (`T_USERUNITDETAILS`)
- `src/main/java/com/ohum/models/masters/TUserStoreDetails.java` (`T_UserStoreDetails`)
- `src/main/java/com/ohum/models/masters/TUserWardDetails.java` (`T_UserWardDetails`)
- `src/main/java/com/ohum/models/masters/Menumaster.java` (`M_MENUMASTER`), `MenumasterOld.java`
- `src/main/java/com/ohum/models/masters/ModulePermissionMaster.java` (`M_ModulePermissionMaster`)
- `src/main/java/com/ohum/models/RoleBasedPermission.java` (`t_role_enabledpermission`)
- `src/main/java/com/ohum/models/MBase.java`, `LBase.java` (tenancy/audit base; `beforeCreateUpdateCallback`)
- `src/main/java/com/ohum/models/Audit.java` (`T_AUDIT`); `src/main/java/com/ohum/models/User.java`, `src/main/java/com/ohum/views/UserView.java`

**Auth flows / tokens / passwords**
- `src/main/java/com/ohum/util/auth/AuthenticationInterface.java` (`getNewEncodedToken` = Base64(UUID))
- `src/main/java/com/ohum/util/auth/DefaultAuthenticationImpl.java` (login query, lockout, test-user bypass, passwordManager, loginViewUtilFunction: role/stores/wards/permissions/token cache)
- `src/main/java/com/ohum/util/auth/PasswordValidator.java` (no-op `attempt` → `return true`)
- `src/main/java/com/ohum/util/auth/LDAPBasedRestrictivePasswordValidator.java` (external PW-manager REST)
- `src/main/java/com/ohum/aspects/IntegrationOperationsFilter.java` (token header/param → cache → ThreadLocal; `secretTokenForOnlineAppointment` bypass)
- `src/main/java/com/ohum/aspects/ServiceLayerAspect.java` (view==null → 401; `@Login`/`@NotSecure` open; permission check commented out)
- `src/main/java/com/ohum/interceptors/sso/SSOAuthFilter.java` (RISL SSO; `__token` cookie; unitWiseRoles)
- `src/main/java/com/ohum/services/AuthenticationServiceImpl.java` (`/auth/*` endpoints incl. `/loginb` stub, `/getuserrole`)
- `src/main/java/com/ohum/services/custom/AddUserServiceImpl.java` (t_userroles insert `(userid,roleid,unitid)`; store/unit/perm maintenance)
- `src/main/java/com/ohum/services/BaseServiceImpl.java` (`checkRolePremission` SQL)
- `application.properties` (session.timeout=30, auth.pwd.manager=password, risl.sso.*, ldap.*, secretTokenForOnlineAppointment, allowSingleSessionPerUser, spring.redis.*, ldap.restrictive.handler.url)
- `src/main/java/com/ohum/SpringSecurityConfig.java` (inert `/**` permitAll)

**Stored procedures (login/permissions)**
- `generated-procs/CIMS_LOGINNAMEEXISTSDOCIT_IMPROVED_ROLE_NEW.sql` (DB-side password compare; 3-strike lockout; defaultRoute)
- `generated-procs/CIMS_GETUSERDETAILSANDPERMISSIONS.sql` (canonical user⋈roles⋈rolemaster⋈unitdetails join; stores; clearance authority)

**Target (read-only review)**
- `services/reference-service/src/main/java/com/hims/reference/configuration/SecurityConfig.java`
- `services/reference-service/src/test/java/com/hims/reference/api/MasterReadControllerTest.java` (401 pinned)
- `services/reference-service/pom.xml` (Boot 3.5.3, starter-security), `src/main/resources/application.yml`
- `services/reference-service/src/main/java/com/hims/reference/api/MasterReadController.java` (unitId param IAM note), `GlobalExceptionHandler.java`, `infrastructure/web/CorrelationIdFilter.java`
- `docs/modernization/08-security-rbac-audit.md` (Agent 08 AS-IS), `docs/modernization/implementation/DECISIONS.md` (IMP-DEC-006), `docs/modernization/MIGRATION-WAVES.md` (§2.2 IAM service 22), `docs/modernization/implementation/{BLOCKERS,MIGRATION-TRACKER}.md` (MIG-004 BLOCKED, S0 security green)
