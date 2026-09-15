import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  TariffValues,
  PackageValues,
  ConcessionTemplateValues,
  TariffFilterValues,
} from "./zodSchema";

/**
 * M05 Tariff Masters hooks — consume the reference-service REST API (Spring Boot :8081).
 * Tariffs, packages, health plans and concession templates live under the master entity
 * registry (`/api/v1/masters/{entity}`) — shared CRUD surface, unit-scoped.
 */

const TARIFF_BASE: string = import.meta.env.VITE_TARIFF_API_BASE ?? "/api";

async function masterJson<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      "X-Request-ID": crypto.randomUUID(),
    },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new ApiError(res.status, err.errorCode ?? "UNKNOWN", err.message ?? "Request failed");
  }
  return res.json();
}

// ---- Types -----------------------------------------------------------------

export interface TariffRecord {
  id: number;
  serviceId: number;
  serviceName: string;
  serviceCategory: string;
  baseRate: number;
  copayPercent: number;
  packageClass?: string;
  status: "ACTIVE" | "INACTIVE";
  unitId?: number;
}

export interface PackageRecord {
  id: number;
  packageName: string;
  packageCode: string;
  description?: string;
  totalValue: number;
  sellPrice: number;
  services: Array<{ serviceId: number; serviceName: string; qty: number }>;
  plans: Array<{ planId: number; planName: string; coveragePercent: number }>;
  status: "ACTIVE" | "INACTIVE";
  unitId?: number;
}

export interface ConcessionTemplateRecord {
  id: number;
  code: string;
  name: string;
  scheme: string;
  description?: string;
  maxPercentOff: number;
  requiresApproval: boolean;
  docStatus: number; // 1=ACTIVE 2=INACTIVE
  unitId?: number;
}

export interface HealthPlanRecord {
  id: number;
  planName: string;
  planCode: string;
  companyId: number;
  coveragePercent: number;
  maxCoverageAmount?: number;
  exclusions: string[];
  status: "ACTIVE" | "INACTIVE";
  unitId?: number;
}

// ---- Query keys ------------------------------------------------------------

export const tariffQueryKeys = {
  tariffs: (filter: TariffFilterValues) => ["tariffs", filter] as const,
  tariff: (id: number) => ["tariffs", id] as const,
  serviceCategories: () => ["tariffs", "categories"] as const,
  packages: () => ["tariffs", "packages"] as const,
  package: (id: number) => ["tariffs", "packages", id] as const,
  concessionTemplates: (filter?: { scheme?: string; status?: string }) =>
    ["tariffs", "concession-templates", filter] as const,
  concessionTemplate: (id: number) => ["tariffs", "concession-templates", id] as const,
  healthPlans: () => ["tariffs", "health-plans"] as const,
  healthPlan: (id: number) => ["tariffs", "health-plans", id] as const,
};

// ---- Service categories ----------------------------------------------------

export function useServiceCategories() {
  return useQuery({
    queryKey: tariffQueryKeys.serviceCategories(),
    queryFn: () => masterJson<string[]>(`${TARIFF_BASE}/api/v1/masters/service-category/all`),
    staleTime: 30 * 60 * 1000,
    placeholderData: [
      "CONSULTATION",
      "DIAGNOSTIC",
      "PROCEDURE",
      "ROOM_CHARGE",
      "PHARMACY",
      "LABORATORY",
      "RADIOLOGY",
      "PACKAGE",
      "MISCELLANEOUS",
    ],
  });
}

// ---- Tariffs ---------------------------------------------------------------

export function useTariffs(filter: TariffFilterValues) {
  return useQuery({
    queryKey: tariffQueryKeys.tariffs(filter),
    queryFn: () => masterJson<TariffRecord[]>(`${TARIFF_BASE}/api/v1/masters/tariffs`, {
      method: "POST",
      body: JSON.stringify(filter),
    }),
  });
}

