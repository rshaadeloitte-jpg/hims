import { useState } from "react";
import { useBloodBags, useBloodBankStock, useCreateBloodBag, useUpdateBloodBagStatus } from "./queries";
import { bloodBagSchema } from "./zodSchema";

const COMPONENTS = ["WHOLE_BLOOD", "PLASMA", "PLATELETS", "RED_CELLS", "CRYOPRECIPITATE"];
const STATUSES = ["AVAILABLE", "RESERVED", "ISSUED", "EXPIRED", "DISCARDED"];

export function BloodBankPage() {
  const { data: bags = [], isLoading } = useBloodBags();
  const { data: stock } = useBloodBankStock();
  const createBag = useCreateBloodBag();
  const updateStatus = useUpdateBloodBagStatus();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ bloodGroup: "", component: "", donorId: "", volume: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const result = bloodBagSchema.safeParse({ ...form, donorId: Number(form.donorId), volume: Number(form.volume) });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createBag.mutate(result.data, { onSuccess: () => { setShowForm(false); setForm({ bloodGroup: "", component: "", donorId: "", volume: "" }); setErrors({}); } });
  };

  if (isLoading) return <p className="muted">Loading blood bank…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Blood Bank</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "Register Bag"}
        </button>
      </header>

      {stock && (
        <div className="card">
          <h2>Stock Summary</h2>
          <div className="stock-grid">
            {Object.entries(stock).map(([group, count]) => (
              <div key={group} className="stock-item"><span>{group}</span><strong>{count}</strong></div>
            ))}
          </div>
        </div>
      )}

      {showForm && (
        <div className="card">
          <h2>New Blood Bag</h2>
          <div className="form-grid">
            <label>Blood Group {errors.bloodGroup && <span className="error">{errors.bloodGroup}</span>}</label>
            <select value={form.bloodGroup} onChange={(e) => setForm({ ...form, bloodGroup: e.target.value })}>
              <option value="">Select…</option>
              {["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"].map((g) => <option key={g} value={g}>{g}</option>)}
            </select>
            <label>Component {errors.component && <span className="error">{errors.component}</span>}</label>
            <select value={form.component} onChange={(e) => setForm({ ...form, component: e.target.value })}>
              <option value="">Select…</option>
              {COMPONENTS.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
            <label>Donor ID {errors.donorId && <span className="error">{errors.donorId}</span>}</label>
            <input value={form.donorId} onChange={(e) => setForm({ ...form, donorId: e.target.value })} />
            <label>Volume (ml) {errors.volume && <span className="error">{errors.volume}</span>}</label>
            <input type="number" value={form.volume} onChange={(e) => setForm({ ...form, volume: e.target.value })} />
          </div>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={createBag.isPending}>
            {createBag.isPending ? "Saving…" : "Save Bag"}
          </button>
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr><th>Bag No</th><th>Group</th><th>Component</th><th>Collected</th><th>Expiry</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {bags.map((b) => (
              <tr key={b.id}>
                <td>{b.bagNo}</td>
                <td>{b.bloodGroup}</td>
                <td>{b.component}</td>
                <td>{b.collectionDate}</td>
                <td>{b.expiryDate}</td>
                <td>{b.status}</td>
                <td>
                  <select value={b.status} onChange={(e) => updateStatus.mutate({ id: b.id, status: e.target.value })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </td>
              </tr>
            ))}
            {bags.length === 0 && <tr><td colSpan={7} className="muted">No bags registered.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
