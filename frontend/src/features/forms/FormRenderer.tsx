import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useSubmitForm } from "./queries";
import type { FormSchemaResponse } from "./types";

interface FormRendererProps {
  schemaResponse: FormSchemaResponse;
  patientId?: number;
  encounterId?: number;
  onSuccess?: () => void;
}

/**
 * Converts a JSON Schema object to a Zod schema for validation.
 * Supports: string, number, integer, boolean, enum, date, datetime formats.
 */
function jsonSchemaToZod(schema: Record<string, unknown>): z.ZodObject<z.ZodRawShape> {
  const properties = (schema.properties ?? {}) as Record<string, unknown>;
  const required = (schema.required ?? []) as string[];
  const shape: z.ZodRawShape = {};

  for (const [key, prop] of Object.entries(properties)) {
    const p = prop as Record<string, unknown>;
    const ptype = p.type as string;
    const isRequired = required.includes(key);
    const label = (p.title as string) ?? key;
    const description = p.description as string | undefined;

    let fieldSchema: z.ZodTypeAny;

    if (ptype === "boolean") {
      fieldSchema = z.boolean({ required_error: `${label} is required` });
      if (!isRequired) fieldSchema = fieldSchema.optional();
    } else if (ptype === "integer") {
      const base = z.number({ required_error: `${label} is required`, invalid_type_error: `${label} must be an integer` }).int();
      fieldSchema = isRequired ? base : base.optional();
    } else if (ptype === "number") {
      const base = z.number({ required_error: `${label} is required`, invalid_type_error: `${label} must be a number` });
      fieldSchema = isRequired ? base : base.optional();
    } else {
      // string (with format variants) and enum
      const enumVals = p.enum as string[] | undefined;
      if (enumVals) {
        fieldSchema = z.enum(enumVals as [string, ...string[]], { required_error: `Please select ${label}` });
        if (!isRequired) fieldSchema = fieldSchema.optional();
      } else {
        const base = z.string({ required_error: `${label} is required` });
        if (isRequired) {
          fieldSchema = base.min(1, `${label} is required`);
        } else {
          fieldSchema = base.optional();
        }
        if (description) {
          fieldSchema = fieldSchema.describe(description);
        }
      }
    }

    shape[key] = fieldSchema;
  }

  return z.object(shape);
}

/**
 * FormRenderer — Stripe-style form rendering with Zod validation.
 * Clean, minimal, focused input experience with inline validation.
 */
export function FormRenderer({ schemaResponse, patientId, encounterId, onSuccess }: FormRendererProps) {
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const submitM = useSubmitForm(schemaResponse.id);
  const validationSchema = jsonSchemaToZod(schemaResponse.schema);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(validationSchema),
    mode: "onChange",
  });

  const properties = (schemaResponse.schema.properties ?? {}) as Record<string, unknown>;
  const required = (schemaResponse.schema.required ?? []) as string[];

  const onSubmit = (data: Record<string, unknown>) => {
    setSubmitSuccess(false);
    submitM.mutate(
      { data, patientId, encounterId },
      {
        onSuccess: () => {
          setSubmitSuccess(true);
          onSuccess?.();
        },
      }
    );
  };

  const renderField = (key: string, prop: Record<string, unknown>) => {
    const ptype = prop.type as string;
    const label = (prop.title as string) ?? key;
    const isRequired = required.includes(key);
    const description = prop.description as string | undefined;
    const error = errors[key];
    const fieldId = `field-${key}`;

    if (ptype === "boolean") {
      return (
        <div key={key} className="form-renderer__field form-renderer__field--checkbox">
          <label htmlFor={fieldId} className="form-renderer__checkbox-label">
            <input id={fieldId} type="checkbox" className="form-renderer__checkbox" {...register(key)} />
            <span className="form-renderer__checkbox-text">{label}{isRequired && <span className="form-renderer__required"> *</span>}</span>
          </label>
          {error && <span className="form-renderer__error">{error.message?.toString()}</span>}
        </div>
      );
    }

    if (ptype === "enum" || prop.enum) {
      const enumVals = (prop.enum as unknown[]) ?? [];
      return (
        <div key={key} className="form-renderer__field">
          <label htmlFor={fieldId} className="form-renderer__label">{label}{isRequired && <span className="form-renderer__required"> *</span>}</label>
          <select id={fieldId} className="form-renderer__select" {...register(key)}>
            <option value="">— Select —</option>
            {enumVals.map((opt) => <option key={String(opt)} value={String(opt)}>{String(opt)}</option>)}
          </select>
          {description && <span className="form-renderer__hint">{description}</span>}
          {error && <span className="form-renderer__error">{error.message?.toString()}</span>}
        </div>
      );
    }

    const inputType = ptype === "number" || ptype === "integer" ? "number" : ptype === "date" ? "date" : ptype === "datetime" ? "datetime-local" : "text";
    const step = ptype === "number" ? "0.01" : ptype === "integer" ? "1" : undefined;

    return (
      <div key={key} className="form-renderer__field">
        <label htmlFor={fieldId} className="form-renderer__label">{label}{isRequired && <span className="form-renderer__required"> *</span>}</label>
        <input
          id={fieldId}
          type={inputType}
          step={step}
          className="form-renderer__input"
          aria-invalid={error ? "true" : "false"}
          {...register(key, { valueAsNumber: ptype === "number" || ptype === "integer" })}
        />
        {description && <span className="form-renderer__hint">{description}</span>}
        {error && <span className="form-renderer__error">{error.message?.toString()}</span>}
      </div>
    );
  };

  return (
    <div className="form-renderer">
      <div className="form-renderer__header">
        <h2 className="form-renderer__title">{schemaResponse.name}</h2>
        <div className="form-renderer__meta">
          <span className="form-renderer__badge">{schemaResponse.specialty}</span>
          <span className="form-renderer__badge form-renderer__badge--muted">v{schemaResponse.version}</span>
        </div>
      </div>

      {schemaResponse.description && (
        <p className="form-renderer__description">{schemaResponse.description}</p>
      )}

      {submitSuccess && (
        <div className="form-renderer__success" role="status">
          ✓ Submitted successfully
        </div>
      )}

      {submitM.isError && (
        <div className="form-renderer__alert" role="alert">
          {String((submitM.error as Error).message)}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="form-renderer__form" noValidate>
        <div className="form-renderer__fields">
          {Object.entries(properties).map(([key, prop]) => renderField(key, prop as Record<string, unknown>))}
        </div>

        <div className="form-renderer__actions">
          <button
            type="submit"
            className="form-renderer__submit"
            disabled={submitM.isPending}
          >
            {submitM.isPending ? "Submitting..." : "Submit"}
          </button>
        </div>
      </form>
    </div>
  );
}
