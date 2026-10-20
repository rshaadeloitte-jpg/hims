# M02 — Database (05 §3.1/3.2, 10 §2.1 row 2)

## Tables
| Table | Notes |
|---|---|
| T_Encounter | Visit header — highest-shared table (52 refs); VisitStatus/visitMark/IsCheckout |
| T_Appointment | Appointment rows; VisitStatus 4/6/11/12; apptCancelReason/apptRescheduleReason |
| T_Visit | Visit sequence types (BR-OPD-004) |
| T_PATIENTTOKEN | Tokens |
| T_REGISTRATIONQUEUE / T_PATIENTQUEUE | Queue (shared with M27) |
| T_PATIENTSPONSOR | Sponsor per visit (BR-OPD-005) |
| FollowUpVisitInfo | Follow-up requests |
| masters/hospital | schedulemaster, shiftmaster, opdStationMaster, roomMasterForQueue, cabinmaster, departmentCabinRoomMapping |
| M_VISITTYPEMASTER | encountervalidity, isEmergency |
| M_APPOINTMENTREASONMASTER | appointment reasons |

## Stored procedures
- `CIMS_ADDPATIENTSPONSOR_NEW_DOC` (BR-OPD-005)
- `CIMS_ADDPATIENTREGISTRATION*` (walk-in registration path)

## Sequences
- `T_APPOINTMENT_CHANNEL_SEQ` (audit), per-table `<t>_seq` convention.

## Migration notes
- Target service owns T_APPOINTMENT/T_ENCOUNTER/T_PATIENTTOKEN + queue/cabin tables; emits `EncounterCreated`, `AppointmentBooked`, `VisitClosed` (10 §2.1 row 2).
- **Partitioning consideration** for T_ENCOUNTER by year (10 §3.4).
- Redis: session/queue state short-lived (no source-of-truth writes).
