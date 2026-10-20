# Researcher Brief T4 — Compliance, Vendor/Market Landscape, Modernization

**Task:** Research security/compliance requirements, the current (2025–2026) vendor/technology landscape, and modernization approaches for hospital/health information management systems. Write findings to `outputs/.drafts/hims-system-research-T4.md`.

## Questions to answer
1. Security & compliance: HIPAA (US) — what it requires for health systems (privacy rule, security rule, breach notification, audit controls); GDPR (EU) — health data as special category; other notable regimes (e.g., India DPDP, UK GDPR/Data Protection Act). Consequences/non-compliance penalties with sources. Role of RBAC, audit logging, encryption, data residency.
2. Vendor landscape 2025–2026: major commercial EHR/HMIS vendors (Epic, Oracle Health (Cerner), Meditech, etc.) — market position, notable events (e.g., Oracle-Cerner acquisition), and any 2025–2026 news (e.g., Epic/Oracle market share, government/enterprise deals). Open-source (OpenMRS/Bahmni) adoption in enterprise settings.
3. Cloud & AI trends: cloud/SaaS adoption in healthcare, AI/ML features in HMIS (clinical documentation, coding, chatbots, ambient scribes) — 2025–2026 examples with sources.
4. Modernization approaches for legacy HMIS: strangler-fig migration, microservices decomposition, API-first/FHIR-first, database modernization (legacy RDBMS → cloud, ETL), frontend modernization (legacy web → React/SPA), data migration risks, documented failure/success cases. Find credible practitioner or industry sources (consulting whitepapers, IEEE/ACM papers via abstracts, HIMSS, Gartner-style summaries available on the web).
5. Market size: HMIS/healthcare IT market size figures with year + source if findable.

## Sources to use
- `web_search` (≥3 distinct queries per facet — compliance, vendors, AI trends, modernization; record exact query strings) — current-date claims MUST come from 2025–2026 web sources
- `alpha_search` for modernization/migration papers (abstracts/metadata only; **no PDF fetching** — cite PDF URLs from metadata and mark parsing blocked if needed)
- `feynman_science_database_search`: pubmed/openalex for security/modernization research
- `fetch_content` for HHS/HIPAA pages, GDPR pages, vendor news, consultancy articles

## Output requirements
- Write structured markdown to `outputs/.drafts/hims-system-research-T4.md`
- Sections: Compliance (regime | requirements | penalties | URL), Vendor landscape 2025–26 (with dates + URLs), Cloud & AI trends (dated examples), Modernization approaches (patterns + evidence), Market size (if found), Sources consulted (all URLs), Blocked/Unverified items
- Record all search queries under "Search terms" at top
- Distinguish dated facts from inference. Do NOT invent market shares, penalties, or features.

## If a tool fails
Continue with remaining tools; record the failure under Blocked/Unverified.
