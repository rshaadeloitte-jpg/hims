# M03 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| HIS admission request | Out | addAdmissionRequestToHIS (inter-hospital admission request, BR-DIS-001) |
| IPCR | Out | patientIPCRSchema (IPCR documentation) |
| RGHS | Out | patientRghsPackageDetails; TID flows (showTidListSchema) |
| Chiranjeevi Yojana | Out | INSURANCESCHEMEID=2 flows on admission/discharge |
| Corona tracking | Out | ipPatientCoronaDetailsSchema; COVID referral on transfer (BR-DIS-001) |
| Blood bank | In | patientrequisition (M17); Solr index on requisition |
| Mortuary/death | In | getMortuaryPatients, death forms (M28) |

## Solr (07 §1.2)
- encounter/admission index: `indexAmissionInSolr` with id `<admId>_A`; discharge clears encounter docs; death → partial patient update `death=true`.
- BedReservationServiceImpl:165 indexes encounters on reservation.

## Redis (07 §2.1)
- `CDD_<opdipdid>` discharge lock (60s); `current_enc_<admId>_A` mirror (100s).
