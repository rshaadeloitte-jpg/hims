# Web Research — External Integrations (ABDM, HCX, RGHS, e-Aushadhi)

> Created: 2026-08-14 · Tool: wigolo (local-first search/fetch; sources cached locally)
> Evidence standard: EXTERNAL/WEB evidence — labelled with confidence; does NOT replace source-code evidence.
> All statements below are FACT (with source), INFERENCE, or RECOMMENDATION. Vendor-blog sources are MEDIUM confidence unless corroborated by official sources (NHA/GitHub/GoR).

---

## 1. ABDM / ABHA ecosystem (Integration #1 in 07-integrations-and-search.md)

### Verified facts
| # | Finding | Source | Confidence |
|---|---|---|---|
| W-01 | ABDM operated by National Health Authority (NHA), MoHFW; "One Nation, One Health ID". 650M+ ABHA accounts as of mid-2026 (NHA dashboard). | ichelonconsulting.com guide (2026-07-14) | MEDIUM (vendor blog citing NHA) |
| W-02 | 4 core components: HFR (facility registry), HPR (professional registry), ABHA (patient health ID, 14-digit), Consent Manager. | multiple vendor guides + abha.abdm.gov.in | HIGH (multi-source + official) |
| W-03 | Hospital must be **HIP** (Health Information Provider) to share records; path: HFR registration → HPR for all clinical staff → HIP module in HIS → ABDM Sandbox (SBX) testing → NHA certification → production credentials. | ichelonconsulting.com guide; abdm.gov.in | MEDIUM-HIGH |
| W-04 | HIP requirements: FHIR R4 records, ABHA verification API, Consent Manager API, sandbox certification before production go-live. Sandbox: sandbox.abdm.gov.in. | ichelonconsulting.com guide | MEDIUM |
| W-05 | AB-PMJAY wave-2 states increasingly require HIP integration for empanelled hospitals; NABH 6th edition references ABDM/ABHA in AAC chapter; DPDP Act 2023 consent obligations align with Consent Manager. | ichelonconsulting.com guide (internal links NABH/DPDP) | MEDIUM |
| W-06 | Typical HIP integration timeline: 8–16 weeks (certified vendor), up to 24 weeks (custom FHIR module). Custom FHIR module cost ₹12L–₹45L. | ichelonconsulting.com guide | MEDIUM (vendor anecdote) |

### Relevance to iHMS (INFERENCE/RECOMMENDATION)
- iHMS already implements a large ABDM surface (~50 endpoints, xcg/abdm, ABDMRequestQueue, Redis correlation store) — this is an asset, not a new build (see evidence-register E-18).
- RECOMMENDATION: ABDM re-certification against current sandbox + FHIR IG v5.0.0 (nrces.in) belongs in interop wave (MIG-055/056) as an adapter test, NOT a rebuild. Keep the ABDM queue/outbox pattern (already proven) when replacing with Kafka.
- RECOMMENDATION: consent artefact handling and FHIR bundle generation are contract-capture candidates for the contract harness (TEST-STRATEGY L0/L1) — sandbox provides a free test environment.
- DPDP Act 2023 note: consent-driven sharing is now a legal baseline — target design should treat ABDM consent artefacts as the canonical patient-consent flow (no parallel ad-hoc data sharing).

---

## 2. HCX / NHCX claims exchange (Integration #2)

### Verified facts
| # | Finding | Source | Confidence |
|---|---|---|---|
| W-10 | HCX = open-source protocol spec (hcx-project/hcx-specs, GitHub), born from NHA+IRDAI Joint Working Group (2019); HCX switches are routing gateways between payors/providers/beneficiaries. | github.com/hcx-project/hcx-specs | HIGH (official repo) |
| W-11 | Current spec version **0.9 (Dec 2023)** — self-contained, no backward-breaking changes vs 0.8; published docs at docs.hcxprotocol.io. | hcx-specs README | HIGH |
| W-12 | Protocol building blocks: primary message flow + additional flows (redirect, forward, relay, intra-cycle communication), notifications; API specs split: registry APIs, primary-flow APIs, operational APIs, notification APIs. | hcx-specs README (v0.9 changelog) | HIGH |
| W-13 | v0.7.1 changed claim bundles from FHIR "document" to "collection" type (simplifies parsing, drops signature overhead, reduces payload). | hcx-specs README (version history) | HIGH |
| W-14 | NHCX profiles in ABDM FHIR Implementation Guide **v5.0.0** (nrces.in): ClaimBundle, EligibilityBundle, PreAuthBundle (Bundle of type collection); NHCX flows: provider/payor details, eligibility check, pre-auth request, claims request, payment notification, payment acknowledgement, search/fetch claims. | nrces.in/ndhm/fhir/r4/5.0.0/hcx-profile.html | HIGH (official IG) |
| W-15 | API security: per-participant, per-user API keys for auditability; production onboarding includes BSP (beneficiary service provider) verification; QR code specs for participant/policy codes; e-claim objects (eObjects) machine-readable domain models. | hcx-specs README (v0.9) + docs.hcxprotocol.io (cached) | HIGH |
| W-16 | Reimbursement claims can flow via TSP/ISNP platform with patient consent verification token; new claim denial codes for consent declines. | hcx-specs README (v0.9) | HIGH |

