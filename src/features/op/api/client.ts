import type {
  AbhaEnrollment,
  AbhaStatus,
  Appointment,
  AppointmentListQuery,
  AppointmentListResponse,
  AppointmentSlot,
  AppointmentSource,
  AppointmentStatus,
  Department,
  Doctor,
  Patient,
  PatientSearchHit,
  PatientSearchQuery,
  QueuePriority,
  QueueToken,
  QueueTokenCreate,
} from "./types";
import { ApiError } from "../../../api/client";

/**
 * OP module API client — typed wrappers for the three downstream services
 * (patient :8090, appointment :8091, queue :8089).
 *
 * The base /api path is the same for all three (the dev proxy + the MIG-005
 * gateway route them by host:port). The `VITE_*_BASE` env vars let per-service
 * overrides land in dev/staging/prod without code changes.
 *
 * Auth: same DEV-ONLY Basic header as the masters client (see api/client.ts).
 * The fetch wrapper emits the X-Request-ID correlation id and normalizes
 * non-2xx into {@link ApiError} so screens can use the existing pattern.
 */

const PATIENT_BASE: string = import.meta.env.VITE_PATIENT_API_BASE ?? "/api";
const APPT_BASE: string = import.meta.env.VITE_APPT_API_BASE ?? "/api";
const QUEUE_BASE: string = import.meta.env.VITE_QUEUE_API_BASE ?? "/api";

const DEV_BASIC_ENABLED: boolean =
  import.meta.env.DEV && import.meta.env.VITE_ENABLE_DEV_BASIC !== "false";
const DEV_USERNAME: string = import.meta.env.DEV
  ? (import.meta.env.VITE_API_USERNAME ?? "devops")
  : "";
const DEV_PASSWORD: string = import.meta.env.DEV
  ? (import.meta.env.VITE_API_PASSWORD ?? "devops-dev-only")
  : "";

function authHeader(): string | undefined {
  if (!DEV_BASIC_ENABLED) return undefined;
  return `Basic ${btoa(`${DEV_USERNAME}:${DEV_PASSWORD}`)}`;
}

function buildUrl(base: string, path: string, query?: Record<string, unknown>): string {
  const root = base.endsWith("/") ? base.slice(0, -1) : base;
  const url = new URL(`${root}${path}`, window.location.origin);
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v === undefined || v === null || v === "") continue;
      url.searchParams.set(k, String(v));
    }
  }
  return url.toString();
}

