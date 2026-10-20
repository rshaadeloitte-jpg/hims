# Researcher Note T4 — Security/Compliance, Vendor/Market Landscape (2025–2026), Cloud & AI Trends, Modernization Approaches, Market Size

**Task:** Research brief `outputs/.plans/hims-system-research-T4.md` (parent plan: `hims-system-research.md`, task 5).
**Date of research:** 2026-08-14. **Researcher:** T4 evidence-gathering subagent.
**Facet status:** Compliance `done` · Vendor landscape `done` · Cloud & AI `done` · Modernization `done` · Market size `done` (with caveats) · `feynman_science_database_search` (pubmed/openalex tool) `blocked` — not present in toolset; substituted with `feynman alpha search` (alphaXiv) and `feynman paper <doi>` (OpenAlex metadata), no PDFs fetched.

---

## Search terms (exact query strings run)

**Facet 1 — Compliance (web_search):**
1. `HIPAA Privacy Rule Security Rule Breach Notification Rule requirements covered entities 2025`
2. `HIPAA penalty amounts OCR enforcement 2025 fines`
3. `GDPR Article 9 special category health data processing requirements penalties`
4. `India DPDP Act 2023 health data special provisions penalties compliance`
5. `HIPAA Security Rule technical safeguards RBAC audit controls encryption required`
6. `UK GDPR health data Data Protection Act 2018 special category penalties ICO fine`
7. `HIPAA civil money penalty amounts 2025 per violation tier inflation adjusted OCR`
8. `GDPR international data transfers Chapter V health data residency requirement`
9. `ICO maximum fine UK GDPR 17.5 million 4% global turnover health data`

**Facet 2 — Vendor landscape (web_search):**
1. `EHR vendor market share 2025 Epic Oracle Health Cerner KLAS`
2. `Oracle Health Cerner acquisition integration 2025 news`
3. `Meditech 2025 EHR news hospital customers`
4. `OpenMRS Bahmni enterprise adoption 2025 open source HMIS`
5. `Meditech acute care EHR market share percent 2025 KLAS third`
6. `OpenMRS used countries worldwide adoption statistics open source medical record`

**Facet 3 — Cloud & AI trends (web_search):**
1. `ambient AI scribe clinical documentation 2025 adoption hospitals`
2. `AI medical coding automation hospitals 2025 revenue cycle`
3. `Epic AI features 2025 ambient documentation agentic`
4. `healthcare cloud computing adoption 2025 SaaS trends`

**Facet 4 — Modernization (web_search):**
1. `legacy EHR modernization strangler fig pattern migration strategy`
2. `FHIR-first API-first healthcare platform modernization approach`
3. `legacy hospital information system migration case study 2025`
4. `EHR data migration risks failures lessons learned hospitals`

**Facet 5 — Market size (web_search):**
1. `EHR market size 2025 forecast billion global`
2. `healthcare IT market size 2025 hospital information system market`

**Paper searches (alphaXiv via `feynman alpha search -m both`):**
1. `legacy health information system modernization migration`
2. `electronic health record system migration FHIR interoperability legacy`
3. `health information system security privacy audit log access control`
4. `strangler fig microservices legacy system migration healthcare` (returned empty)
5. `data migration electronic health records challenges risk factors`

**Paper metadata resolution (`feynman paper <doi>`, OpenAlex — metadata only, no PDF):**
1. `10.1007/s44250-025-00186-x` (HIS migration case study, Discover Health Systems)
2. `10.1055/s-0040-1710007` (Data Migration: A Thorny Issue, ACI Open)

**Fetched directly (`fetch_content`):** HHS Security Rule summary; HHS HIPAA Enforcement; Healthcare Dive (Oracle/KLAS); HIPAA Journal (2025 penalty tiers); gdpr-info.eu Art. 83 and Art. 35; hitconsultant (KLAS 2026 summary); Martin Fowler "Rewriting Strangler Fig"; ICO max-fine page (via search result set).

---

## 1. Security & Compliance

### 1.1 HIPAA (US) — requirements

| Regime | Requirements | Penalties | URL |
|---|---|---|---|
| HIPAA Privacy Rule (45 CFR §160, §164 subpart E) | Protects all PHI; patients' rights (access, amendment, accounting of disclosures); "minimum necessary" standard for uses/disclosures; applies to covered entities (health plans, clearinghouses, providers) and business associates | Civil money penalties (see 1.2); criminal penalties for knowing violations | https://www.hhs.gov/hipaa/for-professionals/security/laws-regulations/index.html [1] |
| HIPAA Security Rule (45 CFR §164 subpart C) | Administrative, physical, technical safeguards for **ePHI**: risk analysis & management, security official, workforce security, information access management, security awareness/training, incident procedures, contingency plan, BA agreements; technical: access control (unique user ID required; RBAC as addressable), **audit controls** (164.312(b): "hardware, software, and/or procedural mechanisms to record and examine activity in information systems that contain or use ePHI"), integrity, authentication, transmission security. Encryption of ePHI at rest/in transit is **addressable**, not mandated per se (HHS FAQ) | Same CMP schedule | https://www.hhs.gov/hipaa/for-professionals/security/laws-regulations/index.html [1]; https://www.hhs.gov/hipaa/for-professionals/faq/security-rule/index.html [2]; NIST SP 800-66 Rev. 2 implementation guide: https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-66r2.pdf [3] |
| Breach Notification Rule (45 CFR §§164.400–414) | Notify individuals, HHS (and media for large breaches) of breaches of **unsecured** PHI; business associates must notify covered entities; FTC parallel rule for PHR vendors (HITECH §13407) | Part of CMP schedule; breaches are the top enforcement trigger | https://www.hhs.gov/hipaa/for-professionals/breach-notification/index.html [4] |
| HITECH Act 2009 + 2013 Omnibus Rule | Extended Security Rule directly to business associates (direct liability); strengthened enforcement and breach rules | Four-tier culpability CMP structure created by HITECH | [1][4] |

Key current item: OCR published a **Proposed Rule (NPRM) to modify the HIPAA Security Rule** to strengthen cybersecurity of ePHI (referenced on the current HHS Security Rule summary page; fetched 2026-08-14) [1]. The NPRM itself (Dec 2024) was not fetched this run — see Blocked/Unverified.

### 1.2 HIPAA — penalty amounts (dated)

