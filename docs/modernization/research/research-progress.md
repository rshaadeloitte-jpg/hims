# Research Progress — iHMS Modernization Research Council

> Living status board. Updated: 2026-08-14 (validation round complete).

## Phase Status

| Phase | Status | Notes |
|---|---|---|
| Phase 0 — Initial baseline | ✅ DONE | repository-snapshot.md; fingerprint re-verified by V1–V6 |
| Phase 1 — Parallel discovery (A1–A9) | ✅ DONE | agent-01…09 findings in `_discovery/` |
| Phase 2 — Cross-layer correlation | ✅ DONE | reconciliation-notes.md; TRACEABILITY-MATRIX |
| Phase 3 — Adversarial validation | ✅ DONE | V1–V6 independent re-verification; contradiction-register CONTR-011…020 |
| Phase 4 — Business domain synthesis | ✅ DONE | 02-module-catalog, MODULE-INVENTORY, FEATURE-MATRIX |
| Phase 5 — Current-state architecture | ✅ DONE | 01, 04a/04b, 05, 03, 08, architecture/ |
| Phase 6 — Target architecture options | ✅ DONE | 10-target-architecture + master plan §17 (3 options) |
| Phase 7 — Migration design | ✅ DONE (PRELIMINARY) | STRANGLER, POSTGRESQL, KAFKA, MICROSERVICE-CATALOG, 09-React |
| Phase 8 — Migration waves | ✅ DONE (PRELIMINARY) | MIGRATION-WAVES W0–W6 |
| Phase 9 — Execution backlog | 🚧 IN PROGRESS | EXECUTION-BACKLOG.md |
| Phase 10 — Final red-team review | 🚧 IN PROGRESS | FINAL-RED-TEAM-REVIEW.md |

## Validation Round Results (2026-08-14)

| Claim | Verdict | Where |
|---|---|---|
| 659 @CrudApi entities | CONFIRMED | E-04, CONTR-005 |
| 137 CXF beans | CONFIRMED | E-04 |
| 3,060 endpoints | CORRECTED → 3,041 verbs / 3,167 @Path | CONTR-005 |
| 719 procs | CONFIRMED (702 P + 16 F, no packages) | E-08, CONTR-002 |
| ~30 live procs | **DISPROVEN → 918 referenced** | CONTR-001 |
| 843 tables | UNVERIFIABLE → 894 @Table names | CONTR-003, UNKNOWN-031 |
| 533 routes | CORRECTED → 636 | CONTR-004 |
| No backend authz | CONFIRMED (+new criticals) | E-06/E-07 |
| Solr-in-transaction | CONFIRMED | E-16 |
| Redis active impl | MemoryCacheImpl (repo); prod UNKNOWN | CONTR-008 |
| Mega-transaction | CONFIRMED (HL7 leg disabled) | E-10 |
| T_BILL etc. no entity | CONFIRMED | E-11 |
| VisitStatus magic numbers | CONFIRMED | E-12 |
| God classes | CONFIRMED (rankings corrected) | E-13, CONTR-017 |

## Definition of Research Complete — Checklist

- [x] Repository deeply inventoried (Phase-0 + V1–V6 double-pass)
- [x] Business modules discovered (40)
- [x] Features mapped (289)
- [x] Routes mapped (636)
- [x] FAUG schemas mapped (1,308) + templates (2,943)
- [x] JavaScript logic mapped (71 controllers; billing arithmetic located)
- [x] APIs mapped (5 routing surfaces; 222 URLs triaged → UNKNOWN-005)
- [x] Services mapped (155 files / 135 Impl)
- [x] DB objects mapped (894 @Table names; sequences; 719 proc files)
- [x] Stored procedures classified (918 referenced vs remainder — per-proc triage pending at W0)
- [x] Statuses understood (dictionary A1–A9; magic-number gaps registered)
- [x] Reports catalogued (REPORT-INVENTORY)
- [x] Integrations catalogued (28+ with config+code evidence)
- [x] Security mapped (E-06/E-07; frontend-only RBAC; critical findings)
- [x] Solr mapped (6 collections, sync paths)
- [x] Deployment mapped (build/deploy in 01 + OBSERVABILITY)
- [x] Critical workflows traced end-to-end (18 workflow docs + TRACEABILITY-MATRIX)
- [x] Business rules extracted (39 + status dictionaries)
- [x] Shared transactions identified (mega-transaction; 7 REQUIRES_NEW sites)
- [x] Shared tables identified (V6 writer maps)
- [x] Target boundaries evaluated (29 services PRELIMINARY; ownership validation in W2)
- [x] Architecture alternatives compared (3 options in master plan §17)
- [x] Target architecture selected (PRELIMINARY recommendation: hybrid strangler → modular monolith-first, §18)
- [x] Migration waves calculated (W0–W6)
- [x] Test strategy established (TEST-STRATEGY L0–L5)
- [x] Rollbacks established (STRANGLER-MIGRATION; per-wave rollback)
- [x] Unknowns recorded (35)
- [x] Risks recorded (40+; new security risks added by V2)
- [x] Final red-team review (FINAL-RED-TEAM-REVIEW.md)

## Blockers to Execution

1. Oracle-side verification (UNKNOWN-001/004/011/014/015/016/031/032) — needs read-only DB access.
2. Deployed-environment verification (UNKNOWN-008/009/010/013) — needs staging/prod access.
3. Contract fixtures from running legacy (TEST-STRATEGY §3).
4. 222 unmatched URL triage (UNKNOWN-005).
5. Security remediation baseline (secrets, password validator, backend authz) — prerequisites for any cutover.
6. Live-proc golden fixtures (scope now 918-referenced, not 30).
## Tooling: wigolo research integration (2026-08-14)

- Installed wigolo v0.2.1 globally (MCP server in `opencode.json` + 26 skill packs to `~/.agents/skills` / `~/.claude/skills`).
- Search engines: core multi-engine backend (mdn/bing/duckduckgo tested live); chromium browser pool; ML reranker `Xenova/ms-marco-MiniLM-L-6-v2` + BGE embeddings; sqlite-vec store; Firefox engine not installed (retryable).
- LLM: provider `gemini`, model `gemini-3.5-flash-lite` (2.5 series retired), key in MCP env block. Verified live (RGHS + EclipseLink->Hibernate searches returned evidence payloads).
- Impact: evidence register may now cite live sources with citation IDs / source spans for UNKNOWN resolution. Requires opencode restart to load MCP.
- [x] Web research round 1 via wigolo: ABDM (W-01..06), HCX (W-10..16), RGHS (W-20..23) — WEB-RESEARCH-INTEGRATIONS.md; e-Aushadhi + Chiranjeevi: no public docs (engine degraded; retry candidates)
