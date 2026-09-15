import { useMemo, useState } from "react";
import { ApiError } from "../../api/client";
import { useIncentiveList, useAshaWorkers, type IncentiveRecord } from "./queries";


type StatusFilter = "" | "PENDING" | "APPROVED" | "PAID" | "REJECTED";

/**
 * IncentiveDashboard — incentive calculation list screen.
 * Linear list pattern: month-wise incentive records per ASHA worker with
 * status badges, summary cards, and approve/pay actions.
 */
export function IncentiveDashboard() {
  const [monthFilter, setMonthFilter] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("");
  const [ashaFilter, setAshaFilter] = useState<number | undefined>(undefined);
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [selectedRecord, setSelectedRecord] = useState<IncentiveRecord | null>(null);

  const filters: Record<string, unknown> = useMemo(
    () => ({
      ...(monthFilter ? { month: monthFilter } : {}),
      ...(statusFilter ? { status: statusFilter } : {}),
      ...(ashaFilter ? { ashaId: ashaFilter } : {}),
      page: pageIndex,
      size: pageSize,
    }),
    [monthFilter, statusFilter, ashaFilter, pageIndex, pageSize],
  );

  const { data: records = [], isLoading, isError, error, isFetching } = useIncentiveList(filters);
  const { data: workers = [] } = useAshaWorkers();

  // Compute summary totals
  const summary = useMemo(() => {
    const total = records.reduce((sum, r) => sum + r.totalIncentive, 0);
    const pending = records.filter((r) => r.status === "PENDING").reduce((sum, r) => sum + r.totalIncentive, 0);
    const approved = records.filter((r) => r.status === "APPROVED").reduce((sum, r) => sum + r.totalIncentive, 0);
    const paid = records.filter((r) => r.status === "PAID").reduce((sum, r) => sum + r.totalIncentive, 0);
    return { total, pending, approved, paid, count: records.length };
  }, [records]);

  const hasMore = records.length >= pageSize;

  const statusBadge = (status: string) => {
    const map: Record<string, string> = {
      PENDING: "badge--warning",
      APPROVED: "badge--info",
      PAID: "badge--success",
      REJECTED: "badge--danger",
    };
    return `badge ${map[status] ?? "badge--muted"}`;
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Incentive Dashboard</h1>
          <p className="muted">ASHA worker incentive calculations and disbursements.</p>
        </div>
      </header>

      {/* Summary cards */}
      <section className="card-grid card-grid--4">
        <div className="stat-card">
          <span className="stat-card__label">Total</span>
          <span className="stat-card__value">₹{summary.total.toLocaleString()}</span>
        </div>
        <div className="stat-card stat-card--warning">
          <span className="stat-card__label">Pending</span>
          <span className="stat-card__value">₹{summary.pending.toLocaleString()}</span>
        </div>
        <div className="stat-card stat-card--info">
          <span className="stat-card__label">Approved</span>
          <span className="stat-card__value">₹{summary.approved.toLocaleString()}</span>
        </div>
        <div className="stat-card stat-card--success">
          <span className="stat-card__label">Paid</span>
          <span className="stat-card__value">₹{summary.paid.toLocaleString()}</span>
        </div>
      </section>

      {/* Filters */}
      <section className="card toolbar" aria-label="Filters">
        <div className="toolbar-row">
          <div className="field">
            <label htmlFor="month-filter">Month</label>
            <input
              id="month-filter"
              type="month"
              value={monthFilter}
              onChange={(e) => { setMonthFilter(e.target.value); setPageIndex(0); }}
            />
          </div>

          <div className="field">
            <label htmlFor="status-filter">Status</label>
            <select
              id="status-filter"
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value as StatusFilter); setPageIndex(0); }}
            >
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="PAID">Paid</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          <div className="field">
            <label htmlFor="asha-filter">ASHA Worker</label>
            <select
              id="asha-filter"
              value={ashaFilter ?? ""}
              onChange={(e) => { setAshaFilter(e.target.value ? Number(e.target.value) : undefined); setPageIndex(0); }}
            >
              <option value="">All Workers</option>
              {workers.map((w) => (
                <option key={w.ashaId} value={w.ashaId}>{w.ashaName}</option>
              ))}
            </select>
          </div>
        </div>
      </section>

      {isError ? (
        <div className="alert alert--error" role="alert">
          <strong>{error instanceof ApiError ? `${error.code} (HTTP ${error.status})` : "Error"}</strong>
          : {error instanceof ApiError ? error.message : String(error.message ?? error)}
        </div>
      ) : null}

      <section className="card">
        {isLoading ? (
          <div className="loading-state" role="status">
            <span className="spinner" aria-hidden="true" /> Loading incentives…
          </div>
        ) : records.length === 0 ? (
          <p className="empty-state">No incentive records found.</p>
        ) : (
          <>
            {isFetching ? <p className="refreshing" role="status">Refreshing…</p> : null}
            <table className="data-table" aria-label="Incentive records">
              <thead>
                <tr>
                  <th scope="col">ASHA Worker</th>
                  <th scope="col">Month</th>
                  <th scope="col" className="cell--num">ANC</th>
                  <th scope="col" className="cell--num">PNC</th>
                  <th scope="col" className="cell--num">Child</th>
                  <th scope="col" className="cell--num">Total</th>
                  <th scope="col">Status</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {records.map((record) => (
                  <tr
                    key={record.id}
                    className="row-clickable"
                    onClick={() => setSelectedRecord(record)}
                  >
                    <td>
                      <div className="list__item-title">{record.ashaName}</div>
                    </td>
                    <td>{record.month}</td>
                    <td className="cell--num">{record.ancIncentive.toLocaleString()}</td>
                    <td className="cell--num">{record.pncIncentive.toLocaleString()}</td>
                    <td className="cell--num">{record.childCareIncentive.toLocaleString()}</td>
                    <td className="cell--num"><strong>₹{record.totalIncentive.toLocaleString()}</strong></td>
                    <td>
                      <span className={statusBadge(record.status)}>{record.status}</span>
                    </td>
                    <td>
                      <div className="btn-group">
                        {record.status === "PENDING" ? (
                          <button className="btn btn--small btn--success" onClick={(e) => e.stopPropagation()}>Approve</button>
                        ) : null}
                        {record.status === "APPROVED" ? (
                          <button className="btn btn--small btn--primary" onClick={(e) => e.stopPropagation()}>Pay</button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}

        {records.length > 0 ? (
          <div className="pagination">
            <span className="pagination-info">
              Showing {records.length} record{records.length === 1 ? "" : "s"} on page {pageIndex + 1}
            </span>
            <div className="pagination-controls">
              <label className="pagination-size">
                Rows
                <select
                  value={pageSize}
                  onChange={(e) => { setPageSize(Number(e.target.value)); setPageIndex(0); }}
                >
                  {[10, 20, 50].map((n) => <option key={n} value={n}>{n}</option>)}
                </select>
              </label>
              <button type="button" className="btn btn-secondary btn-sm" disabled={pageIndex === 0} onClick={() => setPageIndex((p) => Math.max(0, p - 1))}>← Prev</button>
              <span className="pagination-page">Page {pageIndex + 1}</span>
              <button type="button" className="btn btn-secondary btn-sm" disabled={!hasMore} onClick={() => setPageIndex((p) => p + 1)}>Next →</button>
            </div>
          </div>
        ) : null}
      </section>

      {/* Detail drawer */}
      {selectedRecord ? (
        <div className="drawer-overlay" onClick={() => setSelectedRecord(null)}>
          <aside className="drawer" onClick={(e) => e.stopPropagation()} aria-label="Incentive details">
            <header className="drawer__header">
              <h2>{selectedRecord.ashaName} — {selectedRecord.month}</h2>
              <button className="btn btn--icon" onClick={() => setSelectedRecord(null)} aria-label="Close">×</button>
            </header>
            <div className="drawer__body">
              <dl className="detail-grid">
                <dt>Status</dt>
                <dd><span className={statusBadge(selectedRecord.status)}>{selectedRecord.status}</span></dd>
                <dt>Total Incentive</dt>
                <dd><strong>₹{selectedRecord.totalIncentive.toLocaleString()}</strong></dd>
              </dl>
              <h3>Breakdown</h3>
              <table className="data-table data-table--compact">
                <thead><tr><th>Activity</th><th className="cell--num">Count</th><th className="cell--num">Amount</th></tr></thead>
                <tbody>
                  <tr><td>ANC Visits</td><td className="cell--num">{selectedRecord.ancVisits}</td><td className="cell--num">₹{selectedRecord.ancIncentive.toLocaleString()}</td></tr>
                  <tr><td>PNC Visits</td><td className="cell--num">{selectedRecord.pncVisits}</td><td className="cell--num">₹{selectedRecord.pncIncentive.toLocaleString()}</td></tr>
                  <tr><td>Child Care</td><td className="cell--num">{selectedRecord.childCareVisits}</td><td className="cell--num">₹{selectedRecord.childCareIncentive.toLocaleString()}</td></tr>
                  <tr><td>Immunization</td><td className="cell--num">{selectedRecord.immunizationReferrals}</td><td className="cell--num">₹{selectedRecord.immunizationIncentive.toLocaleString()}</td></tr>
                  <tr><td>Institutional Delivery</td><td className="cell--num">{selectedRecord.institutionalDeliveryReferrals}</td><td className="cell--num">₹{selectedRecord.deliveryIncentive.toLocaleString()}</td></tr>
                </tbody>
              </table>
            </div>
          </aside>
        </div>
      ) : null}
    </div>
  );
}
