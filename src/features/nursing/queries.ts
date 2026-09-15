import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const NURSING_BASE: string = import.meta.env.VITE_NURSING_API_BASE ?? "/api";

async function nrsJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Types -----------------------------------------------------------------

export interface CarePlan {
  carePlanId: number;
  admissionId: number;
  patientId: number;
  title: string;
  description?: string;
  goals: string[];
  interventions: string[];
  resolved: boolean;
  createdAt: string;
}

export interface ShiftHandover {
  handoverId: number;
  admissionId: number;
  patientId: number;
  fromNurseId: number;
  toNurseId: number;
  shift: string;
  summary?: string;
  notes?: string;
  signed: boolean;
  createdAt: string;
}

export interface NurseAllocation {
  allocationId: number;
  admissionId: number;
  nurseId: number;
  wardId?: number;
  shift: string;
  startDate: string;
  released: boolean;
}

export interface Incident {
  incidentId: number;
  admissionId: number;
  patientId: number;
  type: string;
  severity: string;
  description: string;
  reportedBy?: number;
  reviewed: boolean;
  createdAt: string;
}

export interface Vitals {
  vitalId: number;
  patientId: number;
  temperature?: number;
  pulse?: number;
  respiration?: number;
  bloodPressureSystolic?: number;
  bloodPressureDiastolic?: number;
  spO2?: number;
  weight?: number;
  height?: number;
  recordedAt: string;
}

export interface NursingNote {
  noteId: number;
  admissionId: number;
  content: string;
  category?: string;
  recordedBy?: number;
  createdAt: string;
}

export interface NursingTask {
  taskId: number;
  admissionId: number;
  patientId: number;
  taskType: string;
  description: string;
  dueTime?: string;
  assignedTo?: number;
  completed: boolean;
  cancelled: boolean;
  createdAt: string;
}

export interface Worklist {
  admissionId: number;
  patientId: number;
  patientName?: string;
  ward?: string;
  bed?: string;
  tasks: { open: number; total: number };
  vitals: { open: number; total: number };
  notes: { open: number; total: number };
}

// ---- Query keys ------------------------------------------------------------

export const nursingQueryKeys = {
  carePlans: (admissionId?: number) => ["nursing", "care-plans", admissionId] as const,
  handovers: () => ["nursing", "shift-handovers"] as const,
  allocations: () => ["nursing", "nurse-allocations"] as const,
  incidents: () => ["nursing", "incidents"] as const,
  vitals: (patientId: number) => ["nursing", "vitals", patientId] as const,
  notes: (admissionId: number) => ["nursing", "notes", admissionId] as const,
  tasks: () => ["nursing", "tasks"] as const,
  worklist: (admissionId: number) => ["nursing", "worklist", admissionId] as const,
};

// ---- Care plans ------------------------------------------------------------

export function useCarePlans(admissionId?: number) {
  return useQuery<CarePlan[]>({
    queryKey: nursingQueryKeys.carePlans(admissionId),
    queryFn: () => {
      const url = admissionId
        ? `${NURSING_BASE}/api/v1/care-plans?admissionId=${admissionId}`
        : `${NURSING_BASE}/api/v1/care-plans`;
      return nrsJson<CarePlan[]>(url);
    },
  });
}

export function useCreateCarePlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      nrsJson<CarePlan>(`${NURSING_BASE}/api/v1/care-plans`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "care-plans"] }),
  });
}

export function useResolveCarePlan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      nrsJson<CarePlan>(`${NURSING_BASE}/api/v1/care-plans/${id}/resolve`, { method: "PUT" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "care-plans"] }),
  });
}

// ---- Shift handovers -------------------------------------------------------

export function useShiftHandovers() {
  return useQuery<ShiftHandover[]>({
    queryKey: nursingQueryKeys.handovers(),
    queryFn: () => nrsJson<ShiftHandover[]>(`${NURSING_BASE}/api/v1/shift-handovers`),
  });
}

