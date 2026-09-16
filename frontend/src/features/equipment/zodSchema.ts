import { z } from "zod";

/**
 * M20 Equipment Zod schemas — mirror the equipment-service request DTOs.
 * Covers: asset register (Equipment), complaint lifecycle + resolution,
 * preventive-maintenance schedules, executions (PM + corrective +
 * calibration), and the master dictionaries.
 *
 * Codes-as-ids rule: equipment-service stores numeric codes (1..N) in the
 * id columns of working_status / complaint_status / pm_status / maintenance_type
 * dictionaries. The schemas below validate the wire shapes exactly.
 */

// ---- Shared preprocess helpers --------------------------------------------


const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().int().positive().optional(),
);

const optionalString = z.preprocess(
  (v) => (v === "" ? undefined : v),
  z.string().optional(),
);

const optionalDateString = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.string().optional(),
);

// ---- Working status dictionary --------------------------------------------

export const WORKING_STATUSES = [1, 2, 3, 4] as const;
export const workingStatusSchema = z.enum(["WORKING", "NOT_WORKING", "UNDER_REPAIR", "CONDEMNED"]);

// ---- Complaint status dictionary (complaint_status_master) -----------------

export const COMPLAINT_STATUSES = [1, 2, 3, 4] as const;
export const complaintStatusSchema = z.enum(["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]);

// ---- Maintenance type dictionary (maintenance_type_master) ------------------

export const MAINTENANCE_TYPES = [1, 2, 3] as const;
export const maintenanceTypeSchema = z.enum(["PREVENTIVE", "CORRECTIVE", "CALIBRATION"]);

// ---- PM status dictionary (pm_status) --------------------------------------

export const PM_STATUSES = [1, 2, 3, 4] as const;
export const pmStatusSchema = z.enum(["SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"]);

// ---- Equipment (asset register) -------------------------------------------

export const equipmentSchema = z.object({
  itemId: z.coerce.number().int().positive("Item is required"),
  accessionNo: z.string().min(1, "Accession number required"),
  serialNo: optionalString,
  model: optionalString,
  make: optionalString,
  purchaseDate: optionalDateString,
  installationDate: optionalDateString,
  warrantyExpiry: optionalDateString,
  departmentId: optionalNumber,
  storeId: optionalNumber,
  location: optionalString,
  cost: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.coerce.number().nonnegative().optional(),
  ),
  supplierId: optionalNumber,
  custodianName: optionalString,
  documents: optionalString,
  workingStatus: z.coerce.number().int().min(1).max(4).default(1),
});

export type EquipmentForm = z.infer<typeof equipmentSchema>;

/** Update variant — all fields optional (null patch = ignored by server). */
export const equipmentUpdateSchema = equipmentSchema.partial();
export type EquipmentUpdateForm = z.infer<typeof equipmentUpdateSchema>;

// ---- Equipment transfer ----------------------------------------------------

export const equipmentTransferSchema = z.object({
  toDepartmentId: z.coerce.number().int().positive("Target department required"),
  reason: optionalString,
  transferDate: optionalDateString,
  toLocation: optionalString,
  toStoreId: optionalNumber,
});

export type EquipmentTransferForm = z.infer<typeof equipmentTransferSchema>;

// ---- Equipment complaint ---------------------------------------------------

export const equipmentComplaintSchema = z.object({
  equipmentId: z.coerce.number().int().positive("Equipment is required"),
  categoryId: optionalNumber,
  complaintTypeId: optionalNumber,
  severityId: optionalNumber,
  description: z.string().min(1, "Description required"),
  reportedBy: optionalNumber,
  reportedDate: optionalDateString,
  roomNo: optionalString,
  extensionNo: optionalString,
  assignedTo: optionalNumber,
  expectedResolutionDate: optionalDateString,
});

export type EquipmentComplaintForm = z.infer<typeof equipmentComplaintSchema>;

export const complaintStatusUpdateSchema = z.object({
  complaintStatus: z.coerce.number().int().min(1).max(4),
});

export type ComplaintStatusUpdateForm = z.infer<typeof complaintStatusUpdateSchema>;

// ---- Complaint resolution --------------------------------------------------

export const complaintResolutionSchema = z.object({
  resolutionNotes: z.string().min(1, "Resolution notes required"),
  partsReplaced: optionalString,
  serviceAgentId: optionalNumber,
  assignedTo: optionalNumber,
  assignedById: optionalNumber,
  resolvedStatus: z.coerce.number().int().min(1).max(4).default(3),
  resolutionDate: optionalDateString,
});

export type ComplaintResolutionForm = z.infer<typeof complaintResolutionSchema>;

/** A single replaced-part line within a resolution. */
export const resolutionDetailSchema = z.object({
  engineerId: optionalNumber,
  itemId: optionalNumber,
  batchId: optionalNumber,
  batchCode: optionalString,
  expiryDate: optionalDateString,
  quantity: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.coerce.number().positive().optional(),
  ),
  cost: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.coerce.number().nonnegative().optional(),
  ),
  totalAmount: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.coerce.number().nonnegative().optional(),
  ),
  storeId: optionalNumber,
});

