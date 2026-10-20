# M15 — Security

- roleLab docit role (docit-roles.js) gates lab worklist UI.
- **Four-eye principle is role-separated:** sample collection (`Investigation.SampleCollection.Collect`) → resulting (technician) → review (`Investigation.ResultReviewConfirm`) → finalize (`Investigation.ResultFinalize`) — frontend-gated only (08 §3.1).
- Critical alerts route to defined destinations (criticalAlertResultSchema); no server-side role verification.
- Sample-level audit: SAMPLE*.CONFIRM/RELEASE events in T_TRX_HISTORY (08 §5.1).
- Report release is the authoritative action — must be server-enforced in target.

## Migration notes
- Enforce review/release role separation server-side (08 §10.1); keep report-release as an auditable state transition.
