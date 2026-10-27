import { useBills } from "./queries";
import { Text } from "@chakra-ui/react";

export function BillingDashboard() {
  const { data: bills = [], isLoading } = useBills({ page: 0, size: 100 });

  if (isLoading) return <div className="loading-state" role="status"><span className="spinner" aria-hidden="true" /> Loading…</div>;

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
        <div className="card"><Text fontSize="sm" fontWeight="medium">Total Bills</Text><strong>{totalBills}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Settled</Text><strong>{settled}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Interim</Text><strong>{interim}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Cancelled</Text><strong>{cancelled}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Revenue (Settled)</Text><strong>{totalRevenue.toFixed(2)}</strong></div>
        <div className="card"><Text fontSize="sm" fontWeight="medium">Outstanding</Text><strong>{totalOutstanding.toFixed(2)}</strong></div>
      </div>
    </section>
  );
}
