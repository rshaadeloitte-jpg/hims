# M02 — Tests (from 06 "Tests:" lines)

| Rule | Test cases |
|---|---|
| BR-OPD-001 | + resident; + non-resident with config; − non-resident without config → falls to `2L` |
| BR-OPD-002 | + slot free; − same-hour → BERA028; − duplicate dept/date → BEXA029 |
| BR-OPD-003 | + normal; + emergency; boundary: seq > 99999 (5-digit overflow) |
| BR-OPD-004 | + new patient seq1; + revisit within validity → next seq; + revisit after validity → restart |
| BR-OPD-005 | + defaults applied; − missing plan → BERP008 |
| BR-OPD-006 | + doctor; − none → BERA022 |
| BR-OPD-007 | + settled → VisitStatus 11; − VisitStatus already 12 → unchanged |

**Golden-master priorities:** VisitStatus 11/12 split-brain divergence test (06 §C.1); slot duration config default 15 min; NHNC variant behavior.
