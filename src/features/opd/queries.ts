import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M02 hooks for the OPD/Consultation flows. All requests hit
 * appointment-service (Spring Boot :8091) via the Vite proxy or the
 * production gateway (VITE_APPT_API_BASE override).
 *
 * The backend is intentionally tolerant of partial JSON bodies (legacy
 * PatientRegApptServiceImpl:3362 parity) — the controller reads each field
 * via AppointmentController.longOf/strOf, so a hook that omits nullable
 * fields still matches the wire contract.
 */

const APPT_BASE: string = import.meta.env.VITE_APPT_API_BASE ?? "/api";

async function apptJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  return res.json();
}

async function apptText(url: string, options?: RequestInit): Promise<string> {
  const res = await fetch(url, {
    headers: { "X-Request-ID": crypto.randomUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  return res.text();
}

// ---- Types -----------------------------------------------------------------

export interface FollowUpVisit {
  id: number;
  parentAppointmentId?: number;
  parentEncounterId?: number;
  patientId: number;
  doctorId?: number;
  departmentId?: number;
  clinicId?: number;
  followUpDate: string;
  slotStart?: string;
  slotEnd?: string;
  scheduleId?: number;
  tokenNo?: number;
  cabin?: string;
  recurrenceRule?: string;
  recurrenceEndDate?: string;
  parentFollowUpId?: number;
  followUpStatus: 1 | 2 | 3; // 1=SCHEDULED 2=COMPLETED 3=CANCELLED
  isFromPatientPortal?: boolean;
  patientPortalId?: number;
  patientPortalUnitId?: number;
  portalPatientId?: number;
  isCancelled?: boolean;
  cancelledBy?: number;
  cancelledDate?: string;
  status?: boolean;
  synchronizedd?: boolean;
}

export interface TelemedicineRoom {
  id: number;
  roomId: string;
  videoLink?: string;
  roomStatus: 1 | 2 | 3; // 1=CREATED 2=ACTIVE 3=ENDED
  appointmentId: number;
  status?: boolean;
  startedAt?: string;
  endedAt?: string;
  expiryTimestamp?: string;
}

export interface QuestionnaireRecord {
  id: number;
  appointmentId: number;
  encounterId?: number;
  patientId?: number;
  doctorId?: number;
  module: string;
  moduleType?: string;
  answers: string; // JSON-encoded [{questionId, answerIds, answerText}]
  clinicianReviewFlag?: boolean;
  status?: boolean;
  reviewedBy?: number;
  reviewedDate?: string;
}

export interface OpdCardData {
  appointmentId: number;
  patientId: number;
  patientName: string;
  doctorName: string;
  departmentName: string;
  apptDate: string;
  tokenNo: number;
  opdNo: string;
  visitType: string;
  unitName?: string;
  age?: string;
  gender?: string;
  abhaAddress?: string;
  // Anything else the backend returns is preserved.
  [k: string]: unknown;
}

export interface FollowUpListState {
  appointmentId?: number;
  patientId?: number;
  doctorId?: number;
  date?: string;
}

// ---- Query keys (one place so cache invalidation is consistent) ------------

export const opdQueryKeys = {
  followUps: (state: FollowUpListState) =>
    ["appointments", "follow-ups", state] as const,
  telemedicine: (id: number) => ["appointments", id, "telemedicine"] as const,
  questionnaire: (id: number, module: string) =>
    ["appointments", id, "questionnaire", module] as const,
  questionnairesByEncounter: (encounterId: number) =>
    ["appointments", "encounters", encounterId, "questionnaires"] as const,
  pendingReview: () => ["appointments", "questionnaires", "pending-review"] as const,
  opdCard: (id: number) => ["appointments", id, "opd-card"] as const,
  opdCardHtml: (id: number) => ["appointments", id, "opd-card-html"] as const,
};

// ---- Follow-up visits ------------------------------------------------------

export function useFollowUpList(state: FollowUpListState) {
  return useQuery<FollowUpVisit[]>({
    queryKey: opdQueryKeys.followUps(state),
    queryFn: async () => {
      let url: string;
      if (state.appointmentId != null) {
        url = `${APPT_BASE}/api/v1/appointments/${state.appointmentId}/follow-ups`;
      } else if (state.patientId != null && state.date) {
        url = `${APPT_BASE}/api/v1/appointments/follow-ups/patient/${state.patientId}?date=${state.date}`;
      } else if (state.doctorId != null && state.date) {
        url = `${APPT_BASE}/api/v1/appointments/follow-ups/doctor/${state.doctorId}?date=${state.date}`;
      } else {
        throw new Error("useFollowUpList needs (appointmentId) or (patientId/date) or (doctorId/date)");
      }
      return apptJson<FollowUpVisit[]>(url);
    },
    enabled:
      state.appointmentId != null ||
      (state.patientId != null && !!state.date) ||
      (state.doctorId != null && !!state.date),
  });
}

export function useScheduleFollowUp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      apptJson<FollowUpVisit>(`${APPT_BASE}/api/v1/appointments/${id}/follow-up`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: ["appointments", id, "follow-ups"] });
      void qc.invalidateQueries({ queryKey: ["appointments", "follow-ups"] });
    },
  });
}

