# M15 — Workflows

## Lab workflow (02 §M15 Workflow)
1. Order (from billing/clinical — BR-BILL-003 auto-creates booking + one detail per sample in `M_PathoTestSample`, SampleCollectionNO=1..n, SampleNo=0, IsOutSourced=false, PathologistId=0).
2. Sample collect (barcode) → accept/reject (sampleRejectionSchema).
3. Run (instrument) → resulting (parameter-level) → review (resultReviewSchema).
4. Release (reportReleaseSchema) → report print / HL7 / WhatsApp.
5. Critical alerts (criticalAlertResultSchema) on abnormal values.

## Charge-removal lock (BR-PATH-001)
- Patho/rad charge with accepted sample (`isaccepted=true` on latest booking detail) **cannot be removed** unless `autoChargeCancellationOnRefund` → else `LEX001`.
- Removable path: booking `ISCANCELLED=true`; nursing → `T_PATIENTNURSINGORDERS.orderstatus=DISCONTINUE`; procedure → EMR service/procedure/schedule status=false.
- Evidence: AddServiceOrderServiceHelper:3693-3719, 3751-3862.

## Status lifecycle
- Test status (pathoShowTestStatus, pathoUpdateOrderStatus) → sample status (acceptReject) → result status (resultReview) → release status (reportRelease); critical alert flag.
