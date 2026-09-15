import { useMemo, useState } from "react";
import { ApiError } from "../../api/client";
import {
  useGeographyList,
  useAshaWorkers,
  type GeographyRecord,
} from "./queries";

type ActiveFilter = "" | "true" | "false";

/**
 * GeographyMaster — population register screen for ASHA catchment areas.
 * Linear list pattern: geography records with state/block/village hierarchy,
 * population summary, and detail view with enumeration data.
 */
export function GeographyMaster() {
  const [stateFilter, setStateFilter] = useState("");
  const [districtFilter, setDistrictFilter] = useState("");
  const [blockFilter, setBlockFilter] = useState("");
  const [ashaFilter, setAshaFilter] = useState<number | undefined>(undefined);
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>("");
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState<string | undefined>(undefined);
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [selectedRecord, setSelectedRecord] = useState<GeographyRecord | null>(null);

  const filters: Record<string, unknown> = useMemo(
    () => ({
      ...(stateFilter ? { state: stateFilter } : {}),
      ...(districtFilter ? { district: districtFilter } : {}),
      ...(blockFilter ? { block: blockFilter } : {}),
      ...(ashaFilter ? { ashaId: ashaFilter } : {}),
      ...(activeFilter ? { isActive: activeFilter } : {}),
      ...(search ? { search } : {}),
      page: pageIndex,
      size: pageSize,
    }),
    [stateFilter, districtFilter, blockFilter, ashaFilter, activeFilter, search, pageIndex, pageSize],
  );

  const { data: records = [], isLoading, isError, error, isFetching } = useGeographyList(filters);
  const { data: workers = [] } = useAshaWorkers();

  // Summary totals
  const summary = useMemo(() => {
    const totalPop = records.reduce((sum, r) => sum + r.totalPopulation, 0);
    const pregnant = records.reduce((sum, r) => sum + r.pregnantWomen, 0);
    const children = records.reduce((sum, r) => sum + r.childrenUnder5, 0);
    const households = records.reduce((sum, r) => sum + r.households, 0);
    return { totalPop, pregnant, children, households, count: records.length };
  }, [records]);

  const hasMore = records.length >= pageSize;

  // Derive unique states/districts/blocks from current data for cascading filters
  const states = useMemo(() => [...new Set(records.map((r) => r.state))].sort(), [records]);
  const districts = useMemo(() => [...new Set(records.map((r) => r.district))].sort(), [records]);
  const blocks = useMemo(() => [...new Set(records.map((r) => r.block))].sort(), [records]);

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Geography & Population Register</h1>
          <p className="muted">ASHA catchment area population enumeration.</p>
        </div>
      </header>

      {/* Summary cards */}
      <section className="card-grid card-grid--4">
        <div className="stat-card">
          <span className="stat-card__label">Total Population</span>
          <span className="stat-card__value">{summary.totalPop.toLocaleString()}</span>
        </div>
        <div className="stat-card stat-card--warning">
          <span className="stat-card__label">Pregnant Women</span>
          <span className="stat-card__value">{summary.pregnant.toLocaleString()}</span>
        </div>
        <div className="stat-card stat-card--info">
          <span className="stat-card__label">Children &lt;5</span>
          <span className="stat-card__value">{summary.children.toLocaleString()}</span>
        </div>
        <div className="stat-card stat-card--success">
          <span className="stat-card__label">Households</span>
          <span className="stat-card__value">{summary.households.toLocaleString()}</span>
        </div>
      </section>

      {/* Filters */}
      <section className="card toolbar" aria-label="Filters">
        <div className="toolbar-row">
          <div className="field">
            <label htmlFor="state-filter">State</label>
            <select
              id="state-filter"
              value={stateFilter}
              onChange={(e) => { setStateFilter(e.target.value); setPageIndex(0); }}
            >
              <option value="">All States</option>
              {states.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>

          <div className="field">
            <label htmlFor="district-filter">District</label>
            <select
              id="district-filter"
              value={districtFilter}
              onChange={(e) => { setDistrictFilter(e.target.value); setPageIndex(0); }}
            >
              <option value="">All Districts</option>
              {districts.map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>

          <div className="field">
            <label htmlFor="block-filter">Block</label>
            <select
              id="block-filter"
              value={blockFilter}
              onChange={(e) => { setBlockFilter(e.target.value); setPageIndex(0); }}
            >
              <option value="">All Blocks</option>
              {blocks.map((b) => <option key={b} value={b}>{b}</option>)}
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

          <div className="field">
            <label htmlFor="active-filter">Status</label>
            <select
              id="active-filter"
              value={activeFilter}
              onChange={(e) => { setActiveFilter(e.target.value as ActiveFilter); setPageIndex(0); }}
            >
              <option value="">All</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
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
                placeholder="Village, sub-center..."
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
            <span className="spinner" aria-hidden="true" /> Loading geography records…
          </div>
        ) : records.length === 0 ? (
          <p className="empty-state">No geography records found.</p>
        ) : (
          <>
            {isFetching ? <p className="refreshing" role="status">Refreshing…</p> : null}
            <table className="data-table" aria-label="Geography records">
              <thead>
                <tr>
                  <th scope="col">Village</th>
                  <th scope="col">Block</th>
                  <th scope="col">ASHA</th>
                  <th scope="col" className="cell--num">Population</th>
                  <th scope="col" className="cell--num">Households</th>
                  <th scope="col" className="cell--num">Pregnant</th>
                  <th scope="col" className="cell--num">Children</th>
                  <th scope="col">Status</th>
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
                      <div className="list__item-title">{record.village}</div>
                      <div className="list__item-subtitle">{record.subCenter} · {record.district}, {record.state}</div>
                    </td>
                    <td>{record.block}</td>
                    <td>{record.ashaName}</td>
                    <td className="cell--num">{record.totalPopulation.toLocaleString()}</td>
                    <td className="cell--num">{record.households.toLocaleString()}</td>
                    <td className="cell--num">{record.pregnantWomen.toLocaleString()}</td>
                    <td className="cell--num">{record.childrenUnder5.toLocaleString()}</td>
                    <td>
                      <span className={`badge ${record.isActive ? "badge--success" : "badge--muted"}`}>
                        {record.isActive ? "Active" : "Inactive"}
                      </span>
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
          <aside className="drawer" onClick={(e) => e.stopPropagation()} aria-label="Geography details">
            <header className="drawer__header">
              <h2>{selectedRecord.village}</h2>
              <button className="btn btn--icon" onClick={() => setSelectedRecord(null)} aria-label="Close">×</button>
            </header>
            <div className="drawer__body">
              <dl className="detail-grid">
                <dt>ASHA Worker</dt>
                <dd>{selectedRecord.ashaName}</dd>
                <dt>Sub-center</dt>
                <dd>{selectedRecord.subCenter}</dd>
                <dt>Block</dt>
                <dd>{selectedRecord.block}</dd>
                <dt>District</dt>
                <dd>{selectedRecord.district}</dd>
                <dt>State</dt>
                <dd>{selectedRecord.state}</dd>
                <dt>Status</dt>
                <dd><span className={`badge ${selectedRecord.isActive ? "badge--success" : "badge--muted"}`}>{selectedRecord.isActive ? "Active" : "Inactive"}</span></dd>
              </dl>
              <h3>Population</h3>
              <table className="data-table data-table--compact">
                <tbody>
                  <tr><td>Total Population</td><td className="cell--num">{selectedRecord.totalPopulation.toLocaleString()}</td></tr>
                  <tr><td>Households</td><td className="cell--num">{selectedRecord.households.toLocaleString()}</td></tr>
                  <tr><td>Target Couples</td><td className="cell--num">{selectedRecord.targetCouples.toLocaleString()}</td></tr>
                  <tr><td>Eligible Couples</td><td className="cell--num">{selectedRecord.eligibleCouples.toLocaleString()}</td></tr>
                  <tr><td>Pregnant Women</td><td className="cell--num">{selectedRecord.pregnantWomen.toLocaleString()}</td></tr>
                  <tr><td>Children Under 5</td><td className="cell--num">{selectedRecord.childrenUnder5.toLocaleString()}</td></tr>
                </tbody>
              </table>
              {selectedRecord.lastEnumerationDate && (
                <>
                  <h3>Last Enumeration</h3>
                  <p>{new Date(selectedRecord.lastEnumerationDate).toLocaleDateString()}</p>
                </>
              )}
              {selectedRecord.remarks && (
                <>
                  <h3>Remarks</h3>
                  <p>{selectedRecord.remarks}</p>
                </>
              )}
            </div>
          </aside>
        </div>
      ) : null}
    </div>
  );
}
