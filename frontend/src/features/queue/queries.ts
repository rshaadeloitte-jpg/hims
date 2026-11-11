import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const QUEUE_BASE: string = import.meta.env.VITE_QUEUE_API_BASE ?? "";

async function queueJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface Counter {
  id?: number;
  code?: string;
  name?: string;
  departmentId?: number;
  doctorId?: number;
  status?: boolean;
  addedBy?: number;
  addedDatetime?: string;
  updatedBy?: number;
  updatedDatetime?: string;
  synchronizedd?: boolean;
  unitId?: number;

  // Legacy/compatibility properties
  counterId: number;
  counterName: string;
  cabinId?: number;
  isActive: boolean;
}

export interface QueueToken {
  id?: number;
  tokenId: number;
  counterId: number;
  patientId: number;
  tokenNo: number;
  departmentId?: number;
  priority: "NORMAL" | "EMERGENCY";
  queueStatus?: number;
  status: number; // 1=WAITING 2=CALLED 3=COMPLETED 4=NO_SHOW
  createdAt: string;
  calledAt?: string;
  completedAt?: string;
  queueDate?: string;
  visitRef?: string;
}

export interface TokenDashboard {
  counterId: number;
  counterName: string;
  nowServing: QueueToken | null;
  waiting: QueueToken[];
  called: QueueToken[];
}

export interface DepartmentDashboard {
  departmentId: number;
  departmentName: string;
  waiting: number;
  called: number;
  nowServing: number;
  avgWaitMinutes: number;
  counters: { counterId: number; counterName: string; nowServing: number; waitingCount: number }[];
}

export interface DoctorBatchItem {
  tokenId: number;
  tokenNo: number;
  patientId: number;
  patientName?: string;
  priority: "NORMAL" | "EMERGENCY";
  status: number;
  createdAt: string;
  calledAt?: string;
}

export interface WaitTimeEstimate {
  counterId: number;
  avgWaitMinutes: number;
  currentWaiting: number;
  estimatedWaitMinutes: number;
}

export interface QueueEvent {
  type: "TOKEN_ISSUED" | "TOKEN_CALLED" | "TOKEN_COMPLETED" | "TOKEN_NO_SHOW" | "TOKEN_TRANSFERRED" | "COUNTER_UPDATED";
  tokenId?: number;
  counterId?: number;
  tokenNo?: number;
  timestamp: string;
}

// ---- Query keys ------------------------------------------------------------

export const queueQueryKeys = {
  counters: () => ["queue", "counters"] as const,
  dashboard: () => ["queue", "dashboard"] as const,
  deptDashboard: (deptId: number) => ["queue", "department", deptId] as const,
  cabinBoard: (counterId: number) => ["queue", "cabin", counterId] as const,
  tokens: () => ["queue", "tokens"] as const,
  deptTokens: (deptId: number) => ["queue", "tokens", "department", deptId] as const,
  doctorBatch: (doctorId: number) => ["queue", "doctor", doctorId, "batch"] as const,
  waitTime: (counterId: number) => ["queue", "wait-time", counterId] as const,
};

// ---- Counters --------------------------------------------------------------

export function useCounters() {
  return useQuery<Counter[]>({
    queryKey: queueQueryKeys.counters(),
    queryFn: async () => {
      const data = await queueJson<Counter[]>(`${QUEUE_BASE}/api/v1/queue/counters`);
      return (data || []).map((c) => {
        const id = c.id ?? c.counterId ?? 0;
        const code = c.code ?? "";
        const name = c.name ?? c.counterName ?? "";
        return {
          ...c,
          id,
          code,
          name,
          counterId: c.counterId ?? id,
          counterName: c.counterName ?? (code ? `${code} - ${name}` : name) ?? `Counter ${id}`,
          isActive: c.isActive ?? c.status ?? true,
        };
      });
    },
  });
}

export function useCreateCounter() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      queueJson<Counter>(`${QUEUE_BASE}/api/v1/queue/counters`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["queue", "counters"] }),
  });
}

// ---- Tokens ----------------------------------------------------------------

export function useTokens() {
  return useQuery<QueueToken[]>({
    queryKey: queueQueryKeys.tokens(),
    queryFn: async () => {
      const data = await queueJson<QueueToken[]>(`${QUEUE_BASE}/api/v1/queue/tokens`);
      return (data || []).map((t) => {
        const id = t.id ?? t.tokenId ?? 0;
        const qStatus =
          typeof t.queueStatus === "number"
            ? t.queueStatus
            : typeof t.status === "number"
              ? t.status
              : 1;
        return {
          ...t,
          id,
          tokenId: t.tokenId ?? id,
          counterId: t.counterId ?? 0,
          queueStatus: qStatus,
          status: qStatus,
          priority:
            (t.priority as unknown) === 2 || t.priority === "EMERGENCY"
              ? "EMERGENCY"
              : "NORMAL",
          createdAt: t.createdAt ?? (t as { addedDatetime?: string }).addedDatetime ?? new Date().toISOString(),
        };
      });
    },
  });
}

