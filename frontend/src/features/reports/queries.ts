import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const REPORTING_BASE: string = import.meta.env.VITE_REPORTING_API_BASE ?? "/api";

async function reportJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface ReportMaster {
  reportId: number;
  reportCode: string;
  title: string;
  kind: number; // 1=TABULAR 2=SUMMARY
  definition: string; // JSON string
  isActive: boolean;
}

export interface ReportExecution {
  executionId: number;
  reportId: number;
  params: string; // JSON string
  status: number; // 0=QUEUED 1=DONE 2=FAILED
  rowCount: number;
  result: string; // JSON string (rows for TABULAR, aggregate value for SUMMARY)
  executedAt: string;
}

export interface DashboardCounts {
  patients: number;
  encounters: number;
  revenueTotal: number;
}

export interface DashboardTrendPoint {
  label: string;
  value: number;
}

// ---- Query keys ------------------------------------------------------------

export const reportQueryKeys = {
  reports: () => ["reports", "master"] as const,
  executions: (reportCode?: string, status?: string) => ["reports", "executions", { reportCode, status }] as const,
  execution: (id: number) => ["reports", "executions", id] as const,
  dashboardCounts: (asOf?: string) => ["reports", "dashboard", "counts", asOf] as const,
  dashboardTrend: (metric: string, window?: string) => ["reports", "dashboard", "trend", metric, window] as const,
};

// ---- Reports ---------------------------------------------------------------

export function useReports(active?: boolean) {
  return useQuery<ReportMaster[]>({
    queryKey: reportQueryKeys.reports(),
    queryFn: () => {
      const params = new URLSearchParams();
      if (active !== undefined) params.set("active", String(active));
      return reportJson<ReportMaster[]>(`${REPORTING_BASE}/api/v1/reports?${params}`);
    },
  });
}

export function useCreateReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      reportJson<ReportMaster>(`${REPORTING_BASE}/api/v1/reports`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["reports", "master"] }),
  });
}

export function useExecuteReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ code, params }: { code: string; params?: Record<string, unknown> }) =>
      reportJson<ReportExecution>(`${REPORTING_BASE}/api/v1/reports/${code}/execute`, {
        method: "POST",
        body: JSON.stringify({ params: params ?? {} }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["reports", "executions"] }),
  });
}

// ---- Executions ------------------------------------------------------------

export function useReportExecutions(reportCode?: string, status?: string) {
  return useQuery<ReportExecution[]>({
    queryKey: reportQueryKeys.executions(reportCode, status),
    queryFn: () => {
      const params = new URLSearchParams();
      if (reportCode) params.set("reportCode", reportCode);
      if (status) params.set("status", status);
      return reportJson<ReportExecution[]>(`${REPORTING_BASE}/api/v1/reports/executions?${params}`);
    },
  });
}

export function useReportExecution(id: number) {
  return useQuery<ReportExecution>({
    queryKey: reportQueryKeys.execution(id),
    queryFn: () => reportJson<ReportExecution>(`${REPORTING_BASE}/api/v1/reports/executions/${id}`),
    enabled: id > 0,
  });
}

// ---- Dashboard -------------------------------------------------------------

export function useDashboardCounts(asOf?: string) {
  return useQuery<DashboardCounts>({
    queryKey: reportQueryKeys.dashboardCounts(asOf),
    queryFn: () => {
      const params = new URLSearchParams();
      if (asOf) params.set("asOf", asOf);
      return reportJson<DashboardCounts>(`${REPORTING_BASE}/api/v1/dashboard/counts?${params}`);
    },
  });
}

export function useDashboardTrend(metric: string, window?: string) {
  return useQuery<DashboardTrendPoint[]>({
    queryKey: reportQueryKeys.dashboardTrend(metric, window),
    queryFn: () => {
      const params = new URLSearchParams();
      if (window) params.set("window", window);
      return reportJson<DashboardTrendPoint[]>(`${REPORTING_BASE}/api/v1/dashboard/trend/${metric}?${params}`);
    },
  });
}

// ---- Result rendering helpers ----------------------------------------------

/** Parse a report execution's result JSON into a usable shape. */
export function parseReportResult(execution: ReportExecution): { rows: Record<string, unknown>[]; summary: string | null } {
  try {
    const raw = JSON.parse(execution.result ?? "null");
    if (Array.isArray(raw)) return { rows: raw as Record<string, unknown>[], summary: null };
    if (raw && typeof raw === "object" && "count" in raw) return { rows: [], summary: String(raw.count) };
    return { rows: [], summary: raw == null ? null : JSON.stringify(raw) };
  } catch {
    return { rows: [], summary: execution.result ?? null };
  }
}
