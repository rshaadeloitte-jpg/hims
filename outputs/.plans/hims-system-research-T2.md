# Researcher Brief T2 — HMIS Functional Architecture & Modules

**Task:** Research the functional architecture, core modules, and typical (especially legacy) architecture patterns of hospital/health management information systems. Write findings to `outputs/.drafts/hims-system-research-T2.md`.

## Questions to answer
1. Core modules of a typical HMIS: patient registration/ADT (admission-discharge-transfer), outpatient & inpatient management, billing/finance/claims, pharmacy, laboratory (LIS), radiology (RIS/PACS), blood bank, inventory/supply chain, HR/payroll (optional), reporting/analytics. For each: what it does, typical data flows between modules.
2. Common legacy architecture patterns: monolithic client-server (e.g., PowerBuilder/VB/Java Swing/COBOL/CGI era), shared database schema, batch interfaces, point-to-point integration, MUMPS-based systems (Epic, Meditech history), aging frontends (e.g., web forms, ActiveX, legacy JS frameworks).
3. Typical integration topologies in legacy HMIS: direct DB links, HL7 message queues, file-based batch, ETL to data warehouse.
4. Reference architectures for modern HMIS: modular/microservices, API-first, FHIR-based, event-driven (Kafka-style), cloud deployment — find at least 2–3 credible reference descriptions (vendor whitepapers, HIMSS, academic).
5. Data model highlights: patient master index (EMPI), encounter model, order-entry model, financial model — sourced from credible docs where possible.

## Sources to use
- `web_search` (≥3 distinct queries, record exact query strings)
- `alpha_search` for papers on hospital information system architecture (abstracts/metadata only; **no PDF fetching** — cite PDF URLs from metadata and mark parsing blocked if needed)
- `feynman_science_database_search`: pubmed/openalex for architecture papers
- `fetch_content` for vendor docs / HIMSS / architecture articles
- Local workspace context: you may skim `docs/modernization/02-business-module-catalog.md` and `docs/modernization/10-target-architecture.md` for an example legacy HMIS module set — label these as LOCAL case context, not external evidence

## Output requirements
- Write structured markdown to `outputs/.drafts/hims-system-research-T2.md`
- Sections: Module catalog (table: module | function | data flows), Legacy architecture patterns (with real examples/sources), Integration topologies, Modern reference architectures, Data model highlights, Sources consulted (all URLs), Blocked/Unverified items
- Record all search queries under "Search terms" at top
- Do NOT invent modules, numbers, or sources. Mark unverified claims.

## If a tool fails
Continue with remaining tools; record the failure under Blocked/Unverified.
