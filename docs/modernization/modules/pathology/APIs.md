# M15 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| PathoOrderServiceImpl | `/pathoOrder` | **114 endpoints / 14,280 LOC** (04 §9.1 god class): booking, sample, result, release, report, download, WhatsApp |
| InvestigationServiceImpl | `/investigation` | investigation ordering (shared M08) |
| MachineConfigServiceImpl | `/machineConfig` | machine config lists (M38) |
| PatientServiceImpl | `/patient` | getResultsFromKDL (KDL result fetch) |
| PatientRequisitionServiceImpl | `/patientrequisition` | blood bank barcode generation |
| AddServiceOrderServiceImpl | `/serviceOrder` | checkSampleStatusForCreditNote, releaseAllLab, updatePathoClinicalDetails (BR-PATH-001 interplay) |

## Endpoint scale (03 §5)
- pathoOrder (114) is the 4th-largest API bucket; patho-modules.js drives most.

## Client-side helpers
- PathoOrderServiceHelper.sendCategoryWiseReportOnWhatsApp (WhatsApp report delivery).
