import { useState } from "react";
import { useCrossMatches, useCreateCrossMatch, useUpdateCrossMatchCompatibility } from "./queries";

const METHODS = ["MAJOR", "MINOR", "BOTH"] as const;
const STATUSES = ["COMPATIBLE", "INCOMPATIBLE", "PENDING"] as const;

type CrossMatchFormState = {
  bagId: string;
  patientId: string;
  method: "MAJOR" | "MINOR" | "BOTH";
  note: string;
};

function field(label: string, value: string, onChange: (v: string) => void, type = "text", extra?: React.ReactNode, options?: string[]) {
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
      {extra}
    </label>
  );
}

export function CrossMatch() {
  const { data: matches = [], isLoading } = useCrossMatches();
  const createMatch = useCreateCrossMatch();
  const updateCompatibility = useUpdateCrossMatchCompatibility();
  const [showForm, setShowForm] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const blank: CrossMatchFormState = { bagId: "", patientId: "", method: "MAJOR", note: "" };
  const [form, setForm] = useState<CrossMatchFormState>(blank);

  const update = (patch: Partial<CrossMatchFormState>) => setForm((f) => ({ ...f, ...patch }));

  const handleSubmit = () => {
    const payload = {
      bagId: Number(form.bagId),
      patientId: Number(form.patientId),
      method: form.method,
      note: form.note || undefined,
    };
    if (Number.isNaN(payload.bagId) || payload.bagId <= 0) {
      setErrors({ bagId: "Valid bag ID required" });
      return;
    }
    if (Number.isNaN(payload.patientId) || payload.patientId <= 0) {
      setErrors({ patientId: "Valid patient ID required" });
      return;
    }
    createMatch.mutate(payload, {
      onSuccess: () => {
        setShowForm(false);
        setForm(blank);
        setErrors({});
      },
    });
  };

  if (isLoading) return <p className="muted">Loading cross matches...</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Cross Match</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New Cross Match"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Cross Match</h2>
          <div className="form-grid">
            {field("Bag ID", form.bagId, (v) => update({ bagId: v }), "number", errors.bagId && <span className="error">{errors.bagId}</span>)}
            {field("Patient ID", form.patientId, (v) => update({ patientId: v }), "number", errors.patientId && <span className="error">{errors.patientId}</span>)}
            {field("Method", form.method, (v) => update({ method: v as CrossMatchFormState["method"] }), "select", undefined, Array.from(METHODS))}
            {field("Note", form.note, (v) => update({ note: v }))}
          </div>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={createMatch.isPending}>
            {createMatch.isPending ? "Saving..." : "Submit"}
          </button>
        </div>
      )}

      <div className="card">
        <h2>Cross Match Requests</h2>
        <table className="table">
          <thead>
            <tr><th>Bag</th><th>Patient</th><th>Method</th><th>Compatibility</th><th>Requested At</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {matches.map((m) => (
              <tr key={m.id}>
                <td>{m.bagId}</td>
                <td>{m.patientId}</td>
                <td>{m.method}</td>
                <td>{m.compatibility}</td>
                <td>{new Date(m.requestedAt).toLocaleString()}</td>
                <td>
                  <select
                    value={m.compatibility}
                    onChange={(e) => updateCompatibility.mutate({ id: m.id, compatibility: e.target.value as typeof m.compatibility })}
                  >
                    {Array.from(STATUSES).map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
            {matches.length === 0 && <tr><td colSpan={6} className="muted">No cross matches.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
