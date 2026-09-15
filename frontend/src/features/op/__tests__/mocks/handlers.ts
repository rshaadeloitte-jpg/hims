import { http, HttpResponse } from "msw";
import type {
  AbhaEnrollment,
  Appointment,
  AppointmentListResponse,
  AppointmentSlot,
  Department,
  Doctor,
  Patient,
  PatientSearchHit,
  QueueToken,
} from "../../api/types";
import type { ApiError } from "../../../../api/client";

/**
 * MSW handlers for the OP module — one file per downstream service so the
 * failing-path branch is trivial to track. Tests wire these via:
 *
 *   import { server } from "../mocks/server";
 *   beforeAll(() => server.listen());
 *   afterAll(() => server.close());
 *
 * The base URL is the same "/api" that the dev proxy uses (vite.config.ts
 * proxies /api -> patient :8090 / appointment :8091 / queue :8089, depending
 * on the path). For the test environment we don't actually proxy; MSW
 * intercepts the requests and returns canned responses.
 */

const ok = (body: unknown, status = 200) =>
  HttpResponse.json(body as never, { status });

const err = (status: number, code: string, message: string) =>
  ok({ status: "ERROR", errorCode: code, message }, status);

/* ── patient-service ────────────────────────────────── */

const samplePatient: Patient = {
  id: 7001,
  mrn: "MRN-7001",
  isTempMrn: false,
  firstName: "Asha",
  lastName: "Verma",
  fatherName: "Rajesh",
  dateOfBirth: "1992-04-12",
  age: 33,
  sex: "F",
  mobile: "+91 98765 43210",
  abha: "12-3456-7890-1234",
  status: true,
  sponsors: [
    { sponsorId: 11, sponsorName: "NHNC", schemeName: "NHNC-General", isPrimary: true },
    { sponsorId: 12, sponsorName: "RSBY", schemeName: "RSBY" },
  ],
};

const sampleSearch: PatientSearchHit[] = [
  { id: 7001, mrn: "MRN-7001", firstName: "Asha", lastName: "Verma", sex: "F", age: 33, mobile: "+91 98765 43210" },
  { id: 7002, mrn: "MRN-7002", firstName: "Ravi", lastName: "Kumar", sex: "M", age: 41, abha: "11-1111-2222-3333" },
];

/* ── appointment-service ─────────────────────────────── */

const sampleDepartments: Department[] = [
  { id: 1, name: "General Medicine", isOpd: true, isActive: true },
  { id: 2, name: "Pediatrics", isOpd: true, isActive: true },
  { id: 3, name: "Dermatology", isOpd: true, isActive: true },
];

const sampleDoctors: Doctor[] = [
  { id: 100, name: "Dr. Anjali Rao", departmentId: 1, departmentName: "General Medicine", isActive: true },
  { id: 101, name: "Dr. Vikram Singh", departmentId: 1, departmentName: "General Medicine", isActive: true },
  { id: 200, name: "Dr. Maya Iyer", departmentId: 2, departmentName: "Pediatrics", isActive: true },
];

const sampleSlots: AppointmentSlot[] = [
  { slotId: 1, doctorId: 100, doctorName: "Dr. Anjali Rao", startAt: "2024-08-15T09:00:00", endAt: "2024-08-15T09:30:00", capacity: 1, booked: 0, available: true },
  { slotId: 2, doctorId: 100, doctorName: "Dr. Anjali Rao", startAt: "2024-08-15T09:30:00", endAt: "2024-08-15T10:00:00", capacity: 1, booked: 1, available: false },
  { slotId: 3, doctorId: 100, doctorName: "Dr. Anjali Rao", startAt: "2024-08-15T10:00:00", endAt: "2024-08-15T10:30:00", capacity: 1, booked: 0, available: true },
  { slotId: 4, doctorId: 100, doctorName: "Dr. Anjali Rao", startAt: "2024-08-15T14:00:00", endAt: "2024-08-15T14:30:00", capacity: 1, booked: 0, available: true },
  { slotId: 5, doctorId: 100, doctorName: "Dr. Anjali Rao", startAt: "2024-08-15T18:00:00", endAt: "2024-08-15T18:30:00", capacity: 1, booked: 0, available: true },
];

