# M01 — Tests (from 06-business-rules-catalog.md "Tests:" lines)

Only test evidence available is per-rule in `06` — no legacy automated test suite is documented.

| Rule | Test cases |
|---|---|
| BR-PAT-001 | + series off/on; + appointment temp MRN padding (1/2/3 digit); + offline reuse; − invalid; boundary: patientId → 9-digit pad; MRN colliding (BERA011) |
| BR-PAT-002 | + unique passes; − same name/mobile/dob → BERA011; − same healthIdNumber → BEXA040 |
| BR-PAT-003 | + both; − familyId only → BERP007 |

**Suggested golden-master tests for migration:** duplicate-identity heuristics (Solr edismax boost weights are load-bearing — 07 §1.7); temp→permanent MRN promotion on visit/admission; MRN series rollover.
