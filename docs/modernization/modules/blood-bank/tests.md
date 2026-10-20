# M17 — Tests (from 06 "Tests:" lines)

| Rule | Test cases |
|---|---|
| BR-BLD-001 | + partial cross-match; + full; + issue; + expire → 10 (details-level; header asymmetry — RISK R7) |
| BR-BLD-002 | + <48h untouched; + ≥48h decrossed; boundary: exactly 2880 min |
| BR-BLD-003 | + expiring today; + already issued; + crossmatched → decross |
| BR-BLD-004 | + expired → 10; + collected → untouched |
| BR-BLD-005 | + 72h boundary; + post-med-exam statuses untouched |

**Golden-master priorities:**
- Donor status 5 collision test (REJECTED vs DONATED) — reports/audit ambiguity (06 §C.3).
- Issuable-bag predicate: `isreserved=false, isbagextracted=false, isdiscard=false, isdiscarded=false, isissued=false, istested=true, iscrossmatched=false`.
- Cross-match volume accumulation (cumulative ≥ ordered).
