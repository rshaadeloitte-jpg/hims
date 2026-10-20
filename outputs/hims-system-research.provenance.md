# Provenance: The Whole HMIS System — Research Brief

- **Date:** 2026-08-14
- **Rounds:** 2 research rounds (4 parallel researcher tracks; T2 timed out on round 1 and was re-run successfully on round 2)
- **Sources consulted:** ~280 URLs across 4 research notes (T1: 52, T2: 52, T3: 75, T4: 100 evidence-table entries), plus 2 LOCAL workspace files
- **Sources accepted:** ~130 URLs cited inline in the final brief (numbered [1]–[229], slots 59/127 intentionally unassigned); all load-bearing quantitative claims independently spot-checked by the verifier and reviewer
- **Sources rejected:** none removed — 20+ URLs marked `[unreachable: …]` (HTTP 403 bot-blocks, JS-rendered pages, DNS failures, timeouts) or `[partially verified: …]`; their claims remain documented in the research notes' evidence tables; PDFs cited from metadata only (full-text parsing not performed, per run rules)
- **Verification:** PASS WITH NOTES
  - Verifier pass: citation numbering consistent; representative load-bearing claims re-checked against fetched content
  - Reviewer pass: 0 FATAL; 3 MAJOR (M1 verification-summary overstatement — fixed; M2 AKASA vendor sponsorship of RCM survey — disclosed; M3 unreachable-marker hygiene — standardized + legend extended) and 10 MINOR (8 addressed in revised file; remaining MINOR items noted as accepted)
  - Post-edit on-disk check: `grep` shows 0 hits for 5 removed phrasings and ≥1 hit for all 8 replacement phrasings in `outputs/.drafts/hims-system-research-revised.md`
  - Final artifact verified on disk: `outputs/hims-system-research.md` (74,776 bytes, 474 lines)
- **Plan:** outputs/.plans/hims-system-research.md
- **Research files:**
  - outputs/.drafts/hims-system-research-T1.md (definition/history/taxonomy)
  - outputs/.drafts/hims-system-research-T2.md (functional architecture/modules)
  - outputs/.drafts/hims-system-research-T3.md (standards/interoperability)
  - outputs/.drafts/hims-system-research-T4.md (compliance/vendors/modernization)
  - outputs/.drafts/hims-system-research-draft.md (uncited synthesis)
  - outputs/.drafts/hims-system-research-cited.md (verifier output)
  - outputs/.drafts/hims-system-research-verification.md (reviewer output)
  - outputs/.drafts/hims-system-research-revised.md (final candidate after fixes)

## Blocked / Unverified capabilities (recorded during run)

1. **`feynman_science_database_search`** — not exposed in researcher subagent runtimes; substituted with `feynman paper <doi>` (OpenAlex/Europe PMC metadata) and alphaXiv search. PubMed keyword search via that route not run.
2. **`alpha_search`** — blocked inside the T3 subagent's toolset (worked for T1/T2/T4); T3 used web_search snippets for paper claims instead.
3. **`memory_remember`** — not in the lead tool set; plan was not saved to memory (recorded in plan decision log).
4. **PDF full-text parsing** — intentionally not performed for any source (workflow rule); PDF claims rest on metadata/search snippets and are labeled `(PDF — full-text parsing not performed; metadata verified)`.
5. **Recovery incident:** the T3 research note (51KB) was accidentally overwritten by a 134-byte artifact stub during file consolidation; the full note was recovered from the researcher's session transcript JSONL and verified on disk (361 lines).
6. **T2 first run** timed out after 30 min with empty output; retry used turn/tool budgets and succeeded.
7. **HIPAA Security Rule NPRM (Dec 2024)** — existence confirmed via HHS page; NPRM text not fetched (details unverified).
8. **KLAS report pages** paywalled; figures used via trade-press re-reporting (Healthcare Dive, hitconsultant, Fierce, KLAS blog).
