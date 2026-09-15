import { z } from "zod";

const toNumber = (v: unknown): number | undefined => {
  if (v === "" || v == null) return undefined;
  const n = typeof v === "number" ? v : Number(v);
  return Number.isNaN(n) ? NaN : n;
};

// ---- OT Schedule (BR-OT-001 / BR-OT-002) -----------------------------------

export const otScheduleSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient ID required"),
  encounterId: z.preprocess(toNumber, z.number().int().positive().optional()),
  admissionId: z.preprocess(toNumber, z.number().int().positive().optional()),
  otTable: z.string().min(1, "OT table/room required"),
  scheduledStart: z.string().min(1, "Start time required"),
  scheduledEnd: z.string().min(1, "End time required"),
  anesthetistId: z.preprocess(toNumber, z.number().int().positive().optional()),
  surgeonId: z.preprocess(toNumber, z.number().int().positive().optional()),
  preBilling: z.boolean().default(false),
  procedures: z.array(z.object({
    procedureCode: z.string().min(1, "Procedure code required"),
    procedureName: z.string().min(1, "Procedure name required"),
    rate: z.preprocess(toNumber, z.number().min(0).optional()),
    dependentOfIndex: z.preprocess(toNumber, z.number().int().min(0).optional()),
    discountPct: z.preprocess(toNumber, z.number().min(0).max(100).optional()),
  })).default([]),
}).refine((d) => d.encounterId != null || d.admissionId != null, {
  message: "Either encounterId or admissionId is required (BR-OT-001)",
  path: ["encounterId"],
});

export type OtScheduleForm = z.infer<typeof otScheduleSchema>;

// ---- Anesthesia Record ----------------------------------------------------

export const anesthesiaPreOpSchema = z.object({
  scheduleId: z.coerce.number().int().positive("Schedule ID required"),
  patientId: z.coerce.number().int().positive("Patient ID required"),
  asaGrade: z.string().min(1, "ASA grade required"),
  airway: z.string().min(1, "Airway assessment required"),
  comorbidities: z.string().optional(),
});

export type AnesthesiaPreOpForm = z.infer<typeof anesthesiaPreOpSchema>;

export const vitalSignSchema = z.object({
  recordedAt: z.string().min(1, "Timestamp required"),
  hr: z.preprocess(toNumber, z.number().int().min(0).max(300).optional()),
  bpSys: z.preprocess(toNumber, z.number().int().min(0).max(300).optional()),
  bpDia: z.preprocess(toNumber, z.number().int().min(0).max(200).optional()),
  spo2: z.preprocess(toNumber, z.number().int().min(0).max(100).optional()),
  rr: z.preprocess(toNumber, z.number().int().min(0).max(60).optional()),
  temp: z.preprocess(toNumber, z.number().min(30).max(45).optional()),
});

export type VitalSignForm = z.infer<typeof vitalSignSchema>;

export const anesthesiaDrugSchema = z.object({
  drugName: z.string().min(1, "Drug name required"),
  dose: z.string().min(1, "Dose required"),
  route: z.string().min(1, "Route required"),
  administeredAt: z.string().min(1, "Administration time required"),
});

export type AnesthesiaDrugForm = z.infer<typeof anesthesiaDrugSchema>;

export const anesthesiaPostOpSchema = z.object({
  pacuMinutes: z.preprocess(toNumber, z.number().int().min(0).optional()),
  recoveryScore: z.preprocess(toNumber, z.number().int().min(0).max(10).optional()),
  complications: z.string().optional(),
});

export type AnesthesiaPostOpForm = z.infer<typeof anesthesiaPostOpSchema>;

// ---- Surgical Checklist (WHO) ---------------------------------------------

export const checklistCreateSchema = z.object({
  scheduleId: z.coerce.number().int().positive("Schedule ID required"),
  patientId: z.coerce.number().int().positive("Patient ID required"),
  phase: z.enum(["SIGN_IN", "TIME_OUT", "SIGN_OUT"], { required_error: "Checklist phase required" }),
});

export type ChecklistCreateForm = z.infer<typeof checklistCreateSchema>;

export const checklistItemSchema = z.object({
  itemName: z.string().min(1, "Item name required"),
  category: z.string().min(1, "Category required"),
});

export type ChecklistItemForm = z.infer<typeof checklistItemSchema>;

export const checklistCompletionSchema = z.object({
  overallStatus: z.enum(["PENDING", "COMPLETED", "ABORTED"], { required_error: "Overall status required" }),
});

export type ChecklistCompletionForm = z.infer<typeof checklistCompletionSchema>;

// ---- OT Room --------------------------------------------------------------

export const otRoomSchema = z.object({
  name: z.string().min(1, "Room name required"),
  type: z.string().min(1, "Room type required"),
});

export type OtRoomForm = z.infer<typeof otRoomSchema>;
