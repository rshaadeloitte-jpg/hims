# M12 — Security

- Surgeon/anesthetist roles; OT schedule conflict checks (server-side validation, not authZ).
- OT schedule operations accessible to any authenticated token (08 §3.1 — no role check).
- otConfiguration master keys gate config UI; otSchedule masters in Configuration family.
- Audit: surgery/schedule status changes flow through T_TRX_HISTORY events (TRANSFER.BED adjacency, procedure events).

## Migration notes
- Enforce surgeon/anesthetist role separation server-side (08 §10.1); keep scheduling conflict checks as service invariants.
