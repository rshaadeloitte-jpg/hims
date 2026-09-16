import type { ChangeEvent } from "react";
import { HimsSelect, TextInput } from "../../../components/ui";
import { Button, Text, Textarea } from "@chakra-ui/react";

interface JsonSchemaFormProps {
  /** JSON Schema object (type: object with properties). */
  schema: Record<string, unknown>;
  /** Current form data (controlled). */
  data: Record<string, unknown>;
  /** Called on every value change. */
  onChange: (data: Record<string, unknown>) => void;
  /** Path prefix for nested arrays (used internally). */
  _prefix?: string;
}

/**
 * Renders a JSON Schema (subset) as dynamic form inputs.
 * Supports: string, number, integer, boolean, enum, array of strings,
 * array of objects (with nested properties), date, date-time formats.
 */
export function JsonSchemaForm({ schema, data, onChange, _prefix = "" }: JsonSchemaFormProps) {
  const properties = (schema.properties ?? {}) as Record<string, unknown>;
  const required = (schema.required ?? []) as string[];
  const title = schema.title as string | undefined;

  const setValue = (key: string, value: unknown) => {
    onChange({ ...data, [key]: value });
  };

  const setNested = (key: string, index: number, nested: Record<string, unknown>) => {
    const arr = (data[key] ?? []) as Record<string, unknown>[];
    const next = [...arr];
    next[index] = nested;
    setValue(key, next);
  };

  return (
    <div className="json-schema-form">
      {title && <h3 className="json-schema-form__title">{title}</h3>}
      {Object.entries(properties).map(([key, prop]) => {
        const p = prop as Record<string, unknown>;
        const ptype = p.type as string;
        const isRequired = required.includes(key);
        const label = (p.title as string) ?? key;
        const path = _prefix ? `${_prefix}.${key}` : key;
        const value = data[key];

        if (ptype === "object" && p.properties) {
          return (
            <fieldset key={path} className="json-schema-form__group">
              <legend>{label}</legend>
              <JsonSchemaForm
                schema={p}
                data={(value ?? {}) as Record<string, unknown>}
                onChange={(nested) => setValue(key, nested)}
                _prefix={path}
              />
            </fieldset>
          );
        }

        if (ptype === "array" && p.items) {
          const items = p.items as Record<string, unknown>;
          const arr = (value ?? []) as unknown[];
          const isObjectArray = items.type === "object" && items.properties;
          return (
            <div key={path} className="json-schema-form__array">
              <Text fontSize="sm" fontWeight="medium">{label}{isRequired ? <span className="required">*</span> : ""}</Text>
              {arr.map((item, i) => (
                <div key={i} className="json-schema-form__array-item">
                  {isObjectArray ? (
                    <JsonSchemaForm
                      schema={items}
                      data={item as Record<string, unknown>}
                      onChange={(nested) => setNested(key, i, nested)}
                      _prefix={`${path}[${i}]`}
                    />
                  ) : (
                    <ArrayItemInput
                      value={item as string}
                      onChange={(v) => {
                        const next = [...arr];
                        next[i] = v;
                        setValue(key, next);
                      }}
                      items={items}
                    />
                  )}
                  <Button variant="ghost" size="sm"
                    type="button"
                    onClick={() => setValue(key, arr.filter((_, j) => j !== i))}
                  >
                    ✕
                  </Button>
                </div>
              ))}
              <Button variant="secondary" size="sm"
                type="button"
                onClick={() => {
                  const next = [...arr, isObjectArray ? {} : ""];
                  setValue(key, next);
                }}
              >
                + Add
              </Button>
            </div>
          );
        }

        return (
          <div key={path} className="json-schema-form__field">
            <Text fontSize="sm" fontWeight="medium">
              {label}{isRequired ? <span className="required">*</span> : ""}
            </Text>
            <FieldInput
              prop={p}
              value={value}
              onChange={(v) => setValue(key, v)}
            />
          </div>
        );
      })}
      <style>{`
        .json-schema-form { display: flex; flex-direction: column; gap: 0.75rem; }
        .json-schema-form__title { font-size: 1.1rem; font-weight: 600; margin: 0; }
        .json-schema-form__group { border: 1px solid var(--border, #d0d0d0); border-radius: 6px; padding: 0.75rem; }
        .json-schema-form__group legend { font-weight: 600; padding: 0 0.5rem; }
        .json-schema-form__field { display: flex; flex-direction: column; gap: 0.25rem; }
        .json-schema-form__label { font-size: 0.875rem; font-weight: 500; }
        .json-schema-form__label .required { color: #d32f2f; margin-left: 2px; }
        .json-schema-form__array { display: flex; flex-direction: column; gap: 0.5rem; }
        .json-schema-form__array-item { display: flex; gap: 0.5rem; align-items: flex-start; }
        .json-schema-form__array-item > .json-schema-form { flex: 1; }
        .json-schema-form input, .json-schema-form select, .json-schema-form textarea {
          padding: 0.4rem 0.6rem; border: 1px solid var(--border, #d0d0d0); border-radius: 4px; font-size: 0.875rem;
        }
      `}</style>
    </div>
  );
}

function FieldInput({ prop, value, onChange }: {
  prop: Record<string, unknown>;
  value: unknown;
  onChange: (v: unknown) => void;
}) {
  const ptype = prop.type as string;
  const enumVals = prop.enum as unknown[] | undefined;
  const format = prop.format as string | undefined;

  if (enumVals) {
    return (
      <HimsSelect value={String(value ?? "")} onChange={(e) => onChange(e.target.value)}>
        <option value="">— Select —</option>
        {enumVals.map((opt) => (
          <option key={String(opt)} value={String(opt)}>{String(opt)}</option>
        ))}
      </HimsSelect>
    );
  }

  if (ptype === "boolean") {
    return (
      <input
        type="checkbox"
        checked={Boolean(value)}
        onChange={(e: ChangeEvent<HTMLInputElement>) => onChange(e.target.checked)}
      />
    );
  }

  if (ptype === "integer" || ptype === "number") {
    const inputType = ptype === "integer" ? "number" : "number";
    const step = ptype === "number" ? "0.01" : "1";
    return (
      <TextInput
        type={inputType}
        step={step}
        value={String(value ?? "")}
        onChange={(e) => onChange(e.target.value === "" ? "" : ptype === "integer" ? parseInt(e.target.value, 10) : parseFloat(e.target.value))}
      />
    );
  }

  if (ptype === "array" && !prop.items) {
    return (
      <Textarea bg="white" borderColor="gray.300"
        value={String(value ?? "")}
        onChange={(e) => onChange(e.target.value)}
        rows={2}
      />
    );
  }

  // default: string
  if (format === "date-time") {
    return (
      <TextInput
        type="datetime-local"
        value={String(value ?? "")}
        onChange={(e) => onChange(e.target.value)}
      />
    );
  }
  if (format === "date") {
    return (
      <TextInput
        type="date"
        value={String(value ?? "")}
        onChange={(e) => onChange(e.target.value)}
      />
    );
  }

  return (
    <TextInput
      type="text"
      value={String(value ?? "")}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

function ArrayItemInput({ value, onChange, items }: {
  value: string;
  onChange: (v: string) => void;
  items: Record<string, unknown>;
}) {
  const enumVals = items.enum as unknown[] | undefined;
  if (enumVals) {
    return (
      <HimsSelect value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">— Select —</option>
        {enumVals.map((opt) => (
          <option key={String(opt)} value={String(opt)}>{String(opt)}</option>
        ))}
      </HimsSelect>
    );
  }
  return (
    <TextInput
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}