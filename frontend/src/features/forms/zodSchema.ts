import { z } from "zod";

const emptyToUndefined = (v: unknown): unknown => (v === "" || v == null ? undefined : v);

const optionalNumber = z.preprocess(
  (v) => (v === "" || v == null ? undefined : v),
  z.coerce.number().int().positive().optional(),
);

const optionalString = z.preprocess(
  (v) => (v === "" ? undefined : v),
  z.string().optional(),
);

export const formFieldSchema = z.object({
  key: z.string().min(1, "Field key is required").regex(/^[a-zA-Z_][a-zA-Z0-9_]*$/, "Key must be a valid identifier"),
  label: z.string().min(1, "Label is required"),
  type: z.enum(["string", "number", "integer", "boolean", "date", "datetime", "enum", "array", "object"]),
  required: z.boolean().default(false),
  description: optionalString,
  enumValues: z.array(z.string()).optional(),
  defaultValue: z.union([z.string(), z.number(), z.boolean()]).optional(),
});

export type FormFieldValues = z.infer<typeof formFieldSchema>;

export const formDefinitionSchema = z.object({
  code: z.string().min(1, "Code is required").regex(/^[A-Z0-9_]+$/, "Code must be uppercase alphanumeric/underscore"),
  name: z.string().min(1, "Name is required").max(120),
  specialty: z.string().min(1, "Specialty is required"),
  description: optionalString,
  version: z.coerce.number().int().positive("Version must be >= 1").default(1),
  schema: z.record(z.unknown()),
});

export type FormDefinitionValues = z.infer<typeof formDefinitionSchema>;

export const formSubmissionSchema = z.object({
  id: z.number(),
  formId: z.number(),
  patientId: optionalNumber,
  encounterId: optionalNumber,
  submissionJson: z.string(),
  submittedBy: optionalNumber,
  submittedAt: z.string(),
  status: z.boolean().optional(),
});

export type FormSubmission = z.infer<typeof formSubmissionSchema>;

export const submissionFilterSchema = z.object({
  patientId: optionalNumber,
  encounterId: optionalNumber,
  formId: optionalNumber,
  fromDate: optionalString,
  toDate: optionalString,
});

export type SubmissionFilterValues = z.infer<typeof submissionFilterSchema>;

export function validateSchemaJson(input: string): { ok: true; value: Record<string, unknown> } | { ok: false; error: string } {
  try {
    const parsed = JSON.parse(input);
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
      return { ok: false, error: "Schema must be a JSON object" };
    }
    return { ok: true, value: parsed as Record<string, unknown> };
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : "Invalid JSON" };
  }
}
