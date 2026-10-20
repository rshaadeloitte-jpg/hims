# WP-1 — IAM service implementation report
_Worker: iam-implementer · Scope: GAP-REGISTER WP-1 (Catalog #22, Security & IAM, Wave 0)_

## What was implemented
`services/iam` was converted from a non-functional stub into a real Spring Boot 3.5 REST
microservice following the `grievance-service` structural template exactly (same layering,
ApiError envelope, GlobalExceptionHandler, CorrelationIdFilter, SecurityConfig/DevSecurityConfig,
Flyway + PostgreSQL conventions).

### Authentication (JWT HS256, server-side secret)
- `POST /api/v1/auth/login` — verifies BCrypt password hash against `t_userdetails`, issues an
  HS256 access token signed with the server-side secret (`iam.jwt.secret`, env-injected;
  default is a dev-only value). Claims: sub=username, uid, roles[], unit. TTL 30 min by default,
  mirroring the legacy Redis session TTL (catalog §22 / 08 §2.4).
- `POST /api/v1/auth/logout` — adds the bearer token (SHA-256 hashed) to a revoke list.
  `TokenRevocationService` interface with in-memory implementation (explicitly allowed by WP-1:
  "token revoke list, in-memory acceptable w/ interface"); swap to Redis at MIG-00x.
- `POST /api/v1/auth/validate` — introspects signature, expiry and revocation; returns
  `{valid, username, userId, roles, expiresAt, reason}`.
- `JwtAuthenticationFilter` populates the security context from the bearer token; everything
  except `/login`, `/validate`, actuator health and swagger docs is **default-deny**.

### User CRUD & role management
- `GET|POST /api/v1/users`, `GET|PUT /api/v1/users/{id}`, `PUT /api/v1/users/{id}/roles`
  (replace-grants PUT semantics; removed grants soft-deactivated).
- `GET|POST /api/v1/roles`.
- Passwords are BCrypt-hashed server-side and never returned (`UserView` projection).

## Files added / changed (services/iam unless noted)
Changed:
- `pom.xml` — rewritten mirroring grievance-service parent 3.5.3 / Java 21: web, data-jpa,
  security, validation, actuator, flyway (+postgresql flavor), postgresql runtime, springdoc,
  spring-boot-starter-test, spring-security-test, testcontainers; added jjwt-api/impl/jackson
  0.12.6 for JWT (no sibling had a JWT lib, so jjwt chosen as the mainstream option).
- `src/main/java/com/hims/iam/IamServiceApplication.java` — kept/adapted.
- `config/keycloak.yaml` — kept unchanged (future Keycloak federation reference only; not wired).

Deleted (dead stub code that could not compile on Boot 3):
- `IamService.java` (nulls/TODOs), `config/KeycloakSecurityConfig.java` (Keycloak adapter API),
  old javax.persistence domain models (`User`, `Role`, `Permission`), `application.properties`.

Added (layered like siblings):
- `api/`: `AuthController`, `UserController`, `RoleController`, `ApiError`,
  `GlobalExceptionHandler`, `dto/{AuthDtos,UserDtos,RoleDtos}`.
- `application/`: `AuthService`, `UserService`, `RoleService`;
  `exceptions/{InvalidCredentialsException,ResourceNotFoundException,InvalidRequestException}`.
- `configuration/`: `SecurityConfig` (!dev, JWT bearer + default-deny + BCrypt encoder),
  `DevSecurityConfig` (dev profile permit-all for first-admin bootstrap, IMP-DEC-019 contract).
- `domain/model/`: `UserDetail` (t_userdetails), `UserRoleMaster` (t_userrolemaster),
  `UserRole` (t_userroles) — BIGSERIAL ids, unit_id, audit cols per register/sibling conventions.
- `domain/repository/`: `UserDetailRepository`, `UserRoleMasterRepository`, `UserRoleRepository`.
- `security/`: `JwtProperties` (@ConfigurationProperties iam.jwt.*), `JwtTokenService`,
  `TokenRevocationService`, `InMemoryTokenRevocationService`, `JwtAuthenticationFilter`.
- `infrastructure/web/CorrelationIdFilter.java` (identical sibling contract).
- `resources/application.yml` (port 8080, ihms_iam schema/flyway config), `application-dev.yml`,
  `db/migration/V1__init_ihms_iam.sql` (schema ihms_iam; t_userdetails, t_userrolemaster,
  t_userroles; seeded admin/staff/patient roles).
Tests (`src/test/java/com/hims/iam/...`): `security/JwtTokenServiceTest` (5),
`application/AuthServiceTest` (10), `api/AuthControllerTest` (6), `api/UserControllerTest` (6),
`api/RoleControllerTest` (3) — MockMvc slices importing the REAL security chain (default-deny
verified) — plus `infrastructure/persistence/IamServiceIntegrationTest` (2, Testcontainers PG16,
executed on real Docker when available). Other: new `openapi/iam-service.yaml` (7 paths, sibling
style); surefire argLine added to pom per overseer review so the integration slice always runs.

## Build & test output
`mvn -f services/iam/pom.xml test` → **BUILD SUCCESS, exit code 0**.

    Tests run: 32, Failures: 0, Errors: 0, Skipped: 0   ← ALL tests execute

Breakdown:
- Unit tests (30): `security/JwtTokenServiceTest` (5), `application/AuthServiceTest` (10),
  `api/AuthControllerTest` (6), `api/UserControllerTest` (6), `api/RoleControllerTest` (3).
- Integration tests (2): `infrastructure/persistence/IamServiceIntegrationTest` — REAL PG16 via
  Testcontainers (`@ServiceConnection`, V1 migration applied on ihms_iam): verifies seeded
  baseline roles (admin/staff/patient) and that user creation persists a BIGSERIAL identity,
  BCrypt password hash and audit timestamps.
- Surefire note: the pom carries `<argLine>-Dapi.version=1.44</argLine>` on maven-surefire-plugin
  (same as appointment/patient-service poms); without it the container-based tests are silently
  skipped even when Docker is available. Added per implementation-overseer review; confirmed the
  integration slice executes (container started, 2/2 passed).

## Deviations from docs (with justification)
1. **Keycloak not integrated** — catalog §22 lists Keycloak/LDAP federation as external
   integrations; WP-1 requires "JWT access token (HS256, server-side secret)". Implemented
   self-issued JWTs; `config/keycloak.yaml` retained as reference for future federation work.
2. **Revoke list in-memory** — explicitly permitted by WP-1 ("in-memory acceptable w/
   interface"). Catalog targets Redis for sessions; interface ready for that swap.
3. **Legacy 12 auth endpoints** (`/auth/loginf`, `loginotp`, …) collapsed into the three
   canonical endpoints required by WP-1; OTP/LDAP paths deferred (LDAP inactive per catalog
   `ldap.auth.enabled=false`).
4. **No outbox/events** — UserCreated/RoleChanged are "proposed, no multi-consumer evidence"
   (catalog §22); WP-1 does not require events, so none emitted (consistent with grievance
   REST-only precedent).
5. **BIGSERIAL ids instead of explicit sequences** — GAP-REGISTER WP-1 explicitly specifies
   "(id BIGSERIAL, ...)" which overrides the sibling explicit-sequence convention; audit columns
   still mirror sibling conventions.
