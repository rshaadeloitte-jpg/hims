import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { entityMetaToZod, type EntityFormValues } from "../zodSchema";
import { editableFields, type EntityMeta } from "../entityMeta";

interface MasterFormProps {
  meta: EntityMeta;
  /** "edit" sends only dirty fields on PUT (red-team F-18); "create" sends all. */
  mode?: "create" | "edit";
  /** Record being edited (edit mode); undefined in create mode. */
  record?: Record<string, unknown> | null;
  /** True while the save mutation is in flight. */
  submitting: boolean;
  onSubmit: (values: Record<string, unknown>) => void;
  onCancel: () => void;
}

/** RHF `values` sync: applies when the async record arrives / user untouched. */
function formValues(meta: EntityMeta, record: Record<string, unknown> | null | undefined): EntityFormValues {
  return recordToFormValues(meta, record);
}

/** Converts a legacy record into RHF default values (strings for inputs). */
export function recordToFormValues(
  meta: EntityMeta,
  record: Record<string, unknown> | null | undefined,
): EntityFormValues {
  const values: Record<string, unknown> = {};
  for (const field of editableFields(meta)) {
    const raw = record?.[field.name];
    if (field.type === "boolean") {
      // Absent booleans become false in the FORM (controlled checkbox); they
      // are excluded from the save payload unless the user actually toggled
      // them (see filterDirtyFields) — NULLs round-trip losslessly.
      values[field.name] = Boolean(raw);
    } else if (raw === null || raw === undefined) {
      values[field.name] = "";
    } else if (field.type === "number") {
      values[field.name] = String(raw);
    } else {
      values[field.name] = String(raw);
    }
  }
  return values as EntityFormValues;
}

/**
 * Red-team F-18: on edit (PUT, nonNull=true) only fields the user actually
 * changed are sent — fields the form defaulted (absent booleans -> false,
 * absent numbers -> "") stay undefined/untouched server-side, so optional
 * NULLs round-trip losslessly instead of being clobbered by defaults.
 */
export function filterDirtyFields(
  values: Record<string, unknown>,
  dirtyFields: Record<string, unknown>,
): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(values)) {
    if (dirtyFields[key]) result[key] = value;
  }
  return result;
}

/**
 * Schema-driven form: inputs, layout and validation are generated from
 * EntityMeta via React Hook Form + zodResolver. Field widgets follow the
 * FieldMeta.type mapping (text/number/boolean/date/select).
 */
export function MasterForm({ meta, mode = "edit", record, submitting, onSubmit, onCancel }: MasterFormProps) {
  const schema = useMemo(() => entityMetaToZod(meta), [meta]);
  const {
    register,
    handleSubmit,
    formState: { errors, isDirty, dirtyFields },
  } = useForm<EntityFormValues>({
    resolver: zodResolver(schema),
    values: formValues(meta, record),
  });

  const fields = editableFields(meta);

  const handleFormSubmit = handleSubmit((values) => {
    const parsed = values as Record<string, unknown>;
    // F-18: PUT sends only dirty fields (undefined = untouched); POST sends the
    // whole validated payload (create is a full write including required fields).
    const payload =
      mode === "edit" ? filterDirtyFields(parsed, dirtyFields as Record<string, unknown>) : parsed;
    onSubmit(payload);
  });

  return (
    <form
      className="master-form"
      onSubmit={handleFormSubmit}
      noValidate
    >
      <div className="form-grid">
        {fields.map((field) => {
          const error = errors[field.name];
          return (
            <div
              key={field.name}
              className={`field ${error ? "field--error" : ""} ${field.type === "boolean" ? "field--check" : ""}`}
            >
              <label htmlFor={`field-${field.name}`}>
                {field.label}
                {field.required ? <span className="req" aria-hidden="true"> *</span> : null}
              </label>

              {field.type === "boolean" ? (
                <div className="checkbox-row">
                  <input
                    id={`field-${field.name}`}
                    type="checkbox"
                    {...register(field.name)}
                  />
                </div>
              ) : field.type === "select" && field.options ? (
                <select id={`field-${field.name}`} {...register(field.name)}>
                  <option value="">— Select —</option>
                  {field.options.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  id={`field-${field.name}`}
                  type={field.type === "number" ? "number" : field.type === "date" ? "date" : "text"}
                  step={field.type === "number" ? "any" : undefined}
                  placeholder={field.placeholder}
                  aria-invalid={error ? true : undefined}
                  {...register(field.name)}
                />
              )}

              {error ? (
                <p className="field-error" role="alert">
                  {String(error.message ?? "Invalid value")}
                </p>
              ) : null}
            </div>
          );
        })}
      </div>

      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "Saving…" : isDirty ? "Save Changes" : "Save"}
        </button>
      </div>
    </form>
  );
}
