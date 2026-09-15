import { z } from "zod";

/**
 * M26 Worklists — Zod schemas for nurse task lists, care goals, and interventions.
 *
 * Linear task list patterns:
 *   - Tasks grouped by patient admission with status / priority / due-time
 *   - Care goals + interventions displayed in tabbed views
 *   - Filter schema mirrors the nursing-service worklist query contract
 */

const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().optional(),
);

const optionalString = z.preprocess(
  (v) => (v === "" ? undefined : v),
  z.string().optional(),
);

/** Worklist filter (GET /api/v1/nursing/worklist). */
export const worklistFilterSchema = z.object({
  dateFrom: z.string().optional(),
  dateTo: z.string().optional(),
  status: z.enum(["ALL", "PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"]).default("ALL"),
  priority: z.enum(["ALL", "LOW", "MEDIUM", "HIGH", "URGENT"]).default("ALL"),
  assignedTo: optionalNumber,
  wardId: optionalNumber,
});

export type WorklistFilterValues = z.infer<typeof worklistFilterSchema>;

/** Nursing task status enum (mirrors backend TaskStatus). */
export const TASK_STATUS = {
  PENDING: "PENDING",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "COMPLETED",
  CANCELLED: "CANCELLED",
} as const;

export type TaskStatus = (typeof TASK_STATUS)[keyof typeof TASK_STATUS];

/** Task priority enum. */
export const TASK_PRIORITY = {
  LOW: "LOW",
  MEDIUM: "MEDIUM",
  HIGH: "HIGH",
  URGENT: "URGENT",
} as const;

export type TaskPriority = (typeof TASK_PRIORITY)[keyof typeof TASK_PRIORITY];

/** Care goal status. */
export const GOAL_STATUS = {
  ACTIVE: "ACTIVE",
  ACHIEVED: "ACHIEVED",
  DISCONTINUED: "DISCONTINUED",
} as const;

export type GoalStatus = (typeof GOAL_STATUS)[keyof typeof GOAL_STATUS];

/** Intervention status. */
export const INTERVENTION_STATUS = {
  SCHEDULED: "SCHEDULED",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "COMPLETED",
  SKIPPED: "SKIPPED",
} as const;

export type InterventionStatus = (typeof INTERVENTION_STATUS)[keyof typeof INTERVENTION_STATUS];

/** Create / update a nursing task (POST/PUT /api/v1/tasks). */
export const nursingTaskFormSchema = z.object({
  admissionId: z.coerce.number().int().positive("Admission is required"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  taskType: z.string().min(1, "Task type is required").max(60),
  description: z.string().min(1, "Description is required").max(500),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]).default("MEDIUM"),
  dueTime: optionalString,
  assignedTo: optionalNumber,
});

export type NursingTaskFormValues = z.infer<typeof nursingTaskFormSchema>;

/** Create a care goal (POST /api/v1/care-plans/{id}/goals). */
export const careGoalFormSchema = z.object({
  carePlanId: z.coerce.number().int().positive("Care plan is required"),
  goalText: z.string().min(1, "Goal is required").max(300),
  targetDate: optionalString,
  status: z.enum(["ACTIVE", "ACHIEVED", "DISCONTINUED"]).default("ACTIVE"),
});

export type CareGoalFormValues = z.infer<typeof careGoalFormSchema>;

/** Create an intervention (POST /api/v1/care-plans/{id}/interventions). */
export const interventionFormSchema = z.object({
  carePlanId: z.coerce.number().int().positive("Care plan is required"),
  interventionText: z.string().min(1, "Intervention is required").max(300),
  scheduledTime: optionalString,
  status: z.enum(["SCHEDULED", "IN_PROGRESS", "COMPLETED", "SKIPPED"]).default("SCHEDULED"),
  assignedTo: optionalNumber,
});

export type InterventionFormValues = z.infer<typeof interventionFormSchema>;

/** Status badge class helper (Linear-style color mapping). */
export function statusBadgeClass(status: TaskStatus | GoalStatus | InterventionStatus): string {
  switch (status) {
    case "PENDING":
    case "SCHEDULED":
    case "ACTIVE":
      return "bg-blue-100 text-blue-800";
    case "IN_PROGRESS":
      return "bg-yellow-100 text-yellow-800";
    case "COMPLETED":
    case "ACHIEVED":
      return "bg-green-100 text-green-800";
    case "CANCELLED":
    case "DISCONTINUED":
    case "SKIPPED":
      return "bg-gray-100 text-gray-600";
    default:
      return "bg-gray-100 text-gray-800";
  }
}

/** Priority indicator class helper. */
export function priorityClass(priority: TaskPriority): string {
  switch (priority) {
    case "URGENT":
      return "bg-red-100 text-red-800 border-red-300";
    case "HIGH":
      return "bg-orange-100 text-orange-800 border-orange-300";
    case "MEDIUM":
      return "bg-blue-50 text-blue-700 border-blue-200";
    case "LOW":
      return "bg-gray-50 text-gray-600 border-gray-200";
    default:
      return "bg-gray-50 text-gray-600 border-gray-200";
  }
}
