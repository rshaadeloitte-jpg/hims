import { z } from "zod";

/**
 * M01 Patient Registration (Epic MyChart + Stripe-style validation).
 * Extended schema with ABHA/Aadhaar, MLC flag, mother-child linking,
 * COVID tracking fields, and photo capture metadata.
 */

const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().int().positive().optional(),
);

const optionalString = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.string().optional(),
);

const optionalDate = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd").optional(),
);

/** ABHA address: alphanumeric, 4-32 chars, optional domain suffix */
const abhaAddressSchema = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z
    .string()
    .regex(/^[a-zA-Z0-9]{4,32}(@abdm)?$/, "Invalid ABHA address (4-32 alphanumeric, optional @abdm)")
    .optional(),
);

/** Aadhaar: 12 digits, optionally space-separated */
const aadhaarSchema = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z
    .string()
    .regex(/^\d{4}\s?\d{4}\s?\d{4}$/, "Invalid Aadhaar (12 digits)")
    .optional(),
);

/** MLC (Medico-Legal Case) section */
export const mlcSchema = z.object({
  isMlc: z.boolean().default(false),
  mlcNumber: optionalString,
  mlcFirNumber: optionalString,
  mlcPoliceStation: optionalString,
  mlcNotes: optionalString,
}).default({ isMlc: false, mlcNumber: "", mlcFirNumber: "", mlcPoliceStation: "", mlcNotes: "" });

export type MlcValues = z.infer<typeof mlcSchema>;

/** Mother-child linking for pediatric patients */
export const motherChildLinkSchema = z.object({
  isChildPatient: z.boolean().default(false),
  motherPatientId: optionalNumber,
  motherMrn: optionalString,
  birthWeightGrams: optionalNumber,
  gestationalAgeWeeks: optionalNumber,
}).default({ isChildPatient: false, motherPatientId: undefined, motherMrn: "", birthWeightGrams: undefined, gestationalAgeWeeks: undefined });

export type MotherChildLinkValues = z.infer<typeof motherChildLinkSchema>;

/** COVID tracking fields */
export const covidTrackingSchema = z.object({
  covidVaccinationStatus: z.enum(["UNVACCINATED", "PARTIAL", "FULL", "BOOSTER"]).optional(),
  lastCovidVaccineDate: optionalDate,
  covidInfectionHistory: z.boolean().default(false),
  lastCovidInfectionDate: optionalDate,
}).default({ covidVaccinationStatus: undefined, lastCovidVaccineDate: "", covidInfectionHistory: false, lastCovidInfectionDate: "" });

export type CovidTrackingValues = z.infer<typeof covidTrackingSchema>;

/** Photo capture metadata */
export const photoCaptureSchema = z.object({
  photoFileId: optionalString,
  photoCapturedAt: optionalString,
  photoDataUrl: z.string().optional(),
}).default({ photoFileId: "", photoCapturedAt: "", photoDataUrl: "" });

export type PhotoCaptureValues = z.infer<typeof photoCaptureSchema>;

/** Main patient registration schema — M01 */
export const patientRegistrationSchema = z.object({
  // Identity
  firstName: z.string().min(1, "First name is required").max(50, "Max 50 characters"),
  middleName: optionalString,
  lastName: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.string().max(50, "Max 50 characters").optional(),
  ),
  fatherName: optionalString,
  motherName: optionalString,

  // Contact
  mobileNo: z
    .string()
    .regex(/^\d{10}$/, "Invalid 10-digit mobile number"),
  emergencyContact: optionalString,
  email: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.string().email("Invalid email").optional(),
  ),

  // Demographics
  genderId: optionalNumber,
  dateOfBirth: optionalDate,
  bloodGroup: optionalString,
  maritalStatus: optionalString,

  // Address
  address: optionalString,
  city: optionalString,
  state: optionalString,
  pincode: z.preprocess(
    (v) => (v === "" || v == null ? undefined : v),
    z.string().regex(/^\d{6}$/, "Invalid 6-digit pincode").optional(),
  ),

  // IDs
  abhaAddress: abhaAddressSchema,
  aadhaarNumber: aadhaarSchema,

  // Unit/Org
  unitId: optionalNumber,

  // Nested sections
  mlc: mlcSchema.optional().default({} as MlcValues),
  motherChildLink: motherChildLinkSchema.optional().default({} as MotherChildLinkValues),
  covidTracking: covidTrackingSchema.optional().default({} as CovidTrackingValues),
  photo: photoCaptureSchema.optional().default({} as PhotoCaptureValues),
});

export type PatientRegistrationValues = z.infer<typeof patientRegistrationSchema>;

/** Search query schema */
export const patientSearchSchema = z.object({
  query: z.string().min(2, "Enter at least 2 characters"),
  searchType: z.enum(["ALL", "NAME", "MRN", "MOBILE", "ABHA", "AADHAAR"]).default("ALL"),
});

export type PatientSearchValues = z.infer<typeof patientSearchSchema>;

/** Document uploader metadata (file is sent separately as multipart). */
export const documentUploadSchema = z.object({
  documentType: z.enum([
    "AADHAAR",
    "PAN",
    "INSURANCE",
    "ID_PROOF",
    "PHOTO",
    "DISCHARGE_SUMMARY",
    "PRESCRIPTION",
    "OTHER",
  ]),
  description: optionalString,
  file: z
    .instanceof(FileList)
    .refine((files) => files.length === 1, "Pick one file")
    .refine(
      (files) => files[0]?.size <= 10 * 1024 * 1024,
      "Max file size 10 MB",
    ),
});

export type DocumentUploadValues = z.infer<typeof documentUploadSchema>;
