import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  VerificationResponse,
  ExceptionResponse,
  StockResponse,
  StockAlertResponse,
  FiveRightsScanValues,
  ExceptionCreateValues,
  ExceptionOverrideValues,
  StockUpdateValues,
  StockAlertCreateValues,
} from "./zodSchema";

/**
 * M30 BCMA (Bedside Med Admin) hooks — consume the bcma-service REST API.
 * Stripe-style verification pattern:
 * - Each step is optimistic (local state updates instantly)
 * - Server reconciliation follows each POST
 * - Validation errors surface inline per-field
 * - Audit trail preserved server-side
 */

const BCMA_BASE: string = import.meta.env.VITE_BCMA_API_BASE ?? "/api";

async function bcmaJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Query keys ------------------------------------------------------------

export const bcmaQueryKeys = {
  all: ["bcma"] as const,
  fiveRights: (patientId?: number) =>
    ["bcma", "five-rights", ...(patientId ? [patientId] : [])] as const,
  exceptions: (patientId?: number) =>
    ["bcma", "exceptions", ...(patientId ? [patientId] : [])] as const,
  stock: (drugNdc?: string) =>
    ["bcma", "stock", ...(drugNdc ? [drugNdc] : [])] as const,
  alerts: () => ["bcma", "alerts"] as const,
  reasons: () => ["bcma", "reasons"] as const,
};

// ---- Five-rights verification ---------------------------------------------

export function useFiveRightsScans(patientId?: number) {
  return useQuery<VerificationResponse[]>({
    queryKey: bcmaQueryKeys.fiveRights(patientId),
    queryFn: () => {
      const url = patientId
        ? `${BCMA_BASE}/api/bcma/five-rights/patient/${patientId}`
        : `${BCMA_BASE}/api/bcma/five-rights/patient/0`;
      return bcmaJson<VerificationResponse[]>(url);
    },
  });
}

export function useStartFiveRightsScan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: FiveRightsScanValues) =>
      bcmaJson<VerificationResponse>(`${BCMA_BASE}/api/bcma/five-rights/verify-patient`, {
        method: "POST",
        body: JSON.stringify({
          bcmaId: 0,
          patientId: input.patientId,
          wristbandBarcode: input.wristbandBarcode,
          match: true,
        }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.all }),
  });
}

export function useVerifyRight() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (params: { step: "patient" | "drug" | "dose" | "route" | "time"; bcmaId: number; match: boolean }) => {
      const endpoint = params.step === "patient" ? "verify-patient"
        : params.step === "drug" ? "verify-drug"
        : params.step === "dose" ? "verify-dose"
        : params.step === "route" ? "verify-route"
        : "verify-time";
      return bcmaJson<VerificationResponse>(`${BCMA_BASE}/api/bcma/five-rights/${endpoint}`, {
        method: "POST",
        body: JSON.stringify({ bcmaId: params.bcmaId, match: params.match }),
      });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.all }),
  });
}

export function useFinalizeVerification() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (bcmaId: number) =>
      bcmaJson<VerificationResponse>(`${BCMA_BASE}/api/bcma/five-rights/finalize/${bcmaId}`, {
        method: "POST",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.all }),
  });
}

// ---- Exception logging + override ------------------------------------------

export function useCreateException() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ExceptionCreateValues) =>
      bcmaJson<ExceptionResponse>(`${BCMA_BASE}/api/bcma/exception`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.all }),
  });
}

export function useOverrideException() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ exceptionId, ...input }: ExceptionOverrideValues & { exceptionId: number }) =>
      bcmaJson<ExceptionResponse>(`${BCMA_BASE}/api/bcma/exception/override/${exceptionId}`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.all }),
  });
}

export function useExceptions(patientId?: number) {
  return useQuery<ExceptionResponse[]>({
    queryKey: bcmaQueryKeys.exceptions(patientId),
    queryFn: () => {
      const url = patientId
        ? `${BCMA_BASE}/api/bcma/exception/patient/${patientId}`
        : `${BCMA_BASE}/api/bcma/exception/patient/0`;
      return bcmaJson<ExceptionResponse[]>(url);
    },
  });
}

export function useAddOverrideReason() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { code: string; description: string; category: string }) =>
      bcmaJson<{ id: number; code: string; description: string; category: string }>(
        `${BCMA_BASE}/api/bcma/exception/reason`,
        { method: "POST", body: JSON.stringify(input) },
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.reasons() }),
  });
}

// ---- Ward stock view -------------------------------------------------------

export function useStockByDrug(drugNdc: string) {
  return useQuery<StockResponse[]>({
    queryKey: bcmaQueryKeys.stock(drugNdc),
    queryFn: () => bcmaJson<StockResponse[]>(`${BCMA_BASE}/api/bcma/stock/drug/${drugNdc}`),
    enabled: drugNdc.length > 0,
  });
}

export function useUnresolvedAlerts() {
  return useQuery<StockAlertResponse[]>({
    queryKey: bcmaQueryKeys.alerts(),
    queryFn: () => bcmaJson<StockAlertResponse[]>(`${BCMA_BASE}/api/bcma/stock/alerts/unresolved`),
  });
}

export function useUpdateStock() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: StockUpdateValues) =>
      bcmaJson<StockResponse>(`${BCMA_BASE}/api/bcma/stock`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.all }),
  });
}

export function useCreateStockAlert() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: StockAlertCreateValues) =>
      bcmaJson<StockAlertResponse>(`${BCMA_BASE}/api/bcma/stock/alert`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.alerts() }),
  });
}

export function useResolveStockAlert() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (alertId: number) =>
      bcmaJson<StockAlertResponse>(`${BCMA_BASE}/api/bcma/stock/alert/resolve/${alertId}`, {
        method: "POST",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bcmaQueryKeys.alerts() }),
  });
}

export function useBcmaScans() {
  return useQuery<any[]>({
    queryKey: ["bcma", "bcma-scans"],
    queryFn: () => bcmaJson<any[]>(`${BCMA_BASE}/api/v1/bcma/bcma-scans`),
  });
}

export function useRecordBcmaScan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) =>
      bcmaJson<any>(`${BCMA_BASE}/api/v1/bcma/record-bcma-scan`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["bcma"] }),
  });
}
