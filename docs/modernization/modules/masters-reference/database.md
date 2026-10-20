# M05/M39 — Database (05 §3.1)

## M_* masters (582 tables, pattern `M_<UPPERCASE_CLASSNAME>`)
- M_UNITMASTER (45 refs) — tenant root; M_DEPARTMENTMASTER (32); M_BEDMASTER (26, also written by ADT procs); M_ITEMMASTER (18); M_SERVICEMASTER (15); M_DOCTORMASTER (16); M_STAFFMASTER (10); M_PATHOTESTMASTER (12); M_ACCOUNTING*/M_ACCLEDGER* (14).
- ~560 more masters across 24 subdomains (admission, ambulance, billing, biomedicalwaste, bloodBank, cssd, diet, ejanswasthya, ejs, equipment, grievance, hospital, inventory, meds, nursing, ot, pathology, patient, Pharmacy, radiology, rmrs, ruleengine).
- CONFIG_APPLICATION (app config key-values); block_mst/city_mst/district_mst (Rajasthan geography).

## M39 commons tables
ConfigApplication, ConfigAutocharges, TUserdetails, Staffmaster, Doctormaster, MrnSeries, AuditEventMaster, MachineConfiguration, modulenamemaster, languageLabel; T_TRX_HISTORY / T_AUDIT / T_LOGIN_AUDIT / T_ABDM_AUDIT / T_APPOINTMENT_CHANNEL (audit); T_FREQ_ITEMS (MERGE INTO upsert); Transactionals: MachineConfiguration, T_QUESTIONANSWER.

## Sequences
- t_trx_history_seq, T_LOGIN_AUDIT_SEQ, T_ABDM_AUDIT_SEQ, T_APPOINTMENT_CHANNEL_SEQ, auto_code_seq.

## Stored procedures
- CIMS_ADDSERVICEMASTER2, CIMS_ADDTARIFFSERVICE2 (10 §2.1 row 21); audit sequences; per-table `<t>_seq` convention.

## Migration notes (10 §2.1 row 21)
- Target owns M_* (609 entities) + cache-key contract; `/masters` CRUD, `/tariffs`, `/packages`, `/plans`, `/services`; emits MasterDataChanged/TariffChanged/PackageChanged.
- PG: 1:1 tables with quoted case preserved (`"M_ADMISSIONTYPE"`), `<table>_seq` sequences kept (05 §8).