export function useCreateShiftHandover() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      nrsJson<ShiftHandover>(`${NURSING_BASE}/api/v1/shift-handovers`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "shift-handovers"] }),
  });
}

export function useSignHandover() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      nrsJson<ShiftHandover>(`${NURSING_BASE}/api/v1/shift-handovers/${id}/sign`, { method: "PUT" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "shift-handovers"] }),
  });
}

// ---- Nurse allocations -----------------------------------------------------

export function useNurseAllocations() {
  return useQuery<NurseAllocation[]>({
    queryKey: nursingQueryKeys.allocations(),
    queryFn: () => nrsJson<NurseAllocation[]>(`${NURSING_BASE}/api/v1/nurse-allocations`),
  });
}

export function useCreateNurseAllocation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      nrsJson<NurseAllocation>(`${NURSING_BASE}/api/v1/nurse-allocations`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "nurse-allocations"] }),
  });
}

// ---- Incidents -------------------------------------------------------------

export function useIncidents() {
  return useQuery<Incident[]>({
    queryKey: nursingQueryKeys.incidents(),
    queryFn: () => nrsJson<Incident[]>(`${NURSING_BASE}/api/v1/incidents`),
  });
}

export function useCreateIncident() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      nrsJson<Incident>(`${NURSING_BASE}/api/v1/incidents`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "incidents"] }),
  });
}

export function useReviewIncident() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      nrsJson<Incident>(`${NURSING_BASE}/api/v1/incidents/${id}/review`, { method: "PUT" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "incidents"] }),
  });
}

// ---- Vitals ----------------------------------------------------------------

export function useVitals(patientId: number) {
  return useQuery<Vitals[]>({
    queryKey: nursingQueryKeys.vitals(patientId),
    queryFn: () => nrsJson<Vitals[]>(`${NURSING_BASE}/api/v1/patients/${patientId}/vitals`),
    enabled: patientId > 0,
  });
}

export function useRecordVitals() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ patientId, payload }: { patientId: number; payload: Record<string, unknown> }) =>
      nrsJson<Vitals>(`${NURSING_BASE}/api/v1/patients/${patientId}/vitals`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: (_, { patientId }) => qc.invalidateQueries({ queryKey: ["nursing", "vitals", patientId] }),
  });
}

// ---- Notes -----------------------------------------------------------------

export function useNursingNotes(admissionId: number) {
  return useQuery<NursingNote[]>({
    queryKey: nursingQueryKeys.notes(admissionId),
    queryFn: () => nrsJson<NursingNote[]>(`${NURSING_BASE}/api/v1/admissions/${admissionId}/notes`),
    enabled: admissionId > 0,
  });
}

export function useCreateNursingNote() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ admissionId, payload }: { admissionId: number; payload: Record<string, unknown> }) =>
      nrsJson<NursingNote>(`${NURSING_BASE}/api/v1/admissions/${admissionId}/notes`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: (_, { admissionId }) => qc.invalidateQueries({ queryKey: ["nursing", "notes", admissionId] }),
  });
}

// ---- Tasks -----------------------------------------------------------------

export function useNursingTasks() {
  return useQuery<NursingTask[]>({
    queryKey: nursingQueryKeys.tasks(),
    queryFn: () => nrsJson<NursingTask[]>(`${NURSING_BASE}/api/v1/tasks`),
  });
}

export function useCreateNursingTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      nrsJson<NursingTask>(`${NURSING_BASE}/api/v1/tasks`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "tasks"] }),
  });
}

export function useCompleteTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      nrsJson<NursingTask>(`${NURSING_BASE}/api/v1/tasks/${id}/complete`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["nursing", "tasks"] }),
  });
}

// ---- Worklist --------------------------------------------------------------

export function useWorklist(admissionId: number) {
  return useQuery<Worklist>({
    queryKey: nursingQueryKeys.worklist(admissionId),
    queryFn: () => nrsJson<Worklist>(`${NURSING_BASE}/api/v1/nursing/worklist/${admissionId}`),
    enabled: admissionId > 0,
  });
}