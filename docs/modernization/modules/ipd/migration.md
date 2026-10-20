# M03 — Migration

**Target service:** 3. ADT & Bed — Wave 1 (10 §6).

**Complexity: 4/5** — bed CAS + 5 CIMS_* procs, discharge/death rules, interim-abscond job, dead APP015 path.

**Steps:**
1. **Proc conversion:** CIMS_ADDADMISSIONDETAILS / CIMS_ADDADMISSIONINFORMATION / CIMS_UPDATEBEDOCCUPIEDFLAG_1 / CIMS_ADDBEDDISCHARGE / CIMS_UPDATEIPDISDISCHARGE → PL/pgSQL functions on the extraction path (10 §5).
2. **Bed CAS semantics** must stay atomic in PG: `UPDATE M_BEDMASTER SET OCCUPIED=true WHERE OCCUPIED=false` — row-level locking identical to legacy.
3. **Pin the BedStatusId=6 release contract** (06 §C.4) — currently literal in discharge SQL while allocation validates against master `isrelease`.
4. **Fix dead APP015 path** (RISK R4): Java `resultStatus==-1` never fires; concurrent admissions rely on pre-checks (BERB013) — add DB-level guard in target.
5. **Events:** PatientAdmitted / PatientDischarged / BedChanged (bed auto-charges → Billing) / TransferRequested (10 §2.1 row 3, §4).
6. **Interim-abscond job** (ApplicationJobs:991-1019) → scheduled task in ADT service.
7. **Discharge flows** keep death handling (isdeath=1, Solr death=true, PatientDeathDetails) — coordinate with Patient Identity + Mortuary (M28) + Reporting.

**Exit criteria (Wave 1):** PatientAdmitted stream live; Solr fed by events; bed CAS verified under concurrency test.
