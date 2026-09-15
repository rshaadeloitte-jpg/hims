import type { components } from "./schema";

/**
 * Augments the OpenAPI-generated types (src/api/schema.d.ts) with the pieces of
 * the wire contract that the generated schema cannot express:
 *
 *  1. Entity-specific legacy fields (code, name, description, ...) are returned
 *     in the JSON but the OpenAPI contract only models the common base fields
 *     (MasterEntityBase). They are documented per entity in
 *     src/features/masters/entityMeta.ts — the schema-driven core.
 *  2. The write surface (POST create / PUT update / DELETE, MIG-010 W1) is
 *     modeled generically with an untyped JsonNode body — the client sends
 *     entity-shaped payloads described by entityMeta.ts.
 */

/** Base entity fields modeled by the OpenAPI contract. */
export type MasterEntityBase = components["schemas"]["MasterEntityBase"];

/**
 * A legacy-shaped master record. The index signature carries the entity-specific
 * fields whose names are preserved verbatim from the legacy Jackson serialization
 * (UnitMaster ~90 fields, DepartmentMaster/CompanyMaster fewer).
 */
export type MasterRecord = MasterEntityBase & Record<string, unknown>;

/** Wire query params shared by the list endpoints (crudgetAll / crudgetAllAll). */
export interface ListQuery {
  /** Unit scope (legacy derives from the session token). */
  unitId?: number;
  /** Search terms. crudgetAll aligns comma-separated terms to searchFld fields. */
  search?: string;
  /** Space-separated field names to search. */
  searchFld?: string;
  /** Legacy page param — see legacyWirePage() for the offset semantics. */
  page?: number;
  /** Page size. */
  count?: number;
}

/** Payload for create/update (MIG-010 write surface, planned contract). */
export type MasterPayload = Record<string, unknown>;
