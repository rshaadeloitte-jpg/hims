# M03 — Screens & Routes

## Screens (schemas, 65-key config)
addpatientAdmissiontdto_schema(+_emergency) · addBabyAdmissiondto_schema · admission_list-schema · admissionRequest_list-schema · bed_management_schema · bed_screen_schema · bedTransferSchema · bedTransferRequestSchema · dischargePatientSchema · discharge_clearance_schema · dischargeUpdateStatus · transferRequest_list-schema · attendantPassSchema · bedReservationdto_schema · interimAdmissions · emergency-schema · bedReleaseSchema · releaseBedNewSchema · billEstimatiomSchema · clearancemaster · getAdmissionClearanceList · mlcClearanceFilters · motherChildLinkingSchema · showAllCradlesSchema · showDischargedPatientSchema · showDoctorIpSchedule · tidListSchema · updateDoctorFromBM_Schema · ipd_patient_service_detail_schema · ipPatientCoronaDetailsSchema · MedicOrderPickListSchema · nursingOrderSchema · admissionServiceSchema · allPreviuosAdmissionsSchema · addVisitChargeSchema · bedReservationdto_schema.

## Routes (23, 03 §2.3)
`ip:admissionOrder` · `ip:admissionOrderEmer(V/:v)` · `ip:admissionList` · `ip:admissionReqList` · `ip:transferReqList` · `ip:admissionConfirm` · `ip:attendantPass` · `ip:bedManagement/:class_id/:ward_id/:bed_id/:building_id/:filterId` · `ip:bedManagementNew/:class_id/:ward_id/:bed_id` · `ip:bedScreen/...` · `ip:otBedManagement` · `ip:bedReservation` · `ip:unReservedBedList` · `ip:motherChildLinking` · `ip:clearanceListFlow` · `ip:MlcclearanceListFlow` · `ip:nursingStation` · `ip:dischargedPatients` · `ip:showAllCradles` · `ip:summaryOpdIpd` · `ip:updateDischargeStatus` · `ip:showInterimAdmissions` · `ip:showTidList` · `ip:doctorCheckin` · `ip:cdrForm4A/4B`.

## Key endpoints (03 §2.3)
- `patientRegAddmission/*` (22), `ipdadmission/*` (getSponsorByAdmissionID, getActiveAdmissionOfRegisteredPatient, addAdmMLCDetails), `bedmanagement/*` (34), `patientTransferDischarge/*` (29), `walkinVisit/printBHTBarcode`, `printAttendantPass`, `otscheduler/*`, `serviceOrder/allServiceFastByGet`.
