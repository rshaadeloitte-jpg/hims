import { z } from "zod";

/**
 * M24 PMRD (Public Medical Record Department) Zod schemas.
 *
 * Ported from legacy PmrdServiceImpl + formj1/2/3, hform templates.
 * JSY (Janani Suraksha Yojana) statutory forms: J1, J2, J3, H-form.
 *
 * Stripe-style validation: real-time feedback, clean error messaging,
 * progressive disclosure, accessible form controls.
 */

const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().int().positive().optional(),
);

const optionalString = z.preprocess(
  (v) => (v === "" ? undefined : v),
  z.string().optional(),
);

// ---- JSY Form types --------------------------------------------------------

/** JSY form types (ported from legacy formj1/2/3, hform templates). */
export const JSY_FORM_TYPES = ["J1", "J2", "J3", "H_FORM"] as const;
export type JsyFormType = (typeof JSY_FORM_TYPES)[number];

/** Beneficiary categories for JSY scheme. */
export const BENEFICIARY_CATEGORIES = ["BPL", "APL", "SC", "ST", "OBC", "GENERAL"] as const;
export type BeneficiaryCategory = (typeof BENEFICIARY_CATEGORIES)[number];

/** Delivery types. */
export const DELIVERY_TYPES = ["NORMAL", "CESAREAN", "HOME"] as const;
export type DeliveryType = (typeof DELIVERY_TYPES)[number];

