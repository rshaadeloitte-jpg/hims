import { useQuery, useQueryClient, useMutation } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type {
  Vehicle,
  FuelLog,
  FuelLogCreateForm,
  MaintenanceRequest,
  MaintenanceRequestCreateForm,
  MaintenanceApprovalForm,
} from "./zodSchema";

const TRN_BASE: string = import.meta.env.VITE_TRANSPORT_API_BASE ?? "/api";

async function trnJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ── Query keys ──────────────────────────────────────────────────────────────

export const transportQueryKeys = {
  vehicles: () => ["transport", "vehicles"] as const,
  vehicle: (id: number) => ["transport", "vehicles", id] as const,
  fuelLogs: () => ["transport", "fuel-logs"] as const,
  fuelLog: (id: number) => ["transport", "fuel-logs", id] as const,
  maintenanceRequests: () => ["transport", "maintenance"] as const,
  maintenanceRequest: (id: number) => ["transport", "maintenance", id] as const,
};

// ── Vehicle requests (existing) ─────────────────────────────────────────────

export interface VehicleRequest {
  id: number;
  vehicleType: string;
  fromLocation: string;
  toLocation: string;
  requester: string;
  status: string;
  requestedAt: string;
}

export function useVehicleRequests() {
  return useQuery({
    queryKey: ["vehicle-requests"],
    queryFn: () => trnJson<VehicleRequest[]>(`${TRN_BASE}/v1/vehicle-requests`),
  });
}

export function useCreateRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Partial<VehicleRequest>) =>
      trnJson<VehicleRequest>(`${TRN_BASE}/v1/vehicle-requests`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["vehicle-requests"] }),
  });
}

// ── Vehicles ────────────────────────────────────────────────────────────────

export function useVehicles() {
  return useQuery<Vehicle[]>({
    queryKey: transportQueryKeys.vehicles(),
    queryFn: () => trnJson<Vehicle[]>(`${TRN_BASE}/api/v1/transport/vehicles`),
  });
}

export function useVehicle(id: number) {
  return useQuery<Vehicle>({
    queryKey: transportQueryKeys.vehicle(id),
    queryFn: () => trnJson<Vehicle>(`${TRN_BASE}/api/v1/transport/vehicles/${id}`),
    enabled: id > 0,
  });
}

export function useVehicleIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { vehicleId: number; location: string; odometerReading: number; notes?: string }) =>
      trnJson<Vehicle>(`${TRN_BASE}/api/v1/transport/vehicles/${input.vehicleId}/in`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: transportQueryKeys.vehicles() }),
  });
}

export function useVehicleOut() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { vehicleId: number; destination: string; odometerReading: number; purpose: string }) =>
      trnJson<Vehicle>(`${TRN_BASE}/api/v1/transport/vehicles/${input.vehicleId}/out`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: transportQueryKeys.vehicles() }),
  });
}

export function useAllocateVehicle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { vehicleId: number; allocatedTo: string; purpose: string; expectedReturnAt?: string }) =>
      trnJson<Vehicle>(`${TRN_BASE}/api/v1/transport/vehicles/${input.vehicleId}/allocate`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: transportQueryKeys.vehicles() }),
  });
}

// ── Fuel logs ───────────────────────────────────────────────────────────────

export function useFuelLogs() {
  return useQuery<FuelLog[]>({
    queryKey: transportQueryKeys.fuelLogs(),
    queryFn: () => trnJson<FuelLog[]>(`${TRN_BASE}/v1/fuel-logs`),
  });
}

export function useAddFuelLog() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: FuelLogCreateForm) =>
      trnJson<FuelLog>(`${TRN_BASE}/v1/fuel-logs`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: transportQueryKeys.fuelLogs() }),
  });
}

// ── Maintenance requests ────────────────────────────────────────────────────

export function useMaintenanceRequests() {
  return useQuery<MaintenanceRequest[]>({
    queryKey: transportQueryKeys.maintenanceRequests(),
    queryFn: () => trnJson<MaintenanceRequest[]>(`${TRN_BASE}/v1/maintenance`),
  });
}

export function useCreateMaintenanceRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: MaintenanceRequestCreateForm) =>
      trnJson<MaintenanceRequest>(`${TRN_BASE}/v1/maintenance`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: transportQueryKeys.maintenanceRequests() }),
  });
}

export function useApproveMaintenanceRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: MaintenanceApprovalForm) =>
      trnJson<MaintenanceRequest>(`${TRN_BASE}/v1/maintenance/${input.id}/approve`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: transportQueryKeys.maintenanceRequests() }),
  });
}
