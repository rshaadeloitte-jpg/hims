import { z } from "zod";

/**
 * M36 EJS/ASHA Zod schemas — mirror the EJS service request DTOs.
 * Numeric HTML inputs come through as strings; `z.coerce.number()` normalises them.
 */

const optionalNumber = z
  .preprocess((v) => (v === "" || v == null ? undefined : v), z.coerce.number().optional());

const optionalString = z
  .preprocess((v) => (v === "" ? undefined : v), z.string().optional());

// ---- AshaWork schemas (ANC/PNC/Child-care tracking) ----

/** Tracking type for ASHA work records. */
export const TRACKING_TYPE = {
  ANC: "ANC",
  PNC: "PNC",
  CHILD: "CHILD",
} as const;

export type TrackingType = (typeof TRACKING_TYPE)[keyof typeof TRACKING_TYPE];

/** Status of an ASHA visit record. */
export const VISIT_STATUS = {
  SCHEDULED: "SCHEDULED",
  COMPLETED: "COMPLETED",
  MISSED: "MISSED",
  RESCHEDULED: "RESCHEDULED",
} as const;

export type VisitStatus = (typeof VISIT_STATUS)[keyof typeof VISIT_STATUS];

/** Create/update an ASHA work record (ANC/PNC/child-care tracking). */
export const ashaWorkSchema = z.object({
  beneficiaryId: z.coerce.number().int().positive("Beneficiary is required"),
  beneficiaryName: z.string().min(1, "Beneficiary name is required"),
  trackingType: z.enum(["ANC", "PNC", "CHILD"]),
  visitDate: z.string().min(1, "Visit date is required"),
  ashaId: z.coerce.number().int().positive("ASHA worker is required"),
  ashaName: z.string().min(1, "ASHA name is required"),
  status: z.enum(["SCHEDULED", "COMPLETED", "MISSED", "RESCHEDULED"]).default("SCHEDULED"),
  // ANC-specific fields
  gestationalWeek: optionalNumber,
  weight: optionalNumber,
  bloodPressureSystolic: optionalNumber,
  bloodPressureDiastolic: optionalNumber,
  hemoglobin: optionalNumber,
  immunizationGiven: optionalString,
  // PNC-specific fields
  deliveryDate: optionalString,
  deliveryType: z.enum(["NORMAL", "CESAREAN", "ASSISTED"]).optional(),
  postnatalDay: optionalNumber,
  // Child-care fields
  childAgeMonths: optionalNumber,
  weightChild: optionalNumber,
  heightChild: optionalNumber,
  immunizationStatus: z.enum(["UPTO_DATE", "DUE", "OVERDUE"]).optional(),
  nutritionStatus: z.enum(["NORMAL", "MALNUTRITION", "SEVERE_MALNUTRITION"]).optional(),
  // Common
  remarks: optionalString,
  nextVisitDate: optionalString,
});

export type AshaWorkValues = z.infer<typeof ashaWorkSchema>;

/** Filter for ASHA work list. */
export const ashaWorkFilterSchema = z.object({
  trackingType: z.enum(["", "ANC", "PNC", "CHILD"]).default(""),
  status: z.enum(["", "SCHEDULED", "COMPLETED", "MISSED", "RESCHEDULED"]).default(""),
  ashaId: optionalNumber,
  fromDate: optionalString,
  toDate: optionalString,
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type AshaWorkFilterValues = z.infer<typeof ashaWorkFilterSchema>;

// ---- Incentive schemas ----

/** Incentive calculation record for an ASHA worker. */
export const incentiveSchema = z.object({
  ashaId: z.coerce.number().int().positive("ASHA worker is required"),
  ashaName: z.string().min(1, "ASHA name is required"),
  month: z.string().min(1, "Month is required (YYYY-MM)"),
  // Activity counts
  ancVisits: z.coerce.number().int().nonnegative().default(0),
  pncVisits: z.coerce.number().int().nonnegative().default(0),
  childCareVisits: z.coerce.number().int().nonnegative().default(0),
  immunizationReferrals: z.coerce.number().int().nonnegative().default(0),
  institutionalDeliveryReferrals: z.coerce.number().int().nonnegative().default(0),
  // Incentive amounts
  ancIncentive: z.coerce.number().nonnegative().default(0),
  pncIncentive: z.coerce.number().nonnegative().default(0),
  childCareIncentive: z.coerce.number().nonnegative().default(0),
  immunizationIncentive: z.coerce.number().nonnegative().default(0),
  deliveryIncentive: z.coerce.number().nonnegative().default(0),
  totalIncentive: z.coerce.number().nonnegative().default(0),
  // Status
  status: z.enum(["PENDING", "APPROVED", "PAID", "REJECTED"]).default("PENDING"),
  approvedBy: optionalString,
  approvedAt: optionalString,
  paidAt: optionalString,
  remarks: optionalString,
});

export type IncentiveValues = z.infer<typeof incentiveSchema>;

/** Incentive filter. */
export const incentiveFilterSchema = z.object({
  ashaId: optionalNumber,
  month: optionalString,
  status: z.enum(["", "PENDING", "APPROVED", "PAID", "REJECTED"]).default(""),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type IncentiveFilterValues = z.infer<typeof incentiveFilterSchema>;

// ---- Geography/Population Register schemas ----

/** Geography master — population register for ASHA catchment area. */
export const geographyMasterSchema = z.object({
  ashaId: z.coerce.number().int().positive("ASHA worker is required"),
  ashaName: z.string().min(1, "ASHA name is required"),
  // Geography
  state: z.string().min(1, "State is required"),
  district: z.string().min(1, "District is required"),
  block: z.string().min(1, "Block is required"),
  village: z.string().min(1, "Village is required"),
  subCenter: z.string().min(1, "Sub-center is required"),
  // Population
  totalPopulation: z.coerce.number().int().nonnegative().default(0),
  targetCouples: z.coerce.number().int().nonnegative().default(0),
  pregnantWomen: z.coerce.number().int().nonnegative().default(0),
  childrenUnder5: z.coerce.number().int().nonnegative().default(0),
  // Enumeration
  households: z.coerce.number().int().nonnegative().default(0),
  eligibleCouples: z.coerce.number().int().nonnegative().default(0),
  // Status
  isActive: z.boolean().default(true),
  lastEnumerationDate: optionalString,
  remarks: optionalString,
});

export type GeographyMasterValues = z.infer<typeof geographyMasterSchema>;

/** Geography filter. */
export const geographyFilterSchema = z.object({
  ashaId: optionalNumber,
  state: optionalString,
  district: optionalString,
  block: optionalString,
  village: optionalString,
  isActive: z.enum(["", "true", "false"]).optional(),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type GeographyFilterValues = z.infer<typeof geographyFilterSchema>;
