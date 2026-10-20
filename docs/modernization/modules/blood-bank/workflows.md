# M17 — Workflows

## Requisition flow (02 §M17 Workflow)
1. Requisition (patient/ward) → donor match / cross-match → issue bag → transfusion record → return/discard.

## Donor cycle
1. Registration → screening → collection → component separation → testing → stock.

## Requisition status machine (BR-BLD-001)
- 1=NEW (cross-match pending/partial) → after cross-match → 2 if cumulative cross-matched volume ≥ ordered volume (else stays 1) → after issue → 3 if cumulative bag volume ≥ ordered volume → cancel/expire → 10.
- Evidence: BloodBankServiceImpl:6290-6317; BloodBankConstants:31-33. **Header/detail asymmetry — RISK R7.**

## Auto-decros (BR-BLD-002)
- Daily 00:00: crossmatched (status=1), not decrossed, `crossMatchedTestDate` ≥ 2880 min old → decross bag (`crosSMatched=false`), requisition → 1, count decremented, BloodBagDecross reason=0. (ApplicationJobs:1045-1061; BloodBankServiceImpl:7657-7701.)

## Bag expiry (BR-BLD-003)
- Daily 23:00: bags with `expirydate = today` → `isbagexpired=true` (issued skipped); crossmatched & unissued → decross + requisition → 1. (ApplicationJobs:769-815.)

## Requisition expiry (BR-BLD-004)
- `requesteddate + orderValidationPeriod(h) ≤ sysdate` and sample not collected and status=1 → details `requisitionstatus=10`, `isorderexpired=true`, reason "Order is expired". (ApplicationJobs:734-766.)

## Donor auto-cancel (BR-BLD-005)
- Donor in DNR_NEW(1)/DNR_FIT(2) with no action within 72h → donationStatus=7 (DNR_CANCELLATION). (ApplicationJobs:832-848.)
