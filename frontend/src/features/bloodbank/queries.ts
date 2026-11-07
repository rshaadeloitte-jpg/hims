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

/* ------------------------------------------------------------------ java shapes */

interface JavaDonor {
  donorId: number;
  name: string;
  bloodGroup: string;
  donationStatus: number;
  lastActionAt: string;
}

interface JavaBag {
  bagId: number;
  donorId: number;
  bloodGroup: string;
  volumeMl: number;
  collectedAt: string;
  expirydate: string;
  crossMatched: boolean;
  isbagexpired: boolean;
  issued: boolean;
}

interface JavaTrail {
  id: number;
  bagId: number;
  eventType: number;
  eventAt: string;
  actorId?: number | null;
  refDocType?: string | null;
  refDocId?: number | null;
  remarks?: string | null;
}

interface JavaIndent {
  id: number;
  indentNo: string;
  fromStoreId: number;
  toStoreId: number;
  bloodGroup: string;
  component: string;
  quantity: number;
  requestedDate: string;
  remarks?: string | null;
  docStatus: number;
  approvedBy?: number | null;
  approvedDatetime?: string | null;
  issuedAt?: string | null;
}

/* ------------------------------------------------------------------ frontend types */

export interface Donor {
  id: number;
  name: string;
  age?: number;
  gender?: "MALE" | "FEMALE" | "OTHER";
  bloodGroup: string;
  phone?: string;
  weight?: number;
  pulse?: number;
  bpSystolic?: number;
  bpDiastolic?: number;
  hemoglobin?: number;
  lastDonationAt?: string;
  registeredAt?: string;
}

export interface DonorInput {
  name: string;
  bloodGroup: string;
  donationStatus?: number;
  unitId?: number;
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

/* ------------------------------------------------------------------ mappers */

function mapDonor(d: JavaDonor): Donor {
  return {
    id: d.donorId,
    name: d.name,
    bloodGroup: d.bloodGroup,
    registeredAt: d.lastActionAt,
  };
}

function mapBag(b: JavaBag): BloodBag {
  let status = "AVAILABLE";
  if (b.issued) status = "ISSUED";
  else if (b.isbagexpired) status = "EXPIRED";
  else if (b.crossMatched) status = "RESERVED";
  return {
    id: b.bagId,
    bagNo: "",
    bloodGroup: b.bloodGroup,
    component: "",
    collectionDate: b.collectedAt,
    expiryDate: b.expirydate,
    status,
    donorId: b.donorId,
  };
}

const EVENT_TYPE_NAMES: Record<number, string> = {
  1: "COLLECTED",
  2: "CROSSMATCHED",
  3: "ISSUED",
  4: "RETURNED",
  5: "DISCARDED",
  6: "SEPARATED",
  7: "INDENT_REQUESTED",
  8: "INDENT_APPROVED",
  9: "INDENT_ISSUED",
  10: "INDENT_CANCELLED",
  11: "CROSS_UNIT_REQUESTED",
  12: "CROSS_UNIT_RECEIVED",
};

function mapTrail(t: JavaTrail): BagEvent {
  return {
    id: t.id,
    bagId: t.bagId,
    type: EVENT_TYPE_NAMES[t.eventType] ?? String(t.eventType),
    occurredAt: t.eventAt,
    performedBy: t.actorId == null ? undefined : String(t.actorId),
    note: t.remarks ?? undefined,
    location: t.refDocType ?? undefined,
  };
}

const INDENT_STATUS: Record<number, Indent["status"]> = {
  1: "REQUESTED",
  2: "APPROVED",
  3: "DISPATCHED",
  4: "REJECTED",
};

function mapIndent(i: JavaIndent): Indent {
  return {
    id: i.id,
    sourceStore: String(i.fromStoreId),
    targetStore: String(i.toStoreId),
    bloodGroup: i.bloodGroup,
    component: i.component,
    units: i.quantity,
    status: INDENT_STATUS[i.docStatus] ?? "REQUESTED",
    requestedBy: "",
    requestedAt: i.requestedDate,
    fulfilledAt: i.issuedAt ?? i.approvedDatetime ?? undefined,
    note: i.remarks ?? undefined,
  };
}

/* ------------------------------------------------------------------ queries */

export function useDonors() {
  return useQuery<Donor[]>({
    queryKey: ["blood-donors"],
    queryFn: async () => (await bbJson<JavaDonor[]>(`${BB_BASE}/v1/donors`)).map(mapDonor),
  });
}

export function useDonor(id: number) {
  return useQuery<Donor>({
    queryKey: ["blood-donor", id],
    queryFn: async () => mapDonor(await bbJson<JavaDonor>(`${BB_BASE}/v1/donors/${id}`)),
    enabled: id > 0,
  });
}

export function useCreateDonor() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: DonorInput) =>
      mapDonor(
        await bbJson<JavaDonor>(`${BB_BASE}/v1/donors`, {
          method: "POST",
          body: JSON.stringify({
            name: input.name,
            bloodGroup: input.bloodGroup,
            donationStatus: input.donationStatus,
            unitId: input.unitId,
          }),
        })
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["blood-donors"] }),
  });
}

