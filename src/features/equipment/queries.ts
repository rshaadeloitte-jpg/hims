import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

/**
 * M20 Equipment TanStack Query hooks — mirror the equipment-service REST API.
 * Base URL: VITE_EQUIPMENT_API_BASE (default /api, proxied to :8088).
 */

const EQ_BASE: string = import.meta.env.VITE_EQUIPMENT_API_BASE ?? "/api";

async function eqJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Type interfaces -------------------------------------------------------

export interface Equipment {
  id: number;
  itemId: number;
  accessionNo: string;
  serialNo?: string;
  model?: string;
  make?: string;
  purchaseDate?: string;
  installationDate?: string;
  warrantyExpiry?: string;
  workingStatus: number;
  departmentId?: number;
  storeId?: number;
  location?: string;
  cost?: number;
  supplierId?: number;
  custodianName?: string;
  documents?: string;
}

export interface EquipmentTransfer {
  id: number;
  equipmentId: number;
  fromDepartmentId?: number;
  toDepartmentId: number;
  transferDate?: string;
  reason?: string;
}

export interface EquipmentComplaint {
  id: number;
  equipmentId: number;
  complaintNo?: string;
  categoryId?: number;
  complaintTypeId?: number;
  severityId?: number;
  description: string;
  complaintDate?: string;
  reportedBy?: number;
  reportedDate?: string;
  complaintStatus: number;
  departmentId?: number;
  storeId?: number;
  accessionNo?: string;
  roomNo?: string;
  extensionNo?: string;
  assignedTo?: number;
  expectedResolutionDate?: string;
  resolvedDate?: string;
  visitDate?: string;
}

export interface EquipmentComplaintDetails {
  id: number;
  complaintId: number;
  itemId?: number;
  serialNo?: string;
  modelNo?: string;
  problemDate?: string;
  problemTime?: string;
  reportedDate?: string;
  reportedTime?: string;
  reportedBy?: number;
  serviceAgentId?: number;
  priorityId?: number;
  expectedResolutionDate?: string;
  expectedResolutionTime?: string;
  eqpMachineStatus?: number;
  supplierId?: number;
  storeId?: number;
  warrantyTypeId?: number;
  description?: string;
}

export interface EquipmentComplaintResolution {
  id: number;
  complaintId: number;
  resolveNo?: string;
  resolutionDate?: string;
  resolvedBy?: number;
  resolutionNotes: string;
  partsReplaced?: string;
  serviceAgentId?: number;
  assignedTo?: number;
  assignedById?: number;
  resolvedStatus: number;
}

export interface ComplaintDetailsResponse {
  complaint: EquipmentComplaint;
  details: EquipmentComplaintDetails[];
  resolutions: EquipmentComplaintResolution[];
}

export interface EqpPmSchedule {
  id: number;
  equipmentId: number;
  scheduleCode?: string;
  frequency: number;
  pmDate?: string;
  jan: boolean;
  feb: boolean;
  mar: boolean;
  apr: boolean;
  may: boolean;
  jun: boolean;
  jul: boolean;
  aug: boolean;
  sep: boolean;
  oct: boolean;
  nov: boolean;
  dec: boolean;
  nextDueDate?: string;
  lastCompletedDate?: string;
  pmType: number;
  storeId?: number;
}

export interface EqpPmScheduleDetails {
  id: number;
  pmScheduleId: number;
  scheduledDate?: string;
  preventiveMaintenance: boolean;
}

export interface EqpPreventiveMaintenance {
  id: number;
  equipmentId: number;
  pmScheduleId?: number;
  pmScheduleDetailId?: number;
  itemId?: number;
  maintenanceNo?: string;
  maintenanceDate?: string;
  performedBy?: number;
  functionalCheckRemark?: string;
  preventiveMaintenanceRemark?: string;
  electricalSafetyRemark?: string;
  calibration?: string;
  furtherActions?: string;
  commentsOnUse?: string;
  remarks?: string;
  deptInCharge?: number;
  biomedicalEngineerId?: number;
  biomedicalInChargeId?: number;
  verifiedBy?: number;
  amountIncurred?: number;
  serviceAgentId?: number;
  pmStatus: number;
}

export interface EquipmentPreventiveChecklist {
  id: number;
  code: string;
  description: string;
}

export interface ComplaintStatusMaster {
  id: number;
  code: string;
  description: string;
  isResolved: boolean;
  isClosed: boolean;
  isReopened: boolean;
}

export interface MaintenanceTypeMaster {
  id: number;
  code: string;
  description: string;
}

// ---- Query keys ------------------------------------------------------------

