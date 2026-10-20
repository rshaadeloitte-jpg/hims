# M02 — Screens & Routes

## Screens (schemas, 43-key config)
appointment_list-schema · appointment_list_doctor-schema · doctor-appintment-slots-schema · walkinVisit-schema · followupVisitReq_list-schema · showDoctorOpSchedule · manageTokenSchema · DepartmentAppointmentSlots · DoctorAppointmentSlots · DoctorReferralSchema · FollowupDeptSchema · walkinVisitNhnc-schema · abhaPatientRegistration · baalAbhaReKyc · childAbhaPatientRegistration · searchAbhaPatient · addpatientDetailRegdto-schema · patientRghsPackageDetails · showDepartmentsApptSlots · showDepartmentWiseSlots · showDeptDoctorInformationSchema · showDoctorFutureAppointmentSchedule · minimal-updateAppointment_schema · minimal-walkin-appointment · questionnairePatientSchema · patientServiceDetailSchema · patientRecordSchema · emergencyRegType · mlcDetails family · patientHistory · patientMedicalRecord · updateJanaadharSchema · updateMLCDetailsSchema · sponsorSchema · AddMlcDetailsSchema · PatientDetailRegistrationDto · AddPatientRegistrationDto · Appointment-ListDto(+Doctor) · ConsultOrderList · DepartmentMasterDetailsListDto · DoctorMasterDetailsListDto · FamilyPlanningRegistration · InvestigationSchema · UpdateAppointmentDto · PatientHistorySchema · ShowPatientDetailSchema.

## Routes (33, 03 §2.1)
- `op:appointmentFlow` — walk-in flagship (open from role menu; `op:appointmentNewForm` checkin variant, `crud.c.url = api/rest/markandaddvisit/checkin`)
- `op:addAppointment/:doctor_id/:doctor_name/:department_id/:app_date/:from/:totime`
- `op:rescheduleApppointment/:.../:appointment_id/:patient_id/:totime`
- `op:addEmergency/:doctor_id/:doctor_name/:department_id`
- `op:appointmentList` / `appointmentListOnline` / `appointmentListdoc` / `appointmentListNhnc`
- `op:doctorSchedules/:doctor_id/:department_id`; `op:manageDoctorSlot`; `op:manageDepartmentSlot`; `op:showDepartmentSlot`
- `op:patientDetails/:allowEdit`; `op:patientHistory/:mrno`; `op:editPatientDetail/:mrnNo/:unitId`
- `op:appointment_confirmed`; `op:walkin_confirmed`; `op:emergency_confirmed`; `op:opFaceSheet`
- `op:uploadAppointments`
- kiosk routes: `kiosk:selfRegPatient`, `kiosk:selfAppointment/...`, `kiosk:searchAppointment/...` (M31)

## Key endpoints per screen (03 §2.1)
- Walk-in: `POST walkinVisit/addwalkinVisit?pat_token_no&app_type`; `searchToken`; `appointmentschedule/getDepartmentsByDate`; `getvisitTypes`; `patient/getPatientGeneralDetailsListForSearch`.
- Slots: `appointmentschedule/getAppointmentScheduleNew`; `getCabinWiseApointmentSlotCount`; `appointmentschedule/addOrRemoveSlots`.
- Facesheets: `patientRegAppt/pdfAppointmentFaceSheet` (+Html, +FromAppList).
