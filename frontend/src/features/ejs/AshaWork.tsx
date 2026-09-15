import { useMemo, useState } from "react";
import { ApiError } from "../../api/client";
import {
  useAshaWorkList,
  useAshaWorkers,
  type AshaWorkRecord,
} from "./queries";
import "./zodSchema";

type TrackingTypeFilter = "" | "ANC" | "PNC" | "CHILD";
type StatusFilter = "" | "SCHEDULED" | "COMPLETED" | "MISSED" | "RESCHEDULED";

/**
 * AshaWork — ANC/PNC/Child-care tracking list screen.
 * Linear list pattern: card-based list with type/status tabs, search,
 * and detail drawer for visit records.
 */
export function AshaWork() {
  const [tab, setTab] = useState<TrackingTypeFilter>("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("");
  const [ashaFilter, setAshaFilter] = useState<number | undefined>(undefined);
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState<string | undefined>(undefined);
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [selectedRecord, setSelectedRecord] = useState<AshaWorkRecord | null>(null);

  const filters: Record<string, unknown> = useMemo(
    () => ({
      ...(tab ? { trackingType: tab } : {}),
      ...(statusFilter ? { status: statusFilter } : {}),
      ...(ashaFilter ? { ashaId: ashaFilter } : {}),
      ...(search ? { search } : {}),
      page: pageIndex,
      size: pageSize,
    }),
    [tab, statusFilter, ashaFilter, search, pageIndex, pageSize],
  );

  const { data: records = [], isLoading, isError, error, isFetching } = useAshaWorkList(filters);
  const { data: workers = [] } = useAshaWorkers();

  const hasMore = records.length >= pageSize;

  const trackingTabs: { value: TrackingTypeFilter; label: string }[] = [
    { value: "", label: "All" },
    { value: "ANC", label: "ANC" },
    { value: "PNC", label: "PNC" },
    { value: "CHILD", label: "Child Care" },
  ];

  const statusBadge = (status: string) => {
    const map: Record<string, string> = {
      SCHEDULED: "badge--info",
      COMPLETED: "badge--success",
      MISSED: "badge--danger",
      RESCHEDULED: "badge--warning",
    };
    return `badge ${map[status] ?? "badge--muted"}`;
  };

  const trackingBadge = (type: string) => {
    const map: Record<string, string> = {
      ANC: "badge--primary",
      PNC: "badge--secondary",
      CHILD: "badge--info",
    };
    return `badge ${map[type] ?? "badge--muted"}`;
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>ASHA Work Tracking</h1>
          <p className="muted">ANC, PNC, and child-care visit records for ASHA workers.</p>
        </div>
      </header>

      {/* Tracking type tabs */}
      <section className="card toolbar" aria-label="Filters">
        <div className="toolbar-row">
          <div className="field">
            <label>Tracking Type</label>
            <div className="tab-group">
              {trackingTabs.map((t) => (
                <button
                  key={t.value}
                  className={`tab ${tab === t.value ? "tab--active" : ""}`}
                  onClick={() => { setTab(t.value); setPageIndex(0); }}
                >
                  {t.label}
                </button>
              ))}
            </div>
          </div>

          <div className="field">
            <label htmlFor="status-filter">Status</label>
            <select
              id="status-filter"
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value as StatusFilter); setPageIndex(0); }}
            >
              <option value="">All Statuses</option>
              <option value="SCHEDULED">Scheduled</option>
              <option value="COMPLETED">Completed</option>
              <option value="MISSED">Missed</option>
              <option value="RESCHEDULED">Rescheduled</option>
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

          <form
            className="field field--search"
            onSubmit={(e) => { e.preventDefault(); setSearch(searchInput.trim() || undefined); setPageIndex(0); }}
          >
            <label htmlFor="search-input">Search</label>
            <div className="search-row">
              <input
                id="search-input"
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Beneficiary name..."
              />
              <button type="submit" className="btn btn-secondary">Apply</button>
            </div>
          </form>
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
            <span className="spinner" aria-hidden="true" /> Loading records…
          </div>
        ) : records.length === 0 ? (
          <p className="empty-state">No visit records found.</p>
        ) : (
          <>
            {isFetching ? <p className="refreshing" role="status">Refreshing…</p> : null}
            <ul className="list list--linear" aria-label="ASHA work records">
              {records.map((record) => (
                <li
                  key={record.id}
                  className="list__item list__item--clickable"
                  onClick={() => setSelectedRecord(record)}
                >
                  <div className="list__item-main">
                    <div className="list__item-title">
                      {record.beneficiaryName}
                      <span className={trackingBadge(record.trackingType)}>{record.trackingType}</span>
                    </div>
                    <div className="list__item-subtitle">
                      ASHA: {record.ashaName} · Visit: {new Date(record.visitDate).toLocaleDateString()}
                      {record.nextVisitDate ? ` · Next: ${new Date(record.nextVisitDate).toLocaleDateString()}` : ""}
                    </div>
                  </div>
                  <div className="list__item-side">
                    <span className={statusBadge(record.status)}>{record.status}</span>
                  </div>
                </li>
              ))}
            </ul>
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
          <aside className="drawer" onClick={(e) => e.stopPropagation()} aria-label="Record details">
            <header className="drawer__header">
              <h2>{selectedRecord.beneficiaryName}</h2>
              <button className="btn btn--icon" onClick={() => setSelectedRecord(null)} aria-label="Close">×</button>
            </header>
            <div className="drawer__body">
              <dl className="detail-grid">
                <dt>Tracking Type</dt>
                <dd><span className={trackingBadge(selectedRecord.trackingType)}>{selectedRecord.trackingType}</span></dd>
                <dt>Status</dt>
                <dd><span className={statusBadge(selectedRecord.status)}>{selectedRecord.status}</span></dd>
                <dt>Visit Date</dt>
                <dd>{new Date(selectedRecord.visitDate).toLocaleDateString()}</dd>
                <dt>ASHA Worker</dt>
                <dd>{selectedRecord.ashaName}</dd>
                {selectedRecord.trackingType === "ANC" && (
                  <>
                    {selectedRecord.gestationalWeek != null && <><dt>Gestational Week</dt><dd>{selectedRecord.gestationalWeek}</dd></>}
                    {selectedRecord.weight != null && <><dt>Weight</dt><dd>{selectedRecord.weight} kg</dd></>}
                    {selectedRecord.bloodPressureSystolic != null && <><dt>BP</dt><dd>{selectedRecord.bloodPressureSystolic}/{selectedRecord.bloodPressureDiastolic}</dd></>}
                    {selectedRecord.hemoglobin != null && <><dt>Hemoglobin</dt><dd>{selectedRecord.hemoglobin} g/dL</dd></>}
                    {selectedRecord.immunizationGiven && <><dt>Immunization</dt><dd>{selectedRecord.immunizationGiven}</dd></>}
                  </>
                )}
                {selectedRecord.trackingType === "PNC" && (
                  <>
                    {selectedRecord.deliveryDate && <><dt>Delivery Date</dt><dd>{new Date(selectedRecord.deliveryDate).toLocaleDateString()}</dd></>}
                    {selectedRecord.deliveryType && <><dt>Delivery Type</dt><dd>{selectedRecord.deliveryType}</dd></>}
                    {selectedRecord.postnatalDay != null && <><dt>Postnatal Day</dt><dd>{selectedRecord.postnatalDay}</dd></>}
                  </>
                )}
                {selectedRecord.trackingType === "CHILD" && (
                  <>
                    {selectedRecord.childAgeMonths != null && <><dt>Age</dt><dd>{selectedRecord.childAgeMonths} months</dd></>}
                    {selectedRecord.weightChild != null && <><dt>Weight</dt><dd>{selectedRecord.weightChild} kg</dd></>}
                    {selectedRecord.heightChild != null && <><dt>Height</dt><dd>{selectedRecord.heightChild} cm</dd></>}
                    {selectedRecord.immunizationStatus && <><dt>Immunization</dt><dd>{selectedRecord.immunizationStatus}</dd></>}
                    {selectedRecord.nutritionStatus && <><dt>Nutrition</dt><dd>{selectedRecord.nutritionStatus}</dd></>}
                  </>
                )}
                {selectedRecord.nextVisitDate && <><dt>Next Visit</dt><dd>{new Date(selectedRecord.nextVisitDate).toLocaleDateString()}</dd></>}
                {selectedRecord.remarks && <><dt>Remarks</dt><dd>{selectedRecord.remarks}</dd></>}
              </dl>
            </div>
          </aside>
        </div>
      ) : null}
    </div>
  );
}