export type ResolutionDetailForm = z.infer<typeof resolutionDetailSchema>;

/** Full create-resolution payload: resolution header + line items. */
export const complaintResolutionWithDetailsSchema = complaintResolutionSchema.extend({
  details: z.array(resolutionDetailSchema).optional(),
});

export type ComplaintResolutionWithDetailsForm = z.infer<typeof complaintResolutionWithDetailsSchema>;

// ---- PM schedule -----------------------------------------------------------

const monthFlagSchema = z.boolean().default(false);

export const pmScheduleSchema = z.object({
  equipmentId: z.coerce.number().int().positive("Equipment is required"),
  scheduleCode: optionalString,
  frequency: z.coerce.number().int().positive("Frequency (months) required"),
  pmDate: optionalDateString,
  jan: monthFlagSchema,
  feb: monthFlagSchema,
  mar: monthFlagSchema,
  apr: monthFlagSchema,
  may: monthFlagSchema,
  jun: monthFlagSchema,
  jul: monthFlagSchema,
  aug: monthFlagSchema,
  sep: monthFlagSchema,
  oct: monthFlagSchema,
  nov: monthFlagSchema,
  dec: monthFlagSchema,
  pmType: z.coerce.number().int().min(1).max(3).default(1),
  storeId: optionalNumber,
});

export type PmScheduleForm = z.infer<typeof pmScheduleSchema>;

/** Planned occurrence line within a PM schedule. */
export const pmScheduleDetailSchema = z.object({
  scheduledDate: optionalDateString,
  preventiveMaintenance: z.boolean().default(true),
});

export type PmScheduleDetailForm = z.infer<typeof pmScheduleDetailSchema>;

/** Full create-schedule payload: header + planned occurrence lines. */
export const pmScheduleWithDetailsSchema = pmScheduleSchema.extend({
  details: z.array(pmScheduleDetailSchema).optional(),
});

export type PmScheduleWithDetailsForm = z.infer<typeof pmScheduleWithDetailsSchema>;

// ---- PM execution (completion) ---------------------------------------------

export const pmCompletionSchema = z.object({
  maintenanceNo: optionalString,
  maintenanceDate: optionalDateString,
  performedBy: optionalNumber,
  functionalCheckRemark: optionalString,
  preventiveMaintenanceRemark: optionalString,
  electricalSafetyRemark: optionalString,
  calibration: optionalString,
  furtherActions: optionalString,
  commentsOnUse: optionalString,
  remarks: optionalString,
  deptInCharge: optionalNumber,
  biomedicalEngineerId: optionalNumber,
  biomedicalInChargeId: optionalNumber,
  verifiedBy: optionalNumber,
  amountIncurred: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.coerce.number().nonnegative().optional(),
  ),
  serviceAgentId: optionalNumber,
  pmScheduleDetailId: optionalNumber,
  itemId: optionalNumber,
});

export type PmCompletionForm = z.infer<typeof pmCompletionSchema>;

// ---- PM checklist template -------------------------------------------------

export const pmChecklistSchema = z.object({
  code: z.string().min(1, "Code required"),
  description: z.string().min(1, "Description required"),
});

export type PmChecklistForm = z.infer<typeof pmChecklistSchema>;

// ---- Master dictionaries ---------------------------------------------------

export const complaintStatusMasterSchema = z.object({
  code: z.string().min(1, "Code required"),
  description: z.string().min(1, "Description required"),
  isResolved: z.boolean().default(false),
  isClosed: z.boolean().default(false),
  isReopened: z.boolean().default(false),
});

export type ComplaintStatusMasterForm = z.infer<typeof complaintStatusMasterSchema>;

export const maintenanceTypeMasterSchema = z.object({
  code: z.string().min(1, "Code required"),
  description: z.string().min(1, "Description required"),
});

export type MaintenanceTypeMasterForm = z.infer<typeof maintenanceTypeMasterSchema>;

// ---- Complaint search filter -----------------------------------------------

export const complaintFilterSchema = z.object({
  unitId: optionalNumber,
  equipmentId: optionalNumber,
  complaintStatus: optionalNumber,
  search: optionalString,
});

export type ComplaintFilterForm = z.infer<typeof complaintFilterSchema>;

// ---- PM schedule search filter ---------------------------------------------

export const pmScheduleFilterSchema = z.object({
  unitId: optionalNumber,
  equipmentId: optionalNumber,
  pmType: optionalNumber,
  dueFrom: optionalDateString,
  dueTo: optionalDateString,
  search: optionalString,
});

export type PmScheduleFilterForm = z.infer<typeof pmScheduleFilterSchema>;

// ---- Equipment search filter -----------------------------------------------

export const equipmentFilterSchema = z.object({
  unitId: optionalNumber,
  workingStatus: optionalNumber,
  search: optionalString,
});

export type EquipmentFilterForm = z.infer<typeof equipmentFilterSchema>;
