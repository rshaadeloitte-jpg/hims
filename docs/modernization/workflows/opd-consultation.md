# Workflow: OPD Consultation (walk-in visit → doctor consult → checkout)

**Source evidence:** `_discovery/agent-06/findings.md` §1.2 (steps 1–14) · `03-legacy-frontend-inventory.md` §2.1 (queue, doctor screens) · `04-api-catalog.md` (walkinVisit endpoints)
**Target service:** Appointment & Encounter (OPD) · Queue & Kiosk · Clinical

## Purpose
Create the encounter for a walk-in/emergency patient, drive the doctor queue (call patient → consult → checkout), track VisitStatus transitions, and pre-stage consultation charges for billing.

## Actors
Front office / registration desk · Doctor (queue call + consult) · Nurse/queue counter · Billing clerk

## Steps

| Step | Layer | Component (file:line) | Action | Data/Table | Notes |
|---|---|---|---|---|---|
| 1 | UI | `op:appointmentFlow` (`startAppointmentFlow`, appointment-module.js) | Walk-in form; visitTypeId*, registrationToken, mlcDetailsDto | – | flagship screen (03 §2.1) |
| 2 | API | `POST api/rest/walkinVisit/addwalkinVisit` (`js/appointment-list-module.js:1986`, `js/nhncModule.js:7180`) | Submit visit | – | |
| 3 | SVC | `addwalkinVisit` → `addwalkinVisitNew` (`EmergencyAndWalkinVisitServiceImpl.java:302-309`) | Orchestrate | – | |
| 4 | SVC | Validation + non-residential tariff (`:315-419`) | `BEXA002`/`BEXA034`/`FWAD001` gates; resState != config.state → `config.nonresidentialtariffId` else literal `2L` | T_PATIENTSPONSOR | BR-OPD-009, BR-OPD-001 |
| 5 | SVC | Walk-in requires doctor (`:380-388`, `:628-630`) | no doctorId → `BERA022`; unit-head fallback else `BERA029` | M_DOCTORMASTER | BR-OPD-006 |
| 6 | SVC | Encounter creation (`:947-1156`) | type=2 emergency / 1 normal (`:998-1002`); `visitstatus2="2"` default (`:1039`), `"9"` compound-med (`:1140-1142`); visitMark=true (`:1194`); token `CIMS_GETAUTOTOKENNO` when queueTokenBaseWorkFlow (`:1176-1186`); MLC flag + row + audit MLCDETAILS.ADD (`:1210-1214`, `:1438-1481`) | T_Encounter, T_Visit, T_MLCDETAILS | BR-OPD-004, BR-OPD-008 |
| 7 | SVC | OPD no. (`:1291-1308`) | `V/<PREFIX>/<YY>/<00001>`; emergency `EV/...`; seq per unit `Unitmaster.getOPDSequenceName` | T_Encounter | BR-OPD-003 |
| 8 | SVC | Visit-type sequence (`:1052-1107`) | revisit same doctor within `encountervalidity` → next sequence type; new → seq 1 | M_VISITTYPEMASTER, T_Visit | BR-OPD-004 |
| 9 | DB | `CIMS_ADDPATIENTSPONSOR_NEW_DOC` (`:1729-1794`) | Sponsor defaults: selfcompanyid/selftariffid/priorityId=1; plan required else `BERP008` (`:1359-1365`) | T_PATIENTSPONSOR | BR-OPD-005 |
| 10 | INT | `indexEncounterInSolr` (`:1796-1800`); consultation charges → Solr cache `current_enc_<id>_E` (`:1838-1847`) | Index encounter + staged charges | Solr `encounter_risl_qa` | |
| 11 | API | `POST walkinVisit/callPatient` / `callPatientNew` (`updateCallPatientData(New)`) | Doctor called patient → `T_Encounter.VisitStatus=3` (`EmergencyAndWalkinVisitServiceImpl.java:2017,2363`) | T_Encounter | queue flow |
| 12 | SVC | `QuetionnaireServiceImpl.java:107,109` | Assessment questionnaire saved → VisitStatus 4; other questionnaire → 6 | T_Appointment | per 06 catalog |
| 13 | API | `PUT walkinVisit/doctorcheckout` (`updateDoctorCheckout` `:2270`) | Doctor checkout → VisitStatus 9 (compound-med pre-visit RX also `:2351`) | T_Encounter | |
| 14 | API | `PUT walkinVisit/releaseCabin` (`releaseCabin` `:2146`) | Cabin released → VisitStatus 10 (only when no pending temp charges — R8) | T_Encounter, T_CHARGES_TEMP | R8 gate |
| 15 | SVC | `getLastVisitStatus` (`:1861-1866`) | Kiosk/queue reads last visit status from **T_Appointment** | T_Appointment | R1 split-brain |
| 16 | API | `POST walkinVisit/updateVisitType`, `PUT walkinVisit/updateCabinandDoctId` | Mid-visit corrections | T_Encounter | |

## Flowchart

```mermaid
flowchart TD
    A[User: front office opens Walk-in screen] --> B[FAUG walkinVisit-schema form]
    B --> C[Client validation: validatePatientDetails, visit type, MLC block]
    C --> D[API: POST walkinVisit/addwalkinVisit]
    D --> E[Service: addwalkinVisitNew]
    E --> E1[Gates: BEXA002 / BEXA034 / FWAD001 / BERA022 doctor check]
    E1 --> E2[Non-residential tariff selection + visit-type sequence]
    E2 --> E3[Encounter creation: visitstatus2=2, token via CIMS_GETAUTOTOKENNO, MLC row]
    E3 --> F[DB: T_Encounter + T_Visit + T_PATIENTSPONSOR + T_MLCDETAILS]
    F --> G[Solr: indexEncounterInSolr + charge cache current_enc_id_E]
    G --> H[Queue: doctor calls patient - POST walkinVisit/callPatient -> VisitStatus 3]
    H --> I[Consult + questionnaires - VisitStatus 4/6]
    I --> J[Doctor checkout - PUT walkinVisit/doctorcheckout -> VisitStatus 9]
    J --> K[Cabin release - PUT walkinVisit/releaseCabin -> VisitStatus 10 if no temp charges]
    K --> L[Billing: addBillPayment -> VisitStatus 11 (see billing workflow)]
```

## Business rules involved
BR-OPD-001 (non-residential tariff), BR-OPD-003 (OPD number), BR-OPD-004 (visit-type sequence), BR-OPD-005 (sponsor defaults), BR-OPD-006 (doctor required), BR-OPD-007 (billed → 11), BR-OPD-008 (MLC), BR-OPD-009 (validation gates), BR-PAT-001 (temp-MRN promotion at visit), BR-PHAR-001 (dispensed → 12, pharmacy workflow)

## Edge cases
- Compound-medication visit type → `visitstatus2="9"`.
- Emergency visit → `EV/...` number, encounter type 2, `isEmergency=true` visit type.
- `releaseCabin` (R8): VisitStatus 10 only set when no pending temp charges remain.
- Kiosk self-registration path (`selfPatientRegistation`) and `markandaddvisit/checkin` variant.

## Unknowns
- Initial VisitStatus value at encounter insert (evidence: "initial value set by UI/DB default" — numeric value not captured; see STATUS-CODE-DICTIONARY §VisitStatus).
- The consult-time charge ordering flow (doctor orders lab/rad/pharmacy) is covered by the billing/lab/prescription workflows; the doctor-side consultation screen internals (diagnosis, notes) are not traced in agent-06 findings.
