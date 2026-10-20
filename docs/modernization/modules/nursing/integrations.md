# M09 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| tiuxml forms engine (M40) | In | 368 forms rendered in patient record (History/Exam/Assessment per specialty, nursing docs, scales, checklists) |
| EJS (outreach) | Out | outreach_data_schema; EjsDetailsServiceImpl:2105 (outreach registration → Solr) |
| CWF/SCWF | Both | clinical workflow task generation (cwfmapping, scwfmapping masters) |
| ABDM EHR sync | Out | syncEHR of notes (02 §M09) |
| Solr | In-process | healthfactor + icd10cm collections (autocomplete), encounter collection (worklists), patient collection (coversheets) |

## Solr detail (07 §1.2)
- icd10cm collection: ~fields icdcode, short/long description, local language, alias, parent, category, coding system; flag-based drainer `syncIcd10cmToSolrJob` (5s).
- healthfactor collection: code, description, category, local lang, SNOMED display; drainer `syncHealthFactorToSolrJob` (5s).

## Redis
- `current_enc_` mirrors; master caches (vitalmaster, notetitleMaster etc.).
