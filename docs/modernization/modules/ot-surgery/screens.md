# M12 — Screens & Routes

## Screens (schemas, 14-key config)
surgeryScheduleSchema · addNewSurgerySchema · otscheduledetails · addOTCharge · addOTBillingSchema · AddPatientProcedureRateDetailsSchema · procedurePerformanceSchema · SurgeryOrderList · surgeryheads · addPatientProcedureRateSchema · addProcSchema · addSurgHeadSchema · itemCategorySchemaForOT · notesCollectionSchema · patientProcedureRateDetailsSchema · showSurgeryOrderList.

## Routes (10, 03 §2.11)
`ot:surgList` · `surgSchedule` · `procPerformance` · `otSchedule/:otId/:tableId` · `otScheduleEvent` · `patMovementAudit` · `nurseScheduleEvent` · `itemDetailsEvent` · `surgSchdule/:otId/:tableId` · `addNewSurg` · `surgDetails`.

## Key endpoints (03 §2.11)
- `otscheduler/*` (79: schedules, events, procedures, patient procedure rates), `otconfig/*` (35), `anesthesianotes`, `surgerynotes`, `master/filterDiagnosis`.

## Client controller
- operation-theatre.js (324 KB).
