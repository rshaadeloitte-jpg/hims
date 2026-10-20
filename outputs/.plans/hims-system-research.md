# Deep Research Plan: Whole HMIS System

**Slug:** `hims-system-research`
**Date:** 2026-08-14
**Topic:** Comprehensive research on the whole HMIS (Health/Health Information Management System) — definition, history, functional architecture, standards, compliance, vendor/technology landscape, and modernization approaches, including how this applies to the legacy HMIS modernization context documented in this workspace (`docs/modernization/`).

---

## Key Questions

1. **Definition & scope** — What is an HMIS/HIMS? How does it relate to and differ from HIS, EHR, EMR, LIS, RIS, and national health information systems?
2. **History & evolution** — How did hospital/health information management systems develop (paper → departmental systems → integrated suites → cloud/API era)?
3. **Functional architecture** — What are the core modules and workflows of a typical HMIS (patient registration/ADT, billing & finance, pharmacy, laboratory, radiology, blood bank, inventory, outpatient/inpatient, reporting)? What are common legacy architecture patterns?
4. **Standards & interoperability** — HL7 v2/v2.x, FHIR, DICOM, SNOMED CT, LOINC, ICD-10/11, IHE profiles, OpenMRS/Bahmni ecosystems; how standards enable module integration and data exchange.
5. **Security, privacy & compliance** — HIPAA, GDPR and other regulatory regimes; RBAC, audit logging, data residency; consequences of non-compliance in health systems.
6. **Technology & vendor landscape (2026)** — Commercial vendors (Epic, Oracle Health/Cerner, Meditech, etc.), open-source options (OpenMRS, Bahmni), cloud/SaaS adoption, AI/ML features in modern HMIS.
7. **Modernization approaches** — How legacy HMIS platforms are modernized: strangler-fig migration, microservices, API-first design, database migration, frontend modernization; documented industry patterns and risk factors.

## Evidence Needed

- **Papers:** HMIS definitional/architectural literature, interoperability studies, security/compliance research (via `alpha_search` / paper metadata / abstracts — no PDF parsing).
- **Web (current):** WHO/national HMIS guidance; HL7/FHIR/DICOM official docs; HIPAA/GDPR summaries; 2025–2026 vendor and market landscape; OpenMRS/Bahmni documentation; modernization case studies.
- **Local workspace:** `docs/modernization/` tree (module catalogs, business rules, architecture docs) as a case-study reference for the legacy HMIS context. (Gathered in a later round; not evidence for external claims.)
- **Minimum queries per angle:** ≥3 distinct search queries per researcher task.

## Scale Decision

**Broad, multi-faceted survey → 4 `researcher` subagents** (parallel, `failFast: false`), followed by lead synthesis, `verifier`, then `reviewer`.

Rationale: the topic spans definition/history, functional architecture, standards, compliance, and market/modernization — clearly beyond a narrow "what is X" explainer, and each facet has distinct source bases (papers vs web vs standards docs).

- T1 — Definition, history, taxonomy (HMIS vs HIS/EHR/EMR/LIS/RIS), global context
- T2 — Functional modules, workflows, legacy architecture patterns
- T3 — Standards & interoperability (HL7/FHIR/DICOM/terminologies/IHE/open ecosystems)
- T4 — Security/compliance + 2026 vendor/technology landscape + modernization approaches

## Task Ledger

| # | Task | Owner | Status |
|---|------|-------|--------|
| 0 | Write plan artifact | lead | done (this file) |
| 1 | Write per-researcher briefs `outputs/.plans/hims-system-research-T1..T4.md` | lead | pending (after approval) |
| 2 | T1 research → `outputs/.drafts/hims-system-research-T1.md` | researcher | done (315 lines; note: `feynman_science_database_search` blocked, substituted `feynman paper`) |
| 3 | T2 research → `outputs/.drafts/hims-system-research-T2.md` | researcher | done on 2nd run (293 lines; first run timed out after 30 min with empty output; retry was time-boxed with turn/tool budgets) |
| 4 | T3 research → `outputs/.drafts/hims-system-research-T3.md` | researcher | done (361 lines; recovered from session transcript after a bad `cp` overwrote the note with a 134-byte artifact stub) |
| 5 | T4 research → `outputs/.drafts/hims-system-research-T4.md` | researcher | done (452 lines) |
| 6 | Update ledger + verification log; reconcile notes | lead | done (this edit) |
| 7 | Draft synthesis → `outputs/.drafts/hims-system-research-draft.md` | lead (no delegation) | done (32KB) |
| 8 | Cite → `outputs/.drafts/hims-system-research-cited.md` | verifier subagent | done (470 lines; URL checks ~130 refs; unreachable markers added) |
| 9 | Review → `outputs/.drafts/hims-system-research-verification.md` | reviewer subagent | done (0 FATAL, 3 MAJOR, 10 MINOR) |
| 10 | Fix FATAL/MAJOR; produce `-revised.md` if needed; on-disk verification | lead | done — 3 MAJOR (M1–M3) + 8 MINOR fixes applied → `hims-system-research-revised.md` (474 lines); post-edit `grep` verified 0 old / ≥1 new for all 8 changed passages |
| 11 | Deliver `outputs/hims-system-research.md` + `outputs/hims-system-research.provenance.md` | lead | done (both verified on disk; 5 required artifacts present) |

## Verification Log

| Date | Check | Result |
|------|-------|--------|
| 2026-08-14 | Plan written to `outputs/.plans/hims-system-research.md` | PASS (user approved) |
| 2026-08-14 | All 4 research notes exist on disk (T1 41KB, T2 49KB, T3 52KB, T4 63KB) | PASS |
| 2026-08-14 | T2 first run timed out (30 min, empty output); retry succeeded with turn/tool budget | PASS WITH NOTES |
| 2026-08-14 | T3 note accidentally overwritten by `cp` of 134-byte stub; recovered full 361-line note from session transcript JSONL and re-verified on disk | PASS WITH NOTES |
| — | Cited draft exists on disk (verifier output verified) | PASS (470 lines, `hims-system-research-cited.md`) |
| — | Reviewer pass completed (0 FATAL; M1–M3 fixed); post-edit `rg`/`grep` proof passed (0 old / ≥1 new on 8 passages) | PASS |
| — | Final `outputs/hims-system-research.md` + provenance exist on disk | PASS (74,776 B brief + 3,675 B provenance; all 5 required artifacts verified via `ls`) |

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-08-14 | Scale = broad survey with 4 researcher subagents | Topic spans ≥4 distinct evidence domains (definition/history, architecture, standards, compliance+market) |
| 2026-08-14 | No PDF parsing; cite PDF URLs from metadata and mark full-text parsing blocked | Workflow rule: avoid crash-prone PDF parsing |
| 2026-08-14 | `memory_remember` not available in visible tool set → plan not saved to memory; noted here instead | Tool not present; instruction says continue without it |
| 2026-08-14 | Local `docs/modernization/` used only as case-study context, not as external evidence | Internal artifacts cannot substantiate claims about the wider industry |
| 2026-08-14 | T2 researcher re-run with `turnBudget` (18+3) and `toolBudget` (soft 30 / hard 45) after first run timed out exploring tools | Bounded execution prevents repeat 30-min timeouts |
