# M17 — Business Rules (06 §B)

| ID | Rule | Evidence | Tests |
|---|---|---|---|
| BR-BLD-001 | Requisition status machine 1→2→3/10; cumulative volumes; header/detail asymmetry (R7) | BloodBankServiceImpl:6290-6317; BloodBankConstants:31-33 | + partial cross-match; + full; + issue; + expire → 10 |
| BR-BLD-002 | Auto-decros after 48h (2880 min), daily 00:00 | ApplicationJobs:1045-1061; BloodBankServiceImpl:7657-7701; :38 | + <48h untouched; + ≥48h decrossed; boundary: exactly 2880 |
| BR-BLD-003 | Bag expiry daily 23:00; issued skipped; crossmatched → decross | ApplicationJobs:769-815 | + expiring today; + already issued; + crossmatched → decross |
| BR-BLD-004 | Requisition expiry via orderValidationPeriod; sample-not-collected | ApplicationJobs:734-766 | + expired → 10; + collected → untouched |
| BR-BLD-005 | Donor auto-cancel 72h (DNR_NEW/FIT only) | ApplicationJobs:832-848; BloodBankConstants:8-17 | + 72h boundary; + post-med-exam untouched |

## Status dictionary (06 §A7/A8)
- Requisition: 1=NEW, 2=fully cross-matched, 3=fully issued, 10=cancelled/expired.
- Bag flags: isbagexpired, isdecrossed/crosSMatched, isreserved, isbagextracted, isdiscard/isdiscarded, isissued, istested, iscrossmatched (issuable predicate: all false except istested=true).
- Donor donationStatus: 1=NEW, 2=FIT, 3=UNFIT/VOLUNTORY (same literal), 4=ACCEPTED, 5=**REJECTED/DONATED (collision!**), 7=CANCELLATION, 10=PERM_UNFIT, 11=TEMP_UNFIT.
