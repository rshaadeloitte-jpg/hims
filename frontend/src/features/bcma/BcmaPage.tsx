import { useState } from "react";
import { useRecordBcmaScan } from "./queries";

export function BcmaPage() {
  const recordScan = useRecordBcmaScan();
  const [form, setForm] = useState({ patientId: "", drugIen: "", drugName: "" });

  return (
    <section className="page">
      <header className="page-header"><h1>BCMA Barcode Verification</h1></header>
      <div className="card">
        <h2>Record Scan</h2>
        <div className="form-grid">
          <label>Patient ID</label>
          <input value={form.patientId} onChange={(e) => setForm({ ...form, patientId: e.target.value })} />
          <label>Drug IEN</label>
          <input value={form.drugIen} onChange={(e) => setForm({ ...form, drugIen: e.target.value })} />
          <label>Drug Name</label>
          <input value={form.drugName} onChange={(e) => setForm({ ...form, drugName: e.target.value })} />
        </div>
        <button className="btn btn-primary" onClick={() => recordScan.mutate({ patientId: Number(form.patientId), drugIen: form.drugIen, drugName: form.drugName })}>
          Record Scan
        </button>
      </div>
      <div className="card">
        <p className="muted">Scan history is not available.</p>
      </div>
    </section>
  );
}
