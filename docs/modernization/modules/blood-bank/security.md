# M17 — Security

- Blood bank technician/officer roles; discard requires reason master (blooddiscardreasonmaster).
- No backend role enforcement (08 §3.1); frontend gating via bloodConfigurationMasters keys.
- e-Raktkosh calls carry no per-request user context (scheduled jobs).
- Audit: BLOODBAG.* events in T_TRX_HISTORY (08 §5.1); MBase stamps on bag/donor writes.
- Donor PII (medical exam results, screening) — PHI-encryptable via IdEncryptionUtil (disabled by default).

## Migration notes
- Enforce officer/technician role separation server-side; keep discard reasons as reference data; protect donor medical data.
