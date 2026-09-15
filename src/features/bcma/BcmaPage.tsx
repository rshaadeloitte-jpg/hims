import { useState } from "react";
import { useBcmaScans, useRecordBcmaScan } from "./queries";
import { bcmaScanSchema } from "./zodSchema";

export function BcmaPage() {
  const { data: scans = [], isLoading } = useBcmaScans();
  const recordScan = useRecordBcmaScan();
  const [form, setForm] = useState({ patientId: "", drugId: "" });

  if (isLoading) return <p className="muted">Loading BCMA scans…</p>;

  return (
    <section className="page">
      <header className="page-header"><h1>BCMA Barcode Verification</h1></header>
      <div className="card">
        <h2>Record Scan</h2>
        <div className="form-grid">
          <label>Patient ID</label>
          <input value={form.patientId} onChange={(e) => setForm({ ...form, patientId: e.target.value })} />
          <label>Drug ID</label>
          <input value={form.drugId} onChange={(e) => setForm({ ...form, drugId: e.target.value })} />
        </div>
        <button className="btn btn-primary" onClick={() => recordScan.mutate({ patientId: Number(form.patientId), drugId: Number(form.drugId) })}>
          Record Scan
        </button>
      </div>
      <div className="card">
        <table className="table">
          <thead><tr><th>Patient</th><th>Drug</th><th>Scanned At</th><th>Status</th></tr></thead>
          <tbody>
            {scans.map((s) => <tr key={s.id}><td>{s.patientName}</td><td>{s.drugName}</td><td>{s.scannedAt}</td><td>{s.status}</td></tr>)}
            {scans.length === 0 && <tr><td colSpan={4} className="muted">No scans.</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
