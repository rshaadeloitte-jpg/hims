# M09 — Migration

**Target service:** 12. Nursing & Inpatient Care — **Wave 3 (Clinical Care)** (10 §6).

**Complexity: 4/5** — god-class decomposition (17,350 LOC / 300 endpoints); tiuxml forms engine; vitals/charting configs; worklist ownership boundary.

**Steps:**
1. **Decompose NursingServiceImpl** by sub-domain: notes, vitals/charting, tasks/worklist (M26), allocation, incidents, shift handover — separate tx boundaries.
2. **Forms engine decision (03 §6 Q3):** reuse the tiuxml data model, not the renderer; 368 versioned forms (_V10.._V15, _new, _P, _B, _AH, _AL, _nhnc, _risl) need a form-definition registry + renderer service.
3. **Configs:** vitalsConfiguration.json (qualifiers/ranges), notesTitle.json, ODCWF_*.json charts (29 formats), mastersConfigration — migrate as versioned reference data.
4. **Ownership decision (02 open Q5):** careplan/charting/shifthandover are template-only configs — confirm as M09 features (recommended) or standalone services.
5. **Events:** VitalsRecorded, NurseTaskCompleted; consume PatientAdmitted/PatientDischarged/OrderCreated; results via LabResultFinalized (M15).
6. **Solr:** healthfactor/icd10 drainers stay; worklists move to event-fed reads.
7. **Wave placement:** Wave 3 ≥ Wave 2 events (orders/billing).

**Open questions:** temp prescription approval flow; nursing bed transfer semantics; incident severity matrix.
