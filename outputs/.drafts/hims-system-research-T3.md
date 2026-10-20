# HMIS Standards & Interoperability — Research Findings (Brief T3)

**Researcher:** Feynman evidence-gathering subagent (T3)
**Date of research:** 2026-08-14
**Input brief:** `outputs/.plans/hims-system-research-T3.md`
**Status:** All 6 assigned questions addressed; capability gaps marked `Blocked` below.

---

## Search terms (exact query strings, chronological)

Round 1 (landscape):
1. `HL7 FHIR R5 R4 current version status 2025 2026`
2. `HL7 version 2 messaging standard current version v2.9`
3. `IHE profiles XDS PDQ PIX PIXm current status 2025`
4. `DICOM standard current edition NEMA 2025`
5. `SNOMED CT LOINC ICD-11 CPT RxNorm UCUM clinical terminology purpose`
6. `OpenMRS FHIR support architecture 2025`
7. `DHIS2 vs HMIS health information system role`
8. `TEFCA QHIN status 2025 2026 interoperability`

Round 2 (national frameworks & adoption):
9. `European Health Data Space EHDS regulation status 2025 2026 implementation`
10. `India ABDM Ayushman Bharat Digital Mission status 2025 ABHA health records`
11. `NHS England FHIR interoperability API standard 2025`
12. `HL7 2025 State of FHIR survey adoption results hospitals`
13. `Bahmni open source hospital information system architecture OpenMRS`
14. `IHE ITI profiles PIX PDQ XDS XCPD PMIR overview 2025`
15. `ONC certification program USCDI 2025 2026 health IT certification`
16. `HL7 CDA clinical document architecture current status R2`

Round 3 (terminology & order flows):
17. `SNOMED International current release SNOMED CT maintainer 2025`
18. `LOINC current version Regenstrief release 2.8`
19. `HL7 v2 ORM ORU lab order result workflow radiology modality worklist DICOM MWL`
20. `IHE MHD mobile access health documents profile FHIR R4`

Round 4 (terminology maintainers, identity):
21. `RxNorm NLM maintainer UCUM Regenstrief unified code units of measure`
22. `ICD-11 effective 1 January 2022 WHO member states implementation`
23. `HL7 v2 OML O21 lab order ORU R01 result message pharmacy RDE`
24. `EMPI enterprise master patient index definition healthcare`

Round 5 (US hospital adoption, ICD):
25. `ONC data brief hospital adoption FHIR API 2024 2025 percent hospitals`
26. `ICD-11 vs ICD-10 differences HMIS use classification morbidity mortality`

Round 6 (workflow verification):
27. `IHE Scheduled Workflow SWF profile radiology modality worklist`
28. `HL7 v2 RDE pharmacy dispense encode order message RDS`
29. `DHIS2 FHIR adapter open source integration`
30. `OpenMRS global adoption number of countries implementations`

**30 distinct web_search queries across 6 rounds** (requirement was ≥3). All results came from `web_search` (multi-provider) plus `fetch_content` of official pages.

---

## Coverage Status

| Question | Status |
|---|---|
| Q1 Messaging/interface standards (HL7 v2, FHIR, DICOM, IHE) | done |
| Q2 Clinical terminologies (SNOMED CT, LOINC, ICD-10/11, CPT, RxNorm, UCUM) | done |
| Q3 Module integration flows (orders, identity, documents) | done |
| Q4 Open-source ecosystems (OpenMRS, Bahmni, DHIS2) | done |
| Q5 National/regional frameworks (US, EU, UK, India) | done |
| Q6 FHIR adoption trends 2024–2026 | done |

**Blocked/Unverified capabilities:**
- `alpha_search` (papers, metadata/abstracts only): **Blocked** — tool is not available in this subagent's toolset. No paper metadata was fetched; paper-related claims below rest on web_search snippets of PubMed/BMJ pages only.
- `feynman_science_database_search` (pubmed/openalex): **Blocked** — tool not available in this subagent's toolset.
- Direct fetch of WHO ICD-11 fact sheet (`who.int`): fetch aborted; claims sourced from WHO FAQ/news pages instead (snippet-verified).
- Direct fetch of `digital.nhs.uk` and `loinc.org`: HTTP 403; claims sourced from search snippets of those domains.
- No academic PDFs were fetched (per brief instruction). PDF URLs are cited where a PDF is the primary source; their content was read via search snippets only, except the ASTP Data Brief No. 79 PDF which was fetched and read in full.
- HL7 v2.9.1 normative/ballot status: not confirmed from an official status page; marked `unverified` in the standards table.
- SNOMED CT release cadence day-of-month: two official pages disagree (1st vs last day of month); only "monthly" is asserted.

---

## Q1. Messaging / Interface Standards

