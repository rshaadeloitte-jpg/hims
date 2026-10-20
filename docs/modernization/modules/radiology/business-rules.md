# M16 — Business Rules (cross-module)

| ID | Rule | Where it lives |
|---|---|---|
| BR-BILL-003 | Rad order auto-creation at billing: rad-specialization charges → `CIMS_BILLFORINVESTIGATIONNEW` → order booking with `IsImageCaptured=false` | 06 §B; AddServiceOrderServiceHelper:421-633 (M04/M08 owner) |
| BR-PATH-001 | Accepted-sample charges locked from removal → LEX001; refund-config override; applies to radiology too | 06 §B; AddServiceOrderServiceHelper:3693-3719, 3751-3862 |

**Status codes:** rad order status / report status — UNKNOWN-20 (02 §M16).

**Note:** no M16-specific BR-* IDs exist in 06; radiology rules are cross-module with M04/M08.
