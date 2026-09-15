import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../api/client";

const OT_BASE: string = import.meta.env.VITE_OT_API_BASE ?? "/api";

async function otJson<T>(url: string, options?: RequestInit): Promise<T> {
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

// ---- Domain types ---------------------------------------------------------

export interface OtRoom {
  id: number;
  roomId?: number;
  roomName: string;
  roomType?: string;
  isActive?: boolean;
}

export interface OtProcedure {
  procedureCode: string;
  procedureName: string;
  rate?: number;
  dependentOfIndex?: number;
  discountPct?: number;
}

export interface OtScheduleDetail {
  id?: number;
  detailId?: number;
  scheduleId?: number;
  procedureCode: string;
  procedureName: string;
  rate?: number;
  dependentOf?: number;
  discountPct?: number;
  status?: string;
}

export interface OtSchedule {
  id?: number;
  scheduleId?: number;
  patientId: number;
  patientName?: string;
  encounterId?: number;
  admissionId?: number;
  otTable: string;
  scheduledStart: string;
  scheduledEnd: string;
  anesthetistId?: number;
  surgeonId?: number;
  status?: number; // 1 PLANNED, 2 IN_PROGRESS, 3 DONE, 4 CANCELLED
  preBilling?: boolean;
  procedures?: OtProcedure[];
  details?: OtScheduleDetail[];
}

export interface OtScheduleInput {
  patientId: number;
  encounterId?: number;
  admissionId?: number;
  otTable: string;
  scheduledStart: string;
  scheduledEnd: string;
  anesthetistId?: number;
  surgeonId?: number;
  preBilling?: boolean;
  procedures?: OtProcedure[];
}

export interface AnesthesiaRecord {
  id?: number;
  recordId?: number;
  scheduleId?: number;
  patientId?: number;
  asaGrade?: string;
  airwayAssessment?: string;
  comorbidities?: string;
  preAnesthesiaNotes?: string;
  intraOpNotes?: string;
  postAnesthesiaNotes?: string;
  pacuStayMinutes?: number;
  recoveryScore?: number;
  complications?: string;
}

export interface VitalSignRecord {
  id?: number;
  vitalId?: number;
  recordId?: number;
  recordedAt?: string;
  heartRate?: number;
  bpSystolic?: number;
  bpDiastolic?: number;
  spo2?: number;
  respiratoryRate?: number;
  temperature?: string;
}

export interface AnesthesiaDrug {
  id?: number;
  drugId?: number;
  recordId?: number;
  drugName?: string;
  dose?: string;
  route?: string;
  administeredAt?: string;
}

export interface SurgicalChecklist {
  id?: number;
  checklistId?: number;
  scheduleId?: number;
  patientId?: number;
  phase?: string; // SIGN_IN, TIME_OUT, SIGN_OUT
  completed?: boolean;
  completedBy?: number;
  completedAt?: string;
}

export interface ChecklistItem {
  id?: number;
  itemId?: number;
  checklistId?: number;
  itemName?: string;
  category?: string;
  completed?: boolean;
  notes?: string;
}

export interface ChecklistCompletion {
  id?: number;
  completionId?: number;
  checklistId?: number;
  overallStatus?: string; // PENDING, COMPLETED, ABORTED
  completedBy?: number;
  completedAt?: string;
}

export interface OtBillingEstimate {
  id?: number;
  estimateId?: number;
  scheduleId?: number;
  procedureCode?: string;
  totalAmount?: number;
  status?: string;
}

export interface OtProcedureCost {
  id?: number;
  estimateId?: number;
  itemName?: string;
  itemType?: string;
  amount?: number;
}

export interface OtStaffSchedule {
  id?: number;
  scheduleId?: number;
  role?: string;
  staffId?: number;
  start?: string;
  end?: string;
}

export interface OtNurseSchedule {
  id?: number;
  staffId?: number;
  dutyDate?: string;
  shiftType?: string;
  dutyRoster?: string;
  emergencyCall?: boolean;
}

// ---- Query keys -----------------------------------------------------------

export const otQueryKeys = {
  rooms: () => ["ot", "rooms"] as const,
  schedules: () => ["ot", "schedules"] as const,
  schedule: (id: number) => ["ot", "schedules", id] as const,
  anesthesia: (scheduleId?: number) =>
    scheduleId != null ? ["ot", "anesthesia", scheduleId] as const : ["ot", "anesthesia"] as const,
  vitals: (recordId: number) => ["ot", "anesthesia", recordId, "vitals"] as const,
  drugs: (recordId: number) => ["ot", "anesthesia", recordId, "drugs"] as const,
  checklists: () => ["ot", "checklists"] as const,
  checklist: (scheduleId: number) => ["ot", "checklists", scheduleId] as const,
  billingEstimates: (scheduleId?: number) =>
    scheduleId != null ? ["ot", "billing", "estimates", scheduleId] as const : ["ot", "billing", "estimates"] as const,
  staffSchedule: (scheduleId: number) => ["ot", "staff-schedule", scheduleId] as const,
  nurseSchedule: (staffId: number) => ["ot", "nurse-schedule", staffId] as const,
  dashboard: () => ["ot", "dashboard"] as const,
};

// ---- OT Rooms -------------------------------------------------------------

export function useOtRooms() {
  return useQuery<OtRoom[]>({
    queryKey: otQueryKeys.rooms(),
    queryFn: () => otJson<OtRoom[]>(`${OT_BASE}/api/v1/ot/rooms`),
  });
}

export function useCreateOtRoom() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; type: string }) =>
      otJson<OtRoom>(`${OT_BASE}/api/v1/ot/staff-schedule/rooms`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.rooms() }),
  });
}

