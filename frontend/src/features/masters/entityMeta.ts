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

/** Doctor master (Doctor.java). Unit-scoped. */
const doctor: EntityMeta = {
  name: "doctor",
  label: "Doctor",
  idField: "id",
  searchFields: ["firstName", "lastName", "mobileNo", "emailId", "registrationNumber"],
  allowedFilters: ["status", "doctorTypeId", "specializationId"],
  fields: [
    idField,
    unitIdField,
    { name: "firstName", label: "First Name", type: "text", required: true, listable: true },
    { name: "middleName", label: "Middle Name", type: "text" },
    { name: "lastName", label: "Last Name", type: "text", required: true, listable: true },
    { name: "genderId", label: "Gender ID", type: "number", listable: true },
    { name: "dob", label: "Date of Birth", type: "date" },
    { name: "mobileNo", label: "Mobile No.", type: "text", listable: true },
    { name: "emailId", label: "Email", type: "text", listable: true },
    { name: "doctorTypeId", label: "Doctor Type", type: "number", listable: true },
    { name: "specializationId", label: "Specialization", type: "number", listable: true },
    { name: "registrationNumber", label: "Registration No.", type: "text", listable: true },
    { name: "doctorNo", label: "Doctor No.", type: "text" },
    { name: "education", label: "Education", type: "text" },
    { name: "experience", label: "Experience", type: "text" },
    statusField,
    addedDatetimeField,
  ],
};

/** Doctor Type master (DoctorType.java). Unit-scoped. */
const doctorType: EntityMeta = {
  name: "doctor-type",
  label: "Doctor Type",
  idField: "id",
  searchFields: ["code", "description"],
  allowedFilters: ["status"],
  fields: [
    idField,
    unitIdField,
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "description", label: "Description", type: "text", required: true, listable: true },
    statusField,
    addedDatetimeField,
  ],
};

/** Specialization master (Specialization.java). Unit-scoped. */
const specialization: EntityMeta = {
  name: "specialization",
  label: "Specialization",
  idField: "id",
  searchFields: ["code", "description"],
  allowedFilters: ["status", "isDoctorShow"],
  fields: [
    idField,
    unitIdField,
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "description", label: "Description", type: "text", required: true, listable: true },
    { name: "printOrder", label: "Print Order", type: "number" },
    { name: "ledgerName", label: "Ledger Name", type: "text" },
    { name: "isDoctorShow", label: "Show in Doctor", type: "boolean", listable: true },
    { name: "isDepartmentShow", label: "Show in Department", type: "boolean" },
    { name: "isRecalculateService", label: "Recalculate Service", type: "boolean" },
    statusField,
    addedDatetimeField,
  ],
};

/** Facility Location master (FacilityLocation.java). Unit-scoped. */
const facilityLocation: EntityMeta = {
  name: "facility-location",
  label: "Facility Location",
  idField: "id",
  searchFields: ["code", "name"],
  allowedFilters: ["status", "locationType"],
  fields: [
    idField,
    unitIdField,
    {
      name: "locationType",
      label: "Location Type",
      type: "select",
      required: true,
      listable: true,
      options: [
        { value: "BUILDING", label: "Building" },
        { value: "FLOOR", label: "Floor" },
        { value: "WING", label: "Wing" },
        { value: "ROOM", label: "Room" },
        { value: "CABIN", label: "Cabin" },
        { value: "WARD", label: "Ward" },
        { value: "NURSING_STATION", label: "Nursing Station" },
        { value: "OT_THEATRE", label: "OT Theatre" },
      ],
    },
    { name: "code", label: "Code", type: "text", listable: true },
    { name: "name", label: "Name", type: "text", required: true, listable: true },
    { name: "nameHindi", label: "Name (Hindi)", type: "text" },
    { name: "parentId", label: "Parent Location", type: "number", listable: true },
    { name: "sequenceNo", label: "Sequence No.", type: "number" },
    { name: "isUniqueSequence", label: "Unique Sequence", type: "boolean" },
    { name: "tokenPrefix", label: "Token Prefix", type: "text" },
    { name: "isRoomAmenities", label: "Room Amenities", type: "boolean" },
    statusField,
    addedDatetimeField,
  ],
};

