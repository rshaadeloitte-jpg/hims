import { z } from "zod";

/**
 * M22 Grievance schemas — mirror grievance-service request DTOs.
 * Matches GrievanceTicket (raise), GrievanceSeverity (master),
 * GrievanceFeedback (5-question satisfaction + free text).
 */

/** Raise grievance ticket (POST /api/v1/grievances). */
export const grievanceSchema = z.object({
  name: z.string().min(1, "Name is required"),
  mobileNo: z
    .string()
    .optional()
    .transform((v) => (v === "" ? undefined : v)),
  address: z.string().optional(),
  categoryId: z.coerce.number().int().positive("Category is required"),
  subCategoryIds: z.array(z.coerce.number()).default([]),
  issue: z.string().min(1, "Issue description is required").max(2000, "Issue too long"),
  severityId: z.coerce.number().int().nonnegative("Severity is required"),
  unitId: z.coerce.number().int().positive().optional(),
  isCitizen: z.boolean().default(true),
  mrno: z.string().optional(),
});

export type GrievanceFormValues = z.infer<typeof grievanceSchema>;

/** Feedback submission (POST /api/v1/grievances/{id}/feedback). */
export const feedbackSchema = z.object({
  grievanceId: z.coerce.number().int().positive(),
  question1: z.coerce.number().int().min(1).max(5, "Rating 1-5 required"),
  question2: z.coerce.number().int().min(1).max(5, "Rating 1-5 required"),
  question3: z.coerce.number().int().min(1).max(5, "Rating 1-5 required"),
  question4: z.coerce.number().int().min(1).max(5, "Rating 1-5 required"),
  question5: z.coerce.number().int().min(1).max(5, "Rating 1-5 required"),
  freeTextFeedback: z.string().max(1000, "Feedback too long").optional(),
  email: z.string().email("Invalid email").optional().or(z.literal("")),
});

export type FeedbackFormValues = z.infer<typeof feedbackSchema>;

/** Status change request. */
export const statusChangeSchema = z.object({
  issueStatus: z.coerce.number().int().min(1).max(4),
  assignToCreator: z.boolean().default(false),
});

export type StatusChangeValues = z.infer<typeof statusChangeSchema>;

/** Severity dictionary (grievance_severity master). */
export const SEVERITY_CODE = {
  LOW: "LOW",
  MEDIUM: "MEDIUM",
  HIGH: "HIGH",
  CRITICAL: "CRITICAL",
} as const;

export type SeverityCode = (typeof SEVERITY_CODE)[keyof typeof SEVERITY_CODE];

/** Issue status dictionary (legacy). */
export const ISSUE_STATUS = {
  1: "NEW",
  2: "ASSIGNED",
  3: "RESOLVED",
  4: "ASSIGN_TO_CREATOR",
} as const;

export type IssueStatusKey = keyof typeof ISSUE_STATUS;
export type IssueStatusLabel = (typeof ISSUE_STATUS)[IssueStatusKey];

/** Close status dictionary. */
export const CLOSE_STATUS = {
  1: "PENDING",
  2: "COMPLETED",
  3: "CLOSED",
} as const;

/** Question labels for feedback form. */
export const FEEDBACK_QUESTIONS = [
  "How satisfied are you with the resolution?",
  "How would you rate the response time?",
  "How clear was the communication?",
  "How professional was the support staff?",
  "Overall, how likely are you to recommend our service?",
] as const;
