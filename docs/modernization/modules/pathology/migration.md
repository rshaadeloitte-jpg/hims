# M15 — Migration

**Target service:** 7. Lab (Pathology) — **Wave 3 (Clinical Care)** (10 §6).

**Complexity: 4/5** — god-class (14,280 LOC); machine interface; critical-alert routing; WhatsApp; sample-lock rule.

**Steps:**
1. **Decompose PathoOrderServiceImpl** by workflow stage: booking/collection/result/review/release/alert — separate tx boundaries.
2. **Events:** `LabOrderCreated`, `LabResultFinalized` (10 §2.1 row 7); order auto-creation (BR-BILL-003) moves from billing tx into Lab via OrderCreated consumption.
3. **Charge-removal lock (BR-PATH-001)** must survive: sample-accepted state blocks order cancellation — coordinate with Billing saga (M04) via LEX001 contract.
4. **Machine interface (KDL):** keep protocol abstraction; machine config tables move with Lab.
5. **WhatsApp delivery:** replace DB-polled T_WHATSAPPMESSAGEQUEUE with Kafka (10 §2.1 row 25); LabResultFinalized → Interop.
6. **EXT LIMS:** decide keep/drop (dormant, ISEXTLMISENABLED DB flag).
7. **Report templates** (pathoTemplate*) → read-model/rendering service; plainText variants preserved.

**Exit criteria (Wave 3):** LabOrderCreated/LabResultFinalized streams live.