export function useDepartmentTokens(departmentId: number) {
  return useQuery<QueueToken[]>({
    queryKey: queueQueryKeys.deptTokens(departmentId),
    queryFn: () => queueJson<QueueToken[]>(`${QUEUE_BASE}/api/v1/queue/department/${departmentId}/tokens`),
    enabled: departmentId > 0,
  });
}

export function useIssueToken() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) => {
      const body = { ...payload };
      if (body.priority === "EMERGENCY") body.priority = 2;
      else if (body.priority === "NORMAL") body.priority = 1;
      return queueJson<QueueToken>(`${QUEUE_BASE}/api/v1/queue/tokens`, {
        method: "POST",
        body: JSON.stringify(body),
      });
    },
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["queue", "tokens"] });
      void qc.invalidateQueries({ queryKey: ["queue", "dashboard"] });
    },
  });
}

export function useCallNext(counterId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () =>
      queueJson<QueueToken>(`${QUEUE_BASE}/api/v1/queue/counters/${counterId}/call-next`, { method: "POST" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["queue"] });
    },
  });
}

export function useCallPatient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (tokenId: number) =>
      queueJson<QueueToken>(`${QUEUE_BASE}/api/v1/queue/tokens/${tokenId}/call`, { method: "POST" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["queue"] });
    },
  });
}

export function useTokenAction(action: "transfer" | "no-show" | "complete") {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (tokenId: number) =>
      queueJson<QueueToken>(`${QUEUE_BASE}/api/v1/queue/tokens/${tokenId}/${action}`, { method: "POST" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["queue", "tokens"] });
      void qc.invalidateQueries({ queryKey: ["queue", "dashboard"] });
    },
  });
}

export function useTransferToken() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ tokenId, targetCounterId }: { tokenId: number; targetCounterId: number }) =>
      queueJson<QueueToken>(`${QUEUE_BASE}/api/v1/queue/tokens/${tokenId}/transfer`, {
        method: "POST",
        body: JSON.stringify({ targetCounterId }),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["queue", "tokens"] });
      void qc.invalidateQueries({ queryKey: ["queue", "dashboard"] });
    },
  });
}

// ---- Dashboards ------------------------------------------------------------

export function useDashboard(refetchInterval = 5000) {
  return useQuery<TokenDashboard[]>({
    queryKey: queueQueryKeys.dashboard(),
    queryFn: () => queueJson<TokenDashboard[]>(`${QUEUE_BASE}/api/v1/queue/dashboard`),
    refetchInterval,
  });
}

export function useDepartmentDashboard(departmentId: number) {
  return useQuery<DepartmentDashboard>({
    queryKey: queueQueryKeys.deptDashboard(departmentId),
    queryFn: () => queueJson<DepartmentDashboard>(`${QUEUE_BASE}/api/v1/queue/department/${departmentId}/dashboard`),
    enabled: departmentId > 0,
    refetchInterval: 5000,
  });
}

export function useCabinBoard(counterId: number) {
  return useQuery<TokenDashboard>({
    queryKey: queueQueryKeys.cabinBoard(counterId),
    queryFn: () => queueJson<TokenDashboard>(`${QUEUE_BASE}/api/v1/queue/cabin/${counterId}/board`),
    enabled: counterId > 0,
    refetchInterval: 3000,
  });
}

// ---- Doctor batch ----------------------------------------------------------

export function useDoctorBatch(doctorId: number) {
  return useQuery<DoctorBatchItem[]>({
    queryKey: queueQueryKeys.doctorBatch(doctorId),
    queryFn: () => queueJson<DoctorBatchItem[]>(`${QUEUE_BASE}/api/v1/queue/doctor/${doctorId}/batch`),
    enabled: doctorId > 0,
    refetchInterval: 5000,
  });
}

// ---- Wait time -------------------------------------------------------------

export function useWaitTime(counterId: number) {
  return useQuery<WaitTimeEstimate>({
    queryKey: queueQueryKeys.waitTime(counterId),
    queryFn: () => queueJson<WaitTimeEstimate>(`${QUEUE_BASE}/api/v1/queue/counters/${counterId}/wait-time`),
    enabled: counterId > 0,
    refetchInterval: 10000,
  });
}