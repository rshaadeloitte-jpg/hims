# M07 — Security

- Pharmacist/verifier roles (verify vs dispense split in UI: pharmacyOrderVerify / pharmacyOrderDispense).
- Store-scoped users: `userstoreIds` + `crr_Default_Store_id` — pharmacy data access is store-scoped.
- No backend role enforcement (08 §3.1); `Pharmacy.*` keys (18) gate UI.
- Label/print endpoints (printMedLabel etc.) return raw HTML — same API tier exposure pattern (04 §9.7).
- BCMA handoff adds administering-nurse identity context (M30).

## Migration notes
- Enforce verify/dispense role separation server-side in target (08 §10.1); keep store-scoping in IAM grants (per-store grants exist in model: T_USERSTOREDETAILS).