### Relevance to iHMS (INFERENCE/RECOMMENDATION)
- Legacy NHCX implementation (xcg/hcx, NHCXServiceImpl, templates/nhcx/*.json FHIR bundles) should be validated against protocol 0.9 + FHIR IG 5.0.0 bundle profiles during interop wave — bundle type "collection" is the key conformance point (legacy templates: Claim_onSubmit_Bundle.json etc.).
- RECOMMENDATION: HCX JWE key management (legacy abdm.keys.file.path, 365d TTL keys) must be re-architected to per-participant/per-user keys (W-15) — this is a genuine deltas to flag in MIG-055.
- HCX claims surface is a candidate to stay behind the interoperability gateway in the strangler plan; eligibility check is synchronous → keep sync path, move only notification/ack to async.

---

## 3. RGHS (Integration #7)

### Verified facts
| # | Finding | Source | Confidence |
|---|---|---|---|
| W-20 | RGHS: cashless medical benefits (OPD/IPD/day-care) for state employees, pensioners, SAB, MLAs/Ex-MLAs; 22 beneficiary categories; nodal agency **RSHAA** (Rajasthan State Health Assurance Agency). | rghs.rajasthan.gov.in (official) + RGHS Connect app listing | HIGH |
| W-21 | Beneficiary database keyed on **Jan Aadhaar** family status; hospital empanelment via HBEC on RGHS portal; **pre-authorization at hospital level only**; online payment system to hospitals. | rghs.rajasthan.gov.in homeintro | HIGH |
| W-22 | Claim settlement via RGHS IT platform; beneficiary EMR kept in RGHS card holder's **e-Wallet**. | rghs.rajasthan.gov.in homeintro | HIGH |
| W-23 | Public API documentation for HMIS vendors: **NOT FOUND on open web** (no public spec located; integration is vendor-empanelment-gated). | wigolo search (2026-08-14) | UNKNOWN → verify via RSHAA/RGHS empanelment docs |

### Relevance to iHMS (INFERENCE/RECOMMENDATION)
- Legacy RGHS surface (rghsapp.api.url, rghs.ihmsTid.api.url, rghs.ihmsDocs.api.url, SIMS push, Chiranjeevi) is contract-sensitive: TID tracking and docs upload are already in code; keep as-is in interop adapter.
- UNKNOWN-34 (RGHS metadata models) remains open; official docs are not public — verification requires the RGHS/RISL empanelment package, not the web.

---

## 4. e-Aushadhi / RMSC (Integration #8) — NO FINDINGS

- All engines failed/timed out (2026-08-14 09:15 UTC: bing, duckduckgo, wikipedia timeout; mojeek 403; marginalia throttled) — retry required; also note engine pool degradation.
- No public API documentation located on the open web for e-Aushadhi/RMSC HMIS integration. Expect vendor-gated docs (like RGHS).
- INFERENCE: e-Aushadhi adapter (InventoryEAushadiIntegrationImpl, 1,963 lines) remains black-box contract; mitigation = contract capture from running legacy (TEST-STRATEGY fixture approach), not from web docs.

---

## 5. Chiranjeevi scheme — NO FINDINGS (brand collision)

- "Chiranjeevi" collides with the actor (Wikipedia/IMDb); the scheme is the RSHAA Chiranjeevi Yojana (health cover for auto/cab drivers). Official source: RSHAA / rghs.rajasthan.gov.in only.
- INFERENCE: same treatment as RGHS — contract fixtures from legacy, not web.

---

## Method notes
- Sources fetched via wigolo with local cache (content hashes recorded); citation IDs in tool transcripts.
- Engine pool degraded at time of writing (healthy 1/5 engines) — the two gaps (e-Aushadhi, Chiranjeevi) are retry candidates before concluding "no public docs".
- Follow-up retry list: (a) e-Aushadhi keyword set ("eaushadhi", "RMSC Rajasthan", "janta item master"), (b) "Chiranjeevi Yojana" exact-quoted with rshaa.rajasthan.gov.in include-domain, (c) ABDM sandbox-to-prod steps from abdm.gov.in (gap from W-03/W-04 coverage).
