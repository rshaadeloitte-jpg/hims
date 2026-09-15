import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M37 Aasmaan hooks — consume the aasmaan-service REST API.
 * Uses Vite proxy (VITE_AASMAAN_API_BASE) or production gateway.
 */

const AASMAAN_BASE: string = import.meta.env.VITE_AASMAAN_API_BASE ?? "/api";

async function aasmaanJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface AasmaanCase {
  caseId: number;
  patientId: number;
  caseType: string;
  status: number; // 1=OPEN, 2=IN_PROGRESS, 3=CLOSED
  openedOn?: string;
  closedOn?: string;
}

export interface AasmaanETicket {
  ticketId: number;
  caseId?: number;
  subject: string;
  priority: string;
  status: string;
  createdAt?: string;
}

export interface AasmaanMonitor {
  monitorId: number;
  caseId: number;
  metric: string;
  value: number;
  recordedAt?: string;
}

export interface AasmaanDevice {
  deviceId: number;
  deviceName: string;
  deviceType: string;
  serialNo: string;
  status: string;
  lastSeenAt?: string;
}

export interface CaseFilter {
  status?: string;
  caseType?: string;
  patientId?: number;
  page?: number;
  size?: number;
}

// ---- Query keys ------------------------------------------------------------

export const aasmaanQueryKeys = {
  cases: (filter: CaseFilter) => ["aasmaan", "cases", filter] as const,
  case: (id: number) => ["aasmaan", "cases", id] as const,
  eTickets: () => ["aasmaan", "e-tickets"] as const,
  eTicket: (id: number) => ["aasmaan", "e-tickets", id] as const,
  monitors: (caseId?: number) => ["aasmaan", "monitors", caseId ?? null] as const,
  devices: () => ["aasmaan", "devices"] as const,
  device: (id: number) => ["aasmaan", "devices", id] as const,
  workload: () => ["aasmaan", "workload"] as const,
};

// ---- Cases -----------------------------------------------------------------

export function useCases(filter: CaseFilter) {
  return useQuery<AasmaanCase[]>({
    queryKey: aasmaanQueryKeys.cases(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter.status) params.set("status", filter.status);
      if (filter.caseType) params.set("caseType", filter.caseType);
      if (filter.patientId) params.set("patientId", String(filter.patientId));
      params.set("page", String(filter.page ?? 0));
      params.set("size", String(filter.size ?? 20));
      return aasmaanJson<AasmaanCase[]>(`${AASMAAN_BASE}/cases?${params}`);
    },
  });
}

export function useCase(id: number) {
  return useQuery<AasmaanCase>({
    queryKey: aasmaanQueryKeys.case(id),
    queryFn: () => aasmaanJson<AasmaanCase>(`${AASMAAN_BASE}/cases/${id}`),
    enabled: id > 0,
  });
}

export function useCreateCase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      aasmaanJson<AasmaanCase>(`${AASMAAN_BASE}/cases`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["aasmaan", "cases"] }),
  });
}

export function useUpdateCase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      aasmaanJson<AasmaanCase>(`${AASMAAN_BASE}/cases/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: aasmaanQueryKeys.case(id) });
      void qc.invalidateQueries({ queryKey: ["aasmaan", "cases"] });
    },
  });
}

export function useDeleteCase() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      aasmaanJson<void>(`${AASMAAN_BASE}/cases/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["aasmaan", "cases"] }),
  });
}

// ---- E-Tickets -------------------------------------------------------------

export function useETickets() {
  return useQuery<AasmaanETicket[]>({
    queryKey: aasmaanQueryKeys.eTickets(),
    queryFn: () => aasmaanJson<AasmaanETicket[]>(`${AASMAAN_BASE}/e-tickets`),
  });
}

export function useCreateETicket() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      aasmaanJson<AasmaanETicket>(`${AASMAAN_BASE}/e-tickets`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["aasmaan", "e-tickets"] }),
  });
}

// ---- Monitors (IoT metrics) ------------------------------------------------

export function useMonitors(caseId?: number) {
  return useQuery<AasmaanMonitor[]>({
    queryKey: aasmaanQueryKeys.monitors(caseId),
    queryFn: () => {
      const url = caseId
        ? `${AASMAAN_BASE}/monitor?caseId=${caseId}`
        : `${AASMAAN_BASE}/monitor`;
      return aasmaanJson<AasmaanMonitor[]>(url);
    },
  });
}

export function useRecordMonitor() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      aasmaanJson<AasmaanMonitor>(`${AASMAAN_BASE}/monitor`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, vars) => {
      const caseId = (vars as { caseId?: number }).caseId;
      void qc.invalidateQueries({ queryKey: aasmaanQueryKeys.monitors(caseId) });
    },
  });
}

// ---- Devices (IoT device management) ---------------------------------------

export function useDevices() {
  return useQuery<AasmaanDevice[]>({
    queryKey: aasmaanQueryKeys.devices(),
    queryFn: () => aasmaanJson<AasmaanDevice[]>(`${AASMAAN_BASE}/devices`),
  });
}

export function useDevice(id: number) {
  return useQuery<AasmaanDevice>({
    queryKey: aasmaanQueryKeys.device(id),
    queryFn: () => aasmaanJson<AasmaanDevice>(`${AASMAAN_BASE}/devices/${id}`),
    enabled: id > 0,
  });
}

export function useCreateDevice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      aasmaanJson<AasmaanDevice>(`${AASMAAN_BASE}/devices`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["aasmaan", "devices"] }),
  });
}

export function useUpdateDevice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      aasmaanJson<AasmaanDevice>(`${AASMAAN_BASE}/devices/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["aasmaan", "devices"] }),
  });
}

export function useDeleteDevice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      aasmaanJson<void>(`${AASMAAN_BASE}/devices/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["aasmaan", "devices"] }),
  });
}

// ---- Workload stats --------------------------------------------------------

export function useFieldWorkerStats() {
  return useQuery<{ totalCases: number; openCases: number; closedToday: number; activeDevices: number }>({
    queryKey: aasmaanQueryKeys.workload(),
    queryFn: () =>
      aasmaanJson<{ totalCases: number; openCases: number; closedToday: number; activeDevices: number }>(
        `${AASMAAN_BASE}/stats/workload`,
      ),
  });
}

// ---- Legacy field sync (existing hook) -------------------------------------

export interface FieldRecord {
  id: number;
  fieldWorker: string;
  village: string;
  syncStatus: string;
  syncedAt: string;
}

export function useFieldRecords() {
  return useQuery({
    queryKey: ["aasmaan-records"],
    queryFn: () => aasmaanJson<FieldRecord[]>(`${AASMAAN_BASE}/v1/aasmaan/records`),
  });
}
