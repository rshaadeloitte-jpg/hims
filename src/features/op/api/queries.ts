import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  abhaApi,
  appointmentApi,
  patientApi,
  queueApi,
} from "./client";
import type {
  Appointment,
  AppointmentListQuery,
  AppointmentListResponse,
  AppointmentSlot,
  Department,
  Doctor,
  Patient,
  PatientSearchQuery,
  QueueToken,
  QueueTokenCreate,
} from "./types";

/**
 * OP module — TanStack Query hooks. Query keys are namespaced so we can
 * invalidate by aggregate ("appointment.list") without colliding with the
 * masters module ("masters.list").
 */
export const opKeys = {
  patientSearch: (q: PatientSearchQuery) => ["op", "patient", "search", q] as const,
  patient: (id: number) => ["op", "patient", id] as const,
  patientSponsors: (id: number) => ["op", "patient", id, "sponsors"] as const,
  departments: () => ["op", "department", "list"] as const,
  doctorSchedules: (departmentId?: number) =>
    ["op", "doctor", "schedules", departmentId ?? null] as const,
  doctorSlots: (doctorId: number, date: string) =>
    ["op", "doctor", doctorId, "slots", date] as const,
  appointmentList: (q: AppointmentListQuery) => ["op", "appointment", "list", q] as const,
  appointment: (id: number) => ["op", "appointment", id] as const,
  queueToday: (counterId?: number) => ["op", "queue", "today", counterId ?? null] as const,
  abha: (patientId: number) => ["op", "abha", patientId] as const,
};

/* ── patient ──────────────────────────────────────────── */

export function usePatientSearch(q: PatientSearchQuery, enabled = true) {
  return useQuery({
    queryKey: opKeys.patientSearch(q),
    queryFn: () => patientApi.search(q),
    enabled: enabled && Object.values(q).some((v) => v !== undefined && v !== ""),
    staleTime: 30_000,
  });
}

export function usePatient(id: number | undefined) {
  return useQuery({
    queryKey: id ? opKeys.patient(id) : ["op", "patient", "none"],
    queryFn: () => patientApi.get(id!),
    enabled: typeof id === "number",
  });
}

export function usePatientSponsors(id: number | undefined) {
  return useQuery({
    queryKey: id ? opKeys.patientSponsors(id) : ["op", "patient", "none", "sponsors"],
    queryFn: () => patientApi.sponsors(id!),
    enabled: typeof id === "number",
  });
}

export function useRegisterPatient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: patientApi.register,
    onSuccess: (created) => {
      void qc.invalidateQueries({ queryKey: ["op", "patient"] });
      void qc.setQueryData(opKeys.patient(created.id), created);
    },
  });
}

export function useUpdatePatient(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (patch: Partial<Patient>) => patientApi.update(id, patch),
    onSuccess: (updated) => {
      void qc.setQueryData(opKeys.patient(id), updated);
      void qc.invalidateQueries({ queryKey: ["op", "patient"] });
    },
  });
}

/* ── departments / doctors / slots (cascade) ───────── */

export function useDepartments() {
  return useQuery({
    queryKey: opKeys.departments(),
    queryFn: appointmentApi.departments,
    staleTime: 5 * 60_000,
  });
}

export function useDoctorSchedules(departmentId?: number) {
  return useQuery({
    queryKey: opKeys.doctorSchedules(departmentId),
    queryFn: () => appointmentApi.doctorSchedules(departmentId),
    enabled: departmentId != null,
    staleTime: 60_000,
  });
}

export function useDoctorSlots(doctorId: number | undefined, date: string | undefined) {
  return useQuery({
    queryKey: opKeys.doctorSlots(doctorId ?? -1, date ?? ""),
    queryFn: () => appointmentApi.slots(doctorId!, date!),
    enabled: doctorId != null && !!date,
    staleTime: 30_000,
  });
}

/* ── appointments ──────────────────────────────────── */

export function useAppointmentList(q: AppointmentListQuery) {
  return useQuery({
    queryKey: opKeys.appointmentList(q),
    queryFn: () => appointmentApi.list(q),
    placeholderData: (prev) => prev,
  });
}

export function useAppointment(id: number | undefined) {
  return useQuery({
    queryKey: id ? opKeys.appointment(id) : ["op", "appointment", "none"],
    queryFn: () => appointmentApi.get(id!),
    enabled: typeof id === "number",
  });
}

export function useCreateAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: appointmentApi.create,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["op", "appointment"] });
    },
  });
}

export function useCancelAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => appointmentApi.cancel(id, reason),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["op", "appointment"] });
    },
  });
}

export function useCheckInAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => appointmentApi.checkIn(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["op", "appointment"] });
    },
  });
}

export function useNoShowAppointment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => appointmentApi.noShow(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["op", "appointment"] });
    },
  });
}

/* ── queue ──────────────────────────────────────────── */

export function useQueueToday(counterId?: number) {
  return useQuery({
    queryKey: opKeys.queueToday(counterId),
    queryFn: () => queueApi.today(counterId),
    refetchInterval: 15_000,
  });
}

export function useIssueToken() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: QueueTokenCreate) => queueApi.issue(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["op", "queue"] });
    },
  });
}

export function useCallNext() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (counterId: number) => queueApi.callNext(counterId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["op", "queue"] });
    },
  });
}

export function useCompleteToken() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => queueApi.complete(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["op", "queue"] });
    },
  });
}

export function useTransferToken() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, toCounterId }: { id: number; toCounterId: number }) =>
      queueApi.transfer(id, toCounterId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["op", "queue"] });
    },
  });
}

/* ── ABHA / ABDM ──────────────────────────────────── */

export function useAbhaForPatient(patientId: number | undefined) {
  return useQuery({
    queryKey: patientId ? opKeys.abha(patientId) : ["op", "abha", "none"],
    queryFn: () => abhaApi.getForPatient(patientId!),
    enabled: typeof patientId === "number",
  });
}

export function useStartAbhaEnrollment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { patientId: number; aadhaar?: string; mobile?: string }) =>
      abhaApi.startEnrollment(body),
    onSuccess: (_data, vars) => {
      void qc.invalidateQueries({ queryKey: opKeys.abha(vars.patientId) });
    },
  });
}

export function useVerifyAbhaOtp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ enrollmentId, otp }: { enrollmentId: number; otp: string }) =>
      abhaApi.verifyOtp(enrollmentId, otp),
    onSuccess: (enr) => {
      void qc.invalidateQueries({ queryKey: opKeys.abha(enr.patientId) });
    },
  });
}

export function useScanAbhaQr() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (qrPayload: string) => abhaApi.scan(qrPayload),
    onSuccess: (enr) => {
      void qc.invalidateQueries({ queryKey: opKeys.abha(enr.patientId) });
    },
  });
}

export function useConfirmAbhaLink() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ patientId, abhaAddress }: { patientId: number; abhaAddress: string }) =>
      abhaApi.confirmLink(patientId, abhaAddress),
    onSuccess: (_data, vars) => {
      void qc.invalidateQueries({ queryKey: opKeys.abha(vars.patientId) });
    },
  });
}

// re-export selected types for screen convenience
export type { Appointment, AppointmentListResponse, AppointmentSlot, Department, Doctor, Patient, QueueToken };
