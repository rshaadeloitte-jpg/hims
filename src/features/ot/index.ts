/**
 * M12 OT/Surgery Feature Module
 *
 * Components:
 * - SurgeryScheduler: Linear calendar + slot booking for OT schedules
 * - OTDashboard: Real-time room status board with timeline
 * - AnesthesiaRecord: Pre-op, intra-op vitals/drugs, post-op PACU
 * - SurgicalChecklist: WHO Surgical Safety Checklist (Sign In / Time Out / Sign Out)
 *
 * Consumes the ot-service REST API (Spring Boot).
 * Legacy: OtPage (original monolithic page, deprecated).
 */

// Components
export { SurgeryScheduler } from "./SurgeryScheduler";
export { OTDashboard } from "./OTDashboard";
export { AnesthesiaRecord } from "./AnesthesiaRecord";
export { SurgicalChecklist } from "./SurgicalChecklist";

// Legacy (deprecated)
export { OtPage } from "./OtPage";

// Queries
export {
  useOtRooms,
  useCreateOtRoom,
  useOtSchedules,
  useOtSchedule,
  useCreateOtSchedule,
  useStartSurgery,
  useCompleteSurgery,
  useCancelSurgery,
  useOtDashboard,
  useAnesthesiaRecords,
  useAnesthesiaRecord,
  useCreatePreOp,
  useAddVitalSign,
  useVitalSigns,
  useAddAnesthesiaDrug,
  useAnesthesiaDrugs,
  useCompletePostOp,
  useChecklists,
  useChecklistsBySchedule,
  useCreateChecklist,
  useAddChecklistItem,
  useCompleteChecklistItem,
  useRecordChecklistCompletion,
  useBillingEstimates,
  useCreateBillingEstimate,
  useFinalizeBillingEstimate,
  useStaffSchedule,
  useAssignStaff,
  useNurseSchedule,
  useCreateNurseDuty,
  useUpdateOtScheduleStatus,
  otQueryKeys,
} from "./queries";

// Zod schemas
export {
  otScheduleSchema,
  anesthesiaPreOpSchema,
  vitalSignSchema,
  anesthesiaDrugSchema,
  anesthesiaPostOpSchema,
  checklistCreateSchema,
  checklistItemSchema,
  checklistCompletionSchema,
  otRoomSchema,
} from "./zodSchema";

// Types
export type {
  OtRoom,
  OtProcedure,
  OtScheduleDetail,
  OtSchedule,
  OtScheduleInput,
  AnesthesiaRecord,
  VitalSignRecord,
  AnesthesiaDrug,
  SurgicalChecklist,
  ChecklistItem,
  ChecklistCompletion,
  OtBillingEstimate,
  OtProcedureCost,
  OtStaffSchedule,
  OtNurseSchedule,
} from "./queries";

export type {
  OtScheduleForm,
  AnesthesiaPreOpForm,
  VitalSignForm,
  AnesthesiaDrugForm,
  AnesthesiaPostOpForm,
  ChecklistCreateForm,
  ChecklistItemForm,
  ChecklistCompletionForm,
  OtRoomForm,
} from "./zodSchema";
