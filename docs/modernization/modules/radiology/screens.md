# M16 — Screens & Routes

## Screens (schemas, 9-key config)
radOrderBookingSchema · radReportSchema · radReportDetailsSchema · radRescheduleSchema · radRetestSchema · patientFilterRadSchema · getRadiologyReports · addRadImg · radFilterSchema.

## Routes (4, 03 §2.6)
- `radiology:radlab(/:p_id/:order_no/:date/:opd_ipd)`
- `radiology:printReport`
- `radiology:reSchedule`
- `radiology:radReTest`

## Templates
radioFinalizeTemplate.html · showRadiologyReport.html · pathoRadioTemplate (shared).

## Key endpoints
- `radiology/*` (12), `reports/*` (getRadiologyReports), `serviceOrder/*` (rad order creation at billing).

## Role
- roleRadiology (docit-roles.js) — radiology worklist role.
