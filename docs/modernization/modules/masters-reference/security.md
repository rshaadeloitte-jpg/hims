# M05/M39 — Security

- **Admin-only master UI:** `showmasternav` flag; role menus gated; `data-roleM` keys — Configuration family is the largest (1,424 keys, 08 §4.2).
- **No backend enforcement** for master CRUD — any authenticated token can call `api/rest/<entity>/crudadd` (08 §3.1); only `/ext/api` (ROLE_INTEGRATION) is backend-gated.
- **Audit as the control:** generic CRUD + service methods flush per-request audit (T_AUDIT + T_TRX_HISTORY, 59 files / 568 call sites); MBase stamps ADDEDBY/ADDEDDATETIME on every entity.
- Permission model designed but dormant: `t_role_enabledpermission → M_ModulePermissionMaster (path, apiMethod, apiPath, moduleName, action, parentId)`; tree editor exists (AddUserServiceImpl.getPermissions); backend check commented out (ServiceLayerAspect:421-426).
- Master cache keys are global per node — no per-user scoping (data confidentiality by store/unit handled via query filters, not cache).

## Migration notes
- Promote M_ModulePermissionMaster to enforced server-side authorization (08 §10.1); cache keys get single-owner contract (10 §2.2).