const sampleAppointments: Appointment[] = [
  {
    id: 9001, patientId: 7001, patientName: "Asha Verma", patientMrn: "MRN-7001",
    doctorId: 100, doctorName: "Dr. Anjali Rao", departmentId: 1, departmentName: "General Medicine",
    startAt: "2024-08-15T09:00:00", endAt: "2024-08-15T09:30:00",
    status: 1, source: "WALK_IN", tokenNumber: "OPD-1",
  },
  {
    id: 9002, patientId: 7002, patientName: "Ravi Kumar", patientMrn: "MRN-7002",
    doctorId: 100, doctorName: "Dr. Anjali Rao", departmentId: 1, departmentName: "General Medicine",
    startAt: "2024-08-15T10:00:00", endAt: "2024-08-15T10:30:00",
    status: 2, source: "ONLINE",
  },
];

/* ── queue-service ──────────────────────────────────── */

const sampleToken: QueueToken = {
  id: 1, tokenNumber: "OPD-1", patientId: 7001, patientName: "Asha Verma",
  counterId: 1, counterName: "Counter 1", priority: "NORMAL", status: "WAITING",
  issuedAt: "2024-08-15T08:55:00", position: 1,
};

/* ── abha ─────────────────────────────────────────── */

const sampleAbha: AbhaEnrollment = {
  id: 555, patientId: 7001, status: "PENDING_OTP",
  createdAt: "2024-08-15T09:00:00",
};

