# M03 — Business Rules (06 §B)

| ID | Rule | Evidence | Tests |
|---|---|---|---|
| BR-IPD-001 | Preconditions: admitting doctor (BERA022); registration required unless `allowDirectAdmission` (BERB009); no other active admission (BERB013); baby needs cradle ward (BERB012); attending defaults to admitting | PatientRegAdmissionServiceImpl:249-281 | + ok; − each violation |
| BR-IPD-002 | Bed auto-allocation priority chain + atomic CAS claim; failure BEXB001; interim fallback | :283-358, :1400-1402 | + first-fit; + waiting fallback; + none → BEXB001; boundary: concurrent claims |
| BR-IPD-003 | `M_BedStatusMaster.isrelease` must be true → else BERB005 | :1289-1301 | + release ok; − non-release → BERB005 |
| BR-IPD-004 | IPD number template `A/<PREFIX>/<YY>/%05d`; `CIMS_ADDADMISSIONINFORMATION` inserts T_Admission; `resultStatus==-1 → APP015` is **dead code** (RISK R4) | :1236-1252, :1265-1268 | + normal; + emergency; boundary seq overflow |
| BR-IPD-005 | Interim auto-abscond after 3 days | ApplicationJobs:991-1019 | + 3-day threshold; boundary exactly 3 days (<=) |
| BR-DIS-001 | Discharge preconditions (BERB023/BERB015/BERB011/24/25); transfer→inter-hospital request | PatientTransferDischargeServiceImpl:389-509 | + clean; − each violation |
| BR-DIS-002 | Discharge execution & death handling via CIMS_ADDBEDDISCHARGE / CIMS_UPDATEIPDISDISCHARGE; Solr `death=true` | :720-831; procs | + normal; + death; + baby delink/decouple |
| BR-DIS-003 | Bed release: OCCUPIED=false + BedStatusId=6 (hard-coded); universal/cradle restore | :779-803 | + normal; + universal; + cradle |

## Status codes (06 §A4/A5)
- T_Admission: ISDischarged 0→1, IsClosed '0'→'1'; ISCANCEL=1 + DISCHARGETYPEID + ABSCONDDATETIME (abscond); BEDID=0/null = interim marker.
- M_BedMaster: BedStatusId 0=reset, 1/3=free predicates, 5=occupied predicate, 6=released.
