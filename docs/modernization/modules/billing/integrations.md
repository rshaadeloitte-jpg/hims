# M04 — Integrations

| Integration | Direction | Evidence |
|---|---|---|
| NHCX (insurance claims) | Out | via nhcx module — claimSubmitRequest, preAuthSubmit, coverageEligiblityRequest (M38) |
| RCM (RGHS/SIMS) | In/Out | M32: eligibility/claim documentation; bill estimation at admission |
| Payment transfer | Out | M33: paymentTransfer for incentive/fund flows |
| Solr | In-process | bill/encounter index events; patient search for bill lookup |
| WhatsApp/SMS | Out | report/notification queues (lab reports etc.) |

## Solr
- Patient search used for bill lookup; `searchPatientInEncouter` for OPD bill context. InvoiceGenerated → search events (target design, 10 §2.1 row 5).

## Redis
- Tariff/rate cache (tariffMaster, classMasterMap_* — **owned by M05/M39**, consumed here); session tokens; cashier counters.

## Audit
- Payment/receipt events in T_TRX_HISTORY; MBase stamps on all bill writes.
