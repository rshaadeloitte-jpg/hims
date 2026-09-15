import { z } from "zod";

/** M15 Lab schemas — mirror lab-service request DTOs. */

const optionalNumber = z.preprocess((v) => (v === "" || v == null ? undefined : v), z.coerce.number().optional());

/** Create lab order (POST /api/v1/lab/orders). */
export const labOrderSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient is required"),
  encounterId: optionalNumber,
  isOutSourced: z.boolean().default(false),
  tests: z.array(z.record(z.string(), z.unknown())).default([]),
});

export type LabOrderValues = z.infer<typeof labOrderSchema>;

/** Lab result entry (POST /api/v1/lab/orders/{id}/details/{detailId}/results). */
export const labResultSchema = z.object({
  results: z.array(z.record(z.string(), z.unknown())).default([]),
});

export type LabResultValues = z.infer<typeof labResultSchema>;

/** Reject reason (POST /api/v1/lab/orders/{id}/details/{detailId}/reject). */
export const labRejectSchema = z.object({
  reason: z.string().min(1, "Reason is required"),
});

export type LabRejectValues = z.infer<typeof labRejectSchema>;