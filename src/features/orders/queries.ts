import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const ORD_BASE: string = import.meta.env.VITE_ORDERING_API_BASE ?? "/api";

async function ordJson<T>(url: string, options?: RequestInit): Promise<T> {
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

export interface ClinicalOrder {
  id: number;
  patientId: number;
  patientName: string;
  orderType: string;
  description: string;
  status: string;
  priority: string;
  orderedBy: string;
  orderedAt: string;
}

export interface ClinicalOrderInput {
  patientId: number;
  orderType: string;
  description: string;
  priority: string;
}

export function useClinicalOrders() {
  return useQuery({ queryKey: ["clinical-orders"], queryFn: () => ordJson<ClinicalOrder[]>(`${ORD_BASE}/v1/orders`) });
}

export function useClinicalOrder(id: number) {
  return useQuery({ queryKey: ["clinical-order", id], queryFn: () => ordJson<ClinicalOrder>(`${ORD_BASE}/v1/orders/${id}`) });
}

export function useCreateClinicalOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ClinicalOrderInput) => ordJson<ClinicalOrder>(`${ORD_BASE}/v1/orders`, { method: "POST", body: JSON.stringify(input) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["clinical-orders"] }),
  });
}

export function useUpdateOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      ordJson<ClinicalOrder>(`${ORD_BASE}/v1/orders/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["clinical-orders"] }),
  });
}

// ---- Order sets -----------------------------------------------------------

export interface OrderSet {
  id: number;
  name: string;
  code: string;
  category: string;
  description?: string;
  items: OrderSetItem[];
  status: "ACTIVE" | "INACTIVE";
  createdAt: string;
  updatedAt: string;
}

export interface OrderSetItem {
  id: string;
  serviceType: string;
  serviceId: number;
  serviceName: string;
  qty: number;
  priority: string;
}

export function useOrderSets() {
  return useQuery({ queryKey: ["order-sets"], queryFn: () => ordJson<OrderSet[]>(`${ORD_BASE}/v1/order-sets`) });
}

export function useOrderSet(id: number) {
  return useQuery({ queryKey: ["order-set", id], queryFn: () => ordJson<OrderSet>(`${ORD_BASE}/v1/order-sets/${id}`), enabled: id > 0 });
}

export function useCreateOrderSet() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      ordJson<OrderSet>(`${ORD_BASE}/v1/order-sets`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["order-sets"] }),
  });
}

export function useUpdateOrderSet() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      ordJson<OrderSet>(`${ORD_BASE}/v1/order-sets/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ["order-set", id] });
      qc.invalidateQueries({ queryKey: ["order-sets"] });
    },
  });
}

export function useApplyOrderSet() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      ordJson<ClinicalOrder[]>(`${ORD_BASE}/v1/order-sets/apply`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["clinical-orders"] }),
  });
}

// ---- CWF / SCWF -----------------------------------------------------------

export interface CwfDefinition {
  cwfId: number;
  cwfName: string;
  serviceType: string;
  steps: CwfStep[];
  status: "ACTIVE" | "INACTIVE";
}

export interface CwfStep {
  stepId: number;
  stepName: string;
  stepOrder: number;
  role: string;
  isMandatory: boolean;
  formSchema?: string;
}

export interface ScwfDefinition {
  scwfId: number;
  scwfName: string;
  serviceType: string;
  condition: string;
  steps: CwfStep[];
  status: "ACTIVE" | "INACTIVE";
}

export function useCwfDefinitions() {
  return useQuery({ queryKey: ["cwf-definitions"], queryFn: () => ordJson<CwfDefinition[]>(`${ORD_BASE}/v1/cwf`) });
}

export function useCwfDefinition(id: number) {
  return useQuery({ queryKey: ["cwf-definition", id], queryFn: () => ordJson<CwfDefinition>(`${ORD_BASE}/v1/cwf/${id}`), enabled: id > 0 });
}

export function useCreateCwfDefinition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      ordJson<CwfDefinition>(`${ORD_BASE}/v1/cwf`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["cwf-definitions"] }),
  });
}

