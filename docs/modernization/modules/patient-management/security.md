# M01 — Security (from 08-security-rbac-audit.md)

- **Auth model:** opaque token (Base64 UUID) in cache; `IntegrationOperationsFilter` validates token per request; `ServiceLayerAspect` returns 401 when absent. **No per-API role check on backend** — permission check commented out (ServiceLayerAspect:421-426).
- **PHI:** `IdEncryptionUtil` optional field encryption (disabled by default: `phi.encryption.enabled=false`).
- **Bypass risk relevant to registration:** `secretTokenForOnlineAppointment` authenticates as user id=1 (kiosk/e-Mitra registration path) and is shipped in browser `fjs-config.json` (08 §9 risk 3).
- **Audit:** MBase stamps ADDEDBY/ADDEDDATETIME etc. from session user; registration events in T_TRX_HISTORY (e.g. `CREATE.VISIT`).
- **Frontend gating:** `data-roleM` permission keys — registration screens gated via `Configuration.Master.*` family (1,424 keys) and role menus.

## Target-architecture notes
- New IAM (22. Security & IAM) issues JWT; registration must re-enforce server-side authorization (08 §10.1).
- Patient identity becomes read-model for all other services via `PatientRegistered` event (10 §2.2).
