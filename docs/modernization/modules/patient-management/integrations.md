# M01 — Integrations (from 07-integrations-and-search.md §3, 02 §M01)

| Integration | Direction | Notes |
|---|---|---|
| ABDM/ABHA | Both | searchPatientForABDM, addPatientAbhNumberKey; ABHA create/link/re-KYC; consent requests; ABHA number → patientId Redis lookup (`ABHANUM_<healthIdNumber>`) |
| Aadhaar | Out | biometric auth + OTP (aadhar_biometric_auth.vm, aadhar_otp_auth_api.vm); RD device |
| Janaadhar / Bhamashah | Out | scheme identity lookup (janaadhar_*vm, bhamashah_api_request.vm) |
| Chiranjeevi Yojana | Out | getChiranjeeviDetailByJanAdhar; INSURANCESCHEMEID=2 flows |
| Solr | In-process | patient index + search (see workflows.md) |
| e-Mitra | Both | online appointment (kiosk); EmitraRedirectFilter |
| Jeevanvahini (108) | Both | emergency case push/fetch (PatientServiceImpl:277-281) |
| Mera Aspataal | Out | feedback window export (PatientServiceImpl:286-296) |
| RGHS/SIMS | Both | patientRghsPackageDetails; RGHS eligibility (M32) |

## Solr detail (07 §1.2)
- patient collection (~100 fields: names, civilid/passport/dlic/ration/janaadhar/pcts/ects/enrId, ABHA healthId+healthIdNumber+defaultHealthId, kin, occupation, blood group, insurance, unitId, address).
- Realtime on register/update; nightly backfill; stale-doc cleanup ≤15 hits.

## Redis detail (07 §2.1)
- `current_pat_<patientId>` / `current_pat_<mrno>` (100s TTL, Solr mirror), `ABHANUM_<healthIdNumber>`, `gender_prefix_maritalstatus_map`.
