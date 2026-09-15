import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type { GrievanceFormValues, FeedbackFormValues, StatusChangeValues } from "./zodSchema";

/**
 * M22 Grievance hooks — consume the grievance-service REST API (Spring Boot :8083).
 * Uses the Vite proxy (VITE_GRIEVANCE_API_BASE) or production gateway.
 */

const GRV_BASE: string = import.meta.env.VITE_GRIEVANCE_API_BASE ?? "/api";

async function grvJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface GrievanceTicket {
  id: number;
  ticketNo: string;
  issueDate: string;
  mrno?: string;
  name: string;
  address?: string;
  mobileNo?: number;
  categoryId: number;
  subCategoryIds: number[];
  issue: string;
  issueStatus: number;
  resolvedDate?: string;
  ticketCloseStatus?: number;
  feedbackId?: number;
  severityId: number;
  assignedUserId: number;
  attachedFileName?: string;
  expectedResolutionDate?: string;
  unitId?: number;
  isCitizen: boolean;
}

export interface GrievanceSeverity {
  id: number;
  code: string;
  description: string;
}

export interface GrievanceCategory {
  id: number;
  code: string;
  description: string;
  isCitizen: boolean;
}

export interface GrievanceSubCategory {
  id: number;
  categoryId: number;
  code: string;
  description: string;
}

export interface GrievanceFeedback {
  id: number;
  grievanceId: number;
  question1: number;
  question2: number;
  question3: number;
  question4: number;
  question5: number;
  freeTextFeedback?: string;
  email?: string;
}

export interface GrievanceComment {
  id: number;
  grievanceId: number;
  comment: string;
  userName?: string;
}

export interface TicketCounts {
  TOTAL: number;
  NEW: number;
  ASSIGNED: number;
  RESOLVED: number;
  RAISED_BY_CITIZEN: number;
  MY_LIST: number;
  TODAYS_TICKET: number;
}

export interface GrievanceFilter {
  unitId?: number;
  mobileNo?: number;
  status?: string;
  severityId?: number;
  categoryId?: number;
  fromDate?: string;
  toDate?: string;
}

// ---- Query keys ------------------------------------------------------------

export const grievanceQueryKeys = {
  tickets: (filter?: GrievanceFilter) => ["grievances", filter] as const,
  ticket: (id: number) => ["grievances", id] as const,
  severities: () => ["grievances", "severities"] as const,
  categories: () => ["grievances", "categories"] as const,
  subCategories: (categoryId?: number) => ["grievances", "sub-categories", categoryId] as const,
  feedback: (grievanceId: number) => ["grievances", grievanceId, "feedback"] as const,
  comments: (grievanceId: number) => ["grievances", grievanceId, "comments"] as const,
  counts: () => ["grievances", "counts"] as const,
};

// ---- Tickets ---------------------------------------------------------------

export function useGrievances(filter?: GrievanceFilter) {
  return useQuery<GrievanceTicket[]>({
    queryKey: grievanceQueryKeys.tickets(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter?.unitId) params.set("unitId", String(filter.unitId));
      if (filter?.mobileNo) params.set("mobileNo", String(filter.mobileNo));
      const qs = params.toString();
      return grvJson<GrievanceTicket[]>(`${GRV_BASE}/api/v1/grievances${qs ? `?${qs}` : ""}`);
    },
  });
}

export function useGrievance(id: number) {
  return useQuery<GrievanceTicket>({
    queryKey: grievanceQueryKeys.ticket(id),
    queryFn: () => grvJson<GrievanceTicket>(`${GRV_BASE}/api/v1/grievances/${id}`),
    enabled: id > 0,
  });
}

export function useCreateGrievance() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: GrievanceFormValues) =>
      grvJson<GrievanceTicket>(`${GRV_BASE}/api/v1/grievances`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["grievances"] });
    },
  });
}

export function useChangeStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: StatusChangeValues }) =>
      grvJson<GrievanceTicket>(`${GRV_BASE}/api/v1/grievances/${id}/status`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: grievanceQueryKeys.ticket(id) });
      void qc.invalidateQueries({ queryKey: ["grievances"] });
    },
  });
}

export function useChangeSeverity() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, severityId }: { id: number; severityId: number }) =>
      grvJson<GrievanceTicket>(`${GRV_BASE}/api/v1/grievances/${id}/severity`, {
        method: "POST",
        body: JSON.stringify({ severityId }),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: grievanceQueryKeys.ticket(id) });
    },
  });
}

// ---- Comments --------------------------------------------------------------

export function useComments(grievanceId: number) {
  return useQuery<GrievanceComment[]>({
    queryKey: grievanceQueryKeys.comments(grievanceId),
    queryFn: () => grvJson<GrievanceComment[]>(`${GRV_BASE}/api/v1/grievances/${grievanceId}/comments`),
    enabled: grievanceId > 0,
  });
}

export function useAddComment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, comment }: { id: number; comment: string }) =>
      grvJson<GrievanceComment>(`${GRV_BASE}/api/v1/grievances/${id}/comments`, {
        method: "POST",
        body: JSON.stringify({ comment }),
      }),
    onSuccess: (_, { id }) => qc.invalidateQueries({ queryKey: grievanceQueryKeys.comments(id) }),
  });
}

// ---- Feedback --------------------------------------------------------------

export function useFeedback(grievanceId: number) {
  return useQuery<GrievanceFeedback>({
    queryKey: grievanceQueryKeys.feedback(grievanceId),
    queryFn: () => grvJson<GrievanceFeedback>(`${GRV_BASE}/api/v1/grievances/${grievanceId}/feedback`),
    enabled: grievanceId > 0,
  });
}

export function useSubmitFeedback() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: FeedbackFormValues }) =>
      grvJson<GrievanceFeedback>(`${GRV_BASE}/api/v1/grievances/${id}/feedback`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: grievanceQueryKeys.feedback(id) });
      void qc.invalidateQueries({ queryKey: grievanceQueryKeys.ticket(id) });
      void qc.invalidateQueries({ queryKey: ["grievances"] });
    },
  });
}

// ---- Masters ---------------------------------------------------------------

export function useSeverities() {
  return useQuery<GrievanceSeverity[]>({
    queryKey: grievanceQueryKeys.severities(),
    queryFn: () => grvJson<GrievanceSeverity[]>(`${GRV_BASE}/api/v1/grievance-severities`),
    staleTime: 10 * 60 * 1000,
  });
}

export function useCategories() {
  return useQuery<GrievanceCategory[]>({
    queryKey: grievanceQueryKeys.categories(),
    queryFn: () => grvJson<GrievanceCategory[]>(`${GRV_BASE}/api/v1/grievance-categories`),
    staleTime: 10 * 60 * 1000,
  });
}

export function useSubCategories(categoryId?: number) {
  return useQuery<GrievanceSubCategory[]>({
    queryKey: grievanceQueryKeys.subCategories(categoryId),
    queryFn: () =>
      grvJson<GrievanceSubCategory[]>(
        `${GRV_BASE}/api/v1/grievance-sub-categories${categoryId ? `?categoryId=${categoryId}` : ""}`,
      ),
    enabled: categoryId !== undefined,
    staleTime: 10 * 60 * 1000,
  });
}

// ---- Dashboard counts ------------------------------------------------------

export function useTicketCounts() {
  return useQuery<TicketCounts>({
    queryKey: grievanceQueryKeys.counts(),
    queryFn: () => grvJson<TicketCounts>(`${GRV_BASE}/api/v1/grievances/counts`),
    refetchInterval: 30000,
  });
}
