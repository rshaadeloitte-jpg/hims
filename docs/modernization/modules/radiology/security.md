# M16 — Security

- roleRadiology docit role gates radiology worklist UI; frontend-gated only (08 §3.1).
- Report entry/release separation implied by role; no server-side enforcement.
- radiologyConfiguration master keys gate config UI (radCabinmaster etc.).

## Migration notes
- Enforce reporting/release role separation server-side; pin PACS access controls (image retrieval endpoints).
