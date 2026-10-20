# M02 — Features (02 §M02, agent-03 §2.1)

| Feature | Evidence |
|---|---|
| Doctor appointment slots | doctor-appintment-slots-schema; `appointmentschedule/getAppointmentScheduleNew`, `getCabinWiseApointmentSlotCount` |
| Department/doctor schedules | showDoctorOpSchedule, DepartmentAppointmentSlots, `appointmentschedule/getDepartmentsByDate` |
| Walk-in visits | walkinVisit-schema (`POST walkinVisit/addwalkinVisit?pat_token_no&app_type`); BR-OPD-006 (doctor required) |
| NHNC walk-in | walkinVisitNhnc-schema (appointmentListNhnc route) |
| Emergency registration | emergencyRegType, `markandaddvisit/checkin` |
| Follow-up visit requests | followupVisitReq_list-schema, FollowUpVisitReqList |
| Doctor cabin check-in/check-out | DoctorServiceImpl checkinCheckout; cabin queue |
| Token generation | manageTokenSchema, token queue side-effects (`stopAndStartCounterQueue`, `fetchLatestToken`, `fetchAbsentToken`) |
| MRN re-print / op labels | opdCard, appointmentOpLabel.vm |
| Referral types | doctorReferralSchema |
| Camp registration | opCampRegistration |
| Telemedicine | telemedicine department/unit/speciality lists; createTeleconsultRoom; `getDeptsForTsu`, `getTelemedicineSpecialityUnit` |
| Appointment upload | op:uploadAppointments (file upload + parse) |
| Questionnaires | questionnairePatientSchema (pre-consult; T_Appointment VisitStatus 4/6) |
| Confirmation/facesheet | op:appointment_confirmed / op:opFaceSheet → `patientRegAppt/pdfAppointmentFaceSheet*` |

## Client-side business logic (03 §3)
- Walk-in screen: 9 `dynamicAspects` branches keyed on gvars (isExtLMISEnabled, app_type_w, isNhncEmer, appointmentAgainstUnitHead, isTelemedicine, isFPV, isPctsRegistration, isCheckinFlow, isInsuranceCategoryMap).
- ~35 jQuery controlEvents for search-as-you-type identity lookups.
