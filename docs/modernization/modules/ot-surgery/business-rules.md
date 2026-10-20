# M12 — Business Rules (06 §B)

| ID | Rule | Evidence | Tests |
|---|---|---|---|
| BR-OT-001 | Scheduling prerequisites: active admission (else latest visit) → none → BEOT004; schedule/OT-details/procedure rows created; dependent-surgery discount%; procedure rate from tariff (0 if none) | OTSchedulerServiceImpl:616-800 | + admitted; + OPD visit; − neither → BEOT004 |
| BR-OT-002 | OT pre-billing: `otprocedure.pre.billing` → procedure charges `consumed=true` (direct charge, not temp); doctor-share skipped for OT tariff services; ordered procedures filter `isOtProcessDone=false` | AddServiceOrderServiceImpl:2202-2204; AddServiceOrderServiceHelper:383-386; OTSchedulerServiceImpl:4069-4077 | + pre-bill on; + pre-bill off (temp charge path) |

## Status codes
- operationstatusmaster, getSurgeryCompleteList, cancelSurgeryPatient; OT schedule status; UNKNOWN-18 (02 §M12).
- Authorization enums (06 §A9): 0=ACCEPTED, 1=APPROVED, 2=PARTIALAPPROVED, 3=REJECTED, 4=FAILED, 5=NEW, 6=CANCEL, 7=SUBMITTED, 8=QUERIED — relevant to approval flows.