export function useCancelFollowUp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (followUpId: number) =>
      apptJson<FollowUpVisit>(`${APPT_BASE}/api/v1/appointments/follow-ups/${followUpId}/cancel`, {
        method: "POST",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments", "follow-ups"] }),
  });
}

export function useCompleteFollowUp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (followUpId: number) =>
      apptJson<FollowUpVisit>(`${APPT_BASE}/api/v1/appointments/follow-ups/${followUpId}/complete`, {
        method: "POST",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments", "follow-ups"] }),
  });
}

// ---- Telemedicine rooms ----------------------------------------------------

export function useGetTelemedicine(appointmentId: number) {
  return useQuery<TelemedicineRoom | null>({
    queryKey: opdQueryKeys.telemedicine(appointmentId),
    queryFn: () => apptJson<TelemedicineRoom>(`${APPT_BASE}/api/v1/appointments/${appointmentId}/telemedicine`),
    retry: false,
  });
}

export function useCreateTelemedicine() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (appointmentId: number) =>
      apptJson<TelemedicineRoom>(`${APPT_BASE}/api/v1/appointments/${appointmentId}/telemedicine`, {
        method: "POST",
      }),
    onSuccess: (_, id) => qc.invalidateQueries({ queryKey: opdQueryKeys.telemedicine(id) }),
  });
}

export function useStartTelemedicine() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (roomId: number) =>
      apptJson<TelemedicineRoom>(`${APPT_BASE}/api/v1/appointments/telemedicine/${roomId}/start`, {
        method: "POST",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments", "telemedicine"] }),
  });
}

export function useEndTelemedicine() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (roomId: number) =>
      apptJson<TelemedicineRoom>(`${APPT_BASE}/api/v1/appointments/telemedicine/${roomId}/end`, {
        method: "POST",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments", "telemedicine"] }),
  });
}

// ---- Pre-consult questionnaire --------------------------------------------

export function useGetQuestionnaire(appointmentId: number, module: string = "Patient Assessment") {
  return useQuery<QuestionnaireRecord | null>({
    queryKey: opdQueryKeys.questionnaire(appointmentId, module),
    queryFn: async () => {
      try {
        return await apptJson<QuestionnaireRecord>(
          `${APPT_BASE}/api/v1/appointments/${appointmentId}/questionnaire?module=${encodeURIComponent(module)}`,
        );
      } catch (e) {
        if (e instanceof ApiError && e.status === 400) return null;
        throw e;
      }
    },
  });
}

export function useSubmitQuestionnaire() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      apptJson<QuestionnaireRecord>(`${APPT_BASE}/api/v1/appointments/${id}/questionnaire`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => qc.invalidateQueries({ queryKey: ["appointments", id, "questionnaire"] }),
  });
}

export function useListQuestionnairesByEncounter(encounterId: number) {
  return useQuery<QuestionnaireRecord[]>({
    queryKey: opdQueryKeys.questionnairesByEncounter(encounterId),
    queryFn: () =>
      apptJson<QuestionnaireRecord[]>(
        `${APPT_BASE}/api/v1/appointments/encounters/${encounterId}/questionnaires`,
      ),
    enabled: encounterId > 0,
  });
}

export function usePendingReviewQuestionnaires() {
  return useQuery<QuestionnaireRecord[]>({
    queryKey: opdQueryKeys.pendingReview(),
    queryFn: () => apptJson<QuestionnaireRecord[]>(`${APPT_BASE}/api/v1/appointments/questionnaires/pending-review`),
  });
}

export function useReviewQuestionnaire() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (questionnaireId: number) =>
      apptJson<QuestionnaireRecord>(`${APPT_BASE}/api/v1/appointments/questionnaires/${questionnaireId}/review`, {
        method: "POST",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments", "questionnaires"] }),
  });
}

// ---- OPD card --------------------------------------------------------------

export function useGetOpdCard(appointmentId: number) {
  return useQuery<OpdCardData>({
    queryKey: opdQueryKeys.opdCard(appointmentId),
    queryFn: () => apptJson<OpdCardData>(`${APPT_BASE}/api/v1/appointments/${appointmentId}/opd-card`),
    enabled: appointmentId > 0,
  });
}

export function useGetOpdCardHtml(appointmentId: number) {
  return useQuery<string>({
    queryKey: opdQueryKeys.opdCardHtml(appointmentId),
    queryFn: () => apptText(`${APPT_BASE}/api/v1/appointments/${appointmentId}/opd-card/html`),
    enabled: appointmentId > 0,
  });
}

// ---- Walk-in (delegated to the master appointment controller) -------------

export function useWalkin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      apptJson<unknown>(`${APPT_BASE}/api/v1/appointments/walkin`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["appointments"] }),
  });
}
