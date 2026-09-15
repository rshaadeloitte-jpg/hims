import { useState } from "react";
import { useClaims, useSubmitClaim } from "./queries";
import { claimSchema } from "./zodSchema";

export function RcmNewPage() {
  const { data: claims = [], isLoading } = useClaims();
  const submitClaim = useSubmitClaim();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ patientName: "", scheme: "", amount: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const result = claimSchema.safeParse({ ...form, amount: Number(form.amount) });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    submitClaim.mutate(result.data, { onSuccess: () => { setShowForm(false); setForm({ patientName: "", scheme: "", amount: "" }); setErrors({}); } });
  };

  if (isLoading) return <p className="muted">Loading claims…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>RCM & Claims</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "New Claim"}
        </button>
      </header>
      {showForm && (
        <div className="card">
          <h2>Submit Claim</h2>
          <div className="form-grid">
            <label>Patient {errors.patientName && <span className="error">{errors.patientName}</span>}</label>
            <input value={form.patientName} onChange={(e) => setForm({ ...form, patientName: e.target.value })} />
            <label>Scheme {errors.scheme && <span className="error">{errors.scheme}</span>}</label>
            <input value={form.scheme} onChange={(e) => setForm({ ...form, scheme: e.target.value })} />
            <label>Amount {errors.amount && <span className="error">{errors.amount}</span>}</label>
            <input type="number" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
          </div>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={submitClaim.isPending}>
            {submitClaim.isPending ? "Submitting…" : "Submit"}
          </button>
        </div>
      )}
      <div className="card">
        <table className="table">
          <thead><tr><th>Patient</th><th>Scheme</th><th>Amount</th><th>Status</th><th>Submitted</th></tr></thead>
          <tbody>
            {claims.map((c) => <tr key={c.id}><td>{c.patientName}</td><td>{c.scheme}</td><td>{c.amount}</td><td>{c.status}</td><td>{c.submittedAt}</td></tr>)}
            {claims.length === 0 && <tr><td colSpan={5} className="muted">No claims.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
