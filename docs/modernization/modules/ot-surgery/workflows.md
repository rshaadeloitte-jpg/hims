# M12 — Workflows

## Surgical case flow (02 §M12 Workflow)
1. Surgery order (nursing/doctor) → schedule (theatre, table, staff, equipment) with conflict checks.
2. Pre-op (PAC / pre-anaesthesia check) → procedure (checklist, anesthesia notes).
3. Post-op (recovery) → charges → billing estimation (OT pre-billing, BR-OT-002).

## Scheduling prerequisites (BR-OT-001)
- Patient needs active admission (else latest visit); none → BEOT004.
- Schedule / OT-details / procedure rows created; dependent-surgery discount% applied; procedure rate from tariff (0 if none).
- Evidence: OTSchedulerServiceImpl:616-800.

## Pre-billing (BR-OT-002)
- `otprocedure.pre.billing=true` → procedure-specialization charges created `consumed=true` (direct charge, billed upfront); doctor-share skipped for OT tariff services; ordered-procedures query filters `isOtProcessDone=false`.
- Evidence: AddServiceOrderServiceImpl:2202-2204; AddServiceOrderServiceHelper:383-386; OTSchedulerServiceImpl:4069-4077.

## Kit support
- CSSD (M11): procedureCssdRequest / procedureCssdIssue — kits requested for scheduled procedures; inventory consumption events (10 §2.1 row 11).
