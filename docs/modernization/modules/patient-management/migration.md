# M01 — Migration (from 10-target-architecture.md)

**Target service:** 1. Patient Identity & Registration (Wave 1 — Admission Front, 10 §6)

**Owned entities (target):** T_REGISTRATION(+DETAILS), T_PATIENTSPONSOR, T_PATIENT_ABDM, T_REGISTRATIONQUEUE; emits `PatientRegistered`; reads IAM + ABDM (Interop).

**Complexity: 4/5** — rationale: patient identity hub feeding all modules; `T_REGISTRATION*` has no entity mapping (schema only in Oracle); Solr is load-bearing for search/duplicates/ABDM consent matching; temp→permanent MRN promotion; ABDM linkage state.

**Migration notes:**
1. **Extract T_REGISTRATION schema from Oracle first** (recon §7) — no JPA entity exists.
2. **Solr must remain** (07 §1): search + duplicate detection + ABDM consent discovery query it; preserve boost semantics (mrno^18, mobile^16, janaadhar^17…); keep Redis `current_pat_` mirror or replace with DB-backed reads.
3. **Sequences:** preserve MRN series / temp-MRN auto-code proc (`cims_getlatestautocodeandincremetitbyone`); PG `CREATE SEQUENCE "<t>_seq"` per table.
4. **Strangler:** all modules join T_REGISTRATION directly today → introduce `/patients` REST + PatientRegistered event as read-model (10 §2.2).
5. **Events:** PatientRegistered → feeds Search (Solr via Kafka), OPD/ADT, Lab, Interop (HL7/ABDM).
6. **Key cutover risk tables:** T_ENCOUNTER, T_REGISTRATION family (10 §5).

**Open questions:** MRN temp→permanent promotion timing; ABDM consent state machine; encounter Solr 3-day retention impact on patient search.
