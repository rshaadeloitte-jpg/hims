# M16 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| RadiologyServiceImpl | `/radiology` | 12 endpoints: booking, queue, report, retest, reschedule, images |
| ReportServiceImpl (dispatcher) | `/reports` | getRadiologyReports (report dispatcher, 03 §2.6) |
| AddServiceOrderServiceImpl | `/serviceOrder` | rad order auto-creation at billing (BR-BILL-003) |

## Notes
- Smallest clinical module surface; rad orders also surface in nursing flows (`js/operation-theatre.js` rad order rows — 02 §M16 evidence).
- roleRadiology docit role.
