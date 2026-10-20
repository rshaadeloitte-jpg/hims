# FINAL VERIFICATION 5 — Tranche 5 Wave-5 Remainder (WP-R1 Radiology, WP-R2 Pharmacy, WP-R3 CSSD, WP-R4 RCM, WP-R5 RMRS)

_Verified 2026-08-23 by the prime agent (independent re-runs). Ground truth: outputs/implementation/GAP-REGISTER-5.md (MICROSERVICE-CATALOG #8/#9/#15/#23/#24, KAFKA-EVENT-CATALOG §3 deferrals, BR-PHAR-001/002). Execution: 5 implementer children + 3 repair children (pharmacy/rcm/rmrs first attempts died silently mid-task; repairs dispatched with exact surefire/compiler diagnostics)._

## 1. Builds (all re-run independently by overseer, sequential, exit codes observed)

| Service | Command | Result |
|---|---|---|
| radiology | `mvn -f services/radiology-service/pom.xml test` | **exit 0 — 25/25** (12 unit + 12 MockMvc + 1 PG16) |
| pharmacy | `mvn -f services/pharmacy-service/pom.xml test` | **exit 0 — 36/36** (16 unit + 16 MockMvc + 4 PG16) |
| cssd | `mvn -f services/cssd-service/pom.xml test` | **exit 0 — 30/30** (18 unit + 9 MockMvc + 3 PG16) |
| rcm | `mvn -f services/rcm-service/pom.xml test` | **exit 0 — 50/50** (27 unit + 15 MockMvc + 2 PG16 + 6 TID unit) |
| rmrs | `mvn -f services/rmrs-service/pom.xml test` | **exit 0 — 33/33** (20 unit + 13 MockMvc + 6 PG16) |

Total: **174 tests, 0 failures, 0 errors, 0 skipped. No tests deleted or disabled to force green** (repairs ADDED missing test layers; rcm repair fixed broken tests against real production classes).

## 2. Spec/controller parity (grep-verified, 1:1)

| Service | Spec | Code ops | Parity |
|---|---|---|---|
| radiology | openapi/radiology-service.yaml | 7 | **OK** |
| pharmacy | openapi/pharmacy-service.yaml (NEW) | 8 | **OK** |
| cssd | openapi/cssd-service.yaml | 7 | **OK** |
| rcm | openapi/rcm-service.yaml (NEW) | 8 | **OK** |
| rmrs | openapi/rmrs-service.yaml | 11 | **OK** |

41 operations total, zero mismatches in either direction.

## 3. Event policy verification (register-mandated DEFERRALS)

All five services have DEFERRED/REJECTED/PROPOSED events per KAFKA-EVENT-CATALOG §3 — the binding requirement was NEGATIVE outbox behavior, verified:

- radiology: RadOrderCreated DEFERRED — zero outbox tables/rows in ihms_rad asserted at 5 points (booking, lifecycle, rejected creates).
- pharmacy: DispensingCompleted PROPOSED — outbox count stays 0 through every flow incl. stock-failure rollback.
- cssd: KitIssued/SterilizationCompleted REJECTED phase 1 — outbox empty after boot, every rejected guard, full lifecycle, failure path (7 assertion sites).
- rcm: ClaimSubmitted DEFERRED — structural (no service takes an OutboxService dependency, reflection-pinned) + behavioral (outbox count == 0 after every flow).
- rmrs: CollectionPosted PROPOSED — `assertThat(outboxCount()).isZero()` on success AND every rejected create.

No service silently invented an event.

## 4. Business-rule verification

- **BR-PHAR-001** (pharmacy): full/partial/boundary(last-line) dispense transitions pinned at unit AND PG16 integration level; sale header dispensed flag + status machine.
- **BR-PHAR-002** (pharmacy): full-qty close (PendingQty=0, IsClosed='1'), partial decrement, master closure — pinned on real rows; local-projection write-back deferred post-saga (documented).
- **CSSD lifecycle**: assemble → cycle (ASSEMBLED/STERILE-reprocess only) → PASS→STERILE / FAIL→REJECTED (unusable, issue-guarded) → single-active-issue — all guards pinned.
- **RCM NHCX status machine** 0..3 with callback idempotency by correlation_id; TID uniqueness enforced at API (409) and DB (unique index backstop test).
- **RMRS balanced postings**: SUM(debit)=SUM(credit) per collection with exactly-one-sided entries — pinned at integration level incl. DB CHECK; head/ledger consistency validated; ROWNUM legacy pagination replaced with limit/offset (documented).

## 5. Repair-cycle disclosure (honest accounting)

Three first-attempt workers completed silently mid-task (pharmacy, rcm, rmrs) — a repeat of the tranche-3 WP-8 failure mode. Each was diagnosed from compiler/surefire output and repaired by a dedicated child:
- **pharmacy**: missing import (main compile); two wrong L0 stubs; register-mandated controller/integration test layers were ABSENT and were added.
- **rcm**: main compiled but test-compile was broken (phantom OutboxRepositoryPlaceholder, unimported enums, 'Nhxs' typos, assertj-db misuse); ALSO caught a real schema blocker — duplicate 'status' column in V1__init_ihms_rcm.sql that would have failed Flyway (renamed check_status/request_status per sibling convention).
- **rmrs**: integration fixture hardcoded generated sequence ids (PG sequences don't roll back with test tx); ALSO caught two real production bugs — AccountingEntryRepository Object[] vs List<Object[]> ClassCastException, and PG-16 untyped-null failures in nullable filters (COALESCE rewrite, semantics identical).

All fixes are documented in the WP reports; no validation logic was weakened.

## 6. Cross-service coverage disclosure

Pharmacy's stock decrement calls item-service's stock API synchronously via RestClient, verified with MockRestServiceServer (unit) and a real-HTTP wire-shape stub (integration) — NOT against a live item-service instance (same independent-Maven-units rationale as WP-13; disclosed in WP-R2-pharmacy.md). RCM's external NHCX/HCX gateway calls are out of scope per register; requests persist with correlation ids and callbacks are simulated locally (disclosed).

## 7. Explicit deferrals (per GAP-REGISTER-5, none silent)

Interop #25, Reporting #26, Search/Solr #27, Govt Programs #28 remain unbuilt with documented rationale (external-gateway/infra/ground-truth-verification blockers). Saga wiring (ordering→billing) remains post-saga per register-4 scope decision.

## Verdict

**ALL TRANCHE-5 ACCEPTANCE CRITERIA PASS.** Five builds exit 0 (174/174); all five OpenAPI specs 1:1 with controllers; negative outbox behavior pinned everywhere events are deferred; WP-R1..R5 reports exist; hive/board.md tranche 5 CLOSED. Cumulative program: 17 WPs · 15 new microservices + iam rebuild · 2 extended services · 498 tests green (324 + 174).
