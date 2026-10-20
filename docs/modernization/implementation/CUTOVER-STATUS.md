# CUTOVER-STATUS — iHMS Modernization

> Records per-module cutover state (strangler rule: default-legacy, flip-per-module; STRANGLER-MIGRATION §2.1/§3).
> **Updated**: 2026-08-20

## Principle
- All production traffic remains 100% on the legacy monolith until a module's wave exit criteria pass (MIGRATION-WAVES §5) + rollback drill (MIG-C03).
- Slice S0 introduces NO traffic changes — new platform seed, shadow mode only.

| Module / Slice | Legacy traffic % | Target traffic % | Shadow? | Flip planned | Rollback armed? | Notes |
|---|---|---|---|---|---|---|
| All M01–M40 | 100% | 0% | — | per wave gates | — | monolith untouched |
| S0 platform seed | n/a | n/a | yes (new) | W1 masters (MIG-010) | n/a | no data ownership switched |
| W1 item-service (MIG-012) | 100% (legacy Inventory/Pharmacy multi-writers) | 0% | yes (new, dev only) | gated on W0 long-poles + W4/W5 stock-writer retirement (D-1 sign-off) | n/a | shadow endpoints + outbox events only; no T_ITEMSTOCK traffic moved |
| W2 grievance-service (MIG-020) | 100% (legacy mdm grievance endpoints) | 0% | yes (new, dev only) | gated on W0 long-poles + W2 exit criteria (gateway flip, React screens, reconciliation) | n/a | shadow endpoints only, REST-only (no outbox — GrievanceRaised rejected phase 1); implementation-without-cutover per IMP-DEC-031 |
| W2 waste-service (MIG-023 sub-slice, S19 BMW) | 100% (legacy BioMedicalWasteManagementServiceImpl) | 0% | yes (new, dev only) | gated on W0 long-poles + W2 exit criteria (gateway flip, React screens, reconciliation) | n/a | shadow endpoints only, REST-only (no outbox — BMWRecorded rejected phase 1); implementation-without-cutover per IMP-DEC-031 |
| W2 linen-service (MIG-023 sub-slice, S16 linen) | 100% (legacy LinenLaundryServiceImpl) | 0% | yes (new, dev only) | gated on W0 long-poles + W2 exit criteria (gateway flip, React screens, reconciliation) | n/a | shadow endpoints only, REST-only (no outbox — no linen events in KAFKA-EVENT-CATALOG); implementation-without-cutover per IMP-DEC-031 |
| W2 transport-service (MIG-023 sub-slice, S17 transport) | 100% (legacy TransportServiceImpl) | 0% | yes (new, dev only) | gated on W0 long-poles + W2 exit criteria (gateway flip, React screens, reconciliation) | n/a | shadow endpoints only, REST-only (no outbox — TransportRequested rejected phase 1 in KAFKA-EVENT-CATALOG); implementation-without-cutover per IMP-DEC-031 |
| W2 diet-service (MIG-023 sub-slice, S18 diet) | 100% (legacy DietServiceImpl) | 0% | yes (new, dev only) | gated on W0 long-poles + W2 exit criteria (gateway flip, React screens, reconciliation) | n/a | shadow endpoints only, REST-only (no outbox — diet produces no catalog events); implementation-without-cutover per IMP-DEC-031 |

## Cutover protocol reminders (per table move, STRANGLER §5 P3)
1. Freeze writes from old side
2. Event-forward new writes (outbox)
3. Backfill + CDC drain
4. Cut reads
5. Verify (row counts, checksums, spot business transactions)
6. Arm rollback switch

Critical cutover tables (strictest protocol): T_ENCOUNTER, T_BILL, T_ITEMSTOCK (MIGRATION-WAVES §4.12).
