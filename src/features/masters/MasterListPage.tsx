import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../../api/client";
import { deriveMetaFromRecord, getEntityMeta, isKnownEntity } from "./entityMeta";
import { MasterTable } from "./components/MasterTable";
import { useMasterEntities, useMasterList, type MasterListState } from "./queries";

/**
 * MasterListPage — the schema-driven list screen.
 *
 * Entity selector, grid columns, search fields and cell rendering are all
 * derived from entityMeta (the mini FAUG-schema adapter). Records come from
 * the legacy-shaped reference-service endpoints:
 *   - unit-scoped  GET /api/v1/masters/{entity}   (unitId input)
 *   - unscoped     GET /api/v1/masters/{entity}/all (unitId blank)
 * Pagination uses the legacy offset semantics: wire page = pageIndex * pageSize
 * so server offset = (page / count) * count = pageIndex * pageSize.
 */
export function MasterListPage() {
  const params = useParams<{ entity?: string }>();
  const navigate = useNavigate();

  const entitiesQuery = useMasterEntities();
  const [entity, setEntity] = useState<string>(params.entity ?? "");
  const [unitIdInput, setUnitIdInput] = useState<string>("1");
  const [searchInput, setSearchInput] = useState<string>("");
  const [search, setSearch] = useState<string | undefined>(undefined);
  const [pageIndex, setPageIndex] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  // Default the entity selector to the first registry entry once loaded.
  useEffect(() => {
    if (!entity && entitiesQuery.data && entitiesQuery.data.length > 0) {
      setEntity(entitiesQuery.data[0]);
    }
  }, [entity, entitiesQuery.data]);

  const unitId =
    unitIdInput.trim() === "" ? undefined : Number.isNaN(Number(unitIdInput)) ? undefined : Number(unitIdInput);

  const listState: MasterListState = useMemo(
    () => ({ entity, unitId, search, searchFld: undefined, pageIndex, pageSize }),
    [entity, unitId, search, pageIndex, pageSize],
  );

  const baseMeta = entity ? getEntityMeta(entity) : null;
  const rowsQuery = useMasterList(listState, baseMeta);
  const rows = rowsQuery.data ?? [];

  // For registry entities the metadata comes from entityMeta.ts; for any future
  // (unknown) entity we derive a generic field model from the first record —
  // the engine stays schema-driven without hand-written per-entity code.
  // Never null: falls back to an empty model until an entity is selected.
  const meta = useMemo(() => {
    if (!baseMeta) {
      return { name: entity, label: entity, idField: "id", fields: [] };
    }
    if (isKnownEntity(entity)) return baseMeta;
    return rows[0] ? deriveMetaFromRecord(entity, rows[0]) : baseMeta;
  }, [entity, rows, baseMeta]);

  const hasMore = rows.length > 0 && rows.length >= pageSize;
  const error = rowsQuery.error;

  const changeEntity = (next: string) => {
    setEntity(next);
    setPageIndex(0);
    setSearch(undefined);
    setSearchInput("");
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Master Data</h1>
          <p className="muted">Schema-driven management of reference master entities.</p>
        </div>
        {entity ? (
          <button
            className="btn btn-primary"
            onClick={() => {
              // Carry the active unit scope so create prefills unitId (F-6).
              const scope = listState.unitId != null ? `?unitId=${listState.unitId}` : "";
              navigate(`/masters/${entity}/new${scope}`);
            }}
          >
            + New {meta?.label ?? entity}
          </button>
        ) : null}
      </header>

      <section className="card toolbar" aria-label="Filters">
        <div className="toolbar-row">
          <div className="field">
            <label htmlFor="entity-select">Entity</label>
            <select
              id="entity-select"
              value={entity}
              onChange={(e) => changeEntity(e.target.value)}
              disabled={!entitiesQuery.data || entitiesQuery.data.length === 0}
            >
              {!entity ? <option value="">— Select entity —</option> : null}
              {(entitiesQuery.data ?? []).map((name) => (
                <option key={name} value={name}>
                  {getEntityMeta(name)?.label ?? name}
                </option>
              ))}
              {entity && !(entitiesQuery.data ?? []).includes(entity) ? (
                <option value={entity}>{entity}</option>
              ) : null}
            </select>
            {entitiesQuery.isError ? (
              <p className="field-error" role="alert">
                Could not load entity registry: {entitiesQuery.error instanceof ApiError ? entitiesQuery.error.message : String(entitiesQuery.error)}
              </p>
            ) : null}
          </div>

          <div className="field">
            <label htmlFor="unit-id-input">Unit ID</label>
            <input
              id="unit-id-input"
              type="number"
              min={1}
              value={unitIdInput}
              onChange={(e) => {
                setUnitIdInput(e.target.value);
                setPageIndex(0);
              }}
              placeholder="blank = unscoped /all"
            />
          </div>

          <form
            className="field field--search"
            onSubmit={(e) => {
              e.preventDefault();
              setSearch(searchInput.trim() === "" ? undefined : searchInput.trim());
              setPageIndex(0);
            }}
          >
            <label htmlFor="search-input">Search</label>
            <div className="search-row">
              <input
                id="search-input"
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder={meta?.searchFields?.join(", ")}
              />
              <button type="submit" className="btn btn-secondary">
                Apply
              </button>
            </div>
          </form>
        </div>
      </section>

      {error ? (
        <div className="alert alert--error" role="alert">
          <strong>
            {error instanceof ApiError ? `${error.code} (HTTP ${error.status})` : "Error"}
          </strong>
          : {error instanceof ApiError ? error.message : String(error.message ?? error)}
        </div>
      ) : null}

      <section className="card">
        {!entity ? (
          <p className="empty-state">Select an entity to load records.</p>
        ) : rowsQuery.isLoading ? (
          <div className="loading-state" role="status">
            <span className="spinner" aria-hidden="true" /> Loading {meta?.label}…
          </div>
        ) : rows.length === 0 ? (
          <p className="empty-state">
            No records found for <strong>{entity}</strong>.
            {search ? " Try clearing the search or unit filter." : ""}
          </p>
        ) : (
          <>
            {rowsQuery.isFetching ? (
              <p className="refreshing" role="status">
                Refreshing…
              </p>
            ) : null}
            <MasterTable
              entity={entity}
              meta={meta}
              rows={rows}
              onRowClick={(record) => {
                // Pass the row's owning unitId for unit-scoped entities so the
                // edit screen can GET/PUT with the correct scope (red-team F-6).
                const scope =
                  record.unitId != null ? `?unitId=${encodeURIComponent(String(record.unitId))}` : "";
                navigate(`/masters/${entity}/${String(record.id ?? "")}${scope}`);
              }}
            />
          </>
        )}

        {rows.length > 0 ? (
          <div className="pagination">
            <span className="pagination-info">
              Showing {rows.length} row{rows.length === 1 ? "" : "s"} on page {pageIndex + 1}
            </span>
            <div className="pagination-controls">
              <label className="pagination-size">
                Rows
                <select
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(Number(e.target.value));
                    setPageIndex(0);
                  }}
                >
                  {[10, 25, 50].map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </select>
              </label>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={pageIndex === 0}
                onClick={() => setPageIndex((p) => Math.max(0, p - 1))}
              >
                ← Prev
              </button>
              <span className="pagination-page">Page {pageIndex + 1}</span>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={!hasMore}
                onClick={() => setPageIndex((p) => p + 1)}
              >
                Next →
              </button>
            </div>
          </div>
        ) : null}
      </section>
    </div>
  );
}
