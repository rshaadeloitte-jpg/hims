import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const ADT_BASE: string = import.meta.env.VITE_ADT_API_BASE ?? "/api";

async function adtJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Types -----------------------------------------------------------------

export interface Bed {
  bedId: number;
  bedNo: string;
  wardId: number;
  wardName?: string;
  status: number; // 1=FREE 2=OCCUPIED 3=RESERVED 4=MAINTENANCE 5=CLEANING
  isActive: boolean;
}

export interface Admission {
  admissionId: number;
  patientId: number;
  ipdNo: string;
  admissionType: string;
  admissionDate: string;
  wardId?: number;
  bedId?: number;
  isMlc: boolean;
  mlcNumber?: string;
  isExpired: boolean;
  isDischarged: boolean;
  isAbsconded: boolean;
  doctorId?: number;
  attendingDoctorId?: number;
  remarks?: string;
  unitId?: number;
}

export interface Ward {
  wardId: number;
  wardName: string;
  departmentId: number;
  totalBeds: number;
  isActive: boolean;
}

export interface Census {
  wardId: number;
  wardName: string;
  totalBeds: number;
  occupied: number;
  free: number;
  cleaning: number;
  interimCount: number;
}

export interface AttendantPass {
  passId: number;
  admissionId: number;
  passNo: string;
  attendantName: string;
  relation?: string;
  mobileNo?: string;
  validUntil?: string;
  status: string; // ISSUED, RETURNED
}

// ---- Query keys ------------------------------------------------------------

export const adtQueryKeys = {
  admissions: (patientId?: number, active?: boolean) => ["adt", "admissions", { patientId, active }] as const,
  admission: (id: number) => ["adt", "admissions", id] as const,
  beds: () => ["adt", "beds"] as const,
  availableBeds: () => ["adt", "beds", "available"] as const,
  wards: () => ["adt", "wards"] as const,
  census: () => ["adt", "census"] as const,
  tids: () => ["adt", "tids"] as const,
  deaths: () => ["adt", "deaths"] as const,
  attendantPasses: (admissionId: number) => ["adt", "attendant-passes", admissionId] as const,
  clearance: (admissionId: number) => ["adt", "clearance", admissionId] as const,
};

// ---- Admissions ------------------------------------------------------------

export function useAdmissions(patientId?: number, active?: boolean) {
  return useQuery<Admission[]>({
    queryKey: adtQueryKeys.admissions(patientId, active),
    queryFn: () => {
      const params = new URLSearchParams();
      if (patientId) params.set("patientId", String(patientId));
      if (active !== undefined) params.set("active", String(active));
      return adtJson<Admission[]>(`${ADT_BASE}/api/v1/admissions?${params}`);
    },
  });
}

export function useAdmission(id: number) {
  return useQuery<Admission & { charges?: unknown[] }>({
    queryKey: adtQueryKeys.admission(id),
    queryFn: () => adtJson(`${ADT_BASE}/api/v1/admissions/${id}`),
    enabled: id > 0,
  });
}

export function useAdmit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      adtJson<Admission>(`${ADT_BASE}/api/v1/admissions`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ["adt"] }); },
  });
}

export function useTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      adtJson<Admission>(`${ADT_BASE}/api/v1/admissions/${id}/transfer`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ["adt"] }); },
  });
}

export function useDischarge() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      adtJson<Admission>(`${ADT_BASE}/api/v1/admissions/${id}/discharge`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ["adt"] }); },
  });
}

export function useAbscond() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      adtJson<Admission>(`${ADT_BASE}/api/v1/admissions/${id}/abscond`, { method: "POST" }),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ["adt"] }); },
  });
}

export function useInterimAdmit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      adtJson<Admission>(`${ADT_BASE}/api/v1/admissions/interim`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ["adt"] }); },
  });
}

// ---- Beds ------------------------------------------------------------------

export function useBeds() {
  return useQuery<Bed[]>({
    queryKey: adtQueryKeys.beds(),
    queryFn: () => adtJson<Bed[]>(`${ADT_BASE}/api/v1/beds`),
  });
}

export function useAvailableBeds() {
  return useQuery<Bed[]>({
    queryKey: adtQueryKeys.availableBeds(),
    queryFn: () => adtJson<Bed[]>(`${ADT_BASE}/api/v1/beds/available`),
  });
}

export function useCleaningComplete() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (bedId: number) =>
      adtJson<Bed>(`${ADT_BASE}/api/v1/beds/${bedId}/cleaning-complete`, { method: "POST" }),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ["adt", "beds"] }); },
  });
}

// ---- Wards -----------------------------------------------------------------

export function useWards() {
  return useQuery<Ward[]>({
    queryKey: adtQueryKeys.wards(),
    queryFn: () => adtJson<Ward[]>(`${ADT_BASE}/api/v1/wards`),
  });
}

// ---- Census ----------------------------------------------------------------

export function useCensus() {
  return useQuery<Census[]>({
    queryKey: adtQueryKeys.census(),
    queryFn: () => adtJson<Census[]>(`${ADT_BASE}/api/v1/adt/census`),
  });
}

// ---- TID / Deaths ----------------------------------------------------------

export function useTids() {
  return useQuery<{ admissionId: number; tid: string; ipdNo: string }[]>({
    queryKey: adtQueryKeys.tids(),
    queryFn: () => adtJson(`${ADT_BASE}/api/v1/adt/tids`),
  });
}

export function useDeaths() {
  return useQuery<Admission[]>({
    queryKey: adtQueryKeys.deaths(),
    queryFn: () => adtJson(`${ADT_BASE}/api/v1/adt/deaths`),
  });
}

// ---- Attendant passes ------------------------------------------------------

export function useAttendantPasses(admissionId: number) {
  return useQuery<AttendantPass[]>({
    queryKey: adtQueryKeys.attendantPasses(admissionId),
    queryFn: () => adtJson(`${ADT_BASE}/api/v1/admissions/${admissionId}/attendant-passes`),
    enabled: admissionId > 0,
  });
}

export function useIssueAttendantPass() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ admissionId, payload }: { admissionId: number; payload: Record<string, unknown> }) =>
      adtJson<AttendantPass>(`${ADT_BASE}/api/v1/admissions/${admissionId}/attendant-pass`, {
        method: "POST", body: JSON.stringify(payload),
      }),
    onSuccess: (_, { admissionId }) => {
      void qc.invalidateQueries({ queryKey: ["adt", "attendant-passes", admissionId] });
    },
  });
}

// ---- Clearance -------------------------------------------------------------

export function useClearance(admissionId: number) {
  return useQuery<{ department: string; cleared: boolean }[]>({
    queryKey: adtQueryKeys.clearance(admissionId),
    queryFn: () => adtJson(`${ADT_BASE}/api/v1/admissions/${admissionId}/clearance`),
    enabled: admissionId > 0,
  });
}

export function useSubmitClearance() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, department, payload }: { id: number; department: string; payload: Record<string, unknown> }) =>
      adtJson(`${ADT_BASE}/api/v1/admissions/${id}/clearance/${department}`, {
        method: "POST", body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => { void qc.invalidateQueries({ queryKey: ["adt", "clearance", id] }); },
  });
}