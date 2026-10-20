# M07 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| e-Aushadhi | Both | item stock sync (eAushadiItemStockUpdate.schedule 04:00); stock/QC/return via InventoryEAushadiIntegrationImpl |
| BCMA (M30) | In | dispensed meds → bedside barcode verification; missing-dose requests |
| ABDM PHR | Out | patPrescriptionPHR.vm (PHR prescription record) |
| WhatsApp/SMS | Out | pharmacy reports via queue (triggerWhatsappReportsInQueue) |
| Solr | In-process | item collection search (searchAllItemsInSolr — pharmacy/linen/gas/asset flags, 10,000 rows); patient search for OP dispense |

## Redis
- Item stock snapshots (lookup-only per 10 §2.1 row 9); `current_pat_` mirrors; `crr_Default_Store_id` gvar drives store-scoped queries.

## Solr item index detail (07 §1.2)
- item collection (~75 fields: code/name/brand, molecule, category, purchase rate/MRP/VAT, inventory code, pharmacy/linen/medical-gas/asset flags, kit/compound, batch/expiry); **not realtime** — flag-based drainer `syncItemsToSolrJob` (5s, SOLR_STATUS=0, batches 500, 90s cap) + manual `startItemSyncingToSolr`.
