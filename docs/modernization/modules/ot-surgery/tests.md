# M12 — Tests (from 06 "Tests:" lines)

| Rule | Test cases |
|---|---|
| BR-OT-001 | + admitted; + OPD visit; − neither → BEOT004 |
| BR-OT-002 | + pre-bill on (consumed direct charge); + pre-bill off (temp charge path) |

**Golden-master priorities:**
- Doctor double-booking conflict check (same-time slot) — checkDoctorAllreadyBookedForSameTime.
- OT availability check across theatres/tables (checkOTAvailability).
- Surgery heads billing estimation (getSurgeryHeadDetailsForBillEstimation) vs final bill.
- Procedure checklist completion gating (isOtProcessDone=false).