export const equipmentQueryKeys = {
  all: ["equipment"] as const,
  list: (filters?: Record<string, unknown>) => ["equipment", "list", filters] as const,
  detail: (id: number) => ["equipment", "detail", id] as const,
  byAccession: (accessionNo: string) => ["equipment", "by-accession", accessionNo] as const,
  byItem: (itemId: number) => ["equipment", "by-item", itemId] as const,
  transfers: (id: number) => ["equipment", "transfers", id] as const,
};

export const complaintQueryKeys = {
  all: ["complaints"] as const,
  list: (filters?: Record<string, unknown>) => ["complaints", "list", filters] as const,
  detail: (id: number) => ["complaints", "detail", id] as const,
  history: (equipmentId: number) => ["complaints", "history", equipmentId] as const,
  resolutions: (complaintId: number) => ["complaints", "resolutions", complaintId] as const,
};

export const pmScheduleQueryKeys = {
  all: ["pm-schedules"] as const,
  list: (filters?: Record<string, unknown>) => ["pm-schedules", "list", filters] as const,
  detail: (id: number) => ["pm-schedules", "detail", id] as const,
  ofEquipment: (equipmentId: number) => ["pm-schedules", "equipment", equipmentId] as const,
  history: (scheduleId: number) => ["pm-schedules", "history", scheduleId] as const,
  checklists: ["pm-schedules", "checklists"] as const,
};

// ---- Equipment (asset register) -------------------------------------------

export function useEquipmentList(filters?: Record<string, unknown>) {
  return useQuery({
    queryKey: equipmentQueryKeys.list(filters),
    queryFn: () => eqJson<Equipment[]>(`${EQ_BASE}/v1/equipment`),
  });
}

export function useEquipment(id: number) {
  return useQuery({
    queryKey: equipmentQueryKeys.detail(id),
    queryFn: () => eqJson<Equipment>(`${EQ_BASE}/v1/equipment/${id}`),
    enabled: id > 0,
  });
}

export function useRegisterEquipment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      eqJson<Equipment>(`${EQ_BASE}/v1/equipment`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: equipmentQueryKeys.all }),
  });
}

export function useUpdateEquipment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      eqJson<Equipment>(`${EQ_BASE}/v1/equipment/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: equipmentQueryKeys.detail(id) });
      qc.invalidateQueries({ queryKey: equipmentQueryKeys.all });
    },
  });
}

export function useTransferEquipment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      eqJson<EquipmentTransfer>(`${EQ_BASE}/v1/equipment/${id}/transfer`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: equipmentQueryKeys.transfers(id) });
      qc.invalidateQueries({ queryKey: equipmentQueryKeys.detail(id) });
    },
  });
}

export function useEquipmentTransfers(id: number) {
  return useQuery({
    queryKey: equipmentQueryKeys.transfers(id),
    queryFn: () => eqJson<EquipmentTransfer[]>(`${EQ_BASE}/v1/equipment/${id}/transfers`),
    enabled: id > 0,
  });
}

// ---- Complaints ------------------------------------------------------------

export function useComplaints(filters?: Record<string, unknown>) {
  return useQuery({
    queryKey: complaintQueryKeys.list(filters),
    queryFn: () => eqJson<EquipmentComplaint[]>(`${EQ_BASE}/v1/equipment/complaints`),
  });
}

export function useComplaint(id: number) {
  return useQuery({
    queryKey: complaintQueryKeys.detail(id),
    queryFn: () => eqJson<ComplaintDetailsResponse>(`${EQ_BASE}/v1/equipment/complaints/${id}`),
    enabled: id > 0,
  });
}

export function useComplaintsByEquipment(equipmentId: number) {
  return useQuery({
    queryKey: complaintQueryKeys.history(equipmentId),
    queryFn: () => eqJson<EquipmentComplaint[]>(`${EQ_BASE}/v1/equipment/${equipmentId}/complaints`),
    enabled: equipmentId > 0,
  });
}

export function useRaiseComplaint() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      eqJson<EquipmentComplaint>(`${EQ_BASE}/v1/equipment/complaints`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: complaintQueryKeys.all }),
  });
}

export function useUpdateComplaintStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) =>
      eqJson<EquipmentComplaint>(`${EQ_BASE}/v1/equipment/complaints/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ complaintStatus: status }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: complaintQueryKeys.all }),
  });
}

// ---- Complaint resolutions -------------------------------------------------

export function useComplaintResolutions(complaintId: number) {
  return useQuery({
    queryKey: complaintQueryKeys.resolutions(complaintId),
    queryFn: () =>
      eqJson<EquipmentComplaintResolution[]>(`${EQ_BASE}/v1/equipment/complaints/${complaintId}/resolutions`),
    enabled: complaintId > 0,
  });
}

