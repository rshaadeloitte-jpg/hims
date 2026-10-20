# The Whole HMIS System — Research Brief

**Cited:** 2026-08-14 (supersedes the uncited draft `hims-system-research-draft.md`)
**Topic:** Health/Health Management Information Systems (HMIS/HIMS) — definition, history, functional architecture, standards, compliance, vendor landscape, and modernization.
**Method:** 4 parallel researcher tracks (definition/history, functional architecture, standards/interoperability, compliance/market/modernization); 75+ sources; local workspace docs used only as case context (marked LOCAL). The draft's bracket refs like [T1-27] (pointing to numbered evidence tables in `outputs/.drafts/hims-system-research-T1..T4.md`) have been converted to a unified inline-citation scheme [1]–[229] (slots 59 and 127 intentionally unassigned) defined in Section 13 below; every Sources entry also carries its research-note ref (e.g., `(T1-27)`) for traceability. Unreachable URLs are marked `[unreachable: …]`; PDFs are marked as metadata-verified only (full-text parsing not performed, per run rules).

---

## 1. Executive summary

- **What it is.** "HMIS" is ambiguous in the field. In WHO/global-health usage, HMIS (health *management* information system) = the routine facility-to-national aggregate reporting subsystem of a national health information system [1][2]. In software/industry usage, HMIS/HIMS = the hospital-level enterprise information system (registration, billing, labs, pharmacy, radiology, etc.), which academia calls a *hospital information system* (HIS) [3]. This brief covers both, with emphasis on the hospital-level system and its modernization, since that matches this workspace's legacy HMIS context.
- **History.** Hospital information systems went from paper records and record-librarian profession (1928) [101] → early experimental hospital-wide systems (Lockheed/El Camino 1965–74 [4], HELP 1967 [5], PROMIS [11], TMIS 1964–87 [10]) → integrated vendor suites (1990s–2000s, e.g., MUMPS-based Epic/Meditech [12][13]) → mass EHR adoption under HITECH (US: <10% basic EHR in 2008 → 99.4% in 2024) [7] → web/cloud/FHIR-API era (FHIR first presented 2012 [110]; R4 normative 2019 [6]; R5 2023 [6][8]; R6 first normative ballot Jan 2026) [9].
- **Standards.** HL7 v2 remains "the workhorse" for clinical messaging (ADT/orders/results/pharmacy) [170][171]; FHIR is the modern API layer [6][8][9]; DICOM handles imaging [15][16]; IHE profiles supply cross-enterprise plumbing (XDS, PIX/PDQ/XCPD, and FHIR-R4 successors PMIR/PIXm/PDQm/MHD) [19][20][21][22][26][27][28]; SNOMED CT/LOINC/ICD/RxNorm/UCUM form the terminology stack [17][18][29][30][31][32][33].
- **Compliance.** HIPAA (US), GDPR/UK GDPR (EU/UK), and India's DPDP Act 2023 dominate [34][37][44]; health data is a special category under GDPR (fines to €20M/4% turnover) [37][38][49]; HIPAA's Security Rule requires audit controls with encryption addressable-but-expected [34][35][36], and enforcement has concentrated on cybersecurity failures (risk analysis #1 finding in 2025) [41][42].
- **Market.** In 2024–25, Epic held >42% of US acute-care hospital share vs Oracle Health ~23% (falling) [60][66]; Meditech ~14.7% [66]. Oracle's Cerner acquisition has lost customers [60][61][62]; Oracle launched an AI-first cloud EHR (ambulatory) in Aug 2025 [63][64][65]. Purchase decisions dropped ~40% in 2025 amid policy uncertainty [61][67][68][69].
- **Modernization.** Industry practice converges on strangler-fig/incremental displacement [71][72], FHIR-first or FHIR-facade API layers [77][156], microservices (43.8% of 89 surveyed HIS architecture studies, with a trend toward event-driven/hybrid designs) [70], and careful data migration — with documented failure cases (VA/Oracle EHRM data-quality problems per GAO) [73][92].
- **Biggest caveats.** Analyst market-size figures disagree by >2× on the same year; OpenMRS/DHIS2 adoption counts are self-reported; several high-profile claims rest on vendor/consultancy material. Confidence per claim is labeled throughout.

## 2. Terminology & taxonomy

Definitions (primary sources):
- **WHO:** routine health information systems (RHIS) are "also termed health management information systems (HMIS)" and constitute the health-services data subsystem of national HIS [1].
- **WHO/Europe:** a health information system is "a complex, multilevel system, aimed at producing health intelligence to inform decision-making, and encompassing data collection, analysis, health reporting, knowledge management and governance" [95].
- **Global health literature:** HMIS "collects, stores, analyses, and evaluates health-related data from health facility to district, regional and national administrative levels… also referred to as routine health information systems" [2].
- **Academic informatics (Winter et al.):** HIS is "the socio-technical subsystem of a health-related setting which comprises all data, information, and knowledge processing as well as the associated human or technical actors" [96].
- **NLM MeSH (hospital level):** Hospital Information Systems are "integrated, computer-assisted systems designed to store, manipulate, and retrieve information concerned with the administrative and clinical aspects of providing medical services within the hospital" [3].
- **ONC:** EMR = digital version of the paper chart, not designed to travel; EHR = broader, spans all clinicians/organizations involved in care [97].

| Term | Level | What it is | Source |
|---|---|---|---|
| Health Information System (HIS) | National / health-system | Umbrella: all systems + actors producing/using health data | [96][98] |
| HMIS / RHIS | Facility → national | Routine aggregate reporting on service delivery, HR, supplies | [1][2] |
| Hospital Information System (hospital HIS) | Single hospital | Integrated administrative + clinical enterprise system | [3] |
| EMR | Practice/department | Digital chart; not designed to cross organizations | [97] |
| EHR | Multi-organization | Patient-lifetime record across care settings; part of HIS | [96][97] |
| LIS / RIS / CIS | Department | Laboratory, radiology, and clinical information subsystems | [99] |

**Interpretive note (inference):** "HIMS" (health/hospital information *management* system) appears in industry and some academic usage but has no standardized WHO definition; "HMIS" is the standard WHO/global-health acronym. Which meaning applies must be fixed per context. This workspace's legacy iHMS/OHUM system is a hospital-level HIS/HMIS (LOCAL context [100]).

## 3. History & evolution

Dated milestones (sources in brackets):
- **1928** — Health information *management* profession origins: Association of Record Librarians of North America (under American College of Surgeons) [101].
- **1961** — HIMSS founded (as Hospital Management Systems Society) — Wikipedia-only, treat as secondary [102].
- **1964–1987** — Technicon Medical Information System (TMIS), systems-theory-era hospital-wide MIS [10].
- **1965–1974** — First hospital-wide computerized medical information system: Lockheed + El Camino Hospital (IEEE-recognized milestone) [4].
- **1967** — HELP system operational at LDS Hospital (later full hospital IS with clinical decision support) [5].
- **1970s** — PROMIS problem-oriented record (Weed, U. Vermont) [11]; 1977 US OTA survey of medical information systems (TMIS, COSTAR) [103].
- **1987** — MeSH descriptor "Hospital Information Systems" introduced [3]; 1991 Collen historical review of US HIS evolution [104].
- **1994** — HISP founded (U. Oslo + U. Western Cape) → DHIS → DHIS2 [105].
- **2004** — OpenMRS born (Eldoret, Kenya; AMPATH) [106].
- **2008** — WHO Health Metrics Network framework for country HIS [107].
- **2009** — US HITECH Act (ARRA, P.L. 111-5) — EHR incentives/meaningful use [108].
- **2010** — SMART project starts (→ SMART on FHIR) [109].
- **2012–13** — FHIR first presented May 2012; DSTU1 Sep 2013 [6][110].
- **2015** — 96% of US non-federal acute-care hospitals using certified health IT [111].
- **2016** — 21st Century Cures Act data-sharing mandates; API/FHIR-based access became the central interoperability mechanism in subsequent ONC rules [7][195].
- **2021** — WHO strategy for optimizing national RHIS [112].
- **2024** — US: 99.4% hospital certified-EHR adoption; Epic ~50% of hospital market; top-3 developers >80% [7].
- **2025** — EHDS Regulation (EU) 2025/327 published [113]; FHIR 2025 State-of-FHIR survey: 71% of surveyed countries report active FHIR use [114].

Era synthesis: **paper records** (pre-1960s) → **departmental systems** (1960s–80s, early academic prototypes) → **integrated hospital IS** (1990s–2000s, vendor platforms) → **mass adoption + interoperability mandates** (2010s) → **cloud/FHIR/API + AI era** (2020s) [4][7][104][115][120][121][129].

## 4. Functional architecture & modules

### 4.1 Core module set
A government cloud-HIS reference (India's e-Hospital) enumerates 11 modules: Patient Registration (OPD/Emergency), ADT (IPD), Billing, Laboratory (LIS), Radiology (RIS), Clinics, Store & Inventory, Pharmacy, Dietary, Laundry, OT Management [131]. India's ABDM MVP spec defines required functional modules for ABDM-compliant HMIS [132]. The local iHMS catalog (LOCAL) enumerates ~29 modules in the same families plus IAM, RCM/claims, reporting/search, and government programs [100].

| Module | Function | Typical data flows |
|---|---|---|
| Patient registration / MPI | Identity, demographics, unique MRN, dedup | Broadcasts registration/ADT events downstream; demographics consumed by lab/rad/billing |
| ADT (admission–discharge–transfer) | Inpatient stay lifecycle | Emits HL7 ADT events (A01/A02/A03) to LIS/RIS/pharmacy/dietary/billing [133] |
| OPD / appointments / queues | Booking, walk-ins, tokens | Encounter → orders → billing |
| Billing / finance / claims | Charges, invoices, payments, claims | Consumes encounter/order events; charge master (CDM) syncs codes [134]; account transactions (charge/payment/adjustment) [135] |
| Pharmacy | Dispensing, compound, ward lists | Consumes orders; emits dispensing → inventory + charge |
| Laboratory (LIS) | Orders, specimens, results, machine interfaces | ORM in; ORU results out [133] |
| Radiology (RIS/PACS) | Imaging orders, scheduling, results | ORM + DICOM MWL; reports → EMR [137][138] |
| Blood bank | Donors, components, cross-match, issue | Consumes ADT + serology; issues → inventory |
| Inventory / supply chain | Item master, stock, batches, procurement | Consumes consumption events; supplies pharmacy/OT/CSSD |
| Reporting / analytics | Dashboards, exports | ETL OLTP → warehouse/OLAP (KPSC RDW: 150+ source tables → 14-table marts) [136] |
| OT, nursing, diet, CSSD, linen, transport, equipment, BMW | Full-suite additions | Consume ADT/encounter events; emit charges/consumption |

*Module-table sourcing note: rows without explicit citations are synthesized from the module catalogs of e-Hospital [131], the ABDM MVP spec [132], the local iHMS catalog [100], and the HL7 v2 integration-flow evidence in [133] (ADT/order/result flows) — they do not carry independent per-row sources.*

### 4.2 Legacy architecture patterns (with evidence)
- **MUMPS-based systems.** MUMPS (1966, MGH) is a language + integrated hierarchical key-value DB [12][139][140]; Epic, MEDITECH, VA VistA, and InterSystems Caché all descend from it [12][13][140]. MEDITECH founded 1969, "generally considered the first EHR company," predating Epic/Cerner (both 1979) [14][141].
- **Mainframe → two-tier client/server.** Asan Medical Center (2,200 beds) migrated mainframe HIS to Unix client/server 1993–96 [142]; VAMC FileMan systems migrated to two-tier/browser then multi-tier components [143].
- **Monolithic shared-DB.** Legacy generations = monolith over one shared DB with stored procedures; the local iHMS is stored-procedure-centric (719 Oracle procs) with a mega-transaction coupling Order→Bill→Lab→Rad→EMR→HL7 (LOCAL [144]).
- **Aging frontends.** Documented stacks: PowerBuilder client-server (US gov healthcare) [145]; ASP.NET Web Forms + monolithic SQL Server [146][147]; VB6/WinForms desktop [148]; ActiveX browser controls (removed by Hyland in Foundation EP5) [149].
- **Evolution-by-integration.** Broussais University Hospital strategy: evolve legacy HMIS via distributed object-oriented integration rather than rewrite [150].

### 4.3 Integration topologies
- **Point-to-point:** n systems → n(n−1)/2 interfaces (6→15, 10→45) [151]; the accumulated mesh of HL7 v2 feeds, custom APIs, batch files, FHIR endpoints is fragile [152].
- **Interface engine / hub-and-spoke:** routes HL7 messages between HIS, LIS, end-user apps [151][153].
- **HL7 v2 message/event model:** ADT (A01/A02/A03), ORM orders, ORU results, BAR patient accounting; segments MSH/EVN/PID/PV1; ACK confirmation; transport via MLLP, text files, FTP, or disk [133].
- **Direct DB links / shared database:** "integrated" style vs "interfaced" message style; data/semantic/UI/process integration aspects [133].
- **ETL → data warehouse:** healthcare DWs consolidate clinical+financial data; legacy systems often lack open APIs; terminology mapping (ICD/SNOMED) is a core burden [154]; most public clinical-DW architectures use i2b2-like star/snowflake schemas [155].
- **FHIR-era:** facade (FHIR API over legacy), hybrid (legacy stays source of truth; sync to FHIR server one system at a time), FHIR-native [156].

### 4.4 Modern reference architectures
- **Systematic review (2025):** of 89 included HIS-architecture studies (2020–2025), microservices dominate (43.8%, n=39), then blockchain/distributed-ledger (33.7%, n=30), then layered (14.x%); trend toward hybrid event-driven/edge; FHIR/openEHR named as interface stabilizers [70]. (Note: "blockchain" count likely reflects publication interest, not production deployment — inference.)
- **Open-source modular:** OpenMRS = Java core API + optional modules (OMODs) [157]; Bahmni integrates OpenMRS + Odoo + OpenELIS via ATOM/RSS feeds consumed by background jobs [158].
- **FHIR-native microservices:** domain services over FHIR contracts [159]; hybrid FHIR + operational data layer reference patterns [160].
- **National blueprints:** Bhutan MoH HIS blueprint uses TOGAF-enabled OpenHIE framework [161] (snippet-level).

### 4.5 Data model highlights
- **EMPI/MPI:** MPI links patients within an institution; EMPI spans institutions; in use since the 1990s [162][163].
- **Encounter model:** FHIR Encounter = patient-provider interaction (ambulatory/emergency/inpatient/virtual) [164]; PCORnet CDM v7 ENCOUNTER = unique PATID+ADMIT_DATE+PROVIDERID+ENC_TYPE with diagnoses/procedures in separate tables [165].
- **Orders:** USCDI v7 order classes: medication, laboratory, diagnostic imaging, clinical test, procedure, nutrition [166].
- **Financial model:** charge master (HL7 v2 CDM segment) [134] → ChargeItem capture [167] → account transactions [135] → claims (UB-04 revenue codes, HCPCS/CPT) [168][169] — consistent across FHIR, HL7, IBM, Epic, and UTSW sources [134][135][167][168][169].

## 5. Standards & interoperability

### 5.1 Messaging/interface standards (current status)
- **HL7 v2.x:** "the workhorse of electronic data exchange in the clinical domain and arguably the most widely implemented standard for healthcare in the world" (HL7's own claim) [170]; v2.9/v2.9.1 product briefs published (2.9.1 normative status unverified) [170][171].
- **HL7 FHIR:** R4 (4.0.1) = first release with normative content, most implemented [6]; R5 (5.0.0, 26 Mar 2023) = current published version, Trial Use [6][8]; HL7 plans next release as normative [6][8]; **R6 first normative ballot completed Jan 2026** [9].
- **DICOM (NEMA PS3):** imaging storage/transport incl. DICOMweb RESTful services; republished several times per year (e.g., 2025e) [15][16].
- **IHE profiles:** XDS (cross-enterprise document sharing), PIX/PDQ/XCPD (identity), SWF.b (radiology workflow; mandates HL7 v2.5.1) [19][138], plus FHIR R4 successors PMIR v1.6.0 [20], PIXm v3.1.0 [21], PDQm [22], MHD v4.2.x [27] — IHE is converging its ITI domain onto FHIR [19][20][21][22][27].

### 5.2 Clinical terminologies
SNOMED CT (clinical concepts; monthly international releases) [29], LOINC (lab/observations; v2.82, Feb 2026; 109,325 concepts) [30], ICD-10/ICD-10-CM (dominant billing classification; ICD-11 in effect globally since 1 Jan 2022) [33][172][173][174], CPT (US procedure billing) [17], RxNorm (drugs, RXCUI) [31], UCUM (units) [32] — all are ONC "applicable vocabulary standards" in the US [17][175]. **Inference:** an exchange-ready HMIS encodes coded elements with at least one of these systems.

### 5.3 How standards enable module integration
- **Lab:** OML^O21 (order) → ORL^O22 (ack) → ORU^R01 (results) [176][177]; IHE PaLM LAB-1/LAB-3 map these [176][178].
- **Radiology:** ORM orders + DICOM Modality Worklist → modality → PACS; ORU results [137][179]; IHE SWF.b + Radiology TF Appendix B formalize the HL7↔DICOM-MWL bridge [137][138].
- **Pharmacy:** OMP (order) → RDE^O11 (encoded order) → RDS^O13 (dispense) / RGV (give) / RAS (administration) [180][181].
- **Identity:** EMPI + IHE PIX/PDQ/XCPD [163][182] and FHIR-based PMIR/PIXm/PDQm [19][20][21][22].
- **Documents:** CDA R2 (2024 online edition current) [23][24] and FHIR documents (Bundle type `document` starting with Composition; can persist in IHE XDS) [25][26][27][28].

### 5.4 Open-source ecosystems
- **OpenMRS:** FHIR R4 support via FHIR2 module (translation layer over custom data model, HAPI-based) [183][184][185]; self-reported 80+ countries, 8,100+ sites, 22M+ patients (self-reported — treat as vendor claim) [186][187].
- **Bahmni:** OpenMRS + Odoo + OpenELIS + DCM4Chee, integrated via ATOM feeds; for low-resource settings [158][188].
- **DHIS2:** itself the world's largest HMIS by self-report (70+ countries national-scale, >40% of world population) [189][190]; aggregate + tracker (patient-based) statistical data — a routine-information platform, not a clinical EMR [191]; original dhis2-fhir-adapter archived with an active fork [192]; official FHIR guidance exists [193][194].
- **Inference:** a common LMIC architecture = OpenMRS/Bahmni at facility + DHIS2 at district/national level.

### 5.5 National/regional frameworks (dated)
- **US:** ONC (ASTP) certification under HTI-1 [197][198]; USCDI v7 released 2026-07-23 [199]; TEFCA live since Dec 2023, 11 designated QHINs (Epic, Oracle Health, eClinicalWorks, etc.) [196], Common Agreement v2.1 (Feb 2025) [196]; RCE self-reports 21,000+ orgs, 96,000+ connections, 1.2B+ documents [200][201] (self-reported).
- **EU:** EHDS Regulation (EU) 2025/327 published 5 Mar 2025, in force 26 Mar 2025 [113][202]; implementing acts by Mar 2027; first-priority primary-use exchange (patient summaries, ePrescriptions) from Mar 2029, second group (images, labs, discharge reports) from Mar 2031 [113].
- **UK:** FHIR UK Core (R4) API standards [203]; Care Connect (STU3, active) [204]; Booking and Referral Standard (BaRS) [205]; ITK3 messaging over MESH [203].
- **India:** ABDM — 73.98 crore ABHA created (Feb 2025) [206][207] → 79.71 crore by Jul 2025 [208]; 49→65 crore records linked [206][208]; 100+ crore records linked milestone [209].

### 5.6 FHIR adoption trends
- **2025 State of FHIR survey (82 experts, 52 countries):** 71% report active FHIR use in their country, up from 66% in 2024 [114][210][211]; diagnostic orders/reports now the leading use case; top challenge = lack of FHIR knowledge [210][211].
- **US hospitals (ASTP Data Brief 79, Aug 2025):** 2024: 81% enabled patient access via API-configured apps, 70% via FHIR-configured apps (57% in 2021); small/rural/critical-access/independent hospitals lag; market-leading EHR vendor: 83% FHIR app access vs 56% others [195].

## 6. Security & compliance

### 6.1 HIPAA (US)
- **Privacy Rule (45 CFR 160/164 subpart E):** protects all PHI; patient rights (access, amendment); minimum necessary; covered entities + business associates [34].
- **Security Rule (164 subpart C):** administrative/physical/technical safeguards for ePHI — risk analysis required, unique user ID required, **audit controls required (164.312(b))** [34][36]; RBAC and encryption are addressable (not per se mandated) [34][35], but OCR's enforcement record treats them as expected [34][36].
- **Breach Notification Rule (164.400–414):** notify individuals, HHS, and media for large breaches of unsecured PHI [43].
- **Penalties (2025 inflation-adjusted):** four HITECH culpability tiers from $145–$2,190,294 per violation (with OCR enforcement discretion halving/capping three tiers); annual caps $36,505.50–$2,190,294 [40]. Enforcement context: 10 resolution agreements Jan–May 2025; inadequate risk analysis the top finding; ransomware the most common trigger [41][42]. Example settlements: PIH Health $600k (phishing) [56], Warby Parker $1.5M CMP [57], OHSU $200k (right-of-access) [58].
- **Current item:** HHS has published a proposed rule (NPRM) to modify the HIPAA Security Rule for stronger cybersecurity (existence confirmed on HHS page; NPRM text not fetched) [34].

### 6.2 GDPR / UK GDPR / DPA 2018
- Health data = special category (Art. 9): processing prohibited unless an Art. 9(2) condition applies [38][49][50].
- DPIA required for large-scale special-category processing (Art. 35(3)(b)) [39].
- Fines: up to **€20M or 4% worldwide turnover** (higher tier, incl. Art. 9); €10M/2% lower tier; Art. 25/32 measures are a fine-calibration factor [37].
- UK: same structure; cap **£17.5M or 4%** [51][52]; dated example: ICO fined Advanced Computer Software **£3.08M (March 2025)** for a health-data ransomware breach [53] (penalty notice PDF; amount via snippet-level source — exact notice date unverified this run).
- Transfers: Chapter V conditions (adequacy/SCCs) [54][55] — no explicit EU residency mandate, but transfer rules plus DPIA obligations operate as de-facto residency constraints for EU health data (inference) [54][55].

### 6.3 India DPDP Act 2023
- First Indian data-protection statute (Act 22 of 2023, enacted 2023-08-11) [44]; **no separate health-data category** — health data is ordinary personal data under a consent-based framework [44][45].
- DPDP Rules 2025 notified (consent, child data, significant data fiduciaries, breach notification) [47].
- Penalties up to **₹250 crore** for serious contraventions [46]; §16 empowers government to restrict cross-border transfers to notified territories [48].

### 6.4 Cross-cutting for HMIS (inference)
RBAC + audit logging + encryption + data residency are the common technical controls across regimes; HIPAA makes audit controls required [34], GDPR makes their absence a fine aggravator [37], and residency constraints differ (none in HIPAA; transfer-based in GDPR [54]; future restriction power in DPDP §16 [48]).

## 7. Vendor & technology landscape (2025–2026)

- **Epic:** >42% of US acute-care hospitals in 2024 vs Oracle 23% [60] (KLAS customer-share basis; distinct from the ONC certified-product-share basis cited in §3, where Epic holds ~50%) [7]; in 2025 Epic was the only vendor chosen by large health systems (>10 hospitals), adding 77 hospitals/18,679 beds [61][67]; ~200 AI features in development (Aug 2025) [120].
- **Oracle Health (ex-Cerner):** >$28B acquisition closed 2022 [60]; lost 57 unique acute-care customers in three years (12 of them >1,000-bed systems, incl. Intermountain, UPMC, Henry Ford, Adventist, ChristianaCare) [60]; share fell ~25% (2021) → 22.9% (2024) [62] [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]; third consecutive year of net losses in 2025 (−56 hospitals) [61][67]; ~1/3 of sampled customers say Oracle is not in their long-term plans [60]; launched an **AI-first, cloud-native EHR on OCI for US ambulatory providers Aug 2025** (acute care planned 2026) [63][64][65].
- **MEDITECH:** ~14.7% acute-care share, third-largest base [66]; Expanse cloud EHR + "MEDITECH as a Service" [66][212]; 84% of legacy customers chose Expanse migration in 2025 (retention record) [61][67]; 2025 go-lives incl. HCA Healthcare UK (11 hospitals, Dec 2025) [212], UPMC Ireland [216] (plus La Paz Regional [213], Comanche County [214], Silver Hill [215]).
- **Market-wide:** acute-care EHR purchase decisions fell ~40% in 2025 vs 2024 (~50% vs 2023) on policy uncertainty and capital redirected to AI [61][67][68][69]; only five vendors had validated net-new decisions in 2025 (Altera, Epic, MEDITECH, Oracle Health, TruBridge) [67].
- **Open-source in enterprise:** OpenMRS/Bahmni dominate LMIC public-health deployments [186][217][218][219][220][221] but are not significant in US acute care or Western enterprise EHR (inference).

## 8. Cloud & AI trends (dated examples)

- **Ambient AI scribes:** 62% of US hospitals on Epic adopted ambient AI documentation (study of 2,784 Epic hospitals) [116][117]; JAMA Network Open QI study: burnout 51.9% → 38.8% after 30 days (263 clinicians, 6 systems) [118]; NEJM Catalyst: 2.5M+ uses in a year at one system [119].
- **Revenue-cycle AI:** 80% of health systems exploring/piloting/implementing gen AI for RCM in 2025 [125] — survey by HFMA, commissioned by AKASA, a vendor of gen-AI RCM tools; treat as vendor-sponsored and directional; KLAS first autonomous-coding report (Aug 2025) [126]; Baylor Scott & White expects $42M annualized net revenue improvement from AI physician review [128] [unreachable: HTTP 403 (bot-blocked); content confirmed via search index].
- **Vendor AI:** Epic ~200 AI features (Art for Clinicians ambient note drafting, Emmie chatbot, RCM agent; agentic AI announced HIMSS25) [120][121][122][123][124]; Oracle Clinical AI Agent + AI-first EHR [60][63].
- **Cloud:** 71% of surveyed hospitals/health systems run ≥1 clinical workload in the cloud (self-published research vendor; methodology not independently verified) [129]; KPMG: ~70% of healthcare tech leaders report AI/automation technologies (PDF snippet) [130].

## 9. Modernization approaches for legacy HMIS

Patterns and evidence:
- **Strangler fig:** incrementally route traffic from legacy monolith to new services until legacy host is retired (Fowler 2004; 2024 rewrite with Cartwright/Horn/Lewis "patterns of legacy displacement") [71][72]. Health-system adaptations: BAA/HIPAA coverage for new services from day one; audit-trail continuity; avoid big-bang cutover [74]; wrap legacy EHR with FHIR API and modernize module-by-module [75][76].
- **FHIR-first / API-first:** FHIR as internal contract, not afterthought; CMS mandates (Patient Access API, prior authorization) as modernization drivers [77][78][79][80][81]; FHIR to the cloud without rip-and-replace (Humana/MongoDB case study — vendor self-report) [82].
- **Microservices decomposition:** legacy modernization is a top motivation for microservices adoption, but rushed modernization is risky (arXiv industry survey) [83].
- **LLM-assisted modernization:** LLMs for MUMPS/mainframe-class code migration [84]; autonomous modernization frameworks (VAPU) [85]; COBOL migration (XMainframe) [86] — arXiv abstracts only.
- **Frontend modernization:** semantics-preserving legacy UI migration (GUIMigrator-style) [87].

Documented cases:
- **Success:** Kaiser Permanente consolidated 12 California EHR instances → 2, ~40M records, each cutover <3 hours without data loss [88][89]; peer-reviewed HIS-migration framework + real non-incremental migration case study (Discover Health Systems, 2025) [90][91].
- **Failure/risk:** GAO found VA/Oracle EHRM transferred-data quality problems (allergy, medication, immunization errors posing patient-safety risk) [73]; literature review: most large data-migration projects run over budget or miss deadlines; mapping/validation core failure points [92][93]; vendor-change complexity from lab/radiology connections [94].
- **Unverified vendor claims:** "60–80% of healthcare IT budgets go to legacy maintenance; big-bang EHR replacements fail 70%+ of the time" — consultancy marketing, not corroborated [76].

## 10. Market & scale (low confidence — analyst estimates conflict)

| Market | Size (year) | Forecast | Sources |
|---|---|---|---|
| Global EHR | $28.86B (2025) | $46.4B by 2035 (4.86% CAGR) | [224] |
| Global EHR | $32.44B (2025) | $52.6B by 2034 (5.61%) | [223] |
| Global EHR | $35.89B (2025) | $53.11B by 2033 (5.10%) | [222] |
| Hospital Information Systems | $61.46B (2025) | $89.75B by 2031 (6.52%) | [226] |
| Hospital Information Systems | $63.81B (2024) | $116.76B by 2030 (10.6%) | [225] |
| Hospital Information Systems | $175.53B (2025) | $533.71B by 2035 | [227] |
| Healthcare IT | $496.09B (2025) | $1,143.67B by 2031 (15.0%) | [228] |
| Healthcare IT | $866.48B (2025) | $2,864.39B by 2033 (16.2%) | [229] |

Defensible adoption anchor (primary source): US non-federal acute-care hospitals <10% basic EHR (2008) → 99% certified EHR (2018) → 99.4% (2024) [7]. **Caveat:** analyst houses differ by >2× on the same base year; treat as order-of-magnitude only.

## 11. Disagreements & caveats

1. **Terminology ambiguity** — "HMIS/HIMS" means different things in WHO/global-health vs industry usage; no single standardized definition [1][2][3][95][96][98].
2. **Market-size conflicts** — EHR 2025 estimates range $28.9–35.9B; HIS 2024/25 $61–176B depending on house [222][223][224][225][226][227].
3. **FHIR versioning** — R5 is current but Trial Use; R4 holds the normative content most targets; R6 normative ballot completed Jan 2026 [6][8][9].
4. **HIPAA penalty numbers** — 2025 adjusted figures differ by source because OCR enforcement discretion halves/caps three tiers [40].
5. **Self-reported adoption counts** — OpenMRS (80+ countries, 22M+ patients) and DHIS2 (70–80+ countries) counts are project self-reports, not independently verified [186][187][189][190].
6. **Consultancy/analyst claims** — Mindbowser "60–80% budget / 70%+ failure" figures unverified [76]; Halkwinds 71% cloud figure self-published [129]; KLAS report pages paywalled, figures via trade press [60][61].
7. **HIMSS founding year (1961)** — Wikipedia-only [102]; MEDITECH generative-AI features — no announcement found (absence unverified; no URL — see research note T4, Blocked item 10).
8. **Epic share figures use different measurement bases** — ONC certified-product share ~50% [7] vs KLAS acute-care customer-share >42% [60]; not directly comparable (see §3 vs §7).
9. **Vendor-sponsored survey** — the "80% RCM gen-AI" figure [125] comes from an HFMA survey commissioned by AKASA, a vendor of gen-AI RCM tools; directional only.

## 12. Open questions

1. **Which HMIS definition applies to this engagement?** National-level RHIS/HMIS (WHO sense) vs hospital-level HIS — the local iHMS is hospital-level (LOCAL), but the national ABDM/DHIS2 context matters for interoperability design [189][206][207][208][209].
2. **FHIR adoption gap:** 70% US hospital FHIR API enablement but clear small/rural divides — what does that imply for a modernization roadmap's API layer? [195]
3. **Oracle Health trajectory:** will the OCI-native AI EHR reverse customer losses, and does its 2026 acute-care launch change the vendor calculus? [60][61][62][63][64][65]
4. **Blockchain in HIS architecture** appears in 33.7% of 2020–25 studies but is it production reality anywhere? (Not resolved by this research — likely publication-interest bias, inference only) [70].
5. **MEDITECH generative-AI roadmap** — unverified in this run (no URL — see research note T4, Blocked item 10).
6. **HIPAA Security Rule NPRM specifics** — proposal exists [34]; details not fetched (see research note T4, Blocked item 1).

## 13. Sources

Unified numbered list. **Status legend:** unmarked = URL reachable and content checked during verification (fetch_content / web_search, 2026-08-14 run); `[unreachable: …]` = URL could not be verified as reachable-with-content this run (reason given; the citation is kept because the claim is additionally documented in the research notes or confirmed via search index); `[partially verified: …]` = page reachable at research time but only quote/snippet-level content was used (direct fetch failed or full text not read), as documented in the research notes; `(PDF — full-text parsing not performed; metadata verified)` = PDF URL cited from metadata/snippets per run rules; `(LOCAL)` = workspace file, not a web URL. Research-note refs in parentheses map to evidence-table rows in `outputs/.drafts/hims-system-research-T1..T4.md`. Numbering follows first-citation order; slots 59 and 127 are intentionally unassigned.

1. WHO — Standards and tools to strengthen health services data — https://www.who.int/data/data-collection-tools/standards-and-tools-to-strengthen-health-services-data (T1-1)
2. Global Health Data Methods — Health management information systems — https://globalhealthdata.org/health-management-information-systems/ (T1-3)
3. NLM MeSH — Hospital Information Systems (D006751) — https://meshb.nlm.nih.gov/record/ui?ui=D006751 (T1-6)
4. IEEE/ETHW Milestone — Pioneering Medical Information System, 1965–1974 — https://ethw.org/Milestones:Pioneering_Medical_Information_System,_1965-1974 (T1-13)
5. The HELP hospital information system: update 1998 (Int. J. Med. Informatics) — https://www.sciencedirect.com/science/article/abs/pii/S1386505699000131 (T1-14)
6. HL7 FHIR — History — https://hl7.org/fhir/history.html (T1-25, T3-2)
7. ONC — Non-Federal Acute Care Hospital EHR Adoption, 2008–2024 (Data Brief 83) — https://healthit.gov/data/data-briefs/non-federal-acute-care-hospital-electronic-health-record-adoption-2008-2024/ (T1-27)
8. HL7 FHIR — Versions — https://hl7.org/fhir/versions.html (T3-1)
9. HL7 FHIR Management Group — 2026-01 Key Messages — https://confluence.hl7.org/spaces/FMG/pages/404100354/2026-01+Key+Messages (T3-3)
10. Plotnick 2010 — Computers, systems theory, and the making of a wired hospital (TMIS 1964–1987), JASIST — https://onlinelibrary.wiley.com/doi/10.1002/asi.21324 (T1-12) — metadata/abstract only per research note; full text not read
11. NLM Circulating Now — The PROMIS of Computer-Based Medical Records — https://circulatingnow.nlm.nih.gov/2019/06/27/the-promis-of-computer-based-medical-records/ (T1-16)
12. Wikipedia — MUMPS — https://en.wikipedia.org/wiki/MUMPS (T2-7)
13. MEDITECH — MEDITECH's 50th Anniversary — https://ehr.meditech.com/about/meditechs-50th-anniversary (T2-10) — vendor self-report
14. Healthcare IT News — Meditech founder Neil Pappalardo dies at 83 — https://www.healthcareitnews.com/news/meditech-founder-neil-pappalardo-dies-83 (T2-12)
15. DICOM — Current Edition — https://www.dicomstandard.org/current (T3-4)
16. DICOM PS3.1 (NEMA) — https://dicom.nema.org/medical/dicom/current/output/html/part01.html (T3-5)
17. ONC ISP — USCDI Applicable Vocabulary Standards — https://isp.healthit.gov/uscdi-applicable-vocabulary-standards (T3-6)
18. NLM Tutorial — Health Data Standards and Terminologies — https://www.nlm.nih.gov/oet/ed/healthdatastandards/02-400.html (T3-7)
19. IHE IT Infrastructure Technical Framework Volume 1 — https://profiles.ihe.net/ITI/TF/Volume1/ (T3-20)
20. IHE — PMIR (Patient Master Identity Registry) — https://profiles.ihe.net/ITI/PMIR/ (T3-21)
21. IHE — PIXm (Patient Identifier Cross-referencing for mobile) — https://profiles.ihe.net/ITI/PIXm/ (T3-22)
22. ONC ISP — Exchanging Patient Identification Within and Between Communities — https://isp.healthit.gov/exchanging-patient-identification-within-and-between-communities (T3-23)
23. HL7 — CDA: Clinical Document Architecture — https://hl7.org/cda/ (T3-24)
24. HL7 — CDA R2 specification (online navigation) — https://www.hl7.org/cda/stds/online-navigation/infrastructure/cda/cda.htm (T3-25)
25. HL7 FHIR — Documents — http://hl7.org/fhir/documents.html (T3-26)
26. IHE Wiki — Profiles — https://wiki.ihe.net/index.php/Profiles (T3-27)
27. IHE — MHD (Mobile access to Health Documents) — https://profiles.ihe.net/ITI/MHD/ (T3-28)
28. IHE — MHD overview — https://profiles.ihe.net/ITI/MHD/1334_overview.html (T3-29)
29. SNOMED International — Our releases — https://www.snomed.org/releases (T3-61)
30. LOINC — Downloads / Get — https://loinc.org/downloads (T3-62) — [unreachable: HTTP 403 (bot-blocked); LOINC v2.82 release confirmed via search index]
31. NLM — RxNorm — https://www.nlm.nih.gov/research/umls/rxnorm/index.html (T3-63)
32. UCUM at NLM — https://ucum.nlm.nih.gov/ (T3-64)
33. WHO — ICD-11 Implementation FAQ — https://www.who.int/standards/classifications/frequently-asked-questions/icd-11-implementation (T3-57)
34. HHS — Summary of the HIPAA Security Rule — https://www.hhs.gov/hipaa/for-professionals/security/laws-regulations/index.html (T4-1)
35. HHS — Security Rule FAQ — https://www.hhs.gov/hipaa/for-professionals/faq/security-rule/index.html (T4-2)
36. eCFR — 45 CFR §164.312 (technical safeguards) — https://federal.elaws.us/cfr/title45.part164.section164.312 (T4-27)
37. gdpr-info.eu — Art. 83 GDPR (fines) — https://gdpr-info.eu/art-83-gdpr/ (T4-15)
38. legislation.gov.uk — UK GDPR Art. 9 (special categories) — https://www.legislation.gov.uk/eur/2016/679/article/9?view=plain (T4-11)
39. gdpr-info.eu — Art. 35 GDPR (DPIA) — https://gdpr-info.eu/art-35-gdpr/ (T4-14)
40. HIPAA Journal — HHS inflation increase to HIPAA penalties (2026-01-28) — https://www.hipaajournal.com/hhs-applies-inflation-increase-penalties-for-hipaa-violations/ (T4-5)
41. Ogletree Deakins — 2025 Enforcement Trends — https://ogletree.com/insights-resources/blog-posts/2025-enforcement-trends-risk-analysis-failures-at-the-center-of-hhss-multimillion-dollar-hipaa-penalties/ (T4-6)
42. DMC Law — HIPAA Enforcement in 2025 — https://dmclawllc.com/2026/03/19/hipaa-enforcement-in-2025-a-year-defined-by-cybersecurity-failures/ (T4-7) — [unreachable: HTTP 503 (service unavailable)]
43. HHS — Breach Notification Rule — https://www.hhs.gov/hipaa/for-professionals/breach-notification/index.html (T4-4)
44. India Code — Digital Personal Data Protection Act 2023 — https://www.indiacode.nic.in/handle/123456789/22037?locale=en (T4-21)
45. Mondaq — Health Data & The DPDP Act — https://www.mondaq.com/india/healthcare/1722484/health-data-the-dpdp-actnavigating-indias-healthcare-privacy-frontier (T4-22)
46. AmLegals — Health Data and the DPDP Act: A Practical Guide — https://amlegals.com/health-data-and-the-dpdp-act-a-practical-guide/ (T4-23)
47. EY India — Decoding the DPDP Act 2023 / Rules 2025 — https://www.ey.com/en_in/insights/cybersecurity/decoding-the-digital-personal-data-protection-act-2023 (T4-24)
48. egazette.gov.in — DPDP Act 2023 official text — https://egazette.gov.in/WriteReadData/2023/248045.pdf (T4-26) — PDF — full-text parsing not performed; metadata verified
49. ICO — What are the rules on special category data? — https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/lawful-basis/special-category-data/what-are-the-rules-on-special-category-data/ (T4-13)
50. European Commission — Under what conditions can my company/organisation process sensitive data? — https://commission.europa.eu/law/law-topic/data-protection/rules-business-and-organisations/legal-grounds-processing-data/sensitive-data/under-what-conditions-can-my-companyorganisation-process-sensitive-data_en (T4-12) — [unreachable: HTTP 404 on direct fetch; page content confirmed at same URL via search index (likely bot-block/transient)]
51. ICO — The maximum amount of a fine under UK GDPR and DPA 2018 — https://ico.org.uk/about-the-ico/our-information/policies-and-procedures/data-protection-fining-guidance/statutory-background/the-maximum-amount-of-a-fine-under-uk-gdpr-and-dpa-2018/ (T4-18)
52. ICO — A guide to the data protection principles — https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/data-protection-principles/a-guide-to-the-data-protection-principles/ (T4-19)
53. ICO — Advanced Computer Software Group penalty notice (2025-03-27) — https://ico.org.uk/media2/gdlfddgc/advanced-penalty-notice-20250327.pdf (T4-20) — PDF — full-text parsing not performed; metadata verified
54. legislation.gov.uk — GDPR Chapter V (transfers) — https://www.legislation.gov.uk/eur/2016/679/chapter/V/adopted (T4-16)
55. EDPB — International data transfers — https://www.edpb.europa.eu/sme-data-protection-guide/international-data-transfers_en (T4-17)
56. HHS OCR — PIH Health settlement ($600,000) — https://www.hhs.gov/press-room/ocr-hipaa-racap-pih.html (T4-8)
57. HHS OCR — Warby Parker civil money penalty ($1.5M) — https://www.hhs.gov/press-room/penalty-against-warby-parker.html (T4-9)
58. HHS OCR — OHSU right-of-access penalty ($200,000) — https://www.hhs.gov/press-room/penalty-against-or-health-science-university.html (T4-10)
60. Healthcare Dive — Oracle Health's market share declined 'substantially' after Cerner buy (KLAS) — https://www.healthcaredive.com/news/oracle-health-lost-customers-satisfaction-stagnated-cerner-acquisition-klas-research/758427/ (T4-28)
61. hitconsultant — KLAS 2026 EHR Market Share Report summary — https://hitconsultant.net/2026/05/14/klas-2026-ehr-market-share-report/ (T4-29)
62. Becker's — What Oracle has done since acquiring Cerner: 6 developments — https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/what-oracle-has-done-since-acquiring-cerner-6-developments/ (T4-30) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
63. MedTech Dive — Oracle launches AI-backed EHR — https://www.medtechdive.com/news/oracle-new-ehr-artificial-intelligence-ai/757660/ (T4-31)
64. Becker's — Oracle Health launches new AI-powered EHR — https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/oracle-health-launches-new-ai-powered-ehr/ (T4-32) — [unreachable: HTTP 403 (bot-blocked)]
65. Fierce Healthcare — Oracle ramps up agentic AI — https://www.fiercehealthcare.com/ai-and-machine-learning/oracle-ramps-healthcare-ai-tech-unveils-new-features-patients-prior-auth (T4-33) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
66. Fierce Healthcare — Epic grows EHR footprint among small health systems (incl. Meditech share) — https://www.fiercehealthcare.com/health-tech/epic-continues-grow-ehr-market-share-it-makes-gains-small-health-systems (T4-34) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
67. KLAS blog — Uncertainty on Multiple Fronts Slowed EHR Purchases (2026 report insights) — https://engage.klasresearch.com/blog/uncertainty-on-multiple-fronts-slowed-ehr-purchases-insights-from-klass-2026-acute-care-ehr-market-share-report/10709/ (T4-35)
68. KLAS — US Acute Care EHR Market Share 2025 (report page) — https://klasresearch.com/report/us-acute-care-ehr-market-share-2025-strength-of-partnership-is-key-in-decisions/3640 (T4-41) — paywalled — figures re-reported via trade press
69. TechTarget — Health systems delay EHR purchases amid uncertainty, rising AI investment — https://www.techtarget.com/searchhealthit/news/366643293/Health-systems-delay-EHR-purchases-amid-uncertainty-rising-AI-investment (T4-42)
70. Frontiers in Digital Health (2025) — Architectural patterns for health information systems: a systematic review — https://www.frontiersin.org/journals/digital-health/articles/10.3389/fdgth.2025.1694839/full (T2-4)
71. Martin Fowler — Strangler Fig bliki — https://martinfowler.com/bliki/StranglerFigApplication.html (T4-68)
72. Martin Fowler, I. Cartwright, R. Horn, J. Lewis — Rewriting Strangler Fig (2024) — https://martinfowler.com/articles/2024-strangler-fig-rewrite.html (T4-68)
73. GAO — Electronic Health Records: VA Needs to Address Data Management Challenges (GAO-22-103718) — https://www.gao.gov/products/gao-22-103718 (T4-88)
74. The Algorithm — Strangler fig for regulated migrations — https://www.the-algo.com/insights/strangler-fig-regulated-migration (T4-69) — practitioner blog; prescriptive claims not independently corroborated
75. Dualboot Partners — Strangler Fig strategy for EHR — https://www.dualbootpartners.com/insights/strangler-fig-strategy/ (T4-70)
76. Mindbowser — EHR modernization guide — https://www.mindbowser.com/ehr-modernization-guide/ (T4-71) — consultancy claims unverified
77. Webdecodes — FHIR-first API strategy — https://webdecodes.com/fhir-first-api-strategy-building-an-app-ecosystem-around-you (T4-72)
78. Nirmitee — HL7 v2 → FHIR migration decision framework — https://nirmitee.io/blog/hl7-v2-to-fhir-migration-decision-framework-architecture-roadmap/ (T4-73)
79. Healthcare Integrations — Good FHIR integration strategy in a mixed legacy environment — https://healthcareintegrations.com/what-a-good-fhir-integration-strategy-looks-like-in-a-mixed-legacy-environment/ (T4-74)
80. Nineleaps — FHIR-first healthcare products — https://www.nineleaps.com/fhir-first-healthcare-products-engineering-for-interoperability-by-design/ (T4-75)
81. 6B — FHIR-first backend platforms — https://6b.health/insight/what-a-healthcare-software-development-company-must-consider-when-building-fhir-first-backend-platforms/ (T4-76)
82. MongoDB — Humana case study (FHIR to cloud) — https://www.mongodb.com/solutions/customer-case-studies/humana (T4-77) — vendor self-report
83. arXiv 2408.10434 — Insights on Microservice Architecture Through the Eyes of Industry Practitioners — https://arxiv.org/abs/2408.10434 (T4-78) — abstract only
84. arXiv 2411.14971 — Leveraging LLMs for Legacy Code Modernization — https://arxiv.org/abs/2411.14971 (T4-79) — abstract only
85. arXiv 2510.18509 — VAPU: System for Autonomous Legacy Code Modernization — https://arxiv.org/abs/2510.18509 (T4-80) — abstract only
86. arXiv 2408.04660 — XMainframe: A Large Language Model for Mainframe Modernization — https://arxiv.org/abs/2408.04660 (T4-81) — abstract only
87. arXiv 2409.16656 — GUIMigrator: Semantics-Preserving Transpilation — https://arxiv.org/abs/2409.16656 (T4-82) — abstract only
88. MedCity News — How Kaiser Permanente Consolidated 12 EHRs into 2 — https://medcitynews.com/2025/12/kaiser-permanente-ehr/ (T4-83) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
89. TechTarget — How Kaiser Permanente consolidated its EHR in California — https://www.techtarget.com/searchhealthit/feature/How-Kaiser-Permanente-consolidated-its-EHR-in-California (T4-84)
90. Springer — Discover Health Systems: Beyond the upgrade — HIS migration case study (2025) — https://link.springer.com/article/10.1007/s44250-025-00186-x (T4-85) — abstract-level per research note
91. OpenAlex W4407268304 — metadata record for DOI 10.1007/s44250-025-00186-x — https://openalex.org/W4407268304 (T4-86) — [unreachable: content not extractable (JS-rendered); metadata resolved via feynman paper at research time]
92. Thieme — ACI Open: Data Migration: A Thorny Issue in EHR Transitions — https://www.thieme-connect.com/products/ejournals/html/10.1055/s-0040-1710007 (T4-89)
93. OpenAlex W3029182195 — metadata record for DOI 10.1055/s-0040-1710007 — https://openalex.org/W3029182195 (T4-90) — [unreachable: content not extractable (JS-rendered); metadata resolved via feynman paper at research time]
94. PMC — Electronic Health Record Transitions—How to Make Them Work — https://pmc.ncbi.nlm.nih.gov/articles/PMC10593672/ (T4-91)
95. WHO/Europe — Strengthening health information systems — https://www.who.int/europe/teams/data-and-digital-health/strengthening-health-information-systems (T1-2)
96. Winter et al., Health Information Systems (Springer) via NCBI Bookshelf NBK602592 — Basic Concepts — https://www.ncbi.nlm.nih.gov/books/NBK602592/ (T1-5)
97. ONC/HealthIT.gov — EMR vs EHR – What is the Difference? — https://healthit.gov/blog/electronic-health-and-medical-records/emr-vs-ehr-difference/ (T1-7)
98. MEASURE Evaluation — Defining Health Information Systems — https://measureevaluation.cpc.unc.edu/his-strengthening-resource-center/his-definitions.html (T1-4) — cpc.unc.edu mirror used per research note
99. NCBI Bookshelf — Glossary (NBK602584) — https://www.ncbi.nlm.nih.gov/books/NBK602584/ (T1-8)
100. LOCAL — `docs/modernization/02-business-module-catalog.md` (iHMS/OHUM PAS NG module catalog) — file:///D:/hims_new_deloitte/docs/modernization/02-business-module-catalog.md (T2-46) — LOCAL
101. PMC — Convergent Evolution of Health Information Management and Informatics (JAMIA) — https://pmc.ncbi.nlm.nih.gov/articles/PMC4377568/ (T1-18)
102. Wikipedia — Healthcare Information and Management Systems Society — https://en.wikipedia.org/wiki/Healthcare_Information_and_Management_Systems_Society (T1-11) — Wikipedia-only, secondary
103. U.S. Office of Technology Assessment (1977) — Policy Implications of Medical Information Systems — https://www.princeton.edu/~ota/disk3/1977/7708/770805.PDF (T1-17) — PDF — full-text parsing not performed; metadata verified
104. Collen 1991 — A brief historical overview of hospital information system evolution in the US — https://pubmed.ncbi.nlm.nih.gov/1778634/ (T1-26) — [unreachable: content-extraction-failed (PubMed page); metadata verified via OpenAlex W2014640834 per research note]
105. DHIS2 — HISP History — https://dhis2.org/hisp-history/ (T1-21)
106. OpenMRS Guide — A Brief History — https://guide.openmrs.org/introduction/a-brief-history/ (T1-22)
107. WHO/HMN — Framework and Standards for Country Health Information Systems, 2nd ed. (2008) — https://www.who.int/publications/i/item/9789241595940 (T1-20)
108. govinfo — Title XXX Health Information Technology (HITECH) — https://www.govinfo.gov/content/pkg/COMPS-9279/pdf/COMPS-9279.pdf (T1-23) — PDF — full-text parsing not performed; metadata verified
109. Mandel et al. 2016 — SMART on FHIR (JAMIA) — https://pmc.ncbi.nlm.nih.gov/articles/PMC4997036/ (T1-24) — [unreachable: content not extractable (JS-rendered)]
110. ONC — FHIR Version History and Maturity fact sheet — https://healthit.gov/wp-content/uploads/2021/04/FHIR-Version-History-Fact-Sheet.pdf (T1-28) — PDF — full-text parsing not performed; metadata verified
111. ONC — 2018 HITECH Report to Congress — https://beta.healthit.gov/wp-content/uploads/2018/12/2018-HITECH-report-to-congress.pdf (T1-29) — PDF — full-text parsing not performed; metadata verified
112. WHO — Strategy for optimizing national routine health information systems (2021) — https://www.who.int/publications/i/item/9789240087163 (T1-49)
113. European Commission — European Health Data Space Regulation (EHDS) — https://health.ec.europa.eu/ehealth-digital-health-and-care/european-health-data-space-regulation-ehds_en (T3-51)
114. Firely — The State of FHIR in 2025 — https://fire.ly/blog/the-state-of-fhir-in-2025/ (T3-45)
115. AMA Journal of Ethics (2011) — Development of the Electronic Health Record — https://journalofethics.ama-assn.org/article/development-electronic-health-record/2011-03 (T1-15) — [partially verified: direct fetch 403; content read via search-result quote per research note]
116. AJMC — Ambient AI Tool Adoption in US Hospitals — https://www.ajmc.com/view/ambient-ai-tool-adoption-in-us-hospitals-and-associated-factors (T4-49) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
117. Emory Rollins School of Public Health — press release on ambient AI adoption — https://sph.emory.edu/news/new-study-finds-nearly-two-thirds-us-hospitals-using-epic-have-adopted-ambient-ai-disparities (T4-50)
118. JAMA Network Open — Use of Ambient AI Scribes to Reduce Administrative Burden and Professional Burnout — https://jamanetwork.com/journals/jamanetworkopen/fullarticle/2839542 (T4-51) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
119. NEJM Catalyst — Ambient AI Scribes: Learnings after 1 Year and over 2.5 Million Uses — https://catalyst.nejm.org/doi/full/10.1056/CAT.25.0040 (T4-52) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
120. CNBC — Epic UGM 2025: Epic touts new AI tools — https://www.cnbc.com/2025/08/20/epic-ugm-2025-epic-touts-new-ai-tools.html (T4-54)
121. Epic — AI, Genomics, and Interoperability at HIMSS 2025 — https://www.epic.com/epic/post/ai-genomics-and-interoperability-at-himss-2025/ (T4-55)
122. Epic — Art for Clinicians — https://www.epic.com/software/art/ (T4-56)
123. Healthcare IT News — Epic unveils AI agents — https://www.healthcareitnews.com/news/epic-unveils-ai-agents-showcases-new-foundational-models (T4-57) — [partially verified: direct fetch 403; snippet-level per research note]
124. KLAS — Epic Ambient AI 2025 (report page) — https://klasresearch.com/report/epic-ambient-ai-2025/3920 (T4-58) — paywalled
125. Fierce Healthcare — Adoption of AI in hospital RCM surges — https://www.fiercehealthcare.com/ai-and-machine-learning/adoption-ai-hospital-rcm-surges-even-health-systems-navigate-cost (T4-59) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index] — underlying survey by HFMA, commissioned by AKASA (vendor of gen-AI RCM tools); vendor-sponsored
126. KLAS blog — From Hype to Reality: Autonomous Coding Solutions — https://engage.klasresearch.com/blog/from-hype-to-reality-what-healthcare-leaders-should-know-about-autonomous-coding-solutions/8341/ (T4-60)
128. Healthcare Finance News — How AI transformed revenue cycle at Baylor Scott & White — https://www.healthcarefinancenews.com/news/how-ai-transformed-revenue-cycle-baylor-scott-white-health (T4-62) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
129. Halkwinds — Healthcare Cloud Infrastructure Report — https://www.halkwinds.com/research/healthcare-cloud-infrastructure-report (T4-64) — self-published research vendor
130. KPMG — Global Tech Report: Healthcare insights — https://assets.kpmg.com/content/dam/kpmgsites/xx/pdf/2025/02/global-tech-report-healthcare-insights.pdf (T4-65) — PDF — full-text parsing not performed; metadata verified
131. e-Hospital Suite (NIC, India) — Cloud HIS with 11 modules — https://informatics.nic.in/uploads/pdfs/4d85cc7c_04_11_ehospital_suite_folder_new.pdf (T2-1) — PDF — full-text parsing not performed; metadata verified
132. ABDM MVP Guidance Document v2.0 (India NHA) — https://abdm.gov.in/strapicms/uploads/Approved_MVP_Document_Version_2_0_4a85e329f9.pdf (T2-3) — PDF — full-text parsing not performed; metadata verified
133. Winter & Haux et al. via NCBI Bookshelf NBK602594 — Ch. 3: Architecture, Integration, and Standards (HL7 V2 message model) — https://www.ncbi.nlm.nih.gov/books/NBK602594/ (T2-5)
134. NIST v2plus — HL7 v2 CDM (Charge Description Master) segment — https://usnistgov.github.io/v2plusDemo/segment-definition/CDM.html (T2-34)
135. Epic — EHI Export Specification: ARPB_TRANSACTIONS — https://open.epic.com/EHITables/GetTable/ARPB_TRANSACTIONS.htm (T2-35)
136. PMC — Research data warehouse (Kaiser Permanente Southern California RDW) — https://pmc.ncbi.nlm.nih.gov/articles/PMC10284679/ (T2-42)
137. IHE Radiology Technical Framework Vol 2x (Rev 23.0) — https://www.ihe.net/uploadedFiles/Documents/Radiology/IHE_RAD_TF_Rev23-0_Vol2x_FT_2025-08-08.pdf (T3-12) — PDF — full-text parsing not performed; metadata verified
138. IHE — Scheduled Workflow.b — https://wiki.ihe.net/index.php/Scheduled_Workflow.b (T3-14)
139. Elysium Digital — MUMPS: The Arcane Database Language Behind Our EMRs — https://www.elys.com/blog/mumps-the-arcane-database-language-behind-our-electronic-medical-records (T2-8)
140. Healthcare IT Today — MUMPS and Healthcare — https://www.healthcareittoday.com/2017/05/11/mumps-and-healthcare/ (T2-9)
141. TechTarget — What is Meditech? — https://www.techtarget.com/searchhealthit/definition/Meditech-Medical-Information-Technology-Inc (T2-13) — [unreachable: fetch timeout]
142. IOS Press (Medinfo) — Migrating towards a Client Server Architecture (Asan Medical Center, 2,200 beds) — https://ebooks.iospress.nl/pdf/doi/10.3233/978-1-60750-896-0-927 (T2-14) — PDF — full-text parsing not performed; metadata verified
143. PubMed — From legacy and client/server systems to components (VAMC FileMan, Finland) — https://pubmed.ncbi.nlm.nih.gov/11604836/ (T2-15) — [unreachable: content-extraction-failed (PubMed page); abstract metadata per research note]
144. LOCAL — `docs/modernization/10-target-architecture.md` (iHMS target architecture) — file:///D:/hims_new_deloitte/docs/modernization/10-target-architecture.md (T2-47) — LOCAL
145. Mphasis — US Government Healthcare cost-saving case study (PowerBuilder) — https://www.mphasis.com/home/services/application-services/application-modernization/case-study/us-government-healthcare-saving-cost.html (T2-18) — vendor self-report
146. i-verve — Revamping a Healthcare App for a Hospital Network — https://i-verve.com/case-study/revamping-a-healthcare-application-for-a-hospital-network/ (T2-40) — vendor self-report
147. AWS Prescriptive Guidance — Modernize ASP.NET Web Forms applications on AWS — https://docs.aws.amazon.com/prescriptive-guidance/latest/patterns/modernize-asp-net-web-forms-applications-on-aws.html (T2-37) — [unreachable: content not extractable (JS-rendered)]
148. Devox Software — Intelligent Refactoring of a Legacy VB6/WinForms HealthTech System — https://devoxsoftware.com/portfolio/intelligent-refactoring-of-a-legacy-vb6-winforms-healthtech-system/ (T2-38) — vendor self-report
149. Hyland OnBase — Removal of ActiveX Client Type — https://docs.hyland.com/r/OnBase/Patient-Window/Foundation-25.1/Patient-Window/Installation/Upgrade-Considerations/Patient-Window-Upgrade-Considerations/Removal-of-ActiveX-Client-Type (T2-39)
150. PMC — Legacy systems: managing evolution through integration (Broussais University Hospital) — https://pmc.ncbi.nlm.nih.gov/articles/PMC2579070/ (T2-17)
151. Nirmitee — HL7 Integration Guide: Message Types & Implementation — https://nirmitee.io/blog/hl7-integration-how-it-works-message-types-implementation-guide/ (T2-20)
152. CaboLabs — Four healthcare integration patterns — https://cabolabs.com/insights/interoperability/integration-patterns (T2-19) — [unreachable: DNS resolution failed (EAI_AGAIN)] — practitioner blog; claim also supported by the n(n−1)/2 interface math in [151]
153. InterSystems — Ensemble Best Practices: Converting Interfaces to Ensemble — https://docs.intersystems.com/latest/csp/docbook/DocBook.UI.Page.cls?KEY=EGBP_interface_conversion (T2-22)
154. PMC (2025) — Building a healthcare data warehouse: considerations, opportunities, and challenges — https://pmc.ncbi.nlm.nih.gov/articles/PMC12748234/ (T2-41) — [unreachable: content not extractable (JS-rendered); content recovered via search includeContent per research note]
155. JSCDM — Clinical Data Warehousing: A Scoping Review — https://www.jscdm.org/article/id/320/ (T2-51) — [unreachable: fetch timeout; page confirmed via search index]
156. Darren Devitt — FHIR Architecture Decisions: facade, hybrid, FHIR-native (white paper) — https://darrendevitt.com/wp-content/uploads/2026/04/FHIR-Architecture-Decisions.pdf (T2-23) — PDF — full-text parsing not performed; metadata verified
157. OpenMRS Dev Manual — Architecture — https://devmanual.openmrs.org/technology/architecture/ (T2-26)
158. Bahmni Wiki — Bahmni Architecture — https://bahmni.atlassian.net/wiki/spaces/BAH/pages/14712833 (T2-27, T3-35)
159. Health Samurai — Building healthcare microservices: a FHIR-native approach — https://www.health-samurai.io/articles/building-healthcare-microservices-a-fhir-native-approach (T2-24)
160. MongoDB Industry Solutions — hybrid-fhir-odl reference pattern — https://github.com/mongodb-industry-solutions/hybrid-fhir-odl (T2-25)
161. Bhutan MoH — Enterprise Architecture Blueprint for Bhutan Health System — https://moh.gov.bt/wp-content/uploads/2025/01/Bhutan-MoH-HSDP-Progress-Report-Blueprint-for-HIS.pdf (T2-50) — PDF — full-text parsing not performed; metadata verified, snippet-level
162. AHIMA Journal — Building an Enterprise Master Person Index — https://journal.ahima.org/Portals/0/archives/AHIMA%20files/Building%20an%20Enterprise%20Master%20Person%20Index.pdf (T2-30) — PDF — full-text parsing not performed; metadata verified
163. NCBI Bookshelf — Managing Patient Identity Across Data Sources — https://www.ncbi.nlm.nih.gov/sites/books/NBK208618/ (T2-31, T3-19)
164. HL7 FHIR R5 — Encounter resource — https://www.hl7.org/fhir/encounter.html (T2-28)
165. PCORnet Common Data Model v7.0 — https://pcornet.org/wp-content/uploads/2025/01/PCORnet-Common-Data-Model-v70-2025_01_23.pdf (T2-32) — PDF — full-text parsing not performed; metadata verified
166. USCDI Version 7 (July 2026), ONC — https://isp.healthit.gov/sites/default/files/2026-07/USCDI-Version-7-July-2026.pdf (T2-43) — PDF — full-text parsing not performed; metadata verified
167. HL7 FHIR R5 — ChargeItem resource — https://www.hl7.org/fhir/chargeitem.html (T2-29)
168. IBM Docs — Modeling patient charges (UdmH 9.4.1) — https://www.ibm.com/docs/en/udmh/9.4.1?topic=scenarios-modeling-patient-charges (T2-33) — [unreachable: content not extractable (JS-rendered)]
169. UT System — UTSW Charge Description Master Report (2023) — https://utsystem.edu/sites/default/files/documents/ut-system-reports/2023/utsw-hospital-billing-and-professional-billing-charge-description-master-report/utsw-hospital-billing-and-professional-billing-charge-description-master-report.pdf (T2-36) — PDF — full-text parsing not performed; metadata verified
170. HL7 — V2.9 product brief — https://www.hl7.org/implement/standards/product_brief.cfm?product_id=516 (T3-65)
171. HL7 — V2.9.1 product brief — https://www.hl7.org/implement/standards/product_brief.cfm?product_id=649 (T3-66) — 2.9.1 normative status unverified
172. WHO — ICD-11 comes into effect (news, 2022-02-11) — https://www.who.int/news/item/11-02-2022-who-s-new-international-classification-of-diseases-%28icd-11%29-comes-into-effect (T3-58)
173. WHO EB144 — ICD revision background (ICD-10 in effect since 1993) — https://apps.who.int/gb/ebwha/pdf_files/EB144/B144_22-en.pdf (T3-59) — PDF — full-text parsing not performed; metadata verified
174. PubMed — Comparative analysis of ICD-11 vs ICD-10/ICD-10-CM — https://pubmed.ncbi.nlm.nih.gov/32364236/ (T3-60) — abstract-level per research note
175. NLM Tutorial — Health Data Standards: terminology detail pages — https://www.nlm.nih.gov/oet/ed/healthdatastandards/02-500.html (T3-8)
176. IHE PaLM Technical Framework Vol 2a — https://www.ihe.net/uploadedFiles/Documents/PaLM/IHE_PaLM_TF_Vol2a.pdf (T3-9) — PDF — full-text parsing not performed; metadata verified
177. HL7v2.Profiles.IHE.LTW (community tooling docs) — https://hl7v2.hexdocs.pm/HL7v2.Profiles.IHE.LTW.html (T3-10)
178. HL7 v2.8.2 Chapter 4 (hl7.eu mirror) — https://www.hl7.eu/HL7v2x/v282/std282/ch04.html (T3-11)
179. Healthcare Online Tools — HL7 v2.x in Radiology — https://healthcareonlinetools.com/en/blog/hl7-tools/hl7-in-radiology-orm-oru-worklist/ (T3-13)
180. HL7 v2.8.2 Chapter 4a (hl7.eu mirror) — https://www.hl7.eu/HL7v2x/v282/std282/ch04a.html (T3-16) — [unreachable: fetch timeout; content confirmed via search index]
181. HL7 EU refactored — RDE^O11 / RDS^O13 message definitions — https://hl7.eu/refactored/msgRDE_O11.html (T3-17)
182. TechTarget — What is an EMPI (master patient index)? — https://www.techtarget.com/searchhealthit/definition/master-patient-index-MPI (T3-18)
183. openmrs/openmrs-module-fhir2 (GitHub) — https://github.com/openmrs/openmrs-module-fhir2 (T3-30)
184. OpenMRS Wiki — FHIR: Strategy, Tools, FHIR API, and Help — https://openmrs.atlassian.net/wiki/spaces/docs/pages/26935684 (T3-31)
185. OpenMRS Talk — Future Major Versions of the FHIR2 module — https://talk.openmrs.org/t/future-major-versions-of-the-fhir2-module/47332 (T3-32) — [unreachable: content not extractable (readable mode); content confirmed via search index]
186. OpenMRS Wiki — OpenMRS Around the World — https://openmrs.atlassian.net/wiki/spaces/docs/pages/282624121/OpenMRS+Around+the+World (T3-33, T1-41, T4-43) — self-reported
187. OpenMRS — Our Impact — https://openmrs.org/our-impact/ (T3-34, T1-40) — self-reported
188. Bahmni.org — https://www.bahmni.org/ (T3-36)
189. DHIS2 — The world's largest HMIS — https://dhis2.org/health/hmis/ (T3-37, T1-35) — self-reported
190. DHIS2 — Health — https://dhis2.org/health/ (T3-38) — self-reported
191. DHIS2 docs — What is DHIS2? — https://docs.dhis2.org/en/use/what-is-dhis2.html (T3-39)
192. dhis2/dhis2-fhir-adapter (GitHub) — https://github.com/dhis2/dhis2-fhir-adapter (T3-40)
193. DHIS2 — DHIS2 and FHIR — https://dhis2.org/integration/fhir/ (T3-41)
194. dhis2/integration-examples (GitHub) — https://github.com/dhis2/integration-examples (T3-42)
195. ASTP — Patient Engagement Capabilities Among Hospitals (Data Brief 79, Aug 2025) — https://healthit.gov/wp-content/uploads/2025/08/Patient-Engagement-Capabilities-Among-Hospitals-DB79_508.pdf (T3-46) — PDF — read in full by research note T3; not re-parsed this run
196. ONC TEFCA RCE — Designated QHINs — https://rce.sequoiaproject.org/designated-qhins/ (T3-47)
197. ONC — HTI-1 Final Rule — https://healthit.gov/regulations/hti-rules/hti-1-final-rule/ (T3-68)
198. ONC — SVAP Approved Standards for 2026 — https://healthit.gov/resources/standards-version-advancement-process-svap-approved-standards-for-2026/ (T3-69)
199. ONC — Standards Bulletin 2026-2 (USCDI v7) — https://healthit.gov/standards-and-technology/onc-standards-bulletin/onc-standards-bulletin-2026-2/ (T3-70)
200. ONC TEFCA RCE — homepage — https://rce.sequoiaproject.org/ (T3-72) — figures self-reported
201. TechTarget — Where healthcare stands on interoperability in 2026 — https://www.techtarget.com/searchhealthit/feature/Where-healthcare-stands-on-interoperability-in-2026-progress-and-challenges (T3-71)
202. EUR-Lex — Regulation (EU) 2025/327 — https://eur-lex.europa.eu/eli/reg/2025/327 (T3-52)
203. NHS England — FHIR UK Core API standards — https://digital.nhs.uk/developer/api-catalogue/fhir-uk-core-standards (T3-48) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
204. NHS Standards Directory — Care Connect FHIR API standards — https://standards.nhs.uk/published-standards/care-connect-fhir-api-standards (T3-49)
205. NHS England — Booking and Referral Standard — https://digital.nhs.uk/services/booking-and-referral-standard (T3-50) — [unreachable: HTTP 403 (bot-blocked)]
206. MoHFW — Update on the implementation of ABDM (2025-02-06) — https://www.mohfw.gov.in/?q=en%2Fpressrelease-209 (T3-53) — [unreachable: content not extractable (JS-rendered); content confirmed via search index]
207. PIB — Update on the implementation of ABDM — https://www.pib.gov.in/PressReleasePage.aspx?PRID=2101737 (T3-54)
208. Business Standard — Over 790 mn ABHA accounts created, 650 mn health records linked (Lok Sabha reply) — https://www.business-standard.com/india-news/over-790-mn-abha-accounts-created-650-mn-health-records-linked-centre-125080101224_1.html (T3-55)
209. NHA ABDM — press releases (100+ crore records linked milestone) — https://abdm.gov.in/press-releases (T3-56) — [unreachable: content not extractable (JS-rendered); milestone corroborated via Indian press coverage (e.g., DD News, Indian Pharma Post) per reviewer spot-check]
210. HL7 — 2025 State of FHIR Survey Report — https://www.hl7.org/documentcenter/public/white-papers/2025%20State%20of%20FHIR%20Survey%20Report.pdf (T3-43) — PDF — full-text parsing not performed; snippet-verified
211. HL7 — 2025 State of FHIR: download / press — https://info.hl7.org/2025-state-of-fhir-download (T3-44)
212. MEDITECH — HCA Healthcare UK goes on the cloud with Expanse (2025-12-18) — https://ehr.meditech.com/news/hca-healthcare-uk-goes-on-the-cloud-with-meditech-expanse-ehr-powering-patient-centred-care (T4-36) — vendor self-report
213. MEDITECH — La Paz Regional Hospital on Expanse (2025-07-02) — https://ehr.meditech.com/news/la-paz-regional-hospital-connects-care-across-the-community-with-centralized-meditech-expanse (T4-37) — vendor self-report
214. MEDITECH — Comanche County Memorial Hospital chooses Expanse (2025-09-03) — https://ehr.meditech.com/news/comanche-county-memorial-hospital-chooses-meditech-expanse-for-a-unified-health-record (T4-38) — vendor self-report
215. MEDITECH — Silver Hill Hospital signs for Expanse (2025-05-01) — https://ehr.meditech.com/news/silver-hill-hospital-signs-for-meditech-expanse-for-integrated-and-patient-centered-care (T4-39) — vendor self-report
216. Becker's — UPMC selects MEDITECH EHR for Ireland (2025-06-05) — https://www.beckershospitalreview.com/healthcare-information-technology/ehrs/upmc-selects-meditech-ehr-for-ireland/ (T4-40) — [unreachable: HTTP 403 (bot-blocked)]
217. ScienceDirect — OpenMRS as a global good (Int. J. Med. Informatics, 2021) — https://www.sciencedirect.com/science/article/abs/pii/S1386505621000319 (T4-44, T1-42) — [unreachable: HTTP 403 (bot-blocked); abstract-level per research note]
218. OpenMRS Talk — O3 milestone: 2,000 production locations — https://talk.openmrs.org/t/huge-milestone-o3-production-sites-passes-1-000-2-000-locations/44128 (T4-45) — [unreachable: content not extractable (readable mode)]; community self-report
219. BMC Med Inform Decis Mak — Bahmni EMR adoption at community hospitals (2025) — https://link.springer.com/article/10.1186/s12911-025-03283-2 (T4-46) — [unreachable: content not extractable (readable mode)]
220. Bangladesh DGHS — Hospital Management System (OpenMRS+) — https://en.info.shr.dghs.gov.bd/hms/ (T4-47)
221. OpenMRS Talk — OpenMRS features strongly in DHIS2 EMR survey — https://talk.openmrs.org/t/openmrs-features-strongly-in-dhis2-emr-survey/49395 (T4-48) — [unreachable: content not extractable (readable mode); content confirmed via search index]; community self-report
222. Grand View Research — Electronic Health Records Market — https://www.grandviewresearch.com/industry-analysis/electronic-health-records-ehr-market (T4-93) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
223. Fortune Business Insights — Electronic Health Records Market — https://www.fortunebusinessinsights.com/electronic-health-records-ehr-market-102660 (T4-94) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]
224. Precedence Research — Electronic Health Records Market — https://www.precedenceresearch.com/electronic-health-records-market (T4-95)
225. MarketsandMarkets — Hospital Information Systems Market — https://www.marketsandmarkets.com/Market-Reports/hospital-information-system-market-144083226.html (T4-96)
226. Mordor Intelligence — Hospital Information System Market — https://www.mordorintelligence.com/industry-reports/hospital-information-system-market (T4-97)
227. Market Research Future — Hospital Information System Market — https://www.marketresearchfuture.com/reports/hospital-information-system-market-10732 (T4-98)
228. MarketsandMarkets — Healthcare IT Market — https://www.marketsandmarkets.com/Market-Reports/healthcare-it-252.html (T4-99)
229. Grand View Research — Healthcare IT Market — https://www.grandviewresearch.com/industry-analysis/healthcare-it-market (T4-100) — [unreachable: HTTP 403 (bot-blocked); content confirmed via search index]

Research-note source of record for all rows above:
- `outputs/.drafts/hims-system-research-T1.md` (definition/history/taxonomy; 52 sources)
- `outputs/.drafts/hims-system-research-T2.md` (architecture/modules; 52 sources)
- `outputs/.drafts/hims-system-research-T3.md` (standards/interoperability; 75 sources)
- `outputs/.drafts/hims-system-research-T4.md` (compliance/vendors/modernization; 100 sources)

**URL verification summary (verifier run, 2026-08-14):** ~130 URLs cited above were checked this run via `fetch_content` (readable and raw modes) and/or `web_search` (existence + content match). Representative load-bearing claims were spot-checked against fetched content: ONC Data Brief 83 (<10% 2008 → 99% 2018 → 99.4% 2024; Epic ~50% 2024; top-3 >80%) [7]; WHO RHIS=HMIS phrasing [1]; HHS Security Rule audit controls 164.312(b) + addressable + NPRM [34]; HL7 FMG "First Normative R6 Ballot completed January 2026" [9]; FHIR R5 trial-use / next release normative / R4 first normative [6][8]; Healthcare Dive (Oracle −57 customers, Epic >42% vs 23%) [60]; hitconsultant/KLAS 2026 (decisions −40%, Epic +77 hospitals/18,679 beds, Oracle −56 hospitals, MEDITECH 84% Expanse) [61][67]; Fierce (Meditech ~14.7% share) [66]; Frontiers review (89 studies; microservices 43.8%; blockchain 33.7%) [70]; GAO (allergy/medication/immunization data-quality risks) [73]; Firely (71% vs 66%, 82 experts, 52 countries) [114]; EHDS dates [113]; HIPAA penalty tiers [40]; ICO fine [53]; ABDM counts [206][207][208][209]; ASTP Data Brief 79 [195]; JAMA burnout 51.9→38.8% [118]; NEJM Catalyst 2.5M+ uses [119]; Kaiser 12→2 / ~40M records / <3h cutovers [88][89]; RCM gen-AI survey sponsorship [125]; Baylor $42M [128]; market figures for Grand View/Fortune/Precedence/MarketsandMarkets/Mordor/MRFR [222][223][224][225][226][227][228][229]. Dead/problem URLs are marked inline above; no claim was removed — every claim in the draft traces to at least one research-note row, and claims whose only URL is problem-marked remain additionally documented in the research notes' evidence tables.

