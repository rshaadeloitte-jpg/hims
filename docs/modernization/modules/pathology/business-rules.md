# M15 — Business Rules (06 §B)

| ID | Rule | Evidence | Tests |
|---|---|---|---|
| BR-PATH-001 | Sample collection locks charge removal: accepted sample (`isaccepted=true`) blocks charge removal → LEX001 unless `autoChargeCancellationOnRefund`; removable → booking ISCANCELLED=true; nursing order DISCONTINUE; procedure status=false | AddServiceOrderServiceHelper:3693-3719, 3751-3862 | + no sample → cancel ok; + collected → LEX001; + refund-config → cancel ok |

## Cross-module rules
- BR-BILL-003 (06): patho orders auto-created at billing via `CIMS_BILLFORINVESTIGATIONNEW` — one booking + one detail per sample in `M_PathoTestSample` (owned jointly with M04).

## Status codes
- Requisition/order status adjacency with BR-BLD-004 (orderValidationPeriod expiry → requisitionstatus=10 — blood-bank requisitions also drive lab).
- UNKNOWN: critical-alert threshold values not in code.