/** Subunit master (Subunit.java). Unit-scoped. */
const subunit: EntityMeta = {
  name: "subunit",
  label: "Subunit",
  idField: "id",
  searchFields: ["code", "description"],
  allowedFilters: ["status"],
  fields: [
    idField,
    unitIdField,
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "description", label: "Description", type: "text", listable: true },
    statusField,
    addedDatetimeField,
  ],
};

/** Geo Country master (GeoCountry.java). Global scope (unit_id NULL). */
const geoCountry: EntityMeta = {
  name: "geo-country",
  label: "Country",
  idField: "id",
  searchFields: ["code", "name"],
  allowedFilters: ["status"],
  fields: [
    idField,
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "name", label: "Name", type: "text", required: true, listable: true },
    { name: "isDefault", label: "Default Country", type: "boolean", listable: true },
    { name: "nationalityName", label: "Nationality", type: "text", listable: true },
    statusField,
    addedDatetimeField,
  ],
};

/** Geo State master (GeoState.java). Global scope (unit_id NULL). */
const geoState: EntityMeta = {
  name: "geo-state",
  label: "State",
  idField: "id",
  searchFields: ["code", "name"],
  allowedFilters: ["status", "countryId"],
  fields: [
    idField,
    { name: "countryId", label: "Country", type: "number", required: true, listable: true },
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "name", label: "Name", type: "text", required: true, listable: true },
    { name: "nameHindi", label: "Name (Hindi)", type: "text" },
    { name: "abdmCode", label: "ABDM Code", type: "text" },
    statusField,
    addedDatetimeField,
  ],
};

/** Geo District master (GeoDistrict.java). Global scope (unit_id NULL). */
const geoDistrict: EntityMeta = {
  name: "geo-district",
  label: "District",
  idField: "id",
  searchFields: ["code", "name"],
  allowedFilters: ["status", "zoneId", "stateId"],
  fields: [
    idField,
    { name: "stateId", label: "State", type: "number", required: true, listable: true },
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "name", label: "Name", type: "text", required: true, listable: true },
    { name: "census2011Code", label: "Census 2011 Code", type: "text" },
    { name: "zoneId", label: "Zone ID", type: "number" },
    { name: "abdmDistrictCode", label: "ABDM District Code", type: "text" },
    statusField,
    addedDatetimeField,
  ],
};

/** Geo City master (GeoCity.java). Global scope (unit_id NULL). */
const geoCity: EntityMeta = {
  name: "geo-city",
  label: "City",
  idField: "id",
  searchFields: ["code", "name"],
  allowedFilters: ["status", "districtId", "stateId"],
  fields: [
    idField,
    { name: "stateId", label: "State", type: "number", required: true, listable: true },
    { name: "districtId", label: "District", type: "number", listable: true },
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "name", label: "Name", type: "text", required: true, listable: true },
    { name: "censusCode", label: "Census Code", type: "text" },
    statusField,
    addedDatetimeField,
  ],
};

/** Geo Area master (GeoArea.java). Global scope (unit_id NULL). */
const geoArea: EntityMeta = {
  name: "geo-area",
  label: "Area",
  idField: "id",
  searchFields: ["code", "name"],
  allowedFilters: ["status", "cityId"],
  fields: [
    idField,
    { name: "cityId", label: "City", type: "number", required: true, listable: true },
    { name: "code", label: "Code", type: "text", required: true, listable: true },
    { name: "name", label: "Name", type: "text", required: true, listable: true },
    statusField,
    addedDatetimeField,
  ],
};

export const ENTITY_METADATA: Record<string, EntityMeta> = {
  [unitMaster.name]: unitMaster,
  [departmentMaster.name]: departmentMaster,
  [companyMaster.name]: companyMaster,
  [doctor.name]: doctor,
  [doctorType.name]: doctorType,
  [specialization.name]: specialization,
  [facilityLocation.name]: facilityLocation,
  [subunit.name]: subunit,
  [geoCountry.name]: geoCountry,
  [geoState.name]: geoState,
  [geoDistrict.name]: geoDistrict,
  [geoCity.name]: geoCity,
  [geoArea.name]: geoArea,
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
