# M03 — Security

- Token auth; bed allocation CAS `UPDATE ... WHERE OCCUPIED=false` is the concurrency control (BR-IPD-002).
- Discharge requires photo + discharge doc + feedback doc as a set (BR-DIS-001) — client-enforced; backend validates (BERB023/24/25).
- Discharge lock `CDD_<opdipdid>` (Redis 60s) prevents duplicate discharge; audit events ADD.DISCHARGE, TRANSFER.BED, MLC* in T_TRX_HISTORY (08 §5.1).
- No server-side role check — any valid token can call `/patientTransferDischarge/*` (08 §3.1).
- MBase audit stamping on all bed/admission writes.
- Frontend gating: Admission module keys (45 `data-roleM`).

## Migration notes
- Death/discharge permissions must be enforced server-side in target (08 §10.1); keep T_TRX_HISTORY semantics for clinical trail.
