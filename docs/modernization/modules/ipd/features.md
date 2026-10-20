# M03 — Features (02 §M03)

| Feature | Evidence |
|---|---|
| Admission request & admission list | admissionRequest_list-schema, admission_list-schema; addpatientAdmissiontdto_schema(+_emergency) |
| Planned/emergency/interim admission | emergency-schema, interimAdmissions; BR-IPD-005 (auto-abscond interim) |
| Bed reservation | bedReservationdto_schema, BedReservationServiceImpl (Solr index at :165) |
| Bed transfer | bedTransferSchema, bedTransferRequestSchema |
| Bed release | bedReleaseSchema, releaseBedNewSchema |
| Cradle management | showAllCradlesSchema (non-census beds) |
| Bed census | getBedCensusAndNonCensusList (BedManagementServiceImpl) |
| Discharge clearance list & update status | discharge_clearance_schema, dischargeUpdateStatus, getAdmissionClearanceList |
| Attendant pass issue/return/reprint | attendantPassSchema; printAttendantPass endpoint; attendant_pass.vm |
| MLC clearance filters | mlcClearanceFilters |
| Baby admission & mother–child linking | addBabyAdmissiondto_schema, motherChildLinkingSchema |
| Bill estimation at admission | billEstimatiomSchema (shared with M04) |
| Discharged patients list | showDischargedPatientSchema |
| Tid list (RGHS TID) | showTidListSchema, tidListSchema |
| Doctor IP schedule | showDoctorIpSchedule |
| Nursing station screen | ip:nursingStation |
| CDR forms (death) | ip:cdrForm4A/4B (child death review) |

## Bed board UX (03 §2.3)
- Building→ward→class→bed drill-down with occupancy colors (bed-managment-building.html / bed-managment-ward.html; color from `row.colorCode`); route params `/class_id/ward_id/bed_id/building_id/filterId`.
- Clearance workflow tabbed (clearanceTab.html); attendant pass separate screens + label printing.
