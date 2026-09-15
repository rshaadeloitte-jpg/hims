import { Link } from "react-router-dom";
import { useItemSales, useSaleReturns, useInterventions } from "./queries";

export function PharmacyDashboard() {
  const { data: sales = [], isLoading: salesLoading } = useItemSales();
  const { data: returns = [] } = useSaleReturns();
  const { data: interventions = [] } = useInterventions();

  if (salesLoading) return <p>Loading…</p>;

  const totalSales = sales.length;
  const dispensed = sales.filter((s) => s.isDispensed).length;
  const pending = sales.filter((s) => !s.isDispensed && !s.isReturned).length;
  const activeReturns = returns.filter((r) => r.status === 1).length;

  return (
    <section className="page">
      <header className="page__header">
        <h1>Pharmacy</h1>
        <Link to="/pharmacy/new" className="btn btn--primary">+ New Sale</Link>
      </header>
      <div className="card-grid">
        <div className="card"><label>Total Sales</label><strong>{totalSales}</strong></div>
        <div className="card"><label>Dispensed</label><strong>{dispensed}</strong></div>
        <div className="card"><label>Pending</label><strong>{pending}</strong></div>
        <div className="card"><label>Active Returns</label><strong>{activeReturns}</strong></div>
        <div className="card"><label>Interventions</label><strong>{interventions.length}</strong></div>
      </div>
      <h2>Recent Sales</h2>
      <table className="table">
        <thead><tr><th>#</th><th>Patient</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead>
        <tbody>
          {sales.slice(0, 20).map((s) => (
            <tr key={s.saleId}>
              <td>{s.saleId}</td>
              <td>{s.patientId}</td>
              <td>{s.totalAmount?.toFixed(2)}</td>
              <td>{s.isDispensed ? "Dispensed" : s.isReturned ? "Returned" : "Pending"}</td>
              <td>{s.createdAt?.slice(0, 10)}</td>
            </tr>
          ))}
          {sales.length === 0 && <tr><td colSpan={5} className="muted">No sales.</td></tr>}
        </tbody>
      </table>
    </section>
  );
}