// ---- Schedules ------------------------------------------------------------

export function useOtSchedules(date?: string, status?: number) {
  const params = new URLSearchParams();
  if (date) params.set("date", date);
  if (status != null) params.set("status", String(status));
  const qs = params.toString();
  return useQuery<OtSchedule[]>({
    queryKey: [...otQueryKeys.schedules(), date ?? "", status ?? ""],
    queryFn: () => otJson<OtSchedule[]>(`${OT_BASE}/api/v1/ot/schedules${qs ? `?${qs}` : ""}`),
  });
}

export function useOtSchedule(id: number) {
  return useQuery<OtSchedule>({
    queryKey: otQueryKeys.schedule(id),
    queryFn: () => otJson<OtSchedule>(`${OT_BASE}/api/v1/ot/schedules/${id}`),
    enabled: id > 0,
  });
}

export function useCreateOtSchedule() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: OtScheduleInput) =>
      otJson<OtSchedule>(`${OT_BASE}/api/v1/ot/schedules`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: otQueryKeys.schedules() });
      void qc.invalidateQueries({ queryKey: otQueryKeys.dashboard() });
    },
  });
}

export function useStartSurgery() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      otJson<OtSchedule>(`${OT_BASE}/api/v1/ot/schedules/${id}/start`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.schedules() }),
  });
}

export function useCompleteSurgery() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      otJson<OtSchedule>(`${OT_BASE}/api/v1/ot/schedules/${id}/complete`, { method: "POST" }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: otQueryKeys.schedules() });
      void qc.invalidateQueries({ queryKey: otQueryKeys.dashboard() });
    },
  });
}

export function useCancelSurgery() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      otJson<OtSchedule>(`${OT_BASE}/api/v1/ot/schedules/${id}/cancel`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.schedules() }),
  });
}

// ---- Dashboard (room status) ----------------------------------------------

export function useOtDashboard(date?: string) {
  return useQuery<{ rooms: OtRoom[]; schedules: OtSchedule[] }>({
    queryKey: otQueryKeys.dashboard(),
    queryFn: async () => {
      const scheds = await otJson<OtSchedule[]>(`${OT_BASE}/api/v1/ot/schedules${date ? `?date=${date}` : ""}`);
      return { rooms: [], schedules: scheds };
    },
  });
}

// ---- Anesthesia Records ---------------------------------------------------

export function useAnesthesiaRecords() {
  return useQuery<AnesthesiaRecord[]>({
    queryKey: otQueryKeys.anesthesia(),
    queryFn: () => otJson<AnesthesiaRecord[]>(`${OT_BASE}/ot/anesthesia`),
  });
}

export function useAnesthesiaRecord(scheduleId: number) {
  return useQuery<AnesthesiaRecord>({
    queryKey: otQueryKeys.anesthesia(scheduleId),
    queryFn: () => otJson<AnesthesiaRecord>(`${OT_BASE}/ot/anesthesia?scheduleId=${scheduleId}`),
    enabled: scheduleId > 0,
  });
}

export function useCreatePreOp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      scheduleId: number;
      patientId: number;
      asaGrade: string;
      airway: string;
      comorbidities?: string;
    }) =>
      otJson<AnesthesiaRecord>(`${OT_BASE}/ot/anesthesia/pre-op`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.anesthesia() }),
  });
}

export function useAddVitalSign() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { recordId: number; data: Omit<VitalSignRecord, "id" | "vitalId" | "recordId"> }) =>
      otJson<VitalSignRecord>(`${OT_BASE}/ot/anesthesia/${input.recordId}/vitals`, {
        method: "POST",
        body: JSON.stringify(input.data),
      }),
    onSuccess: (_, { recordId }) => qc.invalidateQueries({ queryKey: otQueryKeys.vitals(recordId) }),
  });
}

export function useVitalSigns(recordId: number) {
  return useQuery<VitalSignRecord[]>({
    queryKey: otQueryKeys.vitals(recordId),
    queryFn: () => otJson<VitalSignRecord[]>(`${OT_BASE}/ot/anesthesia/${recordId}/vitals`),
    enabled: recordId > 0,
  });
}

export function useAddAnesthesiaDrug() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { recordId: number; data: Omit<AnesthesiaDrug, "id" | "drugId" | "recordId"> }) =>
      otJson<AnesthesiaDrug>(`${OT_BASE}/ot/anesthesia/${input.recordId}/drugs`, {
        method: "POST",
        body: JSON.stringify(input.data),
      }),
    onSuccess: (_, { recordId }) => qc.invalidateQueries({ queryKey: otQueryKeys.drugs(recordId) }),
  });
}

