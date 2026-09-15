import { z } from "zod";

/** M03 ADT schemas — mirror adt-service request parameters. */

const optionalNumber = z.preprocess((v) => (v === "" || v == null ? undefined : v), z.coerce.number().optional());
const optionalString = z.preprocess((v) => (v === "" ? undefined : v), z.string().optional());

/** Admit patient (POST /api/v1/admissions). */
export const admitSchema = z.object({
  patientId: z.coerce.number().int().positive("Patient is required"),
  encounterId: optionalNumber,
  doctorId: optionalNumber,
  admissionType: z.string().min(1, "Admission type is required"),
  unitId: optionalNumber,
  bedId: optionalNumber,
  isMlc: z.boolean().default(false),
  mlcNumber: optionalString,
  mlcRemarks: optionalString,
  attendingDoctorId: optionalNumber,
  remarks: optionalString,
});

export type AdmitValues = z.infer<typeof admitSchema>;

/** Transfer (POST /api/v1/admissions/{id}/transfer). */
export const transferSchema = z.object({
  toBedId: z.coerce.number().int().positive("Target bed is required"),
  reason: optionalString,
});

export type TransferValues = z.infer<typeof transferSchema>;

/** Discharge (POST /api/v1/admissions/{id}/discharge). */
export const dischargeSchema = z.object({
  isExpired: z.boolean().default(false),
  deathDate: optionalString,
  deathCause: optionalString,
  deathType: optionalString,
});

export type DischargeValues = z.infer<typeof dischargeSchema>;

/** Clearance (POST /api/v1/admissions/{id}/clearance/{department}). */
export const clearanceSchema = z.object({
  department: z.string().min(1, "Department is required"),
  clearedBy: optionalNumber,
  remarks: optionalString,
});

export type ClearanceValues = z.infer<typeof clearanceSchema>;

/** Bed reservation (POST /api/v1/bed-reservations). */
export const bedReservationSchema = z.object({
  bedId: z.coerce.number().int().positive("Bed is required"),
  patientId: z.coerce.number().int().positive("Patient is required"),
  fromDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "yyyy-MM-dd"),
  toDate: optionalString,
});

export type BedReservationValues = z.infer<typeof bedReservationSchema>;

/** TID assignment (POST /api/v1/admissions/{id}/tid). */
export const tidSchema = z.object({
  tid: z.string().min(1, "TID is required"),
});

export type TIDValues = z.infer<typeof tidSchema>;

/** Attendant pass (POST /api/v1/admissions/{id}/attendant-pass). */
export const attendantPassSchema = z.object({
  attendantName: z.string().min(1, "Name is required"),
  relation: optionalString,
  mobileNo: optionalString,
  validUntil: optionalString,
});

export type AttendantPassValues = z.infer<typeof attendantPassSchema>;