# Researcher Brief T3 — HMIS Standards & Interoperability

**Task:** Research the standards and interoperability landscape for health information management systems. Write findings to `outputs/.drafts/hims-system-research-T3.md`.

## Questions to answer
1. Messaging/interface standards: HL7 v2 (and v2.x), HL7 FHIR (current major versions, e.g., R4/R5), DICOM (imaging), IHE profiles (XDS, PDQ, PIX, etc.) — what each covers, who maintains it, current status/version.
2. Clinical terminologies & code systems: SNOMED CT, LOINC, ICD-10/ICD-11, CPT, RxNorm, UCUM — what each is used for in an HMIS context.
3. How standards enable module integration: order entry → lab/radiology/pharmacy flows, patient identity (EMPI/PIX), document exchange (CDA/FHIR Documents).
4. Open-source ecosystems: OpenMRS, Bahmni, DHIS2 (and its role vs HMIS) — architecture, FHIR support, global adoption.
5. National/regional interoperability frameworks: e.g., US (ONC certification, TEFCA), EU (EHDS/European Health Data Space), UK (NHS), India (ABDM/NDHM) — current status with dates and sources.
6. Current FHIR adoption trends in hospital systems (2024–2026) if findable.

## Sources to use
- `web_search` (≥3 distinct queries, record exact query strings) — prefer official docs: hl7.org, hl7.org/fhir, dicom.nema.org, ihe.net, snomed.org, loinc.org, openmrs.org, bahmni.org, dhis2.org, healthit.gov, TEFCA, NHS, ABDM
- `alpha_search` for interoperability papers (abstracts/metadata only; **no PDF fetching** — cite PDF URLs from metadata and mark parsing blocked if needed)
- `feynman_science_database_search`: pubmed/openalex for interoperability research
- `fetch_content` for official standard pages

## Output requirements
- Write structured markdown to `outputs/.drafts/hims-system-research-T3.md`
- Sections: Standards table (standard | scope | maintainer | current version/status | URL), Terminology table, Interoperability flows, Open ecosystems, National frameworks (with dates), Sources consulted (all URLs), Blocked/Unverified items
- Record all search queries under "Search terms" at top
- Do NOT invent version numbers or adoption figures. Mark unverified claims.

## If a tool fails
Continue with remaining tools; record the failure under Blocked/Unverified.
