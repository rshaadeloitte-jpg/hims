import { useState } from "react";
import { useDashboardCounts } from "./queries";

export function ReportDashboardPage() {
  const [asOf, setAsOf] = useState("");
  const { data: counts, isLoading } = useDashboardCounts(asOf || undefined);

  return (
    <section className="page">
      <header className="page__header">
        <h1>Hospital Dashboard (M25)</h1>
      </header>

      <div className="row">
        <label>
          As of date
          <input type="date" value={asOf} onChange={(e) => setAsOf(e.target.value)} />
        </label>
      </div>

      {isLoading ? <p>Loading…</p> : !counts ? <p className="muted">No data.</p> : (
        <div className="card-grid">
          <div className="card"><label>Patients Registered</label><strong>{counts.patients?.toLocaleString() ?? "—"}</strong></div>
          <div className="card"><label>Encounters</label><strong>{counts.encounters?.toLocaleString() ?? "—"}</strong></div>
          <div className="card"><label>Revenue (₹)</label><strong>{counts.revenueTotal?.toLocaleString("en-IN", { minimumFractionDigits: 2 }) ?? "—"}</strong></div>
        </div>
      )}
    </section>
  );
}