/**
 * entityMeta.ts — the schema-driven core of the masters engine.
 *
 * This file is a MINI FAUG-SCHEMA ADAPTER: it describes each master entity the
 * same way the legacy FAUG engine did (field model per entity), so that lists,
 * forms, validation and search are all *derived* from metadata instead of being
 * hand-written per screen. The legacy screens this replaces are the FAUG
 * config-driven master CRUD screens (§51 / docs/modernization/09-react-migration-design.md).
 *
 * ── MAPPING CONTRACT (for the future FAUG transpiler) ──────────────────────
 * A legacy FAUG field model (e.g. walkinVisit-schema.json field blocks) maps
 * onto {@link FieldMeta} as follows:
 *
 *   name        <- field key — the JSON property name, PRESERVED VERBATIM.
 *                  Legacy serialization keeps PascalCase-ish identifiers
 *                  (unitId, addeddatetime, isclinical, ...). Never rename.
 *   label       <- ui.label, else humanize(name)
 *   type        <- ui.type / widget:
 *                    'select' | 'combobox' | dropdown      -> 'select' (needs options)
 *                    'checkbox' | 'toggle' | boolean column -> 'boolean'
 *                    'numeric' | 'number' | spinner        -> 'number'
 *                    'date' | 'datetime'                   -> 'date'
 *                    anything else (text/textarea/...)     -> 'text'
 *   required    <- validations.required === true (non-empty / presence rule)
 *   options     <- ui.options [{ value, label }] (for 'select' only)
 *   listable    <- true when the field appears as a grid column
 *                  (FAUG table skeletons / layer/default.json `_t_*` viewers)
 *   readOnly    <- true for system-managed fields (id, addeddatetime, ...)
 *
 * Entity-level:
 *   searchFields  <- FAUG viewer search field list (searchFld on the wire,
 *                    space-joined); defaults to code/name/description.
 *   allowedFilters<- the legacy @CrudApi(allowedFilters=...) whitelist
 *                    (evidence: MasterEntityRegistry — unitmaster: id,status;
 *                    departmentmaster: status,isclinical,isEmergency,
 *                    isteleconsult,defaultdepartment; companymaster: description,status)
 *
 * Keep this file small on purpose: ~12 representative fields per entity. The
 * transpiler will grow it from the 1,750 legacy schemas.
 * ────────────────────────────────────────────────────────────────────────────
 */

export type FieldType = "text" | "number" | "boolean" | "date" | "select";

export interface FieldOption {
  value: string;
  label: string;
}

export interface FieldMeta {
  /** JSON property name — verbatim legacy field name (never rename). */
  name: string;
  /** Human-readable label for forms/columns. */
  label: string;
  /** Field kind; drives the input widget and the zod conversion. */
  type: FieldType;
  /** Presence constraint (mirrors FAUG validations.required). */
  required?: boolean;
  /** Allowed values for type === "select". */
  options?: FieldOption[];
  /** Show as a grid column in MasterListPage. */
  listable?: boolean;
  /** System-managed field — shown read-only, excluded from edit payloads. */
  readOnly?: boolean;
  /** Optional input placeholder. */
  placeholder?: string;
}

export interface EntityMeta {
  /** Registry name as served by GET /api/v1/masters (e.g. "unitmaster"). */
  name: string;
  /** Display name ("Unit Master"). */
  label: string;
  /** Field model — the schema that drives list/form/validation. */
  fields: FieldMeta[];
  /** Default searchFld (space-joined on the wire). */
  searchFields?: string[];
  /** Legacy filter whitelist (mirrors MasterEntityRegistry). */
  allowedFilters?: string[];
  /** Primary key field name (defaults to "id"). */
  idField?: string;
}

const idField: FieldMeta = {
  name: "id",
  label: "ID",
  type: "number",
  readOnly: true,
  listable: true,
};

const unitIdField: FieldMeta = {
  name: "unitId",
  label: "Unit ID",
  type: "number",
  required: true,
  listable: true,
};

const statusField: FieldMeta = {
  name: "status",
  label: "Status",
  type: "boolean",
  listable: true,
};

const addedDatetimeField: FieldMeta = {
  name: "addeddatetime",
  label: "Added",
  type: "date",
  readOnly: true,
  listable: true,
};

/** ~12 representative fields of M_UNITMASTER (UnitMaster.java, ~90 total). */
const unitMaster: EntityMeta = {
  name: "unitmaster",
  label: "Unit Master",
  idField: "id",
  searchFields: ["code", "name", "description", "contactno", "email"],
  allowedFilters: ["id", "status"],
  fields: [
    idField,
    unitIdField,
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "name", label: "Name", type: "text", required: true, listable: true },
    { name: "description", label: "Description", type: "text", listable: true },
    statusField,
    { name: "addressline1", label: "Address Line 1", type: "text" },
    { name: "city", label: "City", type: "text" },
    { name: "state", label: "State", type: "text" },
    { name: "country", label: "Country", type: "text" },
    { name: "pincode", label: "PIN Code", type: "text" },
    { name: "contactno", label: "Contact No.", type: "text", listable: true },
    { name: "email", label: "Email", type: "text" },
    addedDatetimeField,
  ],
};

