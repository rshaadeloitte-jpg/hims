import { z } from "zod";

/**
 * M02 Zod schemas — mirror the legacy DTO field set (AddFollowUpVisitInfoDto)
 * the appointment-service controllers expect. Numeric HTML inputs come through
 * as strings; `z.coerce.number()` normalises them on submit.
 */

const optionalNumber = z
  .preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.coerce.number().optional(),
  );

const optionalString = z
  .preprocess(
    (v) => (v === "" ? undefined : v),
    z.string().optional(),
  );

const optionalTime = z
  .preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.string().regex(/^\d{2}:\d{2}(:\d{2})?$/, "Use HH:MM or HH:MM:SS").optional(),
  );

const optionalDate = z
  .preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Use yyyy-MM-dd").optional(),
  );

/** Schedule follow-up against a parent appointment. */
export const followUpSchema = z.object({
  patientId: optionalNumber,
  doctorId: optionalNumber,
  departmentId: optionalNumber,
  unitId: optionalNumber,
  scheduleId: optionalNumber,
  followUpDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Date is required (yyyy-MM-dd)"),
  slotStart: optionalTime,
  slotEnd: optionalTime,
  cabin: optionalString,
  recurrenceRule: optionalString, // RRULE subset, e.g. FREQ=WEEKLY;COUNT=4
  recurrenceEndDate: optionalDate,
});

export type FollowUpValues = z.infer<typeof followUpSchema>;

/** Pre-consult questionnaire (Patient Assessment / Doctor Assessment / Social Assessment). */
export const questionnaireModule = z.enum([
  "Patient Assessment",
  "Doctor Assessment",
  "Social Assessment",
]);

export const questionnaireAnswerSchema = z.object({
  questionId: z.coerce.number().int().positive(),
  answerIds: z.array(z.coerce.number().int()).optional(),
  answerText: z.string().optional(),
  answerGrp: z.string().optional(),
});

export const questionnaireSchema = z.object({
  module: questionnaireModule,
  moduleType: optionalString,
  answers: z.array(questionnaireAnswerSchema).default([]),
  clinicianReviewFlag: z.boolean().optional(),
});

export type QuestionnaireValues = z.infer<typeof questionnaireSchema>;

/** Walk-in check-in (POST /api/v1/appointments/walkin). */
export const walkinSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient is required"),
  doctorId: z.coerce.number().int().positive("Doctor is required"),
  visitType: z.enum(["WALK_IN", "EMERGENCY", "APPOINTMENT"]).default("WALK_IN"),
  departmentId: optionalNumber,
  cabin: optionalString,
  unitId: optionalNumber,
  scheduleId: optionalNumber,
});

export type WalkinValues = z.infer<typeof walkinSchema>;
