# The Whole HMIS System — Research Brief

**Draft (uncited):** 2026-08-14
**Topic:** Health/Health Management Information Systems (HMIS/HIMS) — definition, history, functional architecture, standards, compliance, vendor landscape, and modernization.
**Method:** 4 parallel researcher tracks (definition/history, functional architecture, standards/interoperability, compliance/market/modernization); 75+ sources; local workspace docs used only as case context (marked LOCAL). Bracket refs like [T1-27] point to numbered evidence tables in `outputs/.drafts/hims-system-research-T1..T4.md`.

---

## 1. Executive summary

- **What it is.** "HMIS" is ambiguous in the field. In WHO/global-health usage, HMIS (health *management* information system) = the routine facility-to-national aggregate reporting subsystem of a national health information system [T1-1][T1-3]. In software/industry usage, HMIS/HIMS = the hospital-level enterprise information system (registration, billing, labs, pharmacy, radiology, etc.), which academia calls a *hospital information system* (HIS) [T1-6]. This brief covers both, with emphasis on the hospital-level system and its modernization, since that matches this workspace's legacy HMIS context.
- **History.** Hospital information systems went from paper records and record-librarian profession (1928) → early experimental hospital-wide systems (Lockheed/El Camino 1965–74, HELP 1967, PROMIS, TMIS 1964–87) → integrated vendor suites (1990s–2000s, e.g., MUMPS-based Epic/Meditech) → mass EHR adoption under HITECH (US: <10% basic EHR in 2008 → 99.4% in 2024) → web/cloud/FHIR-API era (FHIR first presented 2012; R4 normative 2019; R5 2023; R6 first normative ballot Jan 2026) [T1-13][T1-14][T1-25][T1-27][T3-2][T3-3].
- **Standards.** HL7 v2 remains "the workhorse" for clinical messaging (ADT/orders/results/pharmacy); FHIR is the modern API layer; DICOM handles imaging; IHE profiles supply cross-enterprise plumbing (XDS, PIX/PDQ/XCPD, and FHIR-R4 successors PMIR/PIXm/PDQm/MHD); SNOMED CT/LOINC/ICD/RxNorm/UCUM form the terminology stack [T3-1..8][T3-20..29].
- **Compliance.** HIPAA (US), GDPR/UK GDPR (EU/UK), and India's DPDP Act 2023 dominate; health data is a special category under GDPR (fines to €20M/4% turnover), HIPAA's Security Rule requires audit controls with encryption addressable-but-expected, and enforcement has concentrated on cybersecurity failures (risk analysis #1 finding in 2025) [T4-1..27].
- **Market.** In 2024–25, Epic held >42% of US acute-care hospital share vs Oracle Health ~23% (falling); Meditech ~14.7%. Oracle's Cerner acquisition has lost customers; Oracle launched an AI-first cloud EHR (ambulatory) in Aug 2025. Purchase decisions dropped ~40% in 2025 amid policy uncertainty [T4-28..35].
- **Modernization.** Industry practice converges on strangler-fig/incremental displacement, FHIR-first or FHIR-facade API layers, event-driven microservices (43.8% of 89 surveyed HIS architecture studies), and careful data migration — with documented failure cases (VA/Oracle EHRM data-quality problems per GAO) [T4-68..92][T2-4].
- **Biggest caveats.** Analyst market-size figures disagree by >2× on the same year; OpenMRS/DHIS2 adoption counts are self-reported; several high-profile claims rest on vendor/consultancy material. Confidence per claim is labeled throughout.

## 2. Terminology & taxonomy

Definitions (primary sources):
- **WHO:** routine health information systems (RHIS) are "also termed health management information systems (HMIS)" and constitute the health-services data subsystem of national HIS [T1-1].
- **WHO/Europe:** a health information system is "a complex, multilevel system, aimed at producing health intelligence to inform decision-making, and encompassing data collection, analysis, health reporting, knowledge management and governance" [T1-2].
- **Global health literature:** HMIS "collects, stores, analyses, and evaluates health-related data from health facility to district, regional and national administrative levels… also referred to as routine health information systems" [T1-3].
- **Academic informatics (Winter et al.):** HIS is "the socio-technical subsystem of a health-related setting which comprises all data, information, and knowledge processing as well as the associated human or technical actors" [T1-5].
- **NLM MeSH (hospital level):** Hospital Information Systems are "integrated, computer-assisted systems designed to store, manipulate, and retrieve information concerned with the administrative and clinical aspects of providing medical services within the hospital" [T1-6].
- **ONC:** EMR = digital version of the paper chart, not designed to travel; EHR = broader, spans all clinicians/organizations involved in care [T1-7].

| Term | Level | What it is | Source |
|---|---|---|---|
| Health Information System (HIS) | National / health-system | Umbrella: all systems + actors producing/using health data | [T1-4][T1-5] |
| HMIS / RHIS | Facility → national | Routine aggregate reporting on service delivery, HR, supplies | [T1-1][T1-3] |
| Hospital Information System (hospital HIS) | Single hospital | Integrated administrative + clinical enterprise system | [T1-6] |
| EMR | Practice/department | Digital chart; not designed to cross organizations | [T1-7] |
| EHR | Multi-organization | Patient-lifetime record across care settings; part of HIS | [T1-5][T1-7] |
| LIS / RIS / CIS | Department | Laboratory, radiology, and clinical information subsystems | [T1-8] |

**Interpretive note (inference):** "HIMS" (health/hospital information *management* system) appears in industry and some academic usage but has no standardized WHO definition; "HMIS" is the standard WHO/global-health acronym. Which meaning applies must be fixed per context. This workspace's legacy iHMS/OHUM system is a hospital-level HIS/HMIS (LOCAL context [T2-46]).

## 3. History & evolution

Dated milestones (sources in brackets):
- **1928** — Health information *management* profession origins: Association of Record Librarians of North America (under American College of Surgeons) [T1-18].
- **1961** — HIMSS founded (as Hospital Management Systems Society) — Wikipedia-only, treat as secondary [T1-11].
- **1964–1987** — Technicon Medical Information System (TMIS), systems-theory-era hospital-wide MIS [T1-12].
- **1965–1974** — First hospital-wide computerized medical information system: Lockheed + El Camino Hospital (IEEE-recognized milestone) [T1-13].
- **1967** — HELP system operational at LDS Hospital (later full hospital IS with clinical decision support) [T1-14].
- **1970s** — PROMIS problem-oriented record (Weed, U. Vermont) [T1-16]; 1977 US OTA survey of medical information systems (TMIS, COSTAR) [T1-17].
- **1987** — MeSH descriptor "Hospital Information Systems" introduced [T1-6]; 1991 Collen historical review of US HIS evolution [T1-26].
- **1994** — HISP founded (U. Oslo + U. Western Cape) → DHIS → DHIS2 [T1-21].
- **2004** — OpenMRS born (Eldoret, Kenya; AMPATH) [T1-22].
- **2008** — WHO Health Metrics Network framework for country HIS [T1-20].
- **2009** — US HITECH Act (ARRA, P.L. 111-5) — EHR incentives/meaningful use [T1-23].
- **2010** — SMART project starts (→ SMART on FHIR) [T1-24].
- **2012–13** — FHIR first presented May 2012; DSTU1 Sep 2013 [T1-25][T1-28].
- **2015** — 96% of US non-federal acute-care hospitals using certified health IT [T1-29].
- **2016** — 21st Century Cures Act data-sharing/API mandates [T1-27].
- **2021** — WHO strategy for optimizing national RHIS [T1-49].
- **2024** — US: 99.4% hospital certified-EHR adoption; Epic ~50% of hospital market; top-3 developers >80% [T1-27].
- **2025** — EHDS Regulation (EU) 2025/327 published; FHIR 2025 State-of-FHIR survey: 71% of surveyed countries report active FHIR use [T3-51][T3-45].

Era synthesis: **paper records** (pre-1960s) → **departmental systems** (1960s–80s, early academic prototypes) → **integrated hospital IS** (1990s–2000s, vendor platforms) → **mass adoption + interoperability mandates** (2010s) → **cloud/FHIR/API + AI era** (2020s) [T1-13][T1-15][T1-26][T1-27][T4-49..67].

## 4. Functional architecture & modules

### 4.1 Core module set
A government cloud-HIS reference (India's e-Hospital) enumerates 11 modules: Patient Registration (OPD/Emergency), ADT (IPD), Billing, Laboratory (LIS), Radiology (RIS), Clinics, Store & Inventory, Pharmacy, Dietary, Laundry, OT Management [T2-1]. India's ABDM MVP spec defines required functional modules for ABDM-compliant HMIS [T2-3]. The local iHMS catalog (LOCAL) enumerates ~29 modules in the same families plus IAM, RCM/claims, reporting/search, and government programs [T2-46].

| Module | Function | Typical data flows |
|---|---|---|
| Patient registration / MPI | Identity, demographics, unique MRN, dedup | Broadcasts registration/ADT events downstream; demographics consumed by lab/rad/billing |
| ADT (admission–discharge–transfer) | Inpatient stay lifecycle | Emits HL7 ADT events (A01/A02/A03) to LIS/RIS/pharmacy/dietary/billing [T2-5] |
| OPD / appointments / queues | Booking, walk-ins, tokens | Encounter → orders → billing |
| Billing / finance / claims | Charges, invoices, payments, claims | Consumes encounter/order events; charge master (CDM) syncs codes [T2-34]; account transactions (charge/payment/adjustment) [T2-35] |
| Pharmacy | Dispensing, compound, ward lists | Consumes orders; emits dispensing → inventory + charge |
| Laboratory (LIS) | Orders, specimens, results, machine interfaces | ORM in; ORU results out [T2-5] |
| Radiology (RIS/PACS) | Imaging orders, scheduling, results | ORM + DICOM MWL; reports → EMR [T3-12][T3-14] |
| Blood bank | Donors, components, cross-match, issue | Consumes ADT + serology; issues → inventory |
| Inventory / supply chain | Item master, stock, batches, procurement | Consumes consumption events; supplies pharmacy/OT/CSSD |
| Reporting / analytics | Dashboards, exports | ETL OLTP → warehouse/OLAP (KPSC RDW: 150+ source tables → 14-table marts) [T2-42] |
| OT, nursing, diet, CSSD, linen, transport, equipment, BMW | Full-suite additions | Consume ADT/encounter events; emit charges/consumption |

### 4.2 Legacy architecture patterns (with evidence)
- **MUMPS-based systems.** MUMPS (1966, MGH) is a language + integrated hierarchical key-value DB; Epic, MEDITECH, VA VistA, and InterSystems Caché all descend from it [T2-7..10]. MEDITECH founded 1969, "generally considered the first EHR company," predating Epic/Cerner (both 1979) [T2-12][T2-13].
- **Mainframe → two-tier client/server.** Asan Medical Center (2,200 beds) migrated mainframe HIS to Unix client/server 1993–96 [T2-14]; VAMC FileMan systems migrated to two-tier/browser then multi-tier components [T2-15].
- **Monolithic shared-DB.** Legacy generations = monolith over one shared DB with stored procedures; the local iHMS is stored-procedure-centric (719 Oracle procs) with a mega-transaction coupling Order→Bill→Lab→Rad→EMR→HL7 (LOCAL [T2-47]).
- **Aging frontends.** Documented stacks: PowerBuilder client-server (US gov healthcare) [T2-18]; ASP.NET Web Forms + monolithic SQL Server [T2-40][T2-37]; VB6/WinForms desktop [T2-38]; ActiveX browser controls (removed by Hyland in Foundation EP5) [T2-39].
- **Evolution-by-integration.** Broussais University Hospital strategy: evolve legacy HMIS via distributed object-oriented integration rather than rewrite [T2-17].

### 4.3 Integration topologies
- **Point-to-point:** n systems → n(n−1)/2 interfaces (6→15, 10→45) [T2-20]; the accumulated mesh of HL7 v2 feeds, custom APIs, batch files, FHIR endpoints is fragile [T2-19].
- **Interface engine / hub-and-spoke:** routes HL7 messages between HIS, LIS, end-user apps [T2-22][T2-20].
- **HL7 v2 message/event model:** ADT (A01/A02/A03), ORM orders, ORU results, BAR patient accounting; segments MSH/EVN/PID/PV1; ACK confirmation; transport via MLLP, text files, FTP, or disk [T2-5].
- **Direct DB links / shared database:** "integrated" style vs "interfaced" message style; data/semantic/UI/process integration aspects [T2-5].
- **ETL → data warehouse:** healthcare DWs consolidate clinical+financial data; legacy systems often lack open APIs; terminology mapping (ICD/SNOMED) is a core burden [T2-41]; most public clinical-DW architectures use i2b2-like star/snowflake schemas [T2-51].
- **FHIR-era:** facade (FHIR API over legacy), hybrid (legacy stays source of truth; sync to FHIR server one system at a time), FHIR-native [T2-23].

### 4.4 Modern reference architectures
- **Systematic review (2025):** of 89 included HIS-architecture studies (2020–2025), microservices dominate (43.8%, n=39), then blockchain/distributed-ledger (33.7%, n=30), then layered (14.x%); trend toward hybrid event-driven/edge; FHIR/openEHR named as interface stabilizers [T2-4]. (Note: "blockchain" count likely reflects publication interest, not production deployment — inference.)
- **Open-source modular:** OpenMRS = Java core API + optional modules (OMODs) [T2-26]; Bahmni integrates OpenMRS + Odoo + OpenELIS via ATOM/RSS feeds consumed by background jobs [T2-27][T3-35].
- **FHIR-native microservices:** domain services over FHIR contracts [T2-24]; hybrid FHIR + operational data layer reference patterns [T2-25].
- **National blueprints:** Bhutan MoH HIS blueprint uses TOGAF-enabled OpenHIE framework [T2-50] (snippet-level).

### 4.5 Data model highlights
- **EMPI/MPI:** MPI links patients within an institution; EMPI spans institutions; in use since the 1990s [T2-30][T2-31].
- **Encounter model:** FHIR Encounter = patient-provider interaction (ambulatory/emergency/inpatient/virtual); PCORnet CDM v7 ENCOUNTER = unique PATID+ADMIT_DATE+PROVIDERID+ENC_TYPE with diagnoses/procedures in separate tables [T2-28][T2-32].
- **Orders:** USCDI v7 order classes: medication, laboratory, diagnostic imaging, clinical test, procedure, nutrition [T2-43].
- **Financial model:** charge master (HL7 v2 CDM segment) → ChargeItem capture → account transactions → claims (UB-04 revenue codes, HCPCS/CPT) — consistent across FHIR, HL7, IBM, Epic, and UTSW sources [T2-29][T2-33..36].

## 5. Standards & interoperability

### 5.1 Messaging/interface standards (current status)
- **HL7 v2.x:** "the workhorse of electronic data exchange in the clinical domain and arguably the most widely implemented standard for healthcare in the world" (HL7's own claim); v2.9/v2.9.1 product briefs published (2.9.1 normative status unverified) [T3-65][T3-66].
- **HL7 FHIR:** R4 (4.0.1) = first release with normative content, most implemented; R5 (5.0.0, 26 Mar 2023) = current published version, Trial Use; HL7 plans next release as normative; **R6 first normative ballot completed Jan 2026** [T3-1][T3-2][T3-3].
- **DICOM (NEMA PS3):** imaging storage/transport incl. DICOMweb RESTful services; republished several times per year (e.g., 2025e) [T3-4][T3-5].
- **IHE profiles:** XDS (cross-enterprise document sharing), PIX/PDQ/XCPD (identity), SWF.b (radiology workflow; mandates HL7 v2.5.1), plus FHIR R4 successors PMIR v1.6.0, PIXm v3.1.0, PDQm, MHD v4.2.x — IHE is converging its ITI domain onto FHIR [T3-20..23][T3-28][T3-14].

### 5.2 Clinical terminologies
SNOMED CT (clinical concepts; monthly international releases), LOINC (lab/observations; v2.82, Feb 2026; 109,325 concepts), ICD-10/ICD-10-CM (dominant billing classification; ICD-11 in effect globally since 1 Jan 2022), CPT (US procedure billing), RxNorm (drugs, RXCUI), UCUM (units) — all are ONC "applicable vocabulary standards" in the US [T3-6][T3-57..64]. **Inference:** an exchange-ready HMIS encodes coded elements with at least one of these systems.

### 5.3 How standards enable module integration
- **Lab:** OML^O21 (order) → ORL^O22 (ack) → ORU^R01 (results); IHE PaLM LAB-1/LAB-3 map these [T3-9][T3-10][T3-11].
- **Radiology:** ORM orders + DICOM Modality Worklist → modality → PACS; ORU results; IHE SWF.b + Radiology TF Appendix B formalize the HL7↔DICOM-MWL bridge [T3-12][T3-13][T3-14].
- **Pharmacy:** OMP (order) → RDE^O11 (encoded order) → RDS^O13 (dispense) / RGV (give) / RAS (administration) [T3-16][T3-17].
- **Identity:** EMPI + IHE PIX/PDQ/XCPD and FHIR-based PMIR/PIXm/PDQm [T3-18..23].
- **Documents:** CDA R2 (2024 online edition current) and FHIR documents (Bundle type `document` starting with Composition; can persist in IHE XDS) [T3-24..29].

### 5.4 Open-source ecosystems
- **OpenMRS:** FHIR R4 support via FHIR2 module (translation layer over custom data model, HAPI-based); self-reported 80+ countries, 8,100+ sites, 22M+ patients (self-reported — treat as vendor claim) [T3-30..34].
- **Bahmni:** OpenMRS + Odoo + OpenELIS + DCM4Chee, integrated via ATOM feeds; for low-resource settings [T3-35][T3-36].
- **DHIS2:** itself the world's largest HMIS by self-report (70+ countries national-scale, >40% of world population); aggregate + tracker (patient-based) statistical data — a routine-information platform, not a clinical EMR; original dhis2-fhir-adapter archived with an active fork; official FHIR guidance exists [T3-37..42].
- **Inference:** a common LMIC architecture = OpenMRS/Bahmni at facility + DHIS2 at district/national level.

### 5.5 National/regional frameworks (dated)
- **US:** ONC (ASTP) certification under HTI-1; USCDI v7 released 2026-07-23; TEFCA live since Dec 2023, 11 designated QHINs (Epic, Oracle Health, eClinicalWorks, etc.), Common Agreement v2.1 (Feb 2025); RCE self-reports 21,000+ orgs, 96,000+ connections, 1.2B+ documents [T3-46][T3-47][T3-68..72].
- **EU:** EHDS Regulation (EU) 2025/327 published 5 Mar 2025, in force 26 Mar 2025; implementing acts by Mar 2027; first-priority primary-use exchange (patient summaries, ePrescriptions) from Mar 2029, second group (images, labs, discharge reports) from Mar 2031 [T3-51][T3-52].
- **UK:** FHIR UK Core (R4) API standards; Care Connect (STU3, active); Booking and Referral Standard (BaRS); ITK3 messaging over MESH [T3-48..50].
- **India:** ABDM — 73.98 crore ABHA created (Feb 2025) → 79.71 crore by Jul 2025; 49→65 crore records linked; 100+ crore records linked milestone [T3-53..56].

### 5.6 FHIR adoption trends
- **2025 State of FHIR survey (82 experts, 52 countries):** 71% report active FHIR use in their country, up from 66% in 2024; diagnostic orders/reports now the leading use case; top challenge = lack of FHIR knowledge [T3-43..45].
- **US hospitals (ASTP Data Brief 79, Aug 2025):** 2024: 81% enabled patient access via API-configured apps, 70% via FHIR-configured apps (57% in 2021); small/rural/critical-access/independent hospitals lag; market-leading EHR vendor: 83% FHIR app access vs 56% others [T3-46].

## 6. Security & compliance

### 6.1 HIPAA (US)
- **Privacy Rule (45 CFR 160/164 subpart E):** protects all PHI; patient rights (access, amendment); minimum necessary; covered entities + business associates [T4-1].
- **Security Rule (164 subpart C):** administrative/physical/technical safeguards for ePHI — risk analysis required, unique user ID required, **audit controls required (164.312(b))**; RBAC and encryption are addressable (not per se mandated), but OCR's enforcement record treats them as expected [T4-1][T4-2][T4-27].
- **Breach Notification Rule (164.400–414):** notify individuals, HHS, and media for large breaches of unsecured PHI [T4-4].
- **Penalties (2025 inflation-adjusted):** four HITECH culpability tiers from $145–$2,190,294 per violation (with OCR enforcement discretion halving/capping three tiers); annual caps $36,505.50–$2,190,294 [T4-5]. Enforcement context: 10 resolution agreements Jan–May 2025; inadequate risk analysis the top finding; ransomware the most common trigger [T4-6][T4-7]. Example settlements: PIH Health $600k (phishing), Warby Parker $1.5M CMP, OHSU $200k (right-of-access) [T4-8][T4-9][T4-10].
- **Current item:** HHS has published a proposed rule (NPRM) to modify the HIPAA Security Rule for stronger cybersecurity (existence confirmed on HHS page; NPRM text not fetched) [T4-1].

### 6.2 GDPR / UK GDPR / DPA 2018
- Health data = special category (Art. 9): processing prohibited unless an Art. 9(2) condition applies [T4-11][T4-12][T4-13].
- DPIA required for large-scale special-category processing (Art. 35(3)(b)) [T4-14].
- Fines: up to **€20M or 4% worldwide turnover** (higher tier, incl. Art. 9); €10M/2% lower tier; Art. 25/32 measures are a fine-calibration factor [T4-15].
- UK: same structure; cap **£17.5M or 4%**; dated example: ICO fined Advanced Computer Software **£3,076,320** (2025-03-27) for a health-data ransomware breach [T4-18][T4-19][T4-20].
- Transfers: Chapter V conditions (adequacy/SCCs) — no explicit EU residency mandate, but transfer rules plus DPIA obligations operate as de-facto residency constraints for EU health data (inference) [T4-16][T4-17].

### 6.3 India DPDP Act 2023
- First Indian data-protection statute (Act 22 of 2023, enacted 2023-08-11); **no separate health-data category** — health data is ordinary personal data under a consent-based framework [T4-21][T4-22].
- DPDP Rules 2025 notified (consent, child data, significant data fiduciaries, breach notification) [T4-24].
- Penalties up to **₹250 crore** for serious contraventions [T4-23]; §16 empowers government to restrict cross-border transfers to notified territories [T4-26].

### 6.4 Cross-cutting for HMIS (inference)
RBAC + audit logging + encryption + data residency are the common technical controls across regimes; HIPAA makes audit controls required, GDPR makes their absence a fine aggravator, and residency constraints differ (none in HIPAA; transfer-based in GDPR; future restriction power in DPDP §16).

## 7. Vendor & technology landscape (2025–2026)

- **Epic:** >42% of US acute-care hospitals in 2024 vs Oracle 23%; in 2025 Epic was the only vendor chosen by large health systems (>10 hospitals), adding 77 hospitals/18,679 beds; ~200 AI features in development (Aug 2025) [T4-28][T4-29][T4-54].
- **Oracle Health (ex-Cerner):** >$28B acquisition closed 2022; lost 57 unique acute-care customers in three years (12 of them >1,000-bed systems, incl. Intermountain, UPMC, Henry Ford, Adventist, ChristianaCare); share fell ~25% (2021) → 22.9% (2024); third consecutive year of net losses in 2025 (−56 hospitals); ~1/3 of sampled customers say Oracle is not in their long-term plans; launched an **AI-first, cloud-native EHR on OCI for US ambulatory providers Aug 2025** (acute care planned 2026) [T4-28][T4-29][T4-30][T4-31][T4-32][T4-33].
- **MEDITECH:** ~14.7% acute-care share, third-largest base; Expanse cloud EHR + "MEDITECH as a Service"; 84% of legacy customers chose Expanse migration in 2025 (retention record); 2025 go-lives incl. HCA Healthcare UK (11 hospitals, Dec 2025), UPMC Ireland [T4-29][T4-34..40].
- **Market-wide:** acute-care EHR purchase decisions fell ~40% in 2025 vs 2024 (~50% vs 2023) on policy uncertainty and capital redirected to AI; only five vendors had validated net-new decisions in 2025 (Altera, Epic, MEDITECH, Oracle Health, TruBridge) [T4-29][T4-41][T4-42][T4-35].
- **Open-source in enterprise:** OpenMRS/Bahmni dominate LMIC public-health deployments but are not significant in US acute care or Western enterprise EHR (inference) [T4-43..48].

## 8. Cloud & AI trends (dated examples)

- **Ambient AI scribes:** 62% of US hospitals on Epic adopted ambient AI documentation (study of 2,784 Epic hospitals); JAMA Network Open QI study: burnout 51.9% → 38.8% after 30 days (263 clinicians, 6 systems); NEJM Catalyst: 2.5M+ uses in a year at one system [T4-49][T4-50][T4-51][T4-52].
- **Revenue-cycle AI:** 80% of health systems exploring/piloting/implementing gen AI for RCM in 2025; KLAS first autonomous-coding report (Aug 2025); Baylor Scott & White expects $42M annualized net revenue improvement from AI physician review [T4-59][T4-60][T4-62].
- **Vendor AI:** Epic ~200 AI features (Art for Clinicians ambient note drafting, Emmie chatbot, RCM agent; agentic AI announced HIMSS25); Oracle Clinical AI Agent + AI-first EHR [T4-54..58][T4-28][T4-31].
- **Cloud:** 71% of surveyed hospitals/health systems run ≥1 clinical workload in the cloud (self-published research vendor; methodology not independently verified) [T4-64]; KPMG: ~70% of healthcare tech leaders report AI/automation technologies (PDF snippet) [T4-65].

## 9. Modernization approaches for legacy HMIS

Patterns and evidence:
- **Strangler fig:** incrementally route traffic from legacy monolith to new services until legacy host is retired (Fowler 2004; 2024 rewrite with Cartwright/Horn/Lewis "patterns of legacy displacement") [T4-68]. Health-system adaptations: BAA/HIPAA coverage for new services from day one; audit-trail continuity; avoid big-bang cutover [T4-69]; wrap legacy EHR with FHIR API and modernize module-by-module [T4-70][T4-71].
- **FHIR-first / API-first:** FHIR as internal contract, not afterthought; CMS mandates (Patient Access API, prior authorization) as modernization drivers [T4-72..76]; FHIR to the cloud without rip-and-replace (Humana/MongoDB case study — vendor self-report) [T4-77].
- **Microservices decomposition:** legacy modernization is a top motivation for microservices adoption, but rushed modernization is risky (arXiv industry survey) [T4-78].
- **LLM-assisted modernization:** LLMs for MUMPS/mainframe-class code migration; autonomous modernization frameworks (VAPU); COBOL migration (XMainframe) — arXiv abstracts only [T4-79][T4-80][T4-81].
- **Frontend modernization:** semantics-preserving legacy UI migration (GUIMigrator-style) [T4-82].

Documented cases:
- **Success:** Kaiser Permanente consolidated 12 California EHR instances → 2, ~40M records, each cutover <3 hours without data loss [T4-83][T4-84]; peer-reviewed HIS-migration framework + real non-incremental migration case study (Discover Health Systems, 2025) [T4-85][T4-86].
- **Failure/risk:** GAO found VA/Oracle EHRM transferred-data quality problems (allergy, medication, immunization errors posing patient-safety risk) [T4-88]; literature review: most large data-migration projects run over budget or miss deadlines; mapping/validation core failure points [T4-89][T4-90]; vendor-change complexity from lab/radiology connections [T4-91].
- **Unverified vendor claims:** "60–80% of healthcare IT budgets go to legacy maintenance; big-bang EHR replacements fail 70%+ of the time" — consultancy marketing, not corroborated [T4-71].

## 10. Market & scale (low confidence — analyst estimates conflict)

| Market | Size (year) | Forecast | Sources |
|---|---|---|---|
| Global EHR | $28.86B (2025) | $46.4B by 2035 (4.86% CAGR) | [T4-95] |
| Global EHR | $32.44B (2025) | $52.6B by 2034 (5.61%) | [T4-94] |
| Global EHR | $35.89B (2025) | $53.11B by 2033 (5.10%) | [T4-93] |
| Hospital Information Systems | $61.46B (2025) | $89.75B by 2031 (6.52%) | [T4-97] |
| Hospital Information Systems | $63.81B (2024) | $116.76B by 2030 (10.6%) | [T4-96] |
| Hospital Information Systems | $175.53B (2025) | $533.71B by 2035 | [T4-98] |
| Healthcare IT | $496.09B (2025) | $1,143.67B by 2031 (15.0%) | [T4-99] |
| Healthcare IT | $866.48B (2025) | $2,864.39B by 2033 (16.2%) | [T4-100] |

Defensible adoption anchor (primary source): US non-federal acute-care hospitals <10% basic EHR (2008) → 99% certified EHR (2018) → 99.4% (2024) [T1-27]. **Caveat:** analyst houses differ by >2× on the same base year; treat as order-of-magnitude only.

## 11. Disagreements & caveats

1. **Terminology ambiguity** — "HMIS/HIMS" means different things in WHO/global-health vs industry usage; no single standardized definition [T1-1..6].
2. **Market-size conflicts** — EHR 2025 estimates range $28.9–35.9B; HIS 2024/25 $61–176B depending on house [T4-93..98].
3. **FHIR versioning** — R5 is current but Trial Use; R4 holds the normative content most targets; R6 normative ballot completed Jan 2026 [T3-1..3].
4. **HIPAA penalty numbers** — 2025 adjusted figures differ by source because OCR enforcement discretion halves/caps three tiers [T4-5].
5. **Self-reported adoption counts** — OpenMRS (80+ countries, 22M+ patients) and DHIS2 (70–80+ countries) counts are project self-reports, not independently verified [T3-33][T3-34][T3-37][T3-38].
6. **Consultancy/analyst claims** — Mindbowser "60–80% budget / 70%+ failure" figures unverified [T4-71]; Halkwinds 71% cloud figure self-published [T4-64]; KLAS report pages paywalled, figures via trade press [T4-28][T4-29].
7. **HIMSS founding year (1961)** — Wikipedia-only [T1-11]; MEDITECH generative-AI features — no announcement found (absence unverified) [T4-10-blocked].

## 12. Open questions

1. **Which HMIS definition applies to this engagement?** National-level RHIS/HMIS (WHO sense) vs hospital-level HIS — the local iHMS is hospital-level (LOCAL), but the national ABDM/DHIS2 context matters for interoperability design [T3-53..56][T3-37].
2. **FHIR adoption gap:** 70% US hospital FHIR API enablement but clear small/rural divides — what does that imply for a modernization roadmap's API layer? [T3-46]
3. **Oracle Health trajectory:** will the OCI-native AI EHR reverse customer losses, and does its 2026 acute-care launch change the vendor calculus? [T4-28..33]
4. **Blockchain in HIS architecture** appears in 33.7% of 2020–25 studies but is it production reality anywhere? (Not resolved by this research — likely publication-interest bias, inference only) [T2-4].
5. **MEDITECH generative-AI roadmap** — unverified in this run [T4-10-blocked].
6. **HIPAA Security Rule NPRM specifics** — proposal exists; details not fetched [T4-1-blocked].

## 13. Sources

Full numbered evidence tables with URLs live in the four research notes:
- `outputs/.drafts/hims-system-research-T1.md` (definition/history/taxonomy; 52 sources)
- `outputs/.drafts/hims-system-research-T2.md` (architecture/modules; 52 sources)
- `outputs/.drafts/hims-system-research-T3.md` (standards/interoperability; 75 sources)
- `outputs/.drafts/hims-system-research-T4.md` (compliance/vendors/modernization; 100 sources)

Key primary sources cited above (abbreviated; full lists in the notes):
- WHO — RHIS/HMIS terminology: https://www.who.int/data/data-collection-tools/standards-and-tools-to-strengthen-health-services-data
- WHO/Europe HIS definition: https://www.who.int/europe/teams/data-and-digital-health/strengthening-health-information-systems
- MeSH Hospital Information Systems: https://meshb.nlm.nih.gov/record/ui?ui=D006751
- ONC EHR adoption 2008–2024 (Data Brief 83): https://healthit.gov/data/data-briefs/non-federal-acute-care-hospital-electronic-health-record-adoption-2008-2024/
- HL7 FHIR versions/history: https://hl7.org/fhir/versions.html ; https://hl7.org/fhir/history.html
- HL7 FHIR MG 2026-01 Key Messages (R6 ballot): https://confluence.hl7.org/spaces/FMG/pages/404100354/2026-01+Key+Messages
- ASTP Data Brief 79 (FHIR API adoption): https://healthit.gov/wp-content/uploads/2025/08/Patient-Engagement-Capabilities-Among-Hospitals-DB79_508.pdf
- HHS HIPAA Security Rule: https://www.hhs.gov/hipaa/for-professionals/security/laws-regulations/index.html
- GDPR Art. 83 (fines): https://gdpr-info.eu/art-83-gdpr/
- EHDS Regulation: https://health.ec.europa.eu/ehealth-digital-health-and-care/european-health-data-space-regulation-ehds_en ; https://eur-lex.europa.eu/eli/reg/2025/327
- Healthcare Dive — Oracle/KLAS: https://www.healthcaredive.com/news/oracle-health-lost-customers-satisfaction-stagnated-cerner-acquisition-klas-research/758427/
- hitconsultant — KLAS 2026: https://hitconsultant.net/2026/05/14/klas-2026-ehr-market-share-report/
- Martin Fowler — Strangler Fig: https://martinfowler.com/bliki/StranglerFigApplication.html ; https://martinfowler.com/articles/2024-strangler-fig-rewrite.html
- GAO — VA EHR data quality: https://www.gao.gov/products/gao-22-103718
- Frontiers Digit Health 2025 systematic review: https://www.frontiersin.org/journals/digital-health/articles/10.3389/fdgth.2025.1694839/full
- LOCAL case context: `docs/modernization/02-business-module-catalog.md`, `docs/modernization/10-target-architecture.md`
