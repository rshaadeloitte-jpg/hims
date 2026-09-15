import { useBills } from "./queries";

export function BillingDashboard() {
  const { data: bills = [], isLoading } = useBills({ page: 0, size: 100 });

  if (isLoading) return <p>Loading…</p>;

  const totalBills = bills.length;
  const settled = bills.filter((b) => b.isSettled).length;
  const interim = bills.filter((b) => b.isInterim && !b.isSettled && !b.isCancelled).length;
  const cancelled = bills.filter((b) => b.isCancelled).length;
  const totalRevenue = bills
    .filter((b) => b.isSettled)
    .reduce((sum, b) => sum + (b.totalAmount ?? 0), 0);
  const totalOutstanding = bills
    .filter((b) => !b.isSettled && !b.isCancelled)
    .reduce((sum, b) => sum + (b.balanceSelf ?? 0) + (b.balanceNonSelf ?? 0), 0);

  return (
    <section className="page">
      <h1>Billing Dashboard</h1>
      <div className="card-grid">
        <div className="card"><label>Total Bills</label><strong>{totalBills}</strong></div>
        <div className="card"><label>Settled</label><strong>{settled}</strong></div>
        <div className="card"><label>Interim</label><strong>{interim}</strong></div>
        <div className="card"><label>Cancelled</label><strong>{cancelled}</strong></div>
        <div className="card"><label>Revenue (Settled)</label><strong>{totalRevenue.toFixed(2)}</strong></div>
        <div className="card"><label>Outstanding</label><strong>{totalOutstanding.toFixed(2)}</strong></div>
      </div>
    </section>
  );
}