export function useAnesthesiaDrugs(recordId: number) {
  return useQuery<AnesthesiaDrug[]>({
    queryKey: otQueryKeys.drugs(recordId),
    queryFn: () => otJson<AnesthesiaDrug[]>(`${OT_BASE}/ot/anesthesia/${recordId}/drugs`),
    enabled: recordId > 0,
  });
}

export function useCompletePostOp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { recordId: number; pacuMinutes?: number; recoveryScore?: number; complications?: string }) =>
      otJson<AnesthesiaRecord>(`${OT_BASE}/ot/anesthesia/${input.recordId}/post-op`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.anesthesia() }),
  });
}

// ---- Surgical Checklists (WHO) -------------------------------------------

export function useChecklists() {
  return useQuery<SurgicalChecklist[]>({
    queryKey: otQueryKeys.checklists(),
    queryFn: () => otJson<SurgicalChecklist[]>(`${OT_BASE}/ot/checklists`),
  });
}

export function useChecklistsBySchedule(scheduleId: number) {
  return useQuery<SurgicalChecklist[]>({
    queryKey: otQueryKeys.checklist(scheduleId),
    queryFn: () => otJson<SurgicalChecklist[]>(`${OT_BASE}/ot/checklists?scheduleId=${scheduleId}`),
    enabled: scheduleId > 0,
  });
}

export function useCreateChecklist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { scheduleId: number; patientId: number; phase: string }) =>
      otJson<SurgicalChecklist>(`${OT_BASE}/ot/checklists`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.checklists() }),
  });
}

export function useAddChecklistItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { checklistId: number; itemName: string; category: string }) =>
      otJson<ChecklistItem>(`${OT_BASE}/ot/checklists/${input.checklistId}/items`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.checklists() }),
  });
}

export function useCompleteChecklistItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (itemId: number) =>
      otJson<ChecklistItem>(`${OT_BASE}/ot/checklists/items/${itemId}/complete`, { method: "POST" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.checklists() }),
  });
}

export function useRecordChecklistCompletion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { checklistId: number; overallStatus: string }) =>
      otJson<ChecklistCompletion>(`${OT_BASE}/ot/checklists/${input.checklistId}/completion`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.checklists() }),
  });
}

// ---- Billing Estimation ---------------------------------------------------

export function useBillingEstimates(scheduleId?: number) {
  return useQuery<OtBillingEstimate[]>({
    queryKey: otQueryKeys.billingEstimates(scheduleId),
    queryFn: () =>
      otJson<OtBillingEstimate[]>(
        `${OT_BASE}/ot/billing/estimates${scheduleId ? `?scheduleId=${scheduleId}` : ""}`
      ),
  });
}

export function useCreateBillingEstimate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { scheduleId: number; procedureCode: string }) =>
      otJson<OtBillingEstimate>(`${OT_BASE}/ot/billing/estimates`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.billingEstimates() }),
  });
}

export function useFinalizeBillingEstimate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (estimateId: number) =>
      otJson<OtBillingEstimate>(`${OT_BASE}/ot/billing/estimates/${estimateId}/finalize`, {
        method: "POST",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.billingEstimates() }),
  });
}

// ---- Staff Schedule -------------------------------------------------------

export function useStaffSchedule(scheduleId: number) {
  return useQuery<OtStaffSchedule[]>({
    queryKey: otQueryKeys.staffSchedule(scheduleId),
    queryFn: () => otJson<OtStaffSchedule[]>(`${OT_BASE}/ot/staff-schedule?scheduleId=${scheduleId}`),
    enabled: scheduleId > 0,
  });
}

export function useAssignStaff() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { scheduleId: number; role: string; staffId: number; start: string; end: string }) =>
      otJson<OtStaffSchedule>(`${OT_BASE}/ot/staff-schedule`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ot", "staff-schedule"] }),
  });
}

// ---- Nurse Schedule -------------------------------------------------------

export function useNurseSchedule(staffId: number) {
  return useQuery<OtNurseSchedule[]>({
    queryKey: otQueryKeys.nurseSchedule(staffId),
    queryFn: () => otJson<OtNurseSchedule[]>(`${OT_BASE}/ot/nurse-schedule?staffId=${staffId}`),
    enabled: staffId > 0,
  });
}

export function useCreateNurseDuty() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      staffId: number;
      dutyDate: string;
      shiftType: string;
      dutyRoster: string;
      emergencyCall?: boolean;
    }) =>
      otJson<OtNurseSchedule>(`${OT_BASE}/ot/nurse-schedule`, {
        method: "POST",
        body: JSON.stringify(input),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["ot", "nurse-schedule"] }),
  });
}

// Backwards-compat re-exports (preserves old API)
export function useUpdateOtScheduleStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      otJson<OtSchedule>(`${OT_BASE}/api/v1/ot/schedules/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: otQueryKeys.schedules() }),
  });
}
