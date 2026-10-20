# M02 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| Kiosk (M31) | In | selfAppointment / appointmentSearch kiosk routes; `secretTokenForOnlineAppointment` bypass |
| ABDM (ABHA) | Both | ABHA registration in OP (abhaPatientRegistration, childAbhaPatientRegistration); encounter→Solr for consent matching |
| NHNC | Out | walkinVisitNhnc; appointmentListNhnc; NHNC variant forms (op_tiuxml_mapping.json) |
| Telemedicine | Out | createTeleconsultRoom; telemedicine dept/unit/speciality lists |
| e-Mitra | Both | online appointment booking (EmitraServiceHelper:128-212); e-Mitra redirect filter |
| Jeevanvahini (108) | Both | emergency case push/fetch |
| RGHS/SIMS | Out | patientRghsPackageDetails, eligibility check button (`getPolicies`) |

## Solr (07 §1.2/1.3)
- **encounter collection** realtime (PatientRegApptServiceImpl:986,1299; EmergencyAndWalkinVisitServiceImpl:1795-1798,3953-4145); `searchPatientInEncouter` (unit-scoped: `unitId_l`, `isAdmission_b`, `opdNo_s`); `searchPatientInEncounterForCheckinCheckout` (cabin/department/doctor/date filters); `getAppListSearchFilters/TypeFilter` (appointment token lists).
- **Retention:** docs deleted after `solr.encounter.retain.days=3` — old visits unreachable via search (design risk, 07 §1.6).

## Redis (07 §2.1)
- `current_enc_<encId_E/A>` (100s); QMS token queues (`pushToQ/removeFromQ`, `<qName>_S` counters) — live queue state, touch-to-day-end.
