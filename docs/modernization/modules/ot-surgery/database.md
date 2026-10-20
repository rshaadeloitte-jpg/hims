# M12 — Database (05 §3, 10 §2.1 row 11)

## Tables
| Table | Notes |
|---|---|
| T_OTSCHEDULE (+DETAILS) | Surgery schedules |
| T_SURGERYDETAILS | Surgery records |
| T_ANESTHESIANOTES | Anesthesia notes |
| T_SURGCHARGES | Surgery charges |
| PatientBillEstimationSurgeryHead | OT billing estimation |
| PatientProcedureAnesthesiadetails | Procedure anesthesia details |
| PatientProcedureRateDetails | Procedure rates (tariff) |
| PatientScheduleAnasthesiaDetails | Scheduled anesthesia |
| Patientprocedurechecklistitems/detailsDto | Checklist execution |
| masters/ot (~20) | ottheatremaster, ottablemaster, proceduremaster, procedurechecklist*, anesthesiatypemaster, anesthesianotes, surgerynotes, surgeryheadmaster(+details), otschedule, operationresultmaster, operationstatusmaster, intra/post/preoperativeinstructionsmaster, procedurecategorymaster, proceduresubcategorymaster, proceduretypemaster, procedureequipmentdetails, procedureitemdetails |

## Stored procedures
- None directly evidenced; charges flow through CIMS_ADDCHARGE family (M04).

## Migration notes (10 §2.1 row 11)
- Target owns T_OTSCHEDULE(+DETAILS), T_SURGERYDETAILS, procedure masters, T_ANESTHESIANOTES.
- Emits `ProcedureScheduled`, `SurgeryCompleted`; consumes OrderCreated (procedure) + PatientAdmitted; emits Inventory consumption (OT items) and Billing (pre-billing charges).