| Standard | Scope | Maintainer | Current version / status | URL |
|---|---|---|---|---|
| HL7 Version 2.x (V2) | Pipe-delimited messaging for clinical data exchange between systems (ADT, ORM/OML orders, ORU results, RDE pharmacy, etc.); "the workhorse of electronic data exchange in the clinical domain and arguably the most widely implemented standard for healthcare in the world" (HL7's own claim) | HL7 International | v2.9 and v2.9.1 product briefs published (2.9.1 normative status unverified); earlier versions 2.1–2.8.x still in production use | https://www.hl7.org/implement/standards/product_brief.cfm?product_id=516 ; https://www.hl7.org/implement/standards/product_brief.cfm?product_id=649 |
| HL7 FHIR (Fast Healthcare Interoperability Resources) | RESTful API + resource model for exchange of health data; JSON/XML; the basis of modern patient-access APIs | HL7 International (FHIR Management Group) | R4 (v4.0.1) = first release with normative content, most widely implemented; R5 (v5.0.0) = current published version, published 26 Mar 2023, labelled Trial Use (STU); HL7 states next release will be normative; R6 first normative ballot completed Jan 2026. New releases ~every 18–24 months | https://hl7.org/fhir/versions.html ; https://hl7.org/fhir/history.html ; https://confluence.hl7.org/spaces/FMG/pages/404100354/2026-01+Key+Messages |
| DICOM (Digital Imaging and Communications in Medicine), NEMA PS3 | Imaging: transmit, store, retrieve, print, process, display of medical images; includes DICOMweb (RESTful) services | DICOM Standards Committee; published by NEMA / Medical Imaging & Technology Alliance (MITA) | Continuously updated; republished several times per year; e.g., edition 2025e (Nov 2025); "current" links always resolve to the current edition (2026c base per community status page, retrieval date Aug 2026) | https://www.dicomstandard.org/current ; https://dicom.nema.org/medical/dicom/2025e/ ; https://www.dclunie.com/dicom-status/status.html |
| IHE IT Infrastructure (ITI) profiles — XDS | Cross-Enterprise Document Sharing: register, discover, retrieve clinical documents across enterprises (Document Registry/Document Repository) | IHE International (ITI domain founded by HIMSS & RSNA, 2002) | Active profile; part of IHE ITI Technical Framework Vol 1; widely deployed (e.g., Swiss EPR, European implementations) | https://profiles.ihe.net/ITI/TF/Volume1/ ; https://wiki.ihe.net/index.php/Profiles |
| IHE — PIX (Patient Identifier Cross-referencing) | Cross-references patient identifiers across systems/facilities (EMPI-style) | IHE International | Active; ITI TF Vol 1 §2.2.5; mobile/FHIR successor: PIXm v3.1.0 (FHIR R4, Trial-Implementation) | https://profiles.ihe.net/ITI/TF/Volume1/ ; https://profiles.ihe.net/ITI/PIXm/ |
| IHE — PDQ (Patient Demographics Query) | Query patient demographics to find/verify patients across systems; mobile successor PDQm (FHIR R4) | IHE International | Active; ITI TF Vol 1 §2.2.8 | https://profiles.ihe.net/ITI/TF/Volume1/ ; https://isp.healthit.gov/exchanging-patient-identification-within-and-between-communities |
| IHE — XCPD (Cross-Community Patient Discovery) | Locate communities holding a patient's data and translate identifiers across communities | IHE International | Active; ITI TF Vol 1 ch-27 | https://profiles.ihe.net/ITI/TF/Volume1/ch-27.html |
| IHE — PMIR (Patient Master Identity Registry) | FHIR R4-based profile for managing a master identity registry (feed, query, merge, notify) | IHE International | PMIR v1.6.0, Trial-Implementation | https://profiles.ihe.net/ITI/PMIR/ |
| IHE — MHD (Mobile access to Health Documents) | FHIR R4 interface for document sharing to/from mobile/constrained devices; document-content-agnostic; complements XDS | IHE International | MHD v4.2.3 published (v4.2.5-comment in ballot, active as of 2026-06-16) | https://profiles.ihe.net/ITI/MHD/ |
| IHE — SWF/SWF.b (Scheduled Workflow) | Radiology: integrates ordering, scheduling, imaging acquisition, storage, viewing; SWF.b mandates HL7 v2.5.1 for HL7-based transactions | IHE International | Active (Radiology domain; SWF.b supplement Rev 1.7, 2019-08-09) | https://wiki.ihe.net/index.php/Scheduled_Workflow.b ; https://www.ihe.net/uploadedFiles/Documents/Radiology/IHE_RAD_Suppl_SWF.b_Rev1-7_2019-08-09.pdf |

Key facts verified directly from official pages:
- FHIR R5 is explicitly labelled "trial-use" and HL7 "plans to publish the next release as a normative specification"; R4 (4.0.1) carried the 1st normative content [1, 2].
- FHIR R6: "First Normative R6 Ballot was completed in January 2026" per the FHIR Management Group January 2026 Key Messages [3].
- DICOM PS3.1 describes the standard's structure (Parts PS3.1–PS3.20+) and that it is published as NEMA Standard PS3 [4, 5].

---

## Q2. Clinical Terminologies & Code Systems

| Terminology | Use in an HMIS context | Maintainer | Current status | URL |
|---|---|---|---|---|
| SNOMED CT | Clinical concepts — findings, diagnoses, procedures, body structures; the core clinical terminology for structured EHR content; US edition is a USCDI applicable vocabulary | SNOMED International | International Edition released monthly (cadence day-of-month differs across official pages — unverified detail) | https://www.snomed.org/releases ; https://isp.healthit.gov/uscdi-applicable-vocabulary-standards |
| LOINC | Laboratory tests and clinical observations (results); also clinical, survey, and claims-attachment concepts; USCDI applicable vocabulary | Regenstrief Institute | v2.82 released 2026-02-24; semiannual (Feb & Aug), monthly cadence from 2027; 109,325 total concepts (66,861 laboratory, 28,635 clinical, 12,668 surveys, 1,161 claims) | https://loinc.org/downloads ; https://loinc.org/ |
| ICD-10 / ICD-10-CM | Mortality/morbidity classification; ICD-10-CM is the US clinical modification used for diagnosis coding/billing; still the dominant billing classification | WHO (ICD-10); US: NCHS/CDC (ICD-10-CM) | ICD-10 in effect since 1993 (WHA43.24); ICD-10-CM in current US use; comparative analysis vs ICD-11 published | https://apps.who.int/gb/ebwha/pdf_files/EB144/B144_22-en.pdf ; https://pubmed.ncbi.nlm.nih.gov/32364236/ |
| ICD-11 | Successor classification for mortality/morbidity reporting, clinically modernized (post-coordination, digital tooling) | WHO | Adopted at WHA72 (May 2019); in effect globally 1 Jan 2022; implementation transitioning by Member State | https://www.who.int/standards/classifications/frequently-asked-questions/icd-11-implementation ; https://www.who.int/news/item/11-02-2022-who-s-new-international-classification-of-diseases-%28icd-11%29-comes-into-effect |
| CPT (Current Procedural Terminology) | Procedure codes for physician services and other healthcare (US billing/fee schedules); USCDI applicable vocabulary | American Medical Association (AMA) | Active; maintained and distributed by AMA; referenced in USCDI applicable vocabulary standards | https://isp.healthit.gov/uscdi-applicable-vocabulary-standards |
| RxNorm | Normalized names/codes (RXCUI) for clinical drugs; links drug vocabularies (First Databank, Micromedex, Multum, Gold Standard); mediates pharmacy/messaging semantics | US National Library of Medicine (NLM) | Active; source vocabulary within UMLS (SAB=RXNORM); USCDI applicable vocabulary | https://www.nlm.nih.gov/research/umls/rxnorm/index.html ; https://www.nlm.nih.gov/research/umls/rxnorm/overview.html |
| UCUM (Unified Code for Units of Measure) | Unambiguous representation of units for quantitative results (lab values, vital signs) | Developed by Regenstrief Institute (Schadow & McDonald, 1999); spec maintained at unitsofmeasure.org; NLM hosts resources | Active; code system widely bound in FHIR Quantity.unit | https://unitsofmeasure.org/ucum ; https://ucum.nlm.nih.gov/ ; https://ucum.org/history |

Context: The ONC/USCDI "Applicable Vocabulary Standards" page is the authoritative US list tying these code systems into certification requirements (SNOMED CT US Edition, LOINC, RxNorm, CPT, ICD-10-CM, UCUM) [6]. The NLM tutorial "Health Data Standards and Terminologies" describes each system's purpose in lay terms (e.g., LOINC for lab tests and clinical observations, RxNorm for medications) [7, 8].

---

## Q3. How Standards Enable Module Integration

### 3.1 Order entry → Lab
- Placer→filler lab orders use HL7 v2 `OML^O21`; acknowledgement `ORL^O22`; results flow filler→placer via `ORU^R01` [9, 10].
- IHE Pathology and Laboratory Medicine (PaLM) Technical Framework maps these to profiles: LAB-1 Placer Order Management (`OML^O21`) and LAB-3 Order Results Management (`ORU^R01`) [10].
- HL7 v2.8.2 Chapter 4 documents OML^O21 as the laboratory order message (specimen/container-centric) [11].

### 3.2 Order entry → Radiology / Imaging
- HL7 v2.x `ORM^O01`-family orders orchestrate the ordering system (EHR/CPOE) → RIS → modality → PACS → reporting chain; DICOM Modality Worklist (MWL) delivers scheduled exam context to acquisition devices; results return as `ORU^R01`; IHE Radiology TF Vol 2x includes "HL7 Order Mapping to DICOM MWL" (Appendix B) [12, 13].
- IHE Scheduled Workflow.b (SWF.b) integrates ordering, scheduling, acquisition, storage, and viewing, and mandates HL7 v2.5.1 for HL7-based transactions [14].
- DICOM handles image transport (C-STORE, DICOMweb RESTful services) alongside the HL7 workflow layer [12, 13, 15].

### 3.3 Pharmacy
- HL7 v2 pharmacy/treatment messages: `OMP` (pharmacy/treatment order), `RDE^O11` (encoded order), `RDS^O13` (dispense), `RGV` (give), `RAS` (administration); RDE is the parent order message for RDS/RGV/RAS [16, 17].

### 3.4 Patient identity (EMPI / PIX / PDQ / PMIR)
- An EMPI maintains consistent patient identity across a healthcare organization's departments and links multiple master patient indexes; patients get a single identifier across systems [18, 19].
- IHE profiles operationalize cross-enterprise identity: PIX (cross-reference identifiers), PDQ (demographics query), XCPD (cross-community discovery), and the FHIR R4-based PMIR, PIXm, PDQm (used within Master Patient Index communities per ONC ISP) [20, 21, 22, 23].

### 3.5 Document exchange (CDA / FHIR Documents / XDS / MHD)
- HL7 CDA R2 is the document markup standard for exchange of patient summaries and other clinical documents; current normative release is CDA R2.0 Online Edition 2024 (ANSI/HL7 CDAR 2.0 Online E1-2024, 2024-08-20) [24, 25].
- FHIR documents: a document instance is a Bundle of type `document` starting with a Composition, containing frozen versions of other resources; FHIR documents "may be exchanged between systems and persisted in document storage and management systems, including systems such as IHE XDS" (verified on hl7.org/fhir/documents.html) [26].
- IHE XDS (registry/repository, cross-enterprise) and its FHIR-based mobile counterpart MHD provide the document-sharing infrastructure layer [27, 28, 29].

---

## Q4. Open-Source Ecosystems

### OpenMRS
- Open-source EMR platform; FHIR support via the OpenMRS FHIR2 module, described as "a translation layer between the FHIR standard and OpenMRS' custom data model", initially using FHIR R4, built on HAPI FHIR; FHIR2 3.x is the current major line (2.8.0 was the last 2.x) [30, 31, 32].
- Official OpenMRS FHIR Implementation Guide at https://fhir.openmrs.org/ [31].
- Adoption (self-reported by OpenMRS): 80+ countries, 8,100+ healthcare sites, 22M+ patients cared for at OpenMRS sites [33, 34]. (Self-reported — treat as vendor claim.)

### Bahmni
- Open-source EMR + hospital management system for low-resource settings; combines best-of-breed open source products — OpenMRS (EMR), Odoo/OpenERP (inventory, billing, accounting), OpenELIS (lab), DCM4Chee (imaging) — into one distributable; configurable with zero programming [35, 36].
- Integration architecture: the four services integrate via ATOM (RSS-style) feeds of state changes consumed by background jobs [35].

### DHIS2 (role vs HMIS)
- DHIS2 is itself the world's largest HMIS platform: "More than 70 countries use DHIS2 as a national-scale HMIS, representing key health data about more than 40% of the world's population" (dhis2.org/health/hmis/) [37]; another DHIS2 page says Ministries of Health in 80+ countries use it for routine health data [38]. Both are self-reported by the DHIS2 project (University of Oslo/HISP). DHIS2 handles aggregate and patient-based (tracker) statistical data — it is a routine-information platform, not a clinical EMR [39].
- FHIR support: the original dhis2-fhir-adapter repo is archived/deprecated with an active fork at ITINordic/dhis2-fhir-adapter; DHIS2 publishes official FHIR integration guidance and integration examples (FHIR gateway from an Implementation Guide; DHIS2→FHIR bundle transforms) [40, 41, 42].

---

## Q5. National / Regional Interoperability Frameworks

| Framework | What it is | Current status (dates) | URL |
|---|---|---|---|
| US — ONC (ASTP) Health IT Certification Program | Certification program under the 21st Century Cures Act; HTI-1 final rule updated certification criteria (including FHIR-based APIs); SVAP lets developers advance to newer standards; USCDI is the baseline data-class standard | HTI-1 Final Rule in effect; SVAP approved-standards lists published for 2025 and 2026; USCDI v7 released 2026-07-23 (ONC Standards Bulletin 2026-2) | https://healthit.gov/regulations/hti-rules/hti-1-final-rule/ ; https://healthit.gov/resources/standards-version-advancement-process-svap-approved-standards-for-2026/ ; https://healthit.gov/standards-and-technology/onc-standards-bulletin/onc-standards-bulletin-2026-2/ |
| US — TEFCA (Trusted Exchange Framework and Common Agreement) | Nationwide interoperability framework; Recognized Coordinating Entity (RCE) = The Sequoia Project; Qualified Health Information Networks (QHINs) connect directly to each other; Participants/Subparticipants exchange under the Common Agreement | Go-live Dec 2023; Common Agreement v2.1 (Feb 2025); 11 designated QHINs (CommonWell, eClinicalWorks, eHealth Exchange, Epic Nexus, Health Gorilla, Kno2, KONZA, MedAllies, Netsmart, Oracle Health, Surescripts); RCE homepage: 21,000+ organizations live, 96,000+ connections, 1.2B+ documents shared since go-live (figures self-reported by RCE; retrieval Aug 2026) | https://rce.sequoiaproject.org/ ; https://rce.sequoiaproject.org/designated-qhins/ ; https://healthit.gov/wp-content/uploads/2025/02/Common_Agreement_2.1.pdf |
| EU — EHDS (European Health Data Space) | Regulation (EU) 2025/327: common framework for primary use (patient access/control, cross-border care) and secondary use (research) of electronic health data; harmonized EHR system requirements; MyHealth@EU/HealthData@EU infrastructure | Published in OJ 2025-03-05; in force 2025-03-26; implementing acts by Mar 2027; primary-use exchange of first priority data categories (Patient Summaries, ePrescriptions/eDispensations) from Mar 2029; second group (medical images, lab results, hospital discharge reports) from Mar 2031 | https://health.ec.europa.eu/ehealth-digital-health-and-care/european-health-data-space-regulation-ehds_en ; https://eur-lex.europa.eu/eli/reg/2025/327 |
| UK — NHS England | FHIR UK Core API standards (FHIR R4) for unified interoperability across England, Scotland, Wales, NI; Care Connect FHIR API standards (FHIR STU3); Booking and Referral Standard (BaRS); ITK3 messaging (FHIR STU3 over MESH) | Active/current (pages retrieved Aug 2026; Care Connect listed as "Active" in NHS Standards Directory) | https://digital.nhs.uk/developer/api-catalogue/fhir-uk-core-standards ; https://standards.nhs.uk/published-standards/care-connect-fhir-api-standards ; https://digital.nhs.uk/services/booking-and-referral-standard |
| India — ABDM (Ayushman Bharat Digital Mission) | National digital health mission run by the National Health Authority: ABHA health IDs, health facility registry (HFR), healthcare professional registry (HPR), ABDM-enabled software, aiming at interoperability of health data and longitudinal EHRs | MoHFW (2025-02-06): 73.98 crore ABHA created, 49.06 crore health records linked, 3.63 lakh facilities on HFR, 5.64 lakh professionals on HPR, 1.59 lakh facilities using ABDM-enabled software; Lok Sabha reply (2025-07-28): 79.71 crore ABHA, ~65.09 crore records linked; NHA milestone release: 100+ crore health records linked | https://www.mohfw.gov.in/?q=en%2Fpressrelease-209 ; https://www.pib.gov.in/PressReleasePage.aspx?PRID=2101737 ; https://www.business-standard.com/india-news/over-790-mn-abha-accounts-created-650-mn-health-records-linked-centre-125080101224_1.html ; https://abdm.gov.in/press-releases |

---

## Q6. Current FHIR Adoption Trends (2024–2026)

- **2025 State of FHIR Survey (HL7 International + Firely, third annual edition, fielded April 2025; 82 expert respondents from 52 countries):** 71% of respondents report FHIR actively used in their country for at least "a few use cases", up from 66% in 2024; diagnostic orders and reports are emerging as the leading use case (surpassing document exchange and referrals); adoption driven primarily by innovation and regulatory requirements; challenges: lack of FHIR knowledge, unclear benefits, regulatory uncertainty [43, 44, 45].
- **US hospitals (ASTP Data Brief No. 79, Aug 2025, AHA IT Supplement data):** in 2024, 81% of non-federal acute care hospitals enabled patient access via apps configured to API specifications, and 70% via apps configured to FHIR specifications (up from 57% in 2021); inpatient-setting FHIR app access grew 56%→69% (2021→2024), outpatient 49%→64%; lower-resourced hospitals (small, rural, critical access, independent) lag — hospitals on the market-leading EHR developer: 83% FHIR app access vs 56% for other developers [46].
- **Vendor/ecosystem signals:** the 11 designated TEFCA QHINs include major EHR/HIE vendors (Epic, Oracle Health, eClinicalWorks, etc.) [47]; OpenMRS adopted FHIR R4 as its integration API [30, 31]; DHIS2 publishes official FHIR integration guidance [41]; NHS England standardizes on FHIR UK Core (R4) [48].

---

## Evidence table

| # | Source | URL | Key claim | Type | Confidence |
|---|--------|-----|-----------|------|------------|
| 1 | HL7 FHIR Specification — Versions (R5) | https://hl7.org/fhir/versions.html | R5 is current published version; new versions every ~18–24 months | primary (official spec) | high |
| 2 | HL7 FHIR — History | https://hl7.org/fhir/history.html | R5 labelled trial-use; HL7 plans next release as normative; 5.0.0 dated 26 Mar 2023; R4 = 1st normative content | primary (official spec) | high |
| 3 | HL7 FHIR Management Group — 2026-01 Key Messages (Confluence) | https://confluence.hl7.org/spaces/FMG/pages/404100354/2026-01+Key+Messages | "First Normative R6 Ballot was completed in January 2026" | primary (HL7 community, official WG) | high |
| 4 | DICOM Current Edition | https://www.dicomstandard.org/current | DICOM updated/republished several times per year; managed by MITA (NEMA division) | primary (official) | high |
| 5 | DICOM PS3.1 (NEMA) | https://dicom.nema.org/medical/dicom/current/output/html/part01.html | DICOM published as NEMA Standard PS3 | primary (official) | high |
| 6 | ONC ISP — USCDI Applicable Vocabulary Standards | https://isp.healthit.gov/uscdi-applicable-vocabulary-standards | SNOMED CT US Edition, LOINC, RxNorm, CPT etc. are applicable vocabularies for USCDI | primary (government) | high |
| 7 | NLM Tutorial — Health Data Standards and Terminologies | https://www.nlm.nih.gov/oet/ed/healthdatastandards/02-400.html | Describes SNOMED CT, RxNorm, LOINC, ICD, CPT, UCUM purposes | primary (government educational) | high |
| 8 | NLM Tutorial — terminology detail pages | https://www.nlm.nih.gov/oet/ed/healthdatastandards/02-500.html | Examples: SNOMED CT for encounter diagnosis; RxNorm for medications; LOINC for lab tests | primary (government educational) | high |
| 9 | IHE PaLM TF-2a | https://www.ihe.net/uploadedFiles/Documents/PaLM/IHE_PaLM_TF_Vol2a.pdf | LAB-1 uses OML^O21; LAB-3 uses ORU^R01 | primary (official framework PDF; snippet-verified) | medium |
| 10 | HL7v2.Profiles.IHE.LTW docs | https://hl7v2.hexdocs.pm/HL7v2.Profiles.IHE.LTW.html | OML^O21 placer→filler; ORU^R01 filler→results; ORL^O22 ack | secondary (community tooling docs) | medium |
| 11 | HL7 v2.8.2 Chapter 4 (hl7.eu mirror) | https://www.hl7.eu/HL7v2x/v282/std282/ch04.html | OML^O21 is the laboratory order message | primary (spec mirror; snippet-verified) | medium |
| 12 | IHE Radiology TF Vol 2x (Rev 23.0, 2025-08-08) | https://www.ihe.net/uploadedFiles/Documents/Radiology/IHE_RAD_TF_Rev23-0_Vol2x_FT_2025-08-08.pdf | Appendix B: HL7 Order Mapping to DICOM MWL; ORU for reports | primary (official framework PDF; snippet-verified) | medium |
| 13 | Healthcare Online Tools — HL7 v2.x in Radiology | https://healthcareonlinetools.com/en/blog/hl7-tools/hl7-in-radiology-orm-oru-worklist/ | ORM orders, ORU results, worklist integration across EHR/RIS/modality/PACS | secondary (vendor blog) | medium |
| 14 | IHE Scheduled Workflow.b (Wiki + supplement) | https://wiki.ihe.net/index.php/Scheduled_Workflow.b ; https://www.ihe.net/uploadedFiles/Documents/Radiology/IHE_RAD_Suppl_SWF.b_Rev1-7_2019-08-09.pdf | SWF.b integrates ordering/scheduling/acquisition/storage/viewing; mandates HL7 v2.5.1 | primary (IHE official) | high |
| 15 | DICOM Integration with Mirth Connect | https://mirth.support/blog/dicom-integration-with-mirth-connect | HL7 layer handles ORM/ORU/ADT; DICOM handled by gateways (dcm4che, Orthanc) or DICOMweb | secondary (vendor blog) | medium |
| 16 | HL7 v2.8.2 Chapter 4a (hl7.eu mirror) | https://www.hl7.eu/HL7v2x/v282/std282/ch04a.html | RDS/RGV/RAS reference parent RDE (encoded order) message | primary (spec mirror; snippet-verified) | medium |
| 17 | HL7 EU refactored — RDE^O11 / RDS^O13 | https://hl7.eu/refactored/msgRDE_O11.html ; https://hl7.eu/refactored/msgRDS_O13.html | RDE = pharmacy/treatment encoded order (event O11); RDS = dispense (event O13) | primary (spec mirror; snippet-verified) | medium |
| 18 | TechTarget — What is an EMPI | https://www.techtarget.com/searchhealthit/definition/master-patient-index-MPI | EMPI maintains consistent patient data across departments; links MPIs | secondary (trade publication) | medium |
| 19 | NCBI — Managing Patient Identity Across Data Sources | https://www.ncbi.nlm.nih.gov/sites/books/NBK208618/ | MPI vs EMPI distinction; identity infrastructure | primary (NCBI books) | high |
| 20 | IHE ITI TF Volume 1 | https://profiles.ihe.net/ITI/TF/Volume1/ | Descriptions of PIX (§2.2.5), PDQ (§2.2.8), XDS (§2.2.10), XCPD (§2.2.27), PMIR (§2.2.49) | primary (official) | high |
| 21 | IHE PMIR profile | https://profiles.ihe.net/ITI/PMIR/ | PMIR v1.6.0 Trial-Implementation, FHIR R4 | primary (official) | high |
| 22 | IHE PIXm profile | https://profiles.ihe.net/ITI/PIXm/ | PIXm v3.1.0 Trial-Implementation, FHIR R4 | primary (official) | high |
| 23 | ONC ISP — Exchanging Patient Identification | https://isp.healthit.gov/exchanging-patient-identification-within-and-between-communities | PMIR/PDQm/PIXm used within MPI communities; PDQm FHIR-based equivalent of PDQ | primary (government) | high |
| 24 | HL7 CDA landing page + R2 online navigation | https://hl7.org/cda/ ; https://www.hl7.org/implement/standards/product_brief.cfm?product_id=496 | CDA R2.0 Online Edition 2024 current normative release (2024-08-20); used millions of times daily for patient summaries | primary (official) | high |
| 25 | HL7 CDA R2 spec | https://www.hl7.org/cda/stds/online-navigation/infrastructure/cda/cda.htm | CDA defines clinical document structure/semantics; Levels 1–3 | primary (official) | high |
| 26 | HL7 FHIR — Documents (R5) | http://hl7.org/fhir/documents.html | FHIR document = Bundle type `document` starting with Composition, frozen resource versions; may be persisted in systems such as IHE XDS | primary (official spec, read in full) | high |
| 27 | IHE Profiles wiki | https://wiki.ihe.net/index.php/Profiles | XDS shares/discovers EHR documents across enterprises; XCPD locates communities | primary (official) | high |
| 28 | IHE MHD profile | https://profiles.ihe.net/ITI/MHD/ | MHD v4.2.x, FHIR R4, for constrained/mobile devices; complements XDS | primary (official) | high |
| 29 | IHE MHD overview section | https://profiles.ihe.net/ITI/MHD/1334_overview.html | MHD enables document sharing to/from mobile devices; XDS for less constrained environments | primary (official) | high |
| 30 | OpenMRS FHIR2 module README (GitHub) | https://github.com/openmrs/openmrs-module-fhir2 | FHIR2 module = FHIR REST API for OpenMRS; replacement for FHIR module; initially FHIR R4; layered (ResourceProviders on HAPI) | primary (project repo) | high |
| 31 | OpenMRS Wiki — FHIR: Strategy, Tools, API | https://openmrs.atlassian.net/wiki/spaces/docs/pages/26935684 | FHIR module acts as translation layer; FHIR IG at fhir.openmrs.org; self-reported integration-time reduction 6–8 months → 2–3 days (ITECH quote) | primary (project docs, self-reported metric) | high (existence) / medium (metric) |
| 32 | OpenMRS Talk — FHIR2 3.0.0 / future versions | https://talk.openmrs.org/t/future-major-versions-of-the-fhir2-module/47332 | 2.8.0 last 2.x; 3.0.0+ new major line | primary (community forum) | high |
| 33 | OpenMRS Around the World (Wiki) | https://openmrs.atlassian.net/wiki/spaces/docs/pages/282624121/OpenMRS+Around+the+World | Self-reported: production use in 80+ countries, 8,100+ sites | self-reported | medium |
| 34 | OpenMRS — Our Impact | https://openmrs.org/our-impact/ | Self-reported: 22M+ patients, 8,100+ sites, 80+ countries | self-reported | medium |
| 35 | Bahmni Architecture (Wiki) | https://bahmni.atlassian.net/wiki/spaces/BAH/pages/14712833 | Bahmni = 4 integrated services; integration via ATOM feeds | primary (project docs) | high |
| 36 | Bahmni.org + OpenMRS Wiki Bahmni Distribution | https://www.bahmni.org/ ; https://openmrs.atlassian.net/wiki/spaces/docs/pages/25462383/Bahmni+Distribution | Bahmni combines OpenMRS, Odoo, OpenELIS; low-resource EMR/HMIS | primary (project docs) | high |
| 37 | DHIS2 — The world's largest HMIS | https://dhis2.org/health/hmis/ | 70+ countries use DHIS2 as national-scale HMIS; >40% of world population (self-reported) | self-reported | medium |
| 38 | DHIS2 — Health | https://dhis2.org/health/ | Ministries of Health in 80+ countries use DHIS2 for routine health data (self-reported) | self-reported | medium |
| 39 | DHIS2 — What is DHIS2 (docs) | https://docs.dhis2.org/en/use/what-is-dhis2.html | DHIS2 = tool for aggregate + patient-based statistical data; generic, metadata-driven | primary (official docs) | high |
| 40 | dhis2/dhis2-fhir-adapter (GitHub) | https://github.com/dhis2/dhis2-fhir-adapter | Original adapter archived/deprecated; active fork ITINordic/dhis2-fhir-adapter | primary (repo) | high |
| 41 | DHIS2 — DHIS2 and FHIR | https://dhis2.org/integration/fhir/ | DHIS2 supports HL7 FHIR; metadata mapped to FHIR profiles; WHO SMART guidelines alignment | primary (official docs, self-reported) | high (existence) / medium (claims) |
| 42 | DHIS2 integration examples (GitHub) | https://github.com/dhis2/integration-examples | FHIR gateway from IG; DHIS2→FHIR bundle transforms | primary (repo) | high |
| 43 | HL7 — 2025 State of FHIR Survey Report (PDF) | https://www.hl7.org/documentcenter/public/white-papers/2025%20State%20of%20FHIR%20Survey%20Report.pdf | 2025 survey: global FHIR adoption status, versions in use, drivers/challenges | primary (HL7 white paper; snippet-verified) | medium |
| 44 | HL7 — 2025 State of FHIR Download / Media | https://info.hl7.org/2025-state-of-fhir-download ; https://media.hl7.org/press/global-fhir-adoption-gains-momentum-but-gaps-persist-2025-state-of-fhir-survey/ | 82 expert respondents, 52 countries, April 2025; adoption up vs 2024 | primary (HL7) | high |
| 45 | Firely — State of FHIR 2025 blog | https://fire.ly/blog/the-state-of-fhir-in-2025/ | 71% report active FHIR use vs 66% in 2024 | primary (survey co-publisher) | high |
| 46 | ASTP Data Brief No. 79 (Aug 2025) — PDF read in full | https://healthit.gov/wp-content/uploads/2025/08/Patient-Engagement-Capabilities-Among-Hospitals-DB79_508.pdf | 2024: 81% app access, 70% FHIR app access; 2021→2024 FHIR app 57%→70% (any setting) | primary (US government data brief) | high |
| 47 | ONC TEFCA RCE — Designated QHINs (read in full) | https://rce.sequoiaproject.org/designated-qhins/ | 11 designated QHINs incl. Epic, Oracle Health, eClinicalWorks | primary (RCE) | high |
| 48 | NHS England — FHIR UK Core API standards | https://digital.nhs.uk/developer/api-catalogue/fhir-uk-core-standards | UK Core API standards use FHIR R4 across the four nations | primary (government; snippet-verified due to 403 on direct fetch) | medium |
| 49 | NHS Standards Directory — Care Connect | https://standards.nhs.uk/published-standards/care-connect-fhir-api-standards | Care Connect FHIR API standards (STU3), status Active | primary (government; snippet-verified) | medium |
| 50 | NHS England — Booking and Referral Standard | https://digital.nhs.uk/services/booking-and-referral-standard | BaRS exchanges referrals/bookings/service updates | primary (government; snippet-verified) | medium |
| 51 | EC — EHDS Regulation page (read in full) | https://health.ec.europa.eu/ehealth-digital-health-and-care/european-health-data-space-regulation-ehds_en | Reg (EU) 2025/327 published 2025-03-05, in force 2025-03-26; milestones 2027/2029/2031/2035 | primary (EU official) | high |
| 52 | EUR-Lex — Regulation (EU) 2025/327 | https://eur-lex.europa.eu/eli/reg/2025/327 | Legal text of EHDS Regulation | primary (official legal) | high |
| 53 | MoHFW press release — ABDM update (2025-02-06) | https://www.mohfw.gov.in/?q=en%2Fpressrelease-209 | 73.98 crore ABHA; 49.06 crore records linked; HFR/HPR figures | primary (government) | high |
| 54 | PIB — ABDM update | https://www.pib.gov.in/PressReleasePage.aspx?PRID=2101737 | Same figures via Press Information Bureau | primary (government) | high |
| 55 | Business Standard — Lok Sabha reply (2025-07-28) | https://www.business-standard.com/india-news/over-790-mn-abha-accounts-created-650-mn-health-records-linked-centre-125080101224_1.html | 79.71 crore ABHA, 65.09 crore records linked as of 2025-07-28 | secondary (journalism quoting government) | medium |
| 56 | NHA ABDM press releases | https://abdm.gov.in/press-releases | Milestone: 100+ crore health records linked with ABHA | primary (government) | high |
| 57 | WHO — ICD-11 Implementation FAQ | https://www.who.int/standards/classifications/frequently-asked-questions/icd-11-implementation | ICD-11 endorsed WHA72 (2019); in effect globally 1 Jan 2022 | primary (WHO) | high |
| 58 | WHO news — ICD-11 comes into effect (2022-02-11) | https://www.who.int/news/item/11-02-2022-who-s-new-international-classification-of-diseases-%28icd-11%29-comes-into-effect | ICD-11 came into effect; adopted May 2019 | primary (WHO) | high |
| 59 | WHO EB144 — ICD revision background | https://apps.who.int/gb/ebwha/pdf_files/EB144/B144_22-en.pdf | ICD-10 in effect since 1993; ~120 Member States report cause-of-death data on ICD-10 | primary (WHO; snippet-verified) | medium |
| 60 | PubMed — ICD-11 vs ICD-10/ICD-10-CM comparative analysis | https://pubmed.ncbi.nlm.nih.gov/32364236/ | Comparative analysis of ICD-11 vs ICD-10 and ICD-10-CM (metadata only — no PDF fetched) | primary (peer-reviewed abstract) | medium |
| 61 | SNOMED International — Our releases | https://www.snomed.org/releases | SNOMED CT International Edition released monthly | primary (official) | high |
| 62 | LOINC — Downloads / Get | https://loinc.org/downloads ; https://loinc.org/get | LOINC v2.82 released 2026-02-24; semiannual; monthly from 2027; 109,325 concepts | primary (official) | high |
| 63 | RxNorm (NLM) | https://www.nlm.nih.gov/research/umls/rxnorm/index.html | RxNorm = normalized drug names; links FDB, Micromedex, Multum, Gold Standard | primary (government) | high |
| 64 | UCUM at NLM / unitsofmeasure.org | https://ucum.nlm.nih.gov/ ; https://unitsofmeasure.org/ucum | UCUM developed by Regenstrief; unambiguous units code system | primary (official) | high |
| 65 | HL7 v2.9 product brief | https://www.hl7.org/implement/standards/product_brief.cfm?product_id=516 | HL7 Messaging Standard Version 2.9; V2 = "workhorse... most widely implemented standard for healthcare in the world" | primary (HL7, self-described) | high (existence) / medium (claim) |
| 66 | HL7 v2.9.1 product brief | https://www.hl7.org/implement/standards/product_brief.cfm?product_id=649 | HL7 Messaging Standard Version 2.9.1 product brief exists | primary (HL7) | high (existence); normative status unverified |
| 67 | ASTP — Hospital Use of APIs data brief | https://healthit.gov/data/data-briefs/hospital-use-of-apis-to-enable-data-sharing-between-ehrs-and-third-party-technology/ | Hospitals increasingly enable API/standards-based API access | primary (government) | high |
| 68 | ONC HTI-1 Final Rule page | https://healthit.gov/regulations/hti-rules/hti-1-final-rule/ | HTI-1 updates Certification Program (Cures Act provisions, algorithm transparency) | primary (government) | high |
| 69 | ONC SVAP approved standards 2026 | https://healthit.gov/resources/standards-version-advancement-process-svap-approved-standards-for-2026/ | SVAP lets developers advance certified products to newer adopted standards | primary (government) | high |
| 70 | ONC Standards Bulletin 2026-2 | https://healthit.gov/standards-and-technology/onc-standards-bulletin/onc-standards-bulletin-2026-2/ | USCDI v7 released 2026-07-23 | primary (government) | high |
| 71 | TechTarget — Interoperability in 2026 | https://www.techtarget.com/searchhealthit/feature/Where-healthcare-stands-on-interoperability-in-2026-progress-and-challenges | Interoperability progress narrative incl. TEFCA volume growth (journalism) | secondary | low-medium |
| 72 | RCE homepage | https://rce.sequoiaproject.org/ | 21,000+ orgs live on TEFCA; 96,000+ connections; 1.2B+ documents since Dec 2023 go-live (self-reported) | self-reported (RCE) | medium |
| 73 | HL7 CDA R2.0-sd FHIR IG | https://hl7.org/cda/stds/core/ | CDA represented as FHIR StructureDefinition (v2.0.3-sd), active 2026-06-12 | primary (official) | high |
| 74 | WHO — ICD-11 fact sheet | https://www.who.int/news-room/fact-sheets/detail/icd-11 | ICD-11 = international standard for mortality/morbidity recording, reporting, analysis | primary (WHO; fetch aborted, snippet-verified) | medium |
| 75 | NHA ABDM Newsletter (Apr–Jun 2025) | https://abdm.gov.in/strapicms/uploads/ABDM_Newsletter_April_to_June_2025_revised_color_a2b033f077.pdf | ABHA 79,06,39,251 as of 2025-06-30; HPR 6,47,913; HFR 4,09,689 | primary (government; snippet-verified) | medium |

---

## Findings (synthesis)

1. **HL7 v2 remains the backbone of clinical messaging** (orders, ADT, results, pharmacy) while **FHIR is the modern API/ecosystem layer**; both are maintained by HL7 International. FHIR R5 is the current published version but is labelled Trial Use; R4 (4.0.1) carries the normative content most implementations target, and R6 (first normative ballot completed Jan 2026) is planned as the first fully normative release [1, 2, 3, 65, 66].
2. **Imaging interoperability is dual-layer**: HL7 v2 messages orchestrate radiology workflow (ORM → RIS → modality → ORU) while DICOM (NEMA PS3, multi-edition cadence) transports images; IHE SWF.b and the IHE Radiology TF formally map the HL7↔DICOM-MWL bridge [12, 14, 4, 5].
3. **IHE provides the cross-enterprise "plumbing"**: XDS/XCA-style document sharing, PIX/PDQ/XCPD identity, and the newer FHIR R4 profiles (PMIR v1.6.0, PIXm v3.1.0, PDQm, MHD v4.2.x) show IHE converging its ITI domain onto FHIR [20, 21, 22, 23, 28].
4. **Terminology stack for an HMIS**: SNOMED CT for clinical meaning, LOINC for lab/observations, ICD-10/11 for classification and (with CPT) billing, RxNorm for medications, UCUM for units — all USCDI applicable vocabularies in the US context [6, 7, 8, 57, 62, 63, 64]. **Inference:** an interoperable HMIS should encode every coded element with at least one of these systems to be exchange-ready.
5. **Order-driven flows are standardized end-to-end**: lab (OML^O21 → ORL^O22 → ORU^R01 under IHE PaLM LAB-1/LAB-3), radiology (ORM + DICOM MWL + ORU under SWF.b), pharmacy (OMP → RDE^O11 → RDS^O13/RGV/RAS) [9, 10, 11, 12, 14, 16, 17].
6. **Document exchange has two parallel tracks**: CDA R2 (2024 online edition is current) for structured clinical documents, and FHIR documents (Bundle of type `document` starting with Composition) which FHIR explicitly notes can be persisted in IHE XDS repositories [24, 25, 26, 27].
7. **Open-source HMIS stack**: OpenMRS (FHIR R4 via FHIR2 module; 80+ countries self-reported), Bahmni (OpenMRS + Odoo + OpenELIS + DCM4Chee, ATOM-feed integration), and DHIS2 (aggregate/tracker HMIS in 70–80+ countries self-reported; FHIR adapter ecosystem exists though the original repo is archived) [30–42]. **Inference:** a plausible low-cost HMIS architecture = OpenMRS/Bahmni at facility level + DHIS2 at district/national level, bridged by FHIR or aggregate exchange.
8. **National frameworks are converging on FHIR + national identity infrastructure**: US (ONC certification/HTI-1, USCDI v7 July 2026, TEFCA live since Dec 2023 with 11 QHINs), EU (EHDS Reg 2025/327 in force 26 Mar 2025, priority data categories live 2029–2031), UK (FHIR UK Core on R4, BaRS, ITK3), India (ABDM with 79.71 crore ABHA by July 2025 and 100+ crore linked records milestone) [51–56, 68–72, 48–50].
9. **FHIR adoption is steadily rising but uneven**: 71% of surveyed countries report active FHIR use (2025, +5pp vs 2024); US hospital FHIR app access reached 70% (2024, up from 57% in 2021) with clear resource-based divides (market-leading EHR vendor 83% vs 56%) [43–46].

---

## Sources (numbered, matching evidence table)

1. HL7 FHIR — Versions — https://hl7.org/fhir/versions.html
2. HL7 FHIR — History — https://hl7.org/fhir/history.html
3. HL7 FMG — 2026-01 Key Messages — https://confluence.hl7.org/spaces/FMG/pages/404100354/2026-01+Key+Messages
4. DICOM — Current Edition — https://www.dicomstandard.org/current
5. DICOM PS3.1 — https://dicom.nema.org/medical/dicom/current/output/html/part01.html
6. ONC ISP — USCDI Applicable Vocabulary Standards — https://isp.healthit.gov/uscdi-applicable-vocabulary-standards
7. NLM Tutorial — Health Data Standards and Terminologies — https://www.nlm.nih.gov/oet/ed/healthdatastandards/02-400.html
8. NLM Tutorial — terminology pages — https://www.nlm.nih.gov/oet/ed/healthdatastandards/02-500.html
9. IHE PaLM TF-2a — https://www.ihe.net/uploadedFiles/Documents/PaLM/IHE_PaLM_TF_Vol2a.pdf
10. HL7v2.Profiles.IHE.LTW — https://hl7v2.hexdocs.pm/HL7v2.Profiles.IHE.LTW.html
11. HL7 v2.8.2 Chapter 4 (hl7.eu) — https://www.hl7.eu/HL7v2x/v282/std282/ch04.html
12. IHE Radiology TF Vol 2x Rev 23.0 — https://www.ihe.net/uploadedFiles/Documents/Radiology/IHE_RAD_TF_Rev23-0_Vol2x_FT_2025-08-08.pdf
13. Healthcare Online Tools — HL7 v2.x in Radiology — https://healthcareonlinetools.com/en/blog/hl7-tools/hl7-in-radiology-orm-oru-worklist/
14. IHE SWF.b (Wiki + supplement) — https://wiki.ihe.net/index.php/Scheduled_Workflow.b ; https://www.ihe.net/uploadedFiles/Documents/Radiology/IHE_RAD_Suppl_SWF.b_Rev1-7_2019-08-09.pdf
15. Mirth Support — DICOM Integration — https://mirth.support/blog/dicom-integration-with-mirth-connect
16. HL7 v2.8.2 Chapter 4a (hl7.eu) — https://www.hl7.eu/HL7v2x/v282/std282/ch04a.html
17. HL7 EU refactored — RDE^O11 / RDS^O13 — https://hl7.eu/refactored/msgRDE_O11.html ; https://hl7.eu/refactored/msgRDS_O13.html
18. TechTarget — EMPI definition — https://www.techtarget.com/searchhealthit/definition/master-patient-index-MPI
19. NCBI — Managing Patient Identity Across Data Sources — https://www.ncbi.nlm.nih.gov/sites/books/NBK208618/
20. IHE ITI TF Volume 1 — https://profiles.ihe.net/ITI/TF/Volume1/
21. IHE PMIR — https://profiles.ihe.net/ITI/PMIR/
22. IHE PIXm — https://profiles.ihe.net/ITI/PIXm/
23. ONC ISP — Exchanging Patient Identification — https://isp.healthit.gov/exchanging-patient-identification-within-and-between-communities
24. HL7 CDA landing page + product brief — https://hl7.org/cda/ ; https://www.hl7.org/implement/standards/product_brief.cfm?product_id=496
25. HL7 CDA R2 spec — https://www.hl7.org/cda/stds/online-navigation/infrastructure/cda/cda.htm
26. HL7 FHIR — Documents — http://hl7.org/fhir/documents.html
27. IHE Profiles wiki — https://wiki.ihe.net/index.php/Profiles
28. IHE MHD — https://profiles.ihe.net/ITI/MHD/
29. IHE MHD overview — https://profiles.ihe.net/ITI/MHD/1334_overview.html
30. openmrs/openmrs-module-fhir2 — https://github.com/openmrs/openmrs-module-fhir2
31. OpenMRS Wiki — FHIR Strategy/Tools/API — https://openmrs.atlassian.net/wiki/spaces/docs/pages/26935684
32. OpenMRS Talk — FHIR2 future versions — https://talk.openmrs.org/t/future-major-versions-of-the-fhir2-module/47332
33. OpenMRS Around the World — https://openmrs.atlassian.net/wiki/spaces/docs/pages/282624121/OpenMRS+Around+the+World
34. OpenMRS — Our Impact — https://openmrs.org/our-impact/
35. Bahmni Architecture — https://bahmni.atlassian.net/wiki/spaces/BAH/pages/14712833
36. Bahmni.org ; OpenMRS Wiki Bahmni Distribution — https://www.bahmni.org/ ; https://openmrs.atlassian.net/wiki/spaces/docs/pages/25462383/Bahmni+Distribution
37. DHIS2 — The world's largest HMIS — https://dhis2.org/health/hmis/
38. DHIS2 — Health — https://dhis2.org/health/
39. DHIS2 — What is DHIS2 — https://docs.dhis2.org/en/use/what-is-dhis2.html
40. dhis2/dhis2-fhir-adapter — https://github.com/dhis2/dhis2-fhir-adapter
41. DHIS2 — DHIS2 and FHIR — https://dhis2.org/integration/fhir/
42. DHIS2 integration-examples — https://github.com/dhis2/integration-examples
43. HL7 — 2025 State of FHIR Survey Report — https://www.hl7.org/documentcenter/public/white-papers/2025%20State%20of%20FHIR%20Survey%20Report.pdf
44. HL7 — 2025 State of FHIR Download / press — https://info.hl7.org/2025-state-of-fhir-download ; https://media.hl7.org/press/global-fhir-adoption-gains-momentum-but-gaps-persist-2025-state-of-fhir-survey/
45. Firely — The State of FHIR in 2025 — https://fire.ly/blog/the-state-of-fhir-in-2025/
46. ASTP Data Brief No. 79 — https://healthit.gov/wp-content/uploads/2025/08/Patient-Engagement-Capabilities-Among-Hospitals-DB79_508.pdf
47. ONC TEFCA RCE — Designated QHINs — https://rce.sequoiaproject.org/designated-qhins/
48. NHS England — FHIR UK Core API standards — https://digital.nhs.uk/developer/api-catalogue/fhir-uk-core-standards
49. NHS Standards Directory — Care Connect — https://standards.nhs.uk/published-standards/care-connect-fhir-api-standards
50. NHS England — Booking and Referral Standard — https://digital.nhs.uk/services/booking-and-referral-standard
51. EC — EHDS Regulation page — https://health.ec.europa.eu/ehealth-digital-health-and-care/european-health-data-space-regulation-ehds_en
52. EUR-Lex — Regulation (EU) 2025/327 — https://eur-lex.europa.eu/eli/reg/2025/327
53. MoHFW — ABDM update — https://www.mohfw.gov.in/?q=en%2Fpressrelease-209
54. PIB — ABDM update — https://www.pib.gov.in/PressReleasePage.aspx?PRID=2101737
55. Business Standard — ABHA figures (Lok Sabha) — https://www.business-standard.com/india-news/over-790-mn-abha-accounts-created-650-mn-health-records-linked-centre-125080101224_1.html
56. NHA ABDM — press releases — https://abdm.gov.in/press-releases
57. WHO — ICD-11 Implementation FAQ — https://www.who.int/standards/classifications/frequently-asked-questions/icd-11-implementation
58. WHO — ICD-11 comes into effect — https://www.who.int/news/item/11-02-2022-who-s-new-international-classification-of-diseases-%28icd-11%29-comes-into-effect
59. WHO EB144 — ICD revision — https://apps.who.int/gb/ebwha/pdf_files/EB144/B144_22-en.pdf
60. PubMed — ICD-11 vs ICD-10/ICD-10-CM — https://pubmed.ncbi.nlm.nih.gov/32364236/
61. SNOMED International — Our releases — https://www.snomed.org/releases
62. LOINC — Downloads / Get — https://loinc.org/downloads ; https://loinc.org/get
63. NLM — RxNorm — https://www.nlm.nih.gov/research/umls/rxnorm/index.html
64. UCUM at NLM ; unitsofmeasure.org — https://ucum.nlm.nih.gov/ ; https://unitsofmeasure.org/ucum
65. HL7 — V2.9 product brief — https://www.hl7.org/implement/standards/product_brief.cfm?product_id=516
66. HL7 — V2.9.1 product brief — https://www.hl7.org/implement/standards/product_brief.cfm?product_id=649
67. ASTP — Hospital Use of APIs data brief — https://healthit.gov/data/data-briefs/hospital-use-of-apis-to-enable-data-sharing-between-ehrs-and-third-party-technology/
68. ONC — HTI-1 Final Rule — https://healthit.gov/regulations/hti-rules/hti-1-final-rule/
69. ONC — SVAP approved standards 2026 — https://healthit.gov/resources/standards-version-advancement-process-svap-approved-standards-for-2026/
70. ONC Standards Bulletin 2026-2 — https://healthit.gov/standards-and-technology/onc-standards-bulletin/onc-standards-bulletin-2026-2/
71. TechTarget — Interoperability in 2026 — https://www.techtarget.com/searchhealthit/feature/Where-healthcare-stands-on-interoperability-in-2026-progress-and-challenges
72. ONC TEFCA RCE — homepage — https://rce.sequoiaproject.org/
73. HL7 CDA R2.0-sd IG — https://hl7.org/cda/stds/core/
74. WHO — ICD-11 fact sheet — https://www.who.int/news-room/fact-sheets/detail/icd-11
75. NHA — ABDM Newsletter Apr–Jun 2025 — https://abdm.gov.in/strapicms/uploads/ABDM_Newsletter_April_to_June_2025_revised_color_a2b033f077.pdf

---

## Blocked / Unverified items

- **Blocked:** `alpha_search` and `feynman_science_database_search` tools do not exist in this subagent's toolset; no academic-paper metadata search was performed via those channels. Peer-reviewed sources (PubMed ICD-11 comparison [60]) were found via `web_search` snippets only.
- **Blocked (direct fetch):** who.int ICD-11 fact sheet (fetch aborted), digital.nhs.uk (HTTP 403), loinc.org (HTTP 403) — all three were covered via search snippets of the same official domains.
- **Unverified:** HL7 v2.9.1 normative/ballot status; exact SNOMED CT monthly release day-of-month (official pages conflict); R4.0.1 exact publication date (official history table snippet is ambiguous); DICOM "2026c" base edition (from community status page, not nema.org); TEFCA volume figures (1.2B documents, 21,000+ orgs) are RCE self-reported with no independent audit; OpenMRS/DHIS2 adoption counts are self-reported.
