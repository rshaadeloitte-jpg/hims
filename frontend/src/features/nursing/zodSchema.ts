import { z } from "zod";

/** M09 Nursing schemas — mirror nursing-service request DTOs. */

const optionalNumber = z.preprocess((v) => (v === "" || v == null ? undefined : v), z.coerce.number().optional());
const optionalString = z.preprocess((v) => (v === "" ? undefined : v), z.string().optional());

/** Care plan (POST /api/v1/care-plans). */
export const carePlanSchema = z.object({
  admissionId: z.coerce.number().int().positive("Admission is required"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  title: z.string().min(1, "Title is required"),
  description: optionalString,
  goals: z.array(z.string()).default([]),
  interventions: z.array(z.string()).default([]),
});

export type CarePlanValues = z.infer<typeof carePlanSchema>;

/** Shift handover (POST /api/v1/shift-handovers). */
export const shiftHandoverSchema = z.object({
  admissionId: z.coerce.number().int().positive("Admission is required"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  fromNurseId: z.coerce.number().int().positive("From nurse required"),
  toNurseId: z.coerce.number().int().positive("To nurse required"),
  shift: z.string().min(1, "Shift is required"),
  summary: optionalString,
  notes: optionalString,
});

export type ShiftHandoverValues = z.infer<typeof shiftHandoverSchema>;

/** Nurse allocation (POST /api/v1/nurse-allocations). */
export const nurseAllocationSchema = z.object({
  admissionId: z.coerce.number().int().positive("Admission is required"),
  nurseId: z.coerce.number().int().positive("Nurse is required"),
  wardId: optionalNumber,
  shift: z.string().min(1, "Shift is required"),
  startDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd"),
});

export type NurseAllocationValues = z.infer<typeof nurseAllocationSchema>;

/** Incident report (POST /api/v1/incidents). */
export const incidentSchema = z.object({
  admissionId: z.coerce.number().int().positive("Admission is required"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  type: z.string().min(1, "Type is required"),
  severity: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]).default("LOW"),
  description: z.string().min(1, "Description is required"),
  reportedBy: optionalNumber,
});

export type IncidentValues = z.infer<typeof incidentSchema>;

/** Vitals (POST /api/v1/patients/{patientId}/vitals). */
export const vitalsSchema = z.object({
  temperature: optionalNumber,
  pulse: optionalNumber,
  respiration: optionalNumber,
  bloodPressureSystolic: optionalNumber,
  bloodPressureDiastolic: optionalNumber,
  spO2: optionalNumber,
  weight: optionalNumber,
  height: optionalNumber,
  recordedAt: optionalString,
});

export type VitalsValues = z.infer<typeof vitalsSchema>;

/** Nursing note (POST /api/v1/admissions/{admissionId}/notes). */
export const nursingNoteSchema = z.object({
  content: z.string().min(1, "Note content is required"),
  category: optionalString,
  recordedBy: optionalNumber,
});

export type NursingNoteValues = z.infer<typeof nursingNoteSchema>;

/** Nursing task (POST /api/v1/tasks). */
export const nursingTaskSchema = z.object({
  admissionId: z.coerce.number().int().positive("Admission is required"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  taskType: z.string().min(1, "Task type is required"),
  description: z.string().min(1, "Description is required"),
  dueTime: optionalString,
  assignedTo: optionalNumber,
});

export type NursingTaskValues = z.infer<typeof nursingTaskSchema>;
