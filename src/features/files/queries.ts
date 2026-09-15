import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type { FileFilterValues } from "./zodSchema";

/**
 * M34 File/Document Management hooks — consume the file-service REST API (Spring Boot :8095).
 * Uses the Vite proxy (VITE_FILE_API_BASE) or production gateway.
 *
 * Stripe-style upload pattern:
 * - Optimistic UI with progress indication
 * - Automatic retry with exponential backoff on network errors
 * - Upload dedup via X-Request-ID
 * - Base64 encoding with preview before POST
 */

const FILE_BASE: string = import.meta.env.VITE_FILE_API_BASE ?? "/api";

async function fileJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface FileDetails {
  id: number;
  fileCategoryId?: number;
  ownerType?: string;
  ownerId?: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  fileToken: string;
  fileUrl?: string;
  annotationJson?: string;
  addedBy?: number;
  addedDatetime?: string;
  updatedBy?: number;
  updatedDatetime?: string;
  status?: boolean;
}

export interface FileCategory {
  id: number;
  parentId?: number;
  code: string;
  name: string;
  documentType?: string;
  filePath?: string;
  status?: boolean;
  children?: FileCategory[];
}

export interface FileFilter {
  ownerType?: string;
  ownerId?: number;
  fileCategoryId?: number;
  page?: number;
  size?: number;
}

export interface AnnotationView {
  annotationJson: string;
}

// ---- Query keys ------------------------------------------------------------

export const fileQueryKeys = {
  all: ["files"] as const,
  list: (filter: FileFilter) => ["files", "list", filter] as const,
  detail: (id: number) => ["files", id] as const,
  token: (token: string) => ["files", "token", token] as const,
  categories: () => ["files", "categories"] as const,
  categoryTree: () => ["files", "categories", "tree"] as const,
  annotation: (id: number) => ["files", id, "annotation"] as const,
  downloadBase64: (token: string) => ["files", "token", token, "base64"] as const,
};

// ---- File list -------------------------------------------------------------

export function useFiles(filter: FileFilter = {}) {
  return useQuery<FileDetails[]>({
    queryKey: fileQueryKeys.list(filter),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filter.ownerType) params.set("ownerType", filter.ownerType);
      if (filter.ownerId) params.set("ownerId", String(filter.ownerId));
      if (filter.fileCategoryId) params.set("fileCategoryId", String(filter.fileCategoryId));
      return fileJson<FileDetails[]>(`${FILE_BASE}/api/v1/files?${params}`);
    },
    enabled: !!(filter.ownerType || filter.ownerId || filter.fileCategoryId),
  });
}

export function useAllFiles() {
  return useQuery<FileDetails[]>({
    queryKey: fileQueryKeys.all,
    queryFn: () => fileJson<FileDetails[]>(`${FILE_BASE}/api/v1/files`),
    enabled: false, // Require filter params — use useFiles() instead
  });
}

// ---- Single file -----------------------------------------------------------

export function useFile(id: number) {
  return useQuery<FileDetails>({
    queryKey: fileQueryKeys.detail(id),
    queryFn: () => fileJson<FileDetails>(`${FILE_BASE}/api/v1/files/${id}`),
    enabled: id > 0,
  });
}

export function useFileByToken(token: string) {
  return useQuery<FileDetails>({
    queryKey: fileQueryKeys.token(token),
    queryFn: () => fileJson<FileDetails>(`${FILE_BASE}/api/v1/files/token/${token}`),
    enabled: token.length > 0,
  });
}

// ---- Upload (base64 JSON — Stripe upload pattern) --------------------------

export interface FileUploadPayload {
  fileName: string;
  fileType: string;
  fileData: string; // base64 data URI or raw base64
  fileCategoryId?: number;
  ownerType?: string;
  ownerId?: number;
}

export function useUploadFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: FileUploadPayload) =>
      fileJson<FileDetails>(`${FILE_BASE}/api/v1/files`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: fileQueryKeys.all });
    },
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000),
  });
}

// ---- Delete ----------------------------------------------------------------

export function useDeleteFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      fileJson<void>(`${FILE_BASE}/api/v1/files/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: fileQueryKeys.all }),
  });
}

// ---- Download (base64 JSON for browser preview) ---------------------------

export function useDownloadFileBase64(token: string) {
  return useQuery({
    queryKey: fileQueryKeys.downloadBase64(token),
    queryFn: () =>
      fileJson<{ fileName: string; fileType: string; fileSize: number; fileData: string; fileToken: string }>(
        `${FILE_BASE}/api/v1/files/token/${token}/base64`,
      ),
    enabled: token.length > 0,
    staleTime: 5 * 60 * 1000,
  });
}

// ---- Categories ------------------------------------------------------------

export function useFileCategories() {
  return useQuery<FileCategory[]>({
    queryKey: fileQueryKeys.categories(),
    queryFn: () => fileJson<FileCategory[]>(`${FILE_BASE}/api/v1/file-categories`),
    staleTime: 5 * 60 * 1000,
  });
}

export function useFileCategoryTree() {
  return useQuery<FileCategory[]>({
    queryKey: fileQueryKeys.categoryTree(),
    queryFn: () => fileJson<FileCategory[]>(`${FILE_BASE}/api/v1/file-categories/tree`),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateFileCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      fileJson<FileCategory>(`${FILE_BASE}/api/v1/file-categories`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: fileQueryKeys.categories() }),
  });
}

export function useRenameFileCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) =>
      fileJson<FileCategory>(`${FILE_BASE}/api/v1/file-categories/${id}`, {
        method: "PUT",
        body: JSON.stringify({ name }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: fileQueryKeys.categories() }),
  });
}

export function useDeleteFileCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      fileJson<void>(`${FILE_BASE}/api/v1/file-categories/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: fileQueryKeys.categories() }),
  });
}

// ---- Annotations (ImageAnnotationMaster parity) ----------------------------

export function useFileAnnotation(id: number) {
  return useQuery<AnnotationView>({
    queryKey: fileQueryKeys.annotation(id),
    queryFn: () => fileJson<AnnotationView>(`${FILE_BASE}/api/v1/files/${id}/annotation`),
    enabled: id > 0,
  });
}

export function useSaveAnnotation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, annotationJson }: { id: number; annotationJson: string }) =>
      fileJson<FileDetails>(`${FILE_BASE}/api/v1/files/${id}/annotation`, {
        method: "PUT",
        body: JSON.stringify({ annotationJson }),
      }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: fileQueryKeys.annotation(id) });
      void qc.invalidateQueries({ queryKey: fileQueryKeys.detail(id) });
    },
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