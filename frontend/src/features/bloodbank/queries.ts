import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const BB_BASE: string = import.meta.env.VITE_BLOODBANK_API_BASE ?? "/api";

async function bbJson<T>(url: string, options?: RequestInit): Promise<T> {
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

/* ------------------------------------------------------------------ types */

export interface Donor {
  id: number;
  name: string;
  age: number;
  gender: "MALE" | "FEMALE" | "OTHER";
  bloodGroup: string;
  phone?: string;
  weight?: number;
  pulse?: number;
  bpSystolic?: number;
  bpDiastolic?: number;
  hemoglobin?: number;
  lastDonationAt?: string;
  registeredAt: string;
}

export interface DonorInput {
  name: string;
  age: number;
  gender: "MALE" | "FEMALE" | "OTHER";
  bloodGroup: string;
  phone?: string;
  weight?: number;
  pulse?: number;
  bpSystolic?: number;
  bpDiastolic?: number;
  hemoglobin?: number;
  lastDonationAt?: string;
}

export interface BloodBag {
  id: number;
  bagNo: string;
  bloodGroup: string;
  component: string;
  collectionDate: string;
  expiryDate: string;
  status: string;
  donorId: number;
  donorName?: string;
}

export interface BloodBagInput {
  bloodGroup: string;
  component: string;
  donorId: number;
  volume: number;
  collectionDate: string;
  expiryDate: string;
}

export interface BagEvent {
  id: number;
  bagId: number;
  type: string;
  occurredAt: string;
  location?: string;
  performedBy?: string;
  note?: string;
}

export interface CrossMatch {
  id: number;
  bagId: number;
  patientId: number;
  patientName?: string;
  compatibility: "COMPATIBLE" | "INCOMPATIBLE" | "PENDING";
  method: "MAJOR" | "MINOR" | "BOTH";
  requestedBy: string;
  requestedAt: string;
  completedAt?: string;
  note?: string;
}

export interface CrossMatchInput {
  bagId: number;
  patientId: number;
  method: "MAJOR" | "MINOR" | "BOTH";
  note?: string;
}

export interface Indent {
  id: number;
  sourceStore: string;
  targetStore: string;
  bloodGroup: string;
  component: string;
  units: number;
  status: "REQUESTED" | "APPROVED" | "DISPATCHED" | "RECEIVED" | "REJECTED";
  requestedBy: string;
  requestedAt: string;
  fulfilledAt?: string;
  note?: string;
}

export interface IndentInput {
  sourceStore: string;
  targetStore: string;
  bloodGroup: string;
  component: string;
  units: number;
  note?: string;
}

/* ------------------------------------------------------------------ queries */

export function useDonors() {
  return useQuery({ queryKey: ["blood-donors"], queryFn: () => bbJson<Donor[]>(`${BB_BASE}/v1/blood/donors`) });
}

export function useDonor(id: number) {
  return useQuery({ queryKey: ["blood-donor", id], queryFn: () => bbJson<Donor>(`${BB_BASE}/v1/blood/donors/${id}`) });
}

export function useCreateDonor() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: DonorInput) => bbJson<Donor>(`${BB_BASE}/v1/blood/donors`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["blood-donors"] }),
  });
}

export function useBloodBags() {
  return useQuery({ queryKey: ["blood-bags"], queryFn: () => bbJson<BloodBag[]>(`${BB_BASE}/v1/blood-bags`) });
}

export function useBloodBag(id: number) {
  return useQuery({ queryKey: ["blood-bag", id], queryFn: () => bbJson<BloodBag>(`${BB_BASE}/v1/blood-bags/${id}`) });
}

export function useCreateBloodBag() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: BloodBagInput) => bbJson<BloodBag>(`${BB_BASE}/v1/blood-bags`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["blood-bags"] }),
  });
}

export function useUpdateBloodBagStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      bbJson<BloodBag>(`${BB_BASE}/v1/blood-bags/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["blood-bags"] }),
  });
}

export function useBloodBankStock() {
  return useQuery({ queryKey: ["blood-stock"], queryFn: () => bbJson<Record<string, number>>(`${BB_BASE}/v1/blood-bags/stock`) });
}

export function useBagEvents(bagId: number) {
  return useQuery({
    queryKey: ["bag-events", bagId],
    queryFn: () => bbJson<BagEvent[]>(`${BB_BASE}/v1/blood-bags/${bagId}/events`),
    enabled: bagId > 0,
  });
}

export function useAddBagEvent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ bagId, event }: { bagId: number; event: Partial<BagEvent> }) =>
      bbJson<BagEvent>(`${BB_BASE}/v1/blood-bags/${bagId}/events`, { method: "POST", body: JSON.stringify(event) }),
    onSuccess: (_data, vars) => qc.invalidateQueries({ queryKey: ["bag-events", vars.bagId] }),
  });
}

export function useCrossMatches() {
  return useQuery({ queryKey: ["cross-matches"], queryFn: () => bbJson<CrossMatch[]>(`${BB_BASE}/v1/blood/cross-matches`) });
}

export function useCrossMatch(id: number) {
  return useQuery({ queryKey: ["cross-match", id], queryFn: () => bbJson<CrossMatch>(`${BB_BASE}/v1/blood/cross-matches/${id}`) });
}

export function useCreateCrossMatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: CrossMatchInput) =>
      bbJson<CrossMatch>(`${BB_BASE}/v1/blood/cross-matches`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cross-matches"] }),
  });
}

export function useUpdateCrossMatchCompatibility() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, compatibility }: { id: number; compatibility: CrossMatch["compatibility"] }) =>
      bbJson<CrossMatch>(`${BB_BASE}/v1/blood/cross-matches/${id}/compatibility`, {
        method: "PATCH",
        body: JSON.stringify({ compatibility }),
      }),
    onSuccess: (_data, vars) =>
      qc.invalidateQueries({ queryKey: ["cross-match", vars.id] }),
  });
}

export function useIndents() {
  return useQuery({ queryKey: ["indents"], queryFn: () => bbJson<Indent[]>(`${BB_BASE}/v1/blood/indents`) });
}

export function useIndent(id: number) {
  return useQuery({ queryKey: ["indent", id], queryFn: () => bbJson<Indent>(`${BB_BASE}/v1/blood/indents/${id}`) });
}

export function useCreateIndent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: IndentInput) =>
      bbJson<Indent>(`${BB_BASE}/v1/blood/indents`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["indents"] }),
  });
}

export function useUpdateIndentStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: Indent["status"] }) =>
      bbJson<Indent>(`${BB_BASE}/v1/blood/indents/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
    onSuccess: (_data, vars) => qc.invalidateQueries({ queryKey: ["indent", vars.id] }),
  });
}
