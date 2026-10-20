# M12 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| Inventory (items/equipment) | Out | OT item/equipment issues; addProcedureEquipment; procedureitemdetails/procedureequipmentdetails masters; consumption events (10 §2.1 row 11) |
| CSSD (M11) | Both | procedure CSSD kit requests and issue (procedureCssdRequest/Issue) |
| Billing (M04) | Out | OT charges + bill estimation (addOTBillEstimationSchema; BR-OT-002) |
| Nursing (M09) | In | surgery order list from nursing (SurgeryOrderList); isOtProcessDone filter |
| Solr | In | patient search (10 §2.1 row 11: reads patient via Search) |
| tiuxmls (M40) | In | anesthesia/OT forms (anesthesia-related tiuxmls) |

## Scheduled jobs: none evidenced.
