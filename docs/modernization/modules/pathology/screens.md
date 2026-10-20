# M15 — Screens (schemas, 57-key config)

pathoOrderSchema · sampleCollectSchema · sampleRejectionSchema · resultingDetailSchema · resultReviewSchema · reportReleaseSchema · criticalAlertResultSchema · microbiology-schema · machineWiseResultReviewSchema · pathoGenerateBarCodeSchema · acceptReject(Filter/ReportDetails/Report)Schema · criticalAlert(Filter/ResultReportDetails/ResultReport/Result)Schema · getLaboratoryReports · microbiologyAntibiotic-schema · pathagencymaster · pathoFilterSchema · pathoGenerateBarCodeSchema(+ForBloodBank) · pathoGroupingAndTest · pathoKdlVerify · pathoReportDetails/Report/ResultSchema · pathosamplesutability · pathoSchema · pathoShowCovidStatus · pathoShowTestStatus(+New) · pathosubtestmaster · pathoUpdateOrderStatus · PatientFilterLabSchema · reportRelease(+Details/Filter/Microbiology/Report/Template)Schema · resultingDetail/Report/TemplateReportSchema · resultReview(+Filter/Microbiology/ReportDetails/Report/TemplateReportDetails/TemplateReport)Schema · sampleResult(Filter/Report)Schema · sampleRetestSchema · serviceStatusSchema · showLabLoadListSchema · showPatientWorkList.

## Routes
See features.md (14+ pathology routes).

## Key endpoints (03 §2.6)
- `pathoOrder/*` (114: sample save/report/release/result download), `pathotestmaster`, `pathoparameterunits`, `pathoprofilemaster`, `abha/*` (COVID/ABHA links).

## Client controllers
- patho-modules.js (429 KB), patho-booking-module.js.
