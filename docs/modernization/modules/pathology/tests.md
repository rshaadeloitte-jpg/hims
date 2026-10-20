# M15 — Tests (from 06 "Tests:" lines)

| Rule | Test cases |
|---|---|
| BR-PATH-001 | + no sample → cancel ok; + collected → LEX001; + refund-config → cancel ok |

**Golden-master priorities:**
- Order auto-creation at billing (BR-BILL-003): patho bill → orders; 0-sample boundary.
- Sample barcode round-trip (pathoGenerateBarCodeSchema + blood bank variant).
- Result review → release state machine; critical alert trigger thresholds.
- KDL result verify (pathoKdlVerify) — machine result ingest.
- Requisition expiry (BR-BLD-004 adjacency): requested date + orderValidationPeriod → status 10.
