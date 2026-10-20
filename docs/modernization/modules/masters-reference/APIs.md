# M05/M39 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| MasterServiceImpl | `/masterConfigService` | 67 endpoints; master screen tree; getAllDataForReport |
| MasterCachedServiceImpl | `/master` | **264 endpoints / 12,710 LOC** (04 §9.1 god node); cached master lookups; 514 graph edges (10 §1); startItemSyncingToSolr |
| TariffServiceImpl | `/tariff` | tariff CRUD + rate copy |
| ServiceMasterServiceImpl | `/servicemaster` | service catalogue |
| PlanMasterServiceImpl | `/planmaster` | health plans |
| PackageServiceServiceImpl | `/packageservice` | packages |
| ConcessionTemplateServiceImpl | `/concessiontemplate` | concessions |
| CompanyApprovedAmountServiceImpl | `/companyapprovedamount` | company approvals |
| AddServiceMasterImpl | `/addServiceMaster` | service-to-master mapping |
| GenericServiceImpl | generic CRUD via CrudOperationsFilter | crudadd/crudupdate/cruddelete/crudaddMultiple on 659 @CrudApi entities |
| HelperServiceImpl | `/helper` | cross-cutting helpers |
| MachineConfigServiceImpl | `/machineConfig` | machine config |
| MdmServiceImpl | `/mdm` | MDM/Janta master sync |
| IntegrationOperationsFilter | `/reports/<name>`, `/ext/api/...` | report dispatcher + integration mapping |

**Endpoint scale (03 §5):** master (195) 3rd-largest, masterConfigService (67) 10th.
