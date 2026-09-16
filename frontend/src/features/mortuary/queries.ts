import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  ChamberAllocationValues,
  MortuaryClearanceValues,
  DeathDetailsValues,
  DeathCertificateRequest,
} from "./zodSchema";

const PATIENT_BASE: string = import.meta.env.VITE_PATIENT_API_BASE ?? "/api";

async function mortuaryJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Query keys --------------------------------------------------------------
export const mortuaryQueryKeys = {
  all: ["mortuary"] as const,
  chambers: ["mortuary", "chambers"] as const,
  chamber: (id: number) => ["mortuary", "chamber", id] as const,
  deathDetails: (patientId: number) => ["mortuary", "death-details", patientId] as const,
  certificate: (patientId: number) => ["mortuary", "certificate", patientId] as const,
  deceasedList: ["mortuary", "deceased-list"] as const,
};

// ---- Chamber row shape ------------------------------------------------------
export interface ChamberRow {
  id: number;
  chamberNo: string;
  ward?: string;
  bodyName?: string;
  patientId?: number;
  admittedAt?: string;
  status: string;
  deathCertNo?: string;
}

export function useChambers() {
  return useQuery<ChamberRow[]>({
    queryKey: mortuaryQueryKeys.chambers,
    queryFn: () => mortuaryJson<ChamberRow[]>(`${PATIENT_BASE}/patient/getMortuaryPatients`),
    staleTime: 30_000,
  });
}

export function useDeceasedList() {
  return useQuery<ChamberRow[]>({
    queryKey: mortuaryQueryKeys.deceasedList,
    queryFn: () =>
      mortuaryJson<ChamberRow[]>(`${PATIENT_BASE}/patient/getMortuaryPatients`, {
        method: "POST",
        body: JSON.stringify({}),
      }),
    staleTime: 30_000,
  });
}

// ---- Allocation (box mgmt) --------------------------------------------------
export function useAllocateChamber() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (values: ChamberAllocationValues) =>
      mortuaryJson<{ chamberId: number; bodyName: string }>(
        `${PATIENT_BASE}/patient/saveMortuaryAllocation`,
        { method: "POST", body: JSON.stringify(values) },
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: mortuaryQueryKeys.all });
    },
  });
}

// ---- Release (clearance) ----------------------------------------------------
export function useReleaseChamber() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (values: MortuaryClearanceValues) =>
      mortuaryJson<{ chamberId: number; releasedAt: string }>(
        `${PATIENT_BASE}/patient/mortuaryClearance`,
        { method: "POST", body: JSON.stringify(values) },
      ),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: mortuaryQueryKeys.all });
    },
  });
}

// ---- Death details update --------------------------------------------------
export function useUpdateDeathDetails() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (values: DeathDetailsValues) =>
      mortuaryJson<boolean>(`${PATIENT_BASE}/patient/updatePatientDeathDetails`, {
        method: "POST",
        body: JSON.stringify(values),
      }),
    onSuccess: (_data, vars) => {
      void qc.invalidateQueries({ queryKey: mortuaryQueryKeys.deathDetails(vars.patientId) });
      void qc.invalidateQueries({ queryKey: mortuaryQueryKeys.all });
    },
  });
}

// ---- Death certificate (print) ---------------------------------------------
export interface DeathCertificate {
  certNumber: string;
  patientId: number;
  patientName: string;
  age?: number;
  gender?: string;
  deathDate: string;
  causeOfDeath: string;
  deathType: string;
  certifiedBy: string;
  placeOfDeath: string;
  hospitalName: string;
  printedAt: string;
}

export function useDeathCertificate(patientId: number) {
  return useQuery<DeathCertificate>({
    queryKey: mortuaryQueryKeys.certificate(patientId),
    queryFn: () =>
      mortuaryJson<DeathCertificate>(
        `${PATIENT_BASE}/patient/getPatientDetailsForDeathForm?patientId=${patientId}`,
      ),
    enabled: Boolean(patientId),
    staleTime: Infinity, // print-ready snapshot; refresh only on re-open
  });
}

export function useRequestCertificatePrint() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: DeathCertificateRequest) =>
      mortuaryJson<{ printed: boolean; certNumber: string }>(
        `${PATIENT_BASE}/patient/generateDeathCertificate`,
        { method: "POST", body: JSON.stringify(req) },
      ),
    onSuccess: (_data, vars) => {
      void qc.invalidateQueries({ queryKey: mortuaryQueryKeys.certificate(vars.patientId) });
    },
  });
}