async function request<T>(
  base: string,
  path: string,
  options: { method?: "GET" | "POST" | "PUT" | "DELETE"; query?: Record<string, unknown>; body?: unknown } = {},
): Promise<T> {
  const { method = "GET", query, body } = options;
  const auth = authHeader();
  let res: Response;
  try {
    res = await fetch(buildUrl(base, path, query), {
      method,
      headers: {
        Accept: "application/json",
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(auth ? { Authorization: auth } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (err) {
    throw new ApiError(0, "NETWORK_ERROR", err instanceof Error ? err.message : "Network request failed");
  }
  if (!res.ok) {
    let payload: unknown = null;
    try {
      payload = await res.json();
    } catch {
      /* non-JSON body */
    }
    throw ApiError.fromBody(res.status, payload);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

function toPatientQuery(q: PatientSearchQuery): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  if (q.q) out.q = q.q;
  if (q.mrn) out.mrn = q.mrn;
  if (q.mobile) out.mobile = q.mobile;
  if (q.abha) out.abha = q.abha;
  if (q.limit) out.limit = q.limit;
  return out;
}

function toAppointmentListQuery(q: AppointmentListQuery): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  if (q.status !== undefined) out.status = String(q.status);
  if (q.doctorId !== undefined) out.doctorId = q.doctorId;
  if (q.departmentId !== undefined) out.departmentId = q.departmentId;
  if (q.patientId !== undefined) out.patientId = q.patientId;
  if (q.dateFrom) out.dateFrom = q.dateFrom;
  if (q.dateTo) out.dateTo = q.dateTo;
  if (q.page !== undefined) out.page = q.page;
  if (q.size !== undefined) out.size = q.size;
  return out;
}

/** patient-service wrappers (:8090). */
export const patientApi = {
  /** GET /api/v1/patients/search — multi-field lookup (name / MRN / mobile / ABHA). */
  search: (q: PatientSearchQuery) =>
    request<PatientSearchHit[]>(PATIENT_BASE, "/v1/patients/search", { query: toPatientQuery(q) }),

  /** GET /api/v1/patients/{id} — full record (MIG-030). */
  get: (id: number) => request<Patient>(PATIENT_BASE, `/v1/patients/${id}`),

  /** POST /api/v1/patients/register — walk-in registration (BR-PAT-001 MRN gen). */
  register: (p: Omit<Patient, "id" | "mrn" | "createdAt" | "updatedAt">) =>
    request<Patient>(PATIENT_BASE, "/v1/patients/register", { method: "POST", body: p }),

  /** PATCH-style update for demographic fields. */
  update: (id: number, patch: Partial<Patient>) =>
    request<Patient>(PATIENT_BASE, `/v1/patients/${id}`, { method: "PUT", body: patch }),

  /** GET /api/v1/patients/{id}/sponsors — sponsor/scheme list. */
  sponsors: (id: number) =>
    request<Patient["sponsors"]>(PATIENT_BASE, `/v1/patients/${id}/sponsors`),
};

/** appointment-service wrappers (:8091). */
export const appointmentApi = {
  /** GET /api/v1/appointments — list with filters + legacy pagination. */
  list: (q: AppointmentListQuery) =>
    request<AppointmentListResponse>(APPT_BASE, "/v1/appointments", { query: toAppointmentListQuery(q) }),

  /** GET /api/v1/appointments/{id} — one appointment. */
  get: (id: number) => request<Appointment>(APPT_BASE, `/v1/appointments/${id}`),

  /** POST /api/v1/appointments — create (BR-APT-001 unique-slot). */
  create: (body: {
    patientId: number;
    doctorId: number;
    departmentId: number;
    cabinId?: number;
    startAt: string;
    endAt: string;
    source?: AppointmentSource;
    notes?: string;
  }) => request<Appointment>(APPT_BASE, "/v1/appointments", { method: "POST", body }),

  /** PUT /api/v1/appointments/{id}/cancel — terminal 4. */
  cancel: (id: number, reason: string) =>
    request<Appointment>(APPT_BASE, `/v1/appointments/${id}/cancel`, { method: "PUT", body: { reason } }),

  /** PUT /api/v1/appointments/{id}/noshow — terminal 6. */
  noShow: (id: number) =>
    request<Appointment>(APPT_BASE, `/v1/appointments/${id}/noshow`, { method: "PUT" }),

  /** PUT /api/v1/appointments/{id}/checkin — BOOKED -> CHECKED_IN. */
  checkIn: (id: number) =>
    request<Appointment>(APPT_BASE, `/v1/appointments/${id}/checkin`, { method: "PUT" }),

  /** PUT /api/v1/appointments/{id}/complete — CHECKED_IN -> COMPLETED. */
  complete: (id: number) =>
    request<Appointment>(APPT_BASE, `/v1/appointments/${id}/complete`, { method: "PUT" }),

  /** GET /api/v1/doctors/{id}/slots?date=YYYY-MM-DD — slot picker cascade. */
  slots: (doctorId: number, date: string) =>
    request<AppointmentSlot[]>(APPT_BASE, `/v1/doctors/${doctorId}/slots`, { query: { date } }),

  /** GET /api/v1/doctors/schedules — weekly schedule master. */
  doctorSchedules: (departmentId?: number) =>
    request<Doctor[]>(APPT_BASE, "/v1/doctors/schedules", { query: { departmentId } }),

  /** GET /api/v1/departments — for the Department -> Doctor cascade. */
  departments: () => request<Department[]>(APPT_BASE, "/v1/departments"),
};

/** queue-service wrappers (:8089). */
export const queueApi = {
  /** GET /api/v1/queue/tokens — list today's tokens (optional counterId). */
  today: (counterId?: number) =>
    request<QueueToken[]>(QUEUE_BASE, "/v1/queue/tokens", { query: counterId ? { counterId } : undefined }),

  /** POST /api/v1/queue/tokens — issue token (daily-reset per counter). */
  issue: (body: QueueTokenCreate) =>
    request<QueueToken>(QUEUE_BASE, "/v1/queue/tokens", { method: "POST", body }),

  /** POST /api/v1/queue/counters/{id}/call-next — counter calls next. */
  callNext: (counterId: number) =>
    request<QueueToken>(QUEUE_BASE, `/v1/queue/counters/${counterId}/call-next`, { method: "POST" }),

  /** POST /api/v1/queue/tokens/{id}/complete — token completed. */
  complete: (id: number) =>
    request<QueueToken>(QUEUE_BASE, `/v1/queue/tokens/${id}/complete`, { method: "POST" }),

  /** POST /api/v1/queue/tokens/{id}/transfer — transfer to another counter/doctor. */
  transfer: (id: number, toCounterId: number) =>
    request<QueueToken>(QUEUE_BASE, `/v1/queue/tokens/${id}/transfer`, {
      method: "POST",
      body: { toCounterId },
    }),
};

/** interop-service ABHA bridge — minimal contract for the OP screen. */
export const abhaApi = {
  /** POST /api/v1/abha/enroll — start Aadhaar/Mobile OTP ABHA flow. */
  startEnrollment: (body: { patientId: number; aadhaar?: string; mobile?: string }) =>
    request<AbhaEnrollment>("/api", "/v1/abha/enroll", { method: "POST", body }),

  /** POST /api/v1/abha/verify-otp — submit OTP, get PENDING->LINKED. */
  verifyOtp: (enrollmentId: number, otp: string) =>
    request<AbhaEnrollment>("/api", `/v1/abha/enroll/${enrollmentId}/verify-otp`, {
      method: "POST",
      body: { otp },
    }),

  /** POST /api/v1/abha/scan — submit scanned QR payload (PHR pull). */
  scan: (qrPayload: string) =>
    request<AbhaEnrollment>("/api", "/v1/abha/scan", { method: "POST", body: { qrPayload } }),

  /** GET /api/v1/abha/{patientId} — current ABHA status for a patient. */
  getForPatient: (patientId: number) =>
    request<AbhaEnrollment>("/api", `/v1/abha/${patientId}`),

  /** POST /api/v1/abha/{patientId}/confirm-link — confirm ABHA address linkage. */
  confirmLink: (patientId: number, abhaAddress: string) =>
    request<AbhaEnrollment>("/api", `/v1/abha/${patientId}/confirm-link`, {
      method: "POST",
      body: { abhaAddress },
    }),
};

export type {
  AbhaEnrollment,
  AbhaStatus,
  Appointment,
  AppointmentListQuery,
  AppointmentListResponse,
  AppointmentSlot,
  AppointmentSource,
  AppointmentStatus,
  Department,
  Doctor,
  Patient,
  PatientSearchHit,
  PatientSearchQuery,
  QueuePriority,
  QueueToken,
  QueueTokenCreate,
};
