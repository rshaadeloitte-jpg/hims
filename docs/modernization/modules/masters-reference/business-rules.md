# M05/M39 — Business Rules

No BR-* IDs are assigned to masters in `06-business-rules-catalog.md`. Evidenced business rules:

| Rule | Evidence |
|---|---|
| Package types: service / group / medicine / item-category based | 02 §M05 (packagemaster semantics) |
| Tariff package rates class-wise | Tariffpackageclasswiserates |
| Copay per tariff | Tariffcopaydetails |
| Plans can exclude items/services | Planuncovereditemdetails, Planuncoveredservicedetails |
| GST applicability: `isGSTApplicable=true` AND transDate ≥ 2017-07-01 | BaseServiceImpl:521-533 (06 §A9) |
| Generic CRUD validation: `MBase.validate()` on crudadd/update | GenericServiceImpl (04 §2) |

## Rule engine (adjacent, UNKNOWN-08)
- masters/ruleengine (ruleSet.json) + RulesEngineUtil (ROWNUM×8, DUAL — 05 §5) — rules purpose/lifecycle UNKNOWN (02 open Q4).

## Status
- service/tariff active flags; UNKNOWN-12 code lists (02 §M05).
