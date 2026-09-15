import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M01 patient hooks (Epic MyChart patient portal patterns).
 * patient-service runs on :8082 (see services/patient-service).
 * The Vite dev server does not proxy it yet
 * (VITE_PATIENT_API_BASE override in .env.local is the supported escape hatch).
 */

const PATIENT_BASE: string = import.meta.env.VITE_PATIENT_API_BASE ?? "/api";

async function patientJson<T>(url: string, options?: RequestInit): Promise<T> {
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

async function patientUpload<T>(url: string, body: FormData): Promise<T> {
  const res = await fetch(url, {
    method: "POST",
    headers: { "X-Request-ID": crypto.randomUUID() },
    body,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  return res.json();
}

// ---- Types -----------------------------------------------------------------

export interface PatientSearchHit {
  patientId: number;
  mrNo: string;
  firstName: string;
  lastName?: string;
  mobileNo: string;
  abhaAddress?: string;
  gender?: string;
  dateOfBirth?: string;
  ageYears?: number;
}

export interface PatientDocument {
  id: number;
  patientId: number;
  documentType: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedBy: number;
  uploadedAt: string;
  storageKey: string;
  abdmArtifactId?: string;
  abdmSyncStatus?: "PENDING" | "PUSHED" | "FAILED";
}

export interface PatientDetail {
  patientId: number;
  mrNo: string;
  firstName: string;
  lastName?: string;
  middleName?: string;
  fatherName?: string;
  motherName?: string;
  mobileNo: string;
  emergencyContact?: string;
  email?: string;
  abhaAddress?: string;
  aadhaarNumber?: string;
  genderId?: number;
  dateOfBirth?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  bloodGroup?: string;
  maritalStatus?: string;
  status?: boolean;
  photoUrl?: string;
  unitId?: number;
  // MLC
  isMlc?: boolean;
  mlcNumber?: string;
  mlcFirNumber?: string;
  mlcPoliceStation?: string;
  mlcNotes?: string;
  // Mother-child
  isChildPatient?: boolean;
  motherPatientId?: number;
  motherMrn?: string;
  birthWeightGrams?: number;
  gestationalAgeWeeks?: number;
  // COVID
  covidVaccinationStatus?: "UNVACCINATED" | "PARTIAL" | "FULL" | "BOOSTER";
  lastCovidVaccineDate?: string;
  covidInfectionHistory?: boolean;
  lastCovidInfectionDate?: string;
}

export const patientQueryKeys = {
  search: (q: string, type?: string) => ["patients", "search", q, type ?? "ALL"] as const,
  detail: (id: number) => ["patients", id] as const,
  documents: (id: number) => ["patients", id, "documents"] as const,
  photo: (id: number) => ["patients", id, "photo"] as const,
};

// ---- Search + detail -------------------------------------------------------

export function usePatientSearch(query: string, searchType: string = "ALL") {
  return useQuery<PatientSearchHit[]>({
    queryKey: patientQueryKeys.search(query, searchType),
    queryFn: () =>
      patientJson<PatientSearchHit[]>(
        `${PATIENT_BASE}/api/v1/patients/search?q=${encodeURIComponent(query)}&type=${encodeURIComponent(searchType)}`,
      ),
    enabled: query.length >= 2,
    staleTime: 30_000,
  });
}

export function usePatientDetail(id: number) {
  return useQuery<PatientDetail>({
    queryKey: patientQueryKeys.detail(id),
    queryFn: () => patientJson<PatientDetail>(`${PATIENT_BASE}/api/v1/patients/${id}`),
    enabled: id > 0,
  });
}

export function useRegisterPatient() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      patientJson<PatientDetail>(`${PATIENT_BASE}/api/v1/patients`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["patients"] }),
  });
}

export function useUpdatePatient(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      patientJson<PatientDetail>(`${PATIENT_BASE}/api/v1/patients/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: patientQueryKeys.detail(id) });
      qc.invalidateQueries({ queryKey: ["patients"] });
    },
  });
}

// ---- Documents -------------------------------------------------------------

export function usePatientDocuments(patientId: number) {
  return useQuery<PatientDocument[]>({
    queryKey: patientQueryKeys.documents(patientId),
    queryFn: () => patientJson<PatientDocument[]>(`${PATIENT_BASE}/api/v1/patients/${patientId}/documents`),
    enabled: patientId > 0,
  });
}

export function useUploadPatientDocument(patientId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { file: File; documentType: string; description?: string }) => {
      const form = new FormData();
      form.append("file", input.file);
      form.append("documentType", input.documentType);
      if (input.description) form.append("description", input.description);
      return patientUpload<PatientDocument>(
        `${PATIENT_BASE}/api/v1/patients/${patientId}/documents`,
        form,
      );
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: patientQueryKeys.documents(patientId) }),
  });
}

export function useDeletePatientDocument(patientId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (documentId: number) =>
      fetch(`${PATIENT_BASE}/api/v1/patients/${patientId}/documents/${documentId}`, {
        method: "DELETE",
        headers: { "X-Request-ID": crypto.randomUUID() },
      }).then(async (res) => {
        if (!res.ok) {
          const err = await res.json().catch(() => ({ message: res.statusText }));
          throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Delete failed");
        }
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: patientQueryKeys.documents(patientId) }),
  });
}

// ---- Photo -----------------------------------------------------------------

export function useCapturePatientPhoto(patientId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData();
      form.append("photo", file);
      return patientUpload<{ photoUrl: string; photoFileId: string }>(
        `${PATIENT_BASE}/api/v1/patients/${patientId}/photo`,
        form,
      );
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: patientQueryKeys.photo(patientId) }),
  });
}

// ---- ABHA verification ----------------------------------------------------

export function useVerifyAbha() {
  return useMutation({
    mutationFn: (abhaAddress: string) =>
      patientJson<{ verified: boolean; name?: string; gender?: string; dateOfBirth?: string; photo?: string }>(
        `${PATIENT_BASE}/api/v1/abha/verify`,
        { method: "POST", body: JSON.stringify({ abhaAddress }) },
      ),
  });
}
