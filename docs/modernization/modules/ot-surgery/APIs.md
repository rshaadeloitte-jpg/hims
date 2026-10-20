# M12 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| OTSchedulerServiceImpl | `/otscheduler` | 79 endpoints: schedules, events, procedures, addPatientProcdure, checkOTAvailability, checkDoctorAllreadyBookedForSameTime, getSurgeryCompleteList, cancelSurgeryPatient; BR-OT evidence :616-800, :4069-4077 |
| OTConfigServiceImpl | `/otconfig` | 35 endpoints: theatre/table/procedure config |
| OTDetailsServiceImpl | `/otdetails` | OT details |
| SurgeryDetailsServiceImpl | `/surgerydetails` | surgery heads & details |
| AddServiceOrderServiceImpl | `/serviceOrder` | addSurgCharge, getSurgeryHeadDetailsForBillEstimation (billing coupling) |

**Endpoint scale (03 §5):** otscheduler (79) is a top-10 API bucket.
