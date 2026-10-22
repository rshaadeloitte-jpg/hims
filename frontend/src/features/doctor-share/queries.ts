import { useQuery } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M06 Doctor Share hooks — consume the billing-service REST API.
 * Backend endpoints (billing-service DoctorShareController):
 *   POST /api/v1/doctor-share-calculations          — calculate share
 *   GET  /api/v1/doctor-share-calculations/{billId} — shares for a bill
 *   PUT  /api/v1/doctor-share-add                   — add incentive
 *   DELETE /api/v1/doctor-share-deduct              — deduct incentive
 *   GET  /api/v1/doctor-share/bills?doctorId=       — shares by doctor
 *   GET  /api/v1/doctor-shares                     — paginated list
 */

const BASE = import.meta.env.VITE_BILLING_API_BASE ?? "/api";

async function dsJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// ---- Types matching backend DoctorShare / DoctorShareCalculation ----

export interface DoctorShareRecord {
  id: number;
  shareId?: number;
  billId: number;
  chargeId: number;
  doctorId: number;
  shareAmount: number;
  hospitalAmount: number;
  isBilled: boolean;
}

export interface ShareCalculationResult {
  chargeId: number | null;
  doctorId: number | null;
  totalAmount: number;
  sharePercentage: number;
  fixedAmount: number;
  shareAmount: number;
  hospitalAmount: number;
  calculationType: string;
}

export interface DoctorShareFormValues {
  doctorId: number;
  totalAmount: number;
  sharePercentage: number;
  fixedAmount: number;
}

// ---- Query keys ----

export const doctorShareQueryKeys = {
  all: ["doctor-shares"] as const,
  list: (page: number, size: number) => ["doctor-shares", "list", page, size] as const,
  byDoctor: (doctorId: number) => ["doctor-shares", "doctor", doctorId] as const,
  byBill: (billId: number) => ["doctor-shares", "bill", billId] as const,
  calculator: (doctorId: number, totalAmount: number, sharePercentage: number) =>
    ["doctor-shares", "calc", doctorId, totalAmount, sharePercentage] as const,
};

// ---- List shares (paginated) ----

export function useDoctorShares(page = 0, size = 20) {
  return useQuery<{ content: DoctorShareRecord[]; totalElements: number; totalPages: number }>({
    queryKey: doctorShareQueryKeys.list(page, size),
    queryFn: () =>
      dsJson(`${BASE}/api/v1/doctor-shares?page=${page}&size=${size}`),
  });
}

// ---- Shares by doctor ----

export function useDoctorSharesByDoctor(doctorId: number) {
  return useQuery<DoctorShareRecord[]>({
    queryKey: doctorShareQueryKeys.byDoctor(doctorId),
    queryFn: () =>
      dsJson<DoctorShareRecord[]>(`${BASE}/api/v1/doctor-share/bills?doctorId=${doctorId}`),
    enabled: doctorId > 0,
  });
}

// ---- Shares by bill ----

export function useDoctorSharesByBill(billId: number) {
  return useQuery<ShareCalculationResult[]>({
    queryKey: doctorShareQueryKeys.byBill(billId),
    queryFn: () =>
      dsJson<ShareCalculationResult[]>(`${BASE}/api/v1/doctor-share-calculations/${billId}`),
    enabled: billId > 0,
  });
}

// ---- Calculate share ----

export function useDoctorShareCalculator(params: {
  totalAmount: number;
  sharePercentage: number;
  fixedAmount: number;
}) {
  return useQuery<ShareCalculationResult>({
    queryKey: doctorShareQueryKeys.calculator(0, params.totalAmount, params.sharePercentage),
    queryFn: () =>
      dsJson<ShareCalculationResult>(`${BASE}/api/v1/doctor-share-calculations`, {
        method: "POST",
        body: JSON.stringify({
          totalAmount: params.totalAmount,
          sharePercentage: params.sharePercentage,
          fixedAmount: params.fixedAmount,
        }),
      }),
    enabled: params.totalAmount > 0,
    staleTime: 5 * 60 * 1000,
  });
}