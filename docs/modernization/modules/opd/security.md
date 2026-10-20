# M02 — Security

- Token-based auth per request (IntegrationOperationsFilter + ServiceLayerAspect 401 gate); **no backend role check** (08 §3.1).
- **Specific risk:** walk-in/kiosk path uses `secretTokenForOnlineAppointment` static token as user id=1 (08 §9 risk 3) — must be replaced with per-integration credentials.
- VisitStatus mutation is client-triggered, server-owned; server does not verify role before `updateDoctorCheckout`/`releaseCabin`.
- Session timeout 30 min sliding; single-session enforcement via `UNQ_SESS_USER_<userId>`.
- Frontend gating: Appointment keys (27) + QueueManagement (16) `data-roleM` keys; `Login.Clinician`.

## Migration notes (08 §10)
- Re-enable server-side authorization keyed on Role→ModulePermissionMaster (apiMethod+apiPath columns exist for this).
- Preserve `disableBackForwardButton` semantics for clinical screens (09 §2).
