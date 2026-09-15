import { z } from "zod";

/**
 * M39 Commons — Zod schemas for AppConfig, AuditTrail, and HospitalSetup.
 *
 * Validation mirrors the reference-service CommonsController contract:
 *   - AppConfig: key (required), value (text), description (optional)
 *   - Audit: entityType + action required, diff is a JSON object
 *   - Hospital: unit name/code required, longitude/latitude as decimal strings
 */

const optionalTrimmed = z.preprocess(
  (v) => (v === "" ? undefined : v),
  z.string().optional(),
);

/** PUT /api/v1/commons/config — app/unit key-value pair. */
export const appConfigSchema = z.object({
  key: z.string().min(1, "Config key is required").max(120),
  value: z.string().optional(),
  description: optionalTrimmed,
});

export type AppConfigValues = z.infer<typeof appConfigSchema>;

/** POST /api/v1/commons/audit — append-only audit entry. */
export const auditSchema = z.object({
  entityType: z.string().min(1, "Entity type is required").max(64),
  entityId: z.coerce.number().int().positive("Entity ID must be a positive number"),
  action: z.string().min(1, "Action is required").max(16),
  actorId: z.coerce.number().int().positive().optional(),
  actorUsername: optionalTrimmed,
  diff: z.record(z.unknown()).optional(),
});

export type AuditValues = z.infer<typeof auditSchema>;

/** Unit geo-tagging — HospitalSetup creation. */
export const hospitalSetupSchema = z.object({
  code: z.string().min(1, "Unit code is required").max(20),
  name: z.string().min(1, "Unit name is required").max(255),
  description: optionalTrimmed,
  addressline1: optionalTrimmed,
  city: optionalTrimmed,
  state: optionalTrimmed,
  country: optionalTrimmed,
  pincode: z.preprocess(
    (v) => (v === "" ? undefined : v),
    z.string().regex(/^\d{6}$/, "PIN code must be 6 digits").optional(),
  ),
  contactno: optionalTrimmed,
  email: z.preprocess(
    (v) => (v === "" ? undefined : v),
    z.string().email("Invalid email").optional(),
  ),
  longitude: z.preprocess(
    (v) => (v === "" ? undefined : v),
    z.string().regex(/^-?\d{1,3}(\.\d+)?$/, "Invalid longitude").optional(),
  ),
  latitude: z.preprocess(
    (v) => (v === "" ? undefined : v),
    z.string().regex(/^-?\d{1,2}(\.\d+)?$/, "Invalid latitude").optional(),
  ),
});

export type HospitalSetupValues = z.infer<typeof hospitalSetupSchema>;

/** Validate a JSONB diff value for the audit log preview. */
export function parseDiff(raw: string | null | undefined): Record<string, unknown> | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    return typeof parsed === "object" && parsed !== null ? parsed : null;
  } catch {
    return null;
  }
}

/** Validate action string against Linear-style audit action enum. */
export const AUDIT_ACTIONS = ["CREATE", "UPDATE", "DELETE", "VIEW", "EXPORT", "LOGIN", "LOGOUT"] as const;
export type AuditActionType = (typeof AUDIT_ACTIONS)[number];

export const auditActionSchema = z.enum(AUDIT_ACTIONS);
