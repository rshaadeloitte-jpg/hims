# M15 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| KDL (instrument interface) | In | pathoKdlVerify, machineWiseResultReview, getResultsFromKDL (`/patient`); machinemaster |
| HL7 / EMR | Out | Lab_Results in tiuxml; T_OUTBOUNDHIS (dormant outbound sender) |
| Blood bank (M17) | In | pathoGenerateBarCodeSchemaForBloodBank; serology linkage (requisition statuses BR-BLD-*) |
| WhatsApp | Out | PathoOrderServiceHelper.sendCategoryWiseReportOnWhatsApp; queue drained by triggerWhatsappReportsInQueue (15-min window 14:00–07:00) |
| eUpkaran (lab equipment) | Out | IntegrationServiceImpl:1537 |
| EXT LIMS (state lab) | Out | DB insert staging — scheduled but **disabled** (`ISEXTLMISENABLED` in DB, ApplicationJobs.addStats_Archived:2556-2943) |

## Solr/Redis
- patient collection (worklists); item collection (consumables); master caches (test/profiles); `current_pat_` mirrors.

## Scheduled jobs
- triggerWhatsappReportsInQueue (15-min window); EXT LIMS staging (disabled).
