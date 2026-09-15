import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const RCM_BASE: string = import.meta.env.VITE_RCM_API_BASE ?? "/api";

export async function rcmJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface Claim {
  id: number;
  patientName: string;
  scheme: string;
  amount: number;
  status: string;
  submittedAt: string;
}

export interface DMApprovalRecord extends Claim {
  approvalStage: "pending" | "dlh_approval" | "dm_approval" | "approved" | "rejected";
}

export interface DischargeBill {
  id: number;
  patientId: number;
  amount: number;
  status: "unpaid" | "partial" | "settled" | "approved";
}

export interface ChiranjeeviScheme {
  id: number;
  patientName: string;
  patientId: number;
  schemeName: string;
  eligibilityDate: string;
  approvalStatus: "pending" | "under_review" | "approved" | "rejected";
  amount: number;
}

export function useClaims() {
  return useQuery({ queryKey: ["rcm-claims"], queryFn: () => rcmJson<Claim[]>(`${RCM_BASE}/v1/rcm/claims`) });
}

export function useSubmitClaim() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Partial<Claim>) =>
      rcmJson<Claim>(`${RCM_BASE}/v1/rcm/claims`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rcm-claims"] }),
  });
}

export function useClaimDetail(claimId: number) {
  return useQuery({
    queryKey: ["rcm-claim", claimId],
    queryFn: () => rcmJson<Claim>(`${RCM_BASE}/v1/rcm/claims/${claimId}`),
    enabled: !!claimId,
  });
}

export function useApproveClaim() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (claimId: number) =>
      rcmJson<Claim>(`${RCM_BASE}/v1/rcm/claims/${claimId}/approve`, { method: "PATCH" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rcm-claims"] }),
  });
}

export function useRejectClaim() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (claimId: number) =>
      rcmJson<Claim>(`${RCM_BASE}/v1/rcm/claims/${claimId}/reject`, { method: "PATCH" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rcm-claims"] }),
  });
}

export function useDischargeBills(patientId: number) {
  return useQuery({
    queryKey: ["rcm-discharge", patientId],
    queryFn: () => rcmJson<DischargeBill[]>(`${RCM_BASE}/v1/rcm/patients/${patientId}/bills`),
    enabled: !!patientId,
  });
}

export function useClearDischarge() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (patientId: number) =>
      rcmJson<{ success: boolean }>(`${RCM_BASE}/v1/rcm/patients/${patientId}/discharge`, { method: "POST" }),
    onSuccess: (_d, patientId) => qc.invalidateQueries({ queryKey: ["rcm-discharge", patientId] }),
  });
}

export function useBlockDischarge() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (patientId: number) =>
      rcmJson<{ success: boolean }>(`${RCM_BASE}/v1/rcm/patients/${patientId}/block-discharge`, { method: "POST" }),
    onSuccess: (_d, patientId) => qc.invalidateQueries({ queryKey: ["rcm-discharge", patientId] }),
  });
}

export function useChiranjeeviSchemes() {
  return useQuery({ queryKey: ["rcm-chiranjeevi"], queryFn: () => rcmJson<ChiranjeeviScheme[]>(`${RCM_BASE}/v1/rcm/chiranjeevi/schemes`) });
}

export function useApproveScheme() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (schemeId: number) =>
      rcmJson<ChiranjeeviScheme>(`${RCM_BASE}/v1/rcm/chiranjeevi/schemes/${schemeId}/approve`, { method: "PATCH" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rcm-chiranjeevi"] }),
  });
}

export function useRejectScheme() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (schemeId: number) =>
      rcmJson<ChiranjeeviScheme>(`${RCM_BASE}/v1/rcm/chiranjeevi/schemes/${schemeId}/reject`, { method: "PATCH" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["rcm-chiranjeevi"] }),
  });
}
