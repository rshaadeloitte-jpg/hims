# M09 — Workflows

## Ward workflow (02 §M09 Workflow)
1. Ward roster → patient allocation (nurse→ward/doctor/patient; allied→clinic).
2. Worklist (orders/vitals/notes/tasks) — task-driven; CWF/SCWF task generation.
3. Documentation: notes (initial assessment → progress → procedure → addendum), plan of care (problems/goals), charting (vitals, growth charts).
4. Shift handover (HANDOFF_COMMUNICATION, Telephonic_Handover, multiShiftHandoffReport).
5. Incidents → resolution (incidentResolveSchema); notifiable disease reports.

## Notes engine (03 §2.4)
- TIU-XML forms via `tiuxml.js` + `tiuxml.json` (219 KB) + `tiuxml_mapping.json`; note tiles `csNoteTiles.html`, `tilesHF.html`, `noteSummary.html`.
- Patient notes routes carry `:patientId/:admId/:patientIen/:visitIen/:noteIen/:noteTitleId`.
- Note signing: signNotesFromTask / signTemplate (signed status).

## BCMA (M30) handoff
- `bcma:unitDose`, `bcma:scanWristband/:patientMrn` — bedside med scanning with html5-qrcode.

## Solr touchpoints
- icd10cm search (searchAllIcd10cmInSolr — diagnosis/complaints R*/OHC.* filter), healthfactor autocomplete (searchHealthfactor), encounter worklists, patient coversheet lookups.
