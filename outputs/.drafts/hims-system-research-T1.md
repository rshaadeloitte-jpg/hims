# Research Note T1 — HMIS: Definition, Scope, History, Taxonomy

**Status:** complete | **Date:** 2026-08-14 | **Researcher:** evidence-gathering subagent
**Questions tracked:** Q1 definitions `done` · Q2 taxonomy `done` · Q3 history `done` · Q4 global context `done` · Q5 market/scale `done` (figures low-confidence, see caveats)

---

## Search terms (exact query strings)

### web_search (queries array, 20 distinct queries)
1. `HMIS definition Health Management Information System WHO`
2. `Hospital Information System definition HIMSS history`
3. `difference between HIS EHR EMR LIS RIS hospital information systems taxonomy`
4. `DHIS2 OpenMRS health information system low income countries implementation`
5. `hospital information system history milestones 1960s Lockheed Technicon`
6. `healthcare information technology market size 2025 hospital information systems`
7. `WHO Health Information Systems 2024 guidance digital health strategy`
8. `EMR EHR difference definition ONC HITECH act`
9. `MEASURE Evaluation health management information system definition routine health information`
10. `HL7 FHIR history SMART on FHIR interoperability standards timeline`
11. `national health information system vs hospital information system difference`
12. `WHO 2008 Health Metrics Network framework country health information systems definition`
13. `DHIS2 scale countries users 2024 health information system`
14. `OpenMRS number countries implementations scale`
15. `hospital EHR adoption rate HITECH meaningful use percentage 2017 96 percent`
16. `cloud-based hospital information system market trend 2025 API`
17. `DHIS2 history 2006 University of Oslo HISP origin District Health Information Software`
18. `OpenMRS 2004 origin Partners in Health Regenstrief Institute AMPATH Kenya electronic medical record history`
19. `PROMIS Weed HELP LDS Hospital 1970s problem oriented medical record hospital information system history`
20. `WHO support tool strengthen health information systems second edition 2021 definition health information system`

### feynman alpha search (arXiv/alphaXiv, metadata/abstracts only — no PDFs fetched)
21. `alpha search` (semantic) — `health management information system definition`
22. `alpha search` (semantic) — `hospital information system history evolution`
23. `alpha search` (keyword) — `hospital information system`
24. `alpha search` (keyword) — `health management information system`

### feynman paper (OpenAlex/Europe PMC metadata resolution)
25. `feynman paper 10.1002/asi.21324` → OpenAlex W4234767453 (Technicon TMIS history, Plotnick 2010)
26. `feynman paper 10.1016/0020-7101(91)90036-E` → OpenAlex W2014640834, PMID 1778634 (Collen 1991)

### fetch_content (official pages)
WHO data-collection tools page; MeSH D006751; healthit.gov EMR-vs-EHR; globalhealthdata.org HMIS; MEASURE Evaluation HIS definitions (cpc.unc.edu mirror); healthit.gov EHR adoption data brief 2008–2024; NCBI Bookshelf NBK602592; dhis2.org/hisp-history; who.int/europe strengthening-HIS page. 4 fetch failures recorded under Blocked/Unverified.

---

## Q1. Definitions of HMIS/HIMS (2–4 credible definitions)

**Definition 1 — WHO (primary):** The World Health Organization treats "routine health information systems (RHIS); also termed health management information systems (HMIS)" as one of the data subsystems generating health services data, alongside health facility surveys and resource/logistics/workforce systems:
> "Health services data are generated through a number of data subsystems including: routine health information systems (RHIS); also termed health management information systems (HMIS), health facility surveys, and various systems for health resource data, including logistics management information systems, health workforce information systems and master facility lists." [1]

WHO also names its facility-data toolkit as being about "data collected through routine health facility information systems, also known as health management information systems (HMIS)." [1]

**Definition 2 — WHO/Europe (primary, health system level):**
> "A health information system can be defined as a complex, multilevel system, aimed at producing health intelligence to inform decision-making, and encompassing data collection, analysis, health reporting, knowledge management and governance." [2]

**Definition 3 — Academic/global-health (Global Health Data Methods, based on Lippeveld et al. 2019 Palgrave chapter):**
> "A health management information system (HMIS) collects, stores, analyses, and evaluates health-related data from health facility to district, regional and national administrative levels. It provides analytical reports and visualisations that facilitate decision making at all these levels. HMIS are also referred to as routine health information systems." [3]

**Definition 4 — Academic informatics (Winter et al., *Health Information Systems* textbook, Springer; via NCBI Bookshelf):**
> "A *health information system* (HIS) is the socio-technical subsystem of a health-related setting which comprises all data, information, and knowledge processing as well as the associated human or technical actors in their respective data, information, and knowledge processing roles." [5]

**Definition 5 — MEASURE Evaluation (USAID-funded, HIS field):**
> "Health information systems (HIS) provide evidence for policy and program-level decisions to support better health outcomes at both the individual and population levels. However, the meaning of the term 'HIS' varies across sources, often with no clear or precise definition." MEASURE treats HIS as "an umbrella term encompassing a number of systems, both electronic and paper-based for capturing and transferring health data." [4]

**Definition 6 — NLM MeSH (hospital-level system, primary):**
> Hospital Information Systems: "Integrated, computer-assisted systems designed to store, manipulate, and retrieve information concerned with the administrative and clinical aspects of providing medical services within the hospital." (MeSH descriptor D006751, introduced 1987) [6]

