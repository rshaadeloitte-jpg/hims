import { z } from "zod";

/**
 * M34 File/Document Management Zod schemas — mirror file-service request DTOs.
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

/** File categories (ported from M_FILESTORE FileManagementServiceImpl). */
export const FILE_CATEGORIES = [
  "RADIOLOGY",
  "LAB_REPORTS",
  "PRESCRIPTION",
  "DISCHARGE_SUMMARY",
  "CONSENT_FORM",
  "ID_PROOF",
  "INSURANCE",
  "OTHER",
] as const;

export type FileCategory = (typeof FILE_CATEGORIES)[number];

/** Owner types for file scoping. */
export const OWNER_TYPES = ["PATIENT", "ENCOUNTER", "ADMISSION", "EQUIPMENT"] as const;
export type OwnerType = (typeof OWNER_TYPES)[number];

/** Upload file (base64 JSON, 10 MB limit — Stripe-style validation). */
export const fileUploadSchema = z.object({
  fileName: z
    .string()
    .min(1, "File name is required")
    .max(255, "File name too long")
    .regex(/^[^\/:*?"<>|]+$/, "File name contains invalid characters"),
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
  fileCategoryId: optionalNumber,
  ownerType: z.enum(OWNER_TYPES).optional(),
  ownerId: optionalNumber,
});

export type FileUploadValues = z.infer<typeof fileUploadSchema>;

/** File list filter. */
export const fileFilterSchema = z.object({
  ownerType: z.enum(OWNER_TYPES).optional(),
  ownerId: optionalNumber,
  fileCategoryId: optionalNumber,
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type FileFilterValues = z.infer<typeof fileFilterSchema>;

/** File category create. */
export const fileCategorySchema = z.object({
  code: z
    .string()
    .min(1, "Code is required")
    .max(50, "Max 50 characters")
    .regex(/^[A-Z][A-Z0-9_]*$/, "Code must be UPPER_SNAKE_CASE"),
  name: z.string().min(1, "Name is required").max(100, "Max 100 characters"),
  documentType: optionalString,
  parentId: optionalNumber,
});

export type FileCategoryValues = z.infer<typeof fileCategorySchema>;

/** Annotation payload (ImageAnnotationMaster parity). */
export const annotationSchema = z.object({
  annotationJson: z
    .string()
    .min(1, "Annotation data is required")
    .refine(
      (s) => {
        try { JSON.parse(s); return true; } catch { return false; }
      },
      "Annotation must be valid JSON",
    ),
});

export type AnnotationValues = z.infer<typeof annotationSchema>;

/** Rename category. */
export const renameCategorySchema = z.object({
  name: z.string().min(1, "Name is required").max(100, "Max 100 characters"),
});

export type RenameCategoryValues = z.infer<typeof renameCategorySchema>;