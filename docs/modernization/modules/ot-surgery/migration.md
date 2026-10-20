# M12 — Migration

**Target service:** 11. OT & Surgery — **Wave 3 (Clinical Care)** (10 §6).

**Complexity: 3/5** — scheduling + billing integration; moderate surface.

**Steps:**
1. **OT scheduling service** — slot/conflict rules (checkOTAvailability, checkDoctorAllreadyBookedForSameTime) as service invariants; `ProcedureScheduled`/`SurgeryCompleted` events.
2. **Pre-billing flag (BR-OT-002)** must be preserved: `otprocedure.pre.billing` config → direct (consumed) charges; doctor-share skip for OT tariff services — coordinate with Billing saga (M04).
3. **Procedure master + checklists** → reference data with versioning (procedurechecklist* versioning).
4. **CSSD/Inventory coupling:** kit requests (M11) and item consumption (M13) become events: ProcedureScheduled → CSSD request; consumption events → Inventory.
5. **Anesthesia notes** stay with OT context (pre/PACU/post); tiuxml anesthesia forms reuse per M40 strategy.
6. **Wave placement:** Wave 3 ≥ Wave 2 events (orders/billing).

**Exit criteria (Wave 3):** OrderCreated (procedure) stream consumed; SurgeryCompleted produced; OT billing estimation contract live.