**Terminology note on HIMS vs HMIS:** "HMIS" (health *management* information system) is the standard acronym in WHO/global-health usage; "HIMS" (health information management system / hospital information management system) appears in industry/software usage (e.g., "Hospital Management Information System" in some papers [23, alpha result]) but has no standardized WHO definition. In this note, HMIS = aggregate facility-to-national routine reporting system; hospital-level HMIS/HIS = enterprise hospital system. The two readings of "HMIS/HIMS" are the central ambiguity of the research topic.

---

## Q2. Taxonomy: HMIS vs HIS vs EHR/EMR/LIS/RIS

Key distinctions:
- **HIS (health information system)** is the umbrella concept: "all systems, functions, structures, and personnel related to documenting and using health information" [4]; or the socio-technical subsystem of a health setting [5]. It includes surveys, surveillance, vital registration, census, logistics, workforce, financial, infrastructure and clinical/EMR data [4].
- **HMIS / RHIS (routine health information system)** = the facility-to-national aggregate reporting subsystem of the national HIS [1][3]. WHO's RHIS Strategy (2021) frames RHIS as "an integral part of the overall national health information system (HIS)… collects health service data directly from the health facilities" [49].
- **Hospital Information System (HIS, hospital-level)** = the enterprise system of one hospital; "the term HIS is sometimes confused with the broader concept of health information system" [9]. MeSH: integrated computer-assisted administrative+clinical system within the hospital [6].
- **EMR vs EHR:** EMR = "a digital version of the paper charts in the clinician's office… information in EMRs doesn't travel easily out of the practice"; EHR = broader, "designed to reach out beyond the health organization that originally collects and compiles the information… contain information from all the clinicians involved in the patient's care" (ONC/HealthIT.gov) [7]. EHRs are "parts of health information systems" in the academic model [5].
- **LIS, RIS, CIS** = departmental clinical systems (laboratory, radiology, clinical/ancillary) that are modules or components of a hospital IS [8][10]. NCBI glossary: clinical information system (CIS) integrates medical documentation, nursing documentation, and CPOE; often called EHR system [8].
- **National/regional HIS** = country-level infrastructure assembling all subsystems (HMN framework, WHO 2008) [20].

### Taxonomy table

| Term | Scope level | What it is | Primary source |
|---|---|---|---|
| Health Information System (HIS) | National/health-system | Umbrella: all systems+actors producing/using health data; socio-technical subsystem | [4][5] |
| HMIS / RHIS (routine) | Facility → district → national | Aggregate routine reporting on service delivery, HR, supplies | [1][3][49] |
| Hospital Information System (hospital HIS) | Single hospital | Integrated computer-assisted administrative + clinical system | [6][9] |
| EMR | Practice/department | Digital version of clinician's paper chart; not designed to travel | [7] |
| EHR | Multi-organization / patient-lifetime | Records spanning all clinicians/organizations involved in care; part of HIS | [5][7] |
| LIS | Laboratory dept | Lab workflow: orders, specimens, results | [8][10] |
| RIS | Radiology dept | Imaging workflow: scheduling, exams, reports, PACS links | [8][10] |
| CIS (clinical info system) | Clinical depts | Integrates documentation, nursing, CPOE; often ≈ EHRS | [8] |
| National HIS / HMN framework | Country | All data sources incl. census, surveys, vital events, surveillance, facility data | [4][20] |
| Interoperability layer | Cross-system | HL7 FHIR, openEHR; APIs (e.g., SMART on FHIR) | [8][24][25] |

Academic taxonomy reference: a 2024 JMIR viewpoint proposes "A Taxonomy for Health Information Systems" categorizing ICT used in health care [48].

---

## Q3. History & evolution (dated milestones)