/** JSY J1 form — ANM (Auxiliary Nurse Midwife) reporting. */
export const jsyJ1Schema = z.object({
  formType: z.literal("J1"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  beneficiaryName: z.string().min(1, "Beneficiary name is required").max(200),
  husbandName: z.string().min(1, "Husband name is required").max(200),
  address: z.string().min(1, "Address is required").max(500),
  category: z.enum(BENEFICIARY_CATEGORIES),
  bplCardNo: optionalString,
  mobileNo: z
    .string()
    .regex(/^[6-9]\d{9}$/, "Invalid mobile number")
    .optional()
    .or(z.literal("")),
  ancRegistrationDate: z.string().min(1, "ANC registration date is required"),
  ancVisits: z.coerce.number().int().min(0).max(12),
  deliveryDate: z.string().min(1, "Delivery date is required"),
  deliveryType: z.enum(DELIVERY_TYPES),
  placeOfDelivery: z.string().min(1, "Place of delivery is required").max(200),
  childGender: z.enum(["MALE", "FEMALE", "OTHER"]),
  childWeight: z.coerce.number().min(0).max(10),
  anmName: z.string().min(1, "ANM name is required").max(200),
  anmMobile: z.string().regex(/^[6-9]\d{9}$/, "Invalid mobile number"),
  submittedDate: z.string().min(1, "Submitted date is required"),
});

export type JsyJ1Values = z.infer<typeof jsyJ1Schema>;

/** JSY J2 form — Medical Officer / facility reporting. */
export const jsyJ2Schema = z.object({
  formType: z.literal("J2"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  beneficiaryName: z.string().min(1, "Beneficiary name is required").max(200),
  j1FormId: optionalNumber,
  facilityName: z.string().min(1, "Facility name is required").max(200),
  facilityCode: z.string().min(1, "Facility code is required").max(50),
  moName: z.string().min(1, "Medical Officer name is required").max(200),
  moRegistrationNo: z.string().min(1, "Registration number is required").max(50),
  deliveryDate: z.string().min(1, "Delivery date is required"),
  deliveryType: z.enum(DELIVERY_TYPES),
  complications: optionalString,
  referralMade: z.boolean().default(false),
  referralFacility: optionalString,
  dischargeDate: z.string().min(1, "Discharge date is required"),
  amountClaimed: z.coerce.number().nonnegative("Amount must be >= 0"),
  submittedDate: z.string().min(1, "Submitted date is required"),
});

export type JsyJ2Values = z.infer<typeof jsyJ2Schema>;

/** JSY J3 form — District / block consolidation. */
export const jsyJ3Schema = z.object({
  formType: z.literal("J3"),
  districtName: z.string().min(1, "District name is required").max(200),
  blockName: z.string().min(1, "Block name is required").max(200),
  month: z.string().min(1, "Month is required"),
  year: z.coerce.number().int().min(2000).max(2100),
  totalJ1Received: z.coerce.number().int().nonnegative(),
  totalJ2Received: z.coerce.number().int().nonnegative(),
  totalAmountDisbursed: z.coerce.number().nonnegative(),
  submittedBy: z.string().min(1, "Submitted by is required").max(200),
  submittedDate: z.string().min(1, "Submitted date is required"),
});

export type JsyJ3Values = z.infer<typeof jsyJ3Schema>;

/** H-form — High-risk pregnancy tracking. */
export const hFormSchema = z.object({
  formType: z.literal("H_FORM"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  beneficiaryName: z.string().min(1, "Beneficiary name is required").max(200),
  highRiskReason: z.string().min(1, "High-risk reason is required").max(500),
  lmp: z.string().min(1, "LMP is required"),
  edd: z.string().min(1, "EDD is required"),
  riskFactors: z.array(z.string()).default([]),
  actionTaken: z.string().min(1, "Action taken is required").max(500),
  anmName: z.string().min(1, "ANM name is required").max(200),
  submittedDate: z.string().min(1, "Submitted date is required"),
});

export type HFormValues = z.infer<typeof hFormSchema>;

/** Discriminated union for all JSY form types. */
export const jsyFormSchema = z.discriminatedUnion("formType", [
  jsyJ1Schema,
  jsyJ2Schema,
  jsyJ3Schema,
  hFormSchema,
]);

export type JsyFormValues = z.infer<typeof jsyFormSchema>;

/** JSY form list filter. */
export const jsyFormFilterSchema = z.object({
  formType: z.enum(JSY_FORM_TYPES).optional(),
  patientId: optionalNumber,
  fromDate: optionalString,
  toDate: optionalString,
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type JsyFormFilterValues = z.infer<typeof jsyFormFilterSchema>;

// ---- Encounter record (IEN) ------------------------------------------------

/** Encounter record filter. */
export const encounterFilterSchema = z.object({
  patientId: optionalNumber,
  encounterType: optionalString,
  fromDate: optionalString,
  toDate: optionalString,
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type EncounterFilterValues = z.infer<typeof encounterFilterSchema>;

// ---- Document upload (PMRD-specific categories) ---------------------------

/** PMRD document categories (ported from uploadPatientDocumentsPmrd). */
export const PMRD_DOC_CATEGORIES = [
  "JSY_FORM",
  "ANC_RECORD",
  "DELIVERY_RECORD",
  "DISCHARGE_SUMMARY",
  "ID_PROOF",
  "BPL_CARD",
  "CONSENT_FORM",
  "REFERRAL_SLIP",
  "OTHER",
] as const;

export type PmrdDocCategory = (typeof PMRD_DOC_CATEGORIES)[number];

/** PMRD document upload (base64 JSON — Stripe-style validation). */
export const pmrdDocumentUploadSchema = z.object({
  fileName: z
    .string()
    .min(1, "File name is required")
    .max(255, "File name too long")
    .regex(/^[^\\/:*?"<>|]+$/, "File name contains invalid characters"),
  fileType: z
    .string()
    .min(1, "File type is required")
    .regex(/^[a-z]+\/[a-z0-9.+-]+$/i, "Invalid MIME type"),
  fileData: z
    .string()
    .min(1, "File data is required")
    .refine((d) => d.length <= 10 * 1024 * 1024 * 1.37, "File exceeds 10 MB limit")
    .refine(
      (d) => /^data:[a-z]+\/[a-z0-9.+-]+;base64,/.test(d) || /^[A-Za-z0-9+/=]+$/.test(d),
      "Invalid base64 data",
    ),
  documentCategory: z.enum(PMRD_DOC_CATEGORIES),
  patientId: z.coerce.number().int().positive("Patient is required"),
  encounterId: optionalNumber,
  description: optionalString,
});

export type PmrdDocumentUploadValues = z.infer<typeof pmrdDocumentUploadSchema>;

/** PMRD document list filter. */
export const pmrdDocumentFilterSchema = z.object({
  patientId: optionalNumber,
  encounterId: optionalNumber,
  documentCategory: z.enum(PMRD_DOC_CATEGORIES).optional(),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type PmrdDocumentFilterValues = z.infer<typeof pmrdDocumentFilterSchema>;
