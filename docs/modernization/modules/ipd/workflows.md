# M03 — Workflows

## Admission (02 §M03 Workflow)
1. Admission request → bed allocation (auto or manual) → admit (generate admission no `A/<PREFIX>/<YY>/<00001>`, BR-IPD-004).
2. Ward services: nursing (M09), diet (M10), pharmacy (M07), orders (M08).
3. Discharge advised → clearance (billing/BMW/MLC/attendant) → discharge summary → bed release.

## Bed auto-allocation (BR-IPD-002)
- Free bed predicate: `OCCUPIED=false AND status=true AND isnoncensus/isot/ispreop/ispostop=false`.
- Priority chain: (1) same ward+building+category → (2) waiting-bed → (3) floating-bed → (4) waiting+floating.
- Atomic claim: `UPDATE ... SET OCCUPIED=true WHERE OCCUPIED=false`; failure → BEXB001.
- Auto-allocation off + no bed → interim (un-bedded) admission.

## Interim auto-abscond (BR-IPD-005)
- Admission with `BEDID=0/null`, `ISCANCEL=0`, `ADDEDDATETIME ≤ now−3 days` → `ISCANCEL=1`, discharge-type "auto absconded", `ABSCONDDATETIME` (job `${interimAdmission.abscond.schedule}`, ApplicationJobs:991-1019).

## Discharge (BR-DIS-001/002/003)
1. Preconditions: advised discharge active (BERB023); dates ≥ admission date; discharge date ≥ last charge addedDateTime; pending amount > 0 excluding gate-pass → BERB015; cradle babies with non-finalized bills → BERB011; photo+doc+feedback as a set (BERB023/24/25); transfer-to-other-hospital → inter-hospital admission request + COVID referral.
2. Execution: `CIMS_ADDBEDDISCHARGE` inserts T_Discharge; death branch: `T_Registration_Details.isdeath=1`, T_Discharge.IsDeath/DeathDate/HandOvrDate, PatientDeathDetails, Solr `death=true`; `CIMS_UPDATEIPDISDISCHARGE` sets `ISDischarged=1, IsClosed='1'` (+ babies + T_TransferRequest closed); Solr encounters cleared unless death.
3. Bed release: `OCCUPIED=false` + `BedStatusId=6` (hard-coded literal — RISK R5); universal bed restored to origin; cradle bed re-mapped to cradle ward.

## Discharge lock
- `CDD_<opdipdid>` Redis lock (60s) prevents concurrent discharge (ServiceLayerAspect:1147-1156).
