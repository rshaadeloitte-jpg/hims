import { useMemo, useState } from "react";
import { useReports } from "./queries";

const KIND_LABEL: Record<number, string> = { 1: "TABULAR", 2: "SUMMARY" };
const KIND_BADGE: Record<number, string> = { 1: "badge--success", 2: "badge--muted" };

/**
 * ReportCatalog — searchable report library.
 *
 * Lists all report definitions with a live search filter over code + title.
 * Each card shows kind badge, active status, and a "Run" action that opens
 * the ReportViewer for that report.
 */
export function ReportCatalog() {
  const { data: reports = [], isLoading, isError, error } = useReports(true);
  const [search, setSearch] = useState("");
  const [activeOnly, setActiveOnly] = useState(true);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return reports.filter((r) => {
      if (activeOnly && !r.isActive) return false;
      if (!term) return true;
      return (
        r.reportCode.toLowerCase().includes(term) ||
        r.title.toLowerCase().includes(term)
      );
    });
  }, [reports, search, activeOnly]);

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>Report Catalog</h1>
          <p className="muted">Browse and run available reports — M25 Reporting Module.</p>
        </div>
      </header>

      {/* ── Search + filter bar ──────────────────────────────────── */}
      <div className="card toolbar">
        <div className="toolbar-row">
          <div className="field field--search">
            <label htmlFor="catalog-search">Search</label>
            <input
              id="catalog-search"
              type="text"
              placeholder="Search by code or title…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="field field--check">
            <input
              id="active-only"
              type="checkbox"
              checked={activeOnly}
              onChange={(e) => setActiveOnly(e.target.checked)}
            />
            <label htmlFor="active-only">Active only</label>
          </div>
        </div>
      </div>

      {isError && (
        <div className="alert alert--error" role="alert">
          Failed to load reports: {error instanceof Error ? error.message : String(error)}
        </div>
      )}

      {/* ── Results ───────────────────────────────────────────────── */}
      {isLoading ? (
        <div className="loading-state">
          <span className="spinner" aria-hidden="true" />
          Loading reports…
        </div>
      ) : filtered.length === 0 ? (
        <p className="empty-state">No reports match your search.</p>
      ) : (
        <div className="catalog-grid">
          {filtered.map((r) => (
            <article key={r.reportId} className="card catalog-card">
              <div className="catalog-card__header">
                <span className="catalog-card__code">{r.reportCode}</span>
                <span className={`badge ${KIND_BADGE[r.kind] ?? "badge--muted"}`}>
                  {KIND_LABEL[r.kind] ?? r.kind}
                </span>
              </div>
              <h3 className="catalog-card__title">{r.title}</h3>
              <div className="catalog-card__meta">
                <span className={`badge ${r.isActive ? "badge--success" : "badge--muted"}`}>
                  {r.isActive ? "Active" : "Inactive"}
                </span>
              </div>
              <div className="catalog-card__actions">
                <a
                  href={`/reports/view?code=${encodeURIComponent(r.reportCode)}`}
                  className="btn btn-primary btn-sm"
                >
                  Run
                </a>
                <a
                  href={`/reports/executions?reportCode=${encodeURIComponent(r.reportCode)}`}
                  className="btn btn-secondary btn-sm"
                >
                  History
                </a>
              </div>
            </article>
          ))}
        </div>
      )}

      <p className="muted catalog-count">
        Showing {filtered.length} of {reports.length} reports.
      </p>
    </section>
  );
}
