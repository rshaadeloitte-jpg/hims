import type { ListQuery, MasterPayload, MasterRecord } from "./types";

/**
 * Thin fetch wrapper for the reference-service REST API (Spring Boot :8081).
 *
 * - Base URL from VITE_API_BASE (default "/api"; dev server proxies it to :8081).
 * - DEV-ONLY Basic auth header (red-team F-8): the reference-service `dev`
 *   profile accepts devops/devops-dev-only. The header is ONLY attached in
 *   dev-mode builds (import.meta.env.DEV) and can be turned off with
 *   VITE_ENABLE_DEV_BASIC=false. Production builds never send it and the
 *   default credentials are compile-time eliminated from the bundle
 *   (Vite statically folds import.meta.env.DEV to false in prod).
 * - Unified error envelope: non-2xx responses are parsed into {@link ApiError}.
 *   The reference-service sends { status, errorCode, message }; the parser also
 *   tolerates the generic { code, message, details } shape.
 * - Write surface (POST create / PUT update, MIG-010 W1) targets the same
 *   routes as the read API; a successful create returns 201 with the record.
 */

const API_BASE: string = import.meta.env.VITE_API_BASE ?? "/api";

/**
 * DEV-ONLY Basic auth (IMP-DEC-019 / red-team F-8).
 * `import.meta.env.DEV` is statically replaced by Vite (true in dev, false in
 * production builds) so the entire credential branch — including the default
 * devops/devops-dev-only strings — is constant-folded out of prod bundles.
 */
const DEV_BASIC_ENABLED: boolean =
  import.meta.env.DEV && import.meta.env.VITE_ENABLE_DEV_BASIC !== "false";

const DEV_USERNAME: string = import.meta.env.DEV
  ? (import.meta.env.VITE_API_USERNAME ?? "devops")
  : "";
const DEV_PASSWORD: string = import.meta.env.DEV
  ? (import.meta.env.VITE_API_PASSWORD ?? "devops-dev-only")
  : "";

/** Error envelope thrown by the client for any non-2xx / network failure. */
export class ApiError extends Error {
  /** HTTP status (0 = the request never reached the server). */
  readonly status: number;
  /** Machine-readable code, e.g. BEXA003, UNKNOWN_ENTITY, NOT_FOUND. */
  readonly code: string;
  /** Optional structured payload (backend `details` / `status` field). */
  readonly details?: unknown;

  constructor(status: number, code: string, message: string, details?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details;
  }

  /**
   * Parses a non-2xx response body into an ApiError. The reference-service
   * envelope is `{ status, errorCode, message }` (ApiError record); the generic
   * `{ code, message, details }` shape is also accepted.
   */
  static fromBody(status: number, body: unknown): ApiError {
    const b = (body ?? {}) as Record<string, unknown>;
    const code =
      typeof b.errorCode === "string"
        ? b.errorCode
        : typeof b.code === "string"
          ? b.code
          : `HTTP_${status}`;
    const message =
      typeof b.message === "string" && b.message.length > 0
        ? b.message
        : `Request failed with status ${status}`;
    const details = b.details !== undefined ? b.details : typeof b.status === "string" ? b.status : undefined;
    return new ApiError(status, code, message, details);
  }
}

/**
 * Legacy pagination semantics (GenericServiceImpl / GenericQuery.paginate):
 * the server computes `offset = (page / count) * count` (integer division),
 * so the client sends `page = pageIndex * pageSize` and the effective offset
 * equals `pageIndex * pageSize` exactly.
 *
 * NOTE: the legacy API returns a plain JSON array — there is no total-count
 * header or envelope. "Has next page" is therefore derived client-side from a
 * full page: when the server returns exactly `pageSize` rows there may be more
 * (over-enables only on exact multiples of the page size; the next page then
 * simply shows the remaining/empty rows).
 */
export function legacyWirePage(pageIndex: number, pageSize: number): number {
  return pageIndex * pageSize;
}

function basicAuthHeader(): string | undefined {
  if (!DEV_BASIC_ENABLED) return undefined;
  return `Basic ${btoa(`${DEV_USERNAME}:${DEV_PASSWORD}`)}`;
}

