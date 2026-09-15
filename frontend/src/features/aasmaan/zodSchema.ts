import { z } from "zod";

/**
 * M37 Aasmaan Zod schemas — mirror the aasmaan-service request DTOs.
 * Numeric HTML inputs come through as strings; `z.coerce.number()` normalises them.
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

/** Create/update a field case (POST /cases or PUT /cases/{id}). */
export const caseSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient ID is required"),
  caseType: z.string().min(1, "Case type is required").max(40),
  status: z.coerce.number().int().min(1).max(3).default(1),
  openedOn: z.string().optional(),
  closedOn: z.string().optional(),
});

export type CaseValues = z.infer<typeof caseSchema>;

/** Case list filters. */
export const caseFilterSchema = z.object({
  status: z.enum(["", "1", "2", "3"]).optional(),
  caseType: optionalString,
  patientId: optionalNumber,
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type CaseFilterValues = z.infer<typeof caseFilterSchema>;

/** Create an e-ticket (POST /e-tickets). */
export const eTicketSchema = z.object({
  caseId: optionalNumber,
  subject: z.string().min(1, "Subject is required").max(120),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]).default("MEDIUM"),
  status: z.enum(["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]).default("OPEN"),
});

export type ETicketValues = z.infer<typeof eTicketSchema>;

/** Record a monitoring metric (POST /monitor). */
export const monitorSchema = z.object({
  caseId: z.coerce.number().int().positive("Case ID is required"),
  metric: z.string().min(1, "Metric name is required").max(40),
  value: z.coerce.number().finite("Value must be a valid number"),
  recordedAt: z.string().optional(),
});

export type MonitorValues = z.infer<typeof monitorSchema>;

/** IoT device registration (modeled from API catalog device endpoints). */
export const deviceSchema = z.object({
  deviceName: z.string().min(1, "Device name is required").max(80),
  deviceType: z.enum(["BP_MONITOR", "GLUCOMETER", "PULSE_OXIMETER", "THERMOMETER", "WEIGHT_SCALE", "ECG", "OTHER"]).default("OTHER"),
  serialNo: z.string().min(1, "Serial number is required").max(60),
  status: z.enum(["ACTIVE", "INACTIVE", "MAINTENANCE"]).default("ACTIVE"),
  lastSeenAt: z.string().optional(),
});

export type DeviceValues = z.infer<typeof deviceSchema>;
