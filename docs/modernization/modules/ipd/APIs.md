# M03 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| IPDAdmissionServiceImpl | `/ipdadmission` | admission flows, getSponsorByAdmissionID, getActiveAdmissionOfRegisteredPatient, addAdmMLCDetails |
| IPDServiceImpl | `/ipd` | IPD patient service details |
| IPDConfigServiceImpl | `/ipdconfig` | ward/bed/room/class/diet-plan masters (M39-adjacent) |
| BedServiceImpl | `/bed` | bed state |
| BedReservationServiceImpl | `/bedreservation` | reservations; Solr index :165 |
| BedManagementServiceImpl | `/bedmanagement` | 34 endpoints: isBedAvailable, bed census, transfer, release; BedStatusId predicates (06 §A5) |
| CancelAdmissionServiceImpl | `/canceladmission` | admission cancellation |
| PatientTransferDischargeServiceImpl | `/patientTransferDischarge` | 29 endpoints: transfer requests, clearance, discharge; BR-DIS evidence; Solr at :698-3282, :4400-4413 |
| PatientRegAdmissionServiceImpl | `/patientRegAddmission` | 22 endpoints; BR-IPD evidence; Solr at :875, :1180, :1662-1665 |
| PatientRequisitionServiceImpl | `/patientrequisition` | blood requisition for IPD (M17) |
| PatientServiceImpl | `/patient` | mortuary/death endpoints (M28) |

**Endpoint scale (03 §5):** admission-module.js references 132 APIs.
