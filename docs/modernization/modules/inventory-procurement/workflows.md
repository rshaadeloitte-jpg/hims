# M13/M14 — Workflows

## Indent workflow (M13, 02 §M13)
1. Indent (ward/dept) → freeze → level-1 approval → level-2 approval → issue → receipt/consumption → reorder → (expiry/damage return to supplier).
- Indent states per schema names: draft (Editable) / freezed / approved (level-1) / second-level-approved / issued.

## Procurement workflow (M14, 02 §M14)
1. PR → quotation collection → comparison (`compareQuotationSchema`) → PO (approve/freeze) → GRN → QC (`qcStatusUpdate`) → stock receipt → supplier invoice (indent invoice) → payment.
- Custom/in-house/consignment variants; renegotiation path (`showRenegotiateSchema`).

## e-Aushadhi flow (07 §3.2)
- Indent lifecycle: create → issue → QC status → return (final); stock reconciliation; item-master sync scheduled 18:00; auto-indent from main store (unit-wise cron). Basic-auth header hard-coded in properties; timeout 10s; failure → ack DTOs + `reTriggerEAushadhiIndent` AOP hook (ServiceLayerAspect:1204).

## Stock concurrency (10 §3.4)
- T_ITEMSTOCK decrement must use row-level locking (`SELECT … FOR UPDATE` / `UPDATE … WHERE returning`) same as legacy procs; `INV_RI_<indentId>` receive lock (Redis 60s, ServiceLayerAspect:1192-1201).

## Solr item sync (07 §1.2/1.3)
- item collection **not realtime**: flag-based drainer `syncItemsToSolrJob` (5s fixedDelay, SOLR_STATUS=0, batches of 500, 90s cap) + manual REST `startItemSyncingToSolr` (MasterCachedServiceImpl:12592).
