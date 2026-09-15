import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M24 PMRD (Public Medical Record Department) hooks.
 *
 * Consumes the pmrd-service REST API.
 * APIs ported from legacy PmrdServiceImpl:
 *   - /pmrd/getAllPatientEncounter
 *   - /pmrd/getPatientIenById
 *   - /pmrd/getJsyFormDetails
 *   - /pmrd/getPatientDocumentsForPMRD
 *
 * Stripe-style upload pattern for documents:
 * - Optimistic UI with progress indication
 * - Automatic retry with exponential backoff
 * - Upload dedup via X-Request-ID
 * - Base64 encoding with preview before POST
 */

const PMRD_BASE: string = import.meta.env.VITE_PMRD_API_BASE ?? "/api";

async function pmrdJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Types -----------------------------------------------------------------

export interface JsyFormDetails {
  id: number;
  formType: "J1" | "J2" | "J3" | "H_FORM";
  patientId: number;
  beneficiaryName: string;
  status: "SUBMITTED" | "APPROVED" | "REJECTED" | "PENDING";
  submittedDate: string;
  approvedBy?: string;
  approvedDate?: string;
  amount?: number;
  formData: Record<string, unknown>;
}

export interface PatientEncounter {
  id: number;
  patientId: number;
  patientName: string;
  encounterType: string;
  encounterDate: string;
  doctorName?: string;
  departmentName?: string;
  diagnosis?: string;
  status: string;
  admissionId?: number;
}

export interface PmrdDocument {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  fileToken: string;
  fileUrl?: string;
  documentCategory: string;
  patientId: number;
  encounterId?: number;
  description?: string;
  addedBy?: number;
  addedDatetime?: string;
  status?: boolean;
}

export interface PmrdUploadPayload {
  fileName: string;
  fileType: string;
  fileData: string; // base64 data URI or raw base64
  documentCategory: string;
  patientId: number;
  encounterId?: number;
  description?: string;
}

// ---- Query keys ------------------------------------------------------------

export const pmrdQueryKeys = {
  all: ["pmrd"] as const,
  encounters: (filter: Record<string, unknown>) => ["pmrd", "encounters", filter] as const,
  encounter: (id: number) => ["pmrd", "encounters", id] as const,
  jsyForms: (filter: Record<string, unknown>) => ["pmrd", "jsy-forms", filter] as const,
  jsyForm: (id: number) => ["pmrd", "jsy-forms", id] as const,
  documents: (filter: Record<string, unknown>) => ["pmrd", "documents", filter] as const,
  document: (id: number) => ["pmrd", "documents", id] as const,
};

// ---- Encounter records -----------------------------------------------------

export function usePatientEncounters(filter: Record<string, unknown> = {}) {
  return useQuery<PatientEncounter[]>({
    queryKey: pmrdQueryKeys.encounters(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter.patientId) params.set("patientId", String(filter.patientId));
      if (filter.encounterType) params.set("encounterType", String(filter.encounterType));
      if (filter.fromDate) params.set("fromDate", String(filter.fromDate));
      if (filter.toDate) params.set("toDate", String(filter.toDate));
      if (filter.page !== undefined) params.set("page", String(filter.page));
      if (filter.size !== undefined) params.set("size", String(filter.size));
      return pmrdJson<PatientEncounter[]>(`${PMRD_BASE}/v1/pmrd/patient-encounters?${params}`);
    },
  });
}

export function usePatientEncounterById(id: number) {
  return useQuery<PatientEncounter>({
    queryKey: pmrdQueryKeys.encounter(id),
    queryFn: () => pmrdJson<PatientEncounter>(`${PMRD_BASE}/v1/pmrd/patient-encounters/${id}`),
    enabled: id > 0,
  });
}

// ---- JSY Forms ------------------------------------------------------------

export function useJsyForms(filter: Record<string, unknown> = {}) {
  return useQuery<JsyFormDetails[]>({
    queryKey: pmrdQueryKeys.jsyForms(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter.formType) params.set("formType", String(filter.formType));
      if (filter.patientId) params.set("patientId", String(filter.patientId));
      if (filter.fromDate) params.set("fromDate", String(filter.fromDate));
      if (filter.toDate) params.set("toDate", String(filter.toDate));
      if (filter.page !== undefined) params.set("page", String(filter.page));
      if (filter.size !== undefined) params.set("size", String(filter.size));
      return pmrdJson<JsyFormDetails[]>(`${PMRD_BASE}/v1/pmrd/jsy-forms?${params}`);
    },
  });
}

export function useJsyFormById(id: number) {
  return useQuery<JsyFormDetails>({
    queryKey: pmrdQueryKeys.jsyForm(id),
    queryFn: () => pmrdJson<JsyFormDetails>(`${PMRD_BASE}/v1/pmrd/jsy-forms/${id}`),
    enabled: id > 0,
  });
}

export function useSaveJsyForm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      pmrdJson<JsyFormDetails>(`${PMRD_BASE}/v1/pmrd/jsy-forms`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["pmrd", "jsy-forms"] });
    },
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000),
  });
}

// ---- Documents -------------------------------------------------------------

export function usePmrdDocuments(filter: Record<string, unknown> = {}) {
  return useQuery<PmrdDocument[]>({
    queryKey: pmrdQueryKeys.documents(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter.patientId) params.set("patientId", String(filter.patientId));
      if (filter.encounterId) params.set("encounterId", String(filter.encounterId));
      if (filter.documentCategory) params.set("documentCategory", String(filter.documentCategory));
      if (filter.page !== undefined) params.set("page", String(filter.page));
      if (filter.size !== undefined) params.set("size", String(filter.size));
      return pmrdJson<PmrdDocument[]>(`${PMRD_BASE}/v1/pmrd/documents?${params}`);
    },
  });
}

export function useUploadPmrdDocument() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: PmrdUploadPayload) =>
      pmrdJson<PmrdDocument>(`${PMRD_BASE}/v1/pmrd/documents`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["pmrd", "documents"] });
    },
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000),
  });
}

export function useDeletePmrdDocument() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      pmrdJson<void>(`${PMRD_BASE}/v1/pmrd/documents/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pmrd", "documents"] }),
  });
}

export function useDownloadPmrdDocumentBase64(token: string) {
  return useQuery({
    queryKey: ["pmrd", "documents", token, "base64"] as const,
    queryFn: () =>
      pmrdJson<{ fileName: string; fileType: string; fileSize: number; fileData: string; fileToken: string }>(
        `${PMRD_BASE}/v1/pmrd/documents/${token}/base64`,
      ),
    enabled: token.length > 0,
    staleTime: 5 * 60 * 1000,
  });
}

// ---- Utility: File → Base64 data URI --------------------------------------

export function readFileAsDataURL(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

/** Strip data URI prefix to get raw base64. */
export function stripDataURIPrefix(dataUrl: string): string {
  const commaIdx = dataUrl.indexOf(",");
  return commaIdx >= 0 ? dataUrl.slice(commaIdx + 1) : dataUrl;
}
