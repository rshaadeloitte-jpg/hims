import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { formDefinitionSchema, type FormDefinitionValues, type FormFieldValues } from "./zodSchema";
import { useFormSpecialties } from "./queries";

interface FormDesignerProps {
  initialData?: Partial<FormDefinitionValues>;
  onSubmit: (values: FormDefinitionValues) => void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

type SchemaMode = "visual" | "json";

const FIELD_TYPES = [
  { value: "string", label: "Text" },
  { value: "number", label: "Number" },
  { value: "integer", label: "Integer" },
  { value: "boolean", label: "Yes/No" },
  { value: "date", label: "Date" },
  { value: "datetime", label: "Date & Time" },
  { value: "enum", label: "Dropdown" },
] as const;

/**
 * FormDesigner — Notion-style warm minimalism.
 * Visual field palette + JSON schema editor with version & specialty metadata.
 */
export function FormDesigner({ initialData, onSubmit, onCancel, isSubmitting }: FormDesignerProps) {
  const { data: specialties = [] } = useFormSpecialties();
  const [fields, setFields] = useState<FormFieldValues[]>([]);
  const [schemaMode, setSchemaMode] = useState<SchemaMode>("visual");
  const [jsonText, setJsonText] = useState<string>(
    JSON.stringify(initialData?.schema ?? { type: "object", properties: {}, required: [] }, null, 2)
  );
  const [jsonError, setJsonError] = useState<string | null>(null);
  const [activeFieldIdx, setActiveFieldIdx] = useState<number | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormDefinitionValues>({
    resolver: zodResolver(formDefinitionSchema),
    defaultValues: {
      code: initialData?.code ?? "",
      name: initialData?.name ?? "",
      specialty: initialData?.specialty ?? "",
      description: initialData?.description ?? "",
      version: initialData?.version ?? 1,
      schema: initialData?.schema ?? { type: "object", properties: {}, required: [] },
    },
  });

  const buildSchemaFromFields = useCallback((): Record<string, unknown> => {
    const properties: Record<string, unknown> = {};
    const required: string[] = [];
    for (const f of fields) {
      const prop: Record<string, unknown> = { type: f.type === "enum" ? "string" : f.type, title: f.label };
      if (f.description) prop.description = f.description;
      if (f.enumValues && f.enumValues.length > 0) prop.enum = f.enumValues;
      if (f.defaultValue !== undefined) prop.default = f.defaultValue;
      properties[f.key] = prop;
      if (f.required) required.push(f.key);
    }
    return { type: "object", properties, required };
  }, [fields]);

  const handleFormSubmit = (values: FormDefinitionValues) => {
    let schema: Record<string, unknown>;
    if (schemaMode === "json") {
      const result = (() => {
        try {
          const parsed = JSON.parse(jsonText);
          if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
            return { ok: false, error: "Schema must be a JSON object" };
          }
          return { ok: true, value: parsed };
        } catch (e) {
          return { ok: false, error: e instanceof Error ? e.message : "Invalid JSON" };
        }
      })();
      if (!result.ok) {
        setJsonError((result as { error: string }).error);
        return;
      }
      schema = (result as { value: Record<string, unknown> }).value;
      setJsonError(null);
    } else {
      schema = buildSchemaFromFields();
    }
    onSubmit({ ...values, schema });
  };

  const addField = () => {
    setFields([...fields, { key: `field_${fields.length + 1}`, label: "", type: "string", required: false }]);
    setActiveFieldIdx(fields.length);
  };

  const updateField = (idx: number, updates: Partial<FormFieldValues>) => {
    setFields(fields.map((f, i) => (i === idx ? { ...f, ...updates } : f)));
  };

  const removeField = (idx: number) => {
    setFields(fields.filter((_, i) => i !== idx));
    if (activeFieldIdx === idx) setActiveFieldIdx(null);
  };

  const moveField = (idx: number, dir: -1 | 1) => {
    const target = idx + dir;
    if (target < 0 || target >= fields.length) return;
    const next = [...fields];
    [next[idx], next[target]] = [next[target], next[idx]];
    setFields(next);
    setActiveFieldIdx(target);
  };