/** ~10 representative fields of M_DEPARTMENTMASTER (DepartmentMaster.java). */
const departmentMaster: EntityMeta = {
  name: "departmentmaster",
  label: "Department Master",
  idField: "id",
  searchFields: ["code", "description", "email"],
  allowedFilters: ["status", "isclinical", "isEmergency", "isteleconsult", "defaultdepartment"],
  fields: [
    idField,
    unitIdField,
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "description", label: "Description", type: "text", required: true, listable: true },
    statusField,
    { name: "isclinical", label: "Clinical", type: "boolean" },
    { name: "isIpd", label: "IPD Department", type: "boolean" },
    { name: "isEmergency", label: "Emergency", type: "boolean" },
    { name: "isteleconsult", label: "Teleconsult", type: "boolean" },
    { name: "defaultdepartment", label: "Default", type: "boolean" },
    { name: "email", label: "Email", type: "text" },
    addedDatetimeField,
  ],
};

/** ~11 representative fields of M_COMPANYMASTER (CompanyMaster.java). */
const companyMaster: EntityMeta = {
  name: "companymaster",
  label: "Company Master",
  idField: "id",
  searchFields: ["code", "description", "contactperson", "email"],
  allowedFilters: ["description", "status"],
  fields: [
    idField,
    unitIdField,
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "description", label: "Description", type: "text", required: true, listable: true },
    statusField,
    { name: "companyTypeId", label: "Company Type", type: "number" },
    { name: "totallimit", label: "Total Limit", type: "number" },
    { name: "creditlimit", label: "Credit Limit", type: "number" },
    { name: "address", label: "Address", type: "text" },
    { name: "pincode", label: "PIN Code", type: "text" },
    { name: "contactperson", label: "Contact Person", type: "text", listable: true },
    { name: "email", label: "Email", type: "text" },
  ],
};

export const ENTITY_METADATA: Record<string, EntityMeta> = {
  [unitMaster.name]: unitMaster,
  [departmentMaster.name]: departmentMaster,
  [companyMaster.name]: companyMaster,
};

/** Metadata for a registry entity; falls back to a generic text-only model. */
export function getEntityMeta(name: string | undefined): EntityMeta | null {
  if (!name) {
    return null;
  }
  const known = ENTITY_METADATA[name.toLowerCase()];
  if (known) {
    return known;
  }
  // Unknown entity (registry may grow): generic model keyed by whatever fields
  // the first record exposes — MasterListPage fills it in when data arrives.
  return {
    name: name.toLowerCase(),
    label: name,
    idField: "id",
    searchFields: ["description"],
    fields: [idField],
  };
}

/** Column set for the grid — purely derived from metadata. */
export function listableFields(meta: EntityMeta): FieldMeta[] {
  return meta.fields.filter((f) => f.listable);
}

/** Editable fields for forms/payloads — readOnly/system fields excluded. */
export function editableFields(meta: EntityMeta): FieldMeta[] {
  return meta.fields.filter((f) => !f.readOnly);
}

/** True when the entity has a hand-authored metadata entry. */
export function isKnownEntity(name: string | undefined): boolean {
  return !!name && name.toLowerCase() in ENTITY_METADATA;
}

function humanize(name: string): string {
  return name
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/([A-Z]+)([A-Z][a-z])/g, "$1 $2")
    .replace(/^./, (c) => c.toUpperCase());
}

function inferFieldType(value: unknown): FieldType {
  if (typeof value === "number") return "number";
  if (typeof value === "boolean") return "boolean";
  if (typeof value === "string" && /^\d{4}-\d{2}-\d{2}/.test(value)) return "date";
  return "text";
}

/**
 * Generic metadata derived from a record's own keys — keeps the engine
 * schema-driven for registry entities that have no entityMeta entry yet.
 * A future FAUG transpiler will replace this with real metadata.
 */
export function deriveMetaFromRecord(
  name: string,
  record: Record<string, unknown>,
): EntityMeta {
  const fields: FieldMeta[] = Object.keys(record).map((key) => ({
    name: key,
    label: humanize(key),
    type: inferFieldType(record[key]),
    listable: true,
    readOnly: key === "id" || key === "addeddatetime" || key === "updateddatetime",
  }));
  return {
    name: name.toLowerCase(),
    label: humanize(name),
    idField: "id",
    searchFields: ["code", "description", "name"].filter((f) => f in record),
    fields,
  };
}
