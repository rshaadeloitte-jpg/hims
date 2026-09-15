import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  DoctorShareFormValues,
  DoctorShareEditValues,
  PayoutRecord,
  PayoutFilterValues,
} from "./zodSchema";

const DOCTOR_SHARE_BASE: string =
  import.meta.env.VITE_DOCTOR_SHARE_API_BASE ?? "/api";

async function doctorShareJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Query keys ------------------------------------------------------------

export const doctorShareQueryKeys = {
  all: ["doctor-shares"] as const,
  list: (filter: PayoutFilterValues) => ["doctor-shares", "list", filter] as const,
  detail: (id: string) => ["doctor-shares", id] as const,
  payouts: (filter: PayoutFilterValues) => ["doctor-shares", "payouts", filter] as const,
  calculator: (params: { doctorId: number; service: string; baseAmount: number }) =>
    ["doctor-shares", "calculator", params] as const,
} as const;

// ---- Share list ------------------------------------------------------------

export function useDoctorShares() {
  return useQuery({
    queryKey: doctorShareQueryKeys.all,
    queryFn: () => doctorShareJson<DoctorShareFormValues[]>(`${DOCTOR_SHARE_BASE}/v1/doctor-shares`),
  });
}

// ---- Payout ledger --------------------------------------------------------

export function usePayoutList(filter: PayoutFilterValues) {
  return useQuery<{ content: PayoutRecord[]; totalElements: number; totalPages: number; page: number; size: number }>({
    queryKey: doctorShareQueryKeys.payouts(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter.fromDate) params.set("fromDate", filter.fromDate);
      if (filter.toDate) params.set("toDate", filter.toDate);
      if (filter.status) params.set("status", filter.status);
      params.set("page", String(filter.page));
      params.set("size", String(filter.size));
      return doctorShareJson(
        `${DOCTOR_SHARE_BASE}/v1/doctor-shares/payouts?${params}`
      );
    },
  });
}

// ---- Single share detail ---------------------------------------------------

export function useDoctorShareDetail(id: string) {
  return useQuery<DoctorShareFormValues>({
    queryKey: doctorShareQueryKeys.detail(id),
    queryFn: () => doctorShareJson<DoctorShareFormValues>(`${DOCTOR_SHARE_BASE}/v1/doctor-shares/${id}`),
    enabled: !!id,
  });
}

// ---- Create share (add) ----------------------------------------------------

export function useCreateDoctorShare() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: DoctorShareFormValues) =>
      doctorShareJson<DoctorShareFormValues>(`${DOCTOR_SHARE_BASE}/v1/doctor-shares`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.all });
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.payouts({ page: 0, size: 20 }) });
    },
  });
}

// ---- Update share (deduct/edit) ------------------------------------------

export function useUpdateDoctorShare() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: DoctorShareEditValues) =>
      doctorShareJson<DoctorShareFormValues>(
        `${DOCTOR_SHARE_BASE}/v1/doctor-shares/${payload.id}`,
        {
          method: "PUT",
          body: JSON.stringify(payload),
        }
      ),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.detail(id) });
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.all });
    },
  });
}

// ---- Delete share ----------------------------------------------------------

export function useDeleteDoctorShare() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) =>
      doctorShareJson<{ success: boolean }>(`${DOCTOR_SHARE_BASE}/v1/doctor-shares/${id}`, {
        method: "DELETE",
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.all });
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.payouts({ page: 0, size: 20 }) });
    },
  });
}

// ---- Process payout --------------------------------------------------------

export function useProcessPayout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) =>
      doctorShareJson<{ success: boolean }>(
        `${DOCTOR_SHARE_BASE}/v1/doctor-shares/${id}/payout`,
        { method: "POST" }
      ),
    onSuccess: (_, id) => {
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.detail(id) });
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.all });
      void qc.invalidateQueries({ queryKey: doctorShareQueryKeys.payouts({}) });
    },
  });
}

// ---- Calculator endpoint ---------------------------------------------------

export function useDoctorShareCalculator(params: { doctorId: number; service: string; baseAmount: number }) {
  return useQuery<{
    baseAmount: number;
    sharePercent: number;
    shareAmount: number;
    gstAmount: number;
    netAmount: number;
  }>({
    queryKey: doctorShareQueryKeys.calculator(params),
    queryFn: () =>
      doctorShareJson(`
        ${DOCTOR_SHARE_BASE}/v1/doctor-shares/calculate?doctorId=${params.doctorId}&service=${encodeURIComponent(params.service)}&baseAmount=${params.baseAmount}
      `),
    enabled: params.doctorId > 0 && params.baseAmount > 0,
    staleTime: 5 * 60 * 1000,
  });
}