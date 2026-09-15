import { useState } from "react";
import { useIndents, useCreateIndent, useUpdateIndentStatus } from "./queries";

const STATUSES = ["REQUESTED", "APPROVED", "DISPATCHED", "RECEIVED", "REJECTED"] as const;
const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];
const COMPONENTS = ["WHOLE_BLOOD", "PLASMA", "PLATELETS", "RED_CELLS", "CRYOPRECIPITATE"];
const STORES = ["BB-MAIN", "BB-OPD", "BB-OT", "BB-ICU", "BB-PHARMACY"];

type IndentFormState = {
  sourceStore: string;
  targetStore: string;
  bloodGroup: string;
  component: string;
  units: string;
  note: string;
};

type FormFieldProps = {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  error?: string;
  options?: string[];
};

function FormField({ label, value, onChange, type = "text", error, options }: FormFieldProps) {
  return (
    <label>
      {label}
      {type === "select" ? (
        <select value={value} onChange={(e) => onChange(e.target.value)}>
          <option value="">Select...</option>
          {options?.map((o) => (
            <option key={o} value={o}>{o}</option>
          ))}
        </select>
      ) : (
        <input type={type} value={value} onChange={(e) => onChange(e.target.value)} />
      )}
      {error && <span className="error">{error}</span>}
    </label>
  );
}

export function IndentWorkflow() {
  const { data: indents = [], isLoading } = useIndents();
  const createIndent = useCreateIndent();
  const updateStatus = useUpdateIndentStatus();
  const [showForm, setShowForm] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const blank: IndentFormState = { sourceStore: "", targetStore: "", bloodGroup: "", component: "", units: "", note: "" };
  const [form, setForm] = useState<IndentFormState>(blank);

  const update = (patch: Partial<IndentFormState>) => setForm((f) => ({ ...f, ...patch }));

  const handleSubmit = () => {
    const payload = {
      sourceStore: form.sourceStore,
      targetStore: form.targetStore,
      bloodGroup: form.bloodGroup,
      component: form.component,
      units: Number(form.units),
      note: form.note || undefined,
    };
    if (!payload.sourceStore || !payload.targetStore || !payload.bloodGroup || !payload.component) {
      setErrors({ sourceStore: payload.sourceStore ? "" : "Required", targetStore: payload.targetStore ? "" : "Required", bloodGroup: payload.bloodGroup ? "" : "Required", component: payload.component ? "" : "Required" });
      return;
    }
    if (Number.isNaN(payload.units) || payload.units <= 0) {
      setErrors({ units: "Valid units required" });
      return;
    }
    createIndent.mutate(payload, {
      onSuccess: () => {
        setShowForm(false);
        setForm(blank);
        setErrors({});
      },
    });
  };

  if (isLoading) return <p className="muted">Loading indents...</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Indent Workflow</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "Request Blood"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Indent Request</h2>
          <div className="form-grid">
            <FormField label="Source Store" value={form.sourceStore} onChange={(v) => update({ sourceStore: v })} type="select" error={errors.sourceStore} options={STORES} />
            <FormField label="Target Store" value={form.targetStore} onChange={(v) => update({ targetStore: v })} type="select" error={errors.targetStore} options={STORES} />
            <FormField label="Blood Group" value={form.bloodGroup} onChange={(v) => update({ bloodGroup: v })} type="select" error={errors.bloodGroup} options={BLOOD_GROUPS} />
            <FormField label="Component" value={form.component} onChange={(v) => update({ component: v })} type="select" error={errors.component} options={COMPONENTS} />
            <FormField label="Units" value={form.units} onChange={(v) => update({ units: v })} type="number" error={errors.units} />
            <FormField label="Note" value={form.note} onChange={(v) => update({ note: v })} />
          </div>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={createIndent.isPending}>
            {createIndent.isPending ? "Submitting..." : "Submit Request"}
          </button>
        </div>
      )}

      <div className="card">
        <h2>Inter-Store Requests</h2>
        <table className="table">
          <thead>
            <tr><th>Source</th><th>Target</th><th>Group</th><th>Component</th><th>Units</th><th>Status</th><th>Requested</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {indents.map((ind) => (
              <tr key={ind.id}>
                <td>{ind.sourceStore}</td>
                <td>{ind.targetStore}</td>
                <td>{ind.bloodGroup}</td>
                <td>{ind.component}</td>
                <td>{ind.units}</td>
                <td>{ind.status}</td>
                <td>{new Date(ind.requestedAt).toLocaleString()}</td>
                <td>
                  <select
                    value={ind.status}
                    onChange={(e) => updateStatus.mutate({ id: ind.id, status: e.target.value as typeof ind.status })}
                  >
                    {STATUSES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
            {indents.length === 0 && <tr><td colSpan={8} className="muted">No indent requests.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
