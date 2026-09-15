import { z } from "zod";

/** M16 Radiology schemas — mirror radiology-service request DTOs. */

const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().int().positive().optional(),
);

const optionalString = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.string().optional(),
);

/** Create rad order line (item + qty). */
export const radOrderLineSchema = z.object({
  itemId: z.coerce.number().int().positive("Study/item is required"),
  qty: z.coerce.number().positive("Quantity must be > 0").default(1),
});

export type RadOrderLineValues = z.infer<typeof radOrderLineSchema>;

/** Create rad order booking (POST /api/v1/rad-orders). */
export const radOrderBookingSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient is required"),
  encounterId: optionalNumber,
  admissionId: optionalNumber,
  modality: z.enum(["XRAY", "CT", "MRI", "US"], {
    required_error: "Modality is required",
  }),
  priority: z.enum(["ROUTINE", "URGENT"]).default("ROUTINE"),
  lines: z.array(radOrderLineSchema).min(1, "At least one study line is required"),
});

export type RadOrderBookingValues = z.infer<typeof radOrderBookingSchema>;

/** Report entry (POST /api/v1/rad-orders/{id}/report). */
export const radReportSchema = z.object({
  reportText: z.string().min(1, "Report text is required"),
  reportedBy: z.string().min(1, "Reporter name is required"),
});

export type RadReportValues = z.infer<typeof radReportSchema>;

/** Reschedule request (POST /api/v1/radiology/orders/{orderId}/lines/{lineNo}/reschedule). */
export const radRescheduleSchema = z.object({
  rescheduledTo: z.string().min(1, "Reschedule date/time is required"),
});

export type RadRescheduleValues = z.infer<typeof radRescheduleSchema>;

/** Retest request (POST /api/v1/radiology/orders/{orderId}/lines/{lineNo}/retest). */
export const radRetestSchema = z.object({
  reason: z.string().min(1, "Reason is required for retest"),
});

export type RadRetestValues = z.infer<typeof radRetestSchema>;

/** Radiology report template (POST /api/v1/radiology/templates). */
export const radTemplateSchema = z.object({
  templateCode: z.string().min(1, "Template code is required"),
  modality: z.enum(["XRAY", "CT", "MRI", "US"], {
    required_error: "Modality is required",
  }),
  bodyTemplate: z.string().min(1, "Body template is required"),
  description: optionalString,
});

export type RadTemplateValues = z.infer<typeof radTemplateSchema>;

/** Radiology template update (PATCH — same as create but for edit). */
export const radTemplateUpdateSchema = radTemplateSchema.partial();

export type RadTemplateUpdateValues = z.infer<typeof radTemplateUpdateSchema>;
