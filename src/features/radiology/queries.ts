import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  RadOrder,
  RadOrderDetail,
  RadTemplate,
  RadStudyItem,
} from "./types";

const RAD_BASE: string = import.meta.env.VITE_RAD_API_BASE ?? "/api";

async function radJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Studies ---------------------------------------------------------------

export function useRadStudies() {
  return useQuery<RadStudyItem[]>({
    queryKey: ["rad", "studies"],
    queryFn: () => radJson<RadStudyItem[]>(`${RAD_BASE}/api/v1/radiology/studies`),
    placeholderData: (prev) => prev,
  });
}

// ---- Query keys ------------------------------------------------------------

export const radQueryKeys = {
  orders: () => ["rad", "orders"] as const,
  order: (id: number) => ["rad", "orders", id] as const,
  studies: () => ["rad", "studies"] as const,
  templates: () => ["rad", "templates"] as const,
  template: (id: number) => ["rad", "templates", id] as const,
};

// ---- Orders ----------------------------------------------------------------

export function useRadOrders(filters?: { status?: string; patientId?: number; modality?: string }) {
  const params = new URLSearchParams();
  if (filters?.status) params.set("status", filters.status);
  if (filters?.patientId) params.set("patientId", String(filters.patientId));
  if (filters?.modality) params.set("modality", filters.modality);
  const qs = params.toString();
  return useQuery<RadOrder[]>({
    queryKey: [...radQueryKeys.orders(), { filters: filters ?? {} }] as const,
    queryFn: () => radJson<RadOrder[]>(`${RAD_BASE}/api/v1/rad-orders${qs ? `?${qs}` : ""}`),
    placeholderData: (prev) => prev,
  });
}

export function useRadOrder(id: number) {
  return useQuery<RadOrder>({
    queryKey: radQueryKeys.order(id),
    queryFn: () => radJson<RadOrder>(`${RAD_BASE}/api/v1/rad-orders/${id}`),
    enabled: id > 0,
    placeholderData: (prev) => prev,
  });
}

export function useCreateRadOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      radJson<RadOrder>(`${RAD_BASE}/api/v1/rad-orders`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rad", "orders"] }),
  });
}

// ---- Order lifecycle -------------------------------------------------------

export function useRadOrderAction(action: "start" | "capture-image" | "cancel") {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      radJson<RadOrder>(`${RAD_BASE}/api/v1/rad-orders/${id}/${action}`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rad", "orders"] }),
  });
}

export function useReportRadOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reportText, reportedBy }: { id: number; reportText: string; reportedBy: string }) =>
      radJson<RadOrder>(`${RAD_BASE}/api/v1/rad-orders/${id}/report`, {
        method: "POST",
        body: JSON.stringify({ reportText, reportedBy }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rad", "orders"] }),
  });
}

// ---- Reschedule / Retest ---------------------------------------------------

export function useRescheduleLine() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, lineNo, rescheduledTo }: { orderId: number; lineNo: number; rescheduledTo: string }) =>
      radJson<RadOrderDetail>(`${RAD_BASE}/api/v1/radiology/orders/${orderId}/lines/${lineNo}/reschedule`, {
        method: "POST",
        body: JSON.stringify({ rescheduledTo }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rad", "orders"] }),
  });
}

export function useRequestRetest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, lineNo, reason }: { orderId: number; lineNo: number; reason: string }) =>
      radJson<RadOrderDetail>(`${RAD_BASE}/api/v1/radiology/orders/${orderId}/lines/${lineNo}/retest`, {
        method: "POST",
        body: JSON.stringify({ reason }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rad", "orders"] }),
  });
}

// ---- Templates -------------------------------------------------------------

export function useRadTemplates(modality?: string) {
  const qs = modality ? `?modality=${encodeURIComponent(modality)}` : "";
  return useQuery<RadTemplate[]>({
    queryKey: [...radQueryKeys.templates(), modality ?? null] as const,
    queryFn: () => radJson<RadTemplate[]>(`${RAD_BASE}/api/v1/radiology/templates${qs}`),
    placeholderData: (prev) => prev,
  });
}

export function useRadTemplate(id: number) {
  return useQuery<RadTemplate>({
    queryKey: radQueryKeys.template(id),
    queryFn: () => radJson<RadTemplate>(`${RAD_BASE}/api/v1/radiology/templates/${id}`),
    enabled: id > 0,
    placeholderData: (prev) => prev,
  });
}

export function useCreateRadTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      radJson<RadTemplate>(`${RAD_BASE}/api/v1/radiology/templates`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rad", "templates"] }),
  });
}

export function useUpdateRadOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      radJson<RadOrder>(`${RAD_BASE}/api/v1/rad-orders/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rad", "orders"] }),
  });
}

export type { RadOrder, RadOrderDetail, RadTemplate, RadStudyItem };
