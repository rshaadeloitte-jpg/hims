# M09 — Security

- Nurse role flags: `isNurseRole`, `isClinicalRole` (T_USERROLEMASTER) alter behavior (08 §4.1); role-name string checks in JS are brittle (misspellings evidenced).
- docit-roles.js: roleNurse, roleNurseIncharge, roleNursePatientView, roleLab, roleRadiology etc. gate route visibility only — **no backend enforcement** (08 §3.1).
- Note signing (signNotesFromTask/signTemplate) is the closest thing to attestation; no digital signature verified server-side.
- Incident severity/status; T_TRX_HISTORY events for clinical trail.
- Nursing keys (26 `data-roleM`) gate UI: e.g. `Nursing.*` screens.

## Migration notes
- Nursing god-class must decompose into note/vitals/task/allocation services; server-side authZ for documentation flows (08 §10.1); preserve note-signing as attestation with audit.
