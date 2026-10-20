# M16 — Workflows

## Imaging workflow (02 §M16, derived from M15 pattern)
1. Order (clinical/billing) → rad order booking auto-created at billing (BR-BILL-003: analog of patho — `IsImageCaptured=false` detail rows, AddServiceOrderServiceHelper:579-599).
2. Queue (radiologyQueue) → acquisition → image capture (addRadImg / showRadSampleImg).
3. Reporting: template-based (radtemplatemaster/radtemplateresult) → report entry → report details.
4. Retest / reschedule (radRetestSchema, radRescheduleSchema).
5. Report print / release (showRadiologyReport, printReport).

## Charge-removal lock (BR-PATH-001)
- Rad charge with accepted sample cannot be removed → LEX001 unless `autoChargeCancellationOnRefund` (same rule as patho — evidence AddServiceOrderServiceHelper:3693-3862).

## Status lifecycle
- Rad order status → report status; UNKNOWN-20 code lists (02 §M16).
