# M02 — Workflows

## Visit lifecycle (02 §M02 Workflow)
1. Book appointment (walk-in / phone / kiosk / camp / e-Mitra) — slot collision checks (BR-OPD-002).
2. Validate identity (patient search → M01; ABHA/Aadhaar/Janaadhar).
3. Token/queue (M27) → `T_PATIENTTOKEN`, queue state in Redis (QMS).
4. Doctor check-in (cabin) — T_Encounter.VisitStatus 3.
5. Consultation → orders (lab/rad/pharmacy, M08/M15/M16/M07) → billing (M04).
6. Payment → T_Appointment.VisitStatus=11 (+ T_Encounter.isCheckout=true) (BR-OPD-007).
7. Pharmacy fully dispensed → T_Appointment.VisitStatus=12 (BR-PHAR-001).
8. Doctor checkout — VisitStatus 9; cabin released (no pending temp charges) — VisitStatus 10; discharge home / follow-up request.

## Appointment slot rules (BR-OPD-002)
- (a) active same-day same-dept encounter → BEXA029; (b) same-day hour window same dept+unit appointment → BERA028; (c) appointments ≥30 min apart; slot duration `config.appointmentslot` (default 15).

## Visit numbering & types (BR-OPD-003/004)
- `opdNo = V/<PREFIX>/<YY>/<00001>`; emergency `EV/...`; per-unit sequence (Unitmaster.getOPDSequenceName).
- Revisit within `Visittypemaster.encountervalidity` (+1 day) advances visit-type sequence; emergency → isEmergency type.

## Sponsor defaulting (BR-OPD-005)
- Company `config.selfcompanyid`, tariff `config.selftariffid`, priorityId=1; missing plan → BERP008; persisted via `CIMS_ADDPATIENTSPONSOR_NEW_DOC`.

## Solr/encounter index
- Appointment→encounter indexed realtime (PatientRegApptServiceImpl:986,1299); `closeDailyEncountersJob` deletes encounter docs older than `solr.encounter.retain.days=3` (ApplicationJobs:542-570).
