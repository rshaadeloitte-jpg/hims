import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const QUEUE_BASE: string = import.meta.env.VITE_QUEUE_API_BASE ?? "/api";

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
  counterId: number;
  counterName: string;
  departmentId?: number;
  cabinId?: number;
  isActive: boolean;
}

export interface QueueToken {
  tokenId: number;
  counterId: number;
  patientId: number;
  tokenNo: number;
  priority: "NORMAL" | "EMERGENCY";
  status: number; // 1=WAITING 2=CALLED 3=COMPLETED 4=NO_SHOW
  createdAt: string;
  calledAt?: string;
  completedAt?: string;
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
    queryFn: () => queueJson<Counter[]>(`${QUEUE_BASE}/api/v1/queue/counters`),
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
    queryFn: () => queueJson<QueueToken[]>(`${QUEUE_BASE}/api/v1/queue/tokens`),
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
    mutationFn: (payload: Record<string, unknown>) =>
      queueJson<QueueToken>(`${QUEUE_BASE}/api/v1/queue/tokens`, { method: "POST", body: JSON.stringify(payload) }),
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