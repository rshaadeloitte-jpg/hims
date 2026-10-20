# M16 — Migration

**Target service:** 8. Radiology — **Wave 3 (Clinical Care)** (10 §6).

**Complexity: 2/5** — small service; PACS integration moderate.

**Steps:**
1. **Extract RadiologyServiceImpl** (12 endpoints) into Rad service; `RadOrderCreated`/`RadResultFinalized` events (10 §2.1 row 8).
2. **Order auto-creation (BR-BILL-003)** moves out of billing tx → consume OrderCreated.
3. **Template reports:** radtemplatemaster/radtemplateresult → versioned reference data; pathoRadioTemplate shared with Lab — keep shared rendering contract.
4. **Image storage decision:** MinIO vs filesystem fallback ambiguity (07 §3.1 #13) must be pinned before cutover.
5. **Wave placement:** Wave 3 ≥ Wave 2 events.

**Open questions:** PACS direction (upload vs reference URL); retest/reschedule semantics; UNKNOWN-20 status codes.
