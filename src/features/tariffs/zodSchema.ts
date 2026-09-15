import { z } from "zod";

/**
 * M05 Tariff Masters Zod schemas — mirror the reference-service request DTOs.
 * Numeric HTML inputs come through as strings; `z.coerce.number()` normalises them.
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

// ---- Service category enum -------------------------------------------------

export const SERVICE_CATEGORIES = [
  "CONSULTATION",
  "DIAGNOSTIC",
  "PROCEDURE",
  "ROOM_CHARGE",
  "PHARMACY",
  "LABORATORY",
  "RADIOLOGY",
  "PACKAGE",
  "MISCELLANEOUS",
] as const;

export const ServiceCategoryEnum = z.enum(SERVICE_CATEGORIES);

// ---- Tariff schemas --------------------------------------------------------

export const tariffSchema = z.object({
  serviceId: z.coerce.number().int().positive("Service ID is required"),
  serviceName: z.string().min(1, "Service name is required").max(200),
  serviceCategory: ServiceCategoryEnum,
  baseRate: z.coerce.number().nonnegative("Base rate must be >= 0"),
  copayPercent: z.coerce.number().min(0).max(100).default(0),
  packageClass: z.string().max(50).optional(),
  status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
});

export type TariffValues = z.infer<typeof tariffSchema>;

export const tariffFilterSchema = z.object({
  serviceCategory: z.enum(["", ...SERVICE_CATEGORIES]).optional(),
  search: optionalString,
  status: z.enum(["", "ACTIVE", "INACTIVE"]).optional(),
  page: z.coerce.number().int().nonnegative().default(0),
  size: z.coerce.number().int().positive().default(20),
});

export type TariffFilterValues = z.infer<typeof tariffFilterSchema>;

// ---- Package schemas -------------------------------------------------------

export const packageServiceLineSchema = z.object({
  serviceId: z.coerce.number().int().positive(),
  serviceName: z.string().min(1),
  qty: z.coerce.number().int().positive(),
});

export const packagePlanSelectionSchema = z.object({
  planId: z.coerce.number().int().positive("Plan is required"),
  planName: z.string().min(1),
  coveragePercent: z.coerce.number().min(0).max(100).default(100),
  maxCoverageAmount: z.coerce.number().nonnegative().optional(),
});

export const packageSchema = z.object({
  packageName: z.string().min(1, "Package name is required").max(200),
  packageCode: z.string().min(1, "Package code is required").max(64),
  description: optionalString,
  totalValue: z.coerce.number().nonnegative("Total value must be >= 0"),
  sellPrice: z.coerce.number().positive("Sell price must be > 0"),
  services: z.array(packageServiceLineSchema).min(1, "At least one service is required"),
  plans: z.array(packagePlanSelectionSchema).default([]),
  status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
});

export type PackageValues = z.infer<typeof packageSchema>;
export type PackageServiceLineValues = z.infer<typeof packageServiceLineSchema>;
export type PackagePlanSelectionValues = z.infer<typeof packagePlanSelectionSchema>;

// ---- Concession template schemas -------------------------------------------

export const concessionTemplateSchema = z.object({
  code: z.string().min(1, "Code is required").max(64),
  name: z.string().min(1, "Name is required").max(200),
  scheme: z.string().min(1, "Scheme is required").max(64),
  description: z.string().max(500).optional(),
  maxPercentOff: z.coerce
    .number()
    .min(0, "Must be >= 0")
    .max(100, "Must be <= 100"),
  requiresApproval: z.boolean().default(false),
});

export type ConcessionTemplateValues = z.infer<typeof concessionTemplateSchema>;

export const concessionTemplateFilterSchema = z.object({
  scheme: optionalString,
  status: z.enum(["", "1", "2"]).optional(),
});

export type ConcessionTemplateFilterValues = z.infer<typeof concessionTemplateFilterSchema>;

// ---- Health plan schemas ---------------------------------------------------

export const healthPlanSchema = z.object({
  planName: z.string().min(1, "Plan name is required").max(200),
  planCode: z.string().min(1, "Plan code is required").max(64),
  companyId: z.coerce.number().int().positive("Company is required"),
  coveragePercent: z.coerce.number().min(0).max(100).default(100),
  maxCoverageAmount: z.coerce.number().nonnegative().optional(),
  exclusions: z.array(z.string()).default([]),
  status: z.enum(["ACTIVE", "INACTIVE"]).default("ACTIVE"),
});

export type HealthPlanValues = z.infer<typeof healthPlanSchema>;
