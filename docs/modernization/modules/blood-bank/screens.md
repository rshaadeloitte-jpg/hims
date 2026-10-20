# M17 — Screens & Routes

## Screens (schemas, 40-key config)
donorRegistartion · bloodComponents · bloodComponentSeperationSchema · bloodCrossMatchingListSchema · patientRequisitionList · showBloodStockListSchema · showBloodDiscardListSchema · showBloodBankIndentListSchema · showBagsReturnListSchema · VoluntaryDonationSchema · CampDonationSchema · addAdminRequisition · addBloodBagOpeningStock · afterAddOpeningStock · AutologousDonationSchema · bagReturnDetailsSchema · bloodBagReturnSchema · blooddonortypemaster · bloodRequisitionCrossMatchingSchema · bloodtestmaster · bloodtestresultmaster · campRequisitionSchema · crossMatchingBloodbagDetails · crossMatchingForm · crossMatchingPatientDetails · crossUnitOrderSchema · crossUnitOrdersListSchema · donorrejectionreasonmaster · externalBloodStorageCenterRequisition · externalHospitalRequisition · ExternalPatientRequisitionSchema · openingBloodStockSourceDetailsSchema · patientRequisitionDetails · ReplacementDonationSchema · replacementDonorHistory · requestDetailsGeneric · showCrossMatchedBagsSchema · showDiscardedBagsSchema · showDonorListSchema.

## Routes (31, 03 §2.7)
See features.md.

## Key endpoints (03 §2.7)
- `bloodtestmaster/*` (115 — stock counts, bag issue, districts, requisition details), `patientrequisition/*`, `eraktkosh*`, `bloodcomponent*`, `reports/*` (16 blood-bank report routes).

## Client controller
- blood-bank-module.js (653 KB).