export function useBloodBags() {
  return useQuery<BloodBag[]>({
    queryKey: ["blood-bags"],
    queryFn: async () => (await bbJson<JavaBag[]>(`${BB_BASE}/v1/bags`)).map(mapBag),
  });
}

export function useBloodBag(id: number) {
  return useQuery<BloodBag>({
    queryKey: ["blood-bag", id],
    queryFn: async () => mapBag(await bbJson<JavaBag>(`${BB_BASE}/v1/bags/${id}`)),
    enabled: id > 0,
  });
}

export function useCreateBloodBag() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: BloodBagInput) =>
      mapBag(
        await bbJson<JavaBag>(`${BB_BASE}/v1/bags`, {
          method: "POST",
          body: JSON.stringify({
            donorId: input.donorId,
            bloodGroup: input.bloodGroup,
            volumeMl: input.volume,
            expirydate: input.expiryDate,
          }),
        })
      ),
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
  return useQuery<Record<string, number>>({
    queryKey: ["blood-stock"],
    queryFn: () => bbJson<Record<string, number>>(`${BB_BASE}/v1/blood-bags/stock`),
  });
}

export function useBagEvents(bagId: number) {
  return useQuery<BagEvent[]>({
    queryKey: ["bag-events", bagId],
    queryFn: async () => (await bbJson<JavaTrail[]>(`${BB_BASE}/v1/bloodbank/bags/${bagId}/trail`)).map(mapTrail),
    enabled: bagId > 0,
  });
}

export function useAddBagEvent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ bagId, event }: { bagId: number; event: Partial<BagEvent> }) =>
      mapTrail(
        await bbJson<JavaTrail>(`${BB_BASE}/v1/bloodbank/bags/${bagId}/trail`, {
          method: "POST",
          body: JSON.stringify({
            eventType: event.type ? Number(event.type) || undefined : undefined,
            actorId: event.performedBy ? Number(event.performedBy) || undefined : undefined,
            remarks: event.note,
          }),
        })
      ),
    onSuccess: (_data, vars) => qc.invalidateQueries({ queryKey: ["bag-events", vars.bagId] }),
  });
}

export function useCrossMatches() {
  return useQuery<CrossMatch[]>({
    queryKey: ["cross-matches"],
    queryFn: () => bbJson<CrossMatch[]>(`${BB_BASE}/v1/blood/cross-matches`),
  });
}

export function useCrossMatch(id: number) {
  return useQuery<CrossMatch>({
    queryKey: ["cross-match", id],
    queryFn: () => bbJson<CrossMatch>(`${BB_BASE}/v1/blood/cross-matches/${id}`),
    enabled: id > 0,
  });
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
    onSuccess: (_data, vars) => qc.invalidateQueries({ queryKey: ["cross-match", vars.id] }),
  });
}

export function useIndents() {
  return useQuery<Indent[]>({
    queryKey: ["indents"],
    queryFn: async () => (await bbJson<JavaIndent[]>(`${BB_BASE}/v1/bloodbank/indents`)).map(mapIndent),
  });
}

export function useIndent(id: number) {
  return useQuery<Indent>({
    queryKey: ["indent", id],
    queryFn: async () => mapIndent(await bbJson<JavaIndent>(`${BB_BASE}/v1/bloodbank/indents/${id}`)),
    enabled: id > 0,
  });
}

export function useCreateIndent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: IndentInput) =>
      mapIndent(
        await bbJson<JavaIndent>(`${BB_BASE}/v1/bloodbank/indents`, {
          method: "POST",
          body: JSON.stringify({
            fromStoreId: Number(input.sourceStore),
            toStoreId: Number(input.targetStore),
            bloodGroup: input.bloodGroup,
            component: input.component,
            quantity: input.units,
            requestedDate: new Date().toISOString(),
            remarks: input.note,
          }),
        })
      ),
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
