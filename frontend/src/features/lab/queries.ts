import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type { WorkloadItem } from './types';

const LAB_BASE: string = import.meta.env.VITE_LAB_API_BASE ?? "/api";

async function labJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface LabOrder {
  orderId: number;
  patientId: number;
  patientName?: string;
  encounterId?: number;
  isOutSourced: boolean;
  status: string; // e.g. ORDERED / COLLECTED / IN_PROGRESS / CANCELLED
  orderDate: string;
  details?: LabDetail[];
}

export interface LabDetail {
  detailId: number;
  orderId?: number;
  testId?: number;
  testName: string;
  testCode?: string;
  status: string;
  barcode?: string;
  critical?: boolean;
  isCritical?: boolean;
  verified?: boolean;
  isVerified?: boolean;
  verifiedBy?: number;
  verifiedAt?: string;
  sampleCollectedAt?: string;
  resultValue?: string;
  resultUnit?: string;
  referenceRange?: string;
  remarks?: string;
}

export interface Machine {
  machineId: number;
  name: string;
  code?: string;
  interfaceType?: string; // e.g. KDL
  status: string;
}

// ---- Query keys ------------------------------------------------------------

export const labQueryKeys = {
  orders: () => ["lab", "orders"] as const,
  order: (id: number) => ["lab", "orders", id] as const,
  workload: () => ["lab", "workload"] as const,
  covid: () => ["lab", "covid"] as const,
  machines: () => ["lab", "machines"] as const,
};

// ---- Orders ----------------------------------------------------------------

export function useLabOrders() {
  return useQuery<LabOrder[]>({
    queryKey: labQueryKeys.orders(),
    queryFn: () => labJson<LabOrder[]>(`${LAB_BASE}/api/v1/lab/orders`),
  });
}

export function useLabOrder(id: number) {
  return useQuery<LabOrder & { details?: LabDetail[] }>({
    queryKey: labQueryKeys.order(id),
    queryFn: () => labJson<LabOrder & { details?: LabDetail[] }>(`${LAB_BASE}/api/v1/lab/orders/${id}`),
    enabled: id > 0,
  });
}

export function useCreateLabOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      labJson<LabOrder>(`${LAB_BASE}/api/v1/lab/orders`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lab", "orders"] }),
  });
}

// ---- Order workflow actions ------------------------------------------------

export function useLabOrderAction(action: "collect" | "start" | "cancel") {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      labJson<LabOrder>(`${LAB_BASE}/api/v1/lab/orders/${id}/${action}`, { method: "POST" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["lab", "orders"] });
    },
  });
}

export function useEnterResults() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, detailId, results }: { id: number; detailId: number; results: unknown[] }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${id}/details/${detailId}/results`, {
        method: "POST",
        body: JSON.stringify({ results }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lab", "orders"] }),
  });
}

export function useFinalizeResult() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, detailId }: { id: number; detailId: number }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${id}/details/${detailId}/finalize`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lab", "orders"] }),
  });
}

export function useBarcode() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, detailId }: { id: number; detailId: number }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${id}/details/${detailId}/barcode`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lab", "orders"] }),
  });
}

export function useAcceptReject(action: "accept" | "reject") {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, detailId, reason }: { id: number; detailId: number; reason?: string }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${id}/details/${detailId}/${action}`, {
        method: "POST",
        ...(action === "reject" && reason ? { body: JSON.stringify({ reason }) } : {}),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lab", "orders"] }),
  });
}

export function useVerifyResult() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, detailId }: { id: number; detailId: number }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${id}/details/${detailId}/verify`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lab", "orders"] }),
  });
}

export function useNotifyCritical() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, detailId }: { id: number; detailId: number }) =>
      labJson<LabDetail>(`${LAB_BASE}/api/v1/lab/orders/${id}/details/${detailId}/notify-critical`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lab", "orders"] }),
  });
}

// ---- Workload / COVID ------------------------------------------------------

export function useLabWorkload() {
  return useQuery<WorkloadItem[]>({
    queryKey: labQueryKeys.workload(),
    queryFn: () => labJson<WorkloadItem[]>(`${LAB_BASE}/api/v1/lab/orders/workload`),
  });
}

export function useCovidOrders() {
  return useQuery<LabOrder[]>({
    queryKey: labQueryKeys.covid(),
    queryFn: () => labJson<LabOrder[]>(`${LAB_BASE}/api/v1/lab/orders/covid`),
  });
}

// ---- Machines (KDL interface) ----------------------------------------------

export function useMachines() {
  return useQuery<Machine[]>({
    queryKey: labQueryKeys.machines(),
    queryFn: () => labJson<Machine[]>(`${LAB_BASE}/api/v1/machines`),
  });
}

export function useCreateMachine() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      labJson<Machine>(`${LAB_BASE}/api/v1/machines`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lab", "machines"] }),
  });
}