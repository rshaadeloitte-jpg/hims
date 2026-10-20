# M17 — Migration

**Target service:** 13. Blood Bank — **Wave 3 (Clinical Care)** (10 §6).

**Complexity: 4/5** — 21 tables, 5 BR state machines + 4 jobs, e-Raktkosh, donor status literal collisions.

**Steps:**
1. **Fix DNR literal collision (06 §C.3):** donationStatus=5 means REJECTED and DONATED simultaneously — introduce distinct enums with a data-reconciliation migration.
2. **Resolve serology placement:** source 02 wrote "M16 (serology?)" — evidence points to M15 pathology (bloodscreeningmaster lives in bloodBank masters; serology testing in lab); decide ownership before splitting (02 open Q10).
3. **State machines** (requisition 1→2→3/10; bag flags; indent issue/receive/reject/cancel) → SMALLINT + CHECK enum maps (10 §3.5).
4. **Jobs → scheduled tasks:** decross (00:00), bag expiry (23:00), requisition expiry, donor auto-cancel (72h) — keep exact thresholds (2880 min, orderValidationPeriod, 3 days).
5. **Events:** `BloodRequisitionCreated`, `BloodIssued`; consume PatientAdmitted + LabResultFinalized (serology).
6. **e-Raktkosh:** sync direction/frequency + failure handling open question (02 open Q10); move secrets to vault.
7. **Wave placement:** Wave 3 ≥ Wave 2 events.

**Open questions:** e-Raktkosh direction/frequency; external-storage-center sync; header/detail asymmetry (R7).
