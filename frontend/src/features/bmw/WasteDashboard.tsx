import { useMemo, useState } from "react";
import { useWasteList, useWasteSummary, type WasteRequestRow } from "./queries";
import { HandoverView } from "./HandoverView";
import { WasteRequest } from "./WasteRequest";
import {
  STATUS_LABEL,
  STATUS_TONE,
  wasteListFilterSchema,
  type WasteListFilterValues,
} from "./zodSchema";

/**
 * WasteDashboard — the BMW landing screen. Mirrors the legacy FAUG
 * showBioMedicalWasteStatus workflow:
 *   1. Summary strip (pending / accepted / handed-over / received / completed).
 *   2. Date-range + category filter bar.
 *   3. Paginated table of waste requests with Stripe status badges.
 *   4. Per-row actions gated by request status (Accept / Hand Over / Receive /
 *      Decompose).
 *
 * The "Create Request" button opens the WasteRequest form; clicking a row opens
 * the HandoverView for that request.
 */

const PAGE_SIZE = 25;

const CATEGORY_FILTERS = [
  { key: "", label: "All" },
  { key: "pending", label: "Pending" },
  { key: "accepted", label: "Accepted" },
  { key: "handovered", label: "Handed Over" },
  { key: "received", label: "Received" },
  { key: "completed", label: "Completed" },
];

const todayISO = () => new Date().toISOString().slice(0, 10);
const thirtyDaysAgo = () => {
  const d = new Date();
  d.setDate(d.getDate() - 30);
  return d.toISOString().slice(0, 10);
};

interface WasteDashboardProps {
  unitId: number;
  storeId: number;
  isBioMedicalStore?: boolean;
}

export function WasteDashboard({
  unitId,
  storeId,
  isBioMedicalStore = false,
}: WasteDashboardProps) {
  const [fromDate, setFromDate] = useState(thirtyDaysAgo());
  const [toDate, setToDate] = useState(todayISO());
  const [category, setCategory] = useState("");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const filter: WasteListFilterValues = useMemo(
    () =>
      wasteListFilterSchema.parse({
        fromDate,
        toDate,
        category: category || undefined,
        storeId,
        search: search || undefined,
        page,
        size: PAGE_SIZE,
      }),
    [fromDate, toDate, category, storeId, search, page],
  );

  const { data: summary } = useWasteSummary(unitId, storeId, fromDate, toDate);
  const { data: list, isLoading } = useWasteList(filter);

  const rows = list?.items ?? [];

  /** Maps the legacy numeric requestStatus to a WasteRequestStatus enum. */
  const statusFromNum = (n: number) => {
    if (n === 1) return "PENDING" as const;
    if (n === 2) return "ACCEPTED" as const;
    if (n === 3) return "HANDOVERED" as const;
    if (n === 4) return "RECEIVED" as const;
    if (n === 5) return "COMPLETED" as const;
    return "PENDING" as const;
  };

  // Summary counts
  const counts = summary ?? {
    totalCount: 0,
    pendingCount: 0,
    acceptedCount: 0,
    handOveredCount: 0,
    receivedCount: 0,
    completedCount: 0,
  };

  const summaryCards = [
    { label: "Total", value: counts.totalCount, tone: "badge--muted" },
    { label: "Pending", value: counts.pendingCount, tone: "badge--warning" },
    { label: "Accepted", value: counts.acceptedCount, tone: "badge--info" },
    { label: "Handed Over", value: counts.handOveredCount, tone: "badge--info" },
    { label: "Received", value: counts.receivedCount, tone: "badge--success" },
    { label: "Completed", value: counts.completedCount, tone: "badge--success" },
  ];

  const handleRowClick = (row: WasteRequestRow) => {
    // View details if the request is in a late stage
    if (row.requestStatus >= 2) {
      setSelectedId(row.id);
    }
  };

  if (isCreating) {
    return (
      <WasteRequest
        unitId={unitId}
        defaultStoreId={storeId}
        onCancel={() => setIsCreating(false)}
        onSuccess={() => setIsCreating(false)}
      />
    );
  }

  if (selectedId !== null) {
    return (
      <HandoverView
        requestId={selectedId}
        unitId={unitId}
        onClose={() => setSelectedId(null)}
      />
    );
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Biomedical Waste Management</h1>
          <p className="muted">Track, hand over, and dispose of biomedical waste bags.</p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => setIsCreating(true)}
        >
          Create Waste Request
        </button>
      </header>

      {/* Summary strip — Stripe-style status badges */}
      <div className="card-grid">
        {summaryCards.map((c) => (
          <div key={c.label} className="card">
            <span className={`badge ${c.tone}`}>{c.label}</span>
            <strong>{c.value}</strong>
          </div>
        ))}
      </div>

      {/* Filter bar */}
      <div className="card toolbar">
        <div className="toolbar-row">
          <div className="field">
            <label htmlFor="fromDate">From</label>
            <input
              id="fromDate"
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="toDate">To</label>
            <input
              id="toDate"
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
          </div>
          <div className="field field--search">
            <label htmlFor="search">Search</label>
            <input
              id="search"
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Requisition # or barcode…"
            />
          </div>
        </div>
        <nav className="filter-strip" aria-label="Filter by status">
          {CATEGORY_FILTERS.map((f) => {
            const active = category === f.key;
            return (
              <button
                key={f.key}
                type="button"
                className={`filter-pill${active ? " filter-pill--active" : ""}`}
                aria-pressed={active}
                onClick={() => {
                  setCategory(f.key);
                  setPage(0);
                }}
              >
                {f.label}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Request table */}
      <div className="card">
        {isLoading ? (
          <div className="loading-state">
            <span className="spinner" aria-hidden="true" />
            Loading waste requests…
          </div>
        ) : (
          <>
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Requisition</th>
                  <th>From Store</th>
                  <th>To Store</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => {
                  const status = statusFromNum(r.requestStatus);
                  return (
                    <tr key={r.id}>
                      <td>{r.id}</td>
                      <td>
                        <button
                          type="button"
                          className="link-button"
                          onClick={() => handleRowClick(r)}
                        >
                          {r.requisitionNo ?? `REQ-${r.id}`}
                        </button>
                      </td>
                      <td>{r.fromStore}</td>
                      <td>{r.toStore}</td>
                      <td>{r.date}</td>
                      <td>
                        <span className={`badge ${STATUS_TONE[status]}`}>
                          {STATUS_LABEL[status]}
                        </span>
                      </td>
                      <td>
                        {status === "PENDING" && isBioMedicalStore && (
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setSelectedId(r.id)}
                          >
                            Accept
                          </button>
                        )}
                        {status === "ACCEPTED" && !isBioMedicalStore && (
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setSelectedId(r.id)}
                          >
                            Hand Over
                          </button>
                        )}
                        {status === "HANDOVERED" && isBioMedicalStore && (
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setSelectedId(r.id)}
                          >
                            Receive
                          </button>
                        )}
                        {status === "RECEIVED" && isBioMedicalStore && (
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => setSelectedId(r.id)}
                          >
                            Decompose
                          </button>
                        )}
                        {status === "COMPLETED" && (
                          <span className="muted">Done</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={7} className="muted">
                      No waste requests match the current filter.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>

            <div className="pagination">
              <span className="pagination-info">
                Page {page + 1} · {rows.length} rows
              </span>
              <div className="pagination-controls">
                <button
                  className="btn btn-secondary btn-sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  ← Prev
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  disabled={rows.length < PAGE_SIZE}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next →
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  );
}
