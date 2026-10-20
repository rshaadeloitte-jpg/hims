# M07 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| PharmacyServiceImpl | `/pharmacy` | 98 endpoints: getMedications, getItemCurrentStock, dispensing, sale/return, labels, `resetPharmacyUserSession`; **stock dual-write :2140** |
| PrescriptionMasterServiceImpl | `/prescriptionmaster` | prescription master |
| DrugServiceImpl | `/drug` | drug master |
| MedicationServiceImpl | `/medication` | addItemPriceNew, dispenseItemSale (BR-PHAR-001 evidence :799-821) |
| PrescriptionDetailsServiceImpl | `/prescription` | prescription details |
| ItemSalesServiceImpl | `/itemsales` | item sales |
| AddServiceOrderServiceImpl | `/serviceOrder` | addPharmacyItemtoChargeForIP (pharmacy charge for IP billing) |
| MasterCachedServiceImpl | `/master` | filterPharmDiagnosis, getMedsAndItemStockList |
| InventoryEAushadiIntegrationImpl | `/eAushadhiIndent` | item stock sync (M38) |

**Endpoint scale (03 §5):** pharmacy (98 endpoints) is the 7th-largest API bucket.
