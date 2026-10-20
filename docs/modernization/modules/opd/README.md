# Module: opd (M02 — OPD / Outpatient Department)

> Master reference: `../../MODULE-INVENTORY.md` §M02; catalog: `../../02-business-module-catalog.md` §M02; rules: `../../06-business-rules-catalog.md` §B (BR-OPD-*); frontend: `../../03-legacy-frontend-inventory.md` §2.1.

**Target services (10 §2.1):** 2. Appointment & Encounter (OPD) + 29. Queue & Kiosk (queue sub-part).

**One-line purpose:** Outpatient visit lifecycle from appointment to consultation queue.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_op.json` (43 keys) |
| Services | `/appointment`, `/updateappointment`, `/appointmentschedule`, `/patientRegAppt`, `/walkinVisit`, `/doctor`, `/queueManagment`, `/markandaddvisit`, `/doctorlist` |
| Flagship screen | `op:appointmentFlow` (walk-in; walkinVisit-schema.json, 6,621 lines) |
| Business rules | BR-OPD-001..007 |
| Critical risk | **VisitStatus split-brain**: billing writes T_Appointment, clinical writes T_Encounter, same codes (06 §C.1) |
| Complexity | 4/5 |

**Routes (33):** see `screens.md`. **Status codes:** T_Encounter 3/9/10; T_Appointment 4/6/11/12; visitstatus2 "2"/"9" (06 §A1).

**Key dependencies:** M01, M08 (orders), M27 (queue), M04 (billing), M09 (nurse desk), Masters.

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md · tests.md
