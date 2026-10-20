# M04 — Tests (from 06 "Tests:" lines)

| Rule | Test cases |
|---|---|
| BR-BILL-001 | + draft (no number); + final (number); boundary: sequence formatting |
| BR-BILL-002 | + share>0; + share=0 → none; − OT service → none |
| BR-BILL-003 | + patho bill → orders; + rad bill → orders; + non-patho → none; boundary: 0 samples |
| BR-BILL-004 | + tax before concession; + tax after concession; + no taxId → 0 |
| BR-BILL-005 | + self company; + non-self company; + both concession tiers (**self adds, non-self replaces — divergence test**) |
| BR-BILL-006 | + full payment → settled+checkout; + partial → unsettled |
| BR-BILL-007 | + full advance; + partial; + multiple advances order (FIFO) |
| BR-BILL-008 | + advance refund; + bill cancel restore; − zero balance → BEXAR002 |

**Golden-master priorities (06 §C):** VisitStatus 11/12 split-brain (billing writes T_Appointment, clinical writes T_Encounter); non-self concession overwrite; GST threshold date; changeAmount on cash.
