# M13/M14 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| e-Aushadhi (RMS) | Both | InventoryEAushadiIntegrationImpl:110-1963; indent create/issue/QC/return; stock reconciliation; item-master sync 18:00; auto-indent; **Basic-auth header hard-coded**; failure → ack DTOs + reTriggerEAushadhiIndent AOP hook (ServiceLayerAspect:1204) |
| Janta / MDM | Both | syncJantaItemMaster; MdmServiceImpl `/mdm`; master data sync |
| Kaysons (wholesaler) | Out | via e-Aushadhi RMSC (InventoryEAushadiIntegrationImpl:179-1923) |
| RMRS (M23) | Out | PO requests → funds (raiseRMRSPORequest, showRMRSPurchaseOrderRequestListSchema) |
| Solr | In-process | item collection: flag-based drainer syncItemsToSolrJob (5s), searchAllItemsInSolr; manual startItemSyncingToSolr |

## Redis
- `INV_RI_<indentId>` receive lock (60s); item master cache; kill-switches (`disableItemIndexing` variants).

## Scheduled jobs
- syncItemsToSolrJob (5s); InventoryJobSchedule (5 jobs); eAushadiItemStockUpdate.schedule (04:00); e-Aushadhi item sync 18:00.