  return (
    <div className="form-designer">
      <form onSubmit={handleSubmit(handleFormSubmit)} className="form-designer__form">
        {/* Metadata section — Notion-style card */}
        <section className="form-designer__section">
          <h3 className="form-designer__section-title">Form Details</h3>
          <div className="form-designer__grid">
            <div className="form-designer__field">
              <label className="form-designer__label">Code *</label>
              <input
                className="form-designer__input"
                placeholder="CONSULT_NOTE_V1"
                {...register("code")}
              />
              {errors.code && <span className="form-designer__error">{errors.code.message}</span>}
            </div>
            <div className="form-designer__field">
              <label className="form-designer__label">Name *</label>
              <input
                className="form-designer__input"
                placeholder="Consultation Note"
                {...register("name")}
              />
              {errors.name && <span className="form-designer__error">{errors.name.message}</span>}
            </div>
            <div className="form-designer__field">
              <label className="form-designer__label">Specialty *</label>
              <input
                className="form-designer__input"
                placeholder="General Medicine"
                list="specialty-suggestions"
                {...register("specialty")}
              />
              <datalist id="specialty-suggestions">
                {specialties.map((s) => <option key={s} value={s} />)}
              </datalist>
              {errors.specialty && <span className="form-designer__error">{errors.specialty.message}</span>}
            </div>
            <div className="form-designer__field">
              <label className="form-designer__label">Version *</label>
              <input
                type="number"
                min={1}
                className="form-designer__input form-designer__input--narrow"
                {...register("version", { valueAsNumber: true })}
              />
              {errors.version && <span className="form-designer__error">{errors.version.message}</span>}
            </div>
            <div className="form-designer__field form-designer__field--wide">
              <label className="form-designer__label">Description</label>
              <input
                className="form-designer__input"
                placeholder="Optional description..."
                {...register("description")}
              />
            </div>
          </div>
        </section>

        {/* Schema editor section */}
        <section className="form-designer__section">
          <div className="form-designer__section-header">
            <h3 className="form-designer__section-title">Schema</h3>
            <div className="form-designer__mode-toggle" role="group" aria-label="Schema mode">
              <button
                type="button"
                className={`form-designer__mode-btn${schemaMode === "visual" ? " form-designer__mode-btn--active" : ""}`}
                onClick={() => setSchemaMode("visual")}
              >
                Visual
              </button>
              <button
                type="button"
                className={`form-designer__mode-btn${schemaMode === "json" ? " form-designer__mode-btn--active" : ""}`}
                onClick={() => setSchemaMode("json")}
              >
                JSON
              </button>
            </div>
          </div>

          {schemaMode === "visual" ? (
            <div className="form-designer__visual">
              {fields.length === 0 ? (
                <p className="form-designer__empty">No fields yet. Click "Add Field" to start building your form.</p>
              ) : (
                <div className="form-designer__field-list">
                  {fields.map((field, idx) => (
                    <div
                      key={idx}
                      className={`form-designer__field-card${activeFieldIdx === idx ? " form-designer__field-card--active" : ""}`}
                      onClick={() => setActiveFieldIdx(idx)}
                    >
                      <div className="form-designer__field-card-header">
                        <span className="form-designer__field-card-key">{field.key || "(unnamed)"}</span>
                        <span className="form-designer__field-card-type">{field.type}</span>
                        <div className="form-designer__field-card-actions">
                          <button type="button" className="form-designer__icon-btn" onClick={(e) => { e.stopPropagation(); moveField(idx, -1); }} disabled={idx === 0}>↑</button>
                          <button type="button" className="form-designer__icon-btn" onClick={(e) => { e.stopPropagation(); moveField(idx, 1); }} disabled={idx === fields.length - 1}>↓</button>
                          <button type="button" className="form-designer__icon-btn form-designer__icon-btn--danger" onClick={(e) => { e.stopPropagation(); removeField(idx); }}>✕</button>
                        </div>
                      </div>
                      {field.required && <span className="form-designer__badge">Required</span>}
                    </div>
                  ))}
                </div>
              )}
              <button type="button" className="form-designer__add-btn" onClick={addField}>
                + Add Field
              </button>

              {activeFieldIdx !== null && fields[activeFieldIdx] && (
                <div className="form-designer__field-editor">
                  <h4 className="form-designer__editor-title">Edit Field</h4>
                  <div className="form-designer__grid">
                    <div className="form-designer__field">
                      <label className="form-designer__label">Key *</label>
                      <input
                        className="form-designer__input"
                        value={fields[activeFieldIdx].key}
                        onChange={(e) => updateField(activeFieldIdx, { key: e.target.value })}
                      />
                    </div>
                    <div className="form-designer__field">
                      <label className="form-designer__label">Label *</label>
                      <input
                        className="form-designer__input"
                        value={fields[activeFieldIdx].label}
                        onChange={(e) => updateField(activeFieldIdx, { label: e.target.value })}
                      />
                    </div>
                    <div className="form-designer__field">
                      <label className="form-designer__label">Type</label>
                      <select
                        className="form-designer__input"
                        value={fields[activeFieldIdx].type}
                        onChange={(e) => updateField(activeFieldIdx, { type: e.target.value as FormFieldValues["type"] })}
                      >
                        {FIELD_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                      </select>
                    </div>
                    <div className="form-designer__field form-designer__field--checkbox">
                      <label className="form-designer__label">
                        <input
                          type="checkbox"
                          checked={fields[activeFieldIdx].required}
                          onChange={(e) => updateField(activeFieldIdx, { required: e.target.checked })}
                        />
                        Required
                      </label>
                    </div>
                    <div className="form-designer__field form-designer__field--wide">
                      <label className="form-designer__label">Description</label>
                      <input
                        className="form-designer__input"
                        value={fields[activeFieldIdx].description ?? ""}
                        onChange={(e) => updateField(activeFieldIdx, { description: e.target.value || undefined })}
                      />
                    </div>
                    {fields[activeFieldIdx].type === "enum" && (
                      <div className="form-designer__field form-designer__field--wide">
                        <label className="form-designer__label">Options (comma-separated)</label>
                        <input
                          className="form-designer__input"
                          value={(fields[activeFieldIdx].enumValues ?? []).join(", ")}
                          onChange={(e) => updateField(activeFieldIdx, { enumValues: e.target.value.split(",").map((s) => s.trim()).filter(Boolean) })}
                        />
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="form-designer__json">
              <textarea
                className="form-designer__textarea"
                value={jsonText}
                onChange={(e) => { setJsonText(e.target.value); setJsonError(null); }}
                rows={16}
                spellCheck={false}
              />
              {jsonError && <span className="form-designer__error">{jsonError}</span>}
            </div>
          )}
        </section>

        {/* Actions */}
        <div className="form-designer__actions">
          <button type="button" className="form-designer__btn form-designer__btn--secondary" onClick={onCancel}>
            Cancel
          </button>
          <button type="submit" className="form-designer__btn form-designer__btn--primary" disabled={isSubmitting}>
            {isSubmitting ? "Saving..." : "Save Form"}
          </button>
        </div>
      </form>
    </div>
  );
}
