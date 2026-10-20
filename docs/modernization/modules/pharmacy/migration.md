# M07 — Migration

**Target service:** 9. Pharmacy & Dispensing — **Wave 3 (Clinical Care)** (10 §6).

**Complexity: 4/5** — stock dual-write with inventory, dispensing state machine, VisitStatus 12 coupling, compounding/IV.

**Steps:**
1. **Resolve stock ownership first (10 §7 Q1):** Pharmacy must consume Inventory stock APIs (`/stock`) + InventoryUpdated events; eliminate `PharmacyServiceImpl.java:2140` dual-write.
2. **Dispensing state machine** → service logic with documented enum map (legacy char states → SMALLINT + CHECK, 10 §3.5); keep dispense flags isverified/isdispensed/ispartialdispense.
3. **VisitStatus=12 coupling (BR-PHAR-001)** — must coexist with the unified visit lifecycle fix (M02); event `DispensingCompleted` → OPD service.
4. **Procs:** CIMS_ADDITEMSALESDETAILS_NEW / CIMS_ADDITEMSTOCK / CIMS_GETPRESCRIPTIONWARDDETAILS* → PL/pgSQL or service code on extraction path.
5. **PrescriptionCreated / DispensingCompleted events**; BCMA (M30) consumes dispensed meds.
6. **Wave placement:** Wave 3 ≥ Wave 2 (billing) events; Inventory ≥ Pharmacy decision (10 §6 dependencies).

**Exit criteria (Wave 3):** LabOrderCreated/LabResultFinalized/PrescriptionCreated streams live; stock API contract live (Pharmacy uses it).
