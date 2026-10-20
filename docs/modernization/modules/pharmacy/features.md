# M07 — Features (02 §M07)

| Feature | Evidence |
|---|---|
| Prescriptions (doctor/nurse/uploaded) | addPrescriptionSchema, addMedicationSchema(+FromNurse), prescriptionUploadSchema; prescription scan & upload |
| Dispensing (OP/IP/ward/counter) | dispensingtype; opIpPharmacyBillSchema, ipPharmacyBillingSchema; pharmacyCounterSaleSchema |
| Medication administration support (BCMA link) | patientMedicationLogSchema adjacency; IHR_MEDICATION_TMP (M30) |
| Item sales & returns | itemSaleListSchema, itemSaleReturnListSchema; pharmacySaleReturnReport.vm |
| IV/compounding | ivPharm, prepareCompound routes; T_INFUSIONMIXING; compoundingLable |
| Cart-fill & ward pick lists | cartFillFlow/:ward_id/:patient_dfn; wardPickListOrders; cartfillDetail.vm |
| Pharmacy billing (OP/IP separate) | ipPharmacyBillingSchema; ipBillSummeryWithoutPharmcy.vm |
| Interventions | pharmacyInterventionReport; pharmInterventionreport route |
| Home medications | ipHomeMedsForm |
| External patients | externalPatient (opPharmacyAddPatient) |
| Store/stock queries | getItemAndItemBatchListByStoreId, getItemCurrentStock |
| Drug mapping & groups | pharmacyDrugMapping, addItemGroup, updateItemGroupSchema, addItemPriceNew |
| Pharmacy patient coversheet | showPharmacyPatientCoversheetSchema |

## Client-side logic (03 §2.5)
- Prescription→dispense workflow with verify/dispense steps; cart fill per ward/patient; IV admixture; compound preparation; return flows (IP return with print).
