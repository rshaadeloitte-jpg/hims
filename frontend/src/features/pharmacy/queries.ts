import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  Prescription,
  DrugAllergy,
  DrugInteraction,
  ItemSale,
  SaleReturn,
  Intervention,
  AvailableBatch,
} from "./types";

const PHARMACY_BASE: string = import.meta.env.VITE_PHARMACY_API_BASE ?? "";

async function pharmacyJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", "X-Request-ID": crypto.randomUUID() },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  if (res.status === 204) return undefined as T;
  try {
    return await res.json();
  } catch {
    return undefined as T;
  }
}

export const pharmacyQueryKeys = {
  prescriptions: () => ["pharmacy", "prescriptions"] as const,
  prescription: (id: number) => ["pharmacy", "prescriptions", id] as const,
  sales: () => ["pharmacy", "item-sales"] as const,
  sale: (id: number) => ["pharmacy", "item-sales", id] as const,
  allergies: (patientId: number) => ["pharmacy", "drug-allergies", patientId] as const,
  interactions: (patientId: number) => ["pharmacy", "drug-interactions", patientId] as const,
  interventions: (patientId?: number) => ["pharmacy", "interventions", patientId ?? null] as const,
  returns: () => ["pharmacy", "sale-returns"] as const,
  return: (id: number) => ["pharmacy", "sale-returns", id] as const,
};

// Prescription queries (ordering-service)
export function usePrescriptions() {
  return useQuery<Prescription[]>({
    queryKey: pharmacyQueryKeys.prescriptions(),
    queryFn: () => pharmacyJson<Prescription[]>(`${PHARMACY_BASE}/api/v1/prescriptions`),
  });
}

export function usePrescription(id: number) {
  return useQuery<Prescription>({
    queryKey: pharmacyQueryKeys.prescription(id),
    queryFn: () => pharmacyJson<Prescription>(`${PHARMACY_BASE}/api/v1/prescriptions/${id}`),
    enabled: id > 0,
  });
}

// Item sales
export function useItemSales(prescriptionId?: number) {
  const url =
    prescriptionId != null
      ? `${PHARMACY_BASE}/api/v1/item-sales?prescriptionId=${prescriptionId}`
      : `${PHARMACY_BASE}/api/v1/item-sales`;
  return useQuery<ItemSale[]>({
    queryKey:
      prescriptionId != null
        ? [...pharmacyQueryKeys.sales(), prescriptionId]
        : pharmacyQueryKeys.sales(),
    queryFn: () => pharmacyJson<ItemSale[]>(url),
    enabled: prescriptionId == null || prescriptionId > 0,
  });
}

export interface CreateItemSalePayload {
  patientId: number;
  prescriptionId?: number | null;
  lines: { itemId: number; qty: number; batchId?: string }[];
}

export function useCreateItemSale() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateItemSalePayload) =>
      pharmacyJson<ItemSale>(`${PHARMACY_BASE}/api/v1/item-sales`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}

export function useItemSale(id: number) {
  return useQuery<ItemSale>({
    queryKey: pharmacyQueryKeys.sale(id),
    queryFn: () => pharmacyJson<ItemSale>(`${PHARMACY_BASE}/api/v1/item-sales/${id}`),
    enabled: id > 0,
  });
}

export function useAvailableBatches(itemId: number | null | undefined) {
  return useQuery<AvailableBatch[]>({
    queryKey: ["pharmacy", "available-batches", itemId ?? null],
    queryFn: () =>
      pharmacyJson<AvailableBatch[]>(
        `${PHARMACY_BASE}/api/v1/item-sales/available-batches?itemId=${itemId}`,
      ),
    enabled: !!itemId && itemId > 0,
  });
}

export interface DispenseItemSalePayload {
  saleId: number;
  lines: { lineNo: number; qty: number }[];
}

export function useDispenseItemSale() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: DispenseItemSalePayload) =>
      pharmacyJson<ItemSale>(`${PHARMACY_BASE}/api/v1/item-sales/${payload.saleId}/dispense`, {
        method: "POST",
        body: JSON.stringify({ lines: payload.lines }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}


// Sale returns
export function useSaleReturns() {
  return useQuery<SaleReturn[]>({
    queryKey: pharmacyQueryKeys.returns(),
    queryFn: () => pharmacyJson<SaleReturn[]>(`${PHARMACY_BASE}/api/v1/sale-returns`),
  });
}

export function useCompleteSaleReturn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) => {
      const id = typeof payload === "number" ? payload : payload?.returnId ?? payload?.id;
      const approvedBy = typeof payload === "number" ? undefined : payload?.approvedBy;
      const remarks = typeof payload === "number" ? undefined : payload?.remarks;
      const params = new URLSearchParams();
      if (approvedBy !== undefined && approvedBy !== null) params.set("approvedBy", String(approvedBy));
      if (remarks) params.set("remarks", remarks);
      const qs = params.toString();
      return pharmacyJson<any>(
        `${PHARMACY_BASE}/api/v1/sale-returns/${id}/complete${qs ? `?${qs}` : ""}`,
        { method: "POST" },
      );
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}

export function useRejectSaleReturn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) => {
      const id = typeof payload === "number" ? payload : payload?.returnId ?? payload?.id;
      const remarks = typeof payload === "number" ? undefined : payload?.remarks;
      const qs = remarks ? `?remarks=${encodeURIComponent(remarks)}` : "";
      return pharmacyJson<any>(`${PHARMACY_BASE}/api/v1/sale-returns/${id}/reject${qs}`, {
        method: "POST",
      });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}

// Interventions
export function useInterventions(patientId?: number) {
  return useQuery<Intervention[]>({
    queryKey: pharmacyQueryKeys.interventions(patientId),
    queryFn: () =>
      pharmacyJson<Intervention[]>(
        `${PHARMACY_BASE}/api/v1/interventions?patientId=${patientId}`,
      ),
    enabled: patientId !== undefined && patientId > 0,
  });
}

// Drug allergies
export function useDrugAllergies(patientId?: number) {
  return useQuery<DrugAllergy[]>({
    queryKey: patientId ? pharmacyQueryKeys.allergies(patientId) : ["pharmacy", "drug-allergies"],
    queryFn: () =>
      pharmacyJson<DrugAllergy[]>(`${PHARMACY_BASE}/api/v1/drug-allergies/patient/${patientId}`),
    enabled: patientId !== undefined && patientId > 0,
  });
}

export function useCreateDrugAllergy() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) =>
      pharmacyJson<any>(`${PHARMACY_BASE}/api/v1/drug-allergies`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}

// Drug interactions
export function useDrugInteractions(patientId?: number) {
  return useQuery<DrugInteraction[]>({
    queryKey: patientId ? pharmacyQueryKeys.interactions(patientId) : ["pharmacy", "drug-interactions"],
    queryFn: () =>
      pharmacyJson<DrugInteraction[]>(`${PHARMACY_BASE}/api/v1/drug-interactions/patient/${patientId}`),
    enabled: patientId !== undefined && patientId > 0,
  });
}

export function useCreateDrugInteraction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) =>
      pharmacyJson<any>(`${PHARMACY_BASE}/api/v1/drug-interactions`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}
