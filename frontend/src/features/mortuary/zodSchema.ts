import { z } from "zod";

/**
 * M28 Mortuary & Death — Zod schemas.
 *
 * Ported from the legacy `mortuarySchema.json`, `mortuaryBoxManagement.json`,
 * `mortuaryClearanceSchema.json`, and `death_form.vm` / `death_Certificate.vm`
 * templates. Mirrors the backend `Mortuary` DTO fields carried on
 * `PatientServiceImpl#getMortuaryPatients` and `updatePatientDeathDetails`.
 */

// ---- Chamber / box allocation ---------------------------------------------

/**
 * Allocate a body (deceased patient) to a mortuary chamber / box.
 * Mirrors `mortuaryBoxManagement.json` + `saveMortuaryAllocation`.
 */
export const chamberAllocationSchema = z.object({
  /** Mortuary box / chamber id. */
  chamberId: z.coerce.number().positive("Select a chamber"),
  /** Deceased patient id (from the ADT death-handling discharge flow). */
  patientId: z.coerce.number().positive("Patient is required"),
  /** Free-text body / patient name (mirrors `bodyName` in the legacy DTO). */
  bodyName: z.string().min(1, "Body name is required").max(120),
  /** Optional cause-of-death note captured at intake. */
  causeOfDeath: z.string().max(500).optional(),
  /** Date-time the body was placed in the chamber (ISO-8601). */
  admittedAt: z.string().min(1, "Admission date-time is required"),
});

export type ChamberAllocationValues = z.infer<typeof chamberAllocationSchema>;

/**
 * Release (discharge) a body from the mortuary — family/legal handover.
 * Mirrors `mortuaryClearanceSchema.json` + the clearance fields.
 */
export const mortuaryClearanceSchema = z.object({
  chamberId: z.coerce.number().positive("Chamber is required"),
  /** Person to whom the body is released. */
  releasedTo: z.string().min(1, "Released-to name is required").max(120),
  /** Relationship of the recipient to the deceased. */
  relationship: z
    .enum(["SPOUSE", "PARENT", "CHILD", "SIBLING", "LEGAL", "OTHER"])
    .default("OTHER"),
  /** Authorized recipient identity (Aadhaar / govt id number). */
  recipientId: z.string().max(30).optional(),
  /** Date-time of release. */
  releasedAt: z.string().min(1, "Release date-time is required"),
  /** Optional remark. */
  remark: z.string().max(500).optional(),
});

export type MortuaryClearanceValues = z.infer<typeof mortuaryClearanceSchema>;

// ---- Death details update -------------------------------------------------

/**
 * Update death-related fields on a patient record. Mirrors
 * `updatePatientDeathDetails(patientId, isDeath)` plus the extended
 * death-handling fields carried on `T_Registration_Details`.
 */
export const deathDetailsSchema = z.object({
  patientId: z.coerce.number().positive("Patient is required"),
  /** Death flag — always true when submitting via mortuary flow. */
  isDeath: z.literal(true),
  /** Date and time of death (ISO-8601). */
  deathDate: z.string().min(1, "Date of death is required"),
  /** Coded cause of death (free-text or ICD stub). */
  causeOfDeath: z.string().min(1, "Cause of death is required").max(500),
  /** Type of death — natural / accident / suicide / homicide / pending. */
  deathType: z
    .enum(["NATURAL", "ACCIDENT", "SUICIDE", "HOMICIDE", "PENDING", "OTHER"])
    .default("NATURAL"),
  /** Doctor / RMO who certified the death. */
  certifiedBy: z.string().min(1, "Certifying doctor is required").max(120),
  /** MLC (Medico-Legal Case) flag — triggers coroner flow. */
  isMlc: z.boolean().default(false),
  /** MLC number (required when `isMlc=true`). */
  mlcNumber: z.string().max(30).optional(),
}).refine((v) => !v.isMlc || (v.mlcNumber && v.mlcNumber.length > 0), {
  message: "MLC number is required for medico-legal cases",
  path: ["mlcNumber"],
});

export type DeathDetailsValues = z.infer<typeof deathDetailsSchema>;

// ---- Death certificate print request --------------------------------------

/**
 * Request payload for generating a print-ready death certificate.
 * Mirrors `death_Certificate.vm` template fields.
 */
export const deathCertificateRequestSchema = z.object({
  patientId: z.coerce.number().positive("Patient is required"),
  /** Certificate serial number (assigned by records). */
  certNumber: z.string().min(1, "Certificate number is required").max(30),
  /** Override line for place-of-death (defaults to facility name). */
  placeOfDeath: z.string().max(200).optional(),
});

export type DeathCertificateRequest = z.infer<typeof deathCertificateRequestSchema>;

// ---- Chamber status enum (matches backend MortuaryStatus) ---------------

export const chamberStatusSchema = z.enum([
  "EMPTY",
  "OCCUPIED",
  "CLEARING",
  "RELEASED",
]);

export type ChamberStatus = z.infer<typeof chamberStatusSchema>;

export const CHAMBER_STATUS_TONE: Record<ChamberStatus, string> = {
  EMPTY: "badge--success",
  OCCUPIED: "badge--info",
  CLEARING: "badge--warning",
  RELEASED: "badge--muted",
};

export const CHAMBER_STATUS_LABEL: Record<ChamberStatus, string> = {
  EMPTY: "Empty",
  OCCUPIED: "Occupied",
  CLEARING: "Clearing",
  RELEASED: "Released",
};
