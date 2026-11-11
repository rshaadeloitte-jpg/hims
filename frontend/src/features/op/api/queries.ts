import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  abhaApi,
  appointmentApi,
  patientApi,
  queueApi,
} from "./client";
import { useOpSession } from "../store/opSession";
import type {
  Appointment,
  AppointmentListQuery,
  AppointmentListResponse,
  AppointmentSlot,
  Department,
  Doctor,
  DoctorSchedule,
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
  departments: (unitId?: number | null) => ["op", "department", "list", unitId ?? null] as const,
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

export function useDepartments(unitId?: number | null) {
  // The reference-service departmentmaster is unit-scoped: reuse the OP
  // session's unit (legacy Fg.g("UnitId")) when present, but the endpoint
  // also works with no unit, so never fail when one is missing.
  const sessionUnitId = useOpSession().unitId;
  const effectiveUnitId = unitId !== undefined ? unitId : sessionUnitId;
  return useQuery({
    queryKey: opKeys.departments(effectiveUnitId),
    queryFn: () => appointmentApi.departments(effectiveUnitId),
    staleTime: 5 * 60_000,
  });
}

/**
 * Derives unique doctor records from DoctorSchedule rows, resolving names
 * from doctor master records if the schedule row does not provide a name.
 */
export function deriveDoctorsFromSchedules(
  schedules: DoctorSchedule[],
  doctorMasters?: Array<Record<string, unknown>>,
): Doctor[] {
  const masterMap = new Map<number, string>();
  if (doctorMasters) {
    for (const d of doctorMasters) {
      const id = Number(d.id);
      if (!id) continue;
      const fullName = [d.firstName, d.lastName].filter(Boolean).join(" ");
      const name = fullName
        ? (fullName.startsWith("Dr.") ? fullName : `Dr. ${fullName}`)
        : (d.name ? String(d.name) : `Doctor #${id}`);
      masterMap.set(id, name);
    }
  }

  const seenDoctorIds = new Set<number>();
  const doctors: Doctor[] = [];

  for (const s of schedules) {
    const raw = s as unknown as Record<string, unknown>;
    const doctorId = typeof s.doctorId === "number" ? s.doctorId : Number(raw.id);
    if (!doctorId || seenDoctorIds.has(doctorId)) continue;
    seenDoctorIds.add(doctorId);

    const inlineName = (raw.name as string | undefined) ?? (raw.doctorName as string | undefined);
    const name = inlineName ?? masterMap.get(doctorId) ?? `Doctor #${doctorId}`;

    doctors.push({
      id: doctorId,
      name,
      departmentId: s.departmentId ?? 0,
      isActive: s.status !== false,
    });
  }

  return doctors;
}

export function useDoctorSchedules(departmentId?: number) {
  const sessionUnitId = useOpSession().unitId;
  return useQuery({
    queryKey: opKeys.doctorSchedules(departmentId),
    queryFn: async (): Promise<Doctor[]> => {
      const schedules = await appointmentApi.doctorSchedules(departmentId);
      const needsNameResolution = schedules.some((s) => {
        const raw = s as unknown as Record<string, unknown>;
        return !raw.name && !raw.doctorName;
      });

      let doctorMasters: Array<Record<string, unknown>> | undefined;
      if (needsNameResolution && schedules.length > 0) {
        try {
          doctorMasters = await appointmentApi.doctorsMaster(sessionUnitId);
        } catch {
          // If master lookup fails, fall back gracefully to Doctor #id
        }
      }

      return deriveDoctorsFromSchedules(schedules, doctorMasters);
    },
    enabled: departmentId != null,
    staleTime: 60_000,
  });
}

export function normalizeSlots(rawSlots: AppointmentSlot[]): AppointmentSlot[] {
  return (rawSlots ?? []).map((s, idx) => {
    const raw = s as unknown as Record<string, unknown>;
    const scheduleId = (raw.scheduleId as number) ?? (raw.slotId as number) ?? idx + 1;
    const slotId = (raw.slotId as number) ?? scheduleId;
    const doctorId = Number(raw.doctorId);
    const date = (raw.date as string) ?? (typeof raw.startAt === "string" ? raw.startAt.slice(0, 10) : "");
    const slotStart = (raw.slotStart as string) ?? (typeof raw.startAt === "string" && raw.startAt.includes("T") ? raw.startAt.split("T")[1] : "");
    const slotEnd = (raw.slotEnd as string) ?? (typeof raw.endAt === "string" && raw.endAt.includes("T") ? raw.endAt.split("T")[1] : "");
    const startAt = (raw.startAt as string) ?? (date && slotStart ? `${date}T${slotStart}` : "");
    const endAt = (raw.endAt as string) ?? (date && slotEnd ? `${date}T${slotEnd}` : startAt);
    const capacity = typeof raw.capacity === "number" ? raw.capacity : 1;
    const bookedCount = typeof raw.bookedCount === "number" ? raw.bookedCount : (typeof raw.booked === "number" ? raw.booked : 0);
    const booked = typeof raw.booked === "boolean" ? raw.booked : bookedCount >= capacity;
    const available = raw.available !== undefined ? Boolean(raw.available) : !booked;

    return {
      slotId,
      scheduleId,
      doctorId,
      departmentId: raw.departmentId as number | undefined,
      doctorName: raw.doctorName as string | undefined,
      date,
      slotStart,
      slotEnd,
      startAt,
      endAt,
      cabin: raw.cabin as string | undefined,
      capacity,
      booked: bookedCount,
      bookedCount,
      available,
    };
  });
}

export function useDoctorSlots(doctorId: number | undefined, date: string | undefined) {
  return useQuery({
    queryKey: opKeys.doctorSlots(doctorId ?? -1, date ?? ""),
    queryFn: async (): Promise<AppointmentSlot[]> => {
      const raw = await appointmentApi.slots(doctorId!, date!);
      return normalizeSlots(raw);
    },
    enabled: doctorId != null && !!date,
    staleTime: 30_000,
  });
}

/* ── appointments ──────────────────────────────────── */

export function useAppointmentList(q: AppointmentListQuery) {
  return useQuery({
    queryKey: opKeys.appointmentList(q),
    queryFn: () => appointmentApi.list(q),
    enabled: !!q.date,
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