export function useUpdateCwfDefinition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      ordJson<CwfDefinition>(`${ORD_BASE}/v1/cwf/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ["cwf-definition", id] });
      qc.invalidateQueries({ queryKey: ["cwf-definitions"] });
    },
  });
}

export function useScwfDefinitions() {
  return useQuery({ queryKey: ["scwf-definitions"], queryFn: () => ordJson<ScwfDefinition[]>(`${ORD_BASE}/v1/scwf`) });
}

export function useScwfDefinition(id: number) {
  return useQuery({ queryKey: ["scwf-definition", id], queryFn: () => ordJson<ScwfDefinition>(`${ORD_BASE}/v1/scwf/${id}`), enabled: id > 0 });
}

export function useCreateScwfDefinition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      ordJson<ScwfDefinition>(`${ORD_BASE}/v1/scwf`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["scwf-definitions"] }),
  });
}

export function useUpdateScwfDefinition() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: Record<string, unknown> }) =>
      ordJson<ScwfDefinition>(`${ORD_BASE}/v1/scwf/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: ["scwf-definition", id] });
      qc.invalidateQueries({ queryKey: ["scwf-definitions"] });
    },
  });
}

// ---- Workflow step status (runtime) ---------------------------------------

export interface WorkflowStepStatus {
  stepId: number;
  stepName: string;
  stepOrder: number;
  role: string;
  status: "PENDING" | "IN_PROGRESS" | "COMPLETED" | "SKIPPED" | "REJECTED";
  assignedTo?: string;
  completedAt?: string;
  completedBy?: string;
  remarks?: string;
}

export function useWorkflowSteps(orderId: number) {
  return useQuery({
    queryKey: ["workflow-steps", orderId],
    queryFn: () => ordJson<WorkflowStepStatus[]>(`${ORD_BASE}/v1/orders/${orderId}/workflow`),
    enabled: orderId > 0,
  });
}

export function useAdvanceWorkflowStep() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, stepId, action, remarks }: { orderId: number; stepId: number; action: "COMPLETE" | "REJECT" | "SKIP"; remarks?: string }) =>
      ordJson<WorkflowStepStatus[]>(`${ORD_BASE}/v1/orders/${orderId}/workflow/${stepId}/${action.toLowerCase()}`, {
        method: "POST",
        body: JSON.stringify({ remarks }),
      }),
    onSuccess: (_, { orderId }) => {
      qc.invalidateQueries({ queryKey: ["workflow-steps", orderId] });
      qc.invalidateQueries({ queryKey: ["clinical-orders"] });
    },
  });
}

// ---- External orders ------------------------------------------------------

export interface ExternalOrder {
  id: number;
  orderId: number;
  externalLabName: string;
  externalLabCode?: string;
  testName: string;
  testCode?: string;
  sampleType?: string;
  instructions?: string;
  status: "PENDING" | "SENT" | "RECEIVED" | "CANCELLED";
  createdAt: string;
}

export function useExternalOrders(orderId: number) {
  return useQuery({
    queryKey: ["external-orders", orderId],
    queryFn: () => ordJson<ExternalOrder[]>(`${ORD_BASE}/v1/orders/${orderId}/external`),
    enabled: orderId > 0,
  });
}

export function useCreateExternalOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ orderId, payload }: { orderId: number; payload: Record<string, unknown> }) =>
      ordJson<ExternalOrder>(`${ORD_BASE}/v1/orders/${orderId}/external`, { method: "POST", body: JSON.stringify(payload) }),
    onSuccess: (_, { orderId }) => qc.invalidateQueries({ queryKey: ["external-orders", orderId] }),
  });
}

// ---- Investigations -------------------------------------------------------

export interface Investigation {
  id: number;
  orderId: number;
  serviceId: number;
  serviceName: string;
  status: string;
  priority: string;
  orderedBy: string;
  orderedAt: string;
  resultAt?: string;
  resultValue?: string;
  referenceRange?: string;
  unit?: string;
  isCritical?: boolean;
}

export function useInvestigations(orderId: number) {
  return useQuery({
    queryKey: ["investigations", orderId],
    queryFn: () => ordJson<Investigation[]>(`${ORD_BASE}/v1/orders/${orderId}/investigations`),
    enabled: orderId > 0,
  });
}
