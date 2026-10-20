# M16 — Features (02 §M16)

| Feature | Evidence |
|---|---|
| Order booking | radOrderBookingSchema; auto-creation at billing (BR-BILL-003, CIMS_ADDRADORDERBOOKING) |
| Radiology queue | radiologyQueue route (queue module); radFilterSchema |
| Result entry (report) | radReportSchema, radReportDetailsSchema |
| Template reports | radtemplatemaster, radtemplateresult masters; pathoRadioTemplate (shared template) |
| Retest/reschedule | radRetestSchema, radRescheduleSchema |
| Image handling | addRadImg, showRadSampleImg (PACS) |
| Test master | radtestmaster (+radtestcategory, radmodalitymaster) |

## Routes (4, 03 §2.6)
`radiology:radlab(/:p_id/:order_no/:date/:opd_ipd)` · `printReport` · `reSchedule` · `radReTest`.

## Templates
radioFinalizeTemplate.html, showRadiologyReport.html.