export function useAddResolution() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ complaintId, payload }: { complaintId: number; payload: Record<string, unknown> }) =>
      eqJson<EquipmentComplaintResolution>(`${EQ_BASE}/v1/equipment/complaints/${complaintId}/resolutions`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { complaintId }) => {
      qc.invalidateQueries({ queryKey: complaintQueryKeys.resolutions(complaintId) });
      qc.invalidateQueries({ queryKey: complaintQueryKeys.detail(complaintId) });
      qc.invalidateQueries({ queryKey: complaintQueryKeys.all });
    },
  });
}

export function useUpdateResolution() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      complaintId,
      resId,
      payload,
    }: {
      complaintId: number;
      resId: number;
      payload: Record<string, unknown>;
    }) =>
      eqJson<EquipmentComplaintResolution>(
        `${EQ_BASE}/v1/equipment/complaints/${complaintId}/resolutions/${resId}`,
        { method: "PUT", body: JSON.stringify(payload) },
      ),
    onSuccess: (_, { complaintId }) => {
      qc.invalidateQueries({ queryKey: complaintQueryKeys.resolutions(complaintId) });
      qc.invalidateQueries({ queryKey: complaintQueryKeys.detail(complaintId) });
    },
  });
}

// ---- PM schedules ----------------------------------------------------------

export function usePmSchedules(filters?: Record<string, unknown>) {
  return useQuery({
    queryKey: pmScheduleQueryKeys.list(filters),
    queryFn: () => eqJson<EqpPmSchedule[]>(`${EQ_BASE}/v1/equipment/pm-schedules`),
  });
}

export function usePmSchedule(id: number) {
  return useQuery({
    queryKey: pmScheduleQueryKeys.detail(id),
    queryFn: () => eqJson<EqpPmSchedule>(`${EQ_BASE}/v1/equipment/pm-schedules/${id}`),
    enabled: id > 0,
  });
}

export function usePmSchedulesByEquipment(equipmentId: number) {
  return useQuery({
    queryKey: pmScheduleQueryKeys.ofEquipment(equipmentId),
    queryFn: () => eqJson<EqpPmSchedule[]>(`${EQ_BASE}/v1/equipment/${equipmentId}/pm-schedules`),
    enabled: equipmentId > 0,
  });
}

export function useCreatePmSchedule() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      eqJson<EqpPmSchedule>(`${EQ_BASE}/v1/equipment/pm-schedules`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: pmScheduleQueryKeys.all }),
  });
}

// ---- PM executions (completions) -------------------------------------------

export function usePmHistory(scheduleId: number) {
  return useQuery({
    queryKey: pmScheduleQueryKeys.history(scheduleId),
    queryFn: () => eqJson<EqpPreventiveMaintenance[]>(`${EQ_BASE}/v1/equipment/pm-schedules/${scheduleId}/history`),
    enabled: scheduleId > 0,
  });
}

export function useCompletePm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ scheduleId, payload }: { scheduleId: number; payload: Record<string, unknown> }) =>
      eqJson<EqpPreventiveMaintenance>(`${EQ_BASE}/v1/equipment/pm-schedules/${scheduleId}/complete`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, { scheduleId }) => {
      qc.invalidateQueries({ queryKey: pmScheduleQueryKeys.history(scheduleId) });
      qc.invalidateQueries({ queryKey: pmScheduleQueryKeys.detail(scheduleId) });
      qc.invalidateQueries({ queryKey: pmScheduleQueryKeys.all });
    },
  });
}

// ---- PM checklists ---------------------------------------------------------

export function usePmChecklists() {
  return useQuery({
    queryKey: pmScheduleQueryKeys.checklists,
    queryFn: () => eqJson<EquipmentPreventiveChecklist[]>(`${EQ_BASE}/v1/equipment/pm/checklists`),
  });
}

export function useCreatePmChecklist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      eqJson<EquipmentPreventiveChecklist>(`${EQ_BASE}/v1/equipment/pm/checklists`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: pmScheduleQueryKeys.checklists }),
  });
}

// ---- Master dictionaries ---------------------------------------------------

export function useComplaintStatuses() {
  return useQuery({
    queryKey: ["equipment-masters", "complaint-status"],
    queryFn: () => eqJson<ComplaintStatusMaster[]>(`${EQ_BASE}/v1/equipment/masters/complaint-status`),
  });
}

export function useMaintenanceTypes() {
  return useQuery({
    queryKey: ["equipment-masters", "maintenance-type"],
    queryFn: () => eqJson<MaintenanceTypeMaster[]>(`${EQ_BASE}/v1/equipment/masters/maintenance-type`),
  });
}
