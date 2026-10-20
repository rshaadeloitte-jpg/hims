# M17 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| BloodBankServiceImpl | `/bloodtestmaster` | **115 endpoints**: donor reg, requisition, cross-match, issue, discard, bag status; BR-BLD evidence :2273-2277, :6290-6317, :7657-7701; Solr index :1429-1625 |
| PatientRequisitionServiceImpl | `/patientrequisition` | requisitions (patient/external); Solr index :746-854 |

## Endpoint scale (03 §5)
- bloodtestmaster (115) is the 5th-largest API bucket; 16 blood-bank report routes.

## Notes
- donorRegistartion schema note: "donorRegistartion" is the actual schema name (typo in source, preserved).
