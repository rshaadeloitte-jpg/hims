import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";
import type { TaskStatus, TaskPriority, GoalStatus, InterventionStatus } from "./zodSchema";

export type { TaskStatus, TaskPriority, GoalStatus, InterventionStatus } from "./zodSchema";

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

export interface WorklistPatient {
  id: number;
  name: string;
  mrn: string;
  admissionId: number;
  wardName?: string;
  bedLabel?: string;
  goalsCount: number;
  medsCount: number;
  labsCount: number;
  investigationsCount: number;
  openTasks: number;
  totalTasks: number;
}

export interface NursingTaskItem {
  taskId: number;
  admissionId: number;
  patientId: number;
  patientName?: string;
  taskType: string;
  description: string;
  priority: TaskPriority;
  status: TaskStatus;
  dueTime?: string;
  assignedTo?: number;
  completedAt?: string;
  cancelledAt?: string;
  createdAt: string;
}

export interface CareGoal {
  goalId: number;
  carePlanId: number;
  goalText: string;
  status: GoalStatus;
  targetDate?: string;
  achievedAt?: string;
  createdAt: string;
}

export interface Intervention {
  interventionId: number;
  carePlanId: number;
  interventionText: string;
  status: InterventionStatus;
  scheduledTime?: string;
  assignedTo?: number;
  completedAt?: string;
  createdAt: string;
}

export interface CarePlan {
  carePlanId: number;
  admissionId: number;
  patientId: number;
  title: string;
  description?: string;
  goals: CareGoal[];
  interventions: Intervention[];
  resolved: boolean;
  createdAt: string;
}

// ---- Query keys ------------------------------------------------------------

export const worklistQueryKeys = {
  patients: (filter?: Record<string, unknown>) => ["worklist", "patients", filter] as const,
  tasksByAdmission: (admissionId: number) => ["worklist", "tasks", admissionId] as const,
  tasksByPatient: (patientId: number) => ["worklist", "tasks", "patient", patientId] as const,
  carePlan: (carePlanId: number) => ["worklist", "care-plan", carePlanId] as const,
  carePlansByAdmission: (admissionId: number) => ["worklist", "care-plans", admissionId] as const,
};

// ---- Worklist patients -----------------------------------------------------

export function useWorklistPatients(filter?: Record<string, unknown>) {
  return useQuery<WorklistPatient[]>({
    queryKey: worklistQueryKeys.patients(filter),
    queryFn: () => {
      const params = new URLSearchParams();
      if (filter) {
        for (const [k, v] of Object.entries(filter)) {
          if (v !== undefined && v !== null && v !== "" && v !== "ALL") {
            params.set(k, String(v));
          }
        }
      }
      const qs = params.toString();
      return nrsJson<WorklistPatient[]>(
        `${NURSING_BASE}/api/v1/nursing/worklist${qs ? `?${qs}` : ""}`,
      );
    },
  });
}

// ---- Tasks -----------------------------------------------------------------

export function useTasksByAdmission(admissionId: number) {
  return useQuery<NursingTaskItem[]>({
    queryKey: worklistQueryKeys.tasksByAdmission(admissionId),
    queryFn: () => nrsJson<NursingTaskItem[]>(`${NURSING_BASE}/api/v1/nursing/worklist/${admissionId}/tasks`),
    enabled: admissionId > 0,
  });
}

export function useTasksByPatient(patientId: number) {
  return useQuery<NursingTaskItem[]>({
    queryKey: worklistQueryKeys.tasksByPatient(patientId),
    queryFn: () => nrsJson<NursingTaskItem[]>(`${NURSING_BASE}/api/v1/nursing/worklist/patient/${patientId}/tasks`),
    enabled: patientId > 0,
  });
}

export function useCreateTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      nrsJson<NursingTaskItem>(`${NURSING_BASE}/api/v1/tasks`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: (_, vars) => {
      const admissionId = vars.admissionId as number | undefined;
      if (admissionId) {
        void qc.invalidateQueries({ queryKey: worklistQueryKeys.tasksByAdmission(admissionId) });
      }
      void qc.invalidateQueries({ queryKey: ["worklist", "patients"] });
    },
  });
}

export function useUpdateTaskStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: TaskStatus }) =>
      nrsJson<NursingTaskItem>(`${NURSING_BASE}/api/v1/tasks/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["worklist"] });
    },
  });
}

// ---- Care plans / goals / interventions ------------------------------------

export function useCarePlansByAdmission(admissionId: number) {
  return useQuery<CarePlan[]>({
    queryKey: worklistQueryKeys.carePlansByAdmission(admissionId),
    queryFn: () => nrsJson<CarePlan[]>(`${NURSING_BASE}/api/v1/care-plans?admissionId=${admissionId}`),
    enabled: admissionId > 0,
  });
}

export function useCarePlan(carePlanId: number) {
  return useQuery<CarePlan>({
    queryKey: worklistQueryKeys.carePlan(carePlanId),
    queryFn: () => nrsJson<CarePlan>(`${NURSING_BASE}/api/v1/care-plans/${carePlanId}`),
    enabled: carePlanId > 0,
  });
}

export function useCreateCareGoal() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      nrsJson<CareGoal>(`${NURSING_BASE}/api/v1/care-plans/${payload.carePlanId}/goals`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, vars) => {
      const carePlanId = vars.carePlanId as number;
      void qc.invalidateQueries({ queryKey: worklistQueryKeys.carePlan(carePlanId) });
    },
  });
}

export function useCreateIntervention() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      nrsJson<Intervention>(`${NURSING_BASE}/api/v1/care-plans/${payload.carePlanId}/interventions`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: (_, vars) => {
      const carePlanId = vars.carePlanId as number;
      void qc.invalidateQueries({ queryKey: worklistQueryKeys.carePlan(carePlanId) });
    },
  });
}

export function useUpdateGoalStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: GoalStatus }) =>
      nrsJson<CareGoal>(`${NURSING_BASE}/api/v1/care-plans/goals/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["worklist"] });
    },
  });
}

export function useUpdateInterventionStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: InterventionStatus }) =>
      nrsJson<Intervention>(`${NURSING_BASE}/api/v1/care-plans/interventions/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["worklist"] });
    },
  });
}
