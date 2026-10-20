# M02 — Migration

**Target service:** 2. Appointment & Encounter (OPD) — Wave 1 (10 §6). Queue sub-part → 29. Queue & Kiosk.

**Complexity: 4/5** — rationale: VisitStatus split-brain across two tables, Solr encounter retention, slot-collision rules, NHNC/ABDM variants, Redis queue state.

**Key migration steps:**
1. **Unify visit lifecycle** — VisitStatus 11/12 written to both T_Appointment and T_Encounter by different services (06 §C.1); kiosk/queue reads `getLastVisitStatus`. Target: single encounter state machine; emit `EncounterCreated`, `AppointmentBooked`, `VisitClosed` events.
2. **Encounter index:** keep Solr (preserve boost semantics), move indexing to event-driven pipeline; reconsider 3-day retention (legal retention gap — archive instead of delete, 07 §1.7).
3. **Queue state:** QMS Redis queues are ephemeral by design; keep short-lived Redis in target (never source-of-truth).
4. **Cabin/doctor queue tables** move with OPD service; `T_OPDSTATIONMASTER`, `T_TABLETMAPPING` → Queue & Kiosk.
5. **NHNC handling** — flag-gated variants (walkinVisitNhnc, appointmentListNhnc) must be re-implemented as config-driven flows, not hardcoded.
6. **Waves:** OPD sits in Wave 1 with Patient (1) and ADT (3); needs PatientRegistered stream.

**Exit criteria (10 §6 Wave 1):** PatientRegistered/EncounterCreated/PatientAdmitted streams live; Solr fed by events.
