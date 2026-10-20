# M09 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| NursingServiceImpl | `/nursingList` | **300 endpoints / 17,350 LOC** (04 §9.1 god class); worklists, allocation, notes, vitals, tasks, incident, shift handover, `saveMarkAsExpired`, `addChildDeathReview`, `deleteNursePatientAllocationOnShiftHandOver` |
| ClinicalServiceImpl | `/clinical` | getNoteContent, getProviderList, healthfactorMaster, saveAppErrors (4 endpoints) |
| NoteDetailsServiceImpl | `/notedetailsdata` | clinical notes data |
| ActionNotificationServiceImpl | `/actionNotif` | action notifications (CRUD) |
| CWFServiceImpl | `/cwf` | clinical workflow (task generation) |

## Client controllers (03 §1.1)
nursing-module.js · nursing-module-ext.js · nursing-note-module.js · nursing-charting-module.js · incident-reporting-module.js · bcma-module.js (M30).

## Notes
- 04 §9.1: NursingServiceImpl single file = single tx boundary = contention and unmaintainability — primary decomposition target.
