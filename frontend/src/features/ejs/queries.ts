import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const EJS_BASE: string = import.meta.env.VITE_EJS_API_BASE ?? "/api";

async function ejsJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface AshaWorkRecord {
  id: number;
  beneficiaryId: number;
  beneficiaryName: string;
  trackingType: "ANC" | "PNC" | "CHILD";
  visitDate: string;
  ashaId: number;
  ashaName: string;
  status: "SCHEDULED" | "COMPLETED" | "MISSED" | "RESCHEDULED";
  gestationalWeek?: number;
  weight?: number;
  bloodPressureSystolic?: number;
  bloodPressureDiastolic?: number;
  hemoglobin?: number;
  immunizationGiven?: string;
  deliveryDate?: string;
  deliveryType?: "NORMAL" | "CESAREAN" | "ASSISTED";
  postnatalDay?: number;
  childAgeMonths?: number;
  weightChild?: number;
  heightChild?: number;
  immunizationStatus?: "UPTO_DATE" | "DUE" | "OVERDUE";
  nutritionStatus?: "NORMAL" | "MALNUTRITION" | "SEVERE_MALNUTRITION";
  remarks?: string;
  nextVisitDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface IncentiveRecord {
  id: number;
  ashaId: number;
  ashaName: string;
  month: string;
  ancVisits: number;
  pncVisits: number;
  childCareVisits: number;
  immunizationReferrals: number;
  institutionalDeliveryReferrals: number;
  ancIncentive: number;
  pncIncentive: number;
  childCareIncentive: number;
  immunizationIncentive: number;
  deliveryIncentive: number;
  totalIncentive: number;
  status: "PENDING" | "APPROVED" | "PAID" | "REJECTED";
  approvedBy?: string;
  approvedAt?: string;
  paidAt?: string;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
}

export interface GeographyRecord {
  id: number;
  ashaId: number;
  ashaName: string;
  state: string;
  district: string;
  block: string;
  village: string;
  subCenter: string;
  totalPopulation: number;
  targetCouples: number;
  pregnantWomen: number;
  childrenUnder5: number;
  households: number;
  eligibleCouples: number;
  isActive: boolean;
  lastEnumerationDate?: string;
  remarks?: string;
  createdAt: string;
  updatedAt: string;
}

// ---- Query keys ------------------------------------------------------------

export const ejsQueryKeys = {
  ashaWork: (filters: Record<string, unknown>) => ["ejs", "asha-work", filters] as const,
  ashaWorkDetail: (id: number) => ["ejs", "asha-work", id] as const,
  incentives: (filters: Record<string, unknown>) => ["ejs", "incentives", filters] as const,
  incentiveDetail: (id: number) => ["ejs", "incentives", id] as const,
  geography: (filters: Record<string, unknown>) => ["ejs", "geography", filters] as const,
  geographyDetail: (id: number) => ["ejs", "geography", id] as const,
  ashaWorkers: () => ["ejs", "asha-workers"] as const,
};

// ---- AshaWork queries ------------------------------------------------------

export function useAshaWorkList(filters: Record<string, unknown>) {
  return useQuery<AshaWorkRecord[]>({
    queryKey: ejsQueryKeys.ashaWork(filters),
    queryFn: () => ejsJson<AshaWorkRecord[]>(`${EJS_BASE}/api/v1/ejs/asha-work?${new URLSearchParams(filters as Record<string, string>)}`),
  });
}

export function useAshaWorkDetail(id: number) {
  return useQuery<AshaWorkRecord>({
    queryKey: ejsQueryKeys.ashaWorkDetail(id),
    queryFn: () => ejsJson<AshaWorkRecord>(`${EJS_BASE}/api/v1/ejs/asha-work/${id}`),
    enabled: id > 0,
  });
}

export function useCreateAshaWork() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      ejsJson<AshaWorkRecord>(`${EJS_BASE}/api/v1/ejs/asha-work`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["ejs", "asha-work"] });
    },
  });
}

export function useUpdateAshaWork(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      ejsJson<AshaWorkRecord>(`${EJS_BASE}/api/v1/ejs/asha-work/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["ejs", "asha-work"] });
    },
  });
}

// ---- Incentive queries -----------------------------------------------------

export function useIncentiveList(filters: Record<string, unknown>) {
  return useQuery<IncentiveRecord[]>({
    queryKey: ejsQueryKeys.incentives(filters),
    queryFn: () => ejsJson<IncentiveRecord[]>(`${EJS_BASE}/api/v1/ejs/incentives?${new URLSearchParams(filters as Record<string, string>)}`),
  });
}

export function useIncentiveDetail(id: number) {
  return useQuery<IncentiveRecord>({
    queryKey: ejsQueryKeys.incentiveDetail(id),
    queryFn: () => ejsJson<IncentiveRecord>(`${EJS_BASE}/api/v1/ejs/incentives/${id}`),
    enabled: id > 0,
  });
}

export function useApproveIncentive(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () =>
      ejsJson<IncentiveRecord>(`${EJS_BASE}/api/v1/ejs/incentives/${id}/approve`, { method: "POST" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["ejs", "incentives"] });
    },
  });
}

export function usePayIncentive(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () =>
      ejsJson<IncentiveRecord>(`${EJS_BASE}/api/v1/ejs/incentives/${id}/pay`, { method: "POST" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["ejs", "incentives"] });
    },
  });
}

// ---- Geography queries -----------------------------------------------------

export function useGeographyList(filters: Record<string, unknown>) {
  return useQuery<GeographyRecord[]>({
    queryKey: ejsQueryKeys.geography(filters),
    queryFn: () => ejsJson<GeographyRecord[]>(`${EJS_BASE}/api/v1/ejs/geography?${new URLSearchParams(filters as Record<string, string>)}`),
  });
}

export function useGeographyDetail(id: number) {
  return useQuery<GeographyRecord>({
    queryKey: ejsQueryKeys.geographyDetail(id),
    queryFn: () => ejsJson<GeographyRecord>(`${EJS_BASE}/api/v1/ejs/geography/${id}`),
    enabled: id > 0,
  });
}

export function useCreateGeography() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      ejsJson<GeographyRecord>(`${EJS_BASE}/api/v1/ejs/geography`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["ejs", "geography"] });
    },
  });
}

export function useUpdateGeography(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      ejsJson<GeographyRecord>(`${EJS_BASE}/api/v1/ejs/geography/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["ejs", "geography"] });
    },
  });
}

// ---- ASHA workers (for dropdowns) ------------------------------------------

export function useAshaWorkers() {
  return useQuery<{ ashaId: number; ashaName: string }[]>({
    queryKey: ejsQueryKeys.ashaWorkers(),
    queryFn: () => ejsJson<{ ashaId: number; ashaName: string }[]>(`${EJS_BASE}/api/v1/ejs/asha-workers`),
    staleTime: 5 * 60_000,
  });
}
