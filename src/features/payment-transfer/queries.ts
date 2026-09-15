import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M33 Payment Transfer hooks — consume the payment-transfer REST API.
 * DBT (Direct Benefit Transfer) payment workflows with Stripe-like payment form patterns.
 */

const PAYMENT_TRANSFER_BASE: string = import.meta.env.VITE_PAYMENT_TRANSFER_API_BASE ?? "/api";

async function paymentTransferJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface TransferRecord {
  transferId: string;
  beneficiaryName: string;
  beneficiaryAccountNo: string;
  beneficiaryIfsc: string;
  amount: number;
  scheme: string;
  purpose: string;
  remarks?: string;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "CANCELLED";
  utrNo?: string;
  failureReason?: string;
  createdAt: string;
  updatedAt?: string;
  completedAt?: string;
}

export interface TransferRequestPayload {
  beneficiaryName: string;
  beneficiaryAccountNo: string;
  beneficiaryIfsc: string;
  amount: number;
  scheme: string;
  purpose: string;
  remarks?: string;
}

export interface OtpVerificationPayload {
  transferId: string;
  otp: string;
}

export interface TransferStatusFilter {
  fromDate?: string;
  toDate?: string;
  status?: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "CANCELLED";
  page?: number;
  size?: number;
}

export interface PaginatedTransfers {
  content: TransferRecord[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

// ---- Query keys ------------------------------------------------------------

export const paymentTransferQueryKeys = {
  all: ["payment-transfers"] as const,
  list: (filter: TransferStatusFilter) => ["payment-transfers", "list", filter] as const,
  detail: (id: string) => ["payment-transfers", id] as const,
  schemes: ["payment-transfers", "schemes"] as const,
};

// ---- Schemes list ----------------------------------------------------------

export function useTransferSchemes() {
  return useQuery<string[]>({
    queryKey: paymentTransferQueryKeys.schemes,
    queryFn: () => paymentTransferJson<string[]>(`${PAYMENT_TRANSFER_BASE}/api/v1/payment-transfer/schemes`),
    staleTime: 10 * 60 * 1000,
  });
}

// ---- Transfer list ---------------------------------------------------------

export function useTransferList(filter: TransferStatusFilter) {
  return useQuery<PaginatedTransfers>({
    queryKey: paymentTransferQueryKeys.list(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter.fromDate) params.set("fromDate", filter.fromDate);
      if (filter.toDate) params.set("toDate", filter.toDate);
      if (filter.status) params.set("status", filter.status);
      params.set("page", String(filter.page ?? 0));
      params.set("size", String(filter.size ?? 20));
      return paymentTransferJson<PaginatedTransfers>(
        `${PAYMENT_TRANSFER_BASE}/api/v1/payment-transfer?${params}`,
      );
    },
  });
}

// ---- Single transfer -------------------------------------------------------

export function useTransferDetail(transferId: string) {
  return useQuery<TransferRecord>({
    queryKey: paymentTransferQueryKeys.detail(transferId),
    queryFn: () =>
      paymentTransferJson<TransferRecord>(
        `${PAYMENT_TRANSFER_BASE}/api/v1/payment-transfer/${transferId}`,
      ),
    enabled: !!transferId,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data && (data.status === "PENDING" || data.status === "PROCESSING")) {
        return 3000;
      }
      return false;
    },
  });
}

// ---- Initiate transfer (request) -------------------------------------------

export function useInitiateTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: TransferRequestPayload) =>
      paymentTransferJson<TransferRecord>(`${PAYMENT_TRANSFER_BASE}/api/v1/payment-transfer`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: paymentTransferQueryKeys.all });
    },
  });
}

// ---- Verify OTP ------------------------------------------------------------

export function useVerifyOtp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: OtpVerificationPayload) =>
      paymentTransferJson<TransferRecord>(
        `${PAYMENT_TRANSFER_BASE}/api/v1/payment-transfer/${payload.transferId}/verify`,
        {
          method: "POST",
          body: JSON.stringify({ otp: payload.otp }),
        },
      ),
    onSuccess: (_, { transferId }) => {
      void qc.invalidateQueries({ queryKey: paymentTransferQueryKeys.detail(transferId) });
    },
  });
}

// ---- Resend OTP ------------------------------------------------------------

export function useResendOtp() {
  return useMutation({
    mutationFn: ({ transferId }: { transferId: string }) =>
      paymentTransferJson<{ success: boolean }>(
        `${PAYMENT_TRANSFER_BASE}/api/v1/payment-transfer/${transferId}/resend-otp`,
        { method: "POST" },
      ),
  });
}

// ---- Cancel transfer -------------------------------------------------------

export function useCancelTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ transferId, reason }: { transferId: string; reason: string }) =>
      paymentTransferJson<TransferRecord>(
        `${PAYMENT_TRANSFER_BASE}/api/v1/payment-transfer/${transferId}/cancel`,
        {
          method: "POST",
          body: JSON.stringify({ reason }),
        },
      ),
    onSuccess: (_, { transferId }) => {
      void qc.invalidateQueries({ queryKey: paymentTransferQueryKeys.detail(transferId) });
      void qc.invalidateQueries({ queryKey: paymentTransferQueryKeys.all });
    },
  });
}