export function useTariff(id: number) {
  return useQuery({
    queryKey: tariffQueryKeys.tariff(id),
    queryFn: () => masterJson<TariffRecord>(`${TARIFF_BASE}/api/v1/masters/tariffs/${id}`),
    enabled: id > 0,
  });
}

export function useCreateTariff() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: TariffValues) =>
      masterJson<TariffRecord>(`${TARIFF_BASE}/api/v1/masters/tariffs`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tariffs"] }),
  });
}

export function useUpdateTariff() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: TariffValues }) =>
      masterJson<TariffRecord>(`${TARIFF_BASE}/api/v1/masters/tariffs/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: tariffQueryKeys.tariff(id) });
      qc.invalidateQueries({ queryKey: ["tariffs"] });
    },
  });
}

// ---- Packages --------------------------------------------------------------

export function usePackages() {
  return useQuery({
    queryKey: tariffQueryKeys.packages(),
    queryFn: () => masterJson<PackageRecord[]>(`${TARIFF_BASE}/api/v1/masters/packages/all`),
    staleTime: 10 * 60 * 1000,
  });
}

export function usePackage(id: number) {
  return useQuery({
    queryKey: tariffQueryKeys.package(id),
    queryFn: () => masterJson<PackageRecord>(`${TARIFF_BASE}/api/v1/masters/packages/${id}`),
    enabled: id > 0,
  });
}

export function useCreatePackage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: PackageValues) =>
      masterJson<PackageRecord>(`${TARIFF_BASE}/api/v1/masters/packages`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: tariffQueryKeys.packages() }),
  });
}

export function useUpdatePackage() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: PackageValues }) =>
      masterJson<PackageRecord>(`${TARIFF_BASE}/api/v1/masters/packages/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: tariffQueryKeys.package(id) });
      qc.invalidateQueries({ queryKey: tariffQueryKeys.packages() });
    },
  });
}

// ---- Concession templates --------------------------------------------------

export function useConcessionTemplates(filter?: { scheme?: string; status?: string }) {
  return useQuery({
    queryKey: tariffQueryKeys.concessionTemplates(filter),
    queryFn: () => masterJson<ConcessionTemplateRecord[]>(
      `${TARIFF_BASE}/api/v1/masters/concession-templates`,
    ),
    staleTime: 5 * 60 * 1000,
  });
}

export function useConcessionTemplate(id: number) {
  return useQuery({
    queryKey: tariffQueryKeys.concessionTemplate(id),
    queryFn: () =>
      masterJson<ConcessionTemplateRecord>(`${TARIFF_BASE}/api/v1/masters/concession-templates/${id}`),
    enabled: id > 0,
  });
}

export function useCreateConcessionTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: ConcessionTemplateValues) =>
      masterJson<ConcessionTemplateRecord>(`${TARIFF_BASE}/api/v1/masters/concession-templates`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tariffs", "concession-templates"] }),
  });
}

export function useDeactivateConcessionTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      masterJson<ConcessionTemplateRecord>(
        `${TARIFF_BASE}/api/v1/masters/concession-templates/${id}/deactivate`,
        { method: "POST" },
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tariffs", "concession-templates"] }),
  });
}

// ---- Health plans ----------------------------------------------------------

export function useHealthPlans() {
  return useQuery({
    queryKey: tariffQueryKeys.healthPlans(),
    queryFn: () => masterJson<HealthPlanRecord[]>(`${TARIFF_BASE}/api/v1/masters/health-plans/all`),
    staleTime: 10 * 60 * 1000,
  });
}

export function useCreateHealthPlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: unknown) =>
      masterJson<HealthPlanRecord>(`${TARIFF_BASE}/api/v1/masters/health-plans`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: tariffQueryKeys.healthPlans() }),
  });
}

export function useUpdateHealthPlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: unknown }) =>
      masterJson<HealthPlanRecord>(`${TARIFF_BASE}/api/v1/masters/health-plans/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: tariffQueryKeys.healthPlan(id) });
      qc.invalidateQueries({ queryKey: tariffQueryKeys.healthPlans() });
    },
  });
}