Statutory four-tier structure (HITECH) and **2025 inflation-adjusted effective amounts** (per HIPAA Journal, 2026-01-28, citing the Federal Register publication of the annual adjustment; note OCR's 2019 Notice of Enforcement Discretion halves maxima/caps in three tiers) [5]:

| Tier (culpability) | Statutory min–max per violation | 2025 adjusted min–max per violation | 2025 adjusted annual cap (identical provision) |
|---|---|---|---|
| Did not know | $100–$50,000 | $145–$36,505.50 (discretion) / $73,011 (statutory) | $36,505.50 (discretion) / $2,190,294 (statutory) |
| Reasonable cause | $1,000–$50,000 | $1,461–$73,011 | $146,053 |
| Willful neglect (corrected ≤30 days) | $10,000–$50,000 | $14,602–$73,011 | $365,052 |
| Willful neglect (not corrected) | $50,000–$1,500,000 | $73,011–$2,190,294 | $2,190,294 |

2025 enforcement context: OCR entered **10 HIPAA resolution agreements in the first five months of 2025** (Ogletree, law-firm analysis) [6]; failure to conduct an adequate security **risk analysis was the top finding in 2025** and ransomware the most common triggering event (DMC Law, 2026-03-19) [7]. Examples: PIH Health $600,000 settlement (phishing attack) [8]; Warby Parker $1.5M CMP (cybersecurity hacking investigation) [9]; Oregon Health & Science University $200,000 CMP (right-of-access failure) [10].

### 1.3 GDPR (EU) — health data as special category

- Health data is a **special category** under Art. 9(1): processing is *prohibited* unless one of the Art. 9(2) conditions applies (explicit consent; provision of health/social care or treatment with a basis in EU/national law; vital interests; public interest in public health, etc.) [11][12][13].
- **DPIA**: a data protection impact assessment is *required* for large-scale processing of special category data (Art. 35(3)(b)) [14].
- **Penalties** (Art. 83): up to **€20M or 4% of total worldwide annual turnover (whichever higher)** for infringements of basic principles including Art. 9 (special categories); up to **€10M or 2%** for other obligations (Art. 83(4)). When assessing fines, supervisory authorities consider technical/organisational measures implemented under Arts. 25 (data protection by design/default) and 32 (security of processing) (Art. 83(2)(d)) [15].
- **Cross-border transfers / data residency**: no EU-level "residency" mandate, but Chapter V requires transfers outside the EEA to meet adequacy/appropriate-safeguard conditions (SCCs etc.); EDPB: "the protection offered by the GDPR travels with the data" [16][17]. For HMIS operators this effectively constrains cloud siting of EU patient data (inference: GDPR's transfer rules plus DPIA obligations are the de-facto residency mechanism).
- **RBAC / audit logging / encryption** are the standard Art. 25/32-type technical and organisational measures for health systems; their presence/absence is explicitly a fine-calibration factor under Art. 83(2)(d) (inference from [15] Art. 83 text referencing Arts. 25/32).

### 1.4 UK GDPR / Data Protection Act 2018

- UK GDPR (retained EU law) + DPA 2018 govern health data as special category with the same Art. 9 prohibition/exceptions structure [11][12]; ICO guidance lists the ten Art. 9 conditions [13].
- Penalty cap under UK GDPR: **£17.5M or 4% of worldwide annual turnover (higher)** for higher-tier infringements; standard max for lower-tier obligations (ICO; DPA 2018 s.157) [18][19].
- Dated example: **ICO fined Advanced Computer Software Group Ltd £3,076,320 (penalty notice 2025-03-27)** for infringement of UK GDPR in connection with a health-data ransomware breach [20].

### 1.5 India DPDP Act 2023

- **Digital Personal Data Protection Act 2023** (Act 22 of 2023, enacted 2023-08-11) is India's first data protection statute [21]; **no separate health-data category** — health data is "personal data" under the consent-based framework (Mondaq; amlegals) [22][23].
- **DPDP Rules 2025** notified: verifiable consent, child-data rules, significant-data-fiduciary obligations, breach notification duties (EY compliance guide) [24].
- **Penalties:** up to **₹250 crore** for serious contraventions (e.g., significant data breach) per the Act's Schedule (amlegals) [23]; Chapter 8 "Penalties & Adjudication" of the Act [25].
- **Cross-border transfer / residency:** §16 empowers the Central Government to restrict transfer of personal data to notified countries/territories — the Act's residency-relevant lever (egazette official text; India Code) [21][26]. The egazette PDF was not parsed (PDF; see Blocked).

### 1.6 Cross-cutting: RBAC, audit logging, encryption, data residency (HMIS relevance)

- **HIPAA:** audit controls are a *required standard* (164.312(b)) [1][27]; access control required with unique user identification (required) and role-based access (addressable) [27]; encryption addressable (both at rest and in transit) but OCR consistently expects it in practice — risk analysis failures were the #1 enforcement finding in 2025 [6][7]; NIST SP 800-66r2 is the federal implementation playbook [3].
- **GDPR:** Art. 32 security of processing (encryption, access control, logging) + Art. 25 by-design; Art. 9 + DPIA for health data; transfer rules constrain residency [11][14][15][16][17].
- **India DPDP:** consent-centric; health data not elevated to special category (unlike GDPR); transfer restriction power in §16 [21][22][23].
- **Data residency conclusion (inference):** HIPAA has no residency mandate (BAA + safeguards travel with the data); GDPR/UK GDPR impose transfer conditions that operate like residency constraints for EU health data; DPDP §16 creates a future residency-restriction mechanism for India.

---

## 2. Vendor Landscape 2025–2026 (dated)

### 2.1 US acute-care EHR market — KLAS data

- **Epic** — clear market leader: controlled **>42% of US acute-care hospitals in 2024** vs Oracle 23% (KLAS 2025 report via Healthcare Dive, 2025-08-25) [28]. In 2025 purchase decisions: **Epic was the only vendor chosen by large health systems (>10 hospitals)** and added 77 hospitals / 18,679 beds; only two vendors (Epic and TruBridge) achieved positive net market-share change (KLAS 2026 report via hitconsultant, 2026-05-14) [29].
- **Oracle Health (ex-Cerner)** — acquisition closed 2022 at **>$28B** [28]. **Lost 57 unique acute-care customers in three years**, 12 of them health systems >1,000 beds; lost clients include Intermountain Health, UPMC, Henry Ford Health, Adventist Health, ChristianaCare (2022–2024) [28]. Acute-care share fell from ~25% (2021) to 22.9% (2024) (Becker's, six-developments roundup) [30]. KLAS 2026: **third consecutive year of major net losses (−56 hospitals, −14,676 beds in 2025)**; ~1/3 of sampled customers say Oracle is not in their long-term plans [29]. Counterpoints: customer confidence in long-term vision grew; early adopters of the Clinical AI Agent are satisfied [28].
  - **2025 product event:** Oracle launched an **AI-first, cloud-native EHR built on Oracle Cloud Infrastructure** (not Millennium) for US ambulatory providers in **August 2025**; acute care planned for 2026 (MedTech Dive; Becker's; Fierce Healthcare) [31][32][33].
- **MEDITECH** — third-largest US acute-care customer base; **~14.7% acute-care share** (KLAS via Fierce Healthcare) [34]. **Expanse cloud EHR + "MEDITECH as a Service" (MaaS)**; 2025 go-forward decisions: **84% of legacy MEDITECH customers chose to migrate to Expanse** (up from 63% in 2024, 30% in 2023) — strongest retention year on record [29][35]. 2025 customer news (vendor releases): HCA Healthcare UK live on Expanse across 11 hospitals (2025-12-18) [36]; La Paz Regional Hospital (AZ) on MaaS (2025-07-02) [37]; Comanche County Memorial (OK) selected Expanse (2025-09-03) [38]; Silver Hill Hospital (CT) signed for MaaS (2025-05-01) [39]; UPMC Ireland selected Expanse (2025-06-05) [40].
- **Market-wide:** acute-care EHR purchase decisions **dropped 40% in 2025 vs 2024 and ~50% vs 2023** (272 hospitals impacted in 2024 → fewer in 2025), attributed to policy uncertainty and capital redirected to AI (KLAS 2025/2026; TechTarget) [29][41][42]. Only five vendors had validated net-new decisions in 2025: Altera Digital Health, Epic, MEDITECH, Oracle Health, TruBridge [35].

### 2.2 Open-source: OpenMRS / Bahmni

- **OpenMRS** (since 2004): self-reported global production footprint of **80+ countries, 8,100+ healthcare sites, 22M+ patients** (OpenMRS wiki, "Around the World") [43]; a 2021 peer-reviewed analysis reported implementations in **62+ countries** ("OpenMRS as a global good", International Journal of Medical Informatics) [44]; **OpenMRS v3 ("O3") reached 2,000 production locations across 35 countries** (community milestone post) [45].
- **Bahmni** (OpenMRS-based, for low-resource settings): 2025 BMC study evaluated integrated EMR adoption at two community hospitals using Bahmni [46]; Bangladesh public facilities run an OpenMRS-based national system ("OpenMRS+") [47]; DHIS2 community survey shows OpenMRS/Bahmni/DHIS2 Tracker dominate reported EMR implementations in the DHIS2 ecosystem [48].
- **Enterprise adoption note (inference):** OpenMRS/Bahmni are adopted mainly in LMIC public-health and donor-funded programs; they are not significant players in the US acute-care or Western enterprise EHR market, where Epic/Oracle/Meditech dominate.

---

## 3. Cloud & AI Trends (dated examples)

### 3.1 Ambient AI scribes / clinical documentation
- **62% of US hospitals using Epic have adopted ambient AI documentation** (national study of 2,784 Epic hospitals; American Journal of Managed Care, 2025–2026; Emory Rollins press release) [49][50].
- JAMA Network Open quality-improvement study (263 clinicians, 6 systems): after 30 days with an ambient AI scribe, **burnout fell from 51.9% to 38.8%**; after-hours documentation time improved [51].
- NEJM Catalyst: system-level report of **2.5M+ ambient AI scribe uses in one year** [52].
- PHTI (Peterson Health Technology Institute, 2025-03): ~**60 ambient scribe products in practice**; adoption uneven [53].

### 3.2 Vendor AI features (2025–2026)
- **Epic:** ~**200 AI features in development** (UGM, Aug 2025, CNBC) [54]; **Art for Clinicians** (ambient note drafting, message drafting), **Emmie** patient chatbot, RCM agent; agentic AI announced at HIMSS25 [55][56][57]; Epic's own ambient-speech product announcement analyzed by KLAS (2025) [58].
- **Oracle Health:** Clinical AI Agent (documentation drafting + next steps) [28]; AI-first EHR on OCI launched Aug 2025 (ambulatory) [31][32][33].
- **MEDITECH:** Expanse cloud platform with MaaS; no flagship generative-AI announcement found in this run (unverified — see Blocked).

### 3.3 AI in coding / revenue cycle
- **80% of health systems exploring, piloting, or implementing gen AI for RCM in 2025** (+38 pts in <2 years; Fierce Healthcare survey) [59].
- KLAS published its **first autonomous-coding market report (2025-08-26)** [60].
- **Cleveland Clinic × AKASA** collaboration to deploy gen-AI coding/documentation tools (2025-04-29) [61].
- **Baylor Scott & White Health** expects **$42M annualized net revenue improvement** from AI-based revenue-cycle physician-review with Accuity (pilot Feb 2025) [62].
- Waystar survey (316 finance/IT leaders): realized improvements averaged ≥20% across key RCM outcomes [63].

### 3.4 Cloud / SaaS adoption
- **71% of hospitals/health systems now run at least one clinical workload in the cloud** (Halkwinds survey of 341 healthcare IT leaders; 2025/2026 report) [64] — cited as a 2025/2026 finding from a research vendor; methodology not independently verified.
- KPMG global tech report (Feb 2025): ~70% of healthcare technology leaders report having AI/automation technologies; cloud-based delivery expanding [65] (PDF snippet; not parsed).
- Analyst market figures (market-research vendors, wide variance — treat as directional): healthcare cloud computing **$84.5B (2026) → $240.3B (2033), 16.1% CAGR** (Persistence) [66]; healthcare SaaS >90,000 adopting organizations in 2025 (SNS Insider) [67].

---

## 4. Modernization Approaches for Legacy HMIS (patterns + evidence)

### 4.1 Patterns

| Pattern | Description | Evidence |
|---|---|---|
| **Strangler fig** | Incrementally route traffic from legacy monolith to new services until the legacy host can be retired; originated as a metaphor by Martin Fowler (2004 bliki), rewritten 2024 with Ian Cartwright/Rob Horn/James Lewis "patterns of legacy displacement" | Fowler: https://martinfowler.com/bliki/StranglerFigApplication.html and https://martinfowler.com/articles/2024-strangler-fig-rewrite.html [68] |
| Strangler fig **for regulated/health systems** | BAA/HIPAA coverage must extend to new services from day one; audit-trail continuity across old+new systems; avoid big-bang cutover | https://www.the-algo.com/insights/strangler-fig-regulated-migration [69] |
| Strangler fig applied to **EHR without replacing it** | Wrap legacy with a FHIR API layer; add patient engagement/scheduling/billing/analytics one module at a time; AI-assisted discovery | https://www.dualbootpartners.com/insights/strangler-fig-strategy/ [70]; https://www.mindbowser.com/ehr-modernization-guide/ [71] |
| **FHIR-first / API-first** | FHIR as the internal contract/data model, not an afterthought; SMART on FHIR authorization; CMS mandates (Patient Access API, prior authorization) as modernization drivers | https://webdecodes.com/fhir-first-api-strategy-building-an-app-ecosystem-around-you [72]; https://nirmitee.io/blog/hl7-v2-to-fhir-migration-decision-framework-architecture-roadmap/ [73]; https://healthcareintegrations.com/what-a-good-fhir-integration-strategy-looks-like-in-a-mixed-legacy-environment/ [74]; https://www.nineleaps.com/fhir-first-healthcare-products-engineering-for-interoperability-by-design/ [75]; https://6b.health/insight/...fhir-first-backend-platforms/ [76] |
| **FHIR to the cloud (DB modernization)** | Humana used MongoDB Atlas + FHIR to avoid rip-and-replace, modernizing interoperability incrementally (vendor case study — self-reported) | https://www.mongodb.com/solutions/customer-case-studies/humana [77] |
| **Microservices decomposition** | Industry-survey research: legacy-modernization motivation for microservices; findings: most participants modernized legacy systems before migration, but some didn't, and rushed modernization was risky | arXiv 2408.10434 abstract: https://arxiv.org/abs/2408.10434 [78] |
| **LLM-assisted legacy code modernization** | LLMs for migrating MUMPS/mainframe-class systems; VAPU framework for autonomous modernization of information systems; XMainframe for COBOL | arXiv 2411.14971 [79]; arXiv 2510.18509 [80]; arXiv 2408.04660 [81] |
| **Frontend modernization** | Legacy UI transpilation (e.g., Android XML → Jetpack Compose) as a semantics-preserving migration approach | arXiv 2409.16656 [82] |

### 4.2 Documented cases & risk evidence

**Success cases:**
- **Kaiser Permanente** consolidated **12 California EHR instances into 2**, covering ~**40 million patient records**, each cutover completed in under three hours without data loss (MedCity News, 2025-12; TechTarget) [83][84].
- **HIS migration case study (peer-reviewed):** "Beyond the upgrade: unraveling the complexities of health information system migration" (Discover Health Systems, 2025) — framework + real-life **non-incremental** migration from legacy HIS to modern web-based system; stresses planning, stakeholder engagement, training (OpenAlex metadata via `feynman paper`; abstract from search snippet; full text not fetched) [85][86].
- **Torres and Cape Hospital and Health Service (Australia)** — KPMG de-risking EMR data migration to unify records (consultancy client story, self-reported) [87].
- Vendor-published go-lives: La Paz Regional (consolidating multiple legacy systems into Expanse) [37]; HCA UK (11 hospitals) [36].

**Failure / risk evidence:**
- **US VA + Oracle/Cerner EHRM:** GAO found **problems with quality of transferred data** — errors in allergy, medication, and immunization data posing patient-safety risk (GAO-22-103718) [88]; the VA rollout was "plagued with errors and technical issues" and cost overruns (Healthcare Dive) [28].
- **Data migration literature:** "Data Migration: A Thorny Issue in Electronic Health Record Transitions — Case Studies and Review of the Literature" (ACI Open, Thieme) — case studies + review; found most large data-migration projects run over budget or miss deadlines; legacy-data quality, mapping, and validation are core failure points (OpenAlex metadata via `feynman paper`; search-snippet content) [89][90].
- **EHR-to-EHR transition complexity:** "Electronic Health Record Transitions — How to Make Them Work" (PMC article, 2023) — vendor changes complicated by lab/radiology connections and huge data arrays (page JS-rendered; content from search snippet only) [91].
- **AHIMA practitioner guidance:** "Data Migration Lessons Learned" — hard-won best practices from one facility's EHR migration [92].
- Consultancy claims (low confidence, self-reported): **60–80% of healthcare IT budgets go to legacy maintenance; big-bang EHR replacements fail 70%+ of the time** (Mindbowser guide) [71]. These figures could not be corroborated against an independent primary source this run — treat as vendor marketing.

### 4.3 Regulatory push (inference from [1][73])
CMS interoperability mandates (Patient Access API, prior-authorization APIs) and HHS's proposed Security Rule changes are making API/FHIR capability and stronger audit/encryption controls increasingly *de facto* requirements for US vendors — a modernization driver for legacy non-FHIR HMIS.

---

## 5. Market Size (figures with year + source; analyst variance is large — use ranges)

| Market | 2025 size (or nearest year stated) | Forecast | Source (URL) |
|---|---|---|---|
| Global EHR market | **USD 35.89B (2025)** | $53.11B by 2033, 5.10% CAGR (2026–33) | Grand View Research [93] |
| Global EHR market | **USD 32.44B (2025)** | $52.60B by 2034, 5.61% CAGR; NA share 47.33% (2025) | Fortune Business Insights [94] |
| Global EHR market | **USD 28.86B (2025)** | $46.40B by 2035, 4.86% CAGR | Precedence Research [95] |
| Hospital Information System market | **USD 63.81B (2024)** | $116.76B by 2030, 10.6% CAGR | MarketsandMarkets [96] |
| Hospital Information System market | **USD 61.46B (2025)** → $65.47B (2026) | $89.75B by 2031, 6.52% CAGR | Mordor Intelligence [97] |
| Hospital Information System market | **USD 175.53B (2025)** | $533.71B by 2035 | Market Research Future [98] |
| Healthcare IT market | **USD 496.09B (2025)** → $568.32B (2026) | $1,143.67B by 2031, 15.0% CAGR | MarketsandMarkets [99] |
| Healthcare IT market | **USD 866.48B (2025)** | $2,864.39B by 2033, 16.2% CAGR | Grand View Research [100] |

**Takeaway (inference):** credible analyst range for global EHR ≈ **$29–36B (2025)** growing ~5% CAGR; hospital information systems ≈ **$61–64B (2024/25)** growing ~7–11% CAGR; broader healthcare IT ≈ **$0.5–0.9T (2025)**. Numbers differ materially across houses (scope and method differences) — always cite the specific firm+year.

---

## Evidence table

| # | Source | URL | Key claim | Type | Confidence |
|---|---|---|---|---|---|
| 1 | HHS — Summary of the HIPAA Security Rule (fetched) | https://www.hhs.gov/hipaa/for-professionals/security/laws-regulations/index.html | Security Rule safeguards (admin/physical/technical) for ePHI; audit controls 164.312(b); NPRM exists | primary | high |
| 2 | HHS — Security Rule FAQ | https://www.hhs.gov/hipaa/for-professionals/faq/security-rule/index.html | Encryption addressable; e-signature etc. FAQs | primary | high |
| 3 | NIST SP 800-66 Rev. 2 | https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-66r2.pdf | Implementation guide for Security Rule (PDF, not parsed) | primary | high (URL exists) |
| 4 | HHS — Breach Notification Rule | https://www.hhs.gov/hipaa/for-professionals/breach-notification/index.html | 45 CFR 164.400–414 notification duties; FTC parallel rule | primary | high |
| 5 | HIPAA Journal — penalty increase (2026-01-28, fetched) | https://www.hipaajournal.com/hhs-applies-inflation-increase-penalties-for-hipaa-violations/ | 2025 adjusted tier amounts + enforcement discretion caps | secondary (trade press, official numbers) | high |
| 6 | Ogletree — 2025 enforcement trends | https://ogletree.com/insights-resources/blog-posts/2025-enforcement-trends-risk-analysis-failures-at-the-center-of-hhss-multimillion-dollar-hipaa-penalties/ | 10 resolution agreements Jan–May 2025; risk analysis top issue | secondary (law firm) | medium |
| 7 | DMC Law — HIPAA Enforcement 2025 | https://dmclawllc.com/2026/03/19/hipaa-enforcement-in-2025-a-year-defined-by-cybersecurity-failures/ | 2025 enforcement defined by cybersecurity failures; ransomware | secondary (law firm) | medium |
| 8 | HHS — PIH Health settlement | https://www.hhs.gov/press-room/ocr-hipaa-racap-pih.html | $600,000 settlement, phishing breach | primary | high |
| 9 | HHS — Warby Parker CMP | https://www.hhs.gov/press-room/penalty-against-warby-parker.html | $1.5M civil money penalty | primary | high |
| 10 | HHS — OHSU right-of-access penalty | https://www.hhs.gov/press-room/penalty-against-or-health-science-university.html | $200,000 penalty; 53rd right-of-access action | primary | high |
| 11 | legislation.gov.uk — GDPR Art. 9 (UK GDPR) | https://www.legislation.gov.uk/eur/2016/679/article/9?view=plain | Special-category prohibition + exceptions | primary (statute text) | high |
| 12 | European Commission — sensitive data | https://commission.europa.eu/law/law-topic/data-protection/rules-business-and-organisations/legal-grounds-processing-data/sensitive-data/under-what-conditions-can-my-companyorganisation-process-sensitive-data_en | Conditions for processing special categories | primary | high |
| 13 | ICO — special category rules | https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/lawful-basis/special-category-data/what-are-the-rules-on-special-category-data/ | 10 conditions for special category processing | primary | high |
| 14 | gdpr-info.eu — Art. 35 (fetched) | https://gdpr-info.eu/art-35-gdpr/ | DPIA required for large-scale special-category processing | primary (statute text) | high |
| 15 | gdpr-info.eu — Art. 83 (fetched) | https://gdpr-info.eu/art-83-gdpr/ | Fines up to €20M/4% (higher tier), €10M/2% (lower); Arts 25/32 referenced in calibration | primary (statute text) | high |
| 16 | legislation.gov.uk — GDPR Chapter V | https://www.legislation.gov.uk/eur/2016/679/chapter/V/adopted | Transfer conditions to third countries | primary | high |
| 17 | EDPB — international transfers | https://www.edpb.europa.eu/sme-data-protection-guide/international-data-transfers_en | Chapter V conditions; protection travels with data | primary | high |
| 18 | ICO — max fine under UK GDPR | https://ico.org.uk/about-the-ico/our-information/policies-and-procedures/data-protection-fining-guidance/statutory-background/the-maximum-amount-of-a-fine-under-uk-gdpr-and-dpa-2018/ | Two fine levels under Art. 83 UK GDPR / s.157 DPA 2018 | primary | high |
| 19 | ICO — data protection principles guide | https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/data-protection-principles/a-guide-to-the-data-protection-principles/ | Higher-tier fines up to £17.5M or 4% | primary | high |
| 20 | ICO — Advanced Computer Software penalty notice | https://ico.org.uk/media2/gdlfddgc/advanced-penalty-notice-20250327.pdf | £3,076,320 fine (2025-03-27) for UK GDPR breach re health data (PDF, not parsed) | primary | high (snippet) |
| 21 | India Code — DPDP Act 2023 | https://www.indiacode.nic.in/handle/123456789/22037?locale=en | Act 22 of 2023, enacted 2023-08-11 | primary | high |
| 22 | Mondaq — Health Data & DPDP Act | https://www.mondaq.com/india/healthcare/1722484/health-data-the-dpdp-actnavigating-indias-healthcare-privacy-frontier | No separate health-data category; compliance duties | secondary (legal) | medium |
| 23 | amlegals — DPDP practical guide | https://amlegals.com/health-data-and-the-dpdp-act-a-practical-guide/ | Penalties up to ₹250 crore for serious contraventions | secondary (legal) | medium |
| 24 | EY India — DPDP Act/Rules 2025 | https://www.ey.com/en_in/insights/cybersecurity/decoding-the-digital-personal-data-protection-act-2023 | DPDP Rules 2025 scope (consent, SDF, breach notification) | secondary (professional services) | medium |
| 25 | DPDP Act 2023 (mirror) — Chapter 8 | https://www.dpdpact2023.com/chapter-8 | Penalties & adjudication chapter | secondary (unofficial mirror of statute) | medium |
| 26 | egazette.gov.in — DPDP Act text | https://egazette.gov.in/WriteReadData/2023/248045.pdf | Official Act text incl. §16 transfer restriction (PDF, not parsed) | primary | high (URL exists) |
| 27 | eCFR — 45 CFR 164.312 technical safeguards | https://federal.elaws.us/cfr/title45.part164.section164.312 | Technical safeguards incl. audit controls | primary (reg text) | high |
| 28 | Healthcare Dive — Oracle/KLAS (fetched) | https://www.healthcaredive.com/news/oracle-health-lost-customers-satisfaction-stagnated-cerner-acquisition-klas-research/758427/ | Oracle lost 57 acute-care customers in 3 yrs; Epic >42% vs Oracle 23% (2024); VA rollout problems | secondary (journalism of KLAS) | high |
| 29 | hitconsultant — KLAS 2026 summary (fetched) | https://hitconsultant.net/2026/05/14/klas-2026-ehr-market-share-report/ | 2025 decisions −40% y/y; Epic only large-system winner (+77 hospitals); Oracle −56 hospitals; Meditech 84% retention | secondary (journalism of KLAS) | high |
| 30 | Becker's — Oracle since Cerner: 6 developments | https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/what-oracle-has-done-since-acquiring-cerner-6-developments/ | Oracle acute-care share 25% (2021) → 22.9% (2024) | secondary | medium |
| 31 | MedTech Dive — Oracle AI EHR | https://www.medtechdive.com/news/oracle-new-ehr-artificial-intelligence-ai/757660/ | AI-backed EHR launched for US ambulatory, Aug 2025 | secondary (journalism) | high |
| 32 | Becker's — Oracle AI-powered EHR | https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/oracle-health-launches-new-ai-powered-ehr/ | OCI-native EHR, voice commands, AI agents | secondary | medium |
| 33 | Fierce Healthcare — Oracle agentic AI | https://www.fiercehealthcare.com/ai-and-machine-learning/oracle-ramps-healthcare-ai-tech-unveils-new-features-patients-prior-auth | $28B Cerner deal (June 2022); AI-first EHR not built on existing platform | secondary | high |
| 34 | Fierce Healthcare — Epic growth / Meditech share | https://www.fiercehealthcare.com/health-tech/epic-continues-grow-ehr-market-share-it-makes-gains-small-health-systems | Meditech 14.7% share, lost 7 hospitals/1,015 beds in 2025; third-largest base; ~45% on legacy platforms | secondary | medium |
| 35 | KLAS blog — 2026 acute-care report insights | https://engage.klasresearch.com/blog/uncertainty-on-multiple-fronts-slowed-ehr-purchases-insights-from-klass-2026-acute-care-ehr-market-share-report/10709/ | Only 5 vendors with validated decisions; Meditech 84%→Expanse | primary (KLAS) | high |
| 36 | MEDITECH — HCA Healthcare UK | https://ehr.meditech.com/news/hca-healthcare-uk-goes-on-the-cloud-with-meditech-expanse-ehr-powering-patient-centred-care | Expanse live across 11 hospitals (2025-12-18) | self-reported (vendor) | medium |
| 37 | MEDITECH — La Paz Regional | https://ehr.meditech.com/news/la-paz-regional-hospital-connects-care-across-the-community-with-centralized-meditech-expanse | MaaS go-live consolidating legacy systems (2025-07-02) | self-reported (vendor) | medium |
| 38 | MEDITECH — Comanche County | https://ehr.meditech.com/news/comanche-county-memorial-hospital-chooses-meditech-expanse-for-a-unified-health-record | Expanse selected (2025-09-03) | self-reported (vendor) | medium |
| 39 | MEDITECH — Silver Hill | https://ehr.meditech.com/news/silver-hill-hospital-signs-for-meditech-expanse-for-integrated-and-patient-centered-care | MaaS signed (2025-05-01) | self-reported (vendor) | medium |
| 40 | Becker's — UPMC Ireland/Meditech | https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/upmc-selects-meditech-ehr-for-ireland/ | UPMC Ireland selects Expanse (2025-06-05) | secondary | medium |
| 41 | KLAS — US Acute Care EHR Market Share 2025 (report page) | https://klasresearch.com/report/us-acute-care-ehr-market-share-2025-strength-of-partnership-is-key-in-decisions/3640 | 272 hospitals impacted by purchase decisions in 2024 vs 319 in 2023 | primary (paywalled; snippet) | medium |
| 42 | TechTarget — EHR purchases delay | https://www.techtarget.com/searchhealthit/news/366643293/Health-systems-delay-EHR-purchases-amid-uncertainty-rising-AI-investment | Purchase decisions −40% (2025) / −50% (vs 2023) | secondary | medium |
| 43 | OpenMRS wiki — Around the World | https://openmrs.atlassian.net/wiki/spaces/docs/pages/282624121/OpenMRS+Around+the+World | 80+ countries, 8,100+ sites, 22M+ patients (self-reported) | self-reported (community) | medium |
| 44 | ScienceDirect — OpenMRS as a global good | https://www.sciencedirect.com/science/article/abs/pii/S1386505621000319 | Implemented in 62+ countries (peer-reviewed, 2021; abstract only) | primary (academic abstract) | medium |
| 45 | OpenMRS Talk — O3 milestone | https://talk.openmrs.org/t/huge-milestone-o3-production-sites-passes-1-000-2-000-locations/44128 | O3 in 2,000 production locations, 35 countries | self-reported (community) | medium |
| 46 | BMC Med Inform Decis Mak — Bahmni adoption | https://link.springer.com/article/10.1186/s12911-025-03283-2 | Bahmni EMR adoption at 2 community hospitals (2025) | primary (academic) | medium |
| 47 | Bangladesh DGHS — OpenMRS+ HMS | https://en.info.shr.dghs.gov.bd/hms/ | Bangladesh public-facility HMS on OpenMRS | primary (govt program page) | medium |
| 48 | OpenMRS Talk — DHIS2 EMR survey | https://talk.openmrs.org/t/openmrs-features-strongly-in-dhis2-emr-survey/49395 | OpenMRS/Bahmni/DHIS2 Tracker dominate DHIS2-community EMRs | self-reported (community) | medium |
| 49 | AJMC — Ambient AI adoption study | https://www.ajmc.com/view/ambient-ai-tool-adoption-in-us-hospitals-and-associated-factors | 62% of 2,784 Epic hospitals adopted ambient AI | primary (academic) | high |
| 50 | Emory Rollins — press release | https://sph.emory.edu/news/new-study-finds-nearly-two-thirds-us-hospitals-using-epic-have-adopted-ambient-ai-disparities | 62% adoption; adoption disparities | secondary (university) | high |
| 51 | JAMA Network Open — ambient AI scribes | https://jamanetwork.com/journals/jamanetworkopen/fullarticle/2839542 | Burnout 51.9%→38.8% after 30 days (263 clinicians) | primary (academic) | high |
| 52 | NEJM Catalyst — ambient AI scribes | https://catalyst.nejm.org/doi/full/10.1056/CAT.25.0040 | 2.5M+ uses after 1 year | primary (academic/practitioner) | high |
| 53 | PHTI — Adoption of AI in Healthcare (2025-03) | https://phti.org/wp-content/uploads/sites/3/2025/03/PHTI-Adoption-of-AI-in-Healthcare-Delivery-Systems-Early-Applications-Impacts.pdf | ~60 ambient scribes in practice (PDF, not parsed) | secondary (research org) | medium |
| 54 | CNBC — Epic UGM 2025 | https://www.cnbc.com/2025/08/20/epic-ugm-2025-epic-touts-new-ai-tools.html | ~200 AI features in development (2025-08-20) | secondary (journalism) | high |
| 55 | Epic — HIMSS 2025 post | https://www.epic.com/epic/post/ai-genomics-and-interoperability-at-himss-2025/ | GenAI across workflows; AI agents | self-reported (vendor) | medium |
| 56 | Epic — Art for Clinicians | https://www.epic.com/software/art/ | Ambient note drafting, message drafting | self-reported (vendor) | medium |
| 57 | Healthcare IT News — Epic AI agents | https://www.healthcareitnews.com/news/epic-unveils-ai-agents-showcases-new-foundational-models | Art, Emmie, RCM agent; Microsoft collab (fetched attempt 403 — snippet only) | secondary (journalism) | medium |
| 58 | KLAS — Epic Ambient Speech 2025 | https://klasresearch.com/report/epic-ambient-ai-2025/3920 | Epic's ambient speech entry shapes strategy | primary (paywalled; snippet) | medium |
| 59 | Fierce Healthcare — RCM gen AI survey | https://www.fiercehealthcare.com/ai-and-machine-learning/adoption-ai-hospital-rcm-surges-even-health-systems-navigate-cost | 80% of health systems on gen AI for RCM (2025) | secondary (journalism of survey) | medium |
| 60 | KLAS blog — autonomous coding | https://engage.klasresearch.com/blog/from-hype-to-reality-what-healthcare-leaders-should-know-about-autonomous-coding-solutions/8341/ | First KLAS autonomous-coding report (2025-08-26) | primary (KLAS) | medium |
| 61 | Cleveland Clinic — AKASA | https://newsroom.clevelandclinic.org/2025/04/29/cleveland-clinic-and-akasa-announce-strategic-collaboration-to-launch-ai-tools-for-the-revenue-cycle | Gen-AI coding/documentation collaboration (2025-04-29) | primary (org newsroom) | high |
| 62 | Healthcare Finance News — BSWH/Accuity | https://www.healthcarefinancenews.com/news/how-ai-transformed-revenue-cycle-baylor-scott-white-health | $42M annualized net revenue improvement expected | secondary (journalism) | medium |
| 63 | TechTarget — Waystar RCM survey | https://www.techtarget.com/revcyclemanagement/news/366627236/How-AI-is-improving-revenue-cycle-management | ≥20% realized improvements on key RCM outcomes | secondary | medium |
| 64 | Halkwinds — Healthcare Cloud Infrastructure Report | https://www.halkwinds.com/research/healthcare-cloud-infrastructure-report | 71% of hospitals run ≥1 clinical workload in cloud | secondary (research vendor, self-published) | low–medium |
| 65 | KPMG — global tech report healthcare | https://assets.kpmg.com/content/dam/kpmgsites/xx/pdf/2025/02/global-tech-report-healthcare-insights.pdf | ~70% of healthcare tech leaders have AI/automation (PDF, not parsed) | secondary (consultancy) | medium |
| 66 | Persistence Market Research — healthcare cloud | https://www.persistencemarketresearch.com/market-research/healthcare-cloud-computing-market.asp | $84.5B (2026) → $240.3B (2033), 16.1% CAGR | market research (low confidence) | low |
| 67 | SNS Insider — Healthcare SaaS | https://www.snsinsider.com/reports/healthcare-saas-market-9778 | SaaS adoption >90,000 orgs in 2025 | market research (low confidence) | low |
| 68 | Martin Fowler — Strangler Fig (+2024 rewrite) | https://martinfowler.com/bliki/StranglerFigApplication.html ; https://martinfowler.com/articles/2024-strangler-fig-rewrite.html | Strangler fig pattern; legacy displacement patterns (Cartwright/Horn/Lewis) | primary (practitioner) | high |
| 69 | The Algorithm — strangler fig for regulated systems | https://www.the-algo.com/insights/strangler-fig-regulated-migration | BAA from day one; audit continuity; avoids big-bang risk | secondary (practitioner blog) | medium |
| 70 | Dualboot Partners — strangler EHR | https://www.dualbootpartners.com/insights/strangler-fig-strategy/ | Phased EHR modernization with strangler fig + AI discovery | secondary (consultancy) | medium |
| 71 | Mindbowser — EHR modernization guide | https://www.mindbowser.com/ehr-modernization-guide/ | 60–80% IT budgets on legacy; big-bang fail 70%+ (unverified vendor claims) | secondary (consultancy, self-reported) | low |
| 72 | Webdecodes — FHIR-first API strategy | https://webdecodes.com/fhir-first-api-strategy-building-an-app-ecosystem-around-you | FHIR as contract layer; platform rules from FHIR resources | secondary (consultancy) | medium |
| 73 | Nirmitee — HL7 v2→FHIR decision framework | https://nirmitee.io/blog/hl7-v2-to-fhir-migration-decision-framework-architecture-roadmap/ | Migration drivers incl. CMS mandates; data mapping; terminology | secondary (consultancy) | medium |
| 74 | Healthcare Integrations — FHIR in mixed legacy env | https://healthcareintegrations.com/what-a-good-fhir-integration-strategy-looks-like-in-a-mixed-legacy-environment/ | Practical FHIR R4 adoption in mixed v2/API environments | secondary (consultancy) | medium |
| 75 | Nineleaps — FHIR-first products | https://www.nineleaps.com/fhir-first-healthcare-products-engineering-for-interoperability-by-design/ | FHIR as internal architecture standard + SMART on FHIR | secondary (consultancy) | medium |
| 76 | 6B — FHIR-first backend platforms | https://6b.health/insight/what-a-healthcare-software-development-company-must-consider-when-building-fhir-first-backend-platforms/ | FHIR-first as architectural stance, not checkbox | secondary (consultancy) | medium |
| 77 | MongoDB — Humana case study | https://www.mongodb.com/solutions/customer-case-studies/humana | Humana FHIR on MongoDB Atlas; avoided rip-and-replace | self-reported (vendor) | medium |
| 78 | arXiv 2408.10434 — microservices industry insights | https://arxiv.org/abs/2408.10434 | Legacy modernization among top microservices motivations (abstract only) | primary (academic abstract) | medium |
| 79 | arXiv 2411.14971 — LLMs for legacy code modernization | https://arxiv.org/abs/2411.14971 | LLM-based modernization of MUMPS/mainframe-era code (abstract only) | primary (academic abstract) | medium |
| 80 | arXiv 2510.18509 — VAPU | https://arxiv.org/abs/2510.18509 | Autonomous legacy-code modernization framework (abstract only) | primary (academic abstract) | medium |
| 81 | arXiv 2408.04660 — XMainframe | https://arxiv.org/abs/2408.04660 | LLM for COBOL/mainframe modernization (abstract only) | primary (academic abstract) | medium |
| 82 | arXiv 2409.16656 — GUIMigrator | https://arxiv.org/abs/2409.16656 | Semantics-preserving legacy UI migration (abstract only) | primary (academic abstract) | medium |
| 83 | MedCity News — Kaiser 12→2 EHRs | https://medcitynews.com/2025/12/kaiser-permanente-ehr/ | 40M records; cutovers <3 hours each | secondary (journalism) | high |
| 84 | TechTarget — Kaiser consolidation | https://www.techtarget.com/searchhealthit/feature/How-Kaiser-Permanente-consolidated-its-EHR-in-California | 12 instances → 2 (6 N. Cal + 6 S. Cal) | secondary (journalism) | high |
| 85 | Springer — Discover Health Systems HIS migration | https://link.springer.com/article/10.1007/s44250-025-00186-x | Non-incremental legacy-HIS migration framework + case study (2025) | primary (academic; metadata via OpenAlex; full text not fetched) | medium |
| 86 | OpenAlex record — DOI 10.1007/s44250-025-00186-x | https://openalex.org/W4407268304 | Metadata (title, year, OA status) via `feynman paper` | primary (metadata) | high |
| 87 | KPMG AU — Torres and Cape EMR migration | https://kpmg.com/au/en/why-kpmg/clients/torres-cape-hospital-health-derisk-emr-data-migration-client-story.html | De-risked EMR data migration (client story) | self-reported (consultancy) | low–medium |
| 88 | GAO — VA EHR data management | https://www.gao.gov/products/gao-22-103718 | Data-quality problems (allergies, meds, immunizations) | primary (government) | high |
| 89 | Thieme ACI Open — Data Migration: A Thorny Issue | https://www.thieme-connect.com/products/ejournals/html/10.1055/s-0040-1710007 | Case studies/review; migration pitfalls (metadata via OpenAlex; snippet) | primary (academic; full text not fetched) | medium |
| 90 | OpenAlex record — DOI 10.1055/s-0040-1710007 | https://openalex.org/W3029182195 | Metadata via `feynman paper` | primary (metadata) | high |
| 91 | PMC — EHR Transitions: How to Make Them Work | https://pmc.ncbi.nlm.nih.gov/articles/PMC10593672/ | Vendor-change complexity (JS-rendered; snippet only) | primary (academic; not fetched) | low–medium |
| 92 | AHIMA Journal — data migration lessons | https://journal.ahima.org/page/data-migration-lessons-learned-one-facility-relates-hardlearned-best-practices-after-migrating-to-a-new-ehr | Practitioner lessons learned | secondary (practitioner) | medium |
| 93 | Grand View Research — EHR market | https://www.grandviewresearch.com/industry-analysis/electronic-health-records-ehr-market | $35.89B (2025) → $53.11B (2033), 5.10% CAGR | market research | low–medium |
| 94 | Fortune Business Insights — EHR market | https://www.fortunebusinessinsights.com/electronic-health-records-ehr-market-102660 | $32.44B (2025) → $52.60B (2034), 5.61% CAGR | market research | low–medium |
| 95 | Precedence Research — EHR market | https://www.precedenceresearch.com/electronic-health-records-market | $28.86B (2025) → $46.40B (2035), 4.86% CAGR | market research | low |
| 96 | MarketsandMarkets — HIS market | https://www.marketsandmarkets.com/Market-Reports/hospital-information-system-market-144083226.html | $63.81B (2024) → $116.76B (2030), 10.6% CAGR | market research | low–medium |
| 97 | Mordor Intelligence — HIS market | https://www.mordorintelligence.com/industry-reports/hospital-information-system-market | $61.46B (2025) → $89.75B (2031), 6.52% CAGR | market research | low–medium |
| 98 | Market Research Future — HIS market | https://www.marketresearchfuture.com/reports/hospital-information-system-market-10732 | $175.53B (2025) → $533.71B (2035) | market research | low |
| 99 | MarketsandMarkets — Healthcare IT | https://www.marketsandmarkets.com/Market-Reports/healthcare-it-252.html | $496.09B (2025) → $1,143.67B (2031), 15.0% CAGR | market research | low–medium |
| 100 | Grand View Research — Healthcare IT | https://www.grandviewresearch.com/industry-analysis/healthcare-it-market | $866.48B (2025) → $2,864.39B (2033), 16.2% CAGR | market research | low–medium |

---

## Sources consulted (all URLs)

1. https://www.hhs.gov/hipaa/for-professionals/security/laws-regulations/index.html — HHS, Summary of the HIPAA Security Rule (fetched 2026-08-14)
2. https://www.hhs.gov/hipaa/for-professionals/faq/security-rule/index.html — HHS Security Rule FAQ
3. https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-66r2.pdf — NIST SP 800-66 Rev. 2 (not parsed, PDF)
4. https://www.hhs.gov/hipaa/for-professionals/breach-notification/index.html — HHS Breach Notification Rule
5. https://www.hipaajournal.com/hhs-applies-inflation-increase-penalties-for-hipaa-violations/ — HIPAA Journal, 2026-01-28 (fetched)
6. https://ogletree.com/insights-resources/blog-posts/2025-enforcement-trends-risk-analysis-failures-at-the-center-of-hhss-multimillion-dollar-hipaa-penalties/ — Ogletree Deakins
7. https://dmclawllc.com/2026/03/19/hipaa-enforcement-in-2025-a-year-defined-by-cybersecurity-failures/ — DMC Law
8. https://www.hhs.gov/press-room/ocr-hipaa-racap-pih.html — HHS, PIH Health settlement
9. https://www.hhs.gov/press-room/penalty-against-warby-parker.html — HHS, Warby Parker CMP
10. https://www.hhs.gov/press-room/penalty-against-or-health-science-university.html — HHS, OHSU penalty
11. https://www.legislation.gov.uk/eur/2016/679/article/9?view=plain — UK GDPR Art. 9
12. https://commission.europa.eu/law/law-topic/data-protection/rules-business-and-organisations/legal-grounds-processing-data/sensitive-data/under-what-conditions-can-my-companyorganisation-process-sensitive-data_en — European Commission
13. https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/lawful-basis/special-category-data/what-are-the-rules-on-special-category-data/ — ICO
14. https://gdpr-info.eu/art-35-gdpr/ — GDPR Art. 35 (fetched)
15. https://gdpr-info.eu/art-83-gdpr/ — GDPR Art. 83 (fetched)
16. https://www.legislation.gov.uk/eur/2016/679/chapter/V/adopted — GDPR Chapter V
17. https://www.edpb.europa.eu/sme-data-protection-guide/international-data-transfers_en — EDPB
18. https://ico.org.uk/about-the-ico/our-information/policies-and-procedures/data-protection-fining-guidance/statutory-background/the-maximum-amount-of-a-fine-under-uk-gdpr-and-dpa-2018/ — ICO fining guidance
19. https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/data-protection-principles/a-guide-to-the-data-protection-principles/ — ICO principles guide
20. https://ico.org.uk/media2/gdlfddgc/advanced-penalty-notice-20250327.pdf — ICO penalty notice, Advanced Computer Software Group (not parsed, PDF)
21. https://www.indiacode.nic.in/handle/123456789/22037?locale=en — India Code, DPDP Act 2023
22. https://www.mondaq.com/india/healthcare/1722484/health-data-the-dpdp-actnavigating-indias-healthcare-privacy-frontier — Mondaq
23. https://amlegals.com/health-data-and-the-dpdp-act-a-practical-guide/ — AmLegals
24. https://www.ey.com/en_in/insights/cybersecurity/decoding-the-digital-personal-data-protection-act-2023 — EY India
25. https://www.dpdpact2023.com/chapter-8 — DPDP Act text (unofficial mirror)
26. https://egazette.gov.in/WriteReadData/2023/248045.pdf — DPDP Act official gazette (not parsed, PDF)
27. https://federal.elaws.us/cfr/title45.part164.section164.312 — 45 CFR §164.312
28. https://www.healthcaredive.com/news/oracle-health-lost-customers-satisfaction-stagnated-cerner-acquisition-klas-research/758427/ — Healthcare Dive, 2025-08-25 (fetched)
29. https://hitconsultant.net/2026/05/14/klas-2026-ehr-market-share-report/ — hitconsultant, 2026-05-14 (fetched)
30. https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/what-oracle-has-done-since-acquiring-cerner-6-developments/ — Becker's
31. https://www.medtechdive.com/news/oracle-new-ehr-artificial-intelligence-ai/757660/ — MedTech Dive
32. https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/oracle-health-launches-new-ai-powered-ehr/ — Becker's
33. https://www.fiercehealthcare.com/ai-and-machine-learning/oracle-ramps-healthcare-ai-tech-unveils-new-features-patients-prior-auth — Fierce Healthcare
34. https://www.fiercehealthcare.com/health-tech/epic-continues-grow-ehr-market-share-it-makes-gains-small-health-systems — Fierce Healthcare
35. https://engage.klasresearch.com/blog/uncertainty-on-multiple-fronts-slowed-ehr-purchases-insights-from-klass-2026-acute-care-ehr-market-share-report/10709/ — KLAS blog
36. https://ehr.meditech.com/news/hca-healthcare-uk-goes-on-the-cloud-with-meditech-expanse-ehr-powering-patient-centred-care — MEDITECH, 2025-12-18
37. https://ehr.meditech.com/news/la-paz-regional-hospital-connects-care-across-the-community-with-centralized-meditech-expanse — MEDITECH, 2025-07-02
38. https://ehr.meditech.com/news/comanche-county-memorial-hospital-chooses-meditech-expanse-for-a-unified-health-record — MEDITECH, 2025-09-03
39. https://ehr.meditech.com/news/silver-hill-hospital-signs-for-meditech-expanse-for-integrated-and-patient-centered-care — MEDITECH, 2025-05-01
40. https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/upmc-selects-meditech-ehr-for-ireland/ — Becker's, 2025-06-05
41. https://klasresearch.com/report/us-acute-care-ehr-market-share-2025-strength-of-partnership-is-key-in-decisions/3640 — KLAS (paywalled)
42. https://www.techtarget.com/searchhealthit/news/366643293/Health-systems-delay-EHR-purchases-amid-uncertainty-rising-AI-investment — TechTarget
43. https://openmrs.atlassian.net/wiki/spaces/docs/pages/282624121/OpenMRS+Around+the+World — OpenMRS wiki
44. https://www.sciencedirect.com/science/article/abs/pii/S1386505621000319 — Int J Med Inform, OpenMRS as a global good
45. https://talk.openmrs.org/t/huge-milestone-o3-production-sites-passes-1-000-2-000-locations/44128 — OpenMRS Talk
46. https://link.springer.com/article/10.1186/s12911-025-03283-2 — BMC Med Inform Decis Mak 2025
47. https://en.info.shr.dghs.gov.bd/hms/ — Bangladesh DGHS HMS (OpenMRS+)
48. https://talk.openmrs.org/t/openmrs-features-strongly-in-dhis2-emr-survey/49395 — OpenMRS Talk
49. https://www.ajmc.com/view/ambient-ai-tool-adoption-in-us-hospitals-and-associated-factors — AJMC
50. https://sph.emory.edu/news/new-study-finds-nearly-two-thirds-us-hospitals-using-epic-have-adopted-ambient-ai-disparities — Emory Rollins
51. https://jamanetwork.com/journals/jamanetworkopen/fullarticle/2839542 — JAMA Network Open
52. https://catalyst.nejm.org/doi/full/10.1056/CAT.25.0040 — NEJM Catalyst
53. https://phti.org/wp-content/uploads/sites/3/2025/03/PHTI-Adoption-of-AI-in-Healthcare-Delivery-Systems-Early-Applications-Impacts.pdf — PHTI (not parsed, PDF)
54. https://www.cnbc.com/2025/08/20/epic-ugm-2025-epic-touts-new-ai-tools.html — CNBC
55. https://www.epic.com/epic/post/ai-genomics-and-interoperability-at-himss-2025/ — Epic
56. https://www.epic.com/software/art/ — Epic
57. https://www.healthcareitnews.com/news/epic-unveils-ai-agents-showcases-new-foundational-models — Healthcare IT News
58. https://klasresearch.com/report/epic-ambient-ai-2025/3920 — KLAS (paywalled)
59. https://www.fiercehealthcare.com/ai-and-machine-learning/adoption-ai-hospital-rcm-surges-even-health-systems-navigate-cost — Fierce Healthcare
60. https://engage.klasresearch.com/blog/from-hype-to-reality-what-healthcare-leaders-should-know-about-autonomous-coding-solutions/8341/ — KLAS blog
61. https://newsroom.clevelandclinic.org/2025/04/29/cleveland-clinic-and-akasa-announce-strategic-collaboration-to-launch-ai-tools-for-the-revenue-cycle — Cleveland Clinic
62. https://www.healthcarefinancenews.com/news/how-ai-transformed-revenue-cycle-baylor-scott-white-health — Healthcare Finance News
63. https://www.techtarget.com/revcyclemanagement/news/366627236/How-AI-is-improving-revenue-cycle-management — TechTarget
64. https://www.halkwinds.com/research/healthcare-cloud-infrastructure-report — Halkwinds Research
65. https://assets.kpmg.com/content/dam/kpmgsites/xx/pdf/2025/02/global-tech-report-healthcare-insights.pdf — KPMG (not parsed, PDF)
66. https://www.persistencemarketresearch.com/market-research/healthcare-cloud-computing-market.asp — Persistence
67. https://www.snsinsider.com/reports/healthcare-saas-market-9778 — SNS Insider
68. https://martinfowler.com/bliki/StranglerFigApplication.html + https://martinfowler.com/articles/2024-strangler-fig-rewrite.html — Fowler
69. https://www.the-algo.com/insights/strangler-fig-regulated-migration — The Algorithm
70. https://www.dualbootpartners.com/insights/strangler-fig-strategy/ — Dualboot Partners
71. https://www.mindbowser.com/ehr-modernization-guide/ — Mindbowser
72. https://webdecodes.com/fhir-first-api-strategy-building-an-app-ecosystem-around-you — Webdecodes
73. https://nirmitee.io/blog/hl7-v2-to-fhir-migration-decision-framework-architecture-roadmap/ — Nirmitee
74. https://healthcareintegrations.com/what-a-good-fhir-integration-strategy-looks-like-in-a-mixed-legacy-environment/ — Healthcare Integrations
75. https://www.nineleaps.com/fhir-first-healthcare-products-engineering-for-interoperability-by-design/ — Nineleaps
76. https://6b.health/insight/what-a-healthcare-software-development-company-must-consider-when-building-fhir-first-backend-platforms/ — 6B
77. https://www.mongodb.com/solutions/customer-case-studies/humana — MongoDB
78. https://arxiv.org/abs/2408.10434 — arXiv (microservices insights)
79. https://arxiv.org/abs/2411.14971 — arXiv (LLMs for legacy code modernization)
80. https://arxiv.org/abs/2510.18509 — arXiv (VAPU)
81. https://arxiv.org/abs/2408.04660 — arXiv (XMainframe)
82. https://arxiv.org/abs/2409.16656 — arXiv (GUIMigrator)
83. https://medcitynews.com/2025/12/kaiser-permanente-ehr/ — MedCity News
84. https://www.techtarget.com/searchhealthit/feature/How-Kaiser-Permanente-consolidated-its-EHR-in-California — TechTarget
85. https://link.springer.com/article/10.1007/s44250-025-00186-x — Springer, Discover Health Systems
86. https://openalex.org/W4407268304 — OpenAlex metadata (via `feynman paper`)
87. https://kpmg.com/au/en/why-kpmg/clients/torres-cape-hospital-health-derisk-emr-data-migration-client-story.html — KPMG AU
88. https://www.gao.gov/products/gao-22-103718 — GAO
89. https://www.thieme-connect.com/products/ejournals/html/10.1055/s-0040-1710007 — ACI Open (Thieme)
90. https://openalex.org/W3029182195 — OpenAlex metadata (via `feynman paper`)
91. https://pmc.ncbi.nlm.nih.gov/articles/PMC10593672/ — PMC
92. https://journal.ahima.org/page/data-migration-lessons-learned-one-facility-relates-hardlearned-best-practices-after-migrating-to-a-new-ehr — AHIMA Journal
93. https://www.grandviewresearch.com/industry-analysis/electronic-health-records-ehr-market — Grand View
94. https://www.fortunebusinessinsights.com/electronic-health-records-ehr-market-102660 — Fortune Business Insights
95. https://www.precedenceresearch.com/electronic-health-records-market — Precedence
96. https://www.marketsandmarkets.com/Market-Reports/hospital-information-system-market-144083226.html — MarketsandMarkets
97. https://www.mordorintelligence.com/industry-reports/hospital-information-system-market — Mordor
98. https://www.marketresearchfuture.com/reports/hospital-information-system-market-10732 — MRFR
99. https://www.marketsandmarkets.com/Market-Reports/healthcare-it-252.html — MarketsandMarkets
100. https://www.grandviewresearch.com/industry-analysis/healthcare-it-market — Grand View

Additional arXiv/alphaXiv results considered (health-data security & AI frameworks; abstract-only):
- https://arxiv.org/abs/2504.17669 — Towards a HIPAA Compliant Agentic AI System in Healthcare
- https://arxiv.org/abs/2510.02325 — Agentic-AI Healthcare: privacy-first framework (RBAC, AES-GCM, audit logging)
- https://arxiv.org/abs/2509.24224 — ML adoption framework on legacy systems
- https://arxiv.org/abs/2602.01086 — AI-native clinical context graph / FHIR (metadata only)
- https://arxiv.org/abs/2607.fhir-server-security-vulnerabilities — FHIR server security case study (metadata only)

---

## Blocked / Unverified items

1. **`feynman_science_database_search` (pubmed/openalex direct tool): BLOCKED** — not present in this subagent's toolset. Substituted with `feynman alpha search` (alphaXiv) for paper discovery and `feynman paper <doi>` (OpenAlex/Europe PMC metadata resolution) for the two key migration papers; metadata-only, no PDFs fetched (consistent with workflow rule). PubMed keyword search remains not-run via this route.
2. **PDFs intentionally not parsed** (workflow rule): NIST SP 800-66r2, CMS HIPAA basics PDF, egazette DPDP text, ICO Advanced penalty notice, PHTI report, KPMG report, Thieme ACI Open PDF, GAO report. All cited from search snippets/metadata; numbers quoted from these are marked accordingly.
3. **KLAS report pages** (klasresearch.com/report/...) are paywalled — only summary figures re-reported by Healthcare Dive, hitconsultant, Fierce Healthcare, KLAS's own blog were used; direct KLAS report text not read.
4. **Springer case study** (10.1007/s44250-025-00186-x): full text not fetched (fetch returned incomplete content); abstract-level claims only, metadata verified via OpenAlex.
5. **PMC article 10593672** and **healthcareitnews.com**: fetch failed (JS-rendered / HTTP 403); snippet-level claims only.
6. **HIPAA Security Rule NPRM (Dec 2024)**: existence confirmed via the fetched HHS summary page; NPRM text/dates not fetched — flagged for follow-up if specific proposal details are needed.
7. **Mindbowser statistics** ("60–80% of IT budgets on legacy maintenance"; "big-bang EHR replacements fail 70%+") are vendor-consultancy claims, not corroborated by an independent primary source — treat as low-confidence.
8. **Market-size figures** vary widely across analyst houses (scope/method differences); quoted with firm + year; treat as directional only.
9. **Halkwinds 71% cloud-clinical-workload figure** and **KPMG ~70% AI/automation figure**: self-published research-vendor / consultancy survey results; methodology not independently verified.
10. **MEDITECH generative-AI features**: no flagship generative-AI product announcement found in this run — presence/absence unverified.
11. ICO "maximum fine" page URL guessed initially returned 404; corrected URL found via search and cited (item 18).

## Coverage Status

- Checked directly (fetched pages): HHS Security Rule summary (full), HHS enforcement landing, Healthcare Dive Oracle article, hitconsultant KLAS-2026 summary, HIPAA Journal penalty table, gdpr-info.eu Art. 83 + Art. 35, Martin Fowler strangler-fig rewrite, OpenAlex metadata for 2 papers. — done
- Search-snippet level (not fetched): most vendor news, market research, ICO Advanced notice, GAO, PHTI, KPMG, PMC article. — done with confidence labels
- Not available: feynman_science_database_search tool, PubMed direct search, KLAS paywalled text, some PDF full texts. — blocked/unverified
- All current-date (2025–2026) claims trace to 2025–2026 web sources; no invented market shares, penalty figures, or features.
