import { z } from "zod";

/**
 * M30 BCMA (Bedside Med Admin) Zod schemas.
 * Stripe-style validation: real-time feedback, clean error messaging,
 * progressive disclosure, accessible form controls.
 *
 * Five Rights of Medication Administration:
 *   1. Right Patient
 *   2. Right Drug
 *   3. Right Dose
 *   4. Right Route
 *   5. Right Time
 */

const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().optional(),
);


export const bcmaScanSchema = z.object({
  patientId: z.coerce.number().int().positive(),
  drugId: z.coerce.number().int().positive(),
});

export type BcmaScanForm = z.infer<typeof bcmaScanSchema>;

// ---- Five-rights scan workflow ---------------------------------------------

/** Initial scan submission (patient ID + wristband barcode). */
export const fiveRightsScanSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient ID is required"),
  wristbandBarcode: z
    .string()
    .min(1, "Wristband barcode is required")
    .max(100, "Barcode too long"),
  drugNdc: z
    .string()
    .min(1, "Drug NDC is required")
    .max(50, "NDC too long"),
  encounterId: optionalNumber,
});

export type FiveRightsScanValues = z.infer<typeof fiveRightsScanSchema>;

/** Right-patient verification step. */
export const rightPatientSchema = z.object({
  match: z.boolean(),
});

export type RightPatientValues = z.infer<typeof rightPatientSchema>;

/** Right-drug verification step. */
export const rightDrugSchema = z.object({
  match: z.boolean(),
});

/** Right-dose verification step. */
export const rightDoseSchema = z.object({
  match: z.boolean(),
});

/** Right-route verification step. */
export const rightRouteSchema = z.object({
  match: z.boolean(),
});

/** Right-time verification step. */
export const rightTimeSchema = z.object({
  match: z.boolean(),
});

/** Verification response (server returns the full record). */
export const verificationResponseSchema = z.object({
  bcmaId: z.number(),
  patientId: z.number(),
  drugNdc: z.string(),
  encounterId: z.number().nullable().optional(),
  scanTime: z.string(),
  rightPatient: z.boolean(),
  rightDrug: z.boolean(),
  rightDose: z.boolean(),
  rightRoute: z.boolean(),
  rightTime: z.boolean(),
  allRightsPassed: z.boolean(),
  overridden: z.boolean(),
  overrideReason: z.string().nullable().optional(),
  overrideBy: z.number().nullable().optional(),
  administered: z.boolean(),
  administeredAt: z.string().nullable().optional(),
  administeredBy: z.number().nullable().optional(),
  wristbandBarcode: z.string().nullable().optional(),
});

export type VerificationResponse = z.infer<typeof verificationResponseSchema>;

// ---- Exception logging + override ------------------------------------------

export const EXCEPTION_TYPES = [
  "ALLERGY_CONFLICT",
  "DOSE_OUT_OF_RANGE",
  "ROUTE_MISMATCH",
  "TIME_MISMATCH",
  "PATIENT_MISMATCH",
  "DRUG_INTERACTION",
  "DUPLICATE_THERAPY",
  "OTHER",
] as const;

export type ExceptionType = (typeof EXCEPTION_TYPES)[number];

/** Create a BCMA exception. */
export const exceptionCreateSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient is required"),
  drugNdc: z.string().min(1, "Drug NDC is required").max(50, "NDC too long"),
  exceptionType: z.enum(EXCEPTION_TYPES, {
    required_error: "Exception type is required",
  }),
  reason: z.string().min(1, "Reason is required").max(500, "Max 500 chars"),
  overrideNurse: z.coerce.number().int().positive("Nurse is required"),
  wardId: z.coerce.number().int().positive("Ward is required"),
  scannedBy: z.coerce.number().int().positive("Scanned-by is required"),
});

export type ExceptionCreateValues = z.infer<typeof exceptionCreateSchema>;

/** Apply an override to an exception. */
export const exceptionOverrideSchema = z.object({
  justification: z
    .string()
    .min(1, "Justification is required")
    .max(500, "Max 500 chars"),
  overrideNurse: z.coerce.number().int().positive("Nurse is required"),
});

export type ExceptionOverrideValues = z.infer<typeof exceptionOverrideSchema>;

/** Exception response. */
export const exceptionResponseSchema = z.object({
  exceptionId: z.number(),
  patientId: z.number(),
  drugNdc: z.string(),
  exceptionType: z.string(),
  reason: z.string(),
  overrideNurse: z.number().nullable().optional(),
  wardId: z.number().nullable().optional(),
  scannedBy: z.number(),
  createdAt: z.string(),
  overridden: z.boolean(),
  overrideJustification: z.string().nullable().optional(),
  overrideAt: z.string().nullable().optional(),
});

export type ExceptionResponse = z.infer<typeof exceptionResponseSchema>;

// ---- Ward stock view -------------------------------------------------------

/** Stock levels per ward. */
export const stockResponseSchema = z.object({
  stockId: z.number(),
  drugNdc: z.string(),
  batchId: z.string(),
  quantityOnHand: z.number(),
  expiryDate: z.string(),
  storageLocation: z.string(),
});

export type StockResponse = z.infer<typeof stockResponseSchema>;

export const stockAlertResponseSchema = z.object({
  alertId: z.number(),
  drugNdc: z.string(),
  alertType: z.string(),
  message: z.string(),
  severity: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]),
  resolved: z.boolean(),
  createdAt: z.string(),
});

export type StockAlertResponse = z.infer<typeof stockAlertResponseSchema>;

export const stockUpdateSchema = z.object({
  stockId: z.coerce.number().int().positive("Stock ID is required"),
  drugNdc: z.string().min(1, "Drug NDC is required"),
  batchId: z.string().min(1, "Batch ID is required"),
  quantityOnHand: z.coerce.number().nonnegative("Quantity must be >= 0"),
  expiryDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd"),
  storageLocation: z.string().min(1, "Storage location is required"),
});

export type StockUpdateValues = z.infer<typeof stockUpdateSchema>;

export const stockAlertCreateSchema = z.object({
  drugNdc: z.string().min(1, "Drug NDC is required"),
  alertType: z.string().min(1, "Alert type is required"),
  message: z.string().min(1, "Message is required"),
  severity: z.enum(["LOW", "MEDIUM", "HIGH", "CRITICAL"]).default("MEDIUM"),
});

export type StockAlertCreateValues = z.infer<typeof stockAlertCreateSchema>;

// ---- Verification step status (used by ScanWorkflow UI) -------------------

export const VERIFICATION_STEPS = [
  "PATIENT",
  "DRUG",
  "DOSE",
  "ROUTE",
  "TIME",
] as const;

export type VerificationStep = (typeof VERIFICATION_STEPS)[number];
