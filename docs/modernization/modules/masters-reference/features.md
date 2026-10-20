# M05/M39 — Features

## M05 — Billing Masters
| Feature | Evidence |
|---|---|
| Service master | servicemaster, services, serviceMasterSchema; AddServiceMasterImpl `/addServiceMaster` |
| Tariff & copay | tariffmaster, tariffcopaydetails, tariffratecopyutility; Tariffpackageclasswiserates |
| Packages (service/group/medicine/item-category) | packagemaster, packages, itemPackageDetails, servicePackageDetails, bedPackageDetails, consultationPackageDetails |
| Health plans (plan/subplan/tariff-package) | planmaster, subPlanmaster, packagehealthplandefinationmaster, healthPlanPackageSchema |
| Companies & plans | companymaster, companyplandetails, companytariffdetails, companycontractdetails |
| Concessions | concessiontemplatemaster, concessiontemplatewrapper |
| Order sets | ordersetdefinationmaster + ordersetservicemaster (lab/radiology/nursing/consult/procedure/consumables/miscellaneous) + ordersetmedicinedetails + procedureordersetservicemaster |
| Specialization | specialization, subspecialization |
| Plan-uncovered services | Planuncovereditemdetails, Planuncoveredservicedetails |

## M39 — Commons Platform
| Feature | Evidence |
|---|---|
| Audits | auditSchema, newAuditSchema (T_TRX_HISTORY pipeline, 59 files / 568 call sites) |
| App config | configapplication, applicationModuleStatus, hospitalCreationSchema, hospitalGeoTagging |
| Staff/doctors/user-pass | staffmaster, doctormaster, tuserrolemaster, addPatientUserPassSchema, OutreachUserCreationSchema |
| Shifts & rosters | shiftDetails-Schema, shiftRoster-Schema, userShiftDetailsListSchema, addIpdRosterSlotsSchema |
| MRN series | mrnoseries |
| Schemas designer | schemasDesigner (form designer) |
| Generic CRUD filter | CrudOperationsFilter routes `/api/rest/<entity>/*` → GenericServiceImpl (659 @CrudApi entities) |
| Report dispatcher | IntegrationOperationsFilter:469-571 serves `/reports/<reportName>` via Nashorn reports.js |
| /ext/api mapping | ApiIntegrationMapping mappingMap with ROLE_INTEGRATION gating |

## Client controllers
masters-modules.js (566 KB), masters-ext.js, user-module.js, item-category-module.js, notification-template-module.js, docit-roles.js, common-module.js.
