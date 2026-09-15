import { z } from "zod";

/**
 * M08 Clinical Orders Zod schemas — mirror the ordering-service request DTOs.
 * Covers: clinical orders, order sets (batch instantiation), CWF/SCWF
 * (Clinical Workflow / Special Clinical Workflow multi-step approvals),
 * and external order details.
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

// ---- Clinical order -------------------------------------------------------

export const clinicalOrderSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient ID required"),
  orderType: z.string().min(1, "Order type required"),
  description: z.string().min(1, "Description required"),
  priority: z.enum(["ROUTINE", "URGENT", "STAT"]),
});

export type ClinicalOrderForm = z.infer<typeof clinicalOrderSchema>;

// ---- Order set ------------------------------------------------------------

/** A single line inside an order set — one service order to instantiate. */
export const orderSetItemSchema = z.object({
  id: z.string().min(1),
  serviceType: z.enum([
    "CONSULT",
    "LAB",
    "RADIOLOGY",
    "NURSING",
    "PROCEDURE",
    "CONSUMABLES",
    "MISCELLANEOUS",
  ]),
  serviceId: z.coerce.number().int().positive("Service is required"),
  serviceName: z.string().min(1, "Service name is required"),
  qty: z.coerce.number().int().positive("Qty must be >= 1").default(1),
  priority: z.enum(["ROUTINE", "URGENT", "STAT"]).default("ROUTINE"),
});

export type OrderSetItem = z.infer<typeof orderSetItemSchema>;

/** Order set definition — a named, ordered bundle of service orders. */
export const orderSetSchema = z.object({
  name: z.string().min(1, "Order set name is required").max(200),
  code: z.string().min(1, "Code is required").max(64),
  category: z.enum([
    "CONSULT",
    "LAB",
    "RADIOLOGY",
    "NURSING",
    "PROCEDURE",
    "CONSUMABLES",
    "MISCELLANEOUS",
  ]),
  description: optionalString,
  items: z
    .array(orderSetItemSchema)
    .min(1, "At least one item is required"),
  status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
});

export type OrderSetForm = z.infer<typeof orderSetSchema>;

/** Payload to apply (instantiate) an order set for a patient. */
export const applyOrderSetSchema = z.object({
  orderSetId: z.coerce.number().int().positive("Order set is required"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  encounterId: optionalNumber,
  admissionId: optionalNumber,
});

export type ApplyOrderSetForm = z.infer<typeof applyOrderSetSchema>;

// ---- CWF / SCWF (Clinical Workflow) --------------------------------------

/** A single step in a clinical workflow definition. */
export const cwfStepSchema = z.object({
  stepId: z.coerce.number().int().positive(),
  stepName: z.string().min(1, "Step name is required"),
  stepOrder: z.coerce.number().int().nonnegative(),
  role: z.enum(["DOCTOR", "NURSE", "LAB_TECH", "RAD_TECH", "PHARMACIST"]),
  isMandatory: z.boolean().default(true),
  formSchema: optionalString,  // JSON schema reference for structured data
});

export type CwfStep = z.infer<typeof cwfStepSchema>;

/** CWF definition — maps a service type to an ordered set of approval steps. */
export const cwfDefinitionSchema = z.object({
  cwfId: z.coerce.number().int().positive(),
  cwfName: z.string().min(1, "CWF name is required"),
  serviceType: z.enum([
    "CONSULT",
    "LAB",
    "RADIOLOGY",
    "NURSING",
    "PROCEDURE",
    "CONSUMABLES",
    "MISCELLANEOUS",
  ]),
  steps: z.array(cwfStepSchema).min(1, "At least one step required"),
  status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
});

export type CwfDefinitionForm = z.infer<typeof cwfDefinitionSchema>;

/** SCWF — special clinical workflow with conditional branching. */
export const scwfDefinitionSchema = z.object({
  scwfId: z.coerce.number().int().positive(),
  scwfName: z.string().min(1, "SCWF name is required"),
  serviceType: z.enum([
    "CONSULT",
    "LAB",
    "RADIOLOGY",
    "NURSING",
    "PROCEDURE",
    "CONSUMABLES",
    "MISCELLANEOUS",
  ]),
  condition: z.string().min(1, "Trigger condition is required"),
  steps: z.array(cwfStepSchema).min(1, "At least one step required"),
  status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
});

export type ScwfDefinitionForm = z.infer<typeof scwfDefinitionSchema>;

/** Runtime CWF/SCWF step status on an order. */
export const workflowStepStatusSchema = z.object({
  stepId: z.number(),
  stepName: z.string(),
  stepOrder: z.number(),
  role: z.string(),
  status: z.enum(["PENDING", "IN_PROGRESS", "COMPLETED", "SKIPPED", "REJECTED"]),
  assignedTo: optionalString,
  completedAt: optionalString,
  completedBy: optionalString,
  remarks: optionalString,
});

export type WorkflowStepStatus = z.infer<typeof workflowStepStatusSchema>;

// ---- External order details ----------------------------------------------

export const externalOrderSchema = z.object({
  orderId: z.coerce.number().int().positive(),
  externalLabName: z.string().min(1, "Lab name is required"),
  externalLabCode: optionalString,
  testName: z.string().min(1, "Test name is required"),
  testCode: optionalString,
  sampleType: optionalString,
  instructions: optionalString,
  status: z.enum(["PENDING", "SENT", "RECEIVED", "CANCELLED"]).default("PENDING"),
});

export type ExternalOrderForm = z.infer<typeof externalOrderSchema>;

// ---- Order status update --------------------------------------------------

export const orderStatusUpdateSchema = z.object({
  status: z.enum([
    "PENDING",
    "IN_PROGRESS",
    "COMPLETED",
    "CANCELLED",
    "VERIFIED",
    "REJECTED",
  ]),
  remarks: optionalString,
});

export type OrderStatusUpdateForm = z.infer<typeof orderStatusUpdateSchema>;