| Date | Milestone | Source |
|---|---|---|
| 1928 | Origins of health information *management* as a profession: Association of Record Librarians of North America (ARLNA) under the American College of Surgeons | [18, sec. via [19]] |
| 1961 | HIMSS founded as the Hospital Management Systems Society (per Wikipedia; HIMSS.org direct page 403-blocked) | [11] |
| 1964–1987 | Technicon Medical Information System (TMIS): systems-theory era hospital-wide MIS; subject of dedicated history paper (Plotnick 2010) | [12] |
| 1965–1974 | First hospital-wide computerized medical information system: Lockheed Missiles & Space Co. + El Camino Hospital (light-pen terminals); IEEE Milestone "Pioneering Medical Information System" | [13] |
| 1967 | HELP system operational at LDS Hospital (heart cath lab, post-op ICU; later full hospital IS with clinical decision support) | [14] |
| 1960s–70s | First EMR-era systems built by academic medical centers; Lockheed product among first clinical information systems (AMA J. Ethics article; page 403 on direct fetch, quote read via search results) | [15] |
| 1970s | PROMIS (Larry Weed, U. Vermont) — problem-oriented computer-based record; NLM archives | [16] |
| 1977 | U.S. Office of Technology Assessment report describes TMIS (El Camino), COSTAR (Harvard Community Health Plan), etc. as the era's medical information systems | [17] |
| 1987 | MeSH descriptor "Hospital Information Systems" introduced (D006751) | [6] |
| 1991 | Collen, "A brief historical overview of hospital information system (HIS) evolution in the United States" — period review paper (Int. J. Bio-Medical Computing) | [26] |
| 1994 | HISP (Health Information Systems Programme) founded — U. Oslo + U. Western Cape, post-apartheid South Africa; leads to DHIS → DHIS2 | [21] |
| 2004 | OpenMRS born in Eldoret, Kenya (AMPATH HIV program; Indiana U. School of Medicine + Partners In Health collaboration) | [22] |
| 2005–2008 | WHO Health Metrics Network (HMN) launched 2005; *Framework and Standards for Country Health Information Systems*, 2nd ed., June 2008 — defines HIS components and national HIS strengthening | [20] |
| 2009 | U.S. HITECH Act (part of ARRA, signed Feb 17, 2009, P.L. 111-5) — incentives for EHR adoption/meaningful use; ONC established | [23][27] |
| 2010 | SMART project starts (Harvard Medical School + Boston Children's Hospital) — substitutable apps platform, later SMART on FHIR | [24] |
| 2011 | Meaningful-use incentive payments begin; ONC Health IT Certification Program certifies products from 2010/2011 | [27] |
| 2012 | FHIR first presented (May 2012); 49 initial resources | [25][28] |
| 2013 | FHIR Draft Standard for Trial Use 1 published (Sep 2013) | [28] |
| 2015 | 96% of U.S. non-federal acute care hospitals, 78% of office-based physicians adopted certified health IT (HITECH Report to Congress 2018) | [29] |
| 2016 | 21st Century Cures Act mandates data sharing/APIs (per ONC data brief) | [27] |
| 2017–2024 | EHR adoption: <10% (2008, Basic EHR) → 99% (2018) → 99.4% (2024); Epic ~50% of hospital market 2024; top-3 developers >80% share 2024 (ONC data brief, AHA IT Supplement) | [27] |
| 2019 | HealthAffairs study attributes large EHR adoption gains to HITECH (hospitals eligible vs ineligible) | [30] |
| 2021 | WHO *Strategy for optimizing national routine health information systems* (RHIS strategy; 9789240087163) | [49] |
| 2021 | WHO/Europe *Support tool to strengthen health information systems*, 2nd ed. (updated from 2015 tool) | [31][32] |
| 2024 | PAHO *Plan of Action for Strengthening Information Systems for Health 2024–2030* (CD61.R7) | [33] |
| 2024 | WHO/Europe issues updated HIS strengthening guidance (June 2024 news) | [34] |
| 2024 | WHO AFRO migrating VPD surveillance from Epi Info to DHIS2 (MMWR, Feb 2024 update) | [39] |

Era synthesis (per brief's framing, supported by [13][15][26][27]):
- **Paper records era (pre-1960s):** standardization of paper records and record librarians (ARLNA 1928) [18/19].
- **Departmental systems (1960s–80s):** standalone lab, pharmacy, billing, ICU systems; early academic prototypes (Lockheed/El Camino, HELP, PROMIS, COSTAR) [13][14][16][17].
- **Integrated hospital information systems (1990s–2000s):** vendor HIS platforms integrate administrative+clinical functions; MeSH codifies "Hospital Information Systems" (1987); Collen's 1991 review documents the field; national HIS frameworks (HMN 2008) [6][20][26].
- **Web/cloud/API era (2010s–present):** HITECH/meaningful use mass adoption [27][29]; FHIR (2012–13) and SMART on FHIR as the interoperability/API layer [24][25][28]; Cures Act data-sharing mandates [27]; cloud deployment growing (cloud HIS market segment) [44].

---

## Q4. Global context: WHO guidance and LMIC implementations

**WHO guidance:**
- WHO data collection tools: RHIS=HMIS as core health-services data subsystem [1].
- WHO HMN Framework (2008): national HIS standards — first global partnership focused on whole-of-HIS strengthening in low/lower-middle income countries [20].
- WHO RHIS Strategy (2021): principles for integrated, interoperable, evidence-informed RHIS, five strategic goals; RHIS "integral part of the overall national HIS" [49].
- WHO/Europe support tool (2nd ed. 2021; updated guidance 2024) [31][34]; PAHO Plan of Action 2024–2030 [33].

**LMIC national HMIS platforms (self-reported figures — treat as self-reported, not independently verified):**
- **DHIS2** — "the world's largest HMIS" per its own site: national-scale HMIS in **70+ countries** representing key health data for **>40% of the world's population** [35]; also stated as ministries of health in **75–80 LMICs** as government-wide system of record, covering **~3.2 billion people**, and >100 countries incl. NGO programs [36][37]. Open-source, UiO HISP Centre; used for COVID-19 response [21][37].
- **OpenMRS** — EMR platform: self-reported **80+ countries, 8,100+ sites/facilities, 22M+ patients** [40][41]. Peer-reviewed 2021 paper reported 62 countries (community annual report) and quantified data volume at surveyed sites [42].
- **Country cases:** Kenya OpenMRS↔DHIS2 aggregate-data interoperability field test (2016) [38]; Bangladesh national DHIS2 rollout (GIZ-supported, "A Quiet Revolution" case study) [47]; WHO AFRO Epi Info→DHIS2 migration for VPD surveillance (MMWR 2024) [39]; PAHO Antigua & Barbuda DHIS2-based HMIS (2024) [50].
- **Hospital-level vs national-level:** EMHJ 2025 multi-country review distinguishes hospital HIS (intra-hospital coordination) from national health information systems and analyzes governance of interoperability [9].

---

## Q5. Market / scale figures (with sources and year — LOW CONFIDENCE, vendor estimates differ widely)

| Metric | Figure | Year | Source (vendor report) |
|---|---|---|---|
| Global Hospital Information Systems market | USD 63.81B → 116.76B by 2030, CAGR 10.6% | 2024 | MarketsandMarkets [43] |
| Global HIS market | USD 152.72B → 687.32B by 2033, CAGR 18.44% | 2024 | Grand View Research [45] |
| Global HIS market | $140.27B in 2025 → $225.01B by 2030, CAGR 9.9%; North America largest, APAC fastest | 2025 | The Business Research Company [46] |
| HIS market 2024 (range across scenarios) | USD 38.5–64.3B, CAGR 7.0–9.0% to 2030 | 2024 | Research and Markets [51] |
| Cloud-based healthcare information system | US$54.28B (2024) → US$149.74B (2031), CAGR 15.6% | 2024 | QY Research [44] |
| Healthcare IT (broader) | USD 866.48B (2025) → 2,864.39B (2033), CAGR 16.2% | 2025 | Grand View Research [52] |

**Adoption (primary source, high confidence):** U.S. non-federal acute care hospitals: <10% "Basic EHR" (2008) → 99% certified EHR (2018) → 99.4% (2024); Epic ~50% hospital share 2024, top-3 developers >80% [27]. 96% of hospitals / 78% of office physicians with certified health IT as of 2015 (HITECH Report to Congress) [29].

**Caveat:** the four HIS-market reports disagree by ~2.4× on the same base year (38.5–152.7B USD for ~2024); figures are analyst estimates, not audited data. Use only as order-of-magnitude, and prefer the ONC adoption statistics for defensible numbers.

---

## Evidence table

| # | Source | URL | Key claim | Type | Confidence |
|---|--------|-----|-----------|------|------------|
| 1 | WHO — Standards and tools to strengthen health services data | https://www.who.int/data/data-collection-tools/standards-and-tools-to-strengthen-health-services-data | RHIS "also termed health management information systems (HMIS)"; facility-data toolkit targets HMIS | primary | high (fetched) |
| 2 | WHO/Europe — Strengthening health information systems | https://www.who.int/europe/teams/data-and-digital-health/strengthening-health-information-systems | HIS = "complex, multilevel system… producing health intelligence… data collection, analysis, health reporting, knowledge management and governance" | primary | high (fetched) |
| 3 | Global Health Data Methods — HMIS (Lippeveld et al. 2019, Palgrave) | https://globalhealthdata.org/health-management-information-systems/ | HMIS definition; facility→national aggregation; DHIS2/iHRIS/eLMIS examples | secondary (academic chapter summary) | high (fetched) |
| 4 | MEASURE Evaluation — Defining Health Information Systems | https://measureevaluation.cpc.unc.edu/his-strengthening-resource-center/his-definitions.html | HIS umbrella term; no single precise definition; components incl. EHRs, surveys, surveillance, vital events | primary (USAID project) | high (fetched; original domain fetch failed, mirror used) |
| 5 | Winter et al., Health Information Systems (Springer) via NCBI Bookshelf — Basic Concepts | https://www.ncbi.nlm.nih.gov/books/NBK602592/ | HIS = socio-technical subsystem; EHR as part of HIS; transinstitutional HIS | primary (academic book) | high (fetched) |
| 6 | NLM MeSH — Hospital Information Systems D006751 | https://meshb.nlm.nih.gov/record/ui?ui=D006751 | MeSH scope note; descriptor introduced 1987 | primary | high (fetched) |
| 7 | ONC HealthIT.gov — EMR vs EHR | https://healthit.gov/blog/electronic-health-and-medical-records/emr-vs-ehr-difference/ | EMR/EHR distinction per ONC | primary (gov) | high (fetched) |
| 8 | NCBI Bookshelf — Glossary (NBK602584) | https://www.ncbi.nlm.nih.gov/books/NBK602584/ | CIS/EHRS/EHR glossary definitions; FHIR/openEHR for SOA | primary (academic) | medium (fetched via search content) |
| 9 | WHO EMHJ 2025 — multi-country review of hospital HIS interoperability governance | https://www.emro.who.int/emhj-volume-31-2025/volume-31-issue-9/a-multi-country-review-of-the-governance-of-hospital-information-systems-interoperability.html | Hospital HIS vs broader health information system distinction; governance review | primary | medium (abstract-level) |
| 10 | Candelis — RIS, LIS, CIS, HIS differences | https://www.candelis.com/blog/ris-lis-cis-his | RIS/LIS/CIS/HIS functional differences | secondary (vendor blog) | low-medium |
| 11 | Wikipedia — Healthcare Information and Management Systems Society | https://en.wikipedia.org/wiki/Healthcare_Information_and_Management_Systems_Society | HIMSS founded 1961 as Hospital Management Systems Society | secondary | medium |
| 12 | Plotnick 2010 — Computers, systems theory, and the making of a wired hospital: history of Technicon MIS 1964–1987 (JASIST; DOI 10.1002/asi.21324) | https://onlinelibrary.wiley.com/doi/10.1002/asi.21324 | TMIS 1964–1987 history | primary (peer-reviewed) | medium (metadata/abstract via OpenAlex W4234767453; full text not read) |
| 13 | IEEE Milestone — Pioneering Medical Information System, 1965–1974 | https://ethw.org/Milestones:Pioneering_Medical_Information_System,_1965-1974 | Lockheed + El Camino Hospital first hospital-wide computerized MIS 1965–74 | primary (IEEE) | medium-high |
| 14 | The HELP hospital information system: update 1998 (Int. J. Med. Informatics) | https://www.sciencedirect.com/science/article/abs/pii/S1386505699000131 | HELP operational at LDS Hospital since 1967 | primary | medium (abstract-level) |
| 15 | AMA Journal of Ethics 2011 — Development of the Electronic Health Record | https://journalofethics.ama-assn.org/article/development-electronic-health-record/2011-03 | EHR development periods; Lockheed mid-1960s; academic centers 1960s–70s | primary | medium (read via search result quote; direct fetch 403) |
| 16 | NLM Circulating Now — The PROMIS of Computer-Based Medical Records (2019) | https://circulatingnow.nlm.nih.gov/2019/06/27/the-promis-of-computer-based-medical-records/ | PROMIS/Weed U. Vermont archives at NLM | primary (NLM) | medium (snippet) |
| 17 | U.S. OTA 1977 — Policy Implications of Medical Information Systems | https://www.princeton.edu/~ota/disk3/1977/7708/770805.PDF | 1977-era systems: TMIS, COSTAR | primary (gov report, PDF) | medium (metadata/skim only) |
| 18 | PMC — Convergent Evolution of Health Information Management and Informatics (JAMIA) | https://pmc.ncbi.nlm.nih.gov/articles/PMC4377568/ | HIM profession origins 1928 ARLNA under ACS | primary (peer-reviewed) | medium (snippet) |
| 19 | Wikipedia — HIMSS (same as [11]) | — | — | — | — |
| 20 | WHO/HMN — Framework and Standards for Country Health Information Systems, 2nd ed. (2008) | https://www.who.int/publications/i/item/9789241595940 | National HIS framework; HMN launched 2005 | primary | medium-high |
| 21 | DHIS2 — HISP History | https://dhis2.org/hisp-history/ | HISP since 1994 (U. Oslo + U. Western Cape, post-apartheid SA); DHIS→DHIS2; 30 yrs in 2024 | self-reported | high for dates (fetched) |
| 22 | OpenMRS Guide — A Brief History | https://guide.openmrs.org/introduction/a-brief-history/ | OpenMRS origins Eldoret, Kenya 2004 (AMPATH) | self-reported | medium-high |
| 23 | govinfo — HITECH Act text (Title XXX, COMPS-9279) | https://www.govinfo.gov/content/pkg/COMPS-9279/pdf/COMPS-9279.pdf | HITECH definitions, certified EHR technology | primary (US law) | high (metadata) |
| 24 | Mandel et al. 2016 — SMART on FHIR (JAMIA, PMC4997036) | https://pmc.ncbi.nlm.nih.gov/articles/PMC4997036/ | SMART project began 2010 at Harvard/Boston Children's; standards-based apps platform | primary (peer-reviewed) | medium-high |
| 25 | HL7 — FHIR History (v5.0.0) | https://hl7.org/fhir/history.html | FHIR release history (5.0.0 Mar 2023; 4.0.1 Dec 2018; 3.0.2 Feb 2017; 1.0.2) | primary (standards body) | high |
| 26 | Collen 1991 — Brief historical overview of HIS evolution in the US (Int. J. Bio-Medical Computing; PMID 1778634) | https://pubmed.ncbi.nlm.nih.gov/1778634/ | Period review of US HIS evolution | primary (peer-reviewed) | medium (metadata via OpenAlex W2014640834; full text not read) |
| 27 | ONC — Non-Federal Acute Care Hospital EHR Adoption, 2008–2024 (Data Brief 83) | https://healthit.gov/data/data-briefs/non-federal-acute-care-hospital-electronic-health-record-adoption-2008-2024/ | <10% (2008)→99% (2018)→99.4% (2024); Epic ~50% 2024; top-3 >80% | primary (gov, AHA survey) | high (fetched) |
| 28 | ONC — FHIR Version History and Maturity fact sheet | https://healthit.gov/wp-content/uploads/2021/04/FHIR-Version-History-Fact-Sheet.pdf | FHIR first presented May 2012; DSTU1 Sep 2013 with 49 resources | primary (gov) | high (snippet) |
| 29 | ONC — 2018 HITECH Report to Congress | https://beta.healthit.gov/wp-content/uploads/2018/12/2018-HITECH-report-to-congress.pdf | 96% hospitals / 78% physicians with certified health IT as of 2015 | primary (gov) | high (snippet) |
| 30 | Adler-Milstein & Jha 2017 — HITECH Act Drove Large Gains in Hospital EHR Adoption (Health Affairs) | https://www.healthaffairs.org/doi/10.1377/hlthaff.2016.1651 | HITECH differential effect on EHR adoption | primary (peer-reviewed) | medium (abstract) |
| 31 | WHO/Europe — Support tool to strengthen health information systems, 2nd ed. (2021) | https://www.who.int/europe/publications/i/item/9789289061148 | HIS assessment/strategy tool; update of 2015 tool | primary | medium (snippet; iris.who.int page fetch failed) |
| 32 | WHO/Europe news — updated HIS guidance (11 Jun 2024) | https://www.who.int/europe/news/item/11-06-2024-who-issues-updated-guidance-for-strengthening-health-information-systems | 2024 update of support tool | primary | medium (snippet) |
| 33 | PAHO — CD61.R7 Plan of Action for Strengthening Information Systems for Health 2024–2030 | https://www.paho.org/en/documents/cd61r7-plan-action-strengthening-information-systems-health-2024-2030 | Regional HIS plan 2024–2030 | primary | medium (snippet) |
| 34 | JMIR 2024 — A Taxonomy for Health Information Systems | https://www.jmir.org/2024/1/e47682/ | Academic taxonomy of health ICT | primary (peer-reviewed) | medium (abstract) |
| 35 | DHIS2 — DHIS2: The world's largest HMIS | https://dhis2.org/health/hmis/ | 70+ countries national-scale HMIS; >40% world population | self-reported | medium |
| 36 | DHIS2 — About DHIS2 | https://dhis2.org/about-2/ | 80+ LMICs system of record; 100+ countries incl. NGOs | self-reported | medium |
| 37 | DHIS2 — Press / In Action | https://dhis2.org/press/ · https://dhis2.org/in-action/ | 75–80 LMIC ministries; ~3.2B people covered | self-reported | medium |
| 38 | OJPubHealth 2016 — Automating indicator data reporting from health facility EMR to national aggregate data in Kenya (OpenMRS + DHIS2) | https://ojphi.jmir.org/2016/2/e61903 | Kenya interoperability field test; SDMX-HD (WHO 2009) | primary (peer-reviewed) | medium |
| 39 | CDC MMWR 2024 — Migration from Epi Info to DHIS2 for VPD surveillance, WHO AFRO | https://www.cdc.gov/mmwr/volumes/73/wr/mm7323a2.htm | AFRO transition to DHIS2; status by Feb 2024 | primary (CDC) | high |
| 40 | OpenMRS — Our Impact | https://openmrs.org/our-impact/ | 80+ countries, 8,100+ sites, 22M+ patients; 4,050+ publications | self-reported | medium |
| 41 | OpenMRS Wiki — OpenMRS Around the World | https://openmrs.atlassian.net/wiki/spaces/docs/pages/282624121/OpenMRS+Around+the+World | 80+ countries production; 8,100+ facilities | self-reported | medium |
| 42 | Blaya et al./ Sciencedirect 2021 — OpenMRS as a global good | https://www.sciencedirect.com/science/article/abs/pii/S1386505621000319 | OpenMRS in 62+ countries; survey data volumes | primary (peer-reviewed) | medium (abstract) |
| 43 | MarketsandMarkets — Hospital Information Systems Market 2025–2030 | https://www.marketsandmarkets.com/Market-Reports/hospital-information-system-market-144083226.html | USD 63.81B (2024) → 116.76B (2030), CAGR 10.6% | secondary (vendor) | low |
| 44 | QY Research — Cloud-based Healthcare Information System 2025–2031 | https://www.qyresearch.com/reports/5055606/cloud-based-healthcare-information-system | US$54.28B (2024) → US$149.74B (2031), CAGR 15.6% | secondary (vendor) | low |
| 45 | Grand View Research — Hospital Information System Market | https://www.grandviewresearch.com/industry-analysis/hospital-information-systems-his-market | USD 152.72B (2024) → 687.32B (2033), CAGR 18.44% | secondary (vendor) | low |
| 46 | The Business Research Company — Hospital Information System Global Market Report 2026 | https://www.thebusinessresearchcompany.com/report/hospital-information-system-global-market-report | $140.27B (2025) → $225.01B (2030), CAGR 9.9% | secondary (vendor) | low |
| 47 | GIZ / Healthy DEvelopments — A Quiet Revolution (Bangladesh DHIS2) | https://health.bmz.de/studies/a-quiet-revolution/ | Bangladesh national DHIS2 rollout via MoHFW/GIZ | primary (case study) | medium |
| 48 | HealthIT.gov — Base EHR definition / certification criteria | https://healthit.gov/certification-health-it/certification-criteria/base-electronic-health-record-definition/ | CEHRT baseline capabilities | primary (gov) | medium (snippet) |
| 49 | WHO — Strategy for optimizing national RHIS (2021) | https://www.who.int/publications/i/item/9789240087163 | RHIS integral to national HIS; 5 strategic goals | primary | medium-high (snippet) |
| 50 | PAHO news (12 Feb 2024) — Antigua and Barbuda HMIS | https://www.paho.org/en/news/14-2-2024-health-management-tool-strengthening-health-system | DHIS2-based HMIS in Antigua & Barbuda (PAHO) | primary | medium |
| 51 | Research and Markets — HIS Global Market Insights 2025 | https://www.researchandmarkets.com/reports/6036606/hospital-information-system-his-global-market | HIS valued 38.5–64.3B USD (2024); CAGR 7–9% | secondary (vendor) | low |
| 52 | Grand View Research — Healthcare IT Market 2026–2033 | https://www.grandviewresearch.com/industry-analysis/healthcare-it-market | Healthcare IT USD 866.48B (2025) → 2,864.39B (2033), CAGR 16.2% | secondary (vendor) | low |

---

## Sources consulted (all URLs)

1. WHO — Standards and tools to strengthen health services data — https://www.who.int/data/data-collection-tools/standards-and-tools-to-strengthen-health-services-data
2. WHO/Europe — Strengthening health information systems — https://www.who.int/europe/teams/data-and-digital-health/strengthening-health-information-systems
3. Global Health Data Methods — Health management information systems — https://globalhealthdata.org/health-management-information-systems/
4. MEASURE Evaluation — Defining Health Information Systems — https://measureevaluation.cpc.unc.edu/his-strengthening-resource-center/his-definitions.html (original: https://www.measureevaluation.org/his-strengthening-resource-center/his-definitions.html)
5. Winter A. et al., *Health Information Systems: Architectures and Strategies*, Springer 2023, via NCBI Bookshelf NBK602592 — https://www.ncbi.nlm.nih.gov/books/NBK602592/
6. NLM MeSH — Hospital Information Systems — https://meshb.nlm.nih.gov/record/ui?ui=D006751
7. ONC/HealthIT.gov — EMR vs EHR – What is the Difference? — https://healthit.gov/blog/electronic-health-and-medical-records/emr-vs-ehr-difference/
8. NCBI Bookshelf — Glossary (NBK602584) — https://www.ncbi.nlm.nih.gov/books/NBK602584/
9. WHO EMHJ 2025 — A multi-country review of the governance of hospital information systems interoperability — https://www.emro.who.int/emhj-volume-31-2025/volume-31-issue-9/a-multi-country-review-of-the-governance-of-hospital-information-systems-interoperability.html
10. Candelis — RIS, LIS, CIS and HIS: What Are the Differences? — https://www.candelis.com/blog/ris-lis-cis-his
11. Wikipedia — Healthcare Information and Management Systems Society — https://en.wikipedia.org/wiki/Healthcare_Information_and_Management_Systems_Society
12. Plotnick (2010) — Computers, systems theory, and the making of a wired hospital (Technicon TMIS 1964–1987), JASIST — https://onlinelibrary.wiley.com/doi/10.1002/asi.21324 (OpenAlex: https://openalex.org/W4234767453)
13. IEEE Milestone — Pioneering Medical Information System, 1965–1974 — https://ethw.org/Milestones:Pioneering_Medical_Information_System,_1965-1974
14. The HELP hospital information system: update 1998 — https://www.sciencedirect.com/science/article/abs/pii/S1386505699000131
15. AMA Journal of Ethics (2011) — Development of the Electronic Health Record — https://journalofethics.ama-assn.org/article/development-electronic-health-record/2011-03
16. NLM Circulating Now (2019) — The PROMIS of Computer-Based Medical Records — https://circulatingnow.nlm.nih.gov/2019/06/27/the-promis-of-computer-based-medical-records/
17. U.S. OTA (1977) — Policy Implications of Medical Information Systems — https://www.princeton.edu/~ota/disk3/1977/7708/770805.PDF
18. PMC4377568 — Convergent Evolution of Health Information Management and Informatics — https://pmc.ncbi.nlm.nih.gov/articles/PMC4377568/
19. HIMSS — https://www.himss.org/ (fetch 403; cited via [11] instead)
20. WHO/HMN (2008) — Framework and Standards for Country Health Information Systems, 2nd ed. — https://www.who.int/publications/i/item/9789241595940
21. DHIS2 — HISP History — https://dhis2.org/hisp-history/
22. OpenMRS Guide — A Brief History — https://guide.openmrs.org/introduction/a-brief-history/
23. govinfo — Title XXX Health Information Technology (HITECH) — https://www.govinfo.gov/content/pkg/COMPS-9279/pdf/COMPS-9279.pdf
24. Mandel et al. (2016) — SMART on FHIR — https://pmc.ncbi.nlm.nih.gov/articles/PMC4997036/
25. HL7 FHIR — History — https://hl7.org/fhir/history.html
26. Collen (1991) — A brief historical overview of hospital information system (HIS) evolution in the United States — https://pubmed.ncbi.nlm.nih.gov/1778634/ (DOI https://doi.org/10.1016/0020-7101(91)90036-E; OpenAlex https://openalex.org/W2014640834)
27. ONC — Non-Federal Acute Care Hospital EHR Adoption, 2008–2024 — https://healthit.gov/data/data-briefs/non-federal-acute-care-hospital-electronic-health-record-adoption-2008-2024/
28. ONC — FHIR Version History and Maturity — https://healthit.gov/wp-content/uploads/2021/04/FHIR-Version-History-Fact-Sheet.pdf
29. ONC — 2018 HITECH Report to Congress — https://beta.healthit.gov/wp-content/uploads/2018/12/2018-HITECH-report-to-congress.pdf
30. Adler-Milstein & Jha (2017) — HITECH Act Drove Large Gains In Hospital EHR Adoption, Health Affairs — https://www.healthaffairs.org/doi/10.1377/hlthaff.2016.1651
31. WHO/Europe — Support tool to strengthen health information systems, 2nd ed. — https://www.who.int/europe/publications/i/item/9789289061148 (IRIS record: https://iris.who.int/items/f0153248-5dcd-4246-aa79-3a0e3c2ed62e)
32. WHO/Europe news (2024-06-11) — WHO issues updated guidance — https://www.who.int/europe/news/item/11-06-2024-who-issues-updated-guidance-for-strengthening-health-information-systems
33. PAHO — CD61.R7 Plan of Action 2024–2030 — https://www.paho.org/en/documents/cd61r7-plan-action-strengthening-information-systems-health-2024-2030
34. JMIR (2024) — A Taxonomy for Health Information Systems — https://www.jmir.org/2024/1/e47682/
35. DHIS2 — The world's largest HMIS — https://dhis2.org/health/hmis/
36. DHIS2 — About — https://dhis2.org/about-2/
37. DHIS2 — Press — https://dhis2.org/press/ · In Action — https://dhis2.org/in-action/
38. OJPubHealth (2016) — Kenya OpenMRS–DHIS2 interoperability field test — https://ojphi.jmir.org/2016/2/e61903
39. CDC MMWR (2024) — Migration from Epi Info to DHIS2, WHO AFRO — https://www.cdc.gov/mmwr/volumes/73/wr/mm7323a2.htm
40. OpenMRS — Our Impact — https://openmrs.org/our-impact/
41. OpenMRS Wiki — OpenMRS Around the World — https://openmrs.atlassian.net/wiki/spaces/docs/pages/282624121/OpenMRS+Around+the+World
42. OpenMRS as a global good (2021) — https://www.sciencedirect.com/science/article/abs/pii/S1386505621000319
43. MarketsandMarkets — Hospital Information Systems Market — https://www.marketsandmarkets.com/Market-Reports/hospital-information-system-market-144083226.html
44. QY Research — Cloud-based Healthcare Information System — https://www.qyresearch.com/reports/5055606/cloud-based-healthcare-information-system
45. Grand View Research — Hospital Information System Market — https://www.grandviewresearch.com/industry-analysis/hospital-information-systems-his-market
46. The Business Research Company — Hospital Information System Global Market Report 2026 — https://www.thebusinessresearchcompany.com/report/hospital-information-system-global-market-report
47. GIZ — A Quiet Revolution (Bangladesh DHIS2) — https://health.bmz.de/studies/a-quiet-revolution/
48. ONC — Base EHR Definition — https://healthit.gov/certification-health-it/certification-criteria/base-electronic-health-record-definition/
49. WHO (2021) — Strategy for optimizing national routine health information systems — https://www.who.int/publications/i/item/9789240087163
50. PAHO news (2024-02-12) — Antigua and Barbuda HMIS — https://www.paho.org/en/news/14-2-2024-health-management-tool-strengthening-health-system
51. Research and Markets — HIS Global Market Insights 2025 — https://www.researchandmarkets.com/reports/6036606/hospital-information-system-his-global-market
52. Grand View Research — Healthcare IT Market — https://www.grandviewresearch.com/industry-analysis/healthcare-it-market

Also consulted: WHO Implementation guide to the RHIS toolkit — https://www.who.int/publications/i/item/9789240089204; WHO "How do other organizations define HIS?" — https://www.measureevaluation.org/his-strengthening-resource-center/his-definitions/how-do-other-organizations-define-his.html; NTT DATA interoperability blog — https://us.nttdata.com/en/blog/2023/july/healthcare-interoperability-blog2; MSF OpenFn-LIME (DHIS2/OpenMRS/OCL) — https://github.com/OpenFn/openfn-lime.

---

## Coverage Status

- **Checked directly (fetched):** WHO data-collection page, WHO/Europe HIS page, MeSH D006751, HealthIT.gov EMR-vs-EHR, globalhealthdata.org, MEASURE (mirror), ONC data brief 83, NCBI NBK602592 (Basic Concepts), dhis2.org HISP history.
- **Metadata/abstract only (per brief — no PDFs fetched):** Technicon paper (OpenAlex), Collen 1991 (OpenAlex + Europe PMC PMID), HELP 1998, OpenMRS-as-global-good 2021, HealthAffairs 2017, JMIR taxonomy 2024, SMART-on-FHIR 2016.
- **Read via search-result quotes only (direct fetch failed/403):** AMA J. Ethics 2011 (403), HIMSS about page (403), WHO/Europe support tool (IRIS page not parseable), HMN framework PDF (metadata only).
- **Unverified / to confirm later:** exact first-release year of DHIS2 (sources confirm HISP 1994→DHIS→DHIS2 but not a release date; "2006" was NOT sourced and is therefore not asserted); HIMSS 1961 founding (Wikipedia only); all market-size figures (vendor estimates, conflicting); DHIS2/OpenMRS adoption counts (self-reported).
- **Blocked capabilities:** `feynman_science_database_search` (pubmed/openalex/crossref) is not exposed in this runtime's feynman CLI; used `feynman paper` (OpenAlex/Europe PMC) as the closest available equivalent. Direct PDF full-text reading intentionally not performed (brief forbids PDF fetching for alpha_search items).

---

## Blocked / Unverified items

| Item | Status | Detail |
|---|---|---|
| `feynman_science_database_search` tool | Blocked | Not available in this runtime; substituted `feynman paper` (OpenAlex/Europe PMC metadata) + `alpha search` |
| AMA J. Ethics 2011 full text | Blocked | HTTP 403 on direct fetch; claims taken only from search-result quote |
| HIMSS.org about page | Blocked | HTTP 403; founding year via Wikipedia only |
| iris.who.int support-tool record page | Blocked | Could not extract readable content; cited via who.int/europe publication page |
| measureevaluation.org original domain | Blocked | fetch failed; used cpc.unc.edu mirror (same content, verified identical lead text) |
| DHIS2 first release year | Unverified | No source found stating an exact year; do not assert |
| Market size figures | Unverified (low confidence) | Vendor analyst estimates conflict (38.5–152.7B USD for 2024); order-of-magnitude only |
| DHIS2 / OpenMRS country & patient counts | Self-reported | Marked as self-reported; not independently verified |
| HIMSS 1961 founding | Unverified (secondary) | Wikipedia only |