export const handlers = [
  /* ── patient-service ── */
  http.get("/api/v1/patients/search", ({ request }) => {
    const url = new URL(request.url);
    const q = url.searchParams.get("q") ?? "";
    if (q.length < 2) return ok([]);
    if (q.toLowerCase().includes("missing")) return ok([]);
    return ok(sampleSearch);
  }),
  http.get("/api/v1/patients/:id", ({ params }) => {
    const id = Number(params.id);
    if (id === 7001) return ok(samplePatient);
    if (id === 9099) return err(404, "NOT_FOUND", `Patient ${id} not found`);
    return err(404, "NOT_FOUND", `Patient ${id} not found`);
  }),
  http.post("/api/v1/patients/register", async ({ request }) => {
    const body = (await request.json()) as Partial<Patient>;
    if (!body.firstName || !body.lastName) {
      return err(400, "BEXA003", "firstName and lastName are required");
    }
    return ok({ ...samplePatient, ...body, id: 7999, mrn: "MRN-7999", isTempMrn: false }, 201);
  }),
  http.get("/api/v1/patients/:id/sponsors", () => ok(samplePatient.sponsors ?? [])),

  /* ── appointment-service ── */
  http.get("/api/v1/departments", () => ok(sampleDepartments)),
  http.get("/api/v1/doctors/schedules", ({ request }) => {
    const url = new URL(request.url);
    const deptId = Number(url.searchParams.get("departmentId") ?? "0");
    return ok(sampleDoctors.filter((d) => d.departmentId === deptId));
  }),
  http.get("/api/v1/doctors/:id/slots", ({ params, request }) => {
    const url = new URL(request.url);
    const date = url.searchParams.get("date") ?? "";
    if (!date) return ok([]);
    return ok(sampleSlots.filter((s) => s.doctorId === Number(params.id)));
  }),
  http.get("/api/v1/appointments", ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get("status") ?? "all";
    let items = [...sampleAppointments];
    if (status === "pending") items = items.filter((a) => a.status === 1);
    if (status === "checkedIn") items = items.filter((a) => a.status === 2);
    if (status === "noShow") items = items.filter((a) => a.status === 6);
    if (status === "walkIn") items = items.filter((a) => a.source === "WALK_IN");
    if (status === "emergency") items = items.filter((a) => a.source === "EMERGENCY");
    const response: AppointmentListResponse = {
      items, page: 0, size: items.length, total: items.length, hasMore: false,
    };
    return ok(response);
  }),
  http.post("/api/v1/appointments", async ({ request }) => {
    const body = (await request.json()) as { patientId: number; doctorId: number; startAt: string; endAt: string };
    if (!body.patientId || !body.doctorId || !body.startAt || !body.endAt) {
      return err(400, "BEXA003", "patientId, doctorId, startAt, endAt are required");
    }
    return ok({
      id: 9999, patientId: body.patientId, doctorId: body.doctorId, departmentId: 1,
      startAt: body.startAt, endAt: body.endAt, status: 1, source: "WALK_IN",
    } as Appointment, 201);
  }),
  http.put("/api/v1/appointments/:id/cancel", async ({ params, request }) => {
    const body = (await request.json()) as { reason?: string };
    if (!body.reason || body.reason.trim().length === 0) {
      return err(400, "BEXA003", "Cancellation reason is required");
    }
    return ok({ ...sampleAppointments[0], id: Number(params.id), status: 4 });
  }),
  http.put("/api/v1/appointments/:id/checkin", ({ params }) =>
    ok({ ...sampleAppointments[0], id: Number(params.id), status: 2 }),
  ),
  http.put("/api/v1/appointments/:id/noshow", ({ params }) =>
    ok({ ...sampleAppointments[0], id: Number(params.id), status: 6 }),
  ),

  /* ── queue-service ── */
  http.get("/api/v1/queue/tokens", () => ok([sampleToken])),
  http.post("/api/v1/queue/tokens", async ({ request }) => {
    const body = (await request.json()) as { patientId: number };
    if (!body.patientId) return err(400, "BEXA003", "patientId is required");
    return ok({ ...sampleToken, patientId: body.patientId }, 201);
  }),
  http.post("/api/v1/queue/counters/:id/call-next", () => ok(sampleToken)),

  /* ── ABHA / ABDM (the in-tree mock for op only) ── */
  http.get("/api/v1/abha/:patientId", ({ params }) => {
    if (Number(params.patientId) === 7001) {
      return ok({ ...sampleAbha, status: "LINKED" as const, abhaNumber: "12-3456-7890-1234", linkedAt: "2024-08-15T09:10:00" });
    }
    return err(404, "NOT_FOUND", `No ABHA enrolment for patient ${params.patientId}`);
  }),
  http.post("/api/v1/abha/enroll", async ({ request }) => {
    const body = (await request.json()) as { patientId: number };
    return ok({ ...sampleAbha, patientId: body.patientId ?? sampleAbha.patientId, status: "PENDING_OTP" as const }, 201);
  }),
  http.post("/api/v1/abha/enroll/:id/verify-otp", async ({ params, request }) => {
    const body = (await request.json()) as { otp?: string };
    if (!body.otp || body.otp.length < 6) {
      return err(400, "BEXA003", "OTP must be 6 digits");
    }
    return ok({ ...sampleAbha, id: Number(params.id), status: "OTP_VERIFIED" as const });
  }),
  http.post("/api/v1/abha/scan", async ({ request }) => {
    const body = (await request.json()) as { qrPayload?: string };
    if (!body.qrPayload || body.qrPayload.trim().length === 0) {
      return err(400, "BEXA003", "qrPayload is required");
    }
    return ok({ ...sampleAbha, status: "OTP_VERIFIED" as const, abhaAddress: "12-3456-7890-1234@sbx" });
  }),
  http.post("/api/v1/abha/:patientId/confirm-link", async ({ params, request }) => {
    const body = (await request.json()) as { abhaAddress?: string };
    if (!body.abhaAddress) {
      return err(400, "BEXA003", "abhaAddress is required");
    }
    return ok({ ...sampleAbha, patientId: Number(params.patientId), status: "LINKED" as const, linkedAt: new Date().toISOString() });
  }),
];

/* Re-exported sample objects so individual tests can assert on the same data. */
export const opSamples = {
  patient: samplePatient,
  search: sampleSearch,
  departments: sampleDepartments,
  doctors: sampleDoctors,
  slots: sampleSlots,
  appointments: sampleAppointments,
  token: sampleToken,
  abha: sampleAbha,
};
// suppress the ApiError re-export warning (test-only)
export type _ApiErrorUsed = ApiError;
