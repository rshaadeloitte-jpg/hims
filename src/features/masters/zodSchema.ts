import { z } from "zod";
import type { EntityMeta, FieldMeta } from "./entityMeta";

/**
 * Schema adapter: EntityMeta → Zod schema for react-hook-form's zodResolver.
 *
 * Conversion rules (per FieldMeta.type):
 *   text   -> z.string(); required fields also enforce min(1) so blank input fails
 *   number -> z.coerce-style preprocess (string -> Number); required fields reject
 *             empty/undefined/NaN, optional fields treat "" as undefined
 *   boolean-> z.boolean() (checkbox submits a real boolean)
 *   date   -> z.string() ('YYYY-MM-DD' from <input type="date">)
 *   select -> z.string() against option values; required rejects empty
 * readOnly fields are excluded — they never enter validation or the save payload.
 */

const emptyToUndefined = (v: unknown): unknown => (v === "" || v == null ? undefined : v);

const toNumber = (v: unknown): number | undefined => {
  if (v === "" || v == null) return undefined;
  const n = typeof v === "number" ? v : Number(v);
  return Number.isNaN(n) ? NaN : n;
};

/** Converts a single field descriptor into a zod type (exported for tests). */
export function fieldToZod(field: FieldMeta): z.ZodTypeAny {
  const label = field.label;

  switch (field.type) {
    case "number": {
      const base = z.number({ invalid_type_error: `${label} must be a number` });
      return field.required
        ? z.preprocess(toNumber, base)
        : z.preprocess(toNumber, base.optional());
    }
    case "boolean": {
      // Checkbox always submits a boolean, but records loaded from the API may
      // omit the key entirely (legacy NON_NULL serialization) — stay lenient.
      return field.required
        ? z.boolean({ required_error: `${label} is required` })
        : z.boolean().optional();
    }
    case "date": {
      const base = z.string({ required_error: `${label} is required` });
      return field.required
        ? base.min(1, `${label} is required`)
        : z.preprocess(emptyToUndefined, base.optional());
    }
    case "select": {
      const base = z.string({ required_error: `Please select ${label}` });
      return field.required
        ? base.min(1, `Please select ${label}`)
        : z.preprocess(emptyToUndefined, base.optional());
    }
    default: {
      const base = z.string({ required_error: `${label} is required` });
      return field.required
        ? base.min(1, `${label} is required`)
        : z.preprocess(emptyToUndefined, base.optional());
    }
  }
}

/** Builds the Zod object for a whole entity (skips readOnly fields). */
export function entityMetaToZod(meta: EntityMeta): z.ZodObject<z.ZodRawShape> {
  const shape: z.ZodRawShape = {};
  for (const field of meta.fields) {
    if (field.readOnly) continue;
    shape[field.name] = fieldToZod(field);
  }
  return z.object(shape);
}

export type EntityFormValues = z.infer<ReturnType<typeof entityMetaToZod>>;