function buildUrl(path: string, query?: object): string {
  const base = API_BASE.endsWith("/") ? API_BASE.slice(0, -1) : API_BASE;
  const url = new URL(`${base}${path}`, window.location.origin);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  query?: object;
  body?: unknown;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", query, body } = options;

  let res: Response;
  try {
    const authHeader = basicAuthHeader();
    res = await fetch(buildUrl(path, query), {
      method,
      headers: {
        Accept: "application/json",
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(authHeader ? { Authorization: authHeader } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (err) {
    throw new ApiError(
      0,
      "NETWORK_ERROR",
      err instanceof Error ? err.message : "Network request failed",
    );
  }

  if (!res.ok) {
    let payload: unknown = null;
    try {
      payload = await res.json();
    } catch {
      // non-JSON error body (e.g. plain 401) — fromBody falls back to status
    }
    throw ApiError.fromBody(res.status, payload);
  }

  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

/** Endpoints of the reference-service master API. */
export const mastersApi = {
  /** GET /api/v1/masters — supported master entity registry names. */
  listEntities: (): Promise<string[]> => request<string[]>("/masters"),

  /** GET /api/v1/masters/{entity} — crudgetAll parity, unit-scoped list. */
  listScoped: (entity: string, query: ListQuery): Promise<MasterRecord[]> =>
    request<MasterRecord[]>(`/masters/${encodeURIComponent(entity)}`, { query }),

  /** GET /api/v1/masters/{entity}/all — crudgetAllAll parity, unscoped list. */
  listAll: (entity: string, query: ListQuery): Promise<MasterRecord[]> =>
    request<MasterRecord[]>(`/masters/${encodeURIComponent(entity)}/all`, { query }),

  /**
   * GET /api/v1/masters/{entity}/{id} — single record (crudget parity).
   * unitId is REQUIRED for unit-scoped entities (departmentmaster/
   * companymaster — the backend scopes the lookup: `AND t.unitId = :unitId`,
   * see JpaMasterReadRepository.findByIdScoped); Unitmaster is id-only.
   */
  getById: (entity: string, id: number | string, unitId?: number): Promise<MasterRecord> =>
    request<MasterRecord>(`/masters/${encodeURIComponent(entity)}/${id}`, {
      query: unitId ? { unitId } : undefined,
    }),

  /**
   * POST /api/v1/masters/{entity} — create (crudadd parity; MIG-010 W1,
   * returns 201 + the persisted record; emits a MasterDataChanged outbox event).
   * unitId is REQUIRED for unit-scoped entities if the payload lacks it
   * (server: scope = unitId param ?? payload.unitId).
   */
  create: (entity: string, payload: MasterPayload, unitId?: number): Promise<MasterRecord> =>
    request<MasterRecord>(`/masters/${encodeURIComponent(entity)}`, {
      method: "POST",
      query: unitId ? { unitId } : undefined,
      body: payload,
    }),

  /**
   * PUT /api/v1/masters/{entity}/{id} — update (crudupdate parity; MIG-010 W1).
   * unitId is REQUIRED for unit-scoped entities (otherwise the scoped lookup
   * fails with BEXA031 "Not Authorized for editing !"). Server default
   * nonNull=true: null fields are left untouched, id/unitId are never
   * overwritten — matches the form payload (only dirty fields are sent).
   */
  update: (
    entity: string,
    id: number | string,
    payload: MasterPayload,
    unitId?: number,
  ): Promise<MasterRecord> =>
    request<MasterRecord>(`/masters/${encodeURIComponent(entity)}/${id}`, {
      method: "PUT",
      query: unitId ? { unitId } : undefined,
      body: payload,
    }),
};

/** Forms engine API (M40) — versioned JSON-schema store + submit. */
export const formsApi = {
  list: (): Promise<import("../features/forms/types").FormDefinition[]> =>
    request<import("../features/forms/types").FormDefinition[]>("/forms"),
  getSchema: (id: number): Promise<import("../features/forms/types").FormSchemaResponse> =>
    request<import("../features/forms/types").FormSchemaResponse>(`/forms/${id}/schema`),
  submit: (formId: number, data: Record<string, unknown>, patientId?: number, encounterId?: number) =>
    request<import("../features/forms/types").FormSubmission>(`/forms/${formId}/submit`, {
      method: "POST",
      query: { patientId, encounterId },
      body: data,
    }),
  listSubmissions: (formId: number): Promise<import("../features/forms/types").FormSubmission[]> =>
    request<import("../features/forms/types").FormSubmission[]>(`/forms/${formId}/submissions`),
  specialties: (): Promise<string[]> =>
    request<string[]>("/forms/specialties"),
  listBySpecialty: (specialty: string): Promise<import("../features/forms/types").FormDefinition[]> =>
    request<import("../features/forms/types").FormDefinition[]>(`/forms?specialty=${encodeURIComponent(specialty)}`),
  listByPatient: (patientId: number): Promise<import("../features/forms/types").FormSubmission[]> =>
    request<import("../features/forms/types").FormSubmission[]>(`/forms/submissions/patient/${patientId}`),
  listByEncounter: (encounterId: number): Promise<import("../features/forms/types").FormSubmission[]> =>
    request<import("../features/forms/types").FormSubmission[]>(`/forms/submissions/encounter/${encounterId}`),
  deleteSubmission: (formId: number, submissionId: number) =>
    request<import("../features/forms/types").FormSubmission>(`/forms/${formId}/submissions/${submissionId}`, { method: "DELETE" }),
  create: (payload: {
    code: string;
    name: string;
    specialty: string;
    description?: string;
    version: number;
    schema: Record<string, unknown>;
  }) =>
    request<import("../features/forms/types").FormDefinition>("/forms", {
      method: "POST",
      body: payload,
    }),
  update: (
    id: number,
    payload: {
      code: string;
      name: string;
      specialty: string;
      description?: string;
      version: number;
      schema: Record<string, unknown>;
    },
  ) =>
    request<import("../features/forms/types").FormDefinition>(`/forms/${id}`, {
      method: "PUT",
      body: payload,
    }),
};

export type { ListQuery, MasterPayload, MasterRecord };
