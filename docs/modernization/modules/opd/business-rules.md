# M02 — Business Rules (06 §B, BR-OPD-*)

| ID | Rule | Evidence | Tests |
|---|---|---|---|
| BR-OPD-001 | Non-resident patient (`resStateId != config.state`) → sponsor tariff `config.nonresidentialtariffId`, else literal `2L` | EmergencyAndWalkinVisitServiceImpl:410-419; PatientRegAdmissionServiceImpl:366-375 | + resident; + non-resident w/ config; − non-resident w/o config → 2L |
| BR-OPD-002 | Slot collisions: (a) same-day same-dept active encounter → BEXA029; (b) same-day hour window same dept+unit → BERA028; (c) ≥30 min apart, slot=`config.appointmentslot` (15) | :537-546, :547-593, :423-426 | + free slot; − BERA028; − BEXA029 |
| BR-OPD-003 | Visit number template `V/<PREFIX>/<YY>/%05d`; emergency `EV/...` | :1291-1308 | + normal; + emergency; boundary seq>99999 |
| BR-OPD-004 | Visit-type sequence state machine by `encountervalidity`; emergency type flag | :1052-1107 | + seq1; + revisit within validity; + after validity restart |
| BR-OPD-005 | Sponsor defaults + plan required (BERP008); CIMS_ADDPATIENTSPONSOR_NEW_DOC | :1359-1365, :1729-1794 | + defaults; − BERP008 |
| BR-OPD-006 | Walk-in without doctorId → BERA022 (unless unit-head resolves, else BERA029) | :380-388, :628-630 | + doctor; − BERA022 |
| BR-OPD-007 | After OPD bill settlement → T_Appointment.VisitStatus=11 (only if <11); checkout → T_Encounter.isCheckout=true + Solr | AddPaymentServiceHelper:438-447,477-478,820 | + settled → 11; − already 12 unchanged |

## Status dictionary (06 §A1)
- T_Encounter.VisitStatus: 3=doctor called patient; 9=doctor checkout; 10=cabin released.
- T_Appointment.VisitStatus: 4=assessment questionnaire saved; 6=other questionnaire; 11=bill paid/checked out; 12=pharmacy fully dispensed.
- T_Encounter.visitstatus2: "2"=default walk-in; "9"=compound-med visit.
- **Split-brain warning (06 §C.1):** same codes in two tables — kiosk/queue reads `getLastVisitStatus` from one side only.
