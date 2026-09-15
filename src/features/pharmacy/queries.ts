import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type { Prescription, DispensePayload, ItemSale, SaleReturn, Intervention } from "./types";

export interface DrugAllergy {
  allergyId: number;
  patientId: number;
  allergen: string;
  allergyType?: string;
  severity?: string;
  reaction?: string;
}

export interface DrugInteraction {
  interactionId: number;
  patientId: number;
  drug1Id: number;
  drug2Id: number;
  severity?: string;
  description?: string;
  acknowledged: boolean;
}

const PHARMACY_BASE: string = import.meta.env.VITE_PHARMACY_API_BASE ?? "/api";

async function pharmacyJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// Query keys
export const pharmacyQueryKeys = {
  prescriptions: () => ["pharmacy", "prescriptions"] as const,
  prescription: (id: number) => ["pharmacy", "prescriptions", id] as const,
  sales: () => ["pharmacy", "item-sales"] as const,
  sale: (id: number) => ["pharmacy", "item-sales", id] as const,
  allergies: (patientId: number) => ["pharmacy", "drug-allergies", patientId] as const,
  interactions: (patientId: number) => ["pharmacy", "drug-interactions", patientId] as const,
  interventions: () => ["pharmacy", "interventions"] as const,
  returns: () => ["pharmacy", "sale-returns"] as const,
};

// Prescription queries
export function usePrescriptions() {
  return useQuery<Prescription[]>({
    queryKey: pharmacyQueryKeys.prescriptions(),
    queryFn: () => pharmacyJson<Prescription[]>(`${PHARMACY_BASE}/api/v1/pharmacy/prescriptions`),
  });
}

export function usePrescription(id: number) {
  return useQuery<Prescription>({
    queryKey: pharmacyQueryKeys.prescription(id),
    queryFn: () => pharmacyJson<Prescription>(`${PHARMACY_BASE}/api/v1/pharmacy/prescriptions/${id}`),
    enabled: id > 0,
  });
}

// Item sales, returns, interventions for PharmacyDashboard
export function useItemSales() {
  return useQuery<any[]>({
    queryKey: ["pharmacy", "itemSales"],
    queryFn: () => pharmacyJson<any[]>(`${PHARMACY_BASE}/api/v1/pharmacy/item-sales`),
  });
}

export function useSaleReturns() {
  return useQuery<any[]>({
    queryKey: ["pharmacy", "saleReturns"],
    queryFn: () => pharmacyJson<any[]>(`${PHARMACY_BASE}/api/v1/pharmacy/sale-returns`),
  });
}

export function useInterventions() {
  return useQuery<any[]>({
    queryKey: ["pharmacy", "interventions"],
    queryFn: () => pharmacyJson<any[]>(`${PHARMACY_BASE}/api/v1/pharmacy/interventions`),
  });
}

// Dispense mutation
export function useDispense() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: DispensePayload) =>
      pharmacyJson<void>(`${PHARMACY_BASE}/api/v1/pharmacy/prescriptions/${payload.prescriptionId}/dispense`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy", "prescriptions"] }),
  });
}
export function useCreateItemSale() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) =>
      pharmacyJson<any>(`${PHARMACY_BASE}/api/v1/pharmacy/create-item-sale`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}

export function useCompleteSaleReturn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) =>
      pharmacyJson<any>(`${PHARMACY_BASE}/api/v1/pharmacy/complete-sale-return`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}

export function useRejectSaleReturn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) =>
      pharmacyJson<any>(`${PHARMACY_BASE}/api/v1/pharmacy/reject-sale-return`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}

export function useDrugAllergies() {
  return useQuery<any[]>({
    queryKey: ["pharmacy", "drug-allergies"],
    queryFn: () => pharmacyJson<any[]>(`${PHARMACY_BASE}/api/v1/pharmacy/drug-allergies`),
  });
}

export function useDrugInteractions() {
  return useQuery<any[]>({
    queryKey: ["pharmacy", "drug-interactions"],
    queryFn: () => pharmacyJson<any[]>(`${PHARMACY_BASE}/api/v1/pharmacy/drug-interactions`),
  });
}

export function useCreateDrugAllergy() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) =>
      pharmacyJson<any>(`${PHARMACY_BASE}/api/v1/pharmacy/create-drug-allergy`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}

export function useCreateDrugInteraction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: any) =>
      pharmacyJson<any>(`${PHARMACY_BASE}/api/v1/pharmacy/create-drug-interaction`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pharmacy"] }),
  });
}
