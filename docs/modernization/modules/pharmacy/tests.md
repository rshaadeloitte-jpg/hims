# M07 — Tests (from 06 "Tests:" lines)

| Rule | Test cases |
|---|---|
| BR-PHAR-001 | + full; + partial (detail level); boundary: last item sale → VisitStatus 12 |
| BR-PHAR-002 | + full qty → closed; + partial → pending reduced |

**Golden-master priorities:**
- Stock decrement concurrency (row-level locking, 10 §3.4) — dual-write regression between Pharmacy and Inventory.
- VisitStatus 12 vs 11 ordering (payment before/after dispensing).
- Compounding/IV batch rows; IP return flows with print; pharmacy bill separate from IP bill (ipBillSummeryWithoutPharmcy).
