# M02 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| AppointmentServiceImpl | `/appointment` | booking, list, cancel (cancelledAppointmentList, apptCancelReason, apptRescheduleReason) |
| AddUpdateAppointmentServiceImpl | `/updateappointment` | cancelupdateappointment etc. |
| AppointmentScheduleServiceImpl | `/appointmentschedule` | getAppointmentScheduleNew, addOrRemoveSlots, getDepartmentsByDate, getCabinWiseApointmentSlotCount |
| PatientRegApptServiceImpl | `/patientRegAppt` | registration+appointment combined; sendOTP/validateOTP; pdfAppointmentFaceSheet*; Solr index at :986,1299 |
| EmergencyAndWalkinVisitServiceImpl | `/walkinVisit` | addwalkinVisit, walkinVisitNhnc, checkin support; BR-OPD evidence; Solr index at :1795-1798, repair :3953-4145 |
| DoctorServiceImpl | `/doctor` | checkinCheckout, queue assignment, kioskQueue helpers |
| QueueManagmentServiceImpl | `/queueManagment` | token/queue ops (see M27) |
| MarkAndAddVisitServiceImpl | `/markandaddvisit` | checkin (emergency + kiosk) |
| DoctorListServiceImpl | `/doctorlist` | doctor lists |
| QuetionnaireServiceImpl | `/questionnaireService` | pre-consult questionnaires (VisitStatus 4/6) |
| PatientServiceImpl | `/patient` | myWalkingVisit, kioskQueue, getPatientVistaDetails (appointment-list-module.js) |

**Key endpoint counts (03 §5):** patient (83), walkinVisit (51) include OP flows.
