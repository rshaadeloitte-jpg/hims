# M04 — Security

- Cash-counter identity (`cashcountermaster`); concession authority implied by role; no backend enforcement (08 §3.1).
- Billing keys (34): `Billing.OPIPBilling`, `Billing.DeathPatientBilling`.
- Client-side max-concession validation (`calculateTotalBillAmount`) — server re-validates via concession template lookup; asymmetry in BR-BILL-005 makes server truth critical.
- Bill cancellation restore logic touches charges/advances/itemsale/doctor-share in one tx (BR-BILL-008) — audit events on cancel.
- Settlement update happens in payment service via proc `CIMS_UPDATEBILLFORCONCESSION` (AddPaymentServiceHelper:463) — cross-service write in target must become event (InvoiceGenerated) + REST settlement update (10 §2.2, §4).

## Migration notes (08 §10)
- Enforce server-side authorization on all `/bill*`, `/addPayment*`, `/cancelBill*` endpoints.
- Keep T_TRX_HISTORY semantics; standardize audit.
