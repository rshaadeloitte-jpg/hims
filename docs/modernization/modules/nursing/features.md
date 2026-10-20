# M09 — Features (02 §M09)

| Feature | Evidence |
|---|---|
| Nurse worklist & task list | ShowNurseWorklistSchema; nurseTaskList (fjs-config_worklist) |
| Patient coversheet | ShowPatientCoversheetSchema; rolePatientCoversheet/:patientId/:admId/:patientIen/:visitIen |
| Notes (initial assessment, progress, procedure, addendum) | ShowNursePatientNoteSchema; notesTitle.json titles; signNotesFromTask, signTemplate |
| Plan of care (NCP problems/goals) | ShowNursePlanOfCareSchema; careplan templates; unmetGoals |
| Charting (vitals, partograph, growth charts, health factors) | nursing-charting-module.js; vitalsConfiguration.json; growthchart* masters; ODCWF_*.json charts (29 formats) |
| Shift handover | shifthandover templates; multiShiftHandoffReport |
| Allocation (nurse→ward/doctor/patient, allied→clinic) | nurseWardAllocationSchema, nurseListFor*AllocationSchema, alliedStaffListForPatientAllocationschema, staffListForClinicAllocationSchema |
| Incident reporting & resolution | incidentReportSchema, incidentResolveSchema |
| Notifiable disease reporting | notifyablediesase master |
| Vitals entry | vitalmaster; vitalsConfiguration.json (TEMP, qualifiers, slider ranges, units) |
| Temp prescription approval | tempPrescriptionListSchema |
| Nursing bed transfer | nursingList endpoints |
| Nursing orders | nursingOrderSchema (M03 screen), MedicOrderPickListSchema |
| Patient allergies/complaints/diagnosis | PatientAllergies, PatientComplaints, PatientDiagnosisDetails (models) |
| Outreach data (EJS) | outreach_data_schema |

## Roles (08 §4.1)
Nurse, staff nurse, nurse-staff, student nurse, Ward-Incharge, nusing-incharge, nursing supervisor, nursing superintendent (incl. misspelled "Nurseing Superwiser/Superintendent" — brittle role-name strings in JS).